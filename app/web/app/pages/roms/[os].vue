<template>
  <div class="container-page py-10 sm:py-14">
    <!-- Loading -->
    <div v-if="pending" class="flex justify-center py-20">
      <span class="spinner" role="status" aria-label="Loading"></span>
    </div>

    <!-- Invalid OS -->
    <div v-else-if="invalidOs" class="py-20 text-center">
      <p class="text-sm text-[var(--color-text-secondary)]">{{ $t('roms') }}</p>
      <NuxtLink
        :to="'/' + locale + '/roms'"
        class="mt-3 inline-block text-sm font-medium text-[var(--color-accent)] hover:underline"
      >
        {{ $t('roms') }}
      </NuxtLink>
    </div>

    <!-- Content -->
    <template v-else>
      <!-- Breadcrumb -->
      <nav class="mb-8 text-sm" aria-label="Breadcrumb">
        <ol class="flex flex-wrap items-center gap-1.5 text-[var(--color-text-tertiary)]">
          <li>
            <NuxtLink :to="'/' + locale" class="transition-colors hover:text-[var(--color-text)]">
              {{ $t('home') }}
            </NuxtLink>
          </li>
          <li aria-hidden="true">/</li>
          <li>
            <NuxtLink :to="'/' + locale + '/roms'" class="transition-colors hover:text-[var(--color-text)]">
              {{ $t('roms') }}
            </NuxtLink>
          </li>
          <li aria-hidden="true">/</li>
          <li class="truncate font-mono font-medium text-[var(--color-text)]">{{ osLabel(os) }}</li>
        </ol>
      </nav>

      <!-- Title -->
      <header class="mb-8">
        <h1 class="font-mono text-2xl font-semibold tracking-tight sm:text-3xl">
          {{ osLabel(os) }}
        </h1>
        <p class="mt-2 text-sm text-[var(--color-text-secondary)]">
          {{ $t('totalRoms', { count: roms?.length || 0 }) }} · {{ $t('totalDevices', { count: deviceGroups.length }) }}
        </p>
      </header>

      <!-- OS Version Switcher -->
      <div class="mb-6 flex flex-wrap gap-2">
        <NuxtLink
          v-for="item in osIndex"
          :key="item.os"
          :to="'/' + locale + '/roms/' + item.os"
          class="filter-pill-sm"
          :class="item.os === os ? 'filter-pill-sm-active' : ''"
        >
          {{ osLabel(item.os) }}
        </NuxtLink>
      </div>

      <!-- Search -->
      <div class="mb-5 max-w-xl">
        <div class="relative">
          <svg
            class="pointer-events-none absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-[var(--color-text-tertiary)]"
            xmlns="http://www.w3.org/2000/svg"
            fill="none"
            viewBox="0 0 24 24"
            stroke-width="1.5"
            stroke="currentColor"
            aria-hidden="true"
          >
            <path stroke-linecap="round" stroke-linejoin="round" d="m21 21-5.197-5.197m0 0A7.5 7.5 0 1 0 5.196 5.196a7.5 7.5 0 0 0 10.607 10.607Z" />
          </svg>
          <input v-model="searchQuery" type="search" :placeholder="$t('searchPlaceholder')" class="input-base pl-10" />
        </div>
      </div>

      <!-- Zone Filter -->
      <div class="mb-8 flex flex-wrap items-center gap-2">
        <span class="text-xs font-medium text-[var(--color-text-secondary)]">{{ $t('region') }}:</span>
        <button type="button" class="filter-pill-sm" :class="selectedZone === '' ? 'filter-pill-sm-active' : ''" @click="selectedZone = ''">
          {{ $t('allRoms') }}
        </button>
        <button type="button" class="filter-pill-sm" :class="selectedZone === '1' ? 'filter-pill-sm-active' : ''" @click="selectedZone = '1'">
          {{ $t('china') }}
        </button>
        <button type="button" class="filter-pill-sm" :class="selectedZone === '2' ? 'filter-pill-sm-active' : ''" @click="selectedZone = '2'">
          {{ $t('global') }}
        </button>
      </div>

      <!-- Empty -->
      <div v-if="deviceGroups.length === 0" class="py-20 text-center">
        <p class="text-sm text-[var(--color-text-tertiary)]">{{ $t('noResults') }}</p>
      </div>

      <!-- Device Groups & ROMs -->
      <section class="space-y-3 pb-4">
        <div
          v-for="group in pagedGroups"
          :key="group.device"
          class="overflow-hidden rounded-xl border border-[var(--color-border)] bg-[var(--color-bg-surface)]"
        >
          <!-- Device Header -->
          <div class="flex items-center justify-between gap-4 border-b border-[var(--color-border)] px-4 py-3 sm:px-5">
            <div class="min-w-0">
              <h2 class="truncate font-medium text-[var(--color-text)]">{{ deviceName(group) }}</h2>
              <p class="mt-0.5 truncate font-mono text-xs text-[var(--color-text-tertiary)]">{{ group.device }}</p>
            </div>
            <span class="shrink-0 text-xs text-[var(--color-text-tertiary)]">
              {{ $t('totalRoms', { count: group.roms.length }) }}
            </span>
          </div>

          <!-- Mobile: simplified list -->
          <div class="sm:hidden divide-y divide-[var(--color-border)]">
            <div v-for="rom in group.visible" :key="rom.version" class="px-4 py-3">
              <div class="font-mono text-xs text-[var(--color-text)]">{{ rom.version }}</div>
              <div class="mt-1 flex flex-wrap items-center gap-2 text-xs text-[var(--color-text-tertiary)]">
                <span v-if="rom.release" class="tabular-nums">{{ rom.release }}</span>
                <span v-if="rom.android" class="rounded border border-[var(--color-border)] px-1 py-0.5 text-[10px] uppercase">{{ rom.android }}</span>
                <span v-if="zoneLabel(rom)">{{ zoneLabel(rom) }}</span>
              </div>
              <div class="mt-2 flex flex-wrap items-center gap-3">
                <a v-if="rom.recovery" :href="buildDownloadLink(rom.version, rom.recovery)" class="text-xs font-medium text-[var(--color-accent)] hover:underline" target="_blank" rel="noopener noreferrer">
                  {{ $t('recovery') }}
                </a>
                <a v-if="rom.fastboot" :href="buildDownloadLink(rom.version, rom.fastboot)" class="text-xs font-medium text-[var(--color-accent)] hover:underline" target="_blank" rel="noopener noreferrer">
                  {{ $t('fastboot') }}
                </a>
                <button
                  type="button"
                  class="inline-flex items-center gap-1 text-xs font-medium text-[var(--color-text-tertiary)] transition-colors hover:text-[var(--color-text)]"
                  @click="openRomModal(rom)"
                >
                  {{ $t('logs') }}
                </button>
              </div>
            </div>
          </div>

          <!-- Desktop: full table -->
          <div class="hidden overflow-x-auto sm:block">
            <table class="w-full text-sm">
              <thead>
                <tr class="border-b border-[var(--color-border)] text-left text-xs text-[var(--color-text-tertiary)]">
                  <th class="px-4 py-2.5 font-medium sm:px-5">#</th>
                  <th class="px-4 py-2.5 font-medium">{{ $t('version') }}</th>
                  <th class="px-4 py-2.5 font-medium">{{ $t('android') }}</th>
                  <th class="px-4 py-2.5 font-medium">{{ $t('region') }}</th>
                  <th class="px-4 py-2.5 font-medium">{{ $t('release') }}</th>
                  <th class="px-4 py-2.5 font-medium">{{ $t('aspatch') }}</th>
                  <th class="px-4 py-2.5 font-medium">{{ $t('recovery') }}</th>
                  <th class="px-4 py-2.5 font-medium">{{ $t('fastboot') }}</th>
                  <th class="px-4 py-2.5 font-medium">{{ $t('changelog') }}</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-[var(--color-border)]">
                <tr v-for="(rom, index) in group.visible" :key="rom.version" class="transition-colors hover:bg-[var(--color-bg-subtle)]">
                  <td class="px-4 py-2.5 text-[var(--color-text-tertiary)] sm:px-5">{{ group.total - (group.start + index) }}</td>
                  <td class="px-4 py-2.5 font-mono text-xs">{{ rom.version }}</td>
                  <td class="px-4 py-2.5 tabular-nums text-[var(--color-text-secondary)]">{{ rom.android || $t('na') }}</td>
                  <td class="px-4 py-2.5 text-xs text-[var(--color-text-secondary)]">{{ zoneLabel(rom) || $t('na') }}</td>
                  <td class="px-4 py-2.5 tabular-nums text-[var(--color-text-secondary)]">{{ rom.release || $t('na') }}</td>
                  <td class="px-4 py-2.5 tabular-nums text-[var(--color-text-secondary)]">{{ rom.aspatch || $t('na') }}</td>
                  <td class="px-4 py-2.5">
                    <a v-if="rom.recovery" :href="buildDownloadLink(rom.version, rom.recovery)" class="font-medium text-[var(--color-accent)] hover:underline" target="_blank" rel="noopener noreferrer">
                      {{ $t('download') }}
                    </a>
                    <span v-else class="text-[var(--color-text-tertiary)]">{{ $t('na') }}</span>
                  </td>
                  <td class="px-4 py-2.5">
                    <a v-if="rom.fastboot" :href="buildDownloadLink(rom.version, rom.fastboot)" class="font-medium text-[var(--color-accent)] hover:underline" target="_blank" rel="noopener noreferrer">
                      {{ $t('download') }}
                    </a>
                    <span v-else class="text-[var(--color-text-tertiary)]">{{ $t('na') }}</span>
                  </td>
                  <td class="px-4 py-2.5">
                    <button
                      type="button"
                      class="inline-flex items-center gap-1.5 text-xs font-medium text-[var(--color-text-tertiary)] transition-colors hover:text-[var(--color-text)]"
                      @click="openRomModal(rom)"
                    >
                      <svg
                        class="h-3.5 w-3.5"
                        xmlns="http://www.w3.org/2000/svg"
                        fill="none"
                        viewBox="0 0 24 24"
                        stroke-width="1.5"
                        stroke="currentColor"
                        aria-hidden="true"
                      >
                        <path stroke-linecap="round" stroke-linejoin="round" d="M19.5 14.25v-2.625a3.375 3.375 0 0 0-3.375-3.375h-1.5A1.125 1.125 0 0 1 13.5 7.125v-1.5a3.375 3.375 0 0 0-3.375-3.375H8.25m0 12.75h7.5m-7.5 3H12M10.5 2.25H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 0 0-9-9Z" />
                      </svg>
                      {{ $t('logs') }}
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <!-- Pagination -->
          <div v-if="group.pageCount > 1" class="flex items-center justify-between gap-4 border-t border-[var(--color-border)] px-4 py-3 sm:px-5">
            <button
              type="button"
              class="filter-pill-sm disabled:pointer-events-none disabled:opacity-50"
              :disabled="group.page <= 1"
              @click="gotoPage(group.device, group.page - 1)"
            >
              {{ $t('prev') }}
            </button>
            <span class="text-xs tabular-nums text-[var(--color-text-secondary)]">
              {{ $t('pageXofY', { page: group.page, pages: group.pageCount }) }}
            </span>
            <button
              type="button"
              class="filter-pill-sm disabled:pointer-events-none disabled:opacity-50"
              :disabled="group.page >= group.pageCount"
              @click="gotoPage(group.device, group.page + 1)"
            >
              {{ $t('next') }}
            </button>
          </div>
        </div>
      </section>
    </template>

    <!-- ROM Detail Modal -->
    <Teleport to="body">
      <div v-if="romModal" class="fixed inset-0 z-50 flex items-center justify-center p-4" @click.self="romModal = null">
        <div class="fixed inset-0 bg-black/50" @click="romModal = null"></div>
        <div class="relative z-10 w-full max-w-3xl max-h-[85vh] overflow-y-auto rounded-2xl border border-[var(--color-border)] bg-[var(--color-bg-surface)] shadow-2xl">
          <!-- Header -->
          <div class="sticky top-0 flex items-center justify-between border-b border-[var(--color-border)] bg-[var(--color-bg-surface)] px-6 py-4">
            <div>
              <h3 class="font-semibold text-[var(--color-text)]">{{ deviceName({ name: romModal?.name }) }}</h3>
              <p class="mt-0.5 font-mono text-xs text-[var(--color-text-tertiary)]">{{ romModal?.version }}</p>
            </div>
            <button type="button" class="rounded-lg p-1.5 transition-colors hover:bg-[var(--color-bg-subtle)]" @click="romModal = null">
              <svg class="h-5 w-5 text-[var(--color-text-tertiary)]" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" d="M6 18 18 6M6 6l12 12" />
              </svg>
            </button>
          </div>

          <div class="px-6 py-5 space-y-5">
            <!-- Info Grid -->
            <dl class="grid grid-cols-1 sm:grid-cols-2 gap-3 text-sm">
              <div class="rounded-lg bg-[var(--color-bg-subtle)] px-3 py-2.5">
                <dt class="text-xs text-[var(--color-text-secondary)]">{{ $t('version') }}</dt>
                <dd class="mt-0.5 font-mono text-xs break-all text-[var(--color-text)]">{{ romModal?.version }}</dd>
              </div>
              <div class="rounded-lg bg-[var(--color-bg-subtle)] px-3 py-2.5">
                <dt class="text-xs text-[var(--color-text-secondary)]">{{ $t('android') }}</dt>
                <dd class="mt-0.5 text-xs text-[var(--color-text)]">{{ romModal?.android || $t('na') }}</dd>
              </div>
              <div class="rounded-lg bg-[var(--color-bg-subtle)] px-3 py-2.5">
                <dt class="text-xs text-[var(--color-text-secondary)]">{{ $t('release') }}</dt>
                <dd class="mt-0.5 text-xs text-[var(--color-text)]">{{ romModal?.release || $t('na') }}</dd>
              </div>
              <div class="rounded-lg bg-[var(--color-bg-subtle)] px-3 py-2.5">
                <dt class="text-xs text-[var(--color-text-secondary)]">{{ $t('aspatch') }}</dt>
                <dd class="mt-0.5 text-xs text-[var(--color-text)]">{{ romModal?.aspatch || $t('na') }}</dd>
              </div>
            </dl>

            <!-- Downloads -->
            <div>
              <h4 class="mb-2 text-xs font-semibold uppercase tracking-wide text-[var(--color-text-secondary)]">{{ $t('download') }}</h4>
              <div class="flex flex-col gap-1.5">
                <a
                  v-if="romModal?.recovery"
                  :href="buildDownloadLink(romModal.version, romModal.recovery)"
                  class="inline-flex items-center gap-2 rounded-lg bg-[var(--color-accent)] px-3 py-2 text-sm font-medium text-white transition-colors hover:opacity-90"
                  target="_blank" rel="noopener noreferrer"
                >
                  <svg class="h-4 w-4" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" d="M3 16.5v2.25A2.25 2.25 0 0 0 5.25 21h13.5A2.25 2.25 0 0 0 21 18.75V16.5M16.5 12 12 16.5m0 0L7.5 12m4.5 4.5V3" /></svg>
                  {{ $t('recovery') }}
                </a>
                <a
                  v-if="romModal?.fastboot"
                  :href="buildDownloadLink(romModal.version, romModal.fastboot)"
                  class="inline-flex items-center gap-2 rounded-lg border border-[var(--color-border)] px-3 py-2 text-sm font-medium text-[var(--color-text)] transition-colors hover:bg-[var(--color-bg-subtle)]"
                  target="_blank" rel="noopener noreferrer"
                >
                  <svg class="h-4 w-4" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" d="M3 16.5v2.25A2.25 2.25 0 0 0 5.25 21h13.5A2.25 2.25 0 0 0 21 18.75V16.5M16.5 12 12 16.5m0 0L7.5 12m4.5 4.5V3" /></svg>
                  {{ $t('fastboot') }}
                </a>
              </div>
            </div>

            <!-- Changelog -->
            <div v-if="romModalLoading" class="flex justify-center py-4">
              <span class="spinner" role="status"></span>
            </div>
            <div v-else-if="romModalLogs" class="space-y-4">
              <h4 class="text-xs font-semibold uppercase tracking-wide text-[var(--color-text-secondary)]">{{ $t('logs') }}</h4>
              <div v-for="(items, category) in romModalLogs" :key="category">
                <h5 class="mb-1 text-xs font-semibold text-[var(--color-text)]">{{ category }}</h5>
                <ul class="space-y-1 text-sm text-[var(--color-text)]">
                  <li v-for="(item, i) in items" :key="i" class="flex gap-2">
                    <span class="mt-[7px] h-1 w-1 shrink-0 rounded-full bg-[var(--color-text-tertiary)]" aria-hidden="true"></span>
                    <span>{{ item }}</span>
                  </li>
                </ul>
              </div>
            </div>
            <p v-else class="text-sm text-[var(--color-text-tertiary)]">{{ $t('noLogs') }}</p>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
