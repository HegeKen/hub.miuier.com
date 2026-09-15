<template>
  <Teleport to="body">
    <Transition
      enter-active-class="transition duration-200 ease-out"
      enter-from-class="opacity-0"
      enter-to-class="opacity-100"
      leave-active-class="transition duration-150 ease-in"
      leave-from-class="opacity-100"
      leave-to-class="opacity-0"
    >
      <div v-if="open" class="fixed inset-0 z-[80] overflow-y-auto" role="dialog" aria-modal="true" aria-label="数据自查">
        <div class="absolute inset-0 bg-black/40 backdrop-blur-sm" @click="$emit('close')"></div>
        <div class="check-panel relative mx-auto my-6 w-full max-w-6xl rounded-xl border border-[var(--color-border)] bg-[var(--color-bg-surface)] p-5 shadow-xl sm:max-w-7xl 2xl:max-w-[95vw]">
          <!-- 标题 -->
          <div class="mb-4 flex flex-wrap items-start justify-between gap-3">
            <div>
              <h3 class="text-base font-semibold text-[var(--color-text)]">
                数据自查：<span class="font-mono">{{ table }}</span>
              </h3>
              <p class="mt-0.5 text-xs text-[var(--color-text-tertiary)]">
                依据 data/db_structure 中字段注释与示例，主动筛选不符合逻辑的记录；特殊情况可「忽略」后不再报错
              </p>
            </div>
            <div class="flex items-center gap-2">
              <button type="button" class="btn-secondary !py-1.5 text-xs" :disabled="loading || busy" @click="load">
                重新检查
              </button>
              <button
                type="button"
                class="flex h-8 w-8 items-center justify-center rounded-md text-[var(--color-text-tertiary)] transition-colors hover:bg-[var(--color-bg-subtle)] hover:text-[var(--color-text)]"
                aria-label="关闭"
                @click="$emit('close')"
              >
                <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" aria-hidden="true">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M6 18 18 6M6 6l12 12" />
                </svg>
              </button>
            </div>
          </div>

          <!-- 汇总（点击筛选规则列表） -->
          <div v-if="report" class="mb-4 flex flex-wrap items-center gap-2 text-xs">
            <button
              type="button"
              class="filter-pill"
              :class="activeFilter === 'all' ? 'filter-pill-active' : ''"
              :title="'显示全部规则'"
              @click="activeFilter = 'all'"
            >
              <span class="font-medium">违规合计</span>
              <span class="tabular-nums font-semibold">{{ report.summary.total.toLocaleString() }}</span>
            </button>
            <button
              type="button"
              class="filter-pill"
              :class="[activeFilter === 'error' ? 'filter-pill-active' : '', report.summary.errors === 0 ? 'opacity-60' : '']"
              :title="'只显示错误类规则'"
              @click="toggleFilter('error')"
            >
              <span class="dot bg-[var(--color-danger)]" aria-hidden="true"></span>
              <span class="font-medium">错误</span>
              <span class="tabular-nums font-semibold">{{ report.summary.errors.toLocaleString() }}</span>
            </button>
            <button
              type="button"
              class="filter-pill"
              :class="[activeFilter === 'warning' ? 'filter-pill-active' : '', report.summary.warnings === 0 ? 'opacity-60' : '']"
              :title="'只显示警告类规则'"
              @click="toggleFilter('warning')"
            >
              <span class="dot bg-[var(--color-warn)]" aria-hidden="true"></span>
              <span class="font-medium">警告</span>
              <span class="tabular-nums font-semibold">{{ report.summary.warnings.toLocaleString() }}</span>
            </button>
            <span v-if="report.summary.dismissed > 0" class="inline-flex items-center gap-1.5 rounded-full border border-[var(--color-border)] bg-[var(--color-bg-surface)] px-3 py-1.5 font-medium text-[var(--color-text-tertiary)]">
              已忽略规则 <span class="tabular-nums text-[var(--color-text)]">{{ report.summary.dismissed }}</span>
            </span>
            <span v-if="activeFilter !== 'all'" class="text-[var(--color-accent)]">
              已筛选：{{ activeFilter === 'error' ? '仅错误' : '仅警告' }}
            </span>
            <span class="text-[var(--color-text-tertiary)]">检查时间 {{ fmtTime(report.executedAt) }}</span>
            <span v-if="refreshing" class="inline-flex items-center gap-1.5 font-medium text-[var(--color-accent)]">
              <svg class="h-3.5 w-3.5 animate-spin" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" aria-hidden="true">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 0 1 8-8v4a4 4 0 0 0-4 4H4z"></path>
              </svg>
              编辑已保存，正在重新检查…
            </span>
          </div>

          <!-- 加载 -->
          <div v-if="loading" class="flex justify-center py-16">
            <span class="spinner" role="status" aria-label="检查中"></span>
            <span class="ml-3 self-center text-sm text-[var(--color-text-secondary)]">正在扫描数据，请稍候…</span>
          </div>

          <!-- 错误 -->
          <div v-else-if="error" class="alert-danger">
            <p class="text-sm font-medium">检查失败</p>
            <p class="mt-1 break-all font-mono text-xs">{{ error }}</p>
          </div>

          <!-- 规则列表 -->
          <div v-else-if="report" class="space-y-3">
            <div
              v-for="rule in visibleRules"
              :key="rule.id"
              class="overflow-hidden rounded-lg border border-[var(--color-border)]"
              :class="rule.dismissed ? 'opacity-70' : ''"
            >
              <!-- 规则头 -->
              <div class="flex items-center gap-2 px-4 py-3 transition-colors hover:bg-[var(--color-bg-subtle)]">
                <button
                  type="button"
                  class="flex min-w-0 flex-1 items-center gap-3 text-left"
                  :disabled="rule.total === 0 && !rule.dismissed"
                  @click="toggleExpand(rule.id)"
                >
                  <span
                    class="dot h-2 w-2 shrink-0"
                    :class="rule.dismissed ? 'bg-[var(--color-text-tertiary)]' : rule.severity === 'error' ? 'bg-[var(--color-danger)]' : 'bg-[var(--color-warn)]'"
                    :title="rule.dismissed ? '已忽略' : rule.severity === 'error' ? '错误' : '警告'"
                    aria-hidden="true"
                  ></span>
                  <span class="min-w-0 flex-1">
                    <span class="flex flex-wrap items-baseline gap-x-2">
                      <span class="text-sm font-medium text-[var(--color-text)]">{{ rule.name }}</span>
                      <span class="font-mono text-xs text-[var(--color-text-tertiary)]">{{ rule.column }}</span>
                      <span
                        class="rounded px-1.5 py-0.5 text-[10px] font-medium"
                        :class="rule.dismissed
                          ? 'bg-[var(--color-bg-subtle)] text-[var(--color-text-tertiary)]'
                          : rule.severity === 'error'
                            ? 'bg-[var(--color-danger)]/10 text-[var(--color-danger)]'
                            : 'bg-[var(--color-warn)]/10 text-[var(--color-warn)]'"
                      >
                        {{ rule.dismissed ? '已忽略' : rule.severity === 'error' ? '错误' : '警告' }}
                      </span>
                      <span
                        v-if="rule.dismissed"
                        class="rounded bg-[var(--color-bg-subtle)] px-1.5 py-0.5 text-[10px] font-medium text-[var(--color-text-secondary)]"
                      >
                        整条规则不再计入违规
                      </span>
                    </span>
                    <span class="mt-0.5 block text-xs text-[var(--color-text-secondary)]">{{ rule.description }}</span>
                  </span>
                  <span
                    class="shrink-0 rounded-full px-2.5 py-1 text-xs font-semibold tabular-nums"
                    :class="rule.total > 0
                      ? 'bg-[var(--color-accent-soft)] text-[var(--color-accent)]'
                      : 'bg-[var(--color-bg-subtle)] text-[var(--color-text-tertiary)]'"
                  >
                    {{ rule.dismissed ? '已忽略' : rule.total > 0 ? rule.total.toLocaleString() : '无违规' }}
                  </span>
                  <svg
                    v-if="rule.total > 0 && !rule.dismissed"
                    xmlns="http://www.w3.org/2000/svg"
                    class="h-4 w-4 shrink-0 text-[var(--color-text-tertiary)] transition-transform"
                    :class="expanded.has(rule.id) ? 'rotate-180' : ''"
                    fill="none"
                    viewBox="0 0 24 24"
                    stroke-width="1.5"
                    stroke="currentColor"
                    aria-hidden="true"
                  >
                    <path stroke-linecap="round" stroke-linejoin="round" d="m19.5 8.25-7.5 7.5-7.5-7.5" />
                  </svg>
                </button>

                <!-- 忽略 / 恢复操作 -->
                <button
                  v-if="rule.dismissed"
                  type="button"
                  class="btn-secondary !px-2.5 !py-1 shrink-0 text-xs"
                  :disabled="busy"
                  @click="restoreRule(rule)"
                >
                  恢复规则
                </button>
                <button
                  v-else-if="rule.total > 0"
                  type="button"
                  class="btn-secondary !px-2.5 !py-1 shrink-0 text-xs"
                  :disabled="busy"
                  @click="startDismissRule(rule)"
                >
                  忽略此规则
                </button>
              </div>

              <!-- 忽略规则（两步确认 + 原因） -->
              <div v-if="dismissingRuleId === rule.id" class="border-t border-[var(--color-border)] bg-[var(--color-bg-subtle)] px-4 py-3">
                <p class="mb-2 text-xs text-[var(--color-text-secondary)]">
                  忽略后该规则下全部 {{ rule.total.toLocaleString() }} 条违规将不再报错，可在下方「已忽略」列表中恢复。
                </p>
                <div class="flex flex-wrap items-center gap-2">
                  <input
                    v-model="dismissReason"
                    type="text"
                    class="input-base min-w-[200px] flex-1 !py-1.5 text-xs"
                    placeholder="填写忽略原因（可选），如：历史遗留格式，无需修复"
                    @keydown.enter.prevent="confirmDismissRule"
                  />
                  <button type="button" class="btn-primary !px-3 !py-1.5 text-xs" :disabled="busy" @click="confirmDismissRule">
                    确认忽略
                  </button>
                  <button type="button" class="btn-secondary !px-3 !py-1.5 text-xs" :disabled="busy" @click="cancelDismissRule">
                    取消
                  </button>
                </div>
              </div>

              <!-- 样本 -->
              <div v-if="expanded.has(rule.id) && !rule.dismissed && rule.samples.length" class="border-t border-[var(--color-border)]">
                <div class="overflow-x-auto">
                  <table class="table-base">
                    <thead>
                      <tr class="bg-[var(--color-bg-subtle)]">
                        <th class="table-th w-14 whitespace-nowrap">id</th>
                        <th v-for="col in sampleColsOf(rule)" :key="col" class="table-th whitespace-nowrap font-mono">{{ col }}</th>
                        <th class="table-th whitespace-nowrap text-right">操作</th>
                      </tr>
                    </thead>
                    <tbody class="divide-y divide-[var(--color-border)]">
                      <tr v-for="row in rule.samples" :key="String(row.id)" class="hover:bg-[var(--color-bg-subtle)]">
                        <td class="table-td whitespace-nowrap font-mono text-xs">{{ String(row.id) }}</td>
                        <td
                          v-for="col in sampleColsOf(rule)"
                          :key="col"
                          class="table-td max-w-64 truncate font-mono text-xs"
                          :title="cellTitle(row[col])"
                        >
                          <span v-if="row[col] === null || row[col] === undefined" class="text-[var(--color-text-tertiary)]">NULL</span>
                          <span v-else>{{ String(row[col]) }}</span>
                        </td>
                        <td class="table-td whitespace-nowrap text-right">
                          <div class="flex justify-end gap-1.5">
                            <button
                              v-if="canAutofix"
                              type="button"
                              class="btn-primary !px-2.5 !py-1 text-xs"
                              :disabled="busy || autofixingId === row.id"
                              @click="autofixRow(row)"
                            >
                              {{ autofixingId === row.id ? '修复中…' : '一键修复' }}
                            </button>
                            <button type="button" class="btn-secondary !px-2.5 !py-1 text-xs" :disabled="busy" @click="dismissRow(rule, row)">
                              忽略
                            </button>
                            <button type="button" class="btn-secondary !px-2.5 !py-1 text-xs" @click="$emit('edit', row.id)">
                              编辑
                            </button>
                          </div>
                        </td>
                      </tr>
                    </tbody>
                  </table>
                </div>
                <p class="border-t border-[var(--color-border)] px-4 py-2 text-xs text-[var(--color-text-tertiary)]">
                  仅显示最近 {{ rule.samples.length }} 条样本，共 {{ rule.total.toLocaleString() }} 条；「忽略」可跳过单条记录
                </p>
              </div>
            </div>

            <!-- 筛选后为空 -->
            <div v-if="visibleRules.length === 0" class="rounded-lg border border-dashed border-[var(--color-border)] px-6 py-10 text-center">
              <p class="text-sm text-[var(--color-text-tertiary)]">
                当前筛选（{{ activeFilter === 'error' ? '仅错误' : '仅警告' }}）下没有违规规则
              </p>
            </div>

            <!-- 已忽略列表 -->
            <div v-if="report.dismissals.length" class="mt-2 rounded-lg border border-[var(--color-border)]">
              <div class="border-b border-[var(--color-border)] px-4 py-2.5">
                <h4 class="text-sm font-semibold text-[var(--color-text)]">
                  已忽略 <span class="tabular-nums text-[var(--color-text-tertiary)]">{{ report.dismissals.length }}</span>
                </h4>
                <p class="mt-0.5 text-xs text-[var(--color-text-tertiary)]">以下特殊情况已豁免，点击「取消忽略」可恢复检查</p>
              </div>
              <ul class="divide-y divide-[var(--color-border)]">
                <li v-for="g in dismissalGroups" :key="g.ruleId" class="px-4 py-2">
                  <button
                    type="button"
                    class="flex w-full items-center gap-2 rounded px-1 py-1 text-left hover:bg-[var(--color-bg-subtle)]"
                    @click="toggleDismissalGroup(g.ruleId)"
                  >
                    <svg
                      class="h-4 w-4 shrink-0 text-[var(--color-text-tertiary)] transition-transform"
                      :class="dismissalExpanded.has(g.ruleId) ? 'rotate-90' : ''"
                      viewBox="0 0 20 20" fill="currentColor" aria-hidden="true"
                    >
                      <path fill-rule="evenodd" d="M7.21 5.23a.75.75 0 011.06.02L12.5 9.44V5.75a.75.75 0 011.5 0v6.5a.75.75 0 01-.75.75h-6.5a.75.75 0 010-1.5h3.69L6.23 8.29a.75.75 0 01.02-1.06z" clip-rule="evenodd" />
                    </svg>
                    <span class="dot" :class="g.items.some((d) => d.row_id === 0) ? 'bg-[var(--color-warn)]' : 'bg-[var(--color-info)]'" aria-hidden="true"></span>
                    <span class="text-sm font-medium text-[var(--color-text)]">
                      {{ ruleName(g.ruleId) }}
                    </span>
                    <span class="rounded-full bg-[var(--color-bg-subtle)] px-2 py-0.5 text-xs tabular-nums text-[var(--color-text-tertiary)]">
                      {{ g.items.length }} 条
                    </span>
                    <span class="ml-auto text-xs text-[var(--color-text-tertiary)]">{{ fmtTime(g.items[0].created_at) }}</span>
                  </button>
                  <div v-if="dismissalExpanded.has(g.ruleId)" class="mt-1 divide-y divide-[var(--color-border)] rounded border border-[var(--color-border)]">
                    <div
                      v-for="d in g.items"
                      :key="d.id"
                      class="flex flex-wrap items-center gap-2 px-3 py-2"
                    >
                      <span
                        class="rounded bg-[var(--color-bg-subtle)] px-1.5 py-0.5 font-mono text-xs text-[var(--color-text-secondary)]"
                      >
                        {{ d.row_id === 0 ? '整条规则' : '记录 #' + d.row_id }}
                      </span>
                      <span v-if="d.reason" class="min-w-0 flex-1 truncate text-xs text-[var(--color-text-tertiary)]" :title="d.reason">
                        {{ d.reason }}
                      </span>
                      <span v-else class="min-w-0 flex-1"></span>
                      <span class="shrink-0 text-xs text-[var(--color-text-tertiary)]">{{ fmtTime(d.created_at) }}</span>
                      <button
                        type="button"
                        class="btn-secondary !px-2.5 !py-1 shrink-0 text-xs"
                        :disabled="busy"
                        @click="restore(d)"
                      >
                        取消忽略
                      </button>
                    </div>
                  </div>
                </li>
              </ul>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
