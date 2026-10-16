import { getPool } from '../../utils/db'

/**
 * 站点日志（sitelog 表）。
 *
 * 查询参数：
 *   site   站点标识，默认 hub.admin（管理后台自己的日志）；hub.web 为官网前端的日志，
 *          nuxtmr / hyperos.fans 为两个参考站点的历史日志
 *
 * 只返回 kind='entry' 的版本条目（kind='meta' 是站点级元信息，如 hyperos.fans 的图标调色板），
 * 按 sort_order 升序（最新的版本在最前）。条目格式与导出给前端的 api/v3/sitelog.json 一致。
 */

const KNOWN_SITES = ['hub.admin', 'hub.web', 'nuxtmr', 'hyperos.fans'] as const

/** logs 列是 JSON：mysql2 通常已解析为数组，兼容字符串与历史单引号写法 */
function normalizeLogs(value: unknown): Array<{ zh: string; en: string }> {
  let parsed: unknown = value
  if (typeof value === 'string') {
    if (!value.trim()) return []
    try {
      parsed = JSON.parse(value)
    } catch {
      try {
        parsed = JSON.parse(value.replace(/'/g, '"'))
      } catch {
        return []
      }
    }
  }
  if (!Array.isArray(parsed)) return []
  return parsed
    .filter((item) => item && typeof item === 'object')
    .map((item) => {
      const row = item as Record<string, unknown>
      return { zh: String(row.zh ?? ''), en: String(row.en ?? '') }
    })
}

export default defineEventHandler(async (event) => {
  const query = getQuery(event)
  const site = String(query.site ?? 'hub.admin')

  if (!(KNOWN_SITES as readonly string[]).includes(site)) {
    throw createError({ statusCode: 400, message: `未知站点：${site}` })
  }

  const pool = getPool()
  const [rows] = await pool.query<Array<Record<string, unknown>>>(
    'SELECT `id`, `site`, `ver`, `log_date`, `logs`, `sort_order` FROM `sitelog` '
      + "WHERE `site` = ? AND `kind` = 'entry' ORDER BY `sort_order`, `id`",
    [site],
  )

  const items = (rows || []).map((row) => ({
    id: Number(row.id),
    ver: String(row.ver ?? ''),
    date: row.log_date ? String(row.log_date).slice(0, 10) : '',
    log: normalizeLogs(row.logs),
  }))

  return {
    ok: true,
    site,
    sites: KNOWN_SITES,
    latest: items[0]?.ver ?? '',
    total: items.length,
    items,
  }
})
