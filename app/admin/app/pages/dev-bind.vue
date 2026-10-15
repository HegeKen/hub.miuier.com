<template>
  <div class="container-admin py-8 sm:py-10">
    <header class="mb-6">
      <h1 class="text-2xl font-semibold tracking-tight">开发版绑定 ROM</h1>
      <p class="mt-1 max-w-3xl text-sm text-[var(--color-text-secondary)]">
        开发版周报的机型清单不单独存表——「第 N 周有哪些机型」= <code class="font-mono text-xs">roms</code> 表中
        <code class="font-mono text-xs">dev_week = N</code> 且分区相同的行。导入时会按
        <span class="font-medium text-[var(--color-text)]">该周版本号</span>自动绑定，
        没有版本号或版本号对不上的周次（老周报、延期公告）在这里手动关联。
      </p>
    </header>

    <!-- 周次选择 -->
    <section class="card mb-5 px-4 py-3">
      <div class="flex flex-wrap items-end gap-3">
        <label class="text-xs text-[var(--color-text-secondary)]">
          周数
          <input v-model.number="week" type="number" min="1" class="input-base mt-1 !w-28 !py-1.5 !text-sm" />
        </label>
        <label class="text-xs text-[var(--color-text-secondary)]">
          区域
          <select v-model="region" class="input-base mt-1 !w-32 !py-1.5 !text-sm">
            <option value="cn">大陆版</option>
            <option value="global">国际版</option>
          </select>
        </label>
        <button type="button" class="btn-primary" :disabled="loading" @click="loadAll">
          {{ loading ? '加载中…' : '载入该周' }}
        </button>
        <div class="ml-auto flex items-center gap-2">
          <span class="text-xs text-[var(--color-text-tertiary)]">快速跳转</span>
          <select class="input-base !w-auto !py-1.5 !text-xs" @change="jumpTo($event.target.value)">
            <option value="">选择周次…</option>
            <option v-for="w in weekOptions" :key="`${w.week}-${w.region}`" :value="`${w.week}|${w.region}`">
              第 {{ w.week }} 周 · {{ w.region === 'cn' ? '大陆版' : '国际版' }} · {{ w.bigver || '—' }}
              {{ w.bound ? `（${w.bound} 条 / ${w.devices} 台）` : '（未绑定）' }}
            </option>
          </select>
        </div>
      </div>

      <div v-if="dev" class="mt-3 flex flex-wrap items-center gap-x-4 gap-y-1 border-t border-[var(--color-border)] pt-3 text-xs">
        <span class="font-medium text-[var(--color-text)]">第 {{ dev.week }} 周 · {{ dev.region === 'cn' ? '大陆版' : '国际版' }}</span>
        <span class="text-[var(--color-text-tertiary)]">{{ dev.bigver || '—' }}</span>
        <span class="font-mono text-[var(--color-text-secondary)]">{{ dev.versions || '（无版本号）' }}</span>
        <span class="text-[var(--color-text-tertiary)]">{{ dev.release || '无发布日期' }}</span>
        <span class="rounded-full px-2 py-0.5" :class="bound.length ? 'bg-[var(--color-ok)]/10 text-[var(--color-ok)]' : 'bg-[var(--color-danger)]/10 text-[var(--color-danger)]'">
          已绑定 {{ bound.length }} 条 / {{ boundDevices }} 台机型
        </span>
        <span class="text-[var(--color-text-tertiary)]">来源 {{ dev.source || '—' }}</span>
      </div>
      <p v-if="dev && !bound.length" class="mt-3 rounded-lg bg-[var(--color-warn)]/10 px-3 py-2 text-xs text-[var(--color-warn)]">
        本周还没有绑定任何 ROM：从右侧按机型代号或版本号检索，勾选后点「绑定选中」即可。
      </p>
      <p v-else-if="loaded && !dev" class="mt-3 border-t border-[var(--color-border)] pt-3 text-xs text-[var(--color-danger)]">
        没有第 {{ week }} 周（{{ region === 'cn' ? '大陆版' : '国际版' }}）的开发版记录，请先导入该周数据。
      </p>
    </section>

    <div class="grid grid-cols-1 gap-5 lg:grid-cols-2">
      <!-- 已绑定 -->
      <section class="card flex flex-col overflow-hidden">
        <div class="flex items-center justify-between border-b border-[var(--color-border)] px-4 py-3">
          <h2 class="text-sm font-semibold">已绑定到本周（{{ bound.length }}）</h2>
          <button
            type="button"
            class="btn-danger !py-1 !text-xs"
            :disabled="!bound.length || saving"
            @click="unbind(bound.map((r) => r.id))"
          >
            全部解绑
          </button>
        </div>
        <div class="max-h-[32rem] divide-y divide-[var(--color-border)] overflow-y-auto">
          <div v-for="rom in bound" :key="rom.id" class="flex items-center gap-3 px-4 py-2.5">
            <div class="min-w-0 flex-1">
              <p class="truncate text-sm text-[var(--color-text)]">
                {{ rom.name.zh || rom.device }}
                <span class="ml-1 font-mono text-xs text-[var(--color-text-tertiary)]">{{ rom.device }}</span>
              </p>
              <p class="mt-0.5 truncate font-mono text-xs text-[var(--color-text-tertiary)]">
                {{ rom.version || '—' }}<template v-if="rom.android"> · Android {{ rom.android }}</template>
                <template v-if="rom.release"> · {{ rom.release }}</template>
              </p>
            </div>
            <button type="button" class="btn-secondary !py-1 !text-xs" :disabled="saving" @click="unbind([rom.id])">
              解绑
            </button>
          </div>
          <p v-if="!bound.length" class="px-4 py-6 text-center text-xs text-[var(--color-text-tertiary)]">
            本周还没有绑定任何 ROM，从右侧检索并关联。
          </p>
        </div>
      </section>

      <!-- 候选 ROM -->
      <section class="card flex flex-col overflow-hidden">
        <div class="flex flex-wrap items-center gap-2 border-b border-[var(--color-border)] px-4 py-3">
          <h2 class="mr-auto text-sm font-semibold">检索 ROM（{{ total }}）</h2>
          <label class="text-xs text-[var(--color-text-secondary)]">
            绑定状态
            <select v-model="filters.bound" class="input-base !w-auto !py-1 !text-xs" @change="search(1)">
              <option value="free">仅未绑定</option>
              <option value="all">全部</option>
              <option value="other">已绑到别周</option>
            </select>
          </label>
          <label class="text-xs text-[var(--color-text-secondary)]">
            年份
            <input v-model.number="filters.year" type="number" placeholder="不限" class="input-base !w-20 !py-1 !text-xs" @keyup.enter="search(1)" />
          </label>
        </div>

        <div class="flex items-center gap-2 border-b border-[var(--color-border)] px-4 py-3">
          <input
            v-model="filters.search"
            type="search"
            class="input-base !py-1.5 !text-sm"
            placeholder="机型代号 / 版本号 / 包文件名，如 cepheus、21.6.16"
            @keyup.enter="search(1)"
          />
          <button type="button" class="btn-primary !py-1.5" :disabled="searching" @click="search(1)">
            {{ searching ? '检索中…' : '检索' }}
          </button>
        </div>

        <div v-if="versionFacets.length > 1" class="flex flex-wrap items-center gap-1.5 border-b border-[var(--color-border)] px-4 py-2">
          <span class="text-xs text-[var(--color-text-tertiary)]">完整版本号</span>
          <button
            v-for="facet in versionFacets"
            :key="facet.version"
            type="button"
            class="filter-pill !py-0.5 !text-xs"
            :class="filters.search === facet.version ? 'filter-pill-active' : ''"
            @click="pickVersion(facet.version)"
          >
            {{ facet.version }}<template v-if="facet.year">（{{ facet.year }}）</template>
            · {{ facet.total }}
          </button>
        </div>

        <div class="max-h-[28rem] divide-y divide-[var(--color-border)] overflow-y-auto">
          <label
            v-for="rom in candidates"
            :key="rom.id"
            class="flex cursor-pointer items-start gap-3 px-4 py-2.5 hover:bg-[var(--color-bg-subtle)]"
          >
            <input v-model="selected" type="checkbox" :value="rom.id" class="mt-1" />
            <div class="min-w-0 flex-1">
              <p class="truncate text-sm text-[var(--color-text)]">
                {{ rom.name.zh || rom.device }}
                <span class="ml-1 font-mono text-xs text-[var(--color-text-tertiary)]">{{ rom.device }}</span>
                <span v-if="rom.devWeek && rom.devWeek !== week" class="ml-1 rounded-full bg-[var(--color-warn)]/10 px-2 py-0.5 text-[11px] text-[var(--color-warn)]">
                  已绑第 {{ rom.devWeek }} 周{{ rom.devRegion ? `（${rom.devRegion === 'cn' ? '大陆' : '国际'}）` : '' }}
                </span>
                <span v-else-if="rom.zone !== zone" class="ml-1 rounded-full bg-[var(--color-bg-subtle)] px-2 py-0.5 text-[11px] text-[var(--color-text-tertiary)]">
                  分区 {{ rom.zone }} ≠ {{ zone }}
                </span>
              </p>
              <p class="mt-0.5 truncate font-mono text-xs text-[var(--color-text-tertiary)]">
                {{ rom.version || '—' }}<template v-if="rom.android"> · Android {{ rom.android }}</template>
                <template v-if="rom.release"> · {{ rom.release }}</template>
                <template v-if="rom.recovery"> · {{ rom.recovery }}</template>
              </p>
            </div>
          </label>
          <p v-if="!candidates.length" class="px-4 py-6 text-center text-xs text-[var(--color-text-tertiary)]">
            没有匹配的 ROM。
          </p>
        </div>

        <div class="flex flex-wrap items-center gap-2 border-t border-[var(--color-border)] px-4 py-3">
          <button type="button" class="btn-primary" :disabled="!selected.length || saving" @click="bind(false)">
            绑定选中 {{ selected.length }} 条
          </button>
          <button
            type="button"
            class="btn-secondary"
            :disabled="!selected.length || saving"
            title="把已绑定到其他周的 ROM 也抢过来（用于纠正错绑）"
            @click="bind(true)"
          >
            强制绑定
          </button>
          <button type="button" class="btn-secondary" :disabled="!selected.length" @click="selected = []">清空选择</button>
          <div class="ml-auto flex items-center gap-2 text-xs text-[var(--color-text-tertiary)]">
            <button type="button" class="btn-secondary !py-1 !text-xs" :disabled="page <= 1" @click="search(page - 1)">上一页</button>
            <span>{{ page }} / {{ totalPages }}</span>
            <button type="button" class="btn-secondary !py-1 !text-xs" :disabled="page >= totalPages" @click="search(page + 1)">下一页</button>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
