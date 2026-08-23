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

onMounted(load)
</script>
