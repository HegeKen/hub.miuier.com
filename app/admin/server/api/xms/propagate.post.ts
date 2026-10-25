import { getPool } from '../../utils/db'
import { ensureModuleLog } from '../../utils/xms'

/**
 * 把当前行的名称类字段一次性同步到同包名的所有记录。
 *
 * POST /api/xms/propagate
 * body: { pkg, name?, name_en?, names?, apk_name? }
 * → { ok, pkg, updated }
 *
 * 只同步 name / name_en / names / apk_name（xms_app 自查里的「不一致 / 缺失」问题），
 * 日志、版本号、安装包文件等仍按各自的推送保留。
 */

const PKG_RE = /^[A-Za-z][A-Za-z0-9._]*$/

function parseNames(raw: unknown): string | null {
  if (raw === null || raw === undefined) return null
  const text = String(raw).trim()
  if (!text) return null
  let parsed: unknown
  try {
    parsed = JSON.parse(text)
  } catch {
    throw createError({ statusCode: 400, message: 'names 不是合法的 JSON' })
  }
  if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
    throw createError({ statusCode: 400, message: 'names 必须是 JSON 对象' })
  }
  return text
}

export default defineEventHandler(async (event) => {
  const body = await readBody<Record<string, unknown>>(event)
  const pkg = String(body?.pkg || '').trim()
  if (!PKG_RE.test(pkg)) {
    throw createError({ statusCode: 400, message: '包名格式不正确（如 com.miui.securitycenter）' })
  }

  const fields: Record<string, string | null> = {}
  if (body?.name !== undefined) fields.name = String(body.name || '').trim()
  if (body?.name_en !== undefined) fields.name_en = String(body.name_en || '').trim()
  if (body?.names !== undefined) fields.names = parseNames(body.names)
  if (body?.apk_name !== undefined) fields.apk_name = String(body.apk_name || '').trim()

  const columns = Object.keys(fields)
  if (!columns.length) {
    throw createError({ statusCode: 400, message: '没有可同步的字段' })
  }

  // 名称本身也登记到 logs 的 module 行（缺失时创建 zh/en 占位）
  const name = fields.name ?? ''
  if (name) {
    await ensureModuleLog(name, fields.name_en ?? '')
  }

  const [result] = await getPool().query(
    `UPDATE \`xms_app\` SET ${columns.map((key) => `\`${key}\` = ?`).join(', ')} WHERE \`pkg\` = ?`,
    [...columns.map((key) => fields[key]), pkg],
  ) as [{ affectedRows: number }, unknown]

  return { ok: true, pkg, updated: Number(result?.affectedRows || 0) }
})
