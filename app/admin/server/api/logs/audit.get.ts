import { getPool } from '../../utils/db'
import { LOG_LANG_COLUMNS, LOG_LANG_LABELS, decodeLogStructure } from '../../utils/logs'

/**
 * logs 表去重体检。
 *
 * 判定标准与写入侧（data/scripts/miroms/logs_store.py）完全一致：
 * 类型相同、各语言文本互不冲突、且至少共享一门语言文本的两行，本应是同一行。
 * 只要写入走 LogStore.write_logs / /api/logs/manual 就不会出现重复；
 * 这里作为兜底校验，发现重复请运行 data/scripts/dedup_logs.py --apply 合并。
 *
 * 同时统计「没有被任何 roms / dev 周报引用」的孤儿行（可用 --prune-unused 清理）。
 */
export default defineEventHandler(async () => {
  const pool = getPool()
  const columns = LOG_LANG_COLUMNS.map((c) => `\`${c}\``).join(', ')
  const [rows] = await pool.query<Array<Record<string, unknown>>>(
    `SELECT \`id\`, \`type\`, \`content\`, ${columns} FROM \`logs\``,
  )

  const info = new Map<number, { type: string; content: string; texts: Record<string, string> }>()
  const buckets = new Map<string, number[]>()
  for (const row of rows || []) {
    const id = Number(row.id)
    const texts: Record<string, string> = {}
    for (const column of LOG_LANG_COLUMNS) {
      const value = row[column]
      if (typeof value === 'string' && value) texts[column] = value
    }
    const type = String(row.type)
    info.set(id, { type, content: String(row.content ?? ''), texts })
    for (const [column, value] of Object.entries(texts)) {
      const key = `${type}\u0000${column}\u0000${value}`
      const list = buckets.get(key)
      if (list) list.push(id)
      else buckets.set(key, [id])
    }
  }

  const parent = new Map<number, number>()
  const texts = new Map<number, Record<string, string>>()
  for (const [id, item] of info) {
    parent.set(id, id)
    texts.set(id, { ...item.texts })
  }
  const find = (node: number): number => {
    let root = node
    while (parent.get(root) !== root) root = parent.get(root)!
    let cur = node
    while (parent.get(cur) !== root) {
      const next = parent.get(cur)!
      parent.set(cur, root)
      cur = next
    }
    return root
  }
  const compatible = (left: number, right: number): boolean => {
    if (info.get(left)!.type !== info.get(right)!.type) return false
    const a = texts.get(left)!
    const b = texts.get(right)!
    for (const column of LOG_LANG_COLUMNS) {
      if (a[column] && b[column] && a[column] !== b[column]) return false
    }
    return true
  }

  // 反复扫描直到不再合并（一次扫描可能因合并改变根节点而漏掉传递关系）
  for (;;) {
    let merged = false
    for (const members of buckets.values()) {
      if (members.length < 2) continue
      const roots: number[] = []
      for (const member of members) {
        const root = find(member)
        if (!roots.includes(root)) roots.push(root)
      }
      if (roots.length < 2) continue
      const base = roots[0]!
      for (const other of roots.slice(1)) {
        const baseRoot = find(base)
        const otherRoot = find(other)
        if (baseRoot === otherRoot || !compatible(baseRoot, otherRoot)) continue
        parent.set(otherRoot, baseRoot)
        texts.set(baseRoot, { ...texts.get(otherRoot)!, ...texts.get(baseRoot)! })
        merged = true
      }
    }
    if (!merged) break
  }

  const groups = new Map<number, number[]>()
  for (const id of info.keys()) {
    const root = find(id)
    const list = groups.get(root)
    if (list) list.push(id)
    else groups.set(root, [id])
  }

  const duplicateGroups = [...groups.values()]
    .filter((ids) => ids.length > 1)
    .sort((a, b) => b.length - a.length)
    .map((ids) => ({
      ids: ids.sort((a, b) => a - b),
      total: ids.length,
      samples: ids.slice(0, 4).map((id) => {
        const item = info.get(id)!
        return {
          id,
          content: item.content.slice(0, 60),
          languages: Object.keys(item.texts).map((c) => LOG_LANG_LABELS[c] || c),
        }
      }),
    }))

  // 孤儿行：没有任何 roms / dev 周报引用
  const used = new Set<number>()
  for (const table of ['roms', 'dev']) {
    let lastId = 0
    for (;;) {
      const [refs] = await pool.query<Array<{ id: number; logs: string | null }>>(
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
    duplicateGroups: duplicateGroups.slice(0, 50),
    duplicateGroupCount: duplicateGroups.length,
    duplicateRowCount: duplicateGroups.reduce((sum, g) => sum + g.total, 0),
    unreferencedCount: unreferenced.length,
    unreferencedSample: unreferenced.slice(0, 20),
  }
})
