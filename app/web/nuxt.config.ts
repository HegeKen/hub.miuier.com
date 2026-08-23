export default defineNuxtConfig({
  future: { compatibilityVersion: 4 },
  compatibilityDate: '2025-05-08',

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
      apiBaseUrl: 'https://api.miuier.com/',
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
  },
})
