import mysql from 'mysql2/promise'
import { getPool, assertTable } from '../../../utils/db'

/**
 * 批量更新同机型所有分支的指定字段
 * body: { device: string, fields: Record<string, unknown> }
 * 仅允许 devices 表，fields 中的字段必须在白名单内
 */
const SYNC_FIELDS = new Set(['device', 'devtag', 'internal', 'full_brands', 'brands', 'full_names', 'names'])

export default defineEventHandler(async (event) => {
  const table = getRouterParam(event, 'table') || ''
  assertTable(table)

  if (table !== 'devices') {
    throw createError({ statusCode: 400, message: '批量更新仅支持 devices 表' })
  }

  const body = await readBody(event)
  if (!body || typeof body !== 'object' || !body.device || !body.fields || typeof body.fields !== 'object') {
    throw createError({ statusCode: 400, message: '请求体必须包含 device 和 fields' })
  }

  const device: string = body.device
  const rawFields: Record<string, unknown> = body.fields

  // 只允许同步 full_brands / brands / full_names / names
  const fields: Record<string, unknown> = {}
  for (const [k, v] of Object.entries(rawFields)) {
    if (SYNC_FIELDS.has(k)) {
      fields[k] = v === '' ? null : v
    }
  }

  if (Object.keys(fields).length === 0) {
    return { ok: true, affectedRows: 0 }
  }

  const setParts = Object.keys(fields).map((c) => `\`${c}\` = ?`)
  const sql = `UPDATE \`${table}\` SET ${setParts.join(', ')} WHERE \`device\` = ?`
  const params = [...Object.values(fields), device]

  const [result] = await getPool().query<mysql.ResultSetHeader>(sql, params)

  return { ok: true, table, device, affectedRows: result.affectedRows }
})