const route = useRoute()
const { locale } = useI18n()
const { t } = useI18n()
const { buildRomsUrl, buildRomsIndexUrl, buildDownloadLink, buildChangelogUrl } = useApi()

const searchQuery = ref('')
const selectedZone = ref('')

// ROM 详情模态框（含更新日志）
const romModal = ref(null)
const romModalLogs = ref(null)
const romModalLoading = ref(false)
const openRomModal = async (rom) => {
  romModal.value = rom
  romModalLogs.value = null
  romModalLoading.value = true
  try {
    let data = null
    const url = buildChangelogUrl(rom.device, rom.region, rom.version)
    try {
      data = await $fetch(url)
    } catch {
      if (rom.region) {
        data = await $fetch(buildChangelogUrl(rom.device, '', rom.version))
      }
    }
    const raw = data || {}
    romModalLogs.value = locale.value.startsWith('en')
      ? (raw.logs_en || raw.logs_zh || null)
      : (raw.logs_zh || raw.logs_en || null)
  } catch {
    romModalLogs.value = null
  } finally {
    romModalLoading.value = false
  }
}
const closeRomModal = () => { romModal.value = null }
const onKeydown = (e) => {
  if (e.key === 'Escape' && romModal.value) closeRomModal()
}
onMounted(() => document.addEventListener('keydown', onKeydown))
onUnmounted(() => document.removeEventListener('keydown', onKeydown))

