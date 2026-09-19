// 运营商显示名见 i18n.config.ts 的 `carriers` 词条（通过 useCarrierName() 读取），本文件只维护展示顺序。

// 运营商筛选按钮的展示顺序：中国三大运营商在前，其余按欧洲 → 拉美 → 其他地区聚类
export const CARRIER_ORDER = [
  'chinatelecom', 'chinamobile', 'chinaunicom',
  'orange', 'vf', 'h3g', 'tf', 'sf', 'ti', 'by',
  'cr', 'movistar', 'tc', 'mt', 'vc', 'gt', 'sb', 'as', 'dc',
]

// 按 CARRIER_ORDER 排序，未收录运营商排在末尾并按字母序
export const sortCarriers = (carriers: string[]): string[] =>
  [...carriers].sort((a, b) => {
    const ia = CARRIER_ORDER.indexOf(a)
    const ib = CARRIER_ORDER.indexOf(b)
    if (ia !== ib) return (ia < 0 ? CARRIER_ORDER.length : ia) - (ib < 0 ? CARRIER_ORDER.length : ib)
    return a.localeCompare(b)
  })
