import { existsSync, readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { defineNitroPlugin } from 'nitropack/runtime'

// sitemap 支持的语言（multi-sitemap 下每个 locale 独立一个 sitemap）
const SITEMAP_LOCALES = ['zh-cn', 'zh', 'en-us', 'en']

// 运行时按 locale 注入设备/ROM 详情页的真实 URL，使 sitemap 收录详情页。
// 注意：`sitemap:sources` 是 Nitro 运行时钩子，必须通过 defineNitroPlugin 注册，
// 不能写在 nuxt.config.ts 的 nitro.hooks（构建时 NestedHooks<NitroHooks>）中。
function resolveDataRoot(): string {
  const candidates = [
    resolve(process.cwd(), '../../data/api/v3'),
    resolve(process.cwd(), '../../../data/api/v3'),
  ]
  return candidates.find((c) => existsSync(c)) || candidates[0]!
}

export default defineNitroPlugin((nitroApp) => {
  const DATA_ROOT = resolveDataRoot()

  nitroApp.hooks.hook('sitemap:sources', async ({ sitemapName, sources }) => {
    if (!SITEMAP_LOCALES.includes(sitemapName)) return
    console.log('[sitemap:sources] fired for', sitemapName, 'DATA_ROOT=', DATA_ROOT)
    try {
      // 用最近一次数据导出时间作为详情页 lastmod（stats.json.generatedAt）
      let lastmod: string | undefined
      try {
        const stats = JSON.parse(readFileSync(resolve(DATA_ROOT, 'stats.json'), 'utf-8'))
        if (stats.generatedAt) lastmod = String(stats.generatedAt).slice(0, 10)
      } catch (e) {
        console.warn('[sitemap:sources] 读取 stats.json 失败：', (e as Error).message)
      }

      const urls: { loc: string; lastmod?: string }[] = []

      const devices = JSON.parse(readFileSync(resolve(DATA_ROOT, 'index.json'), 'utf-8'))
      for (const d of devices) {
        urls.push({ loc: `/${sitemapName}/devices/${d.device}`, lastmod })
      }

      const romIndex = JSON.parse(readFileSync(resolve(DATA_ROOT, 'roms/index.json'), 'utf-8'))
      for (const o of romIndex) {
        urls.push({ loc: `/${sitemapName}/roms/${o.os}`, lastmod })
      }

      if (urls.length) {
        sources.push({ context: { name: 'data' }, urls })
        console.log('[sitemap:sources] injected', urls.length, 'urls for', sitemapName)
      }
    } catch (e) {
      console.warn('[sitemap] 加载 data 来源失败：', (e as Error).message)
    }
  })
})
