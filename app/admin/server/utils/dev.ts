import { getPool } from './db'

/**
 * 开发版周报（dev 表）相关的公共工具。
 *
 * 注意：dev 表**不存机型 / ROM 清单**——某一周的机型由 roms 表表达：
 *     SELECT * FROM roms WHERE dev_week = <week> AND zone = <区域分区>
 * 因此导出（data/scripts/miroms/exporters.py）与后台的绑定页都按这个关系现算，
 * 机型名统一取 devices 表的代表行（优先 tag='CnOO'）。
 */

export interface DevRomRow {
  id: number
  device: string
  name: { zh: string; en: string }
  version: string
  android: string
  release: string
  recovery: string
  fastboot: string
  branch: string
  zone: number
  devWeek: number | null
  devRegion?: string
}

/** 机型名映射：每台设备取代表行（优先 tag='CnOO'，否则最小 id） */
export async function deviceNameMap(): Promise<Map<string, { zh: string; en: string }>> {
  const pool = getPool()
  const [rows] = await pool.query<Array<{ device: string; full_names: string | null; names: string | null }>>(
    `SELECT d.\`device\`, d.\`full_names\`, d.\`names\` FROM \`devices\` d
       JOIN (SELECT \`device\`,
                    COALESCE(MAX(CASE WHEN \`tag\` = 'CnOO' THEN \`id\` END), MIN(\`id\`)) AS ref_id
               FROM \`devices\` GROUP BY \`device\`) ref
         ON ref.\`device\` = d.\`device\` AND ref.ref_id = d.\`id\``,
  )
  const map = new Map<string, { zh: string; en: string }>()
  for (const row of rows || []) {
    const label = { zh: '', en: '' }
    for (const raw of [row.full_names, row.names]) {
      if (!raw) continue
      try {
        const parsed = JSON.parse(raw)
        if (parsed && typeof parsed === 'object') {
          label.zh = label.zh || String(parsed.zh ?? '')
          label.en = label.en || String(parsed.en ?? '')
        }
      } catch {
        /* 非 JSON 时忽略 */
      }
    }
    map.set(String(row.device), label)
  }
  return map
}

/** 取开发版记录的 zone（cn=1 / global=2），缺失时按 region 推断 */
export function zoneOf(region: string, zone: unknown): number {
  if (zone === null || zone === undefined || zone === '') return region === 'global' ? 2 : 1
  return Number(zone)
}

/** 把 roms 查询结果转成接口输出结构 */
export function toDevRom(row: Record<string, unknown>, names: Map<string, { zh: string; en: string }>): DevRomRow {
  const device = String(row.device ?? '')
  return {
    id: Number(row.id),
    device,
    name: names.get(device) || { zh: '', en: '' },
    version: String(row.version ?? ''),
    android: String(row.android ?? ''),
    release: row.release_date ? String(row.release_date).slice(0, 10) : '',
    recovery: String(row.recovery ?? ''),
    fastboot: String(row.fastboot ?? ''),
    branch: String(row.branch ?? ''),
    zone: Number(row.zone ?? 1),
    devWeek: row.dev_week === null || row.dev_week === undefined ? null : Number(row.dev_week),
  }
}

/** 已绑定到某一周的 ROM 列表 */
export async function boundRoms(week: number, zone: number): Promise<DevRomRow[]> {
  const pool = getPool()
  const [rows] = await pool.query<Array<Record<string, unknown>>>(
    `SELECT \`id\`, \`device\`, \`version\`, \`android\`, \`release_date\`, \`recovery\`, \`fastboot\`,
            \`branch\`, \`zone\`, \`dev_week\`
       FROM \`roms\` WHERE \`dev_week\` = ? AND \`zone\` = ?
       ORDER BY \`device\` ASC, \`version\` ASC`,
    [week, zone],
  )
  const names = await deviceNameMap()
  return (rows || []).map((row) => toDevRom(row, names))
}
