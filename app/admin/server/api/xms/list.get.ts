import { getPool } from '../../utils/db'

/**
 * XMS（SOTA 系统应用更新）推送列表。
 *
 * 一次推送一行（xms），带应用数量与机型名；应用明细走 /api/xms/apps?xmsId=。
 * 修改字段直接用通用接口：PUT /api/db/xms/:id、PUT /api/db/xms_app/:id 等。
 */

/** XMS 版本号首字母 → 系统大版本（C → OS3、D → OS4……，与导出脚本一致） */
function osLabel(version: string): string {
  const first = String(version || '').trim().charAt(0).toUpperCase()
  if (!/^[A-Z]$/.test(first)) return ''
  const major = first.charCodeAt(0) - 64
  return major >= 1 ? `OS${major}` : ''
}

/** JSON 列（mysql2 可能已解析，也可能是字符串）；解析失败按原样返回 */
function jsonValue(value: unknown): unknown {
  if (typeof value !== 'string') return value ?? null
  if (!value.trim()) return null
  try {
    return JSON.parse(value)
  } catch {
    return value
  }
}

export default defineEventHandler(async (event) => {
  const query = getQuery(event)
  const page = Math.max(1, Number(query.page) || 1)
  const pageSize = Math.min(100, Math.max(1, Number(query.pageSize) || 20))
  const search = typeof query.search === 'string' ? query.search.trim() : ''
  const latestOnly = String(query.latest ?? '') === '1'
  const version = typeof query.version === 'string' ? query.version.trim() : ''

  const where: string[] = []
  const params: unknown[] = []

  if (search) {
    where.push('(x.`device` LIKE ? OR x.`code` LIKE ? OR x.`version` LIKE ? '
      + 'OR x.`rom_version` LIKE ? OR x.`notice` LIKE ?)')
    for (let i = 0; i < 5; i++) params.push(`%${search}%`)
  }
  if (latestOnly) where.push('x.`is_latest` = 1')
  if (version) {
    where.push('x.`version` = ?')
    params.push(version)
  }
  const whereSql = where.length ? `WHERE ${where.join(' AND ')}` : ''

  const pool = getPool()

  const [countRows] = await pool.query(
    `SELECT COUNT(*) AS total FROM \`xms\` x ${whereSql}`,
    params,
  ) as [Array<{ total: string | number }>, unknown]
  const total = Number(countRows[0]?.total ?? 0)

  const [rows] = await pool.query(
    `SELECT x.*, COALESCE(d.\`full_names\`, d.\`names\`) AS device_names,
            (SELECT COUNT(*) FROM \`xms_app\` a WHERE a.\`xms_id\` = x.\`id\`) AS app_count
       FROM \`xms\` x
       LEFT JOIN \`devices\` d ON d.\`device\` = x.\`device\` AND d.\`code\` = x.\`code\`
       ${whereSql}
      ORDER BY x.\`is_latest\` DESC, x.\`push_date\` DESC, x.\`id\` DESC
      LIMIT ? OFFSET ?`,
    [...params, pageSize, (page - 1) * pageSize],
  ) as [Array<Record<string, unknown>>, unknown]

  const [versionRows] = await pool.query(
    'SELECT DISTINCT `version` FROM `xms` ORDER BY `version`',
  ) as [Array<{ version: string }>, unknown]

  const items = (rows || []).map((row) => {
    const versionText = String(row.version ?? '')
    return {
      id: Number(row.id),
      device: String(row.device ?? ''),
      code: String(row.code ?? ''),
      region: String(row.region ?? ''),
      zone: row.zone === null || row.zone === undefined ? null : Number(row.zone),
      android: String(row.android ?? ''),
      version: versionText,
      os: osLabel(versionText),
      romVersion: String(row.rom_version ?? ''),
      romId: row.rom_id === null || row.rom_id === undefined ? null : Number(row.rom_id),
      pkgCnt: row.pkg_cnt === null || row.pkg_cnt === undefined ? null : Number(row.pkg_cnt),
      prio: row.prio === null || row.prio === undefined ? null : Number(row.prio),
      notice: String(row.notice ?? ''),
      changelog: jsonValue(row.changelog),
      isLatest: Number(row.is_latest) === 1,
      pushDate: row.push_date ? String(row.push_date).slice(0, 10) : '',
      updateDate: row.update_date === null || row.update_date === undefined ? null : Number(row.update_date),
      appCount: Number(row.app_count ?? 0),
      deviceName: jsonValue(row.device_names),
    }
  })

  return {
    items,
    total,
    page,
    pageSize,
    versions: (versionRows || []).map((row) => String(row.version ?? '')).filter(Boolean),
  }
})