const { push } = useToast()

const week = ref(500)
const region = ref('cn')
const dev = ref(null)
const zone = ref(1)
const bound = ref([])
const candidates = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = 40
const selected = ref([])
const loading = ref(false)
const searching = ref(false)
const saving = ref(false)
const loaded = ref(false)
const weekOptions = ref([])
const filters = ref({ search: '', bound: 'free', year: null })

const boundDevices = computed(() => new Set(bound.value.map((r) => r.device)).size)
const totalPages = computed(() => Math.max(Math.ceil(total.value / pageSize), 1))
const versionFacets = ref([])

const pickVersion = (version) => {
  filters.value.search = version
  search(1)
}

const errorText = (e) => errorMessage(e)

const loadWeeks = async () => {
  try {
    const res = await $fetch('/api/dev/weeks')
    weekOptions.value = res.items || []
  } catch (e) {
    push('error', errorText(e))
  }
}

const loadBound = async () => {
  const res = await $fetch('/api/dev/bind', { params: { week: week.value, region: region.value } })
  dev.value = res.dev
  zone.value = res.zone ?? (region.value === 'global' ? 2 : 1)
  bound.value = res.roms || []
  loaded.value = true
}

const search = async (targetPage = 1) => {
  page.value = Math.max(targetPage, 1)
  searching.value = true
  try {
    const res = await $fetch('/api/dev/roms', {
      params: {
        week: week.value,
        region: region.value,
        search: filters.value.search,
        bound: filters.value.bound,
        year: filters.value.year || '',
        page: page.value,
        pageSize,
      },
    })
    candidates.value = res.items || []
    total.value = res.total || 0
    versionFacets.value = res.versionFacets || []
  } catch (e) {
    push('error', errorText(e))
  } finally {
    searching.value = false
  }
}

