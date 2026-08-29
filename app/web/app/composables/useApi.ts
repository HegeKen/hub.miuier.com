interface FetchOptions {
  timeout?: number
}

export function useApi() {
  const config = useRuntimeConfig()
  const isDev = import.meta.dev

  const buildUrl = (path: string): string => {
    if (isDev) {
      return `/api/data${path}`
    }
    return `${config.public.apiBaseUrl}${path}`
  }

  const buildDeviceUrl = (codename: string): string => {
    return buildUrl(`/v3/devices/${codename}.json`)
  }

  // 图片：开发环境走本地代理（data/images），生产环境由 api.miuier.com 站点根提供
  const buildImageUrl = (file: string): string => {
    if (isDev) {
      return `/api/data/images/${file}`
    }
    const base = config.public.apiBaseUrl.replace(/\/api\/?$/, '')
    return `${base}/images/${file}`
  }

  // 机型照片：<codename>.png
  const buildDeviceImageUrl = (codename: string): string => {
    return buildImageUrl(`${codename}.png`)
  }

  // 品牌默认图：无机型照片时按品牌展示，Xiaomi→mi.svg，POCO→POCO.png，REDMI→REDMI.png
  const buildBrandImageUrl = (brand: string): string => {
    const lower = (brand || '').toLowerCase()
    const file = lower === 'poco' ? 'POCO.png' : lower === 'redmi' ? 'REDMI.png' : 'mi.svg'
    return buildImageUrl(file)
  }

  const buildDevicesIndexUrl = (): string => {
    return buildUrl('/v3/index.json')
  }

  const buildStatsUrl = (): string => {
    return buildUrl('/v3/stats.json')
  }

  const buildStatisticsUrl = (): string => {
    return buildUrl('/v3/statistics.json')
  }

  const buildRomsIndexUrl = (): string => {
    return buildUrl('/v3/roms/index.json')
  }

  const buildRomsUrl = (os: string): string => {
    return buildUrl(`/v3/roms/${os}.json`)
  }

  const buildDownloadLink = (version: string, filename: string): string => {
    return `https://bkt-sgp-miui-ota-update-alisgp.oss-ap-southeast-1.aliyuncs.com/${version}/${filename}`
  }

  const buildChangelogUrl = (device: string, region: string, version: string): string => {
    if (region) {
      return buildUrl(`/v3/logs/${device}/${region}/${version}.json`)
    }
    return buildUrl(`/v3/logs/${device}/${version}.json`)
  }

  return {
    buildUrl,
    buildDeviceUrl,
    buildDeviceImageUrl,
    buildBrandImageUrl,
    buildDevicesIndexUrl,
    buildStatsUrl,
    buildStatisticsUrl,
    buildRomsIndexUrl,
    buildRomsUrl,
    buildDownloadLink,
    buildChangelogUrl,
  }
}
