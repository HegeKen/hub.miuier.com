import { createHash } from 'node:crypto'
import { getPool } from './db'

/**
 * XMS 应用名 / 日志的多语言补全。
 *
 * `logs` 表的 module 行存的是官方界面模块名（如「手机管家」）及其 21 个语种译文，
 * XMS 的应用名正好对得上，用中文（或英文）原文去匹配即可补齐其它语言。
 */

/** 与 data/scripts/miroms/constants.py 的 CHANGELOG_LOCALES 对齐（logs_<key> 列） */
export const LOG_LANG_KEYS = [
  'zh', 'zh_tw', 'en', 'ja', 'ko', 'ru', 'uk', 'pl', 'de', 'fr', 'it',
  'es', 'pt', 'tr', 'id', 'vi', 'th', 'ar', 'in', 'ug', 'bo',
] as const

export type LogLangKey = (typeof LOG_LANG_KEYS)[number]
export type Translations = Partial<Record<LogLangKey, string>>

const COLUMNS_SQL = LOG_LANG_KEYS.map((key) => `\`logs_${key}\``).join(', ')

function normalizeRow(row: Record<string, unknown>): Translations {
  const out: Translations = {}
  for (const key of LOG_LANG_KEYS) {
    const value = String(row[`logs_${key}`] ?? '').trim()
    if (value) out[key] = value
  }
  return out
}

/**
 * 按中文名（优先）或英文名，在 logs 表找 type='module' 的多语言译文。
 * 命中返回 { zh, zh_tw, en, ja, ... }，未命中返回空对象。
 */
export async function moduleTranslations(name: string, nameEn: string): Promise<Translations> {
  const zh = String(name || '').trim()
  const en = String(nameEn || '').trim()
  if (!zh && !en) return {}

  const pool = getPool()
  const lookup = async (column: 'content' | 'logs_en', value: string) => {
    const [rows] = await pool.query(
      `SELECT ${COLUMNS_SQL} FROM \`logs\` WHERE \`type\` = 'module' AND \`${column}\` = ? LIMIT 1`,
      [value],
    ) as [Array<Record<string, unknown>>, unknown]
    return rows?.[0] || null
  }

  let row: Record<string, unknown> | null = null
  if (zh) row = await lookup('content', zh)
  if (!row && en) row = await lookup('logs_en', en)
  return row ? normalizeRow(row) : {}
}

/**
 * logs 的 module 行里没有该应用名时，先写一条占位（zh / en 两列），
 * 后续可以在「日志录入」里继续补全其它语种；已存在时顺手补空的英文列。
 */
export async function ensureModuleLog(
  name: string,
  nameEn = '',
): Promise<{ created: boolean; id: number | null }> {
  const zh = String(name || '').trim()
  let en = String(nameEn || '').trim()
  if (en && en === zh) en = ''
  if (!zh) return { created: false, id: null }

  const pool = getPool()
  const [rows] = await pool.query(
    "SELECT `id`, `logs_en` FROM `logs` WHERE `type` = 'module' "
    + "AND (`content` = ? OR `logs_zh` = ? OR (`logs_en` <> '' AND `logs_en` = ?)) "
    + 'ORDER BY `id` LIMIT 1',
    [zh, zh, en || zh],
  ) as [Array<Record<string, unknown>>, unknown]

  if (rows?.length) {
    const row = rows[0]!
    if (en && !String(row.logs_en ?? '').trim()) {
      await pool.query('UPDATE `logs` SET `logs_en` = ? WHERE `id` = ?', [en, Number(row.id)])
    }
    return { created: false, id: Number(row.id) }
  }

  // MySQL 9 起已移除 MD5()，在 Node 侧算好再写入（与 logs 其它行的 hex 一致）
  const contentHash = createHash('md5').update(zh, 'utf8').digest('hex')
  const [result] = await pool.query(
    "INSERT INTO `logs` (`type`, `content`, `content_hash`, `logs_zh`, `logs_en`) "
    + "VALUES ('module', ?, ?, ?, ?)",
    [zh, contentHash, zh, en || null],
  ) as [{ insertId: number }, unknown]
  return { created: true, id: Number(result?.insertId || 0) }
}