const props = defineProps({
  open: { type: Boolean, default: false },
  table: { type: String, required: true },
  /** 编辑保存等操作后由父组件递增，触发自动重新检查 */
  refreshSignal: { type: Number, default: 0 },
})
const emit = defineEmits(['close', 'edit'])

const { runCheck, dismissViolation, restoreViolation, autofixRecord } = useAdminApi()
const { push } = useToast()

const loading = ref(false)
const refreshing = ref(false)
const error = ref('')
const report = ref(null)
const expanded = ref(new Set())

/** 一键修复：目前仅 roms 表支持（以 recovery/fastboot 文件名为准回填字段） */
const canAutofix = computed(() => props.table === 'roms')
const autofixingId = ref(null)

/** 汇总筛选：all / error / warning */
const activeFilter = ref('all')

const toggleFilter = (sev) => {
  activeFilter.value = activeFilter.value === sev ? 'all' : sev
}

/** 按严重级别筛选后的规则列表（选中筛选时仅显示该级别且有违规的规则） */
const visibleRules = computed(() => {
  if (!report.value) return []
  if (activeFilter.value === 'all') return report.value.rules
  return report.value.rules.filter((r) => r.severity === activeFilter.value && r.total > 0)
})

const busy = ref(false)
const dismissingRuleId = ref('')
const dismissReason = ref('')

