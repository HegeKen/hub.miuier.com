<template>
  <div class="container-page py-10 sm:py-14">
    <header class="mb-8">
      <h1 class="text-2xl font-semibold tracking-tight sm:text-3xl">{{ $t('devices') }}</h1>
      <p class="mt-2 text-sm text-[var(--color-text-secondary)]">{{ $t('devicesSub') }}</p>
    </header>

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
        <input
          v-model="searchQuery"
          type="search"
          :placeholder="$t('searchPlaceholder')"
          class="input-base pl-10"
        />
      </div>
    </div>

    <!-- Brand Filter -->
    <div class="mb-4 flex flex-wrap gap-2">
      <button
        type="button"
        class="filter-pill"
        :class="selectedBrand === '' ? 'filter-pill-active' : ''"
        @click="selectedBrand = ''"
      >
        {{ $t('alldevices') }}
      </button>
      <button
        v-for="brand in availableBrands"
        :key="brand"
        type="button"
        class="filter-pill"
        :class="selectedBrand === brand ? 'filter-pill-active' : ''"
        @click="selectedBrand = brand"
      >
        {{ brand }}
      </button>
    </div>

    <!-- Android Version Filter -->
    <div class="mb-4 flex flex-wrap items-center gap-2">
      <span class="text-xs font-medium text-[var(--color-text-secondary)]">{{ $t('android') }}:</span>
      <button
        type="button"
        class="filter-pill-sm"
        :class="selectedAndroid === '' ? 'filter-pill-sm-active' : ''"
        @click="selectedAndroid = ''"
      >
        {{ $t('alldevices') }}
      </button>
      <button
        v-for="v in availableAndroids"
        :key="v"
        type="button"
        class="filter-pill-sm"
        :class="selectedAndroid === v ? 'filter-pill-sm-active' : ''"
        @click="selectedAndroid = v"
      >
        {{ v }}
      </button>
    </div>

    <!-- OS Version Filter -->
    <div class="mb-8 flex flex-wrap items-center gap-2">
      <span class="text-xs font-medium text-[var(--color-text-secondary)]">{{ $t('supports') }}:</span>
      <button
        type="button"
        class="filter-pill-sm"
        :class="selectedOs === '' ? 'filter-pill-sm-active' : ''"
        @click="selectedOs = ''"
      >
        {{ $t('alldevices') }}
      </button>
      <button
        v-for="v in availableOsVersions"
        :key="v"
        type="button"
        class="filter-pill-sm"
        :class="selectedOs === v ? 'filter-pill-sm-active' : ''"
        @click="selectedOs = v"
      >
        {{ v }}
      </button>
    </div>

    <!-- Loading -->
    <div v-if="pending" class="flex justify-center py-20">
      <span class="spinner" role="status" aria-label="Loading"></span>
    </div>

    <!-- Error -->
    <div v-else-if="error" class="py-20 text-center">
      <p class="text-sm text-[var(--color-text-secondary)]">Failed to load devices</p>
    </div>

    <!-- Empty -->
    <div v-else-if="filteredDevices.length === 0" class="py-20 text-center">
      <p class="text-sm text-[var(--color-text-tertiary)]">{{ $t('noResults') }}</p>
    </div>

    <!-- Device List -->
    <div v-else class="overflow-hidden rounded-xl border border-[var(--color-border)]">
      <ul class="divide-y divide-[var(--color-border)]">
        <li v-for="device in filteredDevices" :key="device.device">
          <NuxtLink
            :to="'/' + locale + '/devices/' + device.device"
            class="group block bg-[var(--color-bg-surface)] px-4 py-3.5 transition-colors hover:bg-[var(--color-bg-subtle)] sm:flex sm:items-center sm:justify-between sm:gap-4 sm:px-5"
          >
            <!-- Mobile: thumbnail + codename + brands + arrow, name below -->
            <div class="flex min-w-0 items-center gap-3 sm:hidden">
              <img
                v-if="(deviceImageStage[device.device] || 0) < 2"
                :src="deviceImgSrc(device)"
                :alt="deviceName(device)"
                class="h-10 w-10 shrink-0 rounded-md border border-[var(--color-border)] bg-[var(--color-bg-subtle)] object-contain"
                loading="lazy"
                @error="onDeviceImgError(device, $event)"
              />
              <div class="min-w-0 flex-1">
                <div class="flex items-center gap-2">
                  <span class="font-mono text-sm font-medium text-[var(--color-text)]">{{ device.device }}</span>
                  <div class="ml-auto flex shrink-0 items-center gap-2">
                    <span
                      v-for="brand in device.brand || []"
                      :key="brand"
                      class="text-xs text-[var(--color-text-tertiary)]"
                    >
                      {{ formatBrand(brand) }}
                    </span>
                    <svg
                      class="h-4 w-4 shrink-0 text-[var(--color-text-tertiary)] transition-all group-hover:translate-x-0.5 group-hover:text-[var(--color-text)]"
                      xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" aria-hidden="true"
                    >
                      <path stroke-linecap="round" stroke-linejoin="round" d="m8.25 4.5 7.5 7.5-7.5 7.5" />
                    </svg>
                  </div>
                </div>
                <p class="mt-0.5 truncate text-xs text-[var(--color-text-secondary)]">
                  {{ deviceName(device) }}
                </p>
              </div>
            </div>
            <!-- Desktop: thumbnail + name as primary, codename below -->
            <div class="hidden min-w-0 items-center gap-4 sm:flex">
              <img
                v-if="(deviceImageStage[device.device] || 0) < 2"
                :src="deviceImgSrc(device)"
                :alt="deviceName(device)"
                class="h-12 w-12 shrink-0 rounded-md border border-[var(--color-border)] bg-[var(--color-bg-subtle)] object-contain"
                loading="lazy"
                @error="onDeviceImgError(device, $event)"
              />
              <div class="min-w-0">
                <h3 class="truncate font-medium text-[var(--color-text)]">
                  {{ deviceName(device) }}
                </h3>
                <p class="mt-0.5 truncate font-mono text-xs text-[var(--color-text-tertiary)]">
                  {{ device.device }}
                </p>
              </div>
            </div>
            <div class="flex shrink-0 items-center gap-3">
              <span
                v-for="brand in device.brand || []"
                :key="brand"
                class="hidden text-xs text-[var(--color-text-tertiary)] sm:inline"
              >
                {{ formatBrand(brand) }}
              </span>
              <svg
                class="hidden h-4 w-4 text-[var(--color-text-tertiary)] transition-all group-hover:translate-x-0.5 group-hover:text-[var(--color-text)] sm:inline"
                xmlns="http://www.w3.org/2000/svg"
                fill="none"
                viewBox="0 0 24 24"
                stroke-width="1.5"
                stroke="currentColor"
                aria-hidden="true"
              >
                <path stroke-linecap="round" stroke-linejoin="round" d="m8.25 4.5 7.5 7.5-7.5 7.5" />
              </svg>
            </div>
          </NuxtLink>
        </li>
      </ul>
    </div>
  </div>
