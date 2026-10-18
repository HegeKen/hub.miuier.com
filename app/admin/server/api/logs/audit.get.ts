import type { RowDataPacket } from 'mysql2/promise'
import { getPool } from '../../utils/db'
import {
  LOG_LANG_COLUMNS,
  LOG_LANG_LABELS,
  decodeLogStructure,
  findDuplicateLogGroups,
  toGroup,
  type LogRow,
} from '../../utils/logs'

interface LogRefRow extends RowDataPacket {
  id: number
  logs: string | null
}

/**
 * logs 表去重体检。
 *
 * 判定标准与写入侧（data/scripts/miroms/logs_store.py）一致：
 * 同一中文日志被拆成多行、且各语言文本互不冲突时，本应是同一行。
 * 去重算法与 data/scripts/miroms/logs_maintenance.py 的 LogMaintenance.duplicate_plan
 * 完全对应（按中文分桶 + 桶内逐对兼容合并），保证后台体检结果与 dedup_logs.py 一致；
 * 发现重复请运行 data/scripts/dedup_logs.py --apply 合并。
 *
 * 同时统计「没有被任何 roms / dev 周报引用」的孤儿行（可用 --prune-unused 清理）。
 */
export default defineEventHandler(async () => {
  const pool = getPool()
  const columns = LOG_LANG_COLUMNS.map((c) => `\`${c}\``).join(', ')
  const [rows] = await pool.query<LogRow[]>(
    `SELECT \`id\`, \`type\`, \`content\`, ${columns} FROM \`logs\``,
  )
  const groups = (rows || []).map(toGroup)
  const info = new Map(groups.map((group) => [group.id, group]))

  const duplicateGroupIds = findDuplicateLogGroups(groups)
  const duplicateGroups = duplicateGroupIds.slice(0, 50).map((ids) => ({
    ids,
    total: ids.length,
    samples: ids.slice(0, 4).map((id) => {
      const item = info.get(id)!
      return {
        id,
        content: item.content.slice(0, 60),
        languages: Object.keys(item.texts).map((column) => LOG_LANG_LABELS[column] || column),
      }
    }),
  }))

  // 孤儿行：没有任何 roms / dev 周报引用
  const used = new Set<number>()
  for (const table of ['roms', 'dev']) {
    let lastId = 0
    for (;;) {
      const [refs] = await pool.query<LogRefRow[]>(
        `SELECT \`id\`, \`logs\` FROM \`${table}\` WHERE \`id\` > ? AND \`logs\` IS NOT NULL ORDER BY \`id\` LIMIT 500`,
        [lastId],
      )
      if (!refs?.length) break
      lastId = Number(refs[refs.length - 1]!.id)
      for (const ref of refs) {
        for (const item of decodeLogStructure(ref.logs)) {
          used.add(item.moduleId)
          item.entryIds.forEach((id) => used.add(id))
        }
      }
    }
  }
  const unreferenced = [...info.keys()].filter((id) => !used.has(id))

  return {
    ok: true,
    total: info.size,
    duplicateGroups,
    duplicateGroupCount: duplicateGroupIds.length,
    duplicateRowCount: duplicateGroupIds.reduce((sum, ids) => sum + ids.length, 0),
    unreferencedCount: unreferenced.length,
    unreferencedSample: unreferenced.slice(0, 20),
  }
})