// 响应式视口判断：Tailwind `sm` 断点为 640px；移动端每页 10 条、桌面端每页 20 条
const isDesktop = ref(false)
const updateViewport = () => {
  if (typeof window === 'undefined') return
  isDesktop.value = window.matchMedia('(min-width: 640px)').matches
}
onMounted(() => {
  updateViewport()
  window.addEventListener('resize', updateViewport)
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', updateViewport)
})
const pageSize = computed(() => (isDesktop.value ? 20 : 10))

// 每台设备各自的当前页（按设备代号存储），数据变化时重置回第 1 页
const currentPage = ref({})
const gotoPage = (device, page) => {
  currentPage.value = { ...currentPage.value, [device]: page }
}

// 兼容数据库 bigver 导出的完整大版本格式：OS4 / V14 / V12.5 / V816 / Stock / STAN 等
const OS_PATTERN = /^(OS\d+|V\d+(\.\d+)?|STAN)$/
const os = computed(() => {
  const param = route.params.os
  const value = Array.isArray(param) ? param[0] : param
  return value || ''
})
// 合法性优先以 roms/index.json 返回的实际大版本列表为准（权威来源），仅在其未加载时回退到正则
const validOsSet = computed(() => new Set((osIndex.value || []).map((item) => item.os)))
const invalidOs = computed(() => {
  if (!os.value) return false
  if (validOsSet.value.size > 0) return !validOsSet.value.has(os.value)
  return !OS_PATTERN.test(os.value)
})

