/**
 * 高速下载链接（superota / ultimateota）代理。
 *
 * 浏览器无法直接请求小米 OTA 接口（CORS），由本路由在服务端转发：
 * 构造 HyperOSForm → AES-128-CBC 加密 → POST update.miui.com →
 * 用 MirrorList 与带签名的 LatestRom.filename 拼出直链。
 *
 * 部署在 Cloudflare Pages Functions 上即对应一条 Pages Function；
 * 本路由只转发体积很小的 OTA 元数据，GB 级刷机包不经过 Cloudflare。
 */

const OTA_URL = 'https://update.miui.com/updates/miotaV3.php'

interface HighSpeedRequestBody {
  /** 分支代号（branch.id，如 mist_eea_global），对应表单 d */
  code: string
  /** 机型代号，对应表单 p */
  device: string
  /** 分支标记 F / X，对应表单 b */
  branchTag: string
  region: string
  zone: number
  /** Android 大版本，如 14，对应表单 c */
  android: string
  /** 版本号，对应表单 v / ov / options.cv */
  version: string
}

/** Android 版本 → SDK_INT，与 data/scripts/miroms/constants.py 的 SDK_VERSIONS 对齐 */
const SDK_BY_ANDROID: Record<string, string> = {
  '17.0': '37', '16.0': '36', '16': '36',
  '15.0': '35', '15': '35',
  '14.0': '34', '14': '34',
  '13.0': '33', '13': '33',
  '12.0': '31', '12': '31',
  '11.0': '30', '11': '30',
  '10.0': '29', '10': '29',
}

/** 表单里接口实际读取的区域写法；其余区域直接用数据里的 region 值 */
const REGION_FIELD_OVERRIDES: Record<string, string> = { cn: 'CN', global: 'GL' }

// 输入白名单：本路由只转发到固定的小米地址，严格校验可避免被当作通用代理滥用
const CODE_RE = /^[a-z][a-z0-9_]*$/
const DEVICE_RE = /^[a-z][a-z0-9]*$/
const VERSION_RE = /^[A-Za-z0-9][A-Za-z0-9.]*$/
const ANDROID_RE = /^[0-9]{1,2}(\.0)?$/
const REGION_RE = /^[a-z]{0,6}$/
const TAG_RE = /^[A-Za-z]$/

export default defineEventHandler(async (event) => {
  // 签名直链具有时效性，任何层级都不应缓存本接口响应
  setResponseHeader(event, 'cache-control', 'no-store')

  const body = await readBody<HighSpeedRequestBody>(event)

  if (
    !body ||
    !CODE_RE.test(body.code || '') ||
    !DEVICE_RE.test(body.device || '') ||
    !VERSION_RE.test(body.version || '') ||
    !ANDROID_RE.test(body.android || '') ||
    !REGION_RE.test(body.region || '') ||
    !TAG_RE.test(body.branchTag || '') ||
    typeof body.zone !== 'number'
  ) {
    throw createError({ statusCode: 400, statusMessage: 'Invalid request' })
  }

  const region = body.region.toLowerCase()

  // 字段与 HyperOSForm 模板一致，只覆盖与机型/版本相关的项。
  // 注意：JSON.stringify 默认包含全部字段（等价于 kotlinx encodeDefaults = true），
  // 固定字段一个都不能省，否则服务端只回没有 LatestRom 的空响应。
  const form = {
    obv: 'OS1.0',
    channel: '',
    sys: '0',
    bv: '816',
    id: '',
    sn: '0x0000043b716a25f1',
    a: '0',
    b: body.branchTag,
    c: body.android,
    unlock: '0',
    d: body.code,
    lockZoneChannel: 'normal',
    f: '1',
    ov: body.version,
    g: '9b65722a06722e8d8dffa35a9fd58586',
    i: '14db85f96df2efc324323fa7679f0d847ff53f3bff7179ea0c778ce5d980bc03',
    i2: '2cd7c24f21e33b236fc63f26d044227b96d8b39a80400654f88182322688793b',
    isR: '0',
    l: 'zh_CN',
    n: '',
    p: body.device,
    pb: 'Redmi',
    r: REGION_FIELD_OVERRIDES[region] || body.region,
    v: body.version,
    sdk: SDK_BY_ANDROID[body.android] || '36',
    // 与 fetch_changelog.py 一致：pn 去掉 _global 后缀
    pn: body.code.split('_global')[0],
    options: {
      zone: body.zone,
      hashId: 'dae7d50f696d7403',
      ab: '1',
      previewPlan: '0',
      sv: 3,
      av: '8.8.8',
      cv: body.version,
    },
  }

  try {
    const encrypted = await miuiEncrypt(JSON.stringify(form))

    const raw = await $fetch<string>(OTA_URL, {
      method: 'POST',
      headers: {
        // 与 miroms/network.py 的请求头保持一致
        'User-Agent': 'Dalvik/2.1.0 (Linux; U; Android 13; MI 9 Build/TKQ1.220829.002)',
        Cookie: 'serviceToken=;',
        // 必须显式声明；application/json 会被接口回 406
        Accept: '*/*',
        'Content-Type': 'application/x-www-form-urlencoded',
      },
      body: `q=${encrypted}&s=1&t=`,
      responseType: 'text',
      timeout: 10000,
    })

    const text = String(raw).trim()

    // 出错时接口可能回明文 JSON（含 "code" 字段），不是密文
    if (text.startsWith('{')) {
      return { status: 'notfound' }
    }

    // 正常响应是纯 base64；个别情况下带 q= 后缀（Python: split("q=")[0]）
    const cipherPart = (text.split('q=')[0] || '').trim()
    if (!cipherPart) {
      return { status: 'notfound' }
    }

    const parsed = JSON.parse(await miuiDecrypt(cipherPart)) as {
      MirrorList?: string[]
      LatestRom?: { version?: string; filename?: string }
    }

    const latest = parsed.LatestRom
    const version = latest?.version || ''
    const filename = latest?.filename || ''

    if (!latest || !version || !filename) {
      return { status: 'notfound' }
    }
    // 没有 ?t=&s= 说明接口没给签名，拼出来的直链会 403
    if (!filename.includes('?')) {
      return { status: 'unsigned' }
    }
    // 接口只对该分支最新版签发签名；返回的版本与请求不一致说明请求的是旧版
    if (version !== body.version) {
      return { status: 'outdated', latestVersion: version }
    }

    const links = [...new Set(parsed.MirrorList || [])]
      .filter((m) => m.startsWith('https://') && (m.includes('superota') || m.includes('ultimateota')))
      .map((mirror) => ({
        mirror,
        url: `${mirror.replace(/\/+$/, '')}/${version}/${filename}`,
      }))

    if (links.length === 0) {
      return { status: 'notfound' }
    }
    return { status: 'available', version, links }
  } catch (err) {
    const message = err instanceof Error ? err.message : 'unknown error'
    return { status: 'failed', message }
  }
})
