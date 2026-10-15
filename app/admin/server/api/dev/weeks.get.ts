import { getPool } from '../../utils/db'

/**
 * 开发版周次一览（绑定页的快速跳转）：GET /api/dev/weeks
 *
 * 返回每个周次的版本号、大版本以及**已绑定 ROM 数与机型数**，
 * 便于一眼看出哪些周还没绑上（bound = 0 即需要手动关联）。
 */
export default defineEventHandler(async () => {
  const pool = getPool()
  const [rows] = await pool.query<Array<Record<string, unknown>>>(
    `SELECT d.\`week\`, d.\`region\`, d.\`zone\`, d.\`bigver\`, d.\`versions\`, d.\`release_date\`, d.\`source\`,
            COUNT(r.\`id\`) AS bound,
            COUNT(DISTINCT r.\`device\`) AS devices
       FROM \`dev\` d
       LEFT JOIN \`roms\` r ON r.\`dev_week\` = d.\`week\` AND r.\`zone\` = d.\`zone\`
      GROUP BY d.\`week\`, d.\`region\`, d.\`zone\`, d.\`bigver\`, d.\`versions\`, d.\`release_date\`, d.\`source\`
      ORDER BY d.\`week\` DESC, d.\`region\` ASC`,
  )

  const items = (rows || []).map((row) => ({
    week: Number(row.week),
    region: String(row.region ?? 'cn'),
    zone: Number(row.zone ?? 1),
    bigver: String(row.bigver ?? ''),
    versions: String(row.versions ?? ''),
    release: row.release_date ? String(row.release_date).slice(0, 10) : '',
    source: String(row.source ?? ''),
    bound: Number(row.bound ?? 0),
    devices: Number(row.devices ?? 0),
  }))

  return {
    ok: true,
    total: items.length,
    withRoms: items.filter((item) => item.bound > 0).length,
    items,
  }
})
