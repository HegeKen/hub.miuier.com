import { createHash } from 'node:crypto'
import type { Pool, RowDataPacket } from 'mysql2/promise'

/**
 * 更新日志（logs 表）在管理后台侧的工具集。
 *
 * 与 data/scripts/miroms/constants.py 的 CHANGELOG_LOCALES、logs_store.py 保持一致：
 *   logs 表 = 同一日志的各语言版本共用一行（id / type / content / content_hash / logs_xx ...）；
 *   roms.logs = [[模块ID, [条目ID, ...], 语言掩码], ...]（语言掩码第 i 位对应下面的第 i 个语言列）。
 */

/** 语言列（顺序必须与 constants.py 的 CHANGELOG_LOCALES 一致，掩码位序依赖它） */
export const LOG_LANG_COLUMNS = [
  'logs_zh',
  'logs_zh_tw',
  'logs_en',
  'logs_ja',
  'logs_ko',
  'logs_ru',
  'logs_uk',
  'logs_pl',
  'logs_de',
  'logs_fr',
  'logs_it',
  'logs_es',
  'logs_pt',
  'logs_tr',
  'logs_id',
  'logs_vi',
  'logs_th',
  'logs_ar',
  'logs_in',
  'logs_ug',
  'logs_bo',
] as const

export type LogLangColumn = (typeof LOG_LANG_COLUMNS)[number]

/** 语言列 → 中文名（界面展示用） */
export const LOG_LANG_LABELS: Record<string, string> = {
  logs_zh: '简体中文',
  logs_zh_tw: '繁体中文',
  logs_en: '英文',
  logs_ja: '日文',
  logs_ko: '韩文',
  logs_ru: '俄文',
  logs_uk: '乌克兰文',
  logs_pl: '波兰文',
  logs_de: '德文',
  logs_fr: '法文',
  logs_it: '意大利文',
  logs_es: '西班牙文',
  logs_pt: '葡萄牙文',
  logs_tr: '土耳其文',
  logs_id: '印尼文',
  logs_vi: '越南文',
  logs_th: '泰文',
  logs_ar: '阿拉伯文',
  logs_in: '印地文',
  logs_ug: '维吾尔文',
  logs_bo: '藏文',
}

export const LOG_TYPE_LABELS: Record<string, string> = {
  module: '模块名',
  log: '日志条目',
}

export function isLogLang(value: unknown): value is LogLangColumn {
  return typeof value === 'string' && (LOG_LANG_COLUMNS as readonly string[]).includes(value)
}

/** 主内容归一化：去掉首尾空白（与 Python 侧 LogStore.normalize 一致） */
export function normalizeLogText(value: unknown): string {
  return String(value ?? '').trim()
}

export function logTextHash(text: string): string {
  return createHash('md5').update(text, 'utf8').digest('hex')
}

export function columnsToMask(columns: string[]): number {
  let mask = 0
  for (const column of columns) {
    const index = (LOG_LANG_COLUMNS as readonly string[]).indexOf(column)
    if (index >= 0) mask |= 1 << index
  }
  return mask
}

export function maskToColumns(mask: number): string[] {
  return LOG_LANG_COLUMNS.filter((_, index) => (mask >> index) & 1)
}

/** logs 表整行（只取用得到的列） */
export interface LogRow extends RowDataPacket {
  id: number
  type: string
  content: string
  content_hash: string
  [column: string]: unknown
}

export interface LogGroup {
  id: number
  type: string
  content: string
  texts: Record<string, string>
}

export function toGroup(row: LogRow): LogGroup {
  const texts: Record<string, string> = {}
  for (const column of LOG_LANG_COLUMNS) {
    const value = row[column]
    if (typeof value === 'string' && value) texts[column] = value
  }
  return { id: Number(row.id), type: String(row.type), content: String(row.content ?? ''), texts }
}

/** 读取候选分组：主内容命中 + 任一语言列命中（logs 表只有几千行，全列 OR 扫描可接受） */
export async function findLogGroups(
  pool: Pool,
  type: string,
  text: string,
  limit = 20,
): Promise<LogGroup[]> {
  const norm = normalizeLogText(text)
  if (!norm) return []
  const hash = logTextHash(norm)
  const ors = LOG_LANG_COLUMNS.map((column) => `\`${column}\` = ?`).join(' OR ')
  const params: unknown[] = [type, hash, ...LOG_LANG_COLUMNS.map(() => norm)]
  const [rows] = await pool.query<LogRow[]>(
    `SELECT * FROM \`logs\` WHERE \`type\` = ? AND (\`content_hash\` = ? OR ${ors}) LIMIT ${Number(limit)}`,
    params,
  )
  return (rows || []).map(toGroup)
}

/** 两个分组是否兼容（同一门语言要么为空、要么内容一致） */
export function groupsCompatible(left: LogGroup, right: LogGroup): boolean {
  for (const column of LOG_LANG_COLUMNS) {
    const a = left.texts[column]
    const b = right.texts[column]
    if (a && b && a !== b) return false
  }
  return true
}

