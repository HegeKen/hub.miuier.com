<template>
  <div class="container-admin py-8 sm:py-10">
    <header class="mb-6">
      <h1 class="text-2xl font-semibold tracking-tight">仪表盘</h1>
      <p class="mt-1 text-sm text-[var(--color-text-secondary)]">数据库直连概览（MySQL · miroms 库）</p>
    </header>

    <!-- 加载 -->
    <div v-if="pending" class="flex justify-center py-24">
      <span class="spinner" role="status" aria-label="加载中"></span>
    </div>

    <!-- 错误 -->
    <div v-else-if="error" class="card px-6 py-12 text-center">
      <p class="text-sm text-[var(--color-text-secondary)]">无法连接到数据库</p>
      <p class="mt-1 break-all font-mono text-xs text-[var(--color-danger)]">{{ error }}</p>
      <button type="button" class="btn-secondary mt-4" @click="load">重试</button>
    </div>

    <template v-else>
      <!-- 统计卡片 -->
      <section class="grid grid-cols-2 gap-4 lg:grid-cols-4" aria-label="数据统计">
        <NuxtLink to="/roms" class="card group p-5 transition-colors hover:border-[var(--color-border-strong)]">
          <p class="text-sm text-[var(--color-text-tertiary)]">ROM 总数</p>
          <p class="mt-1 text-3xl font-semibold tabular-nums text-[var(--color-text)] group-hover:text-[var(--color-accent)]">
            {{ fmt(data.counts.roms) }}
          </p>
        </NuxtLink>
        <NuxtLink to="/devices" class="card group p-5 transition-colors hover:border-[var(--color-border-strong)]">
          <p class="text-sm text-[var(--color-text-tertiary)]">设备总数</p>
          <p class="mt-1 text-3xl font-semibold tabular-nums text-[var(--color-text)] group-hover:text-[var(--color-accent)]">
            {{ fmt(data.counts.devices) }}
          </p>
        </NuxtLink>
        <NuxtLink to="/branches" class="card group p-5 transition-colors hover:border-[var(--color-border-strong)]">
          <p class="text-sm text-[var(--color-text-tertiary)]">分支总数</p>
          <p class="mt-1 text-3xl font-semibold tabular-nums text-[var(--color-text)] group-hover:text-[var(--color-accent)]">
            {{ fmt(data.counts.branches) }}
          </p>
        </NuxtLink>
        <NuxtLink to="/roms" class="card group p-5 transition-colors hover:border-[var(--color-border-strong)]">
          <p class="text-sm text-[var(--color-text-tertiary)]">今日新增 ROM</p>
          <p class="mt-1 text-3xl font-semibold tabular-nums text-[var(--color-text)] group-hover:text-[var(--color-accent)]">
            {{ fmt(data.romsStats.todayNew) }}
          </p>
        </NuxtLink>
      </section>

      <!-- 近 7 天新增趋势 -->
      <section class="mt-6">
        <div class="card p-5">
          <div class="mb-4 flex items-center justify-between gap-3">
            <div>
              <h2 class="text-sm font-semibold text-[var(--color-text)]">近 7 天新增趋势</h2>
              <p class="mt-0.5 text-xs text-[var(--color-text-secondary)]">ROM 按录入日期统计，设备/分支按最近记录估算</p>
            </div>
            <div v-if="dailyData" class="flex items-center gap-4 text-xs">
              <span class="flex items-center gap-1.5">
                <span class="inline-block h-2.5 w-2.5 rounded-sm bg-[var(--color-accent)]"></span>
                ROM
              </span>
              <span class="flex items-center gap-1.5">
                <span class="inline-block h-2.5 w-2.5 rounded-sm bg-[var(--color-ok)]"></span>
                设备
              </span>
              <span class="flex items-center gap-1.5">
                <span class="inline-block h-2.5 w-2.5 rounded-sm bg-[var(--color-warn)]"></span>
                分支
              </span>
            </div>
          </div>

          <div v-if="dailyPending" class="flex justify-center py-8">
            <span class="spinner" role="status" aria-label="加载中"></span>
          </div>

          <div v-else-if="dailyData" class="space-y-4">
            <!-- 分组柱状图 -->
            <div>
              <div class="flex items-end gap-1" style="height: 140px;">
                <div
                  v-for="day in dailyData.days"
                  :key="day.date"
                  class="group flex h-full flex-1 items-end justify-center gap-0.5"
                >
                  <div
                    class="relative w-3 rounded-t bg-[var(--color-accent)] transition-all duration-300"
                    :style="{ height: barHeight(day.roms) }"
                    :class="day.roms === 0 && 'opacity-20'"
                  >
                    <span v-if="day.roms > 0" class="absolute -top-4 left-1/2 -translate-x-1/2 text-[9px] font-medium tabular-nums text-[var(--color-accent)]">{{ day.roms }}</span>
                  </div>
                  <div
                    class="relative w-3 rounded-t bg-[var(--color-ok)] transition-all duration-300"
                    :style="{ height: barHeight(day.devices) }"
                    :class="day.devices === 0 && 'opacity-20'"
                  >
                    <span v-if="day.devices > 0" class="absolute -top-4 left-1/2 -translate-x-1/2 text-[9px] font-medium tabular-nums text-[var(--color-ok)]">{{ day.devices }}</span>
                  </div>
                  <div
                    class="relative w-3 rounded-t bg-[var(--color-warn)] transition-all duration-300"
                    :style="{ height: barHeight(day.branches) }"
                    :class="day.branches === 0 && 'opacity-20'"
                  >
                    <span v-if="day.branches > 0" class="absolute -top-4 left-1/2 -translate-x-1/2 text-[9px] font-medium tabular-nums text-[var(--color-warn)]">{{ day.branches }}</span>
                  </div>
                </div>
              </div>
              <div class="mt-1 flex gap-1">
                <div v-for="day in dailyData.days" :key="'l-' + day.date" class="flex-1 text-center text-[10px] text-[var(--color-text-tertiary)]">
                  {{ day.date.slice(5) }}
                </div>
              </div>
            </div>

            <!-- 汇总条 -->
            <div class="grid grid-cols-3 gap-3 border-t border-[var(--color-border)] pt-3">
              <div class="rounded-lg bg-[var(--color-bg-subtle)] p-2.5 text-center">
                <p class="text-lg font-semibold tabular-nums text-[var(--color-accent)]">{{ dailyTotalRoms }}</p>
                <p class="mt-0.5 text-[10px] text-[var(--color-text-tertiary)]">ROM 近 7 天新增</p>
              </div>
              <div class="rounded-lg bg-[var(--color-bg-subtle)] p-2.5 text-center">
                <p class="text-lg font-semibold tabular-nums text-[var(--color-ok)]">{{ dailyTotalDevs }}</p>
                <p class="mt-0.5 text-[10px] text-[var(--color-text-tertiary)]">设备近 7 天新增</p>
              </div>
              <div class="rounded-lg bg-[var(--color-bg-subtle)] p-2.5 text-center">
                <p class="text-lg font-semibold tabular-nums text-[var(--color-warn)]">{{ dailyTotalBr }}</p>
                <p class="mt-0.5 text-[10px] text-[var(--color-text-tertiary)]">分支近 7 天新增</p>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- 数据自查 -->
      <section class="mt-6">
        <div class="card p-5">
          <div class="mb-3 flex flex-wrap items-center justify-between gap-3">
            <div>
              <h2 class="text-sm font-semibold text-[var(--color-text)]">数据自查</h2>
              <p class="mt-0.5 text-xs text-[var(--color-text-secondary)]">
                依据 data/db_structure 字段注释与示例，主动筛选不符合逻辑的数据（全表扫描，可能需要几秒）
              </p>
            </div>
            <button type="button" class="btn-primary !py-1.5 text-xs" :disabled="checking" @click="runCheck">
              {{ checking ? '扫描中…' : checkResult ? '重新扫描' : '运行自查' }}
            </button>
          </div>

          <div v-if="checking" class="flex items-center justify-center gap-2 py-8">
            <span class="spinner" role="status" aria-label="扫描中"></span>
            <span class="text-sm text-[var(--color-text-secondary)]">正在扫描 devices / branches / roms …</span>
          </div>

          <div v-else-if="checkError" class="alert-danger">
            <p class="text-sm font-medium">检查失败</p>
            <p class="mt-1 break-all font-mono text-xs">{{ checkError }}</p>
          </div>

          <div v-else-if="checkResult" class="grid grid-cols-1 gap-3 sm:grid-cols-3">
            <NuxtLink
              v-for="item in checkResult.tables"
              :key="item.table"
              :to="'/' + item.table"
              class="rounded-lg border border-[var(--color-border)] bg-[var(--color-bg-subtle)] p-3 transition-colors hover:border-[var(--color-border-strong)]"
            >
              <div class="flex items-center justify-between gap-2">
                <p class="font-mono text-sm font-medium text-[var(--color-text)]">{{ item.table }}</p>
                <span
                  class="rounded-full px-2 py-0.5 text-xs font-semibold tabular-nums"
                  :class="item.summary.total > 0
                    ? 'bg-[var(--color-accent-soft)] text-[var(--color-accent)]'
                    : 'bg-[var(--color-ok)]/10 text-[var(--color-ok)]'"
                >
                  {{ item.summary.total > 0 ? item.summary.total.toLocaleString() + ' 条违规' : '无违规 ✓' }}
                </span>
              </div>
              <p class="mt-1.5 text-xs text-[var(--color-text-tertiary)]">
                <span class="text-[var(--color-danger)]">错误 {{ item.summary.errors.toLocaleString() }}</span>
                <span class="mx-1">·</span>
                <span class="text-[var(--color-warn)]">警告 {{ item.summary.warnings.toLocaleString() }}</span>
                <span class="mx-1">·</span>前往查看 →
              </p>
            </NuxtLink>
            <p v-if="checkTime" class="sm:col-span-3 text-right text-xs text-[var(--color-text-tertiary)]">
              最近检查：{{ checkTime }}
            </p>
          </div>

          <div v-else class="py-6 text-center text-sm text-[var(--color-text-tertiary)]">
            尚未运行自查，点击「运行自查」开始扫描
          </div>
        </div>
      </section>

      <!-- 数据库信息 -->
      <section class="mt-6 grid gap-4 lg:grid-cols-3">
        <div class="card p-5">
          <h2 class="mb-3 text-sm font-semibold text-[var(--color-text)]">数据库状态</h2>
          <dl class="space-y-2 text-sm">
            <div class="flex items-center justify-between gap-3">
              <dt class="text-[var(--color-text-secondary)]">连接</dt>
              <dd class="flex items-center gap-1.5 font-medium text-[var(--color-ok)]">
                <span class="dot bg-[var(--color-ok)]" aria-hidden="true"></span>正常
              </dd>
            </div>
            <div class="flex items-center justify-between gap-3">
              <dt class="text-[var(--color-text-secondary)]">版本</dt>
              <dd class="font-mono text-xs">{{ data.ping.version }}</dd>
            </div>
            <div class="flex items-center justify-between gap-3">
              <dt class="text-[var(--color-text-secondary)]">最近录入日期</dt>
              <dd class="font-mono text-xs">{{ data.romsStats.latestInsdate || '—' }}</dd>
            </div>
          </dl>
        </div>

        <div class="card p-5 lg:col-span-2">
          <h2 class="mb-3 text-sm font-semibold text-[var(--color-text)]">表空间占用</h2>
          <div class="grid grid-cols-1 gap-3 sm:grid-cols-3">
            <div v-for="(info, name) in data.sizes" :key="name" class="rounded-lg border border-[var(--color-border)] bg-[var(--color-bg-subtle)] p-3">
              <p class="font-mono text-sm font-medium text-[var(--color-text)]">{{ name }}</p>
              <p class="mt-1 text-xs text-[var(--color-text-tertiary)]">
                {{ fmt(info.rows) }} 行
                <span class="mx-1">·</span>
                {{ fmtBytes(info.bytes) }}
              </p>
            </div>
          </div>
        </div>
      </section>

      <!-- 最近记录 -->
      <section class="mt-6 grid gap-4 xl:grid-cols-3">
        <div v-for="block in recentBlocks" :key="block.table" class="card overflow-hidden">
          <div class="flex items-center justify-between border-b border-[var(--color-border)] px-4 py-3">
            <h2 class="text-sm font-semibold text-[var(--color-text)]">{{ block.label }}（最近 5 条）</h2>
            <NuxtLink :to="block.to" class="text-xs font-medium text-[var(--color-accent)] hover:underline">
              管理全部
            </NuxtLink>
          </div>
          <div class="overflow-x-auto">
            <table class="table-base">
              <thead>
                <tr class="border-b border-[var(--color-border)] bg-[var(--color-bg-subtle)]">
                  <th v-for="col in block.cols" :key="col" class="table-th whitespace-nowrap">{{ col }}</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-[var(--color-border)]">
                <tr v-for="row in block.rows" :key="String(row.id)" class="hover:bg-[var(--color-bg-subtle)]">
                  <td
                    v-for="col in block.cols"
                    :key="col"
                    class="table-td max-w-40 truncate font-mono text-xs text-[var(--color-text)]"
                    :title="row[col] !== null && row[col] !== undefined ? String(row[col]) : ''"
                  >
                    <span v-if="row[col] === null || row[col] === undefined" class="text-[var(--color-text-tertiary)]">NULL</span>
                    <span v-else>{{ String(row[col]) }}</span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </section>
    </template>
  </div>
