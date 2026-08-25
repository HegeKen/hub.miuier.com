/**
 * 客户端插件：自动将 localStorage 中的数据库连接配置
 * 附加到所有 $fetch 请求的 x-db-config 请求头，
 * 供服务端 getPool() 按请求创建对应的连接池。
 */
export default defineNuxtPlugin(() => {
  const STORAGE_KEY = 'miroms_db_config'

  const getConfig = (): string | null => {
    try {
      return localStorage.getItem(STORAGE_KEY)
    } catch {
      return null
    }
  }

  // 拦截全局 $fetch，自动附加数据库配置头
  if (import.meta.client && typeof window !== 'undefined') {
    const originalFetch = window.fetch
    window.fetch = function (input: RequestInfo | URL, init?: RequestInit): Promise<Response> {
      const config = getConfig()
      if (config) {
        const headers = new Headers(init?.headers)
        headers.set('x-db-config', config)
        init = { ...init, headers }
      }
      return originalFetch.call(this, input, init)
    }
  }
})
