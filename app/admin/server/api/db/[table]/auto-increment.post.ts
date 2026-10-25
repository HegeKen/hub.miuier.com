import { getPool, getTableMeta, assertTable } from '../../../utils/db'

/**
 * 重置某表的 AUTO_INCREMENT 为 MAX(id)+1，去掉尾部跳号。
 *
 * 只影响后续新行的编号，不会重排已有 ID（重排会破坏 xms_app.xms_id、
 * roms.dev_week 等引用关系）。
 */
export default defineEventHandler(async (event) => {
  const table = getRouterParam(event, 'table') || ''
  assertTable(table)

  const columns = await getTableMeta(table)
  if (!columns.some((col) => col.isPrimary && col.name === 'id')) {
    throw createError({ statusCode: 400, message: `表 ${table} 没有 id 主键，无法重置自增` })
  }

  const pool = getPool()
  // information_schema.TABLES 的 AUTO_INCREMENT 有统计缓存（默认 24h），
  // 用 SHOW CREATE TABLE 读数据字典里的真实值。
  const readAutoIncrement = async () => {
    const [rows] = await pool.query(
      `SHOW CREATE TABLE \`${table}\``,
    ) as [Array<Record<string, string>>, unknown]
    const ddl = rows?.[0]?.['Create Table'] || ''
    const match = ddl.match(/AUTO_INCREMENT=(\d+)/)
    return match ? Number(match[1]) : 0
  }

  const before = await readAutoIncrement()

  const [maxRows] = await pool.query(
    `SELECT COALESCE(MAX(\`id\`), 0) AS maxId FROM \`${table}\``,
  ) as [Array<{ maxId: string | number }>, unknown]
  const maxId = Number(maxRows?.[0]?.maxId ?? 0)
  const next = maxId + 1

  // ALTER TABLE 只改自增计数器，不会重建数据（InnoDB 元数据操作）
  await pool.query(`ALTER TABLE \`${table}\` AUTO_INCREMENT = ${next}`)

  const after = await readAutoIncrement()
  return {
    ok: true,
    table,
    before,
    after,
    maxId,
    next,
    changed: before !== after,
  }
})
