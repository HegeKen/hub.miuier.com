<template>
  <div class="container-page py-10 sm:py-14">
    <header class="mb-8">
      <h1 class="text-2xl font-semibold tracking-tight sm:text-3xl">{{ $t('xms') }}</h1>
      <p class="mt-2 text-sm text-[var(--color-text-secondary)]">{{ $t('xmsSub') }}</p>
      <p class="mt-1.5 text-xs text-[var(--color-text-tertiary)]">{{ $t('xmsArchiveHint') }}</p>
      <p v-if="generatedAt" class="mt-2 text-xs text-[var(--color-text-tertiary)]">
        {{ $t('statsGeneratedAt') }}: {{ generatedAt }}
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
    <div v-else-if="!apps.length && !devices.length" class="py-20 text-center">
      <p class="text-sm text-[var(--color-text-secondary)]">{{ $t('xmsEmpty') }}</p>
    </div>

    <template v-else>
      <!-- 版本分布 -->
      <div class="mb-6 flex flex-wrap items-center gap-2">
        <span
          v-for="version in versions"
          :key="version.version"
          class="inline-flex items-center gap-2 rounded-full border border-[var(--color-border)] bg-[var(--color-bg-surface)] px-3 py-1 text-xs"
        >
          <span class="font-mono font-semibold text-[var(--color-text)]">{{ version.version }}</span>
          <span
            v-if="version.os"
            class="rounded-full border border-[var(--color-border)] px-1.5 py-0.5 text-[10px] text-[var(--color-text-secondary)]"
          >
            {{ version.os }}
          </span>
          <span class="text-[var(--color-text-tertiary)]">
            {{ $t('xmsApps', { count: version.apps }) }} · {{ $t('totalDevices', { count: version.devices }) }}
          </span>
        </span>
      </div>

      <!-- 搜索 -->
      <div class="relative mb-8">
        <svg
          class="pointer-events-none absolute start-4 top-1/2 h-4 w-4 -translate-y-1/2 text-[var(--color-text-tertiary)]"
          xmlns="http://www.w3.org/2000/svg"
          fill="none"
          viewBox="0 0 24 24"
          stroke-width="1.5"
          stroke="currentColor"
          aria-hidden="true"
        >
          <path stroke-linecap="round" stroke-linejoin="round" d="m21 21-5.197-5.197m0 0A7.5 7.5 0 1 0 5.196 5.196a7.5 7.5 0 0 0 10.607 10.607Z" />
        </svg>
        <input
          v-model="query"
          type="search"
          :placeholder="$t('xmsSearchPlaceholder')"
          class="input-base ps-11"
        />
      </div>

      <!-- 筛选：系统版本 / 区域 / 系列 -->
      <div class="mb-8 flex flex-wrap items-center gap-x-5 gap-y-3">
        <div class="flex flex-wrap items-center gap-1.5">
          <span class="me-1 text-xs font-medium text-[var(--color-text-tertiary)]">OS</span>
          <button
            type="button"
            class="filter-pill !px-2.5 !py-1 text-xs"
            :class="osFilter === '' ? 'filter-pill-active' : ''"
            @click="osFilter = ''"
          >
            {{ $t('xmsAll') }}
          </button>
          <button
            v-for="os in osOptions"
            :key="os"
            type="button"
            class="filter-pill !px-2.5 !py-1 text-xs"
            :class="osFilter === os ? 'filter-pill-active' : ''"
            @click="osFilter = osFilter === os ? '' : os"
          >
            {{ os }}
          </button>
        </div>

        <div class="flex flex-wrap items-center gap-1.5">
          <span class="me-1 text-xs font-medium text-[var(--color-text-tertiary)]">{{ $t('region') }}</span>
          <button
            type="button"
            class="filter-pill !px-2.5 !py-1 text-xs"
            :class="regionFilter === '' ? 'filter-pill-active' : ''"
            @click="regionFilter = ''"
          >
            {{ $t('xmsAll') }}
          </button>
          <button
            v-for="region in regionOptions"
            :key="region"
            type="button"
            class="filter-pill !px-2.5 !py-1 text-xs"
            :class="regionFilter === region ? 'filter-pill-active' : ''"
            @click="regionFilter = regionFilter === region ? '' : region"
          >
            {{ $t('regions.' + region) }}
          </button>
        </div>

        <div class="flex flex-wrap items-center gap-1.5">
          <span class="me-1 text-xs font-medium text-[var(--color-text-tertiary)]">{{ $t('xmsSeries') }}</span>
          <button
            type="button"
            class="filter-pill !px-2.5 !py-1 text-xs"
            :class="seriesFilter === '' ? 'filter-pill-active' : ''"
            @click="seriesFilter = ''"
          >
            {{ $t('xmsAll') }}
          </button>
          <button
            v-for="series in seriesOptions"
            :key="series"
            type="button"
            class="filter-pill !px-2.5 !py-1 text-xs"
            :class="seriesFilter === series ? 'filter-pill-active' : ''"
            @click="seriesFilter = seriesFilter === series ? '' : series"
          >
            {{ seriesLabel(series) }}
          </button>
        </div>
      </div>

      <!-- 按应用 -->
      <section class="mb-10">
        <h2 class="mb-3 flex flex-wrap items-baseline gap-x-2">
          <span class="text-lg font-semibold text-[var(--color-text)]">{{ $t('xmsByApp') }}</span>
          <span class="text-xs text-[var(--color-text-tertiary)]">{{ $t('xmsApps', { count: filteredApps.length }) }}</span>
        </h2>

        <p v-if="!filteredApps.length" class="rounded-xl border border-[var(--color-border)] px-4 py-8 text-center text-sm text-[var(--color-text-secondary)]">
          {{ $t('noResults') }}
        </p>

        <div v-else class="grid grid-cols-1 gap-2 sm:grid-cols-2 lg:grid-cols-3">
          <NuxtLink
            v-for="app in filteredApps"
            :key="app.pkg"
            :to="appPath(app.pkg)"
            class="group flex items-center justify-between gap-3 rounded-xl border border-[var(--color-border)] bg-[var(--color-bg-surface)] px-4 py-3 transition-colors hover:border-[var(--color-border-strong)] hover:bg-[var(--color-bg-subtle)]"
          >
            <span class="min-w-0">
              <span class="block truncate text-sm font-medium text-[var(--color-text)]">
                {{ pickXmsText(app.name, locale) || app.pkg }}
              </span>
              <span class="mt-0.5 block truncate font-mono text-[11px] text-[var(--color-text-tertiary)]">
                {{ app.pkg }}
              </span>
              <span v-if="app.types && app.types.length" class="mt-1 flex flex-wrap gap-1">
                <span
                  v-for="type in app.types"
                  :key="type"
                  class="rounded-full border border-[var(--color-border)] px-1.5 py-0.5 text-[10px] text-[var(--color-text-secondary)]"
                >
                  {{ typeLabel(type) }}
                </span>
              </span>
            </span>
            <span class="shrink-0 text-end">
              <span class="block font-mono text-xs text-[var(--color-text)]">{{ app.latestVersion || '—' }}</span>
              <span class="mt-0.5 block text-[11px] text-[var(--color-text-tertiary)]">
                {{ $t('totalDevices', { count: app.deviceCount }) }}
              </span>
            </span>
          </NuxtLink>
        </div>
      </section>

      <!-- 按机型 -->
      <section>
        <h2 class="mb-3 flex flex-wrap items-baseline gap-x-2">
          <span class="text-lg font-semibold text-[var(--color-text)]">{{ $t('xmsByDevice') }}</span>
          <span class="text-xs text-[var(--color-text-tertiary)]">{{ $t('totalDevices', { count: filteredDevices.length }) }}</span>
        </h2>

        <p v-if="!filteredDevices.length" class="rounded-xl border border-[var(--color-border)] px-4 py-8 text-center text-sm text-[var(--color-text-secondary)]">
          {{ $t('noResults') }}
        </p>

        <div v-else class="grid grid-cols-1 gap-2 sm:grid-cols-2 lg:grid-cols-3">
          <NuxtLink
            v-for="item in filteredDevices"
            :key="item.device"
            :to="devicePath(item.device)"
            class="group rounded-xl border border-[var(--color-border)] bg-[var(--color-bg-surface)] px-4 py-3 transition-colors hover:border-[var(--color-border-strong)] hover:bg-[var(--color-bg-subtle)]"
          >
            <span class="flex items-baseline justify-between gap-2">
              <span class="truncate text-sm font-medium text-[var(--color-text)]">
                {{ pickXmsText(item.name, locale) || item.device }}
              </span>
              <span class="shrink-0 font-mono text-xs text-[var(--color-text-tertiary)]">{{ item.device }}</span>
            </span>
            <span v-if="item.latest" class="mt-1.5 flex flex-wrap items-center gap-x-2 gap-y-1 text-[11px] text-[var(--color-text-tertiary)]">
              <span class="rounded border border-[var(--color-border)] px-1.5 py-0.5 font-mono text-[10px] text-[var(--color-text-secondary)]">
                {{ item.latest.version }}
              </span>
              <span class="truncate font-mono">{{ item.latest.romVersion }}</span>
              <span v-if="item.latest.pushDate" class="tabular-nums">{{ item.latest.pushDate }}</span>
            </span>
          </NuxtLink>
        </div>
      </section>
    </template>
  </div>
