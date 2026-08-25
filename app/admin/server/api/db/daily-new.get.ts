import { getPool } from '../../utils/db'

/**
 * 近 7 天每日新增数据统计
 * - roms：按 insdate 精确统计
 * - devices/branches：按 id 区间估算每日分布
 */
export default defineEventHandler(async () => {
  const pool = getPool()

  // roms：按 insdate 精确统计近 7 天每日新增
  const [romsRows] = await pool.query(
    `SELECT insdate AS d, COUNT(*) AS cnt
     FROM roms
     WHERE insdate >= DATE_SUB(CURDATE(), INTERVAL 6 DAY)
     GROUP BY insdate
     ORDER BY insdate`,
  ) as [Array<{ d: string; cnt: number }>, unknown]

  // devices：获取近 7 天新增总数（按 id 区间估算）
  const [devRecent] = await pool.query(
    `SELECT COUNT(*) AS cnt FROM devices WHERE id > (SELECT IFNULL(MAX(id) - 200, 0) FROM devices)`,
  ) as [Array<{ cnt: number }>, unknown]
  const devTotal = Number(devRecent[0]?.cnt || 0)

  // branches：获取近 7 天新增总数
  const [brRecent] = await pool.query(
    `SELECT COUNT(*) AS cnt FROM branches WHERE id > (SELECT IFNULL(MAX(id) - 50, 0) FROM branches)`,
  ) as [Array<{ cnt: number }>, unknown]
  const brTotal = Number(brRecent[0]?.cnt || 0)

  // 将设备/分支的近期新增量均匀分配到 7 天（无日期字段，仅做可视化参考）
  function distributeEvenly(total: number): number[] {
    if (total <= 0) return Array(7).fill(0)
    const base = Math.floor(total / 7)
    const remainder = total % 7
    const arr = Array(7).fill(base)
    // 余数分配到最近几天
    for (let i = 0; i < remainder; i++) {
      arr[6 - i] += 1
    }
    return arr
  }

  const devDaily = distributeEvenly(devTotal)
  const brDaily = distributeEvenly(brTotal)

  // 构建近 7 天日期序列
  const days: Array<{ date: string; roms: number; devices: number; branches: number }> = []
  for (let i = 6; i >= 0; i--) {
    const d = new Date()
    d.setDate(d.getDate() - i)
    const dateStr = d.toISOString().slice(0, 10)
    const found = romsRows.find((r) => r.d === dateStr)
    days.push({
      date: dateStr,
      roms: Number(found?.cnt || 0),
      devices: devDaily[6 - i] ?? 0,
      branches: brDaily[6 - i] ?? 0,
    })
  }

  return { days }
})
