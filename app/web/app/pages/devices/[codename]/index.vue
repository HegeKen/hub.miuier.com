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
        <div class="flex w-28 shrink-0 items-center justify-center border-r border-[var(--color-border)] bg-[var(--color-bg-subtle)] p-3 sm:w-36">
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
          <div class="grid gap-1 border-b border-[var(--color-border)] px-4 py-3 sm:grid-cols-[160px_1fr] sm:gap-4 sm:px-5">
            <dt class="text-sm text-[var(--color-text-secondary)]">{{ $t('devname') }}</dt>
            <dd class="text-sm font-medium">{{ deviceName(device) }}</dd>
          </div>
          <div class="grid gap-1 border-b border-[var(--color-border)] px-4 py-3 sm:grid-cols-[160px_1fr] sm:gap-4 sm:px-5">
            <dt class="text-sm text-[var(--color-text-secondary)]">{{ $t('devcode') }}</dt>
            <dd class="font-mono text-sm">{{ device.device }}</dd>
          </div>
          <div class="grid gap-1 border-b border-[var(--color-border)] px-4 py-3 sm:grid-cols-[160px_1fr] sm:gap-4 sm:px-5">
            <dt class="text-sm text-[var(--color-text-secondary)]">{{ $t('brand') }}</dt>
            <dd class="text-sm">{{ (device.brand || []).join(' / ') || '—' }}</dd>
          </div>
          <div class="grid gap-1 border-b border-[var(--color-border)] px-4 py-3 sm:grid-cols-[160px_1fr] sm:gap-4 sm:px-5">
            <dt class="text-sm text-[var(--color-text-secondary)]">{{ $t('android') }}</dt>
            <dd class="text-sm">{{ (device.android || []).join(', ') || '—' }}</dd>
          </div>
          <div class="grid gap-1 px-4 py-3 sm:grid-cols-[160px_1fr] sm:gap-4 sm:px-5">
            <dt class="text-sm text-[var(--color-text-secondary)]">{{ $t('supports') }}</dt>
            <dd class="text-sm">{{ (device.supports || []).join(', ') || '—' }}</dd>
          </div>
        </dl>
      </div>

      <!-- Branch Filter -->
      <div class="mb-6 flex flex-wrap gap-2">
        <button
          type="button"
          class="filter-pill"
          :class="selectedZone === '' ? 'filter-pill-active' : ''"
          @click="selectedZone = ''"
        >
          {{ $t('alldevices') }}
        </button>
        <button
          type="button"
          class="filter-pill"
          :class="selectedZone === '1' ? 'filter-pill-active' : ''"
          @click="selectedZone = '1'"
        >
          {{ $t('china') }}
        </button>
        <button
          type="button"
          class="filter-pill"
          :class="selectedZone === '2' ? 'filter-pill-active' : ''"
          @click="selectedZone = '2'"
        >
          {{ $t('global') }}
        </button>
      </div>

      <!-- Branches & ROMs -->
      <section class="space-y-3 pb-4">
        <div
          v-for="branch in filteredBranches"
          :key="branch.id + '-' + branch.tags?.branch"
          class="overflow-hidden rounded-xl border border-[var(--color-border)] bg-[var(--color-bg-surface)]"
        >
          <!-- Branch Header -->
          <button
            type="button"
            class="flex w-full items-center justify-between gap-4 px-4 py-4 text-left transition-colors hover:bg-[var(--color-bg-subtle)] sm:px-5"
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
                    :class="branch.zone === '1' ? 'bg-[var(--color-warn)]' : 'bg-[var(--color-info)]'"
                    aria-hidden="true"
                  ></span>
                  {{ branch.zone === '1' ? $t('china') : $t('global') }}
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
                  class="flex w-full items-center justify-between gap-3 px-4 py-3 text-left transition-colors hover:bg-[var(--color-bg-subtle)]"
                  @click="openRomModal(rom, branch)"
                >
                  <div class="min-w-0">
                    <div class="font-mono text-xs text-[var(--color-text)]">{{ rom.miui }}</div>
                    <div class="mt-1 flex items-center gap-2 text-xs text-[var(--color-text-tertiary)]">
                      <span v-if="rom.release" class="tabular-nums">{{ rom.release }}</span>
                      <span v-if="rom.android" class="rounded border border-[var(--color-border)] px-1 py-0.5 text-[10px] uppercase">{{ rom.android }}</span>
                    </div>
                  </div>
                  <svg class="h-4 w-4 shrink-0 text-[var(--color-text-tertiary)]" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" d="m8.25 4.5 7.5 7.5-7.5 7.5" />
                  </svg>
                </button>
              </div>

              <!-- Desktop: full table -->
              <div class="hidden overflow-x-auto sm:block">
                <table class="w-full text-sm">
                  <thead>
                    <tr class="border-b border-[var(--color-border)] text-left text-xs text-[var(--color-text-tertiary)]">
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
                              class="text-xs text-[var(--color-text-secondary)] hover:text-[var(--color-accent)] hover:underline"
                              target="_blank"
                              rel="noopener noreferrer"
                            >
                              {{ $t('ctelecom') }}
                            </a>
                            <a
                              v-if="rom.cmobile"
                              :href="buildDownloadLink(rom.miui, rom.cmobile)"
                              class="text-xs text-[var(--color-text-secondary)] hover:text-[var(--color-accent)] hover:underline"
                              target="_blank"
                              rel="noopener noreferrer"
                            >
                              {{ $t('cmobile') }}
                            </a>
                            <a
                              v-if="rom.cunicom"
                              :href="buildDownloadLink(rom.miui, rom.cunicom)"
                              class="text-xs text-[var(--color-text-secondary)] hover:text-[var(--color-accent)] hover:underline"
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

    <!-- ROM Detail Modal -->
    <Teleport to="body">
      <div v-if="romModal" class="fixed inset-0 z-50 flex items-center justify-center p-4" @click.self="romModal = null">
        <div class="fixed inset-0 bg-black/50" @click="romModal = null"></div>
        <div class="relative z-10 w-full max-w-3xl max-h-[85vh] overflow-y-auto rounded-2xl border border-[var(--color-border)] bg-[var(--color-bg-surface)] shadow-2xl">
          <!-- Header -->
          <div class="sticky top-0 flex items-center justify-between border-b border-[var(--color-border)] bg-[var(--color-bg-surface)] px-6 py-4">
            <div>
              <h3 class="font-semibold text-[var(--color-text)]">{{ romModalBranch?.name?.[localeKey] || romModalBranch?.name?.en || '' }}</h3>
              <p class="mt-0.5 font-mono text-xs text-[var(--color-text-tertiary)]">{{ romModal?.miui }}</p>
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
                <dd class="mt-0.5 font-mono text-xs break-all text-[var(--color-text)]">{{ romModal?.miui }}</dd>
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
                  :href="buildDownloadLink(romModal.miui, romModal.recovery)"
                  class="inline-flex items-center gap-2 rounded-lg bg-[var(--color-accent)] px-3 py-2 text-sm font-medium text-white transition-colors hover:opacity-90"
                  target="_blank" rel="noopener noreferrer"
                >
                  <svg class="h-4 w-4" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" d="M3 16.5v2.25A2.25 2.25 0 0 0 5.25 21h13.5A2.25 2.25 0 0 0 21 18.75V16.5M16.5 12 12 16.5m0 0L7.5 12m4.5 4.5V3" /></svg>
                  {{ $t('recovery') }}
                </a>
                <a
                  v-if="romModal?.fastboot"
                  :href="buildDownloadLink(romModal.miui, romModal.fastboot)"
                  class="inline-flex items-center gap-2 rounded-lg border border-[var(--color-border)] px-3 py-2 text-sm font-medium text-[var(--color-text)] transition-colors hover:bg-[var(--color-bg-subtle)]"
                  target="_blank" rel="noopener noreferrer"
                >
                  <svg class="h-4 w-4" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" d="M3 16.5v2.25A2.25 2.25 0 0 0 5.25 21h13.5A2.25 2.25 0 0 0 21 18.75V16.5M16.5 12 12 16.5m0 0L7.5 12m4.5 4.5V3" /></svg>
                  {{ $t('fastboot') }}
                </a>
                <template v-if="romModal?.ctelecom || romModal?.cmobile || romModal?.cunicom">
                  <div class="h-px bg-[var(--color-border)]"></div>
                  <a v-if="romModal.ctelecom" :href="buildDownloadLink(romModal.miui, romModal.ctelecom)" class="text-xs font-medium text-[var(--color-accent)] hover:underline" target="_blank" rel="noopener noreferrer">{{ $t('ctelecom') }}</a>
                  <a v-if="romModal.cmobile" :href="buildDownloadLink(romModal.miui, romModal.cmobile)" class="text-xs font-medium text-[var(--color-accent)] hover:underline" target="_blank" rel="noopener noreferrer">{{ $t('cmobile') }}</a>
                  <a v-if="romModal.cunicom" :href="buildDownloadLink(romModal.miui, romModal.cunicom)" class="text-xs font-medium text-[var(--color-accent)] hover:underline" target="_blank" rel="noopener noreferrer">{{ $t('cunicom') }}</a>
                </template>
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
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { validateCodename, sanitizeString } from '~/utils/validation'