</template>

<script setup>
const { locale } = useI18n()
const { t } = useI18n()
const { buildDevicesIndexUrl, buildDeviceImageUrl, buildBrandImageUrl } = useApi()

// 机型缩略图加载状态：0=机型图，1=品牌默认图，2=隐藏；无封面时按品牌显示默认图（Xiaomi→mi.svg 等）
const deviceImageStage = reactive({})

const deviceImgSrc = (device) => {
  const stage = deviceImageStage[device.device] || 0
  return stage === 1 ? buildBrandImageUrl(device.brand?.[0]) : buildDeviceImageUrl(device.device)
}

const onDeviceImgError = (device, e) => {
  const cur = e.currentTarget.currentSrc || e.currentTarget.src || ''
  const stage = deviceImageStage[device.device] || 0
  const failedDevice = cur.includes(`${device.device}.png`)
  if (failedDevice) {
    // 机型图失败 -> 切到品牌默认图（仅当尚未切换时；两个 img 并发报错时忽略重复）
    if (stage === 0) deviceImageStage[device.device] = 1
  } else {
    // 品牌默认图也失败 -> 隐藏
    if (stage === 1) deviceImageStage[device.device] = 2
  }
}

const route = useRoute()
const router = useRouter()

const searchQuery = ref('')
const selectedBrand = ref('')
const selectedAndroid = ref('')
const selectedOs = ref(typeof route.query.os === 'string' ? route.query.os : '')

// Sync OS filter to URL query (?os=OS3)，支持分享链接直接命中筛选结果
watch(selectedOs, (val) => {
  const query = { ...route.query }
  if (val) {
    query.os = val
  } else {
    delete query.os
  }
  router.replace({ query })
})

