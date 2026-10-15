import { getPool } from '../../utils/db'
import { LOG_LANG_COLUMNS, LOG_LANG_LABELS } from '../../utils/logs'

/**
 * 日志分组列表（手动录入页右侧的「最近 / 检索」列表）。
 *
 * 查询参数：
 *   page / pageSize    分页
 *   search             关键字（主内容 + 各语言列 LIKE）
 *   type               module / log
 *   missing            只显示缺少该语言列的日志（语言列名，如 logs_ja）
 *   id                 直接查某个分组
 */
export default defineEventHandler(async (event) => {
  const query = getQuery(event)
  const page = Math.max(Number(query.page) || 1, 1)
  const pageSize = Math.min(Math.max(Number(query.pageSize) || 30, 1), 200)
  const search = String(query.search ?? '').trim()
  const type = query.type === 'module' ? 'module' : query.type === 'log' ? 'log' : ''
  const missing = typeof query.missing === 'string' && (LOG_LANG_COLUMNS as readonly string[]).includes(query.missing)
    ? query.missing
    : ''

  const where: string[] = []
  const params: unknown[] = []
  if (type) {
    where.push('`type` = ?')
    params.push(type)
  }
  if (search) {
    const like = `%${search}%`
    const ors = ['`content` LIKE ?', ...LOG_LANG_COLUMNS.map((c) => `\`${c}\` LIKE ?`)]
    where.push(`(${ors.join(' OR ')})`)
    params.push(...ors.map(() => like))
  }
  if (missing) {
    where.push(`\`${missing}\` IS NULL`)
  }
  const whereSql = where.length ? `WHERE ${where.join(' AND ')}` : ''

  const pool = getPool()
  const [countRows] = await pool.query<Array<{ total: number }>>(
    `SELECT COUNT(*) AS total FROM \`logs\` ${whereSql}`,
    params,
  )
  const total = Number(countRows?.[0]?.total || 0)

  const coverage = LOG_LANG_COLUMNS.map((c) => `SUM(\`${c}\` IS NOT NULL) AS \`${c}\``).join(', ')
  const [coverageRows] = await pool.query<Array<Record<string, number>>>(
    `SELECT ${coverage} FROM \`logs\` ${whereSql}`,
    params,
  )
  const coverageCounts: Record<string, number> = {}
  for (const column of LOG_LANG_COLUMNS) coverageCounts[column] = Number(coverageRows?.[0]?.[column] || 0)

  const [rows] = await pool.query<Array<Record<string, unknown>>>(
    `SELECT * FROM \`logs\` ${whereSql} ORDER BY \`id\` DESC LIMIT ? OFFSET ?`,
    [...params, pageSize, (page - 1) * pageSize],
  )

  const items = (rows || []).map((row) => {
    const texts: Record<string, string> = {}
    for (const column of LOG_LANG_COLUMNS) {
      const value = row[column]
      if (typeof value === 'string' && value) texts[column] = value
    }
    return {
      id: Number(row.id),
      type: String(row.type),
      content: String(row.content ?? ''),
      texts,
      languages: Object.keys(texts),
      missing: LOG_LANG_COLUMNS.filter((c) => !texts[c]),
    }
  })

  return {
    ok: true,
    page,
    pageSize,
    total,
    items,
    languages: LOG_LANG_COLUMNS.map((c) => ({
      column: c,
      label: LOG_LANG_LABELS[c] || c,
      count: coverageCounts[c] || 0,
    })),
  }
})
