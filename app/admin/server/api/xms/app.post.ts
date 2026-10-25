import { getPool } from '../../utils/db'
import { ensureModuleLog, moduleTranslations } from '../../utils/xms'

/**
 * 新增 / 更新一条 XMS 应用，并把「同一包名」的其它推送一并更新。
 *
 * POST /api/xms/app
 * body: { id?, xms_id, pkg, name, name_en, names, pkg_ver, apk_name, file_name,
 *         size, md5, sha256, logs, propagate? }
 *
 * - names 为空时按 name / name_en 生成；随后用 logs 表 type='module' 的同名译文
 *   补齐缺失语种（显式填写的语种不被覆盖）。
 * - propagate 默认 true：保存后把 name / name_en / names 同步到所有 pkg 相同的行
 *   （日志内容按推送各自保留，不参与同步）。
 */

const PKG_RE = /^[A-Za-z][A-Za-z0-9._]*$/

interface XmsAppBody {
  id?: number | null
  xms_id?: number
  pkg?: string
  name?: string
  name_en?: string
  names?: string | null
  pkg_ver?: string
  apk_name?: string
  file_name?: string
  size?: number | string | null
  md5?: string
  sha256?: string
  logs?: string | null
  propagate?: boolean
}

/** 解析 JSON 文本；空串返回 null */
function parseJson(raw: unknown): { ok: boolean; value: unknown } {
  if (raw === null || raw === undefined) return { ok: true, value: null }
  const text = String(raw).trim()
  if (!text) return { ok: true, value: null }
  try {
    return { ok: true, value: JSON.parse(text) }
  } catch {
    return { ok: false, value: null }
  }
}

