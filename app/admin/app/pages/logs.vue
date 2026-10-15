<template>
  <div class="container-admin py-8 sm:py-10">
    <header class="mb-6">
      <h1 class="text-2xl font-semibold tracking-tight">日志录入</h1>
      <p class="mt-1 text-sm text-[var(--color-text-secondary)]">
        一段文本按<span class="font-medium text-[var(--color-text)]">换行符</span>拆成多条日志写入
        <code class="font-mono text-xs">logs</code> 表；同一日志的各语言版本共用一行，
        保存时会自动比对库中已有内容，反推并补齐其他语言的译文。
      </p>
    </header>

    <!-- 录入区 -->
    <section class="card overflow-hidden">
      <div class="flex flex-wrap items-center gap-2 border-b border-[var(--color-border)] px-4 py-3">
        <label class="flex items-center gap-1.5 text-xs text-[var(--color-text-secondary)]">
          类型
          <select v-model="form.type" class="input-base !w-auto !py-1 !text-xs">
            <option value="log">日志条目（log）</option>
            <option value="module">模块名（module）</option>
          </select>
        </label>
        <label class="flex items-center gap-1.5 text-xs text-[var(--color-text-secondary)]">
          文本语言
          <select v-model="form.lang" class="input-base !w-auto !py-1 !text-xs">
            <option v-for="l in languageOptions" :key="l.column" :value="l.column">
              {{ l.label }}（{{ l.column }}）
            </option>
          </select>
        </label>
        <span class="ml-auto text-xs text-[var(--color-text-tertiary)]">
          已输入 {{ lineCount }} 行（去空行 / 去重后）
        </span>
      </div>

      <textarea
        v-model="form.text"
        rows="8"
        class="w-full resize-y border-0 bg-[var(--color-bg-surface)] p-4 font-mono text-xs leading-relaxed text-[var(--color-text)] outline-none placeholder:text-[var(--color-text-tertiary)]"
        placeholder="每行一条日志，例如：&#10;新增 支持下拉展开小窗，多任务场景高效处理&#10;优化 系统的流畅度"
        spellcheck="false"
        @keydown.ctrl.enter.prevent="save(false)"
        @keydown.meta.enter.prevent="save(false)"
      ></textarea>

      <div class="flex flex-wrap items-center justify-between gap-2 border-t border-[var(--color-border)] px-4 py-3">
        <p class="text-xs text-[var(--color-text-tertiary)]">
          Ctrl/⌘ + Enter 直接保存 · 「解析比对」只预览不写库
        </p>
        <div class="flex items-center gap-2">
          <button type="button" class="btn-secondary" :disabled="busy || !lineCount" @click="save(true)">
            {{ busy && previewMode ? '解析中…' : '解析比对' }}
          </button>
          <button type="button" class="btn-primary" :disabled="busy || !lineCount" @click="save(false)">
            {{ busy && !previewMode ? '保存中…' : '保存入库' }}
          </button>
        </div>
      </div>
    </section>

    <!-- 解析结果 -->
    <section v-if="result" class="mt-5">
      <div class="mb-2 flex flex-wrap items-center gap-3 text-xs text-[var(--color-text-secondary)]">
        <span class="badge">{{ result.preview ? '预览（未写库）' : '已写入' }}</span>
        <span>共 {{ result.summary.total }} 行</span>
        <span>新增 {{ result.summary.created }}</span>
        <span>命中已有 {{ result.summary.existing }}</span>
        <span>合并补全 {{ result.summary.merged }}</span>
        <span>补写语言项 {{ result.summary.filledLanguages }}</span>
      </div>

      <div class="card divide-y divide-[var(--color-border)]">
        <div v-for="(item, i) in result.items" :key="i" class="flex flex-wrap items-start gap-3 px-4 py-3">
          <span
            class="mt-0.5 shrink-0 rounded-full px-2 py-0.5 text-[11px] font-medium"
            :class="item.status === 'new'
              ? 'bg-[var(--color-accent-soft)] text-[var(--color-accent)]'
              : 'bg-[var(--color-bg-subtle)] text-[var(--color-text-secondary)]'"
          >
            {{ item.status === 'new' ? '新增' : item.status === 'merged' ? '合并补全' : '已存在' }}
          </span>
          <p class="min-w-0 flex-1 break-words text-sm text-[var(--color-text)]">{{ item.text }}</p>
          <div class="flex flex-wrap items-center gap-1">
            <span class="text-[11px] text-[var(--color-text-tertiary)]">#{{ item.groupId ?? '—' }}</span>
            <span
              v-for="c in item.languages"
              :key="c"
              class="rounded border px-1.5 py-0.5 text-[11px]"
              :class="item.filled.includes(c)
                ? 'border-[var(--color-accent)] text-[var(--color-accent)]'
                : 'border-[var(--color-border)] text-[var(--color-text-tertiary)]'"
              :title="c"
            >
              {{ langLabel(c) }}
            </span>
          </div>
        </div>
      </div>
    </section>

    <!-- 去重体检 -->
    <section class="mt-6">
      <div class="mb-2 flex flex-wrap items-center gap-3">
        <h2 class="text-sm font-semibold text-[var(--color-text)]">去重体检</h2>
        <button type="button" class="btn-secondary !py-1 !text-xs" :disabled="auditing" @click="runAudit">
          {{ auditing ? '检查中…' : '运行检查' }}
        </button>
        <template v-if="audit">
          <span
            class="rounded-full px-2 py-0.5 text-[11px] font-medium"
            :class="audit.duplicateGroupCount
              ? 'bg-[var(--color-danger)]/10 text-[var(--color-danger)]'
              : 'bg-[var(--color-ok)]/10 text-[var(--color-ok)]'"
          >
            {{ audit.duplicateGroupCount ? `重复分组 ${audit.duplicateGroupCount} 组` : '无重复分组 ✓' }}
          </span>
          <span class="text-xs text-[var(--color-text-tertiary)]">
            共 {{ audit.total }} 行 · 重复 {{ audit.duplicateRowCount }} 行 · 未被引用 {{ audit.unreferencedCount }} 行
          </span>
        </template>
      </div>

      <p class="text-xs text-[var(--color-text-secondary)]">
        判定标准与写入侧一致：类型相同、各语言互不冲突、且共享同一门语言文本的两行本应是同一行。
        若出现重复，请在 data 仓库运行
        <code class="font-mono">python3 scripts/dedup_logs.py --apply --prune-unused</code> 合并。
      </p>

      <div v-if="audit?.duplicateGroups?.length" class="card mt-2 divide-y divide-[var(--color-border)]">
        <div v-for="group in audit.duplicateGroups" :key="group.ids.join(',')" class="px-4 py-3">
          <p class="text-xs font-medium text-[var(--color-danger)]">
            重复 {{ group.total }} 行 · ID {{ group.ids.join(' / ') }}
          </p>
          <ul class="mt-1 space-y-0.5">
            <li v-for="sample in group.samples" :key="sample.id" class="text-xs text-[var(--color-text-secondary)]">
              #{{ sample.id }} {{ sample.content }}（{{ sample.languages.join('、') }}）
            </li>
          </ul>
        </div>
      </div>
    </section>

    <div v-if="error" class="alert-danger mt-5">
      <p class="text-sm font-medium text-[var(--color-danger)]">操作失败</p>
      <p class="mt-1 break-all font-mono text-xs text-[var(--color-text-secondary)]">{{ error }}</p>
    </div>

    <!-- 库中日志 -->
    <section class="mt-8">
      <div class="mb-3 flex flex-wrap items-center gap-2">
        <h2 class="text-sm font-semibold text-[var(--color-text)]">库中日志</h2>
        <div class="ml-auto flex flex-wrap items-center gap-2">
          <select v-model="browse.type" class="input-base !w-auto !py-1 !text-xs">
            <option value="">全部类型</option>
            <option value="log">日志条目</option>
            <option value="module">模块名</option>
          </select>
          <select v-model="browse.missing" class="input-base !w-auto !py-1 !text-xs">
            <option value="">不限语言</option>
            <option v-for="l in languageOptions" :key="l.column" :value="l.column">
              缺少 {{ l.label }}
            </option>
          </select>
          <input
            v-model="browse.search"
            class="input-base !w-56 !py-1 !text-xs"
            placeholder="搜索内容…"
            @keydown.enter="loadList(1)"
          />
          <button type="button" class="btn-secondary !py-1 !text-xs" :disabled="loadingList" @click="loadList(1)">
            查询
          </button>
        </div>
      </div>

      <p class="mb-2 text-xs text-[var(--color-text-tertiary)]">
        共 {{ list.total }} 条分组（当前条件下各语言覆盖：<span
          v-for="l in list.languages"
          :key="l.column"
          class="mr-2 inline-block"
        >{{ l.label }} {{ l.count }}</span>）
      </p>

      <div class="card overflow-hidden">
        <div class="max-h-[520px] overflow-auto">
          <table class="table-base">
            <thead class="sticky top-0">
              <tr class="border-b border-[var(--color-border)] bg-[var(--color-bg-subtle)]">
                <th class="table-th w-16">ID</th>
                <th class="table-th w-16">类型</th>
                <th class="table-th">主内容（中文）</th>
                <th class="table-th w-[420px]">语言覆盖</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-[var(--color-border)]">
              <tr v-for="row in list.items" :key="row.id" class="align-top hover:bg-[var(--color-bg-subtle)]">
                <td class="table-td font-mono text-xs text-[var(--color-text-tertiary)]">{{ row.id }}</td>
                <td class="table-td text-xs">{{ row.type === 'module' ? '模块' : '条目' }}</td>
                <td class="table-td text-sm break-words">{{ row.content }}</td>
                <td class="table-td">
                  <div class="flex flex-wrap gap-1">
                    <span
                      v-for="c in languages"
                      :key="c.column"
                      class="rounded border px-1.5 py-0.5 text-[11px]"
                      :class="row.texts[c.column]
                        ? 'border-[var(--color-ok)] text-[var(--color-ok)]'
                        : 'border-[var(--color-border)] text-[var(--color-text-tertiary)]'"
                      :title="row.texts[c.column] || '（缺）'"
                    >
                      {{ c.label }}
                    </span>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-if="!list.items.length" class="px-6 py-12 text-center text-sm text-[var(--color-text-tertiary)]">
          没有匹配的日志
        </div>
      </div>

      <div class="mt-3 flex items-center justify-between text-xs text-[var(--color-text-secondary)]">
        <span>第 {{ list.page }} / {{ totalPages }} 页</span>
        <div class="flex items-center gap-2">
          <button type="button" class="btn-secondary !py-1 !text-xs" :disabled="list.page <= 1" @click="loadList(list.page - 1)">
            上一页
          </button>
          <button
            type="button"
            class="btn-secondary !py-1 !text-xs"
            :disabled="list.page >= totalPages"
            @click="loadList(list.page + 1)"
          >
            下一页
          </button>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