</template>

<script setup>
import { pickXmsText } from '~/utils/xms'

// 支持以网址传参直达：/xms?pkg=<包名> 或 /xms?device=<机型代号>
definePageMeta({
  middleware: [
    (to) => {
      const pkg = typeof to.query.pkg === 'string' ? to.query.pkg.trim() : ''
      if (pkg) return `${to.path.replace(/\/+$/, '')}/${encodeURIComponent(pkg)}`
      const device = typeof to.query.device === 'string' ? to.query.device.trim() : ''
      if (device) return `${to.path.replace(/\/+$/, '')}/device/${encodeURIComponent(device)}`
    },
  ],
})

const { locale, t } = useI18n()
const { buildXmsIndexUrl } = useApi()

const { data, pending, error, refresh } = await useAsyncData('xms-index', () => $fetch(buildXmsIndexUrl()))

const apps = computed(() => data.value?.apps || [])
const devices = computed(() => data.value?.devices || [])
const versions = computed(() => data.value?.versions || [])
const generatedAt = computed(() => data.value?.generatedAt || '')

const query = ref('')
const keyword = computed(() => query.value.trim().toLowerCase())

const osFilter = ref('')
const regionFilter = ref('')
const seriesFilter = ref('')

const osOptions = computed(() => {
  const set = new Set()
  for (const app of apps.value) for (const os of app.os || []) set.add(os)
  for (const version of versions.value) if (version.os) set.add(version.os)
  return [...set].sort()
})