const formatBrand = (b) => b.toLowerCase() === 'xiaomi' ? 'Xiaomi' : b.toUpperCase()

// Localized device name: data keys are 'zh' / 'en' while locale is 'zh-cn' / 'en-us'
const deviceName = (d) =>
  d.name?.[locale.value.startsWith('zh') ? 'zh' : 'en'] ||
  d.name?.en ||
  d.name?.zh ||
  d.device

const { data: devices, pending, error } = await useAsyncData(
  'devices-list',
  () => $fetch(buildDevicesIndexUrl())
)

const availableBrands = computed(() => {
  if (!devices.value) return []
  const brandMap = new Map()
  for (const d of devices.value) {
    for (const b of d.brand || []) {
      const key = b.toLowerCase()
      if (!brandMap.has(key)) brandMap.set(key, key === 'xiaomi' ? 'Xiaomi' : b.toUpperCase())
    }
  }
  const order = { xiaomi: 0, redmi: 1, poco: 2 }
  return Array.from(brandMap.entries())
    .sort(([ka, va], [kb, vb]) => {
      const oa = order[ka] ?? 99
      const ob = order[kb] ?? 99
      return oa !== ob ? oa - ob : va.localeCompare(vb)
    })
    .map(([, v]) => v)
})

const availableAndroids = computed(() => {
  if (!devices.value) return []
  let pool = devices.value
  if (selectedOs.value) {
    pool = pool.filter((d) => (d.supports || []).includes(selectedOs.value))
  }
  if (selectedBrand.value) {
    const sel = selectedBrand.value.toLowerCase()
    pool = pool.filter((d) => (d.brand || []).some((b) => b.toLowerCase() === sel))
  }
  const versions = new Set()
  for (const d of pool) {
    for (const v of d.android || []) {
      versions.add(v)
    }
  }
  return Array.from(versions).sort((a, b) => {
    const pa = a.split('.').map(Number)
    const pb = b.split('.').map(Number)
    for (let i = 0; i < Math.max(pa.length, pb.length); i++) {
      const diff = (pb[i] || 0) - (pa[i] || 0)
      if (diff !== 0) return diff
    }
    return 0
  })
})

const availableOsVersions = computed(() => {
  if (!devices.value) return []
  let pool = devices.value
  if (selectedAndroid.value) {
    pool = pool.filter((d) => (d.android || []).includes(selectedAndroid.value))
  }
  if (selectedBrand.value) {
    const sel = selectedBrand.value.toLowerCase()
    pool = pool.filter((d) => (d.brand || []).some((b) => b.toLowerCase() === sel))
  }
  const versions = new Set()
  for (const d of pool) {
    for (const v of d.supports || []) {
      versions.add(v)
    }
  }
  return Array.from(versions).sort((a, b) => {
    const isOsA = a.startsWith('OS')
    const isOsB = b.startsWith('OS')
    if (isOsA !== isOsB) return isOsA ? -1 : 1
    const numA = parseInt(a.replace(/\D/g, ''), 10) || 0
    const numB = parseInt(b.replace(/\D/g, ''), 10) || 0
    return numB - numA
  })
})

// Reset cross-filter selection when it becomes unavailable
watch(availableAndroids, (list) => {
  if (selectedAndroid.value && !list.includes(selectedAndroid.value)) {
    selectedAndroid.value = ''
  }
})
watch(availableOsVersions, (list) => {
  if (selectedOs.value && !list.includes(selectedOs.value)) {
    selectedOs.value = ''
  }
})

const filteredDevices = computed(() => {
  if (!devices.value) return []

  let result = devices.value

  if (selectedBrand.value) {
    const sel = selectedBrand.value.toLowerCase()
    result = result.filter((d) => (d.brand || []).some((b) => b.toLowerCase() === sel))
  }

  if (selectedAndroid.value) {
    result = result.filter((d) => (d.android || []).includes(selectedAndroid.value))
  }

  if (selectedOs.value) {
    result = result.filter((d) => (d.supports || []).includes(selectedOs.value))
  }

  if (searchQuery.value) {
    const q = searchQuery.value.toLowerCase()
    result = result.filter(
      (d) =>
        d.device.toLowerCase().includes(q) ||
        (d.name?.zh || '').toLowerCase().includes(q) ||
        (d.name?.en || '').toLowerCase().includes(q)
    )
  }

  return result
})

useHead({
  title: `${t('devices')} - ${t('site')}`,
})
</script>
