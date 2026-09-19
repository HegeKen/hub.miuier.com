<template>
  <div class="container-page">
    <!-- Hero -->
    <section class="pb-14 pt-16 text-center sm:pb-16 sm:pt-24">
      <h1 class="text-center">
        <img
          :src="logoUrl"
          :alt="$t('site')"
          class="mx-auto h-24 w-auto sm:h-32"
          width="708"
          height="340"
        />
      </h1>
      <p class="mt-3 text-base text-[var(--color-text-secondary)]">
        {{ $t('devicesSub') }}
      </p>
    </section>

    <!-- Stats -->
    <section v-if="statistics" class="mx-auto mb-14 grid max-w-3xl grid-cols-2 gap-y-8 py-2 sm:grid-cols-4" aria-label="Statistics">
      <div class="text-center">
        <div class="text-3xl font-semibold tabular-nums">{{ totalDevices }}</div>
        <div class="mt-1 text-sm text-[var(--color-text-secondary)]">{{ $t('devices') }}</div>
      </div>
      <div class="text-center">
        <div class="text-3xl font-semibold tabular-nums">{{ totalBranches }}</div>
        <div class="mt-1 text-sm text-[var(--color-text-secondary)]">{{ $t('branches') }}</div>
      </div>
      <div class="text-center">
        <div class="text-3xl font-semibold tabular-nums">{{ totalRoms }}</div>
        <div class="mt-1 text-sm text-[var(--color-text-secondary)]">ROMs</div>
      </div>
      <div class="text-center">
        <div class="text-3xl font-semibold tabular-nums">{{ todayNewRoms }}</div>
        <div class="mt-1 text-sm text-[var(--color-text-secondary)]">{{ $t('todayNew') }}</div>
      </div>
    </section>

    <!-- Feedback -->
    <section class="mx-auto mb-10 flex flex-col items-center gap-3" :aria-label="$t('feedback')">
      <p class="text-center text-sm text-[var(--color-text-secondary)]">
        {{ $t('feedbackNote') }}
      </p>
      <div class="flex flex-wrap items-center justify-center gap-3">
        <a
          href="https://github.com/HegeKen/hub.miuier.com/issues"
          target="_blank"
          rel="noopener noreferrer"
          class="inline-flex items-center gap-2 rounded-lg border border-[var(--color-border)] bg-[var(--color-bg-surface)] px-4 py-2 text-sm font-medium text-[var(--color-text)] transition-colors hover:border-[var(--color-border-strong)] hover:bg-[var(--color-bg-subtle)]"
        >
          <svg viewBox="0 0 16 16" fill="currentColor" class="h-4 w-4" aria-hidden="true">
            <path d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27s1.36.09 2 .27c1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.01 8.01 0 0 0 16 8c0-4.42-3.58-8-8-8Z"/>
          </svg>
          {{ $t('feedbackIssues') }}
        </a>
        <a
          href="mailto:hegeken@foxmail.com"
          class="inline-flex items-center gap-2 rounded-lg border border-[var(--color-border)] bg-[var(--color-bg-surface)] px-4 py-2 text-sm font-medium text-[var(--color-text)] transition-colors hover:border-[var(--color-border-strong)] hover:bg-[var(--color-bg-subtle)]"
        >
          <svg viewBox="0 0 20 20" fill="currentColor" class="h-4 w-4" aria-hidden="true">
            <path d="M3 4a2 2 0 0 0-2 2v1.161l8.441 4.221a1.25 1.25 0 0 0 1.118 0L19 7.162V6a2 2 0 0 0-2-2H3Z"/>
            <path d="M19 8.839l-7.77 3.885a2.75 2.75 0 0 1-2.46 0L1 8.839V14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V8.839Z"/>
          </svg>
          {{ $t('feedbackEmail') }}
        </a>
        <button
          type="button"
          class="inline-flex items-center gap-2 rounded-lg border border-[var(--color-border)] bg-[var(--color-bg-surface)] px-4 py-2 text-sm font-medium text-[var(--color-text)] transition-colors hover:border-[var(--color-border-strong)] hover:bg-[var(--color-bg-subtle)]"
          @click="openDateQuery"
        >
          <svg viewBox="0 0 20 20" fill="currentColor" class="h-4 w-4" aria-hidden="true">
            <path fill-rule="evenodd" clip-rule="evenodd" d="M5.75 2a.75.75 0 0 1 .75.75V4h7V2.75a.75.75 0 0 1 1.5 0V4h.25A2.75 2.75 0 0 1 18 6.75v8.5A2.75 2.75 0 0 1 15.25 18H4.75A2.75 2.75 0 0 1 2 15.25v-8.5A2.75 2.75 0 0 1 4.75 4H5V2.75A.75.75 0 0 1 5.75 2Zm-1 5.5c-.69 0-1.25.56-1.25 1.25v6.5c0 .69.56 1.25 1.25 1.25h10.5c.69 0 1.25-.56 1.25-1.25v-6.5c0-.69-.56-1.25-1.25-1.25H4.75Z" />
          </svg>
          {{ $t('dateQuery') }}
        </button>
      </div>
    </section>

    <!-- Recent summary -->
    <section v-if="stats" class="mx-auto mb-10 max-w-xl text-center">
      <p class="text-sm text-[var(--color-text-secondary)]">
        {{ $t('recent7d', { count: stats.recentRoms.toLocaleString() }) }}
      </p>
    </section>

    <!-- Recent 7-day ROM updates -->
      <div class="rounded-xl border border-[var(--color-border)] bg-[var(--color-bg-surface)]"v-if="stats?.recent?.length" aria-label="Recent updates">
        <div class="flex items-center justify-between gap-3 border-b border-[var(--color-border)] px-5 py-3.5">
          <div class="min-w-0">
            <h2 class="text-sm font-semibold text-[var(--color-text)]">{{ $t('recent7dList') }}</h2>
            <p class="mt-0.5 text-xs tabular-nums text-[var(--color-text-tertiary)]">
              {{ $t('statsGeneratedAt') }} {{ generatedAtText }}
            </p>
          </div>
          <span class="shrink-0 rounded-full bg-[var(--color-accent-soft)] px-2.5 py-0.5 text-xs font-semibold tabular-nums text-[var(--color-accent)]">
            {{ stats.recentRoms.toLocaleString() }}
          </span>
        </div>
        <div>
          <!-- Mobile: compact list -->
          <div class="sm:hidden divide-y divide-[var(--color-border)]">
            <NuxtLink
              v-for="item in stats.recent"
              :key="item.device + ':' + item.version"
              :to="'/' + locale + '/devices/' + item.device + '#rom-' + item.version"
              class="flex items-center justify-between gap-3 px-4 py-3 transition-colors hover:bg-[var(--color-bg-subtle)]"
            >
              <div class="min-w-0">
                <div class="font-mono text-xs text-[var(--color-text)]">{{ item.version }}</div>
                <div class="mt-0.5 text-xs text-[var(--color-text-tertiary)]">
                  {{ item.name?.[localeKey] || item.name?.en || item.device }} ({{ item.device }})
                </div>
              </div>
              <span class="shrink-0 text-xs tabular-nums text-[var(--color-text-secondary)]">{{ item.release }}</span>
            </NuxtLink>
          </div>

          <!-- Desktop: full table -->
          <table class="hidden w-full text-sm sm:table">
            <thead>
              <tr class="border-b border-[var(--color-border)] text-start text-xs text-[var(--color-text-tertiary)]">
                <th class="px-5 py-2.5 font-medium">{{ $t('device') }}</th>
                <th class="px-5 py-2.5 font-medium">{{ $t('version') }}</th>
                <th class="px-5 py-2.5 font-medium">{{ $t('region') }}</th>
                <th class="px-5 py-2.5 font-medium">{{ $t('release') }}</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-[var(--color-border)]">
              <tr
                v-for="item in stats.recent"
                :key="item.device + ':' + item.version"
                class="transition-colors hover:bg-[var(--color-bg-subtle)]"
              >
                <td class="px-5 py-2.5">
                  <NuxtLink :to="'/' + locale + '/devices/' + item.device" class="group block">
                    <span class="font-medium text-[var(--color-text)] group-hover:text-[var(--color-accent)]">
                      {{ item.name?.[localeKey] || item.name?.en || item.device }}
                    </span>
                    <span class="block font-mono text-xs text-[var(--color-text-tertiary)]">{{ item.device }}</span>
                  </NuxtLink>
                </td>
                <td class="px-5 py-2.5 font-mono text-xs">
                  <NuxtLink
                    :to="'/' + locale + '/devices/' + item.device + '#rom-' + item.version"
                    class="text-[var(--color-text)] hover:text-[var(--color-accent)] hover:underline"
                  >
                    {{ item.version }}
                  </NuxtLink>
                </td>
                <td class="px-5 py-2.5 text-[var(--color-text-secondary)]">
                  <span class="rounded border border-[var(--color-border)] px-1.5 py-0.5 text-xs font-medium uppercase">
                    {{ item.region || '—' }}
                  </span>
                </td>
                <td class="px-5 py-2.5 tabular-nums text-[var(--color-text-secondary)]">{{ item.release }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

    <!-- 按发布日期查询 ROM -->
    <Teleport to="body">
      <div v-if="dateModal" class="fixed inset-0 z-50 flex items-center justify-center p-4" @click.self="closeDateQuery">
        <div class="fixed inset-0 bg-black/50" @click="closeDateQuery"></div>
        <div
          class="relative z-10 flex max-h-[85vh] w-full max-w-3xl flex-col overflow-hidden rounded-2xl border border-[var(--color-border)] bg-[var(--color-bg-surface)] shadow-2xl"
          role="dialog"
          aria-modal="true"
        >
          <!-- Header -->
          <div class="flex items-start justify-between gap-4 border-b border-[var(--color-border)] px-6 py-4">
            <div class="min-w-0">
              <h3 class="font-semibold text-[var(--color-text)]">{{ $t('dateQueryTitle') }}</h3>
              <p class="mt-0.5 text-xs text-[var(--color-text-tertiary)]">{{ $t('dateQueryHint') }}</p>
            </div>
            <button
              type="button"
              class="shrink-0 rounded-lg p-1.5 transition-colors hover:bg-[var(--color-bg-subtle)]"
              :aria-label="$t('close')"
              @click="closeDateQuery"
            >
              <svg class="h-5 w-5 text-[var(--color-text-tertiary)]" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" aria-hidden="true">
                <path stroke-linecap="round" stroke-linejoin="round" d="M6 18 18 6M6 6l12 12" />
              </svg>
            </button>
          </div>

          <!-- Date picker -->
          <div class="flex flex-wrap items-center gap-x-4 gap-y-2 border-b border-[var(--color-border)] px-6 py-4">
            <input
              v-model="queryDate"
              type="date"
              class="input-base w-auto"
              :min="releaseRange.min"
              :max="releaseRange.max"
              :aria-label="$t('dateQuery')"
            />
            <span v-if="dateLoading" class="spinner" role="status"></span>
            <span v-else-if="queryDate" class="text-xs tabular-nums text-[var(--color-text-secondary)]">
              {{ dateEntries.length ? $t('dateQueryCount', { count: dateEntries.length }) : $t('dateQueryEmpty') }}
            </span>
            <span v-if="releaseRange.min" class="text-xs tabular-nums text-[var(--color-text-tertiary)]">
              {{ $t('dateQueryRange', { min: releaseRange.min, max: releaseRange.max }) }}
            </span>
          </div>

          <!-- Results -->
          <div class="min-h-[10rem] overflow-y-auto">
            <div v-if="dateLoading" class="flex justify-center py-12">
              <span class="spinner" role="status"></span>
            </div>
            <p v-else-if="!queryDate" class="px-6 py-12 text-center text-sm text-[var(--color-text-tertiary)]">
              {{ $t('dateQueryHint') }}
            </p>
            <p v-else-if="dateEntries.length === 0" class="px-6 py-12 text-center text-sm text-[var(--color-text-tertiary)]">
              {{ $t('dateQueryEmpty') }}
            </p>
            <template v-else>
              <!-- Mobile: compact list -->
              <div class="divide-y divide-[var(--color-border)] sm:hidden">
                <NuxtLink
                  v-for="item in dateEntries"
                  :key="item.device + ':' + item.version"
                  :to="'/' + locale + '/devices/' + item.device + '#rom-' + item.version"
                  class="flex items-center justify-between gap-3 px-4 py-3 transition-colors hover:bg-[var(--color-bg-subtle)]"
                >
                  <div class="min-w-0">
                    <div class="font-mono text-xs text-[var(--color-text)]">{{ item.version }}</div>
                    <div class="mt-0.5 text-xs text-[var(--color-text-tertiary)]">
                      {{ deviceLabel(item.device) }} ({{ item.device }})
                    </div>
                  </div>
                  <span class="shrink-0 rounded border border-[var(--color-border)] px-1.5 py-0.5 text-xs font-medium uppercase text-[var(--color-text-secondary)]">
                    {{ item.region || '—' }}
                  </span>
                </NuxtLink>
              </div>

              <!-- Desktop: full table -->
              <table class="hidden w-full text-sm sm:table">
                <thead>
                  <tr class="border-b border-[var(--color-border)] text-start text-xs text-[var(--color-text-tertiary)]">
                    <th class="px-6 py-2.5 font-medium">{{ $t('device') }}</th>
                    <th class="px-6 py-2.5 font-medium">{{ $t('version') }}</th>
                    <th class="px-6 py-2.5 font-medium">{{ $t('region') }}</th>
                  </tr>
                </thead>
                <tbody class="divide-y divide-[var(--color-border)]">
                  <tr
                    v-for="item in dateEntries"
                    :key="item.device + ':' + item.version"
                    class="transition-colors hover:bg-[var(--color-bg-subtle)]"
                  >
                    <td class="px-6 py-2.5">
                      <NuxtLink :to="'/' + locale + '/devices/' + item.device" class="group block" @click="closeDateQuery">
                        <span class="font-medium text-[var(--color-text)] group-hover:text-[var(--color-accent)]">
                          {{ deviceLabel(item.device) }}
                        </span>
                        <span class="block font-mono text-xs text-[var(--color-text-tertiary)]">{{ item.device }}</span>
                      </NuxtLink>
                    </td>
                    <td class="px-6 py-2.5 font-mono text-xs">
                      <NuxtLink
                        :to="'/' + locale + '/devices/' + item.device + '#rom-' + item.version"
                        class="text-[var(--color-text)] hover:text-[var(--color-accent)] hover:underline"
                        @click="closeDateQuery"
                      >
                        {{ item.version }}
                      </NuxtLink>
                    </td>
                    <td class="px-6 py-2.5">
                      <span class="rounded border border-[var(--color-border)] px-1.5 py-0.5 text-xs font-medium uppercase text-[var(--color-text-secondary)]">
                        {{ item.region || '—' }}
                      </span>
                    </td>
                  </tr>
                </tbody>
              </table>
            </template>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import logoUrl from '~/assets/images/words.svg'

const { locale } = useI18n()
const { t } = useI18n()
const { buildStatsUrl, buildStatisticsUrl, buildDevicesIndexUrl, buildReleasesIndexUrl, buildReleasesUrl } = useApi()

// 数据里的多语言字段是 'zh' / 'en'，而 locale 是 'zh-cn' / 'en-us'
const localeKey = computed(() => (locale.value.startsWith('zh') ? 'zh' : 'en'))

// 近 7 日更新的 ROM 版本列表（由 generate-index.mjs 生成 v3/stats.json）
// cache: 'no-store' 绕过浏览器对旧版 stats.json 的缓存
const { data: stats } = await useAsyncData(
  'devices-stats',
  () => $fetch(buildStatsUrl(), { cache: 'no-store' }).catch(() => null)
)

// 顶部统计卡片数据来自数据库真实统计（v3/statistics.json，口径与后台一致）
const { data: statistics } = await useAsyncData(
  'devices-statistics',
  () => $fetch(buildStatisticsUrl(), { cache: 'no-store' }).catch(() => null)
)

const totalDevices = computed(() => statistics.value?.deviceCount || 0)
const totalBranches = computed(() => statistics.value?.branchCount || 0)
const totalRoms = computed(() => statistics.value?.romCount || 0)
const todayNewRoms = computed(() => statistics.value?.todayNewRoms || 0)

// stats.json 的生成时间，格式化为本地时区 YYYY-MM-DD HH:mm:ss
const generatedAtText = computed(() => {
  const ts = stats.value?.generatedAt
  if (!ts) return ''
  const d = new Date(ts)
  if (Number.isNaN(d.getTime())) return ''
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
})

// ---- 按发布日期查询 ROM ----
// 数据来自 v3/releases/：index.json 提供日期范围，<year>.json 提供该年按日期分组的 ROM。
// 年份分片按需加载并在内存缓存，避免一次拉取全部 5 万+ ROM。
const dateModal = ref(false)
const queryDate = ref('')
const dateLoading = ref(false)
const dateEntries = ref([])
const deviceNameMap = ref(null)          // 设备代号 -> { zh, en }
const yearCache = new Map()              // '2026' -> { '2026-09-18': [{ device, version, region }] }
let dateRequestId = 0

const { data: releaseIndex } = await useAsyncData(
  'releases-index',
  () => $fetch(buildReleasesIndexUrl(), { cache: 'no-store' }).catch(() => null)
)

const releaseRange = computed(() => ({
  min: releaseIndex.value?.minDate || '',
  max: releaseIndex.value?.maxDate || '',
}))

// 设备名称（zh/en）按需从设备索引读取，避免在发布索引里重复存储名称
const deviceLabel = (codename) => {
  const name = deviceNameMap.value?.[codename]
  return name?.[localeKey.value] || name?.en || name?.zh || codename
}

const loadDeviceNames = async () => {
  if (deviceNameMap.value) return
  const list = await $fetch(buildDevicesIndexUrl(), { cache: 'no-store' }).catch(() => null)
  if (!Array.isArray(list)) return
  const map = {}
  for (const d of list) {
    if (d?.device) map[d.device] = d.name || {}
  }
  deviceNameMap.value = map
}

const loadYear = async (year) => {
  if (yearCache.has(year)) return yearCache.get(year)
  const data = await $fetch(buildReleasesUrl(year), { cache: 'no-store' }).catch(() => null)
  const dates = data?.dates || {}
  yearCache.set(year, dates)
  return dates
}

// 日期变化时加载对应年份分片；用请求序号丢弃过期响应，避免快速改日期时结果错位
watch(queryDate, async (value) => {
  const requestId = ++dateRequestId
  if (!value) {
    dateEntries.value = []
    dateLoading.value = false
    return
  }
  dateLoading.value = true
  try {
    const [dates] = await Promise.all([loadYear(value.slice(0, 4)), loadDeviceNames()])
    if (requestId !== dateRequestId) return
    dateEntries.value = dates[value] || []
  } finally {
    if (requestId === dateRequestId) dateLoading.value = false
  }
})

const todayString = () => {
  const now = new Date()
  const pad = (n) => String(n).padStart(2, '0')
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`
}

const openDateQuery = () => {
  dateModal.value = true
  if (queryDate.value) return
  // 默认选中数据中最新的一天；若数据比今天更新则退回今天
  const max = releaseRange.value.max
  const today = todayString()
  queryDate.value = max && max < today ? max : today
}

const closeDateQuery = () => {
  dateModal.value = false
}

const onDateQueryKeydown = (e) => {
  if (e.key === 'Escape' && dateModal.value) closeDateQuery()
}
onMounted(() => document.addEventListener('keydown', onDateQueryKeydown))
onUnmounted(() => document.removeEventListener('keydown', onDateQueryKeydown))

useHead({
  title: `${t('site')} - ${t('devicesSub')}`,
})
</script>