const route = useRoute()
const { locale } = useI18n()
const { t } = useI18n()
const { buildDeviceUrl, buildDeviceImageUrl, buildBrandImageUrl, buildDownloadLink, buildChangelogUrl } = useApi()

const selectedZone = ref('')
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

const branchName = (b) =>
  b?.name?.[locale.value.startsWith('zh') ? 'zh' : 'en'] ||
  b?.name?.en ||
  b?.name?.zh ||
  b?.id

// ROM 排序：版本号为主（降序），release_date 为辅（降序、空值置后）
const romVersionParts = (version) => {
  const m = String(version || '').match(/^[A-Za-z]*(\d+(?:\.\d+)*)/)
  return m ? m[1].split('.').map((n) => parseInt(n, 10) || 0) : []
}
const compareRoms = (a, b) => {
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

const filteredBranches = computed(() => {
  if (!device.value?.branches) return []
  let branches = device.value.branches.filter((b) => b.show === '1')
  if (selectedZone.value) {
    branches = branches.filter((b) => b.zone === selectedZone.value)
  }
  return branches.map((b) => ({
    ...b,
    roms: [...(b.roms || [])].sort(compareRoms),
  }))
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
    romModalLogs.value = locale.value.startsWith('en')
      ? (raw.logs_en || raw.logs_zh || null)
      : (raw.logs_zh || raw.logs_en || null)
  } catch {
    romModalLogs.value = null
  } finally {
    romModalLoading.value = false
  }
}

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


