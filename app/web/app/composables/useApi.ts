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

  const buildDevicesIndexUrl = (): string => {
    return buildUrl('/v3/index.json')
  }

  const buildStatsUrl = (): string => {
    return buildUrl('/v3/stats.json')
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
    buildDevicesIndexUrl,
    buildStatsUrl,
    buildRomsIndexUrl,
    buildRomsUrl,
    buildDownloadLink,
    buildChangelogUrl,
  }
}
