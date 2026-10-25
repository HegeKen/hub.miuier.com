/**
 * XMS（SOTA 系统应用更新）APK 下载链接代理。
 *
 * 小米 getXmsVer 接口返回的 APK 直链带 `?t=&s=` 时效签名（未签名直链 403），
 * 不能提前写进静态 JSON；这里在服务端按记录重新请求一次，换取当下的签名链接
 * （与 ROM 高速下载 /api/ota/highspeed 同一策略）。
 *
 * 部署在 Cloudflare Pages Functions 上即对应一条 Pages Function；
 * 本路由只转发很小的元数据，APK 本体不经过 Cloudflare。
 */

const XMS_URL = 'https://update.miui.com/api/v3/xms/getXmsVer'

interface XmsDownloadBody {
  /** 应用包名，对应 pkgs[].pkg */
  pkg: string
  /** 分支代号（branch.id），对应表单 d */
  code: string
  /** 机型代号，对应表单 p */
  device: string
  region: string
  zone: number
  /** Android 大版本，如 16.0，对应表单 c（去掉 .0） */
  android: string
  /** 该 XMS 所基于的系统版本，对应表单 rv */
  romVersion: string
  /** XMS 版本号（如 C11），对应表单 lsv */
  xmsVersion: string
}

interface XmsApkItem {
  packName?: string
  lastVerCode?: string | number
  fileName?: string
  size?: string | number
  md5?: string
  sha256?: string
  downloadUrls?: string[]
}

/** 表单里接口实际读取的区域写法；其余区域直接用数据里的 region 值 */
const REGION_FIELD_OVERRIDES: Record<string, string> = { cn: 'CN', global: 'GL' }

// 输入白名单：只转发到固定的小米地址，严格校验避免被当作通用代理滥用
const PKG_RE = /^[A-Za-z][A-Za-z0-9._]*$/
const CODE_RE = /^[a-z][a-z0-9_]*$/
const DEVICE_RE = /^[a-z][a-z0-9]*$/
const VERSION_RE = /^[A-Za-z0-9][A-Za-z0-9.]*$/
const ANDROID_RE = /^[0-9]{1,2}(\.0)?$/
const REGION_RE = /^[a-z]{0,6}$/
const XMS_VERSION_RE = /^[A-Za-z0-9][A-Za-z0-9._-]*$/

/** 只接受小米自家 CDN 的 https 链接，避免把任意 URL 透传给前端 */
const isAllowedDownloadUrl = (value: string): boolean => {
  try {
    const url = new URL(value)
    return url.protocol === 'https:' && /(^|\.)miui\.com$/i.test(url.hostname)
  } catch {
    return false
  }
}

/** 随机 request id（getXmsVer 的 n 参数）；workerd 与 Node 都有 crypto.randomUUID */
const requestId = (): string => {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID()
  }
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (ch) => {
    const r = Math.floor(Math.random() * 16)
    const v = ch === 'x' ? r : (r & 0x3) | 0x8
    return v.toString(16)
  })
}

export default defineEventHandler(async (event) => {
  setResponseHeader(event, 'cache-control', 'no-store')

  const body = await readBody<XmsDownloadBody>(event)

  if (
    !body ||
    !PKG_RE.test(body.pkg || '') ||
    !CODE_RE.test(body.code || '') ||
    !DEVICE_RE.test(body.device || '') ||
    !VERSION_RE.test(body.romVersion || '') ||
    !ANDROID_RE.test(body.android || '') ||
    !REGION_RE.test(body.region || '') ||
    !XMS_VERSION_RE.test(body.xmsVersion || '') ||
    typeof body.zone !== 'number'
  ) {
    throw createError({ statusCode: 400, statusMessage: 'Invalid request' })
  }

  const region = body.region.toLowerCase()
  const android = String(body.android).replace(/\.0$/, '')

  // 字段与 miroms/xms.py 的 _fetch_apks 保持一致（该接口不要求 HyperOSForm 的全部固定字段）
  const payload = {
    b: 'F',
    c: android,
    d: body.code,
    f: '1',
    l: region === 'cn' ? 'zh_CN' : 'en_US',
    lsv: body.xmsVersion,
    r: REGION_FIELD_OVERRIDES[region] || body.region,
    id: '',
    rv: body.romVersion,
    pkgs: [{ pkg: body.pkg, pkgVer: '1' }],
  }

  try {
    const encrypted = await miuiEncrypt(JSON.stringify(payload))
    const query = [
      `q=${encrypted}`,
      's=1',
      't=',
      `n=${requestId()}`,
      `ts=${Date.now()}`,
    ].join('&')

    const raw = await $fetch<string>(XMS_URL, {
      method: 'POST',
      headers: {
        // 与 miroms/network.py 的请求头保持一致
        'User-Agent': 'Dalvik/2.1.0 (Linux; U; Android 13; MI 9 Build/TKQ1.220829.002)',
        Cookie: 'serviceToken=;',
        // 必须显式声明；application/json 会被接口回 406
        Accept: '*/*',
        'Content-Type': 'application/x-www-form-urlencoded',
      },
      body: query,
      responseType: 'text',
      timeout: 10000,
    })

    const text = String(raw).trim()

    // 出错时接口可能回明文 JSON（含 code 字段），不是密文
    if (!text || text.startsWith('{')) {
      return { status: 'notfound' }
    }

    const cipherPart = (text.split('q=')[0] || '').trim()
    if (!cipherPart) {
      return { status: 'notfound' }
    }

    const parsed = JSON.parse(await miuiDecrypt(cipherPart)) as { apkLists?: XmsApkItem[] }
    const apk = (parsed.apkLists || []).find((item) => item.packName === body.pkg)
    if (!apk) {
      return { status: 'notfound' }
    }

    const urls = [...new Set((apk.downloadUrls || []).filter(isAllowedDownloadUrl))]
    if (!urls.length) {
      return { status: 'unsigned' }
    }

    return {
      status: 'available',
      version: String(apk.lastVerCode || ''),
      fileName: String(apk.fileName || ''),
      size: Number(apk.size || 0) || 0,
      md5: String(apk.md5 || ''),
      sha256: String(apk.sha256 || ''),
      urls,
    }
  } catch (err) {
    const message = err instanceof Error ? err.message : 'unknown error'
    return { status: 'failed', message }
  }
})
