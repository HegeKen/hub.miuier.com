<template>
  <div class="container-admin py-8 sm:py-10">
    <!-- 标题 -->
    <header class="mb-6 flex flex-wrap items-end justify-between gap-3">
      <div>
        <h1 class="text-2xl font-semibold tracking-tight">{{ title }}</h1>
        <p v-if="description" class="mt-1 text-sm text-[var(--color-text-secondary)]">{{ description }}</p>
      </div>
      <NuxtLink to="/" class="text-xs font-medium text-[var(--color-text-tertiary)] transition-colors hover:text-[var(--color-text)]">
        ← 返回仪表盘
      </NuxtLink>
    </header>

    <!-- 工具栏 -->
    <div class="mb-4 flex flex-wrap items-center gap-2">
      <div class="relative min-w-[220px] flex-1 sm:max-w-sm">
        <svg
          class="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-[var(--color-text-tertiary)]"
          xmlns="http://www.w3.org/2000/svg"
          fill="none"
          viewBox="0 0 24 24"
          stroke-width="1.5"
          stroke="currentColor"
          aria-hidden="true"
        >
          <path stroke-linecap="round" stroke-linejoin="round" d="m21 21-5.197-5.197m0 0A7.5 7.5 0 1 0 5.196 5.196a7.5 7.5 0 0 0 10.607 10.607Z" />
        </svg>
        <input
          v-model="search"
          type="search"
          :placeholder="'搜索 ' + table + '（匹配文本字段）'"
          class="input-base py-2 pl-10 text-sm"
          @input="onSearchInput"
        />
      </div>

      <select v-model="pageSize" class="input-base w-auto py-2 text-sm" title="每页条数" @change="onPageSizeChange">
        <option :value="20">20 条/页</option>
        <option :value="50">50 条/页</option>
        <option :value="100">100 条/页</option>
      </select>

      <button type="button" class="btn-secondary" @click="checkOpen = true" title="依据 data/db_structure 字段逻辑筛选异常数据">
        <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" aria-hidden="true">
          <path stroke-linecap="round" stroke-linejoin="round" d="M9 12.75 11.25 15 15 9.75m-3-7.036A11.959 11.959 0 0 1 3.598 6 11.99 11.99 0 0 0 3 9.749c0 5.592 3.824 10.29 9 11.623 5.176-1.332 9-6.03 9-11.622 0-1.31-.21-2.571-.598-3.751h-.152c-3.196 0-6.1-1.248-8.25-3.285Z" />
        </svg>
        自查
      </button>

      <button type="button" class="btn-primary ml-auto" @click="openCreate">
        <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" aria-hidden="true">
          <path stroke-linecap="round" stroke-linejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
        </svg>
        新增记录
      </button>
    </div>

    <!-- 加载中 -->
    <div v-if="loading && rows.length === 0" class="flex justify-center py-24">
      <span class="spinner" role="status" aria-label="加载中"></span>
    </div>

    <!-- 错误 -->
    <div v-else-if="error" class="card px-6 py-10 text-center">
      <p class="text-sm text-[var(--color-text-secondary)]">数据加载失败</p>
      <p class="mt-1 break-all font-mono text-xs text-[var(--color-danger)]">{{ error }}</p>
      <button type="button" class="btn-secondary mt-4" @click="refresh">重试</button>
    </div>

    <!-- 表格 -->
    <div v-else class="card overflow-hidden">
      <div class="overflow-x-auto">
        <table class="table-base">
          <thead>
            <tr class="border-b border-[var(--color-border)] bg-[var(--color-bg-subtle)]">
              <th
                v-for="col in columns"
                :key="col.name"
                class="table-th whitespace-nowrap"
                :class="col.isPrimary ? 'w-20' : ''"
              >
                <button
                  v-if="!col.isLong && !col.isJson"
                  type="button"
                  class="group inline-flex items-center gap-1 uppercase transition-colors hover:text-[var(--color-text)]"
                  :class="sort === col.name ? 'text-[var(--color-text)]' : ''"
                  :title="col.comment || ''"
                  @click="toggleSort(col)"
                >
                  {{ col.name }}
                  <svg
                    v-if="sort === col.name"
                    xmlns="http://www.w3.org/2000/svg"
                    class="h-3 w-3"
                    fill="none"
                    viewBox="0 0 24 24"
                    stroke-width="2"
                    stroke="currentColor"
                    aria-hidden="true"
                  >
                    <path v-if="order === 'asc'" stroke-linecap="round" stroke-linejoin="round" d="m4.5 15.75 7.5-7.5 7.5 7.5" />
                    <path v-else stroke-linecap="round" stroke-linejoin="round" d="m19.5 8.25-7.5 7.5-7.5-7.5" />
                  </svg>
                  <svg v-else xmlns="http://www.w3.org/2000/svg" class="h-3 w-3 opacity-0 transition-opacity group-hover:opacity-60" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" aria-hidden="true">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M8.25 15 12 18.75 15.75 15m-7.5-6L12 5.25 15.75 9" />
                  </svg>
                </button>
                <span v-else class="inline-flex items-center gap-1" :title="col.comment || ''">
                  {{ col.name }}
                  <span
                    v-if="col.isJson"
                    class="rounded bg-[var(--color-accent-soft)] px-1 py-0.5 text-[10px] font-normal normal-case text-[var(--color-accent)]"
                  >
                    JSON
                  </span>
                </span>
              </th>
              <th class="table-th sticky right-0 bg-[var(--color-bg-subtle)] text-right">操作</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-[var(--color-border)]">
            <tr v-for="row in rows" :key="String(row.id)" class="transition-colors hover:bg-[var(--color-bg-subtle)]">
              <td
                v-for="col in columns"
                :key="col.name"
                class="table-td"
                :class="[cellClass(col), col.isLong || col.isJson ? 'max-w-72' : 'max-w-56']"
                :title="cellTitle(col, row[col.name])"
              >
                <template v-if="row[col.name] === null || row[col.name] === undefined">
                  <span class="font-mono text-xs text-[var(--color-text-tertiary)]">NULL</span>
                </template>
                <template v-else>
                  <span v-if="col.isJson" class="mr-1.5 inline-block rounded bg-[var(--color-accent-soft)] px-1 py-0.5 font-mono text-[10px] text-[var(--color-accent)]">
                    JSON
                  </span>
                  <span class="block truncate">{{ cellText(row[col.name]) }}</span>
                </template>
              </td>
              <td class="table-td sticky right-0 bg-[var(--color-bg-surface)] text-right">
                <div class="flex justify-end gap-1.5">
                  <button
                    type="button"
                    class="btn-secondary !px-2.5 !py-1 text-xs"
                    @click="openEdit(row)"
                  >
                    编辑
                  </button>
                  <button
                    type="button"
                    class="btn-danger !px-2.5 !py-1 text-xs"
                    @click="askDelete(row)"
                  >
                    删除
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 空状态 -->
      <div v-if="rows.length === 0" class="px-6 py-16 text-center">
        <p class="text-sm text-[var(--color-text-tertiary)]">没有匹配的记录</p>
      </div>

      <Pagination
        v-else
        :page="page"
        :page-size="pageSize"
        :total="total"
        :loading="loading"
        @change="onPageChange"
      />
    </div>

    <!-- 新增 / 编辑 -->
    <RecordModal
      :open="modalOpen"
      :table="table"
      :columns="columns"
      :record="editingRecord"
      :title="editingRecord ? `编辑记录 #${String(editingRecord.id)}` : `新增 ${table} 记录`"
      @close="modalOpen = false"
      @saved="onRecordSaved"
    />

    <!-- 删除确认 -->
    <ConfirmDialog
      :open="deleting !== null"
      title="确认删除"
      :message="deleting ? `确定要删除 ${table} 表中的记录 #${String(deleting.id)} 吗？此操作不可恢复。` : ''"
      :loading="deleteLoading"
      @confirm="doDelete"
      @cancel="deleting = null"
    />

    <!-- 数据自查 -->
    <CheckPanel
      :open="checkOpen"
      :table="table"
      :refresh-signal="checkRefreshTick"
      @close="checkOpen = false"
      @edit="onCheckEdit"
    />
  </div>
