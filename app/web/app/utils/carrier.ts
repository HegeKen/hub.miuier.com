// carrier 值 → 显示名（zh/en），未收录值回退到大写代号
export const CARRIER_LABELS: Record<string, { zh: string; en: string }> = {
  chinatelecom: { zh: '中国电信', en: 'China Telecom' },
  chinamobile: { zh: '中国移动', en: 'China Mobile' },
  chinaunicom: { zh: '中国联通', en: 'China Unicom' },
  orange: { zh: '法国电信（Orange）', en: 'Orange' },
  vf: { zh: '沃达丰（Vodafone）', en: 'Vodafone' },
  h3g: { zh: '3 香港（ThreeHK）', en: 'ThreeHK' },
  tf: { zh: '西班牙电信（Telefonica）', en: 'Telefonica' },
  sf: { zh: '法国SFR（Altice）', en: 'Altice (SFR)' },
  ti: { zh: '意大利电信（TIM）', en: 'TIM' },
  by: { zh: '布依格（Bouygues）', en: 'Bouygues Telecom' },
  cr: { zh: '克拉罗（Claro）', en: 'Claro' },
  movistar: { zh: '移动之星（Movistar）', en: 'Movistar' },
  tc: { zh: '泰尔塞尔（Telcel）', en: 'Telcel' },
  mt: { zh: '南非 MTN', en: 'MTN' },
  vc: { zh: '沃达康（Vodacom）', en: 'Vodacom' },
  gt: { zh: '危地马拉 Tigo', en: 'Tigo' },
  sb: { zh: '软银（SoftBank）', en: 'SoftBank' },
  as: { zh: '泰国 AIS', en: 'AIS' },
}

// 运营商筛选按钮的展示顺序：中国三大运营商在前，其余按欧洲 → 拉美 → 其他地区聚类
export const CARRIER_ORDER = [
  'chinatelecom', 'chinamobile', 'chinaunicom',
  'orange', 'vf', 'h3g', 'tf', 'sf', 'ti', 'by',
  'cr', 'movistar', 'tc', 'mt', 'vc', 'gt', 'sb', 'as',
]

export const carrierLabel = (carrier: string, locale: string): string => {
  const labels = CARRIER_LABELS[carrier]
  if (!labels) return String(carrier || '').toUpperCase()
  return locale.startsWith('zh') ? labels.zh : labels.en
}

// 按 CARRIER_ORDER 排序，未收录运营商排在末尾并按字母序
export const sortCarriers = (carriers: string[]): string[] =>
  [...carriers].sort((a, b) => {
    const ia = CARRIER_ORDER.indexOf(a)
    const ib = CARRIER_ORDER.indexOf(b)
    if (ia !== ib) return (ia < 0 ? CARRIER_ORDER.length : ia) - (ib < 0 ? CARRIER_ORDER.length : ib)
    return a.localeCompare(b)
  })
