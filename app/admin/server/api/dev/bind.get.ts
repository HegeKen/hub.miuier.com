import { getPool } from '../../utils/db'
import { boundRoms, zoneOf } from '../../utils/dev'

/**
 * 开发版周报的「已绑定 ROM」：GET /api/dev/bind?week=500&region=cn
 *
 * dev 表不再存机型 / ROM 清单 JSON，某一周的机型 = roms 表中 dev_week = 该周且 zone 相同的行。
 * 返回该周信息 + 已绑定 ROM（含 devices 表里的中英文名），供绑定页左右两栏使用。
 */
export default defineEventHandler(async (event) => {
  const query = getQuery(event)
  const week = Number(query.week)
  if (!Number.isFinite(week) || week <= 0) {
    throw createError({ statusCode: 400, message: 'week 参数无效' })
  }
  const region = String(query.region || 'cn') === 'global' ? 'global' : 'cn'

  const pool = getPool()
  const [devRows] = await pool.query<Array<Record<string, unknown>>>(
    `SELECT \`id\`, \`week\`, \`region\`, \`zone\`, \`bigver\`, \`versions\`, \`title_zh\`, \`title_en\`,
            \`release_date\`, \`source\`, \`show\`
       FROM \`dev\` WHERE \`week\` = ? AND \`region\` = ? LIMIT 1`,
    [week, region],
  )
  const dev = devRows?.[0]
  if (!dev) {
    return { ok: true, dev: null, zone: null, roms: [], deviceCount: 0 }
  }
  const zone = zoneOf(region, dev.zone)
  const roms = await boundRoms(week, zone)

  return {
    ok: true,
    dev: {
      id: Number(dev.id),
      week: Number(dev.week),
      region: String(dev.region),
      zone,
      bigver: String(dev.bigver ?? ''),
      versions: String(dev.versions ?? ''),
      title: { zh: String(dev.title_zh ?? ''), en: String(dev.title_en ?? '') },
      release: dev.release_date ? String(dev.release_date).slice(0, 10) : '',
      source: String(dev.source ?? ''),
      show: Number(dev.show ?? 1),
    },
    zone,
    roms,
    deviceCount: new Set(roms.map((item) => item.device)).size,
  }
})
