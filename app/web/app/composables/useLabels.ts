/**
 * 区域 / 运营商显示名。
 *
 * 所有译名集中在 i18n.config.ts 的 `regions` / `carriers` 词条里（键即数据中的 region / carrier
 * 代号，见 data/api/v3/index.json），这两个函数只负责查词条并把未收录的代号回退为大写代号，
 * 这样新增语言时只需补 i18n.config.ts，无需改动页面。
 *
 * 排序（REGION_ORDER / CARRIER_ORDER）仍在 ~/utils/region.ts、~/utils/carrier.ts 中维护。
 */
export const useRegionName = () => {
  const { t, te } = useI18n()
  return (region?: string | null): string => {
    if (!region) return ''
    const key = `regions.${region}`
    return te(key) ? t(key) : String(region).toUpperCase()
  }
}

export const useCarrierName = () => {
  const { t, te } = useI18n()
  return (carrier?: string | null): string => {
    if (!carrier) return ''
    const key = `carriers.${carrier}`
    return te(key) ? t(key) : String(carrier).toUpperCase()
  }
}

/**
 * 分支显示名本地化（如「中国大陆正式版」「Europe Orange Carrier Edition」）。
 *
 * 数据里 branch.name 只有 zh/en，且存在两类特殊情况：
 * 1) 部分运营商分支 region 字段是 global，但名字是明确的国别运营商（韩国 KTF、日本 KDDI…），
 *    所以真实区域优先按英文名前缀推断，region 字段仅作兜底；
 * 2) 数据英文名有历史拼写（Carrier「Edtion」），统一组合后顺带修正。
 *
 * 所有名称都能拆成「区域 + 运营商 + 类型词」，三块均有全语言词条；中文界面直接用数据名。
 * 入参兼容设备详情页 branch 与 ROM 列表页的扁平 rom（{ name/branchName, region }）。
 */
export const useBranchName = () => {
  const { t, locale } = useI18n()
  const regionName = useRegionName()
  const carrierName = useCarrierName()

  // 仅含中日韩表意/假名时相邻部分不加分隔（中国本土+安定版）；其余一律空格（韩文、阿拉伯文、含拉丁词）
  const cjkOnly = (s: string) => /^[\u2e80-\u9fff\u3000-\u303f\u3040-\u30ff\uff00-\uffef]+$/.test(s)
  const sep = (a: string, b: string) => (cjkOnly(a) && cjkOnly(b) ? '' : ' ')
  const join = (...parts: string[]) =>
    parts.filter(Boolean).reduce((acc, p) => (acc ? acc + sep(acc, p) + p : p), '')

  return (b: {
    name?: { zh?: string; en?: string } | null
    region?: string | null
    ep?: string | number | null
  } | null | undefined): string => {
    if (!b) return ''
    const zh = b.name?.zh || ''
    const en = b.name?.en || zh
    if (!en) return regionName(b.region)
    if (locale.value.startsWith('zh')) return zh || en

    // 从英文名前缀推断真实区域（长前缀优先；命中失败才用 region 字段）
    const region = inferRegion(en) || b.region || ''

    // 1. 标准正式版 / 开发版
    if (STANDARD_STABLE_EN.has(en)) return join(regionName(region), t('stable'))
    if (en === 'China Dev' || en === 'Global Dev') return join(regionName(region), t('dev'))

    // 2. Alpha / Beta
    if (en === 'China Alpha') return join(regionName(region), t('alpha'))
    if (en === 'China Beta' || en === 'Singapore Beta') return join(regionName(region), t('beta'))
    if (en === 'Enhanced Experience Beta') return t('enhancedBeta')

    // 3. 推荐版（含中国电信 / 中国联通两种运营商推荐版）
    if (en === 'China Mainland Suggested Edition') return join(regionName(region), t('suggested'))
    if (en === 'China Telecom Suggested Edition') return join(carrierName('chinatelecom'), t('suggested'))
    if (en === 'China Unicom Suggested Edition') return join(carrierName('chinaunicom'), t('suggested'))
    if (en === 'Suggested Edition for Mi 1 Lite') return t('mi1LiteSuggested')

    // 4. 门店演示机
    if (en === 'China Mainland MiStore Demo' || en === 'Indonesia MiStore Demo')
      return join(regionName(region), t('demo'))

    // 5. 政企版：标准版 / 带客户代码的企业版（代码保留原文）
    if (en === 'China Mainland Enterprise Standard') return join(regionName(region), t('enterpriseStd'))
    const epMatch = /^China Mainland Enterprise\(([^)]+)\)$/.exec(en)
    if (epMatch) return `${join(regionName(region), t('enterprise'))} (${epMatch[1]})`

    // 6. DC 正式版：carriers.dc 的译文本身已含「Global/글로벌」，不再加区域
    if (en === 'Global DC Stable') return join(carrierName('dc'), t('stable'))

    // 7. 原生安卓
    if (en === 'Stock Android') return t('stockAndroid')

    // 8. Android 开发者预览版：cn 无后缀，global 追加区域名（数据 zh 名以「国际版」区分）
    if (en === 'Android Developer Preview Global') {
      const base = join(t('android'), t('devPreview'))
      return zh.includes('国际版') ? join(base, regionName('global')) : base
    }

    // 9. 运营商定制版（数据有 Carrier Edition / 历史拼写 Edtion 两种结尾）
    if (/Carrier Edi?tion$/.test(en)) {
      const key = detectCarrier(en)
      if (key) return join(regionName(region), carrierName(key), t('carrierEdition'))
    }

    // 未归类的新名称：英文兜底，不显示空串
    return en
  }
}

