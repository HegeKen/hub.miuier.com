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
      include: ['../server/**/*', '../i18n.config.ts'],
    },
  },

  css: ['~/assets/css/main.css'],

  ssr: true,
  components: true,
  devtools: { enabled: false },

  app: {
    pageTransition: false,
    // 图标链接放在这里而非组件内 useHead：内容区是 ClientOnly，放这里才能进入 SSR 的 HTML，
    // 浏览器无需等 JS 执行就开始拉取 favicon
    head: {
      link: [
        // 标签页 / 书签栏：16~48px 用去掉字标的图形版（字标在该尺寸下无法辨认）
        { rel: 'icon', href: '/favicon.ico', sizes: '16x16 32x32 48x48' },
        { rel: 'icon', type: 'image/png', sizes: '16x16', href: '/favicon-16x16.png' },
        { rel: 'icon', type: 'image/png', sizes: '32x32', href: '/favicon-32x32.png' },
        // 主屏 / 安装图标：完整 logo；iOS 不支持透明应用图标，apple-touch-icon 已压白底
        { rel: 'apple-touch-icon', sizes: '180x180', href: '/apple-touch-icon.png' },
        { rel: 'manifest', href: '/site.webmanifest' },
      ],
    },
  },

  modules: [
    '@nuxtjs/tailwindcss',
    '@nuxtjs/sitemap',
    '@nuxtjs/device',
    '@nuxtjs/i18n',
  ],

  i18n: {
    // name = 语言自称（下拉展示），short = 顶栏按钮上的短标签，language = SEO / 浏览器语言匹配用的 BCP47 标签
    locales: [
      { code: 'zh-cn', language: 'zh-Hans', name: '简体中文', short: '简中' },
      { code: 'zh-tw', language: 'zh-Hant', name: '繁體中文', short: '繁中' },
      { code: 'en-us', language: 'en', name: 'English', short: 'EN' },
      { code: 'ja', language: 'ja', name: '日本語', short: '日本語' },
      { code: 'ko', language: 'ko', name: '한국어', short: '한국어' },
      { code: 'ru', language: 'ru', name: 'Русский', short: 'Рус' },
      { code: 'uk', language: 'uk', name: 'Українська', short: 'УКР' },
      { code: 'pl', language: 'pl', name: 'Polski', short: 'PL' },
      { code: 'de', language: 'de', name: 'Deutsch', short: 'DE' },
      { code: 'fr', language: 'fr', name: 'Français', short: 'FR' },
      { code: 'it', language: 'it', name: 'Italiano', short: 'IT' },
      { code: 'es', language: 'es', name: 'Español', short: 'ES' },
      { code: 'pt', language: 'pt', name: 'Português', short: 'PT' },
      { code: 'tr', language: 'tr', name: 'Türkçe', short: 'TR' },
      { code: 'id', language: 'id', name: 'Bahasa Indonesia', short: 'ID' },
      { code: 'vi', language: 'vi', name: 'Tiếng Việt', short: 'VI' },
      { code: 'th', language: 'th', name: 'ไทย', short: 'ไทย' },
      // dir: 'rtl' 是 RTL 的唯一声明处，MiRoms.vue 据此写 <html dir>
      { code: 'ar', language: 'ar', name: 'العربية', short: 'ع', dir: 'rtl' },
      // 兼容既有链接（/zh/、/en/）：hidden 的语言不出现在切换器里
      { code: 'zh', language: 'zh-Hans', name: '中文', hidden: true },
      { code: 'en', language: 'en', name: 'English', hidden: true },
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

    // 预热后，sitemap 生成过程在服务端完全消失，客户端请求时直接返回缓存数据
    zeroRuntime: true, 
  },
})