const regionOptions = computed(() => {
  const set = new Set()
  for (const app of apps.value) for (const region of app.regions || []) set.add(region)
  for (const item of devices.value) if (item.region) set.add(item.region)
  return [...set].sort()
})

const SERIES_ORDER = ['xiaomi', 'redmi', 'poco', 'tablet']
const SERIES_LABELS = { xiaomi: 'Xiaomi', redmi: 'Redmi', poco: 'POCO' }

const seriesOptions = computed(() => {
  const set = new Set()
  for (const app of apps.value) {
    for (const brand of app.brands || []) set.add(brand)
    if (app.tablet) set.add('tablet')
  }
  for (const item of devices.value) {
    for (const brand of item.brands || []) set.add(brand)
    if (item.tablet) set.add('tablet')
  }
  return SERIES_ORDER.filter((key) => set.has(key))
})

const seriesLabel = (key) => (key === 'tablet' ? t('xmsTablet') : SERIES_LABELS[key] || key)

const typeLabel = (type) =>
  type === 'phone' ? t('xmsTypePhone')
    : type === 'pad' ? t('xmsTypePad')
      : type === 'fold' ? t('xmsTypeFold')
        : type === 'universal' ? t('xmsTypeUniversal')
          : ''

const matches = (values) => {
  if (!keyword.value) return true
  return values
    .filter(Boolean)
    .join(' ')
    .toLowerCase()
    .includes(keyword.value)
}

const matchesOs = (item) => {
  if (!osFilter.value) return true
  const values = item.os || (item.latest?.os ? [item.latest.os] : [])
  return values.includes(osFilter.value)
}

const matchesRegion = (item) => {
  if (!regionFilter.value) return true
  const values = item.regions || (item.region ? [item.region] : [])
  return values.includes(regionFilter.value)
}

const matchesSeries = (item) => {
  if (!seriesFilter.value) return true
  if (seriesFilter.value === 'tablet') return Boolean(item.tablet)
  return (item.brands || []).includes(seriesFilter.value)
}

const filteredApps = computed(() =>
  apps.value.filter((app) =>
    matches([app.pkg, app.name?.zh, app.name?.en, app.apkName])
    && matchesOs(app)
    && matchesRegion(app)
    && matchesSeries(app)
  )
)

const filteredDevices = computed(() =>
  devices.value.filter((item) =>
    matches([item.device, item.name?.zh, item.name?.en, item.code])
    && matchesOs(item)
    && matchesRegion(item)
    && matchesSeries(item)
  )
)

const appPath = (pkg) => `/${locale.value}/xms/${encodeURIComponent(pkg)}`
const devicePath = (device) => `/${locale.value}/xms/device/${encodeURIComponent(device)}`

useHead({
  title: computed(() => `${t('xms')} - ${t('site')}`),
})
</script>