</template>

<script setup>
const { getStats, getCheckSummary } = useAdminApi()

const data = ref(null)
const pending = ref(true)
const error = ref('')

const dailyData = ref(null)
const dailyPending = ref(true)

const dailyMax = computed(() => {
  if (!dailyData.value?.days) return 1
  let max = 1
  for (const d of dailyData.value.days) {
    if (d.roms > max) max = d.roms
    if (d.devices > max) max = d.devices
    if (d.branches > max) max = d.branches
  }
  return max
})

const barHeight = (val) => {
  if (!val || dailyMax.value <= 0) return '0%'
  const pct = (val / dailyMax.value) * 100
  return `${Math.max(pct, val > 0 ? 6 : 0)}%`
}

const dailyTotalRoms = computed(() => {
  if (!dailyData.value?.days) return 0
  return dailyData.value.days.reduce((s, d) => s + d.roms, 0)
})

const dailyTotalDevs = computed(() => {
  if (!dailyData.value?.days) return 0
  return dailyData.value.days.reduce((s, d) => s + d.devices, 0)
})

const dailyTotalBr = computed(() => {
  if (!dailyData.value?.days) return 0
  return dailyData.value.days.reduce((s, d) => s + d.branches, 0)
})

const checkResult = ref(null)
const checkTime = ref('')
const checking = ref(false)
const checkError = ref('')

