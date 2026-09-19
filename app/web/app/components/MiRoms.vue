<template>
  <div></div>
</template>

<script setup>
// <html lang> / <html dir> 跟随当前语言，而非固定 zh-cn（SEO / 无障碍 / RTL）
const { locale, locales } = useI18n()
const current = computed(() => locales.value.find((l) => l.code === locale.value))

const htmlLang = computed(() => {
  // 优先用 nuxt.config 里配置的 BCP47 language（zh-Hans / zh-Hant / en / ja ...），否则回退到 locale code
  return String(current.value?.language || current.value?.code || locale.value)
})

// RTL（阿拉伯语等）在 nuxt.config.ts 的 i18n.locales 里用 `dir: 'rtl'` 声明，这里直接读取，
// 避免语言清单与方向判断两处维护。布局镜像由 CSS 逻辑属性自动完成：
// start/end（定位）、ps/pe、ms/me（内外边距）、text-start、border-e，方向性图标见 main.css 的 .dir-flip
const htmlDir = computed(() => (current.value?.dir === 'rtl' ? 'rtl' : 'ltr'))

useHead({
  htmlAttrs: { lang: htmlLang, dir: htmlDir },
  meta: [
    { name: 'theme-color', content: '#FF6900' },
    { name: 'description', content: 'Xiaomi / REDMI / POCO ROM Archive - MiROMS HUB' },
  ],
})
</script>
