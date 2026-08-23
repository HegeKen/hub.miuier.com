export default defineNuxtConfig({
  future: { compatibilityVersion: 4 },
  compatibilityDate: '2025-05-08',

  css: ['~/assets/css/main.css'],

  // 管理后台使用 SPA 模式：页面数据全部通过服务端 API 从数据库实时读取
  ssr: false,
  devtools: { enabled: false },

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
    db: {
      host: process.env.NUXT_DB_HOST || 'localhost',
      port: Number(process.env.NUXT_DB_PORT || 3306),
      user: process.env.NUXT_DB_USER || 'root',
      password: process.env.NUXT_DB_PASSWORD || '199621@Aa',
      database: process.env.NUXT_DB_NAME || 'miroms',
    },
  },
})
