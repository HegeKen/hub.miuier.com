// data/api/v3/logs/<device>/[region/]<version>.json 中的更新日志键 ↔ 站点 locale。
// 键名与 data/scripts/miroms/constants.py 的 CHANGELOG_LOCALES 一一对应，
// 新增语种时两处一起改（导出侧由 exporters.py 自动补齐键）。
const LOG_KEYS: Record<string, string> = {
  'zh-cn': 'logs_zh',
  zh: 'logs_zh',
  'zh-tw': 'logs_zh_tw',
  'zh-hk': 'logs_zh_tw',
  'en-us': 'logs_en',
  en: 'logs_en',
  ja: 'logs_ja',
  ko: 'logs_ko',
  ru: 'logs_ru',
  uk: 'logs_uk',
  pl: 'logs_pl',
  de: 'logs_de',
  fr: 'logs_fr',
  it: 'logs_it',
  es: 'logs_es',
  pt: 'logs_pt',
  tr: 'logs_tr',
  id: 'logs_id',
  vi: 'logs_vi',
  th: 'logs_th',
  ar: 'logs_ar',
  hi: 'logs_in',
  ug: 'logs_ug',
  bo: 'logs_bo',
}

// 站点 locale → 日志键；未知 locale 取主语言标签，仍未知则回落英文
export const logKeyFor = (locale: string): string => {
  const code = String(locale || '').toLowerCase()
  if (LOG_KEYS[code]) return LOG_KEYS[code]
  return LOG_KEYS[code.split('-')[0] ?? ''] || 'logs_en'
}

// 读取顺序：当前语言 → 英文 → 简体中文（早期数据只有 logs_zh / logs_en）
export const pickLogs = (raw: Record<string, unknown> | null | undefined, locale: string) => {
  if (!raw) return null
  for (const key of [logKeyFor(locale), 'logs_en', 'logs_zh']) {
    const value = raw[key]
    if (value && typeof value === 'object' && Object.keys(value).length > 0) {
      return value
    }
  }
  return null
}