const CONTEXT = {
  devices: ['device', 'code'],
  branches: ['branch', 'name_zh'],
  roms: ['device', 'version'],
}

const rulesMap = computed(() => Object.fromEntries((report.value?.rules || []).map((r) => [r.id, r])))

const ruleName = (ruleId) => rulesMap.value[ruleId]?.name || ruleId

/** 已忽略记录按规则分组（同一类别合并显示） */
const dismissalGroups = computed(() => {
  const list = report.value?.dismissals || []
  const map = new Map()
  for (const d of list) {
    if (!map.has(d.rule_id)) map.set(d.rule_id, [])
    map.get(d.rule_id).push(d)
  }
  // 每组内按创建时间倒序
  for (const arr of map.values()) arr.sort((a, b) => b.created_at - a.created_at)
  // 分组按组内最新时间倒序
  return [...map.entries()]
    .map(([ruleId, items]) => ({ ruleId, items }))
    .sort((a, b) => b.items[0].created_at - a.items[0].created_at)
})

const dismissalExpanded = ref(new Set())

const toggleDismissalGroup = (ruleId) => {
  const next = new Set(dismissalExpanded.value)
  if (next.has(ruleId)) next.delete(ruleId)
  else next.add(ruleId)
  dismissalExpanded.value = next
}

