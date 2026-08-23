import mysql from 'mysql2/promise'
import { getPool, getTableMeta, assertTable, sanitizeRecord } from '../../../utils/db'

export default defineEventHandler(async (event) => {
  const table = getRouterParam(event, 'table') || ''
  assertTable(table)
  const id = getRouterParam(event, 'id') || ''

  const body = await readBody(event)
  if (!body || typeof body !== 'object') {
    throw createError({ statusCode: 400, message: '请求体必须是 JSON 对象' })
  }

  const meta = await getTableMeta(table)
  const record = sanitizeRecord(meta, body, false)

  // 主键不允许修改
  for (const col of meta) {
    if (col.isPrimary) delete record[col.name]
  }

  const cols = Object.keys(record)
  if (cols.length === 0) {
    throw createError({ statusCode: 400, message: '没有可更新的字段' })
  }

  const setSql = cols.map((c) => `\`${c}\` = ?`).join(', ')
  const [result] = await getPool().query<mysql.ResultSetHeader>(
    `UPDATE \`${table}\` SET ${setSql} WHERE id = ?`,
    [...cols.map((c) => record[c]), id],
  )

  if (result.affectedRows === 0) {
    throw createError({ statusCode: 404, message: `记录不存在 (${table} id=${id})` })
  }
  return { ok: true, table, id: Number(id), affectedRows: result.affectedRows }
})
