import { getPool, getTableMeta, assertTable } from '../../../utils/db'

export default defineEventHandler(async (event) => {
  const table = getRouterParam(event, 'table') || ''
  assertTable(table)
  const id = getRouterParam(event, 'id') || ''

  await getTableMeta(table) // 校验表存在

  const [rows] = await getPool().query(`SELECT * FROM \`${table}\` WHERE id = ? LIMIT 1`, [id])
  const row = (rows as unknown[])[0]
  if (!row) {
    throw createError({ statusCode: 404, message: `记录不存在 (${table} id=${id})` })
  }
  return row
})