// Localized name: data keys are 'zh' / 'en' while locale is 'zh-cn' / 'en-us'
const localeKey = computed(() => (locale.value.startsWith('zh') ? 'zh' : 'en'))
const deviceName = (group) =>
  group.name?.[localeKey.value] ||
  group.name?.en ||
  group.name?.zh ||
  group.device
const zoneLabel = (rom) =>
  rom.branchName?.[localeKey.value] ||
  rom.branchName?.en ||
  rom.branchName?.zh ||
  (rom.zone === '1' ? t('china') : rom.zone === '2' ? t('global') : '')

// ROM 排序：版本号为主（降序），release_date 为辅（降序、空值置后）
const romVersionParts = (version) => {
  const m = String(version || '').match(/^[A-Za-z]*(\d+(?:\.\d+)*)/)
  return m ? m[1].split('.').map((n) => parseInt(n, 10) || 0) : []
}
const compareRoms = (a, b) => {
  const va = romVersionParts(a.version)
  const vb = romVersionParts(b.version)
  const len = Math.max(va.length, vb.length)
  for (let i = 0; i < len; i++) {
    const x = va[i] || 0
    const y = vb[i] || 0
    if (x !== y) return y - x
  }
  const ra = a.release || ''
  const rb = b.release || ''
  if (ra !== rb) {
    if (!ra) return 1
    if (!rb) return -1
    return ra < rb ? 1 : -1
  }
  return 0
}

