<template>
  <div class="container-page py-10 sm:py-14">
    <!-- 返回 -->
    <NuxtLink
      :to="`/${locale}/dev`"
      class="mb-5 inline-flex items-center gap-1.5 text-sm text-[var(--color-text-secondary)] transition-colors hover:text-[var(--color-text)]"
    >
      <svg class="dir-flip h-4 w-4" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" aria-hidden="true">
        <path stroke-linecap="round" stroke-linejoin="round" d="M15.75 19.5 8.25 12l7.5-7.5" />
      </svg>
      {{ $t('devTitle') }}
    </NuxtLink>

    <div v-if="pending" class="flex justify-center py-20">
      <span class="spinner" role="status" aria-label="Loading"></span>
    </div>

    <div v-else-if="error || !week" class="py-20 text-center">
      <p class="text-sm text-[var(--color-text-secondary)]">{{ $t('devNoLogs') }}</p>
    </div>

    <template v-else>
      <header class="mb-6">
        <div class="flex flex-wrap items-center gap-2">
          <h1 class="text-2xl font-semibold tracking-tight sm:text-3xl">
            {{ $t('devWeek', { week: week.week }) }}
          </h1>
          <span v-if="week.bigver" class="rounded-full border border-[var(--color-border)] px-2.5 py-0.5 text-xs text-[var(--color-text-secondary)]">
            {{ week.bigver }}
          </span>
          <span v-if="week.region !== 'cn'" class="rounded-full border border-[var(--color-border)] px-2.5 py-0.5 text-xs text-[var(--color-text-secondary)]">
            {{ $t('devRegionGlobal') }}
          </span>
        </div>
        <p class="mt-2 text-sm text-[var(--color-text)]">{{ localized(week.title) }}</p>
        <p class="mt-1 font-mono text-xs text-[var(--color-text-tertiary)]" :title="week.versions">
          {{ versionsLabel }}<template v-if="week.release"> · {{ week.release }}</template>
        </p>

        <!-- 国际版 / 大陆版切换 -->
        <div v-if="availableRegions.length > 1" class="mt-3 flex flex-wrap gap-2">
          <button
            v-for="item in availableRegions"
            :key="item"
            type="button"
            class="filter-pill"
            :class="region === item ? 'filter-pill-active' : ''"
            @click="region = item"
          >
            {{ item === 'cn' ? $t('devRegionCn') : $t('devRegionGlobal') }}
          </button>
        </div>
      </header>

      <!-- 升级说明 / 公告正文 -->
      <section v-if="attention.length" class="mb-6 rounded-xl border border-[var(--color-border)] bg-[var(--color-bg-surface)] p-4">
        <h2 class="mb-2 text-sm font-semibold text-[var(--color-text)]">{{ $t('devAttention') }}</h2>
        <ul class="space-y-1.5">
          <li v-for="(item, i) in attention" :key="i" class="flex gap-2 text-sm text-[var(--color-text-secondary)]">
            <span class="mt-2 h-1 w-1 shrink-0 rounded-full bg-[var(--color-text-tertiary)]" aria-hidden="true"></span>
            <span>{{ localized(item) }}</span>
          </li>
        </ul>
      </section>

      <section v-if="description.length" class="mb-6 rounded-xl border border-[var(--color-border)] bg-[var(--color-bg-surface)] p-4">
        <h2 class="mb-2 text-sm font-semibold text-[var(--color-text)]">{{ $t('devDescription') }}</h2>
        <ul class="space-y-1.5">
          <li v-for="(item, i) in description" :key="i" class="flex gap-2 text-sm text-[var(--color-text-secondary)]">
            <span class="mt-2 h-1 w-1 shrink-0 rounded-full bg-[var(--color-text-tertiary)]" aria-hidden="true"></span>
            <span>{{ localized(item) }}</span>
          </li>
        </ul>
      </section>

      <!-- 更新日志 -->
      <section class="mb-8">
        <h2 class="mb-3 text-sm font-semibold text-[var(--color-text)]">{{ $t('changelog') }}</h2>
        <div v-if="logs && Object.keys(logs).length" class="space-y-4">
          <div
            v-for="(entries, module) in logs"
            :key="module"
            class="rounded-xl border border-[var(--color-border)] bg-[var(--color-bg-surface)] p-4"
          >
            <h3 class="mb-2 text-sm font-semibold text-[var(--color-text)]">{{ module }}</h3>
            <ul class="space-y-1.5">
              <li v-for="(line, i) in entries" :key="i" class="flex gap-2 text-sm text-[var(--color-text-secondary)]">
                <span class="mt-2 h-1 w-1 shrink-0 rounded-full bg-[var(--color-accent)]" aria-hidden="true"></span>
                <span>{{ line }}</span>
              </li>
            </ul>
          </div>
        </div>
        <p v-else class="rounded-xl border border-[var(--color-border)] bg-[var(--color-bg-surface)] p-6 text-center text-sm text-[var(--color-text-tertiary)]">
          {{ $t('devNoLogs') }}
        </p>
      </section>

      <!-- 机型 / ROM 列表 -->
      <section v-if="devices.length || roms.length" class="mb-8">
        <h2 class="mb-3 text-sm font-semibold text-[var(--color-text)]">
          {{ $t('devDevices', { count: devices.length || roms.length }) }}
        </h2>
        <div class="grid grid-cols-1 gap-2 sm:grid-cols-2 lg:grid-cols-3">
          <div
            v-for="item in deviceCards"
            :key="item.device"
            class="cursor-pointer rounded-xl border border-[var(--color-border)] bg-[var(--color-bg-surface)] px-4 py-3 transition-colors hover:border-[var(--color-border-strong)] hover:bg-[var(--color-bg-subtle)]"
            role="button"
            tabindex="0"
            @click="openRom(item)"
            @keydown.enter.prevent="openRom(item)"
            @keydown.space.prevent="openRom(item)"
          >
            <div class="flex items-start justify-between gap-2">
              <p class="text-sm font-medium text-[var(--color-text)]">{{ localized(item.name) || item.device }}</p>
              <NuxtLink
                :to="`/${locale}/devices/${item.device}`"
                class="shrink-0 text-xs text-[var(--color-text-tertiary)] transition-colors hover:text-[var(--color-accent)]"
                @click.stop
              >
                {{ $t('devinfo') }}
              </NuxtLink>
            </div>
            <p class="mt-1 font-mono text-xs text-[var(--color-text-tertiary)]">
              {{ item.device }}<template v-if="item.version"> · {{ item.version }}</template>
            </p>
            <p v-if="item.release" class="mt-0.5 text-xs text-[var(--color-text-tertiary)]">{{ item.release }}</p>
          </div>
        </div>
      </section>
    </template>

    <RomModal
      :rom="activeRom"
      version-key="version"
      :title="activeTitle"
      :logs="logs"
      @close="activeRom = null"
    />
  </div>
