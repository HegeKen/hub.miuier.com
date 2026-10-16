<template>
  <div class="container-admin py-8">
    <div class="mb-6 flex flex-wrap items-end justify-between gap-4">
      <div>
        <h1 class="text-xl font-semibold text-[var(--color-text)]">站点日志</h1>
        <p class="mt-1 text-sm text-[var(--color-text-secondary)]">
          按 git 记录生成的版本更新日志（sitelog 表）。管理后台展示自己的日志，官网前端展示 hub.web。
        </p>
        <p v-if="latest" class="mt-2 flex items-center gap-2 text-sm">
          <span class="text-[var(--color-text-secondary)]">当前版本</span>
          <span class="font-mono text-base font-semibold text-[var(--color-text)]">{{ latest }}</span>
        </p>
      </div>

      <!-- 站点切换：管理后台 / 官网前端 -->
      <div class="flex flex-wrap gap-2">
        <button
          v-for="item in siteTabs"
          :key="item.value"
          type="button"
          class="filter-pill"
          :class="site === item.value ? 'filter-pill-active' : ''"
          @click="switchSite(item.value)"
        >
          {{ item.label }}
        </button>
      </div>
    </div>

    <div v-if="loading" class="flex justify-center py-24">
      <span class="spinner" role="status" aria-label="Loading"></span>
    </div>

    <div v-else-if="loadError" class="card px-4 py-10 text-center">
      <p class="text-sm text-[var(--color-text-secondary)]">{{ loadError }}</p>
      <button type="button" class="btn-secondary mt-4" @click="load">重试</button>
    </div>

    <div v-else-if="!items.length" class="card px-4 py-10 text-center text-sm text-[var(--color-text-secondary)]">
      暂无站点日志
    </div>

    <!-- 版本时间线：最新一版在最上方 -->
    <ol v-else class="ms-1 space-y-7 border-s border-[var(--color-border)] ps-6">
      <li v-for="(entry, index) in items" :key="entry.id" class="relative">
        <span
          class="absolute -start-[30px] top-1.5 h-2.5 w-2.5 rounded-full"
          :class="index === 0 ? 'bg-[var(--color-accent)]' : 'bg-[var(--color-border-strong)]'"
          aria-hidden="true"
        />
        <div class="flex flex-wrap items-center gap-x-3 gap-y-1">
          <span class="font-mono text-base font-semibold text-[var(--color-text)]">{{ entry.ver }}</span>
          <span class="badge border border-[var(--color-border)] text-[var(--color-text-tertiary)]">{{ entry.date }}</span>
          <span v-if="index === 0" class="badge bg-[var(--color-accent-soft)] text-[var(--color-accent)]">当前版本</span>
          <span class="text-xs text-[var(--color-text-tertiary)]">{{ entry.log.length }} 条</span>
        </div>
        <ul v-if="entry.log.length" class="mt-2.5 space-y-1.5">
          <li
            v-for="(item, i) in entry.log"
            :key="i"
            class="flex gap-2.5 text-sm leading-relaxed text-[var(--color-text-secondary)]"
          >
            <span class="mt-2 h-1 w-1 shrink-0 rounded-full bg-[var(--color-border-strong)]" aria-hidden="true" />
            <span>{{ item.zh || item.en }}</span>
          </li>
        </ul>
      </li>
    </ol>
  </div>
</template>

<script setup>
const SITE_TABS = [
  { value: 'hub.admin', label: '管理后台' },
  { value: 'hub.web', label: '官网前端' },
]

const loading = ref(false)
const loadError = ref('')
const site = ref('hub.admin')
const items = ref([])
const latest = ref('')

const siteTabs = SITE_TABS

const load = async () => {
  loading.value = true
  loadError.value = ''
  try {
    const data = await $fetch('/api/sitelog/list', { query: { site: site.value } })
    items.value = data?.items || []
    latest.value = data?.latest || ''
  } catch (e) {
    loadError.value = errorMessage(e)
    items.value = []
    latest.value = ''
  } finally {
    loading.value = false
  }
}

const switchSite = (value) => {
  if (site.value === value) return
  site.value = value
  load()
}

onMounted(load)
</script>
