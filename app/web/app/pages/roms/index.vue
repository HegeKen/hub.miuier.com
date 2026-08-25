<template>
  <div class="container-page py-10 sm:py-14">
    <header class="mb-8">
      <h1 class="text-2xl font-semibold tracking-tight sm:text-3xl">{{ $t('roms') }}</h1>
      <p class="mt-2 text-sm text-[var(--color-text-secondary)]">{{ $t('romsSub') }}</p>
    </header>

    <!-- Loading -->
    <div v-if="pending" class="flex justify-center py-20">
      <span class="spinner" role="status" aria-label="Loading"></span>
    </div>

    <!-- Error -->
    <div v-else-if="error" class="py-20 text-center">
      <p class="text-sm text-[var(--color-text-secondary)]">Failed to load ROM index</p>
    </div>

    <!-- OS Version Grid -->
    <div v-else class="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
      <NuxtLink
        v-for="item in osIndex"
        :key="item.os"
        :to="'/' + locale + '/roms/' + item.os"
        class="group rounded-xl border border-[var(--color-border)] bg-[var(--color-bg-surface)] px-4 py-4 transition-colors hover:bg-[var(--color-bg-subtle)]"
      >
        <div class="flex items-center justify-between gap-4">
          <span class="font-mono text-lg font-semibold text-[var(--color-text)]">{{ osLabel(item.os) }}</span>
          <svg
            class="h-4 w-4 shrink-0 text-[var(--color-text-tertiary)] transition-all group-hover:translate-x-0.5 group-hover:text-[var(--color-text)]"
            xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" aria-hidden="true"
          >
            <path stroke-linecap="round" stroke-linejoin="round" d="m8.25 4.5 7.5 7.5-7.5 7.5" />
          </svg>
        </div>
        <p class="mt-1.5 text-xs text-[var(--color-text-tertiary)]">
          {{ $t('totalRoms', { count: item.count }) }} · {{ $t('totalDevices', { count: item.deviceCount }) }}
        </p>
      </NuxtLink>
    </div>
  </div>
</template>

<script setup>
const { locale } = useI18n()
const { t } = useI18n()
const { buildRomsIndexUrl } = useApi()

// OS 名称本地化：Stock 中文「原生安卓」、STAN 中文「现代原生安卓」，其余大版本编码保持不变
const osLabel = (os) =>
  os === 'Stock' && locale.value.startsWith('zh')
    ? t('osStock')
    : os === 'STAN' && locale.value.startsWith('zh')
      ? t('osStan')
      : os

const { data: osIndex, pending, error } = await useAsyncData(
  'roms-index',
  () => $fetch(buildRomsIndexUrl())
)

useHead({
  title: computed(() => `${t('roms')} - ${t('site')}`),
})
</script>
