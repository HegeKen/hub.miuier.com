import { getPool } from '../../utils/db'
import { deviceNameMap, toDevRom, zoneOf } from '../../utils/dev'

/**
 * 候选 ROM 检索（开发版绑定页右侧）：GET /api/dev/roms
 *
 * 查询参数：
 *   week / region      目标周次（用于对齐 zone，并把已绑定到别周的 ROM 标出来）
 *   search             关键字：机型代号 / 版本号 / 卡刷线刷包文件名
 *   device             指定机型代号（精确）
 *   branch             分支，默认 X（开发版）
 *   zone               区域分区，默认取目标周的 zone（0 = 不限）
 *   year               只看某年发布（老周报时间跨度大，按年缩小范围）
 *   bound              all=全部 / free=只看未绑定 / other=只看已绑到别的周
 *   page / pageSize    分页
 */
export default defineEventHandler(async (event) => {
  const query = getQuery(event)
  const week = Number(query.week) || 0
  const region = String(query.region || 'cn') === 'global' ? 'global' : 'cn'
  const search = String(query.search ?? '').trim()
  const device = String(query.device ?? '').trim()
  const branch = String(query.branch ?? 'X').trim()
  const bound = ['all', 'free', 'other'].includes(String(query.bound)) ? String(query.bound) : 'free'
  const year = Number(query.year) || 0
  const page = Math.max(Number(query.page) || 1, 1)
  const pageSize = Math.min(Math.max(Number(query.pageSize) || 40, 1), 200)

  const pool = getPool()

  // 目标周的 zone：用于「同分区优先」与绑定时的分区一致性
  let zone = Number(query.zone) || 0
  if (!zone) {
    const [devRows] = await pool.query<Array<{ zone: unknown }>>(
      'SELECT `zone` FROM `dev` WHERE `week` = ? AND `region` = ? LIMIT 1',
      [week, region],
    )
    zone = devRows?.length ? zoneOf(region, devRows[0]!.zone) : (region === 'global' ? 2 : 1)
  }

  const where: string[] = ["`branch` = ?"]
  const params: unknown[] = [branch]
  if (search) {
    where.push('(`device` LIKE ? OR `version` LIKE ? OR `recovery` LIKE ? OR `fastboot` LIKE ?)')
    const like = `%${search}%`
    params.push(like, like, like, like)
  }
  if (device) {
    where.push('`device` = ?')
    params.push(device)
  }
  if (year) {
    where.push('YEAR(`release_date`) = ?')
    params.push(year)
  }
  if (bound === 'free') where.push('`dev_week` IS NULL')
  if (bound === 'other') where.push('`dev_week` IS NOT NULL AND `dev_week` <> ?')
  if (bound === 'other') params.push(week)

  const whereSql = where.join(' AND ')
  const [countRows] = await pool.query<Array<{ total: number }>>(
    `SELECT COUNT(*) AS total FROM \`roms\` WHERE ${whereSql}`,
    params,
  )
  const total = Number(countRows?.[0]?.total ?? 0)

  // 同分区的排在前面，其次按发布日期倒序（没有日期的排后面）
  const [rows] = await pool.query<Array<Record<string, unknown>>>(
    `SELECT \`id\`, \`device\`, \`version\`, \`android\`, \`release_date\`, \`recovery\`, \`fastboot\`,
            \`branch\`, \`zone\`, \`dev_week\`
       FROM \`roms\` WHERE ${whereSql}
       ORDER BY (\`zone\` = ?) DESC, \`release_date\` IS NULL, \`release_date\` DESC, \`id\` DESC
       LIMIT ? OFFSET ?`,
    [...params, zone, pageSize, (page - 1) * pageSize],
  )

  // 版本号候选：老周报的公告里常写作「5.20」，库里是「21.5.20」「22.5.20」，
  // 直接 LIKE 检索会挑错年份，这里把命中的完整版本号按条数返回，前端一键精确筛选
  const [facetRows] = await pool.query<Array<{ version: string; total: number; year: number | null }>>(
    `SELECT \`version\`, COUNT(*) AS total, MIN(YEAR(\`release_date\`)) AS year
       FROM \`roms\` WHERE ${whereSql} AND \`version\` IS NOT NULL AND \`version\` <> ''
      GROUP BY \`version\` ORDER BY total DESC, \`version\` DESC LIMIT 12`,
    params,
  )

  const names = await deviceNameMap()
  const items = (rows || []).map((row) => toDevRom(row, names))
  // 已绑定到别的周的，补上那一周的区域，便于前端提示
  const otherWeeks = [...new Set(items.map((item) => item.devWeek).filter((w): w is number => !!w))]
  if (otherWeeks.length) {
    const [devRows] = await pool.query<Array<{ week: number; region: string }>>(
      `SELECT \`week\`, \`region\` FROM \`dev\` WHERE \`week\` IN (${otherWeeks.map(() => '?').join(',')})`,
      otherWeeks,
    )
    const regionOf = new Map((devRows || []).map((row) => [Number(row.week), String(row.region)]))
    for (const item of items) {
      if (item.devWeek) item.devRegion = regionOf.get(item.devWeek) || ''
    }
  }

  return {
    ok: true,
    total,
    page,
    pageSize,
    zone,
    items,
    versionFacets: (facetRows || []).map((row) => ({
      version: String(row.version),
      total: Number(row.total ?? 0),
      year: row.year ? Number(row.year) : null,
    })),
  }
})