// 英文名前缀 → region 代号（覆盖全库分支名的开头写法，长前缀必须排在前面）
const EN_REGION_PREFIX: [string, string][] = [
  ['China Mainland', 'cn'],
  ['China Taiwan', 'tw'],
  ['China Hong Kong', 'hk'],
  ['Latin America', 'lm'],
  ['South Africa', 'za'],
  ['South Korea', 'kr'],
  ['Europe', 'eea'],
  ['Global', 'global'],
  ['Russia', 'ru'],
  ['India', 'in'],
  ['Indonesia', 'id'],
  ['Turkey', 'tr'],
  ['Japan', 'jp'],
  ['Malaysia', 'my'],
  ['Singapore', 'sg'],
  ['Thailand', 'th'],
  ['Mexico', 'mx'],
  ['Chile', 'cl'],
  ['Guatemala', 'gt'],
]

function inferRegion(en: string): string {
  for (const [prefix, code] of EN_REGION_PREFIX) {
    if (en.startsWith(prefix)) return code
  }
  return ''
}

// 运营商英文名特征串 → carrier 词条键（长串优先；特征均来自全库英文分支名）
const CARRIER_TOKENS: [string, string][] = [
  ['Bouygues Telecom', 'by'],
  ['SK Telecom', 'skt'],
  ['LG U+', 'lgu'],
  ['Altice(SFR)', 'sf'],
  ['ThreeHK', 'h3g'],
  ['Telefonica', 'tf'],
  ['Vodacom', 'vc'],
  ['Vodafone', 'vf'],
  ['Movistar', 'movistar'],
  ['SoftBank', 'sb'],
  ['Orange', 'orange'],
  ['Entel', 'entel'],
  ['AT&T', 'att'],
  ['Tigo', 'gt'],
  ['Telcel', 'tc'],
  ['Claro', 'cr'],
  ['KDDI', 'kddi'],
  ['KTF', 'ktf'],
  ['MTN', 'mt'],
  ['TIM', 'ti'],
  ['AIS', 'as'],
]

function detectCarrier(en: string): string {
  for (const [token, key] of CARRIER_TOKENS) {
    if (en.includes(token)) return key
  }
  return ''
}

// 全库标准英文正式版分支名（data/api/v3/devices/*.json 汇总；新增标准分支时补这里）
const STANDARD_STABLE_EN = new Set([
  'China Mainland Stable',
  'China Taiwan Stable',
  'China Hong Kong Stable',
  'Global Stable',
  'Europe EEA Stable',
  'Russia Stable',
  'India Stable',
  'Indonesia Stable',
  'Turkey Stable',
  'Japan Stable',
  'South Korea Stable',
  'Malaysia Stable',
  'Singapore Stable',
])