const { push } = useToast()

const languageOptions = [
  { column: 'logs_zh', label: '简体中文' },
  { column: 'logs_zh_tw', label: '繁体中文' },
  { column: 'logs_en', label: '英文' },
  { column: 'logs_ja', label: '日文' },
  { column: 'logs_ko', label: '韩文' },
  { column: 'logs_ru', label: '俄文' },
  { column: 'logs_uk', label: '乌克兰文' },
  { column: 'logs_pl', label: '波兰文' },
  { column: 'logs_de', label: '德文' },
  { column: 'logs_fr', label: '法文' },
  { column: 'logs_it', label: '意大利文' },
  { column: 'logs_es', label: '西班牙文' },
  { column: 'logs_pt', label: '葡萄牙文' },
  { column: 'logs_tr', label: '土耳其文' },
  { column: 'logs_id', label: '印尼文' },
  { column: 'logs_vi', label: '越南文' },
  { column: 'logs_th', label: '泰文' },
  { column: 'logs_ar', label: '阿拉伯文' },
  { column: 'logs_in', label: '印地文' },
  { column: 'logs_ug', label: '维吾尔文' },
  { column: 'logs_bo', label: '藏文' },
]
const languages = languageOptions
const langLabel = (column) => languageOptions.find((l) => l.column === column)?.label || column

