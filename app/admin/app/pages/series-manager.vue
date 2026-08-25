<template>
  <div class="container-admin py-8 sm:py-10">
    <!-- 标题 -->
    <header class="mb-6 flex flex-wrap items-end justify-between gap-3">
      <div>
        <h1 class="text-2xl font-semibold tracking-tight">设备系列管理</h1>
        <p class="mt-1 text-sm text-[var(--color-text-secondary)]">
          按品牌内产品系列归组，用设备 id 有序数组理清同品牌多马甲机型的展示/排序顺序
        </p>
      </div>
      <NuxtLink to="/" class="text-xs font-medium text-[var(--color-text-tertiary)] transition-colors hover:text-[var(--color-text)]">
        ← 返回仪表盘
      </NuxtLink>
    </header>

    <!-- 加载 -->
    <div v-if="loading" class="flex justify-center py-24">
      <span class="spinner" role="status" aria-label="加载中"></span>
    </div>

    <!-- 错误 -->
    <div v-else-if="loadError" class="card px-6 py-12 text-center">
      <p class="text-sm text-[var(--color-text-secondary)]">系列数据加载失败</p>
      <p class="mt-1 break-all font-mono text-xs text-[var(--color-danger)]">{{ loadError }}</p>
      <button type="button" class="btn-secondary mt-4" @click="load">重试</button>
    </div>

    <template v-else>
      <!-- 品牌筛选 -->
      <div class="mb-5 flex flex-wrap items-center gap-2">
        <button
          v-for="t in brandTabs"
          :key="t.key"
          type="button"
          class="btn-secondary !py-1.5 text-xs"
          :class="activeBrand === t.key && '!border-[var(--color-primary)] !text-[var(--color-primary)]'"
          @click="activeBrand = t.key"
        >
          {{ t.label }}
          <span class="ml-1.5 tabular-nums opacity-70">{{ countByBrand(t.key) }}</span>
        </button>
        <button type="button" class="btn-primary ml-auto !py-1.5 text-xs" @click="openCreate">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" aria-hidden="true">
            <path stroke-linecap="round" stroke-linejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
          </svg>
          新增系列
        </button>
      </div>

      <!-- 系列列表 -->
      <div v-if="filteredSeries.length === 0" class="card px-6 py-16 text-center">
        <p class="text-sm text-[var(--color-text-tertiary)]">暂无可展示的品牌系列，点击右上角「新增系列」开始</p>
      </div>

      <section v-else class="grid grid-cols-1 gap-4 lg:grid-cols-2">
        <div
          v-for="s in filteredSeries"
          :key="s.id"
          class="card group overflow-hidden transition-colors hover:border-[var(--color-border-strong)]"
        >
          <div class="flex flex-wrap items-center justify-between gap-2 border-b border-[var(--color-border)] px-5 py-3.5">
            <div class="min-w-0">
              <div class="flex items-center gap-2">
                <span class="badge border border-[var(--color-border)] text-[var(--color-text-secondary)]">
                  {{ brandLabel(s.brand) }}
                </span>
                <h2 class="truncate text-sm font-semibold text-[var(--color-text)]">{{ s.name_zh || '未命名系列' }}</h2>
                <span v-if="s.name_en" class="truncate text-xs text-[var(--color-text-tertiary)]">{{ s.name_en }}</span>
              </div>
            </div>
            <div class="flex items-center gap-1.5 opacity-0 transition-opacity group-hover:opacity-100">
              <span class="badge border border-[var(--color-border)] font-mono text-xs text-[var(--color-text-tertiary)]">
                #{{ s.sort_order }}
              </span>
              <button type="button" class="btn-secondary !px-2 !py-1 text-xs" @click="openEdit(s)">编辑</button>
              <button type="button" class="btn-secondary !px-2 !py-1 text-xs !text-[var(--color-danger)]" @click="confirmDelete(s)">删除</button>
            </div>
          </div>

          <div class="px-5 py-4">
            <p class="mb-2 text-xs text-[var(--color-text-tertiary)]">
              {{ s.devices.length }} 个设备 · 数组顺序即展示顺序
            </p>
            <div v-if="s.devices.length" class="flex flex-wrap gap-2">
              <span
                v-for="(d, idx) in s.devices"
                :key="d.id"
                class="inline-flex items-center gap-1.5 rounded-md border border-[var(--color-border)] bg-[var(--color-bg-subtle)] px-2 py-1 text-xs"
              >
                <span class="font-mono text-[10px] text-[var(--color-text-tertiary)]">{{ idx + 1 }}</span>
                <span class="font-medium text-[var(--color-text)]">{{ d.name }}</span>
                <span class="font-mono text-[10px] text-[var(--color-text-tertiary)]">{{ d.device }}</span>
              </span>
            </div>
            <p v-else class="text-xs text-[var(--color-text-tertiary)]">尚未绑定设备</p>
          </div>
        </div>
      </section>
    </template>

    <!-- 编辑 / 新增弹窗 -->
    <SeriesModal
      :open="modalOpen"
      :record="formRecord"
      :brands="brands"
      :devices="devices"
      @close="modalOpen = false"
      @saved="load"
    />
  </div>
</template>

<script setup>
const { deleteRecord } = useAdminApi()
const { push } = useToast()

const loading = ref(false)
const loadError = ref('')
const data = ref(null)

const activeBrand = ref('all')

const brandTabs = computed(() => [
  { key: 'all', label: '全部' },
  ...(data.value?.brands || []),
])

const devices = computed(() => data.value?.devices || [])
const series = computed(() => data.value?.series || [])
const brands = computed(() => data.value?.brands || [])

/** 按品牌统计 series 数量 */
const countByBrand = (key) => {
  if (key === 'all') return series.value.length
  return series.value.filter((s) => s.brand === key).length
}

const brandLabel = (key) => {
  const found = brands.value.find((b) => b.key === key)
  return found ? found.label : key
}

const filteredSeries = computed(() => {
  if (activeBrand.value === 'all') return series.value
  return series.value.filter((s) => s.brand === activeBrand.value)
})

const load = async () => {
  loading.value = true
  loadError.value = ''
  try {
    data.value = await $fetch('/api/series-manager')
  } catch (e) {
    loadError.value = errorMessage(e)
  } finally {
    loading.value = false
  }
}

// ---- 编辑弹窗 ----
const modalOpen = ref(false)
const formRecord = ref(null)

const openCreate = () => {
  formRecord.value = { id: undefined, brand: '', name_zh: '', name_en: '', sort_order: 0, device_ids: [] }
  modalOpen.value = true
}

const openEdit = (s) => {
  formRecord.value = {
    id: s.id,
    brand: s.brand,
    name_zh: s.name_zh,
    name_en: s.name_en,
    sort_order: s.sort_order,
    device_ids: [...s.device_ids],
  }
  modalOpen.value = true
}

const confirmDelete = async (s) => {
  if (!window.confirm(`确认删除系列「${s.name_zh || s.name_en || s.id}」？`)) return
  try {
    await deleteRecord('series', s.id)
    push('success', `系列 #${s.id} 已删除`)
    await load()
  } catch (e) {
    push('error', errorMessage(e))
  }
}

onMounted(load)
</script>
