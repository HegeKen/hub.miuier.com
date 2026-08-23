import mysql from 'mysql2/promise'
import { getPool, getTableMeta, assertTable } from '../../../utils/db'

export default defineEventHandler(async (event) => {
  const table = getRouterParam(event, 'table') || ''
  assertTable(table)
  const id = getRouterParam(event, 'id') || ''

  await getTableMeta(table)

  const [result] = await getPool().query<mysql.ResultSetHeader>(`DELETE FROM \`${table}\` WHERE id = ?`, [id])

  if (result.affectedRows === 0) {
    throw createError({ statusCode: 404, message: `记录不存在 (${table} id=${id})` })
  }
  return { ok: true, table, id: Number(id), affectedRows: result.affectedRows }
})
