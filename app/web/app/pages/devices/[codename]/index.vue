<template>
  <div class="container-page py-10 sm:py-14">
    <!-- Loading -->
    <div v-if="pending" class="flex justify-center py-20">
      <span class="spinner" role="status" aria-label="Loading"></span>
    </div>

    <!-- Error -->
    <div v-else-if="error || !device" class="py-20 text-center">
      <p class="text-sm text-[var(--color-text-secondary)]">{{ error?.message || 'Device not found' }}</p>
      <NuxtLink
        :to="'/' + locale + '/devices'"
        class="mt-3 inline-block text-sm font-medium text-[var(--color-accent)] hover:underline"
      >
        {{ $t('devices') }}
      </NuxtLink>
    </div>

    <!-- Device Content -->
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
            <NuxtLink
              :to="'/' + locale + '/devices'"
              class="transition-colors hover:text-[var(--color-text)]"
            >
              {{ $t('devices') }}
            </NuxtLink>
          </li>
          <li aria-hidden="true">/</li>
          <li class="truncate font-medium text-[var(--color-text)]">
            {{ deviceName(device) }}
          </li>
        </ol>
      </nav>

      <!-- Title -->


      <!-- Device Info -->
      <div class="mb-10 flex overflow-hidden rounded-xl border border-[var(--color-border)]">
        <div class="flex w-28 shrink-0 items-center justify-center border-e border-[var(--color-border)] bg-[var(--color-bg-subtle)] p-3 sm:w-36">
          <img
            v-if="!deviceImageError"
            :src="deviceImageSrc"
            :alt="deviceName(device)"
            class="max-h-40 w-auto max-w-full object-contain"
            loading="lazy"
            @error="onDeviceImageError"
          />
        </div>
        <dl class="min-w-0 flex-1">
          <div class="grid grid-cols-[60px_1fr] items-center gap-x-3 gap-y-1 border-b border-[var(--color-border)] px-4 py-3 sm:grid-cols-[110px_1fr] sm:gap-3 sm:px-5">
            <dt class="whitespace-nowrap text-sm text-[var(--color-text-secondary)]">{{ $t('devname') }}</dt>
            <dd class="min-w-0 break-words text-sm font-medium">{{ deviceName(device) }}</dd>
          </div>
          <div class="grid grid-cols-[60px_1fr] items-center gap-x-3 gap-y-1 border-b border-[var(--color-border)] px-4 py-3 sm:grid-cols-[110px_1fr] sm:gap-3 sm:px-5">
            <dt class="whitespace-nowrap text-sm text-[var(--color-text-secondary)]">{{ $t('devcode') }}</dt>
            <dd class="min-w-0 break-words font-mono text-sm">{{ device.device }}</dd>
          </div>
          <div class="grid grid-cols-[60px_1fr] items-center gap-x-3 gap-y-1 border-b border-[var(--color-border)] px-4 py-3 sm:grid-cols-[110px_1fr] sm:gap-3 sm:px-5">
            <dt class="whitespace-nowrap text-sm text-[var(--color-text-secondary)]">{{ $t('brand') }}</dt>
            <dd class="min-w-0 break-words text-sm">{{ (device.brand || []).join(' / ') || '—' }}</dd>
          </div>
          <div class="grid grid-cols-[60px_1fr] items-center gap-x-3 gap-y-1 border-b border-[var(--color-border)] px-4 py-3 sm:grid-cols-[110px_1fr] sm:gap-3 sm:px-5">
            <dt class="whitespace-nowrap text-sm text-[var(--color-text-secondary)]">{{ $t('android') }}</dt>
            <dd class="min-w-0 break-words text-sm">{{ (device.android || []).join(', ') || '—' }}</dd>
          </div>
          <div class="grid grid-cols-[60px_1fr] items-center gap-x-3 gap-y-1 px-4 py-3 sm:grid-cols-[110px_1fr] sm:gap-3 sm:px-5">
            <dt class="whitespace-nowrap text-sm text-[var(--color-text-secondary)]">{{ $t('supports') }}</dt>
            <dd class="min-w-0 break-words text-sm">{{ (device.supports || []).join(', ') || '—' }}</dd>
          </div>
        </dl>
      </div>

      <!-- ROM Search -->
      <div class="mb-6">
        <div class="relative">
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
            v-model="searchQuery"
            type="text"
            :placeholder="$t('searchRomPlaceholder')"
            class="input-base ps-11 pe-10"
          />
          <button
            v-if="searchQuery"
            type="button"
            class="absolute end-3 top-1/2 -translate-y-1/2 rounded p-1 text-[var(--color-text-tertiary)] transition-colors hover:text-[var(--color-text)]"
            aria-label="Clear search"
            @click="searchQuery = ''"
          >
            <svg class="h-4 w-4" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" aria-hidden="true">
              <path stroke-linecap="round" stroke-linejoin="round" d="M6 18 18 6M6 6l12 12" />
            </svg>
          </button>
        </div>
      </div>

      <!-- Branch Filter -->
      <div class="mb-6 flex flex-wrap gap-2">
        <button
          type="button"
          class="filter-pill"
          :class="selectedRegion === '' ? 'filter-pill-active' : ''"
          @click="selectedRegion = ''"
        >
          {{ $t('alldevices') }}
        </button>
        <button
          v-for="region in availableRegions"
          :key="region"
          type="button"
          class="filter-pill"
          :class="selectedRegion === region ? 'filter-pill-active' : ''"
          @click="selectedRegion = region"
        >
          {{ regionName(region) }}
        </button>
      </div>

      <!-- Branches & ROMs -->
      <section class="space-y-3 pb-4">
        <div
          v-if="searchQuery && filteredBranches.length === 0"
          class="rounded-xl border border-[var(--color-border)] bg-[var(--color-bg-surface)] px-4 py-10 text-center text-sm text-[var(--color-text-secondary)]"
        >
          {{ $t('noRomResults') }}
        </div>
        <div
          v-for="branch in filteredBranches"
          :key="branch.id + '-' + branch.tags?.branch"
          class="overflow-hidden rounded-xl border border-[var(--color-border)] bg-[var(--color-bg-surface)]"
        >
          <!-- Branch Header -->
          <button
            type="button"
            class="flex w-full items-center justify-between gap-4 px-4 py-4 text-start transition-colors hover:bg-[var(--color-bg-subtle)] sm:px-5"
            :aria-expanded="expandedBranches.includes(branchKey(branch))"
            @click="toggleBranch(branchKey(branch))"
          >
            <div class="min-w-0">
              <h2 class="font-medium text-[var(--color-text)]">
                {{ branchName(branch) }}
              </h2>
              <div class="mt-1.5 flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-[var(--color-text-tertiary)]">
                <span class="inline-flex items-center gap-1.5">
                  <span
                    class="dot"
                    :class="branchDotColor(branch)"
                    aria-hidden="true"
                  ></span>
                  {{ branchRegionLabel(branch) }}
                </span>
                <span
                  v-if="branch.tags?.branchtag === 'F'"
                  class="inline-flex items-center gap-1.5"
                >
                  <span class="dot bg-[var(--color-ok)]" aria-hidden="true"></span>
                  {{ $t('stable') }}
                </span>
                <span
                  v-else-if="branch.tags?.branchtag === 'X'"
                  class="inline-flex items-center gap-1.5"
                >
                  <span class="dot bg-[var(--color-warn)]" aria-hidden="true"></span>
                  {{ $t('dev') }}
                </span>
                <span v-if="branch.ep === '1'" class="inline-flex items-center gap-1.5">
                  <span class="dot bg-[var(--color-info)]" aria-hidden="true"></span>
                  {{ $t('ep') }}
                </span>
              </div>
            </div>
            <svg
              class="h-4 w-4 shrink-0 text-[var(--color-text-tertiary)] transition-transform duration-200"
              :class="{ 'rotate-180': expandedBranches.includes(branchKey(branch)) }"
              xmlns="http://www.w3.org/2000/svg"
              fill="none"
              viewBox="0 0 24 24"
              stroke-width="1.5"
              stroke="currentColor"
              aria-hidden="true"
            >
              <path stroke-linecap="round" stroke-linejoin="round" d="m19.5 8.25-7.5 7.5-7.5-7.5" />
            </svg>
          </button>

          <!-- ROM Table -->
          <Transition
            enter-active-class="transition duration-200 ease-out"
            enter-from-class="opacity-0 -translate-y-1"
            enter-to-class="opacity-100 translate-y-0"
            leave-active-class="transition duration-150 ease-in"
            leave-from-class="opacity-100 translate-y-0"
            leave-to-class="opacity-0 -translate-y-1"
          >
            <div v-if="expandedBranches.includes(branchKey(branch))" class="border-t border-[var(--color-border)]">
              <!-- Mobile: simplified list -->
              <div class="sm:hidden divide-y divide-[var(--color-border)]">
                <button
                  v-for="(rom, index) in branch.roms || []"
                  :key="rom.miui"
                  :id="'rom-' + rom.miui"
                  type="button"
                  class="flex w-full items-center justify-between gap-3 px-4 py-3 text-start transition-colors hover:bg-[var(--color-bg-subtle)]"
                  @click="openRomModal(rom, branch)"
                >
                  <div class="min-w-0">
                    <div class="font-mono text-xs text-[var(--color-text)]">{{ rom.miui }}</div>
                    <div class="mt-1 flex items-center gap-2 text-xs text-[var(--color-text-tertiary)]">
                      <span v-if="rom.release" class="tabular-nums">{{ rom.release }}</span>
                      <span v-if="rom.android" class="rounded border border-[var(--color-border)] px-1 py-0.5 text-[10px] uppercase">{{ rom.android }}</span>
                    </div>
                  </div>
                  <svg class="dir-flip h-4 w-4 shrink-0 text-[var(--color-text-tertiary)]" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" d="m8.25 4.5 7.5 7.5-7.5 7.5" />
                  </svg>
                </button>
              </div>

              <!-- Desktop: full table -->
              <div class="hidden overflow-x-auto sm:block">
                <table class="w-full text-sm">
                  <thead>
                    <tr class="border-b border-[var(--color-border)] text-start text-xs text-[var(--color-text-tertiary)]">
                      <th class="px-4 py-2.5 font-medium sm:px-5">#</th>
                      <th class="px-4 py-2.5 font-medium">{{ $t('version') }}</th>
                      <th class="px-4 py-2.5 font-medium">{{ $t('android') }}</th>
                      <th class="px-4 py-2.5 font-medium">{{ $t('release') }}</th>
                      <th class="px-4 py-2.5 font-medium">{{ $t('aspatch') }}</th>
                      <th class="px-4 py-2.5 font-medium">{{ $t('recovery') }}</th>
                      <th class="px-4 py-2.5 font-medium">{{ $t('fastboot') }}</th>
                      <th class="px-4 py-2.5 font-medium">{{ $t('changelog') }}</th>
                    </tr>
                  </thead>
                  <tbody class="divide-y divide-[var(--color-border)]">
                    <template
                      v-for="(rom, index) in branch.roms || []"
                      :key="rom.miui"
                    >
                      <tr :id="'rom-' + rom.miui" class="transition-colors hover:bg-[var(--color-bg-subtle)]">
                        <td class="px-4 py-2.5 text-[var(--color-text-tertiary)] sm:px-5">
                          {{ (branch.roms?.length || 0) - index }}
                        </td>
                        <td class="px-4 py-2.5 font-mono text-xs">{{ rom.miui }}</td>
                        <td class="px-4 py-2.5 tabular-nums text-[var(--color-text-secondary)]">{{ rom.android }}</td>
                        <td class="px-4 py-2.5 tabular-nums text-[var(--color-text-secondary)]">
                          {{ rom.release || $t('na') }}
                        </td>
                        <td class="px-4 py-2.5 tabular-nums text-[var(--color-text-secondary)]">
                          {{ rom.aspatch || $t('na') }}
                        </td>
                        <td class="px-4 py-2.5">
                          <div class="flex flex-col gap-1">
                            <a
                              v-if="rom.recovery"
                              :href="buildDownloadLink(rom.miui, rom.recovery)"
                              class="font-medium text-[var(--color-accent)] hover:underline"
                              target="_blank"
                              rel="noopener noreferrer"
                            >
                              {{ $t('download') }}
                            </a>
                            <span v-else class="text-[var(--color-text-tertiary)]">{{ $t('na') }}</span>
                          </div>
                        </td>
                        <td class="px-4 py-2.5">
                          <div class="flex flex-col gap-1">
                            <a
                              v-if="rom.fastboot"
                              :href="buildDownloadLink(rom.miui, rom.fastboot)"
                              class="font-medium text-[var(--color-accent)] hover:underline"
                              target="_blank"
                              rel="noopener noreferrer"
                            >
                              {{ $t('download') }}
                            </a>
                            <span v-else class="text-[var(--color-text-tertiary)]">{{ $t('na') }}</span>
                            <a
                              v-if="rom.ctelecom"
                              :href="buildDownloadLink(rom.miui, rom.ctelecom)"
                              class="font-medium text-[var(--color-accent)] hover:underline"
                              target="_blank"
                              rel="noopener noreferrer"
                            >
                              {{ $t('ctelecom') }}
                            </a>
                            <a
                              v-if="rom.cmobile"
                              :href="buildDownloadLink(rom.miui, rom.cmobile)"
                              class="font-medium text-[var(--color-accent)] hover:underline"
                              target="_blank"
                              rel="noopener noreferrer"
                            >
                              {{ $t('cmobile') }}
                            </a>
                            <a
                              v-if="rom.cunicom"
                              :href="buildDownloadLink(rom.miui, rom.cunicom)"
                              class="font-medium text-[var(--color-accent)] hover:underline"
                              target="_blank"
                              rel="noopener noreferrer"
                            >
                              {{ $t('cunicom') }}
                            </a>
                          </div>
                        </td>
                        <td class="px-4 py-2.5">
                          <button
                            type="button"
                            class="inline-flex items-center gap-1.5 text-xs font-medium text-[var(--color-text-tertiary)] transition-colors hover:text-[var(--color-text)]"
                            @click="openRomModal(rom, branch)"
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
                    </template>
                  </tbody>
                </table>
              </div>
            </div>
          </Transition>
        </div>
      </section>
    </template>

    <!-- ROM Detail Modal（共用组件，开发版周报页 / 刷机包页同款） -->
    <RomModal
      :rom="romModal"
      version-key="miui"
      :title="branchName(romModalBranch)"
      :logs="romModalLogs"
      :logs-loading="romModalLoading"
      :high-speed-params="highSpeedParams"
      @close="romModal = null"
    />
  </div>