</template>

<script setup>
const props = defineProps({
  table: { type: String, required: true },
  title: { type: String, required: true },
  description: { type: String, default: '' },
})

const { listTable, getTableMeta, getRecord, deleteRecord } = useAdminApi()
const { push } = useToast()

const columns = ref([])
const rows = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const search = ref('')
const sort = ref('id')
const order = ref('desc')
const loading = ref(false)
const error = ref('')

const modalOpen = ref(false)
const editingRecord = ref(null)
const deleting = ref(null)
const deleteLoading = ref(false)
const checkOpen = ref(false)
/** 递增后通知自查窗口自动重新检查（编辑保存成功后） */
const checkRefreshTick = ref(0)

let searchTimer = null
const onSearchInput = () => {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    page.value = 1
    refresh()
  }, 350)
}

const onPageChange = (p) => {
  page.value = p
  refresh()
}

const onPageSizeChange = () => {
  page.value = 1
  refresh()
}

const toggleSort = (col) => {
  if (sort.value === col.name) {
    order.value = order.value === 'asc' ? 'desc' : 'asc'
  } else {
    sort.value = col.name
    order.value = 'desc'
  }
  page.value = 1
  refresh()
}

const refresh = async () => {
  loading.value = true
  error.value = ''
  try {
    const data = await listTable(props.table, {
      page: page.value,
      pageSize: pageSize.value,
      search: search.value,
      sort: sort.value,
      order: order.value,
    })
    rows.value = data.rows
    total.value = data.total
  } catch (e) {
    error.value = errorMessage(e)
  } finally {
    loading.value = false
  }
}

