#!/usr/bin/env node

/**
 * i18n 语言包自检：确保每个语言包的词条与简体中文（zh-cn）完全对齐，
 * 并且 i18n.config.ts / nuxt.config.ts 中登记的语言与实际语言包一一对应。
 *
 * 用法：node scripts/check-locales.mjs   （package.json: pnpm check:i18n）
 *
 * 语言包是纯字面量对象（不含 TS 类型标注），因此这里直接按 JS 模块求值，
 * 避免依赖 ts 运行时。
 */

import { readdir, readFile } from 'fs/promises'
import { join, dirname, basename } from 'path'
import { fileURLToPath, pathToFileURL } from 'url'

const __dirname = dirname(fileURLToPath(import.meta.url))
const WEB_DIR = join(__dirname, '..')
const LOCALES_DIR = join(WEB_DIR, 'i18n/locales')
const I18N_CONFIG = join(WEB_DIR, 'i18n.config.ts')
const NUXT_CONFIG = join(WEB_DIR, 'nuxt.config.ts')

const REFERENCE = 'zh-cn'

// 兼容既有链接（/zh/、/en/）的别名 locale：复用另一个语言包的词条，不单独建文件
const ALIASES = { zh: 'zh-cn', en: 'en-us' }

const loadMessages = async (file) => {
  const source = await readFile(file, 'utf-8')
  const encoded = Buffer.from(source, 'utf-8').toString('base64')
  const mod = await import(`data:text/javascript;base64,${encoded}`)
  return mod.default
}

// 展平成 'regions.cn' 这样的路径，便于逐键比对
const flatten = (obj, prefix = '') =>
  Object.entries(obj).flatMap(([key, value]) =>
    value && typeof value === 'object' ? flatten(value, `${prefix}${key}.`) : [`${prefix}${key}`]
  )

// 取出 {count} / {page} 这类占位符
const placeholders = (text) => [...String(text).matchAll(/\{(\w+)\}/g)].map((m) => m[1]).sort().join(',')

const pick = (obj, path) => path.split('.').reduce((acc, key) => (acc == null ? acc : acc[key]), obj)

const main = async () => {
  const files = (await readdir(LOCALES_DIR)).filter((f) => f.endsWith('.ts')).sort()
  const codes = files.map((f) => basename(f, '.ts'))

  if (!codes.includes(REFERENCE)) {
    throw new Error(`找不到基准语言包 ${REFERENCE}.ts`)
  }

  const messages = {}
  for (const code of codes) {
    messages[code] = await loadMessages(join(LOCALES_DIR, `${code}.ts`))
  }

  const referenceKeys = flatten(messages[REFERENCE])
  const problems = []

  // 1) 键集合 + 占位符必须与基准语言一致
  for (const code of codes) {
    if (code === REFERENCE) continue
    const keys = flatten(messages[code])
    const missing = referenceKeys.filter((k) => !keys.includes(k))
    const extra = keys.filter((k) => !referenceKeys.includes(k))
    if (missing.length) problems.push(`${code}: 缺少词条 ${missing.join(', ')}`)
    if (extra.length) problems.push(`${code}: 多出词条 ${extra.join(', ')}`)

    const placeholderDiff = referenceKeys
      .filter((k) => keys.includes(k))
      .filter((k) => placeholders(pick(messages[REFERENCE], k)) !== placeholders(pick(messages[code], k)))
      .map((k) => `${k}（应为 {${placeholders(pick(messages[REFERENCE], k))}}）`)
    if (placeholderDiff.length) problems.push(`${code}: 占位符不一致 ${placeholderDiff.join(', ')}`)
  }

  // 2) 语言包 ↔ i18n.config.ts / nuxt.config.ts 双向登记检查
  const i18nConfig = await readFile(I18N_CONFIG, 'utf-8')
  const nuxtConfig = await readFile(NUXT_CONFIG, 'utf-8')
  const registeredLocales = [...nuxtConfig.matchAll(/\{\s*code:\s*'([^']+)'/g)].map((m) => m[1])

  for (const code of codes) {
    const importName = `./i18n/locales/${code}'`
    if (!i18nConfig.includes(importName)) {
      problems.push(`${code}: 未在 i18n.config.ts 中 import`)
    }
    if (!new RegExp(`['"]?${code}['"]?\\s*[:,]`).test(i18nConfig.split('messages:')[1] || '')) {
      problems.push(`${code}: 未在 i18n.config.ts 的 messages 中登记`)
    }
  }
  for (const code of registeredLocales) {
    if (!codes.includes(code) && !(code in ALIASES)) {
      problems.push(`nuxt.config.ts 登记了 ${code}，但缺少 i18n/locales/${code}.ts`)
    }
  }
  for (const [alias, base] of Object.entries(ALIASES)) {
    if (!codes.includes(base)) problems.push(`别名 ${alias} 指向的语言包 ${base}.ts 不存在`)
    if (!registeredLocales.includes(alias)) problems.push(`别名 ${alias} 未在 nuxt.config.ts 中登记`)
  }

  const languages = new Set(registeredLocales)
  const missingInNuxt = codes.filter((c) => !languages.has(c))
  if (missingInNuxt.length) {
    problems.push(`未在 nuxt.config.ts 的 i18n.locales 中登记：${missingInNuxt.join(', ')}`)
  }

  if (problems.length) {
    console.error(`✖ i18n 自检失败（${problems.length} 项）：`)
    for (const p of problems) console.error(`  - ${p}`)
    process.exit(1)
  }

  console.log(`✔ i18n 自检通过：${codes.length} 个语言包，各 ${referenceKeys.length} 条词条，占位符一致`)
}

main().catch((error) => {
  console.error(`✖ i18n 自检异常：${error.message}`)
  process.exit(1)
})