const sampleColsOf = (rule) => ['id', ...(CONTEXT[props.table] || []), ...(rule.sampleCols || [rule.column])]

const toggleExpand = (id) => {
  const next = new Set(expanded.value)
  if (next.has(id)) next.delete(id)
  else next.add(id)
  expanded.value = next
}

/** 加载自查报告；quiet=true 时保留当前内容静默刷新（用于编辑保存后自动重查） */
const load = async (quiet = false) => {
  if (quiet) {
    refreshing.value = true
  } else {
    loading.value = true
    error.value = ''
    report.value = null
  }
  try {
    report.value = await runCheck(props.table)
    if (!quiet) error.value = ''
  } catch (e) {
    if (quiet) {
      // 静默刷新失败：保留旧数据，仅提示
      push('error', `自动重新检查失败：${errorMessage(e)}`)
    } else {
      error.value = errorMessage(e)
    }
  } finally {
    loading.value = false
    refreshing.value = false
  }
}

watch(
  () => props.open,
  (open) => {
    if (open) {
      expanded.value = new Set()
      dismissingRuleId.value = ''
      dismissReason.value = ''
      activeFilter.value = 'all'
      load()
    }
  },
)

// 编辑保存后自动重新检查
watch(
  () => props.refreshSignal,
  () => {
    if (props.open && props.refreshSignal > 0) {
      load(true)
    }
  },
)