const openCreate = () => {
  editingRecord.value = null
  modalOpen.value = true
}

const openEdit = (row) => {
  editingRecord.value = row
  modalOpen.value = true
}

/** 自查样本 → 拉取完整记录后打开编辑 */
const onCheckEdit = async (id) => {
  try {
    const rec = await getRecord(props.table, id)
    editingRecord.value = rec
    modalOpen.value = true
  } catch (e) {
    push('error', errorMessage(e))
  }
}

/** 保存成功：刷新列表，并通知自查窗口自动重新检查 */
const onRecordSaved = () => {
  refresh()
  checkRefreshTick.value += 1
}

const askDelete = (row) => {
  deleting.value = row
}

const doDelete = async () => {
  if (!deleting.value) return
  deleteLoading.value = true
  try {
    await deleteRecord(props.table, deleting.value.id)
    push('success', `记录 #${String(deleting.value.id)} 已删除`)
    deleting.value = null
    // 若当前页删空则回退一页
    if (rows.value.length === 1 && page.value > 1) page.value -= 1
    refresh()
  } catch (e) {
    push('error', errorMessage(e))
    deleting.value = null
  } finally {
    deleteLoading.value = false
  }
}

const cellClass = (col) => {
  if (col.isNumber || col.isDate) return 'font-mono text-xs whitespace-nowrap tabular-nums'
  return 'text-xs'
}

const cellTitle = (col, v) => {
  if (v === null || v === undefined) return ''
  const s = String(v)
  return s.length > 80 ? s : (col.comment ? `${col.comment}：${s}` : s)
}

const cellText = (v) => String(v)

// 初始化
onMounted(async () => {
  try {
    const meta = await getTableMeta(props.table)
    columns.value = meta.columns
    // 默认按主键倒序
    const pk = meta.columns.find((c) => c.isPrimary)
    if (pk) sort.value = pk.name
  } catch (e) {
    error.value = errorMessage(e)
    return
  }
  refresh()
})
</script>