const form = ref({ text: '', type: 'log', lang: 'logs_zh' })
const busy = ref(false)
const previewMode = ref(false)
const result = ref(null)
const error = ref('')

const lineCount = computed(() => {
  const seen = new Set()
  for (const line of form.value.text.split(/\r?\n/)) {
    const t = line.trim()
    if (t) seen.add(t)
  }
  return seen.size
})

const save = async (preview) => {
  if (busy.value || !lineCount.value) return
  busy.value = true
  previewMode.value = preview
  error.value = ''
  try {
    const data = await $fetch('/api/logs/manual', {
      method: 'POST',
      body: { text: form.value.text, type: form.value.type, lang: form.value.lang, preview },
    })
    result.value = data
    push('success', preview
      ? `解析完成：新增 ${data.summary.created} 条，补全 ${data.summary.merged} 条`
      : `已保存：新增 ${data.summary.created} 条，补全 ${data.summary.merged} 条`)
    if (!preview) {
      form.value.text = ''
      loadList(1)
    }
  } catch (e) {
    error.value = errorMessage(e)
    push('error', error.value)
  } finally {
    busy.value = false
  }
}

const audit = ref(null)
const auditing = ref(false)

const runAudit = async () => {
  auditing.value = true
  try {
    audit.value = await $fetch('/api/logs/audit')
    if (audit.value.duplicateGroupCount) {
      push('error', `发现 ${audit.value.duplicateGroupCount} 组重复日志，请用 dedup_logs.py 合并`)
    } else {
      push('success', '去重体检通过：没有重复分组')
    }
  } catch (e) {
    push('error', errorMessage(e))
  } finally {
    auditing.value = false
  }
}

const browse = ref({ search: '', type: '', missing: '' })
const loadingList = ref(false)
const list = ref({ page: 1, pageSize: 20, total: 0, items: [], languages: [] })
const totalPages = computed(() => Math.max(Math.ceil(list.value.total / list.value.pageSize), 1))

const loadList = async (page = 1) => {
  loadingList.value = true
  try {
    list.value = await $fetch('/api/logs/list', {
      params: {
        page,
        pageSize: list.value.pageSize,
        search: browse.value.search,
        type: browse.value.type,
        missing: browse.value.missing,
      },
    })
  } catch (e) {
    push('error', errorMessage(e))
  } finally {
    loadingList.value = false
  }
}

onMounted(() => loadList(1))
</script>
