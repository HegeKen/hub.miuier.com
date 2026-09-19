// 区域显示名见 i18n.config.ts 的 `regions` 词条（通过 useRegionName() 读取），本文件只维护顺序与归类。

// 中国大陆 / 港澳台，用于区分分支指示点颜色
export const CHINA_REGIONS = ['cn', 'tw', 'hk', 'mo']

// 区域筛选按钮的展示顺序（未收录区域排在末尾并按字母序）
export const REGION_ORDER = [
  'cn', 'tw', 'hk', 'mo', 'global', 'eea', 'ru', 'in', 'id', 'tr',
  'jp', 'kr', 'sg', 'my', 'th', 'mx', 'lm', 'cl', 'za', 'gt',
]

export const isChinaRegion = (region: string): boolean => CHINA_REGIONS.includes(region)

// 按 REGION_ORDER 排序，未收录区域排在末尾
export const sortRegions = (regions: string[]): string[] =>
  [...regions].sort((a, b) => {
    const ia = REGION_ORDER.indexOf(a)
    const ib = REGION_ORDER.indexOf(b)
    if (ia !== ib) return (ia < 0 ? REGION_ORDER.length : ia) - (ib < 0 ? REGION_ORDER.length : ib)
    return a.localeCompare(b)
  })
