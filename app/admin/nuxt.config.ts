import { existsSync, readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

/**
 * 本地默认数据库连接（登录页「本地数据库」按钮 + `/api/auth/test-local`）。
 *
 * 取值优先级：
 *   1. `NUXT_DB_*` 环境变量（`app/admin/.env` 或 shell 里显式指定）—— 最高优先级；
 *   2. `data/scripts/config.py`（数据仓库的本地抓取配置，dev 环境下一定存在）
 *      —— 与管理后台同处一个仓库，直接复用，避免「本地连接永远 Access denied」；
 *   3. 内置默认值（localhost / 3306 / root / 空密码 / miroms）。
 *
 * 生产环境（Cloudflare Pages / 独立部署）通常没有 config.py，请用 NUXT_DB_* 注入。
 */
const DB_DEFAULTS = {
  host: 'localhost',
  port: 3306,
  user: 'root',
  password: '',
  database: 'miroms',
}

function fromDataScriptsConfig(): Partial<typeof DB_DEFAULTS> {
  try {
    const here = fileURLToPath(new URL('.', import.meta.url))
    const file = resolve(here, '../../data/scripts/config.py')
    if (!existsSync(file)) return {}
    const source = readFileSync(file, 'utf-8')
    const text = (key: string) => {
      const matched = source.match(new RegExp(`^\\s*${key}\\s*=\\s*['"]([^'"]*)['"]`, 'm'))
      return matched?.[1]
    }
    const num = (key: string) => {
      const matched = source.match(new RegExp(`^\\s*${key}\\s*=\\s*(\\d+)`, 'm'))
      return matched ? Number(matched[1]) : undefined
    }
    return {
      host: text('host'),
      port: num('port'),
      user: text('user'),
      password: text('password'),
      database: text('database'),
    }
  } catch {
    return {}
  }
}

function localDb() {
  const fromFile = fromDataScriptsConfig()
  return {
    host: process.env.NUXT_DB_HOST || fromFile.host || DB_DEFAULTS.host,
    port: Number(process.env.NUXT_DB_PORT || fromFile.port || DB_DEFAULTS.port),
    user: process.env.NUXT_DB_USER || fromFile.user || DB_DEFAULTS.user,
    password: process.env.NUXT_DB_PASSWORD ?? fromFile.password ?? DB_DEFAULTS.password,
    database: process.env.NUXT_DB_NAME || fromFile.database || DB_DEFAULTS.database,
  }
}

const db = localDb()
const dbSource = process.env.NUXT_DB_HOST || process.env.NUXT_DB_PASSWORD !== undefined
  ? '环境变量 NUXT_DB_*'
  : (Object.keys(fromDataScriptsConfig()).length ? 'data/scripts/config.py' : '内置默认值')
// eslint-disable-next-line no-console
console.log(`[admin] 本地默认数据库连接：${db.user}@${db.host}:${db.port}/${db.database}（来源：${dbSource}）`)

export default defineNuxtConfig({
  future: { compatibilityVersion: 4 },
  compatibilityDate: '2025-05-08',

  css: ['~/assets/css/main.css'],

  // 管理后台使用 SPA 模式：页面数据全部通过服务端 API 从数据库实时读取
  ssr: false,
  devtools: { enabled: true },

  modules: ['@nuxtjs/tailwindcss'],

  devServer: {
    port: 3100,
  },

  app: {
    head: {
      title: 'MiROMS HUB - 数据管理',
      meta: [
        { name: 'robots', content: 'noindex, nofollow' },
      ],
    },
  },

  runtimeConfig: {
    db,
  },
})
