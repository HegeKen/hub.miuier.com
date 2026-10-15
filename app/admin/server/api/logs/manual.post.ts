import { createError, readBody } from 'h3'
import { getPool } from '../../utils/db'
import {
  LOG_LANG_COLUMNS,
  LOG_LANG_LABELS,
  findLogGroups,
  groupsCompatible,
  mergeGroupTexts,
  insertLogGroup,
  fillLogGroup,
  isLogLang,
  normalizeLogText,
  type LogGroup,
} from '../../utils/logs'

/**
 * 手动录入更新日志。
 *
 * 请求体：
 *   { text: string, type?: 'log'|'module', lang?: string, preview?: boolean, limit?: number }
 *   - text    多行文本，按换行符切分，每行一条日志（自动去空行 / 去重 / 去首尾空白）
 *   - type    日志类型，默认 log（条目）；module 表示模块名
 *   - lang    输入文本所属语言列，默认 logs_zh（其余语言可自行选择）
 *   - preview true 时只解析比对、不写库
 *
 * 「反推完善其他语言版本」：每一行都会在 logs 表里比对同一个日志的内容——
 * 主内容相同、或任意一门语言的文本相同即视为同一条日志；命中多个分组时，
 * 把互相兼容的分组的语言文本合并到覆盖最全的那一行，从而用库里已有数据
 * 补齐其他语言的译文。
 */
export default defineEventHandler(async (event) => {
  const body = await readBody<{
    text?: string
    type?: string
    lang?: string
    preview?: boolean
    limit?: number
  }>(event)

  const raw = String(body?.text ?? '')
  const type = body?.type === 'module' ? 'module' : 'log'
  const lang = isLogLang(body?.lang) ? body!.lang! : 'logs_zh'
  const preview = Boolean(body?.preview)
  const limit = Math.min(Math.max(Number(body?.limit) || 2000, 1), 20000)

  const lines: string[] = []
  const seen = new Set<string>()
  for (const piece of raw.split(/\r?\n/)) {
    const text = normalizeLogText(piece)
    if (!text || seen.has(text)) continue
    seen.add(text)
    lines.push(text)
    if (lines.length >= limit) break
  }

  if (!lines.length) {
    throw createError({ statusCode: 400, message: '没有可录入的内容（按换行符切分后为空）' })
  }

  const pool = getPool()
  const items: Array<{
    text: string
    status: 'new' | 'existing' | 'merged'
    groupId: number | null
    languages: string[]
    filled: string[]
  }> = []

  let created = 0
  let existing = 0
  let merged = 0
  let filledTotal = 0

  for (const text of lines) {
    const candidates = await findLogGroups(pool, type, text)
    // 与输入文本所属语言列一致的候选优先（避免把同名不同语言的日志混在一起）
    const exact = candidates.filter((c) => c.texts[lang] === text)
    const pool2 = (exact.length ? exact : candidates).slice()

    if (!pool2.length) {
      if (preview) {
        items.push({ text, status: 'new', groupId: null, languages: [lang], filled: [] })
      } else {
        const id = await insertLogGroup(pool, type, { [lang]: text })
        items.push({ text, status: 'new', groupId: id, languages: [lang], filled: [] })
      }
      created += 1
      continue
    }

    // 覆盖最全的分组作为合并目标
    pool2.sort((a, b) => Object.keys(b.texts).length - Object.keys(a.texts).length)
    const target: LogGroup = { ...pool2[0]!, texts: { ...pool2[0]!.texts } }
    const filled: string[] = []
    const compatibleIds = [target.id]

    for (const candidate of pool2.slice(1)) {
      if (candidate.id === target.id) continue
      if (!groupsCompatible(target, candidate)) continue
      compatibleIds.push(candidate.id)
      filled.push(...mergeGroupTexts(target, candidate))
    }
    // 输入本身所属语言若目标还没有，补上（例如库里的同一条日志缺这门语言）
    if (!target.texts[lang]) {
      target.texts[lang] = text
      filled.push(lang)
    }

    const languages = Object.keys(target.texts)
    if (preview) {
      items.push({
        text,
        status: filled.length ? 'merged' : 'existing',
        groupId: target.id,
        languages,
        filled: [...new Set(filled)],
      })
    } else {
      // 目标行补齐缺失语言，同时把所有兼容的重复行也补全（用库中已有数据反推完善）
      const written = new Set<string>()
      for (const id of compatibleIds) {
        for (const column of await fillLogGroup(pool, id, target.texts)) written.add(column)
      }
      items.push({
        text,
        status: filled.length ? 'merged' : 'existing',
        groupId: target.id,
        languages,
        filled: [...(written.size ? written : new Set(filled))],
      })
    }

    if (filled.length) merged += 1
    else existing += 1
    filledTotal += filled.length
  }

  return {
    ok: true,
    preview,
    type,
    lang,
    langLabel: LOG_LANG_LABELS[lang] || lang,
    languages: LOG_LANG_COLUMNS.map((c) => ({ column: c, label: LOG_LANG_LABELS[c] || c })),
    summary: {
      total: lines.length,
      created,
      existing,
      merged,
      filledLanguages: filledTotal,
    },
    items,
  }
})