export default defineEventHandler(async (event) => {
  const body = await readBody<XmsAppBody>(event)

  const xmsId = Number(body?.xms_id)
  const pkg = String(body?.pkg || '').trim()
  if (!Number.isInteger(xmsId) || xmsId <= 0) {
    throw createError({ statusCode: 400, message: 'xms_id 无效' })
  }
  if (!PKG_RE.test(pkg)) {
    throw createError({ statusCode: 400, message: '包名格式不正确（如 com.miui.securitycenter）' })
  }

  const namesParsed = parseJson(body?.names)
  if (!namesParsed.ok) {
    throw createError({ statusCode: 400, message: 'names 不是合法的 JSON' })
  }
  if (namesParsed.value !== null
    && (typeof namesParsed.value !== 'object' || Array.isArray(namesParsed.value))) {
    throw createError({ statusCode: 400, message: 'names 必须是 JSON 对象' })
  }

  const logsParsed = parseJson(body?.logs)
  if (!logsParsed.ok) {
    throw createError({ statusCode: 400, message: '更新日志不是合法的 JSON' })
  }
  if (logsParsed.value !== null && !Array.isArray(logsParsed.value)) {
    throw createError({ statusCode: 400, message: '更新日志必须是 JSON 数组' })
  }

  const pool = getPool()
  const id = body?.id === null || body?.id === undefined ? 0 : Number(body.id)

  const [pushRows] = await pool.query(
    'SELECT `id` FROM `xms` WHERE `id` = ?',
    [xmsId],
  ) as [Array<Record<string, unknown>>, unknown]
  if (!pushRows.length) {
    throw createError({ statusCode: 404, message: `XMS 推送不存在 (id=${xmsId})` })
  }

  if (id) {
    const [rows] = await pool.query(
      'SELECT `id`, `xms_id` FROM `xms_app` WHERE `id` = ?',
      [id],
    ) as [Array<Record<string, unknown>>, unknown]
    if (!rows.length || Number(rows[0]!.xms_id) !== xmsId) {
      throw createError({ statusCode: 404, message: `应用不存在或不属于推送 #${xmsId}（id=${id}）` })
    }
  }

  const name = String(body?.name || '').trim()
  const nameEn = String(body?.name_en || '').trim()

  // 先按表单的中英文名整理，再用 logs 表补全其它语种
  const namesObj: Record<string, string> = {}
  const namesSource = (namesParsed.value as Record<string, unknown> | null) || {}
  for (const [key, value] of Object.entries(namesSource)) {
    const text = String(value ?? '').trim()
    if (text) namesObj[key] = text
  }
  if (name) namesObj.zh = name
  if (nameEn) namesObj.en = nameEn

  const translations = await moduleTranslations(name || namesObj.zh || '', nameEn || namesObj.en || '')
  for (const [key, value] of Object.entries(translations)) {
    if (!namesObj[key]) namesObj[key] = value
  }
  if (!namesObj.zh && translations.zh) namesObj.zh = translations.zh
  if (!namesObj.en) namesObj.en = namesObj.zh || translations.en || ''

  const canonicalZh = String(namesObj.zh || '').trim()
  const canonicalEn = String(namesObj.en || canonicalZh).trim()

  // 名称在 logs 的 module 里不存在时先写占位，后续可补全其它语种
  const ensured = canonicalZh ? await ensureModuleLog(canonicalZh, canonicalEn) : null
  const namesJson = Object.keys(namesObj).length ? JSON.stringify(namesObj) : null
  const logsJson = Array.isArray(logsParsed.value) && logsParsed.value.length
    ? JSON.stringify(logsParsed.value)
    : null

  const sizeValue = body?.size === null || body?.size === undefined || String(body.size).trim() === ''
    ? null
    : Number(body.size)
  if (sizeValue !== null && (!Number.isFinite(sizeValue) || sizeValue < 0)) {
    throw createError({ statusCode: 400, message: '体积必须是非负数字（字节）' })
  }

  const fields = {
    xms_id: xmsId,
    pkg,
    name: canonicalZh,
    name_en: canonicalEn,
    names: namesJson,
    pkg_ver: String(body?.pkg_ver || '').trim(),
    apk_name: String(body?.apk_name || '').trim(),
    file_name: String(body?.file_name || '').trim(),
    size: sizeValue,
    md5: String(body?.md5 || '').trim(),
    sha256: String(body?.sha256 || '').trim(),
    logs: logsJson,
  }

  let savedId = id
  try {
    if (id) {
      const columns = Object.keys(fields).filter((key) => key !== 'xms_id')
      await pool.query(
        `UPDATE \`xms_app\` SET ${columns.map((key) => `\`${key}\` = ?`).join(', ')} WHERE \`id\` = ?`,
        [...columns.map((key) => fields[key as keyof typeof fields]), id],
      )
    } else {
      const columns = Object.keys(fields)
      const [result] = await pool.query(
        `INSERT INTO \`xms_app\` (${columns.map((key) => `\`${key}\``).join(', ')}) `
        + `VALUES (${columns.map(() => '?').join(', ')})`,
        columns.map((key) => fields[key as keyof typeof fields]),
      ) as [{ insertId: number }, unknown]
      savedId = result.insertId
    }
  } catch (e) {
    const message = e instanceof Error ? e.message : String(e)
    if (/duplicate entry/i.test(message)) {
      throw createError({ statusCode: 409, message: `推送 #${xmsId} 下已存在包名 ${pkg} 的记录` })
    }
    throw createError({ statusCode: 500, message: `保存失败：${message}` })
  }

  // 同一包名的所有推送共享一份名称 / 多语言；日志按推送各自保留
  let propagated = 0
  if (body?.propagate !== false) {
    const [result] = await pool.query(
      'UPDATE `xms_app` SET `name` = ?, `name_en` = ?, `names` = ? WHERE `pkg` = ?',
      [canonicalZh, canonicalEn, namesJson, pkg],
    ) as [{ affectedRows: number }, unknown]
    propagated = Number(result?.affectedRows || 0)
  }

  return {
    ok: true,
    id: savedId,
    propagated,
    translated: Object.keys(translations).length,
    logCreated: ensured?.created ?? false,
    name: canonicalZh,
    name_en: canonicalEn,
    names: namesObj,
  }
})
