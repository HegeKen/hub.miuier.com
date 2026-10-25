// XMS（SOTA 系统应用更新）页面的展示工具。
//
// 数据里只有 zh / en 两列（与 sitelog / tools 一致）：中文（含繁中）取 zh，
// 其余语言取 en，缺失时互相兜底。

export interface XmsText {
  zh?: string
  en?: string
}

/** 按当前语言取 zh / en 文案 */
export const pickXmsText = (item: XmsText | null | undefined, locale: string): string => {
  if (!item) return ''
  return (locale.startsWith('zh') ? item.zh : item.en) || item.en || item.zh || ''
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
