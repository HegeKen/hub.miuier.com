import mysql from 'mysql2/promise'
import { getPool, getTableMeta, sanitizeRecord, assertTable } from '../../../../utils/db'
import { parseFilename, type ParsedRom } from '../../../../utils/firmware-parser'

/**
 * 一键修复单条记录：以 recovery / fastboot 文件名为准，
 * 重新解析并回填 version / android / code / device / type / bigver / region / zone 等字段。
 *
 * POST /api/db/:table/:id/autofix
 * 目前仅 roms 表支持。
 * body: { fields?: string[] }  // 可选，仅修复指定字段；缺省修复全部可解析字段
 */
export default defineEventHandler(async (event) => {
  const table = getRouterParam(event, 'table') || ''
  assertTable(table)

  if (table !== 'roms') {
    throw createError({ statusCode: 400, message: '一键修复目前仅支持 roms 表' })
  }

  const id = Number(getRouterParam(event, 'id') || 0)
  if (!id || !Number.isFinite(id)) {
    throw createError({ statusCode: 400, message: '无效的记录 id' })
  }

  const body = await readBody(event).catch(() => ({}))
  const onlyFields: string[] | undefined = Array.isArray(body?.fields) ? body.fields : undefined

  const pool = getPool()

  // 拉取当前记录
  const [rows] = await pool.query<mysql.RowDataPacket[]>('SELECT * FROM roms WHERE id = ? LIMIT 1', [id])
  const row = rows[0]
  if (!row) {
    throw createError({ statusCode: 404, message: `记录不存在 (roms id=${id})` })
  }

  // 同时解析 recovery 与 fastboot，合并结果。
  // recovery 解析 code/device 依赖 branchcode 查表，可能失败；
  // fastboot 的 code 直接嵌在文件名中更可靠，故 code/device 优先取 fastboot。
  const recoveryFn = (row.recovery as string) || ''
  const fastbootFn = (row.fastboot as string) || ''
  if (!recoveryFn && !fastbootFn) {
    throw createError({ statusCode: 400, message: '该记录没有 recovery / fastboot 文件名，无法解析修复' })
  }

  const recParsed = recoveryFn ? await parseFilename(recoveryFn) : null
  const fbParsed = fastbootFn ? await parseFilename(fastbootFn) : null

  // 合并：code/device 优先 fastboot；其余优先 recovery，空值回退另一来源
  const pick = (key: keyof ParsedRom) => {
    const rec = recParsed?.[key]
    const fb = fbParsed?.[key]
    if (key === 'code' || key === 'device') {
      return (fb || rec || '') as string
    }
    return (rec || fb || '') as string
  }

  const parsed = {
    version: pick('version'),
    android: pick('android'),
    code: pick('code'),
    device: pick('device'),
    type: pick('type') as ParsedRom['type'],
    bigver: pick('bigver'),
    region: pick('region'),
    zone: (fbParsed?.zone ?? recParsed?.zone ?? 1) as number,
  }

  if (!parsed.version && !parsed.android && !parsed.code) {
    throw createError({ statusCode: 400, message: '文件名无法解析为有效的 ROM 信息' })
  }

  // 可修复字段映射（仅回填非空解析值）
  // 注意：tag 字段依赖分支逻辑（PRE/DPP/EP 等特殊分支 tag 无法仅从文件名推导），
  // getData 对其处理不完整，故不自动修复 tag，避免误改。
  const fixable: Record<string, unknown> = {}
  if (parsed.version) fixable.version = parsed.version
  if (parsed.android) fixable.android = parsed.android
  if (parsed.code) fixable.code = parsed.code
  if (parsed.device) fixable.device = parsed.device
  if (parsed.type) fixable.type = parsed.type
  if (parsed.bigver) fixable.bigver = parsed.bigver
  if (parsed.region) fixable.region = parsed.region
  fixable.zone = parsed.zone

  // 若指定了 fields 白名单，只保留这些字段
  const candidate = onlyFields
    ? Object.fromEntries(Object.entries(fixable).filter(([k]) => onlyFields.includes(k)))
    : fixable

  // 只更新与当前值不同的字段
  const meta = await getTableMeta('roms')
  const updates: Record<string, unknown> = {}
  const changes: Record<string, { from: unknown; to: unknown }> = {}
  for (const [key, val] of Object.entries(candidate)) {
    const cur = row[key]
    if (cur === undefined) continue
    const curNorm = cur === null ? '' : String(cur)
    const valNorm = val === null ? '' : String(val)
    if (curNorm !== valNorm) {
      updates[key] = val
      changes[key] = { from: cur, to: val }
    }
  }

  if (Object.keys(updates).length === 0) {
    return { ok: true, id, changed: {}, message: '无需修复，所有字段已与文件名一致' }
  }

  // 走 sanitizeRecord 做类型/字段校验
  const sanitized = sanitizeRecord(meta, updates, false)
  const cols = Object.keys(sanitized)
  if (cols.length === 0) {
    return { ok: true, id, changed: {}, message: '无可更新字段' }
  }

  const setSql = cols.map((c) => `\`${c}\` = ?`).join(', ')
  await pool.query(
    `UPDATE roms SET ${setSql} WHERE id = ?`,
    [...cols.map((c) => sanitized[c]), id],
  )

  return { ok: true, id, changed: changes, filename: recoveryFn || fastbootFn }
})