</template>

<script setup>
import { validateCodename, sanitizeString } from '~/utils/validation'
import { isChinaRegion } from '~/utils/region'
import { pickLogs } from '~/utils/logs'

const route = useRoute()
const { locale } = useI18n()
const { t } = useI18n()
const { buildDeviceUrl, buildDeviceImageUrl, buildBrandImageUrl, buildDownloadLink, buildChangelogUrl, fetchHighSpeed } = useApi()

// 区域显示名统一取自 i18n 词条
const regionName = useRegionName()

const selectedRegion = ref('')
const searchQuery = ref('')
const expandedBranches = ref([])

// 机型照片加载失败时按品牌显示默认图（Xiaomi→mi.svg，POCO→POCO.png，REDMI→REDMI.png），再失败才隐藏
const brandFallback = ref(false)
const deviceImageError = ref(false)

const deviceImageSrc = computed(() => {
  if (!device.value?.device) return ''
  return brandFallback.value
    ? buildBrandImageUrl(device.value.brand?.[0])
    : buildDeviceImageUrl(device.value.device)
})

const onDeviceImageError = () => {
  if (brandFallback.value) {
    deviceImageError.value = true
  } else {
    brandFallback.value = true
  }
}

// ROM Detail Modal
const romModal = ref(null)        // 当前展示的 ROM 对象
const romModalBranch = ref(null)  // ROM 所在的分支
const romModalLogs = ref(null)    // 更新日志
const romModalLoading = ref(false)
const localeKey = computed(() => (locale.value.startsWith('zh') ? 'zh' : 'en'))