</template>

<script setup>
import { pickLogs } from '~/utils/logs'

const route = useRoute()
const { locale, t } = useI18n()
const { buildDevWeekUrl, buildDevIndexUrl } = useApi()

const weekNumber = computed(() => String(route.params.week || ''))
const region = ref(String(route.query.region || 'cn'))

const { data: index } = await useAsyncData('dev-index', () => $fetch(buildDevIndexUrl()))

const { data: week, pending, error } = await useAsyncData(
  () => `dev-week-${region.value}-${weekNumber.value}`,
  () => $fetch(buildDevWeekUrl(region.value, weekNumber.value)),
  { watch: [region] }
)

// 该周存在哪些区域（大陆版 / 国际版）
const availableRegions = computed(() => {
  const weeks = index.value?.weeks || []
  const found = weeks.filter((item) => String(item.week) === weekNumber.value).map((item) => item.region)
  return found.length ? found : ['cn']
})

// 标题 / 公告正文按当前语言取中文或英文
const localized = (value) => {
  if (!value) return ''
  if (typeof value === 'string') return value
  const isZh = String(locale.value).startsWith('zh')
  return (isZh ? value.zh || value.en : value.en || value.zh) || ''
}

const attention = computed(() => week.value?.attention || [])
const description = computed(() => week.value?.description || [])

const logs = computed(() => pickLogs(week.value?.logs || null, locale.value))

// 点击机型卡片直接弹出该 ROM 的详情（日志用本周的多语言日志）
// NuxtMR 等来源的 versions 是「逐机型版本号」的逗号 / 分号拼接串，
// 直接铺在标题下太长，这里只显示首个版本号，完整串放 title 提示
const versionsLabel = computed(() => {
  const raw = String(week.value?.versions || '').trim()
  if (!raw) return '—'
  const parts = raw.split(/[,;，；]/).map((item) => item.trim()).filter(Boolean)
  if (parts.length <= 1) return parts[0] || raw
  return `${parts[0]} +${parts.length - 1}`
})

const activeRom = ref(null)
const activeTitle = ref('')
const openRom = (item) => {
  const rom = roms.value.find((r) => r.device === item.device)
  if (!rom) return
  activeRom.value = { ...rom, release: rom.release || week.value?.release || '' }
  activeTitle.value = localized(item.name) || item.device
}

const devices = computed(() => week.value?.devices || [])
const roms = computed(() => week.value?.roms || [])

// 机型卡片：优先展示公告里的机型清单，其次退回 ROM 列表
const deviceCards = computed(() => {
  if (devices.value.length) {
    const romMap = new Map(roms.value.map((item) => [item.device, item]))
    return devices.value.map((item) => {
      const rom = romMap.get(item.device) || {}
      return {
        device: item.device,
        name: item.name || {},
        version: rom.version || String(week.value?.versions || '').split(/[,;，；]/)[0]?.trim() || '',
        release: rom.release || week.value?.release || '',
      }
    })
  }
  return roms.value.map((item) => ({
    device: item.device,
    name: {},
    version: item.version || '',
    release: item.release || '',
  }))
})

useHead({
  title: computed(() => `${t('devWeek', { week: weekNumber.value })} - ${t('site')}`),
})
</script>
