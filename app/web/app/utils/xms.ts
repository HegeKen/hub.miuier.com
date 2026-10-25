// XMS（SOTA 系统应用更新）页面的展示工具。
//
// 数据里只有 zh / en 两列（与 sitelog / tools 一致）：中文（含繁中）取 zh，
// 其余语言取 en，缺失时互相兜底。

export interface XmsText {
  zh?: string
  en?: string
  [key: string]: string | undefined
}

// 站点 locale -> 数据里的语言键（与 logs 表列名对齐：logs_zh_tw -> zh_tw、logs_in -> in）
const XMS_LANG_KEYS: Record<string, string> = {
  'zh-cn': 'zh',
  zh: 'zh',
  'zh-tw': 'zh_tw',
  'zh-hk': 'zh_tw',
  'en-us': 'en',
  en: 'en',
  hi: 'in',
}

/** 按当前语言取文案：当前语种 -> 英文 -> 中文（XMS 名称与日志都可能是多语言对象） */
export const pickXmsText = (item: XmsText | null | undefined, locale: string): string => {
  if (!item) return ''
  const code = String(locale || '').toLowerCase()
  const key = XMS_LANG_KEYS[code] || XMS_LANG_KEYS[code.split('-')[0] ?? ''] || (code.split('-')[0] ?? '')
  return item[key] || item.en || item.zh || ''
}

/** 字节数 -> 人类可读体积（空值返回空串） */
export const formatBytes = (size?: number | null): string => {
  if (!size || size <= 0) return ''
  const units = ['B', 'KB', 'MB', 'GB']
  let value = size
  let unit = 0
  while (value >= 1024 && unit < units.length - 1) {
    value /= 1024
    unit += 1
  }
  return `${value >= 100 || unit === 0 ? Math.round(value) : value.toFixed(1)} ${units[unit]}`
}

/**
 * XMS 版本号首字母 → 对应系统大版本（接口约定：C 适配 OS3、D 适配 OS4……）。
 * 导出数据里已有 os 字段，这里作为旧数据 / 缺字段时的兜底。
 */
export const xmsOsOf = (version?: string | null): string => {
  const text = String(version || '').trim()
  const first = text.charAt(0).toUpperCase()
  if (!/^[A-Z]$/.test(first)) return ''
  const major = first.charCodeAt(0) - 64
  return major >= 1 ? `OS${major}` : ''
}

/**
 * 从 APK 文件名里提取更适合展示的版本号。
 * 例：cleanmaster-1.0.3.8_260629_cn_release-universal-2d1f13b229.apk → 1.0.3.8_260629
 * 没有日期段时退回纯数字版本（contacts-17.20.10.50-cn-universal-… → 17.20.10.50）。
 */
export function xmsFileVersion(file?: string | null, fallback = ''): string {
  const name = String(file || '').trim()
  if (!name) return fallback
  const withDate = name.match(/(\d+(?:\.\d+)+_\d+)/)
  if (withDate?.[1]) return withDate[1]
  const plain = name.match(/(\d+(?:\.\d+)+)/)
  return plain?.[1] || fallback
}
