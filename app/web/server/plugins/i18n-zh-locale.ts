import { defineNitroPlugin } from 'nitropack/runtime'

/**
 * 修正中文区域标签的浏览器语言识别（让 root 跳转把繁中用户送到 /zh-tw）。
 *
 * 背景：@nuxtjs/i18n 的 `findBrowserLocale`（dist/runtime/kit/browser.js）分两轮匹配：
 *   1) 按 `locale.language` **完整串**精确比对；
 *   2) 只取主语言子标签 `language.split('-')[0]` 比对，取列表里**第一个**命中的。
 *
 * 本站 `zh-cn` / `zh-tw` 的 `language` 用的是脚本形式 `zh-Hans` / `zh-Hant`，
 * 而浏览器实际发的是区域形式：台湾 `zh-TW`、港澳 `zh-HK` / `zh-MO`，
 * 部分浏览器还会发 `zh-Hant-TW`。这些标签第一轮都匹配不上（完整串不等），
 * 第二轮又统统退化成 `zh`，于是命中 locales 里更靠前的 `zh-cn`——繁中用户被跳到简体。
 *
 * 做法：在 i18n 读取请求头之前，把请求头里的中文区域标签归一化成脚本形式
 * （繁体类 → `zh-Hant`，简体类 → `zh-Hans`），让模块第一轮的精确匹配自己落到
 * `zh-tw` / `zh-cn`。**这里不自己发跳转**，只改这一个请求头，因此
 * cookie（`i18n_redirected`）优先、`redirectOn: 'root'` 只对根路径生效、
 * 查询串保留等行为仍然完全由模块负责，不会和模块打架。
 *
 * 时机：模块的 root 跳转挂在 Nitro 的 `render:before` 钩子上，读取 header 是在
 * `useDetectors(event).header()` 里懒执行的；而所有 `request` 钩子都早于
 * `render:before`，且 h3 v1 的 `getRequestHeader` 每次都重新读 `event.node.req.headers`，
 * 所以在这里改写一定生效。已确认项目未配置自定义 `localeDetector`（构建产物里
 * `localeDetector: ""`，不会在 request 阶段提前解析语言）。
 *
 * 繁体 / 简体之外的语种一律原样放行，模块的默认行为不变。
 */

// 台湾 zh-TW / zh-Hant-TW、港澳 zh-HK / zh-MO / zh-Hant-HK
const TRADITIONAL = /^zh-(?:hant|tw|hk|mo)(?:-|$)/i
// 大陆 zh-CN / zh-Hans-CN、新加坡 zh-SG（新加坡用简体）
const SIMPLIFIED = /^zh-(?:hans|cn|sg)(?:-|$)/i

/** 把单个语言标签归一化成 zh-Hant / zh-Hans；其余原样返回（含裸 `zh`，语义不明确，交给模块） */
function normalizeTag(tag: string): string {
  const value = tag.trim()
  if (TRADITIONAL.test(value)) return 'zh-Hant'
  if (SIMPLIFIED.test(value)) return 'zh-Hans'
  return value
}

export default defineNitroPlugin((nitroApp) => {
  nitroApp.hooks.hook('request', (event) => {
    const headers = event.node.req.headers
    const raw = headers['accept-language']
    // 没带该头、或与中文无关的请求直接跳过，避免影响绝大多数流量
    if (typeof raw !== 'string' || !/zh/i.test(raw)) return

    const normalized = raw
      .split(',')
      .map((part) => {
        // 只替换语言标签本身，保留 "zh-TW;q=0.9" 这类参数（q 值决定优先级，不能丢）
        const semi = part.indexOf(';')
        const tag = semi === -1 ? part : part.slice(0, semi)
        const params = semi === -1 ? '' : part.slice(semi)
        return normalizeTag(tag) + params
      })
      .join(',')

    if (normalized !== raw) {
      headers['accept-language'] = normalized
    }
  })
})