/* ---------- 忽略 / 恢复 ---------- */

const startDismissRule = (rule) => {
  dismissingRuleId.value = rule.id
  dismissReason.value = ''
}

const cancelDismissRule = () => {
  dismissingRuleId.value = ''
  dismissReason.value = ''
}

const confirmDismissRule = async () => {
  if (!dismissingRuleId.value || busy.value) return
  const ruleId = dismissingRuleId.value
  const reason = dismissReason.value.trim()
  busy.value = true
  try {
    await dismissViolation(props.table, ruleId, { reason })
    push('success', `规则「${ruleName(ruleId)}」已忽略，不再计入违规`)
    cancelDismissRule()
    await load()
  } catch (e) {
    push('error', errorMessage(e))
  } finally {
    busy.value = false
  }
}

const dismissRow = async (rule, row) => {
  if (busy.value) return
  busy.value = true
  try {
    await dismissViolation(props.table, rule.id, { rowId: row.id })
    push('success', `已忽略记录 #${String(row.id)}（${rule.name}）`)
    await load()
  } catch (e) {
    push('error', errorMessage(e))
  } finally {
    busy.value = false
  }
}

/** 一键修复：以 recovery/fastboot 文件名为准回填字段 */
const autofixRow = async (row) => {
  if (busy.value || autofixingId.value !== null) return
  autofixingId.value = row.id
  try {
    const res = await autofixRecord(row.id)
    const changed = res.changed || {}
    const keys = Object.keys(changed)
    if (keys.length === 0) {
      push('info', res.message || '无需修复，字段已与文件名一致')
    } else {
      const detail = keys.map((k) => `${k}: ${String(changed[k].from)} → ${String(changed[k].to)}`).join('；')
      push('success', `记录 #${String(row.id)} 已修复（${keys.length} 项）：${detail}`)
    }
    await load()
  } catch (e) {
    push('error', errorMessage(e))
  } finally {
    autofixingId.value = null
  }
}

