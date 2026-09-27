interface FetchOptions {
  timeout?: number
}

/** 高速下载请求所需的机型/分支/版本信息 */
export interface HighSpeedParams {
  /** 分支代号（branch.id） */
  code: string
  /** 机型代号 */
  device: string
  /** 分支标记 F / X */
  branchTag: string
  region: string
  zone: number
  /** Android 大版本，如 14 */
  android: string
  version: string
}

/** /api/ota/highspeed 的响应：available 成功，其余为各种如实反馈的失败状态 */
export type HighSpeedResult =
  | { status: 'available'; version: string; links: { mirror: string; url: string }[] }
  | { status: 'outdated'; latestVersion: string }
  | { status: 'unsigned' }
  | { status: 'notfound' }
  | { status: 'failed'; message: string }

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

  // 发布日期索引：index.json 提供日期范围，<year>.json 提供该年按日期分组的 ROM
  const buildReleasesIndexUrl = (): string => {
    return buildUrl('/v3/releases/index.json')
  }

  const buildReleasesUrl = (year: string): string => {
    return buildUrl(`/v3/releases/${year}.json`)
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

  // 高速下载：始终同源走 Nitro（开发为 Node 路由，生产为 Cloudflare Pages Function），
  // 由服务端代理小米 OTA 接口。签名直链有实效性，不能缓存。
  const fetchHighSpeed = async (params: HighSpeedParams): Promise<HighSpeedResult> => {
    return await $fetch<HighSpeedResult>('/api/ota/highspeed', {
      method: 'POST',
      body: params,
    })
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
    buildReleasesIndexUrl,
    buildReleasesUrl,
    buildDownloadLink,
    buildChangelogUrl,
    fetchHighSpeed,
  }
}