/** 把 source 的语言文本补进 target（只填空缺），返回补写的语言列 */
export function mergeGroupTexts(target: LogGroup, source: LogGroup): string[] {
  const filled: string[] = []
  for (const column of LOG_LANG_COLUMNS) {
    const value = source.texts[column]
    if (value && !target.texts[column]) {
      target.texts[column] = value
      filled.push(column)
    }
  }
  return filled
}

/** 新建一行日志分组（content = 命中语言文本；中文优先由调用方保证） */
export async function insertLogGroup(pool: Pool, type: string, texts: Record<string, string>): Promise<number> {
  const content = texts.logs_zh || texts.logs_en || Object.values(texts)[0] || ''
  const columns = ['`type`', '`content`', '`content_hash`', ...LOG_LANG_COLUMNS.map((c) => `\`${c}\``)]
  const params = [type, content, logTextHash(content), ...LOG_LANG_COLUMNS.map((c) => texts[c] || null)]
  const [result] = await pool.query<import('mysql2/promise').ResultSetHeader>(
    `INSERT INTO \`logs\` (${columns.join(', ')}) VALUES (${columns.map(() => '?').join(', ')})`,
    params,
  )
  return Number(result.insertId)
}

/** 把若干语言文本写入已有分组（COALESCE，不覆盖已有值） */
export async function fillLogGroup(pool: Pool, id: number, texts: Record<string, string>): Promise<string[]> {
  const sets: string[] = []
  const params: unknown[] = []
  for (const column of LOG_LANG_COLUMNS) {
    const value = normalizeLogText(texts[column])
    if (!value) continue
    sets.push(`\`${column}\` = COALESCE(\`${column}\`, ?)`)
    params.push(value)
  }
  if (!sets.length) return []
  await pool.query(`UPDATE \`logs\` SET ${sets.join(', ')} WHERE \`id\` = ?`, [...params, id])
  return sets.map((s) => s.split('`')[1] || '')
}

/** 解析 roms.logs（[[模块ID,[条目ID,...],掩码],...]），容错处理历史两元素格式 */
export function decodeLogStructure(value: unknown): Array<{ moduleId: number; entryIds: number[]; mask: number }> {
  let parsed: unknown = value
  if (typeof value === 'string') {
    try {
      parsed = JSON.parse(value)
    } catch {
      return []
    }
  }
  if (!Array.isArray(parsed)) return []
  const out: Array<{ moduleId: number; entryIds: number[]; mask: number }> = []
  for (const item of parsed) {
    if (!Array.isArray(item) || item.length < 2) continue
    const moduleId = Number(item[0])
    const entryIds = Array.isArray(item[1]) ? item[1].map(Number).filter(Number.isFinite) : []
    const mask = item.length > 2 && typeof item[2] === 'number' ? item[2] : (1 << LOG_LANG_COLUMNS.length) - 1
    if (Number.isFinite(moduleId)) out.push({ moduleId, entryIds, mask })
  }
  return out
}

/** 按 id 批量取分组（导出 / 预览用） */
export async function loadLogGroups(pool: Pool, ids: number[]): Promise<Map<number, LogGroup>> {
  const map = new Map<number, LogGroup>()
  const unique = [...new Set(ids.filter((x) => Number.isFinite(x)))]
  for (let i = 0; i < unique.length; i += 500) {
    const chunk = unique.slice(i, i + 500)
    const [rows] = await pool.query<LogRow[]>(
      `SELECT * FROM \`logs\` WHERE \`id\` IN (${chunk.map(() => '?').join(',')})`,
      chunk,
    )
    for (const row of rows || []) map.set(Number(row.id), toGroup(row))
  }
  return map
}

/** roms.logs -> {语言列: {模块: [条目, ...]}}（与 Python 侧 build_localized 行为一致） */
export function buildLocalizedLogs(
  structure: Array<{ moduleId: number; entryIds: number[]; mask: number }>,
  groups: Map<number, LogGroup>,
): Record<string, Record<string, string[]>> {
  const result: Record<string, Record<string, string[]>> = {}
  for (const { moduleId, entryIds, mask } of structure) {
    const module = groups.get(moduleId)
    if (!module) continue
    const entries = entryIds.map((id) => groups.get(id)).filter((x): x is LogGroup => Boolean(x))
    for (let index = 0; index < LOG_LANG_COLUMNS.length; index += 1) {
      if (!((mask >> index) & 1)) continue
      const column = LOG_LANG_COLUMNS[index]!
      const moduleText = module.texts[column]
      if (!moduleText) continue
      const lines: string[] = []
      for (const entry of entries) {
        const text = entry.texts[column]
          || (column === 'logs_zh' ? '' : entry.texts.logs_en || entry.texts.logs_zh || '')
        if (text) lines.push(text)
      }
      const bucket = result[column] || (result[column] = {})
      bucket[moduleText] = (bucket[moduleText] || []).concat(lines)
    }
  }
  return result
}