// 高速下载：结果带时效签名，状态跟随当前 ROM，切换 ROM 时重置


const codename = computed(() => {
  const param = route.params.codename
  return Array.isArray(param) ? param[0] : param
})

// Localized device name: data keys are 'zh' / 'en' while locale is 'zh-cn' / 'en-us'
const deviceName = (d) =>
  d?.name?.[locale.value.startsWith('zh') ? 'zh' : 'en'] ||
  d?.name?.en ||
  d?.name?.zh ||
  d?.device

const branchKey = (b) => `${b.id}-${b.tags?.branch || ''}`

const branchName = useBranchName()

// ROM 排序：版本族（HyperOS/OS 优先于 MIUI/V）为主，同族内版本号降序，release_date 为辅
const romVersionParts = (version) => {
  const m = String(version || '').match(/^[A-Za-z]*(\d+(?:\.\d+)*)/)
  return m ? m[1].split('.').map((n) => parseInt(n, 10) || 0) : []
}
const romFamilyRank = (version) => {
  const m = String(version || '').match(/^([A-Za-z]+)/)
  const prefix = m ? m[1].toUpperCase() : ''
  if (prefix.startsWith('OS')) return 0
  if (prefix.startsWith('V')) return 1
  return 2
}
const compareRoms = (a, b) => {
  const fa = romFamilyRank(a.miui)
  const fb = romFamilyRank(b.miui)
  if (fa !== fb) return fa - fb
  const va = romVersionParts(a.miui)
  const vb = romVersionParts(b.miui)
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

const { data: device, error, pending } = await useAsyncData(
  'device-' + codename.value,
  async () => {
    const sanitized = sanitizeString(codename.value)
    if (!validateCodename(sanitized)) {
      throw new Error(t('invalidDevice') || 'Invalid codename')
    }
    return await $fetch(buildDeviceUrl(sanitized))
  }
)

// 机型切换后重置照片加载状态
watch(() => device.value?.device, () => { brandFallback.value = false; deviceImageError.value = false })

// 分支卡片上的区域标签：优先显示具体区域（如 中国台湾 / 欧洲经济区 / 俄罗斯），无 region 时回退到 zone 的 中国/国际
const branchRegionLabel = (b) => {
  if (b?.region) return regionName(b.region)
  return b?.zone === '1' ? t('china') : t('global')
}
const branchDotColor = (b) => {
  if (b?.region) return isChinaRegion(b.region) ? 'bg-[var(--color-warn)]' : 'bg-[var(--color-info)]'
  return b?.zone === '1' ? 'bg-[var(--color-warn)]' : 'bg-[var(--color-info)]'
}

// 当前机型实际支持的区域（按 branch.region 动态生成），去重并保持稳定顺序
const availableRegions = computed(() => {
  if (!device.value?.branches) return []
  const regions = new Set()
  for (const b of device.value.branches) {
    if (b.show === '1' && b.region) regions.add(b.region)
  }
  return [...regions]
})

// 机型切换后，若已选区域不在当前机型支持范围内，则重置为「全部设备」
watch(availableRegions, (regions) => {
  if (selectedRegion.value && !regions.includes(selectedRegion.value)) {
    selectedRegion.value = ''
  }
})

// 搜索匹配：版本号 / Android 版本 / 分支名 / 分支标签 / release 等关键词
const romMatchesQuery = (rom, branch, query) => {
  if (!query) return true
  const haystack = [
    rom.miui,
    rom.android,
    rom.release,
    rom.aspatch,
    branchName(branch),
    branch.id,
    branch.tags?.branch,
    branch.tags?.branchtag,
  ]
    .filter(Boolean)
    .join(' ')
    .toLowerCase()
  return haystack.includes(query)
}

const filteredBranches = computed(() => {
  if (!device.value?.branches) return []
  const q = searchQuery.value.trim().toLowerCase()
  let branches = device.value.branches.filter((b) => b.show === '1')
  if (selectedRegion.value) {
    branches = branches.filter((b) => b.region === selectedRegion.value)
  }
  return branches
    .map((b) => {
      let roms = [...(b.roms || [])].sort(compareRoms)
      if (q) roms = roms.filter((r) => romMatchesQuery(r, b, q))
      return { ...b, roms }
    })
    .filter((b) => !q || b.roms.length > 0)
})

// 搜索时自动展开命中分支；清空搜索时收起
watch(searchQuery, () => {
  const q = searchQuery.value.trim().toLowerCase()
  if (!q) {
    expandedBranches.value = []
    return
  }
  expandedBranches.value = (device.value?.branches || [])
    .filter((b) => b.show === '1' && (!selectedRegion.value || b.region === selectedRegion.value))
    .filter((b) => (b.roms || []).some((r) => romMatchesQuery(r, b, q)))
    .map((b) => branchKey(b))
})

// 高速下载所需的参数（含分支代号），交给共用弹窗组件调用
const highSpeedParams = computed(() => {
  const branch = romModalBranch.value
  const dev = device.value
  if (!branch || !dev || !romModal.value) return null
  return {
    code: branch.id,
    device: dev.device,
    branchTag: branch.tags?.branchtag || branch.tags?.btag || 'F',
    region: branch.region || '',
    zone: parseInt(branch.zone, 10) || 1,
    android: romModal.value.android || '',
  }
})

// 打开 ROM 详情模态框
const openRomModal = async (rom, branch) => {
  romModal.value = rom
  romModalBranch.value = branch
  romModalLogs.value = null
  romModalLoading.value = true
  try {
    let data = null
    const url = buildChangelogUrl(device.value.device, branch.region, rom.miui)
    try {
      data = await $fetch(url)
    } catch {
      if (branch.region) {
        data = await $fetch(buildChangelogUrl(device.value.device, '', rom.miui))
      }
    }
    const raw = data || {}
    // 按当前语言取日志，缺失时依次回落英文 / 简体中文
    romModalLogs.value = pickLogs(raw, locale.value)
  } catch {
    romModalLogs.value = null
  } finally {
    romModalLoading.value = false
  }
}

// 按需请求高速下载链接（由同源 Nitro 路由代理小米 OTA 接口）


// 从 URL fragment 中读取目标 ROM 版本（如 #rom-OS3.0.303.0.WPLIDXM）
// 自动打开模态框展示该版本详情
const openFromFragment = () => {
  const hash = window.location.hash
  if (!hash.startsWith('#rom-')) return
  const version = decodeURIComponent(hash.slice(5))
  for (const branch of device.value?.branches || []) {
    const rom = branch.roms?.find((r) => r.miui === version)
    if (rom) {
      openRomModal(rom, branch)
      break
    }
  }
}

watch(device, (val) => {
  if (val) openFromFragment()
}, { immediate: true })

// ESC 键关闭模态框
const onKeydown = (e) => {
  if (e.key === 'Escape' && romModal.value) romModal.value = null
}
onMounted(() => document.addEventListener('keydown', onKeydown))
onUnmounted(() => document.removeEventListener('keydown', onKeydown))

const toggleBranch = (id) => {
  const index = expandedBranches.value.indexOf(id)
  if (index === -1) {
    expandedBranches.value.push(id)
  } else {
    expandedBranches.value.splice(index, 1)
  }
}

useHead({
  title: computed(() => {
    if (device.value) {
      const name = deviceName(device.value)
      return `${name} (${device.value.device}) - ${t('site')}`
    }
    return t('site')
  }),
})
</script>


