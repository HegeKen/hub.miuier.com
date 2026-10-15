<template>
  <div class="container-page py-10 sm:py-14">
    <header class="mb-6">
      <h1 class="text-2xl font-semibold tracking-tight sm:text-3xl">{{ $t('devTitle') }}</h1>
      <p class="mt-2 text-sm text-[var(--color-text-secondary)]">{{ $t('devSub') }}</p>
    </header>

    <!-- 区域筛选：大陆版 / 国际版 -->
    <div v-if="regions.length > 1" class="mb-4 flex flex-wrap gap-2">
      <button
        v-for="item in regionTabs"
        :key="item.value"
        type="button"
        class="filter-pill"
        :class="region === item.value ? 'filter-pill-active' : ''"
        @click="region = item.value"
      >
        {{ item.label }}
      </button>
    </div>

    <!-- Loading -->
    <div v-if="pending" class="flex justify-center py-20">
      <span class="spinner" role="status" aria-label="Loading"></span>
    </div>

    <div v-else-if="error" class="py-20 text-center">
      <p class="text-sm text-[var(--color-text-secondary)]">{{ $t('devNoLogs') }}</p>
    </div>

    <template v-else>
      <!-- 按大版本分组 -->
      <section v-for="group in groupedWeeks" :key="group.key" class="mb-8">
        <h2 class="mb-3 text-sm font-semibold text-[var(--color-text)]">
          {{ group.key || $t('devUnknownBigver') }}
        </h2>
        <div class="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
          <NuxtLink
            v-for="item in group.items"
            :key="item.region + '-' + item.week"
            :to="`/${locale}/dev/${item.week}` + (item.region === 'cn' ? '' : `?region=${item.region}`)"
            class="group rounded-xl border border-[var(--color-border)] bg-[var(--color-bg-surface)] px-4 py-3 transition-colors hover:bg-[var(--color-bg-subtle)]"
          >
            <div class="flex items-center justify-between gap-3">
              <span class="text-base font-semibold text-[var(--color-text)]">
                {{ $t('devWeek', { week: item.week }) }}
              </span>
              <span
                v-if="item.region !== 'cn'"
                class="rounded-full border border-[var(--color-border)] px-2 py-0.5 text-[11px] text-[var(--color-text-tertiary)]"
              >
                {{ $t('devRegionGlobal') }}
              </span>
            </div>
            <p class="mt-1.5 font-mono text-xs text-[var(--color-text-secondary)]">{{ item.versions || '—' }}</p>
            <p class="mt-1 text-xs text-[var(--color-text-tertiary)]">
              {{ item.release || '—' }}
              <template v-if="item.deviceCount"> · {{ $t('devDevices', { count: item.deviceCount }) }}</template>
            </p>
          </NuxtLink>
        </div>
      </section>
    </template>
  </div>
</template>

<script setup>
const { locale, t } = useI18n()
const { buildDevIndexUrl } = useApi()

const { data, pending, error } = await useAsyncData('dev-index', () => $fetch(buildDevIndexUrl()))

const regions = computed(() => data.value?.regions || ['cn'])
const region = ref('cn')
// 只有一种区域时直接用它
watchEffect(() => {
  if (regions.value.length && !regions.value.includes(region.value)) {
    region.value = regions.value[0]
  }
})

const regionTabs = computed(() => regions.value.map((value) => ({
  value,
  label: value === 'cn' ? t('devRegionCn') : t('devRegionGlobal'),
})))

const weeks = computed(() =>
  (data.value?.weeks || []).filter((item) => item.region === region.value)
)

const groupedWeeks = computed(() => {
  const map = new Map()
  for (const item of weeks.value) {
    const key = item.bigver || ''
    if (!map.has(key)) map.set(key, [])
    map.get(key).push(item)
  }
  // 未标注大版本的周次统一排到最后（其余保持接口顺序：按周数降序）
  return [...map.entries()]
    .map(([key, items]) => ({ key, items }))
    .sort((a, b) => (a.key ? 0 : 1) - (b.key ? 0 : 1))
})

useHead({
  title: computed(() => `${t('devTitle')} - ${t('site')}`),
})
</script>
