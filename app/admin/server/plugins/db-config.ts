import { getHeader } from 'h3'
import { defineNitroPlugin } from 'nitropack/runtime'
import { setRequestDbConfig } from '../utils/db'

/**
 * Nitro 插件：在每个请求开始时从 x-db-config 请求头读取数据库配置，
 * 并设置到 db.ts 的模块级变量中，供 getPool() 使用。
 */
export default defineNitroPlugin((nitroApp) => {
  nitroApp.hooks.hook('request', (event) => {
    try {
      const raw = getHeader(event, 'x-db-config')
      if (raw) {
        setRequestDbConfig(JSON.parse(raw))
      } else {
        setRequestDbConfig(null)
      }
    } catch {
      setRequestDbConfig(null)
    }
  })
})
