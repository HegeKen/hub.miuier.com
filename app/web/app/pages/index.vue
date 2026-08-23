<template>
  <div class="container-page">
    <!-- Hero -->
    <section class="pb-14 pt-16 text-center sm:pb-16 sm:pt-24">
      <h1 class="text-4xl font-semibold tracking-tight sm:text-5xl text-HyperBlue">
        Mi<span class="text-[var(--color-accent)]">ROMs</span> HUB
      </h1>
      <p class="mt-3 text-base text-[var(--color-text-secondary)]">
        {{ $t('devicesSub') }}
      </p>
    </section>

    <!-- Stats -->
    <section v-if="devices" class="mx-auto mb-14 grid max-w-3xl grid-cols-2 gap-y-8 py-2 sm:grid-cols-4" aria-label="Statistics">
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
        <div class="text-3xl font-semibold tabular-nums">{{ brandCount }}</div>
        <div class="mt-1 text-sm text-[var(--color-text-secondary)]">{{ $t('brand') }}</div>
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
        <div class="flex items-center justify-between border-b border-[var(--color-border)] px-5 py-3.5">
          <h2 class="text-sm font-semibold text-[var(--color-text)]">{{ $t('recent7dList') }}</h2>
          <span class="rounded-full bg-[var(--color-accent-soft)] px-2.5 py-0.5 text-xs font-semibold tabular-nums text-[var(--color-accent)]">
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
              <tr class="border-b border-[var(--color-border)] text-left text-xs text-[var(--color-text-tertiary)]">
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
  </div>
</template>

<script setup>
const { locale } = useI18n()
const { t } = useI18n()
const { buildDevicesIndexUrl, buildStatsUrl } = useApi()

const { data: devices, error } = await useAsyncData(
  'devices-index',
  () => $fetch(buildDevicesIndexUrl())
)

// 近 7 日更新的 ROM 版本列表（由 generate-index.mjs 生成 v3/stats.json）
// cache: 'no-store' 绕过浏览器对旧版 stats.json 的缓存
const { data: stats } = await useAsyncData(
  'devices-stats',
  () => $fetch(buildStatsUrl(), { cache: 'no-store' }).catch(() => null)
)

const totalDevices = computed(() => devices.value?.length || 0)
const totalBranches = computed(() =>
  devices.value?.reduce((sum, d) => sum + (d.branchCount || 0), 0) || 0
)
const totalRoms = computed(() =>
  devices.value?.reduce((sum, d) => sum + (d.romCount || 0), 0) || 0
)
const brandCount = computed(() => {
  if (!devices.value) return 0
  const brands = new Set()
  for (const d of devices.value) {
    for (const b of d.brand || []) {
      brands.add(b.toLowerCase())
    }
  }
  return brands.size
})

useHead({
  title: `${t('site')} - ${t('devicesSub')}`,
})
</script>