const restore = async (d) => {
  if (busy.value) return
  busy.value = true
  try {
    await restoreViolation(props.table, d.rule_id, d.row_id === 0 ? undefined : d.row_id)
    push('success', `已恢复「${ruleName(d.rule_id)}」的检查`)
    await load()
  } catch (e) {
    push('error', errorMessage(e))
  } finally {
    busy.value = false
  }
}

const restoreRule = (rule) => restore({ rule_id: rule.id, row_id: 0 })

const fmtTime = (ts) => new Date(ts).toLocaleString('zh-CN', { hour12: false })

const cellTitle = (v) => {
  if (v === null || v === undefined) return ''
  const s = String(v)
  return s.length > 80 ? s : ''
}
</script>

<style scoped>
/* 自查弹窗密度优化：4K / 大屏下更紧凑 */
.check-panel {
  padding: 1.25rem;
}

.check-panel :deep(.table-th),
.check-panel :deep(.table-td) {
  padding: 0.4rem 0.75rem;
  line-height: 1.35;
}

/* 4K 及以上：进一步压缩，提升信息密度 */
@media (min-width: 1920px) {
  .check-panel {
    padding: 1rem 1.5rem;
  }
  .check-panel :deep(.table-th),
  .check-panel :deep(.table-td) {
    padding: 0.3rem 0.6rem;
    font-size: 0.8125rem;
  }
}
</style>
