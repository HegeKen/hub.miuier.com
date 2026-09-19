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
