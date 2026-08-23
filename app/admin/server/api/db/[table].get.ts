import { getPool, getTableMeta, assertTable, searchableColumns, sanitizeSort } from '../../utils/db'

export default defineEventHandler(async (event) => {
  const table = getRouterParam(event, 'table') || ''
  assertTable(table)

  const query = getQuery(event)
  const page = Math.max(1, Number(query.page) || 1)
  const pageSize = Math.min(200, Math.max(1, Number(query.pageSize) || 20))
  const search = typeof query.search === 'string' ? query.search.trim() : ''

  const meta = await getTableMeta(table)
  const { sortCol, sortOrder } = sanitizeSort(meta, typeof query.sort === 'string' ? query.sort : undefined, typeof query.order === 'string' ? query.order : undefined)

  const where = search ? searchableColumns(meta) : []
  let whereSql = ''
  const params: unknown[] = []

  if (where.length > 0) {
    whereSql = `WHERE ${where.map((c) => `\`${c}\` LIKE ?`).join(' OR ')}`
    const like = `%${search}%`
    for (let i = 0; i < where.length; i++) params.push(like)
  }

  const [countRows] = await getPool().query<Array<{ total: string }>>(
    `SELECT COUNT(*) AS total FROM \`${table}\` ${whereSql}`,
    params,
  )
  const total = Number(countRows[0]?.total || 0)

  const offset = (page - 1) * pageSize
  const [rows] = await getPool().query(
    `SELECT * FROM \`${table}\` ${whereSql} ORDER BY \`${sortCol}\` ${sortOrder} LIMIT ? OFFSET ?`,
    [...params, pageSize, offset],
  )

  return { table, rows, total, page, pageSize, sort: sortCol, order: sortOrder }
})
