// region 值 → 显示名（zh/en），未收录值回退到大写代号
export const REGION_LABELS: Record<string, { zh: string; en: string }> = {
  cn: { zh: '中国大陆', en: 'China Mainland' },
  tw: { zh: '中国台湾', en: 'Taiwan，China' },
  global: { zh: '国际', en: 'Global' },
  eea: { zh: '欧洲经济区', en: 'European Economic Area' },
  ru: { zh: '俄罗斯', en: 'Russia' },
  in: { zh: '印度', en: 'India' },
  tr: { zh: '土耳其', en: 'Turkey' },
  id: { zh: '印度尼西亚', en: 'Indonesia' },
  jp: { zh: '日本', en: 'Japan' },
  kr: { zh: '韩国', en: 'Korea' },
  mx: { zh: '墨西哥', en: 'Mexico' },
  lm: { zh: '拉美', en: 'Latin America' },
  th: { zh: '泰国', en: 'Thailand' },
  hk: { zh: '中国香港', en: 'Hong Kong，China' },
  sg: { zh: '新加坡', en: 'Singapore' },
  my: { zh: '马来西亚', en: 'Malaysia' },
  cl: { zh: '智利', en: 'Chile' },
  za: { zh: '南非', en: 'South Africa' },
  gt: { zh: '危地马拉', en: 'Guatemala' },
}

// 中国大陆 / 港澳台，用于区分分支指示点颜色
export const CHINA_REGIONS = ['cn', 'tw', 'hk', 'mo']

// 区域筛选按钮的展示顺序（未收录区域排在末尾并按字母序）
export const REGION_ORDER = [
  'cn', 'tw', 'hk', 'global', 'eea', 'ru', 'in', 'id', 'tr',
  'jp', 'kr', 'sg', 'my', 'th', 'mx', 'lm', 'cl', 'za', 'gt',
]

export const regionLabel = (region: string, locale: string): string => {
  const labels = REGION_LABELS[region]
  if (!labels) return String(region || '').toUpperCase()
  return locale.startsWith('zh') ? labels.zh : labels.en
}

export const isChinaRegion = (region: string): boolean => CHINA_REGIONS.includes(region)

// 按 REGION_ORDER 排序，未收录区域排在末尾
export const sortRegions = (regions: string[]): string[] =>
  [...regions].sort((a, b) => {
    const ia = REGION_ORDER.indexOf(a)
    const ib = REGION_ORDER.indexOf(b)
    if (ia !== ib) return (ia < 0 ? REGION_ORDER.length : ia) - (ib < 0 ? REGION_ORDER.length : ib)
    return a.localeCompare(b)
  })
