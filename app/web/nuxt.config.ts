export default defineNuxtConfig({
  future: { compatibilityVersion: 4 },
  compatibilityDate: '2025-05-08',

  typescript: {
    tsConfig: {
      compilerOptions: {
        types: ['node'],
      },
      // 让 VS Code 把 server/** 归入主 TS 项目（含 nitropack 路径别名与 node 类型），
      // 否则 server 文件只被独立的 .nuxt/tsconfig.server.json 覆盖，编辑器里会误报
      include: ['../server/**/*'],
    },
  },

  css: ['~/assets/css/main.css'],

  ssr: true,
  components: true,
  devtools: { enabled: false },

  app: {
    pageTransition: false,
  },

  modules: [
    '@nuxtjs/tailwindcss',
    '@nuxtjs/sitemap',
    '@nuxtjs/device',
    '@nuxtjs/i18n',
  ],

  i18n: {
    locales: [
      { code: 'zh-cn', name: '中文' },
      { code: 'zh', name: '中文' },
      { code: 'en-us', name: 'English' },
      { code: 'en', name: 'English' },
    ],
    strategy: 'prefix',
    defaultLocale: 'zh-cn',
    vueI18n: '../i18n.config.ts',
  },

  runtimeConfig: {
    public: {
      apiBaseUrl: 'https://api.miuier.com/api',
    },
  },

  vite: {
    define: {
      __BUILD_TIME__: JSON.stringify(new Date().toISOString()),
    },
    server: {
      hmr: { overlay: false },
    },
  },

  site: {
    url: 'https://hub.miuier.com',
    name: 'MiROMS HUB',
  },

  sitemap: {
    // 为自动扫描到的页面（如 /zh-cn、/zh-cn/devices、/zh-cn/roms）自动填充 lastmod
    autoLastmod: true,

    // --- Sitemap Performance（@nuxtjs/sitemap 8.5，文档见 nuxtseo.com/sitemap/guides/performance）---
    // 生产环境 SWR 缓存 1 小时（默认 10 分钟）。sitemap 数据来自本地 data/api/v3，随部署更新，
    // 调高 TTL 可显著降低源站与序列化压力。
    cacheMaxAgeSeconds: 3600,

    // 流式序列化：按 ~64KB 分块输出 XML，避免完整 XML 字符串驻留内存。
    experimentalStreaming: true,

    // 客户端支持时流式 gzip/deflate 压缩（不支持 CompressionStream 的运行时自动降级）。
    experimentalCompression: true,

    // Nitro 启动后预热各 locale sitemap（源为本地文件，预热开销极小），首次请求不再慢。
    experimentalWarmUp: true,
  },
})