const { data: osIndex } = await useAsyncData('roms-index', () => $fetch(buildRomsIndexUrl()))

// OS 名称本地化：Stock 中文「原生安卓」、STAN 中文「现代原生安卓」，其余大版本编码保持不变
const osLabel = (os) =>
  os === 'Stock' && locale.value.startsWith('zh')
    ? t('osStock')
    : os === 'STAN' && locale.value.startsWith('zh')
      ? t('osStan')
      : os

const { data: roms, pending, error, refresh } = await useAsyncData(
  'roms-' + os.value,
  () => $fetch(buildRomsUrl(os.value)),
  { watch: [os], default: () => [] }
)

// 当 OS / 搜索 / 区域 / 数据变化时，重置所有设备的当前页回第 1 页
watch([os, searchQuery, selectedZone, roms], () => {
  currentPage.value = {}
})

const deviceGroups = computed(() => {
  if (!roms.value || !Array.isArray(roms.value)) return []
  let pool = roms.value
  if (selectedZone.value) {
    pool = pool.filter((r) => r.zone === selectedZone.value)
  }
  if (searchQuery.value) {
    const q = searchQuery.value.toLowerCase()
    pool = pool.filter(
      (r) =>
        r.device.toLowerCase().includes(q) ||
        (r.name?.zh || '').toLowerCase().includes(q) ||
        (r.name?.en || '').toLowerCase().includes(q) ||
        (r.version || '').toLowerCase().includes(q)
    )
  }

  const groups = new Map()
  for (const rom of pool) {
    if (!groups.has(rom.device)) {
      groups.set(rom.device, { device: rom.device, name: rom.name, brand: rom.brand, roms: [] })
    }
    groups.get(rom.device).roms.push(rom)
  }
  return Array.from(groups.values()).map((group) => ({
    ...group,
    roms: [...group.roms].sort(compareRoms),
  }))
})

// 每台设备内部按页切分 ROM：超过阈值时启用分页，否则一次性展示全部
const pagedGroups = computed(() =>
  deviceGroups.value.map((group) => {
    const total = group.roms.length
    const size = pageSize.value
    const pageCount = Math.max(1, Math.ceil(total / size))
    const page = Math.min(currentPage.value[group.device] || 1, pageCount)
    const start = (page - 1) * size
    return {
      ...group,
      total,
      page,
      pageCount,
      start,
      visible: group.roms.slice(start, start + size),
    }
  })
)

useHead({
  title: computed(() => {
    if (invalidOs.value) return t('roms')
    return `${osLabel(os.value)} - ${t('site')}`
  }),
})
</script>
