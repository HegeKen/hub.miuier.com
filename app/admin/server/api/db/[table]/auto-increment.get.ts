import { getPool, assertTable } from '../../../utils/db'

/**
 * 查看某表的自增状态：当前 AUTO_INCREMENT、MAX(id) 以及跳号数量。
 * 跳号 = AUTO_INCREMENT - MAX(id) - 1（正常应为 0，表示下一号紧接最后一行）。
 */
export default defineEventHandler(async (event) => {
  const table = getRouterParam(event, 'table') || ''
  assertTable(table)

  const pool = getPool()
  // information_schema.TABLES 的 AUTO_INCREMENT 有统计缓存（默认 24h），
  // 用 SHOW CREATE TABLE 读数据字典里的真实值。
  const [ddlRows] = await pool.query(
    `SHOW CREATE TABLE \`${table}\``,
  ) as [Array<Record<string, string>>, unknown]
  const ddl = ddlRows?.[0]?.['Create Table'] || ''
  const match = ddl.match(/AUTO_INCREMENT=(\d+)/)
  const autoIncrement = match ? Number(match[1]) : 0

  const [maxRows] = await pool.query(
    `SELECT COALESCE(MAX(\`id\`), 0) AS maxId FROM \`${table}\``,
  ) as [Array<{ maxId: string | number }>, unknown]
  const maxId = Number(maxRows?.[0]?.maxId ?? 0)

  return {
    table,
    autoIncrement,
    maxId,
    gap: Math.max(0, autoIncrement - maxId - 1),
  }
})
