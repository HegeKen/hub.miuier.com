<template>
  <div class="container-page py-10 sm:py-14">
    <header class="mb-8">
      <h1 class="text-2xl font-semibold tracking-tight sm:text-3xl">{{ $t('sitelog') }}</h1>
      <p class="mt-2 text-sm text-[var(--color-text-secondary)]">{{ $t('sitelogSub') }}</p>
      <p v-if="site?.latest" class="mt-3 flex items-center gap-2 text-sm">
        <span class="text-[var(--color-text-secondary)]">{{ $t('currentVersion') }}</span>
        <span class="font-mono text-base font-semibold text-[var(--color-text)]">{{ site.latest }}</span>
      </p>
    </header>

    <!-- Loading -->
    <div v-if="pending" class="flex justify-center py-20">
      <span class="spinner" role="status" aria-label="Loading"></span>
    </div>

    <!-- Error -->
    <div v-else-if="error" class="py-20 text-center">
      <p class="text-sm text-[var(--color-text-secondary)]">{{ $t('loadFailed') }}</p>
      <button type="button" class="pill mt-4" @click="refresh()">{{ $t('retry') }}</button>
    </div>

    <!-- Empty -->
    <div v-else-if="!logs.length" class="py-20 text-center">
      <p class="text-sm text-[var(--color-text-secondary)]">{{ $t('sitelogEmpty') }}</p>
    </div>

    <!-- 版本时间线：最新一版在最上方并标注当前版本 -->
    <ol v-else class="ms-1 space-y-7 border-s border-[var(--color-border)] ps-6">
      <li v-for="(entry, index) in logs" :key="entry.ver" class="relative">
        <span
          class="absolute -start-[30px] top-1.5 h-2.5 w-2.5 rounded-full"
          :class="index === 0 ? 'bg-[var(--color-accent)]' : 'bg-[var(--color-border-strong)]'"
          aria-hidden="true"
        />
        <div class="flex flex-wrap items-center gap-x-3 gap-y-1">
          <span class="font-mono text-base font-semibold text-[var(--color-text)]">{{ entry.ver }}</span>
          <span class="rounded-full border border-[var(--color-border)] px-2 py-0.5 text-[11px] text-[var(--color-text-tertiary)]">
            {{ entry.date }}
          </span>
          <span
            v-if="index === 0"
            class="rounded-full bg-[var(--color-accent-soft)] px-2 py-0.5 text-[11px] font-medium text-[var(--color-accent)]"
          >
            {{ $t('currentVersion') }}
          </span>
        </div>
        <ul v-if="entry.log?.length" class="mt-2.5 space-y-1.5">
          <li
            v-for="(item, i) in entry.log"
            :key="i"
            class="flex gap-2.5 text-sm leading-relaxed text-[var(--color-text-secondary)]"
          >
            <span class="mt-2 h-1 w-1 shrink-0 rounded-full bg-[var(--color-border-strong)]" aria-hidden="true" />
            <span>{{ pick(item) }}</span>
          </li>
        </ul>
      </li>
    </ol>
  </div>
</template>

<script setup>
const { locale, t } = useI18n()
const { buildSiteLogUrl } = useApi()

// 站点日志按语言取用：数据里只有 zh / en 两列，中文（含 zh-tw）用 zh，其余用 en 兜底 zh
const lang = computed(() => (locale.value.startsWith('zh') ? 'zh' : 'en'))
const pick = (item) => item?.[lang.value] || item?.zh || item?.en || ''

const { data, pending, error, refresh } = await useAsyncData(
  'sitelog',
  () => $fetch(buildSiteLogUrl())
)

// 本页只展示本站（hub.web）这一条线；admin 的日志由管理后台自己读取
const site = computed(() => data.value?.sites?.['hub.web'] || null)
const logs = computed(() => site.value?.logs || [])

useHead({
  title: computed(() => `${t('sitelog')} - ${t('site')}`),
})
</script>
