<template>
  <div class="container-admin py-8 sm:py-10">
    <header class="mb-6">
      <h1 class="text-2xl font-semibold tracking-tight">SQL 控制台</h1>
      <p class="mt-1 text-sm text-[var(--color-text-secondary)]">
        只读模式：仅允许 SELECT / SHOW / DESCRIBE / EXPLAIN / WITH 语句，未指定 LIMIT 时自动限制
        <span class="font-medium text-[var(--color-warn)]">1000</span> 行
      </p>
    </header>

    <!-- 常用示例 -->
    <div class="mb-3 flex flex-wrap items-center gap-2">
      <span class="text-xs text-[var(--color-text-tertiary)]">示例：</span>
      <button
        v-for="q in samples"
        :key="q"
        type="button"
        class="filter-pill !px-2.5 !py-1 font-mono text-xs"
        @click="setQuery(q)"
      >
        {{ q }}
      </button>
    </div>

    <!-- 输入区 -->
    <div class="card overflow-hidden">
      <textarea
        v-model="sql"
        rows="6"
        class="w-full resize-y border-0 bg-[var(--color-bg-surface)] p-4 font-mono text-xs leading-relaxed text-[var(--color-text)] outline-none placeholder:text-[var(--color-text-tertiary)]"
        placeholder="输入 SQL 查询，Ctrl/⌘ + Enter 执行"
        spellcheck="false"
        @keydown.ctrl.enter.prevent="run"
        @keydown.meta.enter.prevent="run"
      ></textarea>
      <div class="flex flex-wrap items-center justify-between gap-2 border-t border-[var(--color-border)] px-4 py-3">
        <p class="text-xs text-[var(--color-text-tertiary)]">
          Ctrl/⌘ + Enter 执行 · 修改数据请使用左侧表格管理
        </p>
        <button type="button" class="btn-primary" :disabled="running || !sql.trim()" @click="run">
          <svg v-if="running" class="h-4 w-4 animate-spin" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" aria-hidden="true">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 0 1 8-8v4a4 4 0 0 0-4 4H4z"></path>
          </svg>
          {{ running ? '执行中…' : '执行查询' }}
        </button>
      </div>
    </div>

    <!-- 结果 -->
    <div v-if="result" class="mt-5">
      <div class="mb-2 flex flex-wrap items-center gap-3 text-xs text-[var(--color-text-secondary)]">
        <span class="inline-flex items-center gap-1.5">
          <span class="dot bg-[var(--color-ok)]" aria-hidden="true"></span>
          成功
        </span>
        <span>{{ result.rowCount }} 行</span>
        <span v-if="result.affectedRows !== 0">影响 {{ result.affectedRows }} 行</span>
        <span>耗时 {{ result.duration }} ms</span>
      </div>

      <div class="card overflow-hidden">
        <div class="max-h-[480px] overflow-auto">
          <table class="table-base">
            <thead class="sticky top-0">
              <tr class="border-b border-[var(--color-border)] bg-[var(--color-bg-subtle)]">
                <th class="table-th w-14 whitespace-nowrap">#</th>
                <th v-for="f in result.fields" :key="f" class="table-th whitespace-nowrap font-mono">{{ f }}</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-[var(--color-border)]">
              <tr v-for="(row, i) in result.rows" :key="i" class="hover:bg-[var(--color-bg-subtle)]">
                <td class="table-td whitespace-nowrap font-mono text-xs text-[var(--color-text-tertiary)]">{{ i + 1 }}</td>
                <td
                  v-for="f in result.fields"
                  :key="f"
                  class="table-td max-w-72 truncate font-mono text-xs"
                  :title="cellTitle(row[f])"
                >
                  <span v-if="row[f] === null || row[f] === undefined" class="text-[var(--color-text-tertiary)]">NULL</span>
                  <span v-else>{{ cellText(row[f]) }}</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-if="result.rows.length === 0" class="px-6 py-12 text-center text-sm text-[var(--color-text-tertiary)]">
          查询成功，无数据返回
        </div>
      </div>
    </div>

    <!-- 错误 -->
    <div v-if="error" class="alert-danger mt-5">
      <p class="text-sm font-medium text-[var(--color-danger)]">查询失败</p>
      <p class="mt-1 break-all font-mono text-xs text-[var(--color-text-secondary)]">{{ error }}</p>
    </div>

    <!-- 历史 -->
    <section v-if="history.length" class="mt-8">
      <h2 class="mb-2 text-sm font-semibold text-[var(--color-text)]">本次会话历史</h2>
      <ul class="space-y-1.5">
        <li v-for="(item, i) in history" :key="i">
          <button
            type="button"
            class="w-full rounded-lg border border-[var(--color-border)] bg-[var(--color-bg-surface)] px-3 py-2 text-left font-mono text-xs text-[var(--color-text-secondary)] transition-colors hover:border-[var(--color-border-strong)] hover:text-[var(--color-text)]"
            :title="'点击重新执行'"
            @click="rerun(item.sql)"
          >
            <span class="mr-2 text-[var(--color-text-tertiary)]">{{ item.duration }}ms · {{ item.rows }} 行</span>
            <span class="break-all">{{ item.sql }}</span>
          </button>
        </li>
      </ul>
    </section>
  </div>
</template>

<script setup>
const { runSql } = useAdminApi()

const sql = ref('SELECT id, device, version, type, branch, release_date\nFROM roms\nORDER BY id DESC\nLIMIT 50;')
const running = ref(false)
const result = ref(null)
const error = ref('')
const history = ref([])

const samples = [
  'SELECT COUNT(*) AS total FROM roms',
  'SELECT device, COUNT(*) AS cnt FROM roms GROUP BY device ORDER BY cnt DESC LIMIT 10',
  'SHOW TABLES',
  'DESCRIBE branches',
  'SELECT * FROM devices WHERE device LIKE "%marble%" LIMIT 20',
]

const setQuery = (q) => {
  sql.value = q
}

const run = async () => {
  const trimmed = sql.value.trim()
  if (!trimmed || running.value) return

  running.value = true
  error.value = ''
  try {
    const data = await runSql(trimmed)
    result.value = data
    history.value.unshift({ sql: trimmed, duration: data.duration, rows: data.rowCount })
    if (history.value.length > 20) history.value.pop()
  } catch (e) {
    result.value = null
    error.value = errorMessage(e)
  } finally {
    running.value = false
  }
}

const rerun = (q) => {
  sql.value = q
  run()
}

const cellText = (v) => String(v)
const cellTitle = (v) => {
  if (v === null || v === undefined) return ''
  const s = String(v)
  return s.length > 80 ? s : ''
}
</script>