const loadAll = async () => {
  loading.value = true
  selected.value = []
  try {
    await loadBound()
    // 该周没绑上 ROM 时，用它的版本号预填检索词（老周报的版本号常是 5.20 这种短写法，
    // LIKE 检索能直接命中库里的 21.5.20），方便快速关联
    if (!bound.value.length && dev.value?.versions) {
      filters.value.search = dev.value.versions
    }
    await search(1)
  } catch (e) {
    push('error', errorText(e))
  } finally {
    loading.value = false
  }
}

const jumpTo = (value) => {
  if (!value) return
  const [w, r] = String(value).split('|')
  week.value = Number(w)
  region.value = r || 'cn'
  loadAll()
}

const bind = async (force) => {
  saving.value = true
  try {
    const res = await $fetch('/api/dev/bind', {
      method: 'POST',
      body: { week: week.value, region: region.value, action: 'bind', romIds: selected.value, force },
    })
    const extra = []
    if (res.skippedTaken?.length) extra.push(`${res.skippedTaken.length} 条已绑到其他周（需强制绑定）`)
    if (res.skippedZone?.length) extra.push(`${res.skippedZone.length} 条分区不一致`)
    push(res.updated ? 'success' : 'error',
      `绑定 ${res.updated} 条${extra.length ? `，跳过：${extra.join('、')}` : ''}`)
    selected.value = []
    await loadBound()
    await search(page.value)
  } catch (e) {
    push('error', errorText(e))
  } finally {
    saving.value = false
  }
}

const unbind = async (romIds) => {
  if (!romIds.length) return
  saving.value = true
  try {
    const res = await $fetch('/api/dev/bind', {
      method: 'POST',
      body: { week: week.value, region: region.value, action: 'unbind', romIds },
    })
    push('success', `已解绑 ${res.updated} 条`)
    await loadBound()
    await search(page.value)
  } catch (e) {
    push('error', errorText(e))
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadWeeks()
  loadAll()
})
</script>
