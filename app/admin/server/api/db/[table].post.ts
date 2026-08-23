import mysql from 'mysql2/promise'
import { getPool, getTableMeta, assertTable, sanitizeRecord } from '../../utils/db'

export default defineEventHandler(async (event) => {
  const table = getRouterParam(event, 'table') || ''
  assertTable(table)

  const body = await readBody(event)
  if (!body || typeof body !== 'object') {
    throw createError({ statusCode: 400, message: '请求体必须是 JSON 对象' })
  }

  const meta = await getTableMeta(table)
  const record = sanitizeRecord(meta, body)

  const cols = Object.keys(record)
  if (cols.length === 0) {
    throw createError({ statusCode: 400, message: '没有可写入的字段' })
  }

  const placeholders = cols.map(() => '?').join(', ')
  const sql = `INSERT INTO \`${table}\` (\`${cols.join('`, `')}\`) VALUES (${placeholders})`
  const [result] = await getPool().query<mysql.ResultSetHeader>(sql, cols.map((c) => record[c]))

  return { ok: true, table, id: result.insertId, affectedRows: result.affectedRows }
})