const runCheck = async () => {
  checking.value = true
  checkError.value = ''
  try {
    const res = await getCheckSummary()
    checkResult.value = res
    checkTime.value = new Date(res.executedAt).toLocaleString('zh-CN', { hour12: false })
  } catch (e) {
    checkError.value = errorMessage(e)
  } finally {
    checking.value = false
  }
}

const fmt = (n) => (n === null || n === undefined ? '—' : Number(n).toLocaleString())

const fmtBytes = (b) => {
  if (!b) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  let i = 0
  let v = Number(b)
  while (v >= 1024 && i < units.length - 1) {
    v /= 1024
    i++
  }
  return `${v.toFixed(v >= 10 || i === 0 ? 0 : 1)} ${units[i]}`
}

const fmtTs = (ts) => {
  if (!ts) return '—'
  const d = new Date(Number(ts))
  return isNaN(d.getTime()) ? String(ts) : d.toLocaleString('zh-CN', { hour12: false })
}

const recentBlocks = computed(() => {
  if (!data.value) return []
  return [
    {
      table: 'roms',
      label: 'ROM',
      to: '/roms',
      cols: ['id', 'device', 'version', 'type', 'branch', 'release_date'],
      rows: data.value.recent.roms,
    },
    {
      table: 'devices',
      label: '设备',
      to: '/devices',
      cols: ['id', 'device', 'code', 'tag', 'region', 'launch_date'],
      rows: data.value.recent.devices,
    },
    {
      table: 'branches',
      label: '分支',
      to: '/branches',
      cols: ['id', 'branch', 'name_zh', 'tag', 'region', 'visibility'],
      rows: data.value.recent.branches,
    },
  ]
})

const loadDaily = async () => {
  dailyPending.value = true
  try {
    dailyData.value = await $fetch('/api/db/daily-new')
  } catch { /* 非关键错误，静默处理 */ } finally {
    dailyPending.value = false
  }
}

const load = async () => {
  pending.value = true
  error.value = ''
  try {
    data.value = await getStats()
  } catch (e) {
    error.value = errorMessage(e)
  } finally {
    pending.value = false
  }
}

onMounted(() => {
  load()
  loadDaily()
})
</script>
