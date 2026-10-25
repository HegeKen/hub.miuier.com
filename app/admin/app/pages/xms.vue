<template>
  <div class="container-admin py-8 sm:py-10">
    <!-- 标题 -->
    <header class="mb-6 flex flex-wrap items-end justify-between gap-3">
      <div>
        <h1 class="text-2xl font-semibold tracking-tight">XMS 应用更新</h1>
        <p class="mt-1 text-sm text-[var(--color-text-secondary)]">
          XMS（SOTA 系统应用更新）推送与应用明细。推送由抓取脚本写入，这里可修正应用名（多语言）、更新日志、安装包信息与推送备注。
        </p>
        <p class="mt-1 text-xs text-[var(--color-text-tertiary)]">
          XMS 版本号首字母对应系统大版本：C → HyperOS 3、D → HyperOS 4；没有包名的日志模块存在推送的 changelog 里。
        </p>
      </div>
      <div class="flex items-center gap-3">
        <NuxtLink
          to="/xms_app"
          class="text-xs font-medium text-[var(--color-text-tertiary)] transition-colors hover:text-[var(--color-text)]"
        >
          原始应用表 →
        </NuxtLink>
        <NuxtLink to="/" class="text-xs font-medium text-[var(--color-text-tertiary)] transition-colors hover:text-[var(--color-text)]">
          ← 返回仪表盘
        </NuxtLink>
      </div>
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
          placeholder="搜索机型 / 分支 / 版本 / 备注"
          class="input-base py-2 pl-10 text-sm"
          @input="onSearchInput"
        />
      </div>

      <select v-model="version" class="input-base w-auto py-2 text-sm" @change="reload">
        <option value="">全部 XMS 版本</option>
        <option v-for="v in versions" :key="v" :value="v">{{ v }}（{{ osLabel(v) }}）</option>
      </select>

      <button
        type="button"
        class="filter-pill"
        :class="latestOnly ? 'filter-pill-active' : ''"
        @click="toggleLatest"
      >
        仅最新
      </button>

      <select v-model="pageSize" class="input-base w-auto py-2 text-sm" title="每页条数" @change="onPageSizeChange">
        <option :value="20">20 条/页</option>
        <option :value="50">50 条/页</option>
        <option :value="100">100 条/页</option>
      </select>

      <button type="button" class="btn-secondary ml-auto" @click="reload">刷新</button>
    </div>

    <!-- 加载中 -->
    <div v-if="loading && !items.length" class="flex justify-center py-24">
      <span class="spinner" role="status" aria-label="加载中"></span>
    </div>

    <!-- 错误 -->
    <div v-else-if="error" class="card px-6 py-10 text-center">
      <p class="text-sm text-[var(--color-text-secondary)]">数据加载失败</p>
      <p class="mt-1 break-all font-mono text-xs text-[var(--color-danger)]">{{ error }}</p>
      <button type="button" class="btn-secondary mt-4" @click="reload">重试</button>
    </div>

    <!-- 空 -->
    <div v-else-if="!items.length" class="card px-6 py-16 text-center text-sm text-[var(--color-text-tertiary)]">
      暂无 XMS 数据。抓取脚本（ota_former / ota_full）检测到系统应用更新后会自动写入。
    </div>

    <!-- 列表 -->
    <div v-else class="card overflow-hidden">
      <div class="overflow-x-auto">
        <table class="table-base">
          <thead>
            <tr class="border-b border-[var(--color-border)] bg-[var(--color-bg-subtle)]">
              <th class="table-th w-16">ID</th>
              <th class="table-th">机型 / 分支</th>
              <th class="table-th">XMS</th>
              <th class="table-th">系统版本</th>
              <th class="table-th">Android</th>
              <th class="table-th">应用</th>
              <th class="table-th">最新</th>
              <th class="table-th">推送日期</th>
              <th class="table-th text-right">操作</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-[var(--color-border)]">
            <template v-for="item in items" :key="item.id">
              <tr class="align-top transition-colors hover:bg-[var(--color-bg-subtle)]">
                <td class="table-td font-mono text-xs text-[var(--color-text-tertiary)]">{{ item.id }}</td>
                <td class="table-td">
                  <div class="font-medium text-[var(--color-text)]">{{ deviceName(item) }}</div>
                  <div class="mt-0.5 font-mono text-[11px] text-[var(--color-text-tertiary)]">{{ item.code }}</div>
                </td>
                <td class="table-td">
                  <div class="flex items-center gap-1.5">
                    <span class="font-mono text-sm font-semibold text-[var(--color-text)]">{{ item.version }}</span>
                    <span v-if="item.os" class="badge border border-[var(--color-border)] text-[var(--color-text-secondary)]">{{ item.os }}</span>
                  </div>
                </td>
                <td class="table-td break-all font-mono text-xs">{{ item.romVersion }}</td>
                <td class="table-td font-mono text-xs">{{ item.android || '—' }}</td>
                <td class="table-td tabular-nums">{{ item.appCount }}</td>
                <td class="table-td">
                  <span
                    class="badge"
                    :class="item.isLatest
                      ? 'bg-[var(--color-accent-soft)] text-[var(--color-accent)]'
                      : 'border border-[var(--color-border)] text-[var(--color-text-tertiary)]'"
                  >
                    {{ item.isLatest ? '最新' : '历史' }}
                  </span>
                </td>
                <td class="table-td whitespace-nowrap tabular-nums text-xs">{{ item.pushDate || '—' }}</td>
                <td class="table-td text-right">
                  <div class="flex items-center justify-end gap-1.5">
                    <button type="button" class="btn-secondary !px-2 !py-1 text-xs" @click="openPushEdit(item)">编辑</button>
                    <button type="button" class="btn-secondary !px-2 !py-1 text-xs" @click="toggleExpand(item)">
                      {{ expandedId === item.id ? '收起' : '应用' }}
                    </button>
                  </div>
                </td>
              </tr>

              <!-- 展开：应用明细 -->
              <tr v-if="expandedId === item.id" :key="item.id + '-apps'" class="bg-[var(--color-bg-subtle)]">
                <td :colspan="9" class="px-4 py-4">
                  <div v-if="appsOf(item.id).loading" class="flex justify-center py-8">
                    <span class="spinner" role="status" aria-label="加载中"></span>
                  </div>
                  <div v-else-if="appsOf(item.id).error" class="py-6 text-center text-sm">
                    <p class="text-[var(--color-danger)]">{{ appsOf(item.id).error }}</p>
                    <button type="button" class="btn-secondary mt-3" @click="loadApps(item.id)">重试</button>
                  </div>
                  <template v-else>
                    <div class="mb-3 flex flex-wrap items-center justify-between gap-2">
                      <p class="text-sm font-medium text-[var(--color-text)]">
                        应用明细
                        <span class="ml-1.5 text-xs font-normal text-[var(--color-text-tertiary)]">
                          {{ appsOf(item.id).items.length }} 条 · 应用名按包名统一，names 为多语言 JSON
                        </span>
                      </p>
                      <button type="button" class="btn-primary !py-1.5 text-xs" @click="openCreateApp(item.id)">新增应用</button>
                    </div>

                    <div class="overflow-x-auto rounded-lg border border-[var(--color-border)] bg-[var(--color-bg-surface)]">
                      <table class="table-base">
                        <thead>
                          <tr class="border-b border-[var(--color-border)] text-xs text-[var(--color-text-tertiary)]">
                            <th class="table-th">包名</th>
                            <th class="table-th">名称（中 / 英）</th>
                            <th class="table-th">版本号</th>
                            <th class="table-th">安装包</th>
                            <th class="table-th">大小</th>
                            <th class="table-th">日志</th>
                            <th class="table-th text-right">操作</th>
                          </tr>
                        </thead>
                        <tbody class="divide-y divide-[var(--color-border)]">
                          <tr v-for="app in appsOf(item.id).items" :key="app.id" class="align-top">
                            <td class="table-td break-all font-mono text-[11px]">{{ app.pkg || '（无包名）' }}</td>
                            <td class="table-td">
                              <div class="text-sm">{{ app.name || '—' }}</div>
                              <div class="text-[11px] text-[var(--color-text-tertiary)]">{{ app.name_en || '—' }}</div>
                            </td>
                            <td class="table-td font-mono text-xs">{{ app.pkg_ver || '—' }}</td>
                            <td class="table-td max-w-[260px]">
                              <div class="truncate font-mono text-[11px]" :title="app.file_name || ''">{{ app.file_name || '—' }}</div>
                              <div class="truncate text-[11px] text-[var(--color-text-tertiary)]">{{ app.apk_name || '' }}</div>
                            </td>
                            <td class="table-td whitespace-nowrap tabular-nums text-xs">{{ formatSize(app.size) }}</td>
                            <td class="table-td tabular-nums text-xs">{{ Array.isArray(app.logsObj) ? app.logsObj.length : 0 }}</td>
                            <td class="table-td text-right">
                              <div class="flex items-center justify-end gap-1.5">
                                <button type="button" class="btn-secondary !px-2 !py-1 text-xs" @click="openEditApp(item.id, app)">编辑</button>
                                <button type="button" class="btn-secondary !px-2 !py-1 text-xs !text-[var(--color-danger)]" @click="removeApp(item.id, app)">删除</button>
                              </div>
                            </td>
                          </tr>
                          <tr v-if="!appsOf(item.id).items.length">
                            <td :colspan="7" class="table-td py-8 text-center text-sm text-[var(--color-text-tertiary)]">
                              该推送暂无应用明细
                            </td>
                          </tr>
                        </tbody>
                      </table>
                    </div>

                    <!-- 无包名模块（changelog） -->
                    <details v-if="Array.isArray(appsOf(item.id).push?.changelog) && appsOf(item.id).push.changelog.length" class="mt-3">
                      <summary class="cursor-pointer text-xs text-[var(--color-text-secondary)]">
                        无包名模块 {{ appsOf(item.id).push.changelog.length }} 个（存在推送的 changelog，点「编辑」推送可修改）
                      </summary>
                      <pre class="mt-2 max-h-64 overflow-auto rounded-lg border border-[var(--color-border)] bg-[var(--color-bg-surface)] p-3 font-mono text-[11px] text-[var(--color-text-secondary)]">{{ prettyJson(appsOf(item.id).push.changelog) }}</pre>
                    </details>
                  </template>
                </td>
              </tr>
            </template>
          </tbody>
        </table>
      </div>

      <Pagination
        :page="page"
        :page-size="pageSize"
        :total="total"
        :loading="loading"
        @change="onPageChange"
      />
    </div>

    <!-- 推送编辑：复用通用记录弹窗 -->
    <RecordModal
      :open="pushModalOpen"
      table="xms"
      :columns="xmsColumns"
      :record="editingPush"
      :title="editingPush ? `编辑 XMS 推送 #${editingPush.id}` : 'XMS 推送'"
      @close="pushModalOpen = false"
      @saved="onPushSaved"
    />

    <!-- 应用编辑 / 新增 -->
    <Teleport to="body">
      <Transition
        enter-active-class="transition duration-200 ease-out"
        enter-from-class="opacity-0"
        enter-to-class="opacity-100"
        leave-active-class="transition duration-150 ease-in"
        leave-from-class="opacity-100"
        leave-to-class="opacity-0"
      >
        <div v-if="appModalOpen" class="fixed inset-0 z-[90] overflow-y-auto" role="dialog" aria-modal="true" aria-label="编辑 XMS 应用">
          <div class="absolute inset-0 bg-black/40 backdrop-blur-sm" @click="closeAppModal"></div>
          <div class="relative mx-auto my-8 w-full max-w-3xl rounded-xl border border-[var(--color-border)] bg-[var(--color-bg-surface)] p-6 shadow-xl">
            <div class="mb-5 flex items-start justify-between gap-4">
              <div>
                <h3 class="text-base font-semibold text-[var(--color-text)]">
                  {{ appForm.id ? `编辑应用 #${appForm.id}` : '新增应用' }}
                </h3>
                <p class="mt-0.5 text-xs text-[var(--color-text-tertiary)]">
                  推送 #{{ appForm.xms_id }} · 同一包名的应用名会跨推送统一，改动后可在导出前用 enrich_xms.py 再整理一次
                </p>
              </div>
              <button type="button" class="btn-secondary !px-2 !py-1 text-xs" @click="closeAppModal">关闭</button>
            </div>

            <form class="grid grid-cols-1 gap-4 sm:grid-cols-2" @submit.prevent="saveApp">
              <div class="sm:col-span-2">
                <label class="form-label" for="app-pkg">包名 <span class="text-[var(--color-danger)]">*</span></label>
                <input id="app-pkg" v-model.trim="appForm.pkg" type="text" class="input-base font-mono text-sm" placeholder="com.miui.securitycenter" required />
              </div>

              <div>
                <label class="form-label" for="app-name">中文名</label>
                <input id="app-name" v-model="appForm.name" type="text" class="input-base text-sm" placeholder="手机管家" />
              </div>
              <div>
                <label class="form-label" for="app-name-en">英文名</label>
                <input id="app-name-en" v-model="appForm.name_en" type="text" class="input-base text-sm" placeholder="Security" />
              </div>

              <div class="sm:col-span-2">
                <label class="form-label" for="app-names">
                  names（多语言 JSON）
                  <span class="ml-1 text-xs font-normal text-[var(--color-text-tertiary)]">留空则按中文 / 英文名生成 {"zh","en"}；键名同 logs 列：zh / zh_tw / en / ja / `in`(印地) / …</span>
                </label>
                <textarea
                  id="app-names"
                  v-model="appForm.names"
                  rows="4"
                  class="input-base font-mono text-xs"
                  :class="jsonError.names ? '!border-[var(--color-danger)]' : ''"
                  spellcheck="false"
                  placeholder='{"zh":"手机管家","en":"Security","ja":"セキュリティ"}'
                ></textarea>
                <p v-if="jsonError.names" class="mt-1 text-xs text-[var(--color-danger)]">{{ jsonError.names }}</p>
                <div class="mt-1.5 flex flex-wrap items-center gap-2">
                  <button
                    type="button"
                    class="btn-secondary !py-1 text-xs"
                    :disabled="translating"
                    @click="completeTranslations"
                  >
                    {{ translating ? '查询中…' : '用 logs 表补全翻译' }}
                  </button>
                  <span class="text-[11px] text-[var(--color-text-tertiary)]">
                    按中文 / 英文名匹配 logs 的 module 行；保存时也会自动补全，并同步到同包名的其它推送
                  </span>
                </div>
              </div>

              <div>
                <label class="form-label" for="app-ver">应用版本号</label>
                <input id="app-ver" v-model.trim="appForm.pkg_ver" type="text" class="input-base font-mono text-sm" placeholder="40011279" />
              </div>
              <div>
                <label class="form-label" for="app-apk-name">安装包名称</label>
                <input id="app-apk-name" v-model="appForm.apk_name" type="text" class="input-base text-sm" placeholder="平板管家-CN-OS3" />
              </div>

              <div class="sm:col-span-2">
                <label class="form-label" for="app-file">APK 文件名</label>
                <input id="app-file" v-model.trim="appForm.file_name" type="text" class="input-base font-mono text-xs" placeholder="securitycenter-…-universal-….apk" />
              </div>

              <div>
                <label class="form-label" for="app-size">体积（字节）</label>
                <input id="app-size" v-model.trim="appForm.size" type="number" min="0" class="input-base font-mono text-sm" placeholder="74830912" />
              </div>
              <div>
                <label class="form-label" for="app-md5">MD5</label>
                <input id="app-md5" v-model.trim="appForm.md5" type="text" class="input-base font-mono text-xs" />
              </div>

              <div class="sm:col-span-2">
                <label class="form-label" for="app-sha256">SHA-256</label>
                <input id="app-sha256" v-model.trim="appForm.sha256" type="text" class="input-base font-mono text-xs" />
              </div>

              <div class="sm:col-span-2">
                <label class="form-label" for="app-logs">
                  更新日志（JSON 数组）
                  <span class="ml-1 text-xs font-normal text-[var(--color-text-tertiary)]">条目形如 {"zh":"…","en":"…","ja":"…"}，可只写中文</span>
                </label>
                <textarea
                  id="app-logs"
                  v-model="appForm.logs"
                  rows="6"
                  class="input-base font-mono text-xs"
                  :class="jsonError.logs ? '!border-[var(--color-danger)]' : ''"
                  spellcheck="false"
                  placeholder='[{"zh":"优化 …","en":"Optimization: …"}]'
                ></textarea>
                <p v-if="jsonError.logs" class="mt-1 text-xs text-[var(--color-danger)]">{{ jsonError.logs }}</p>
              </div>

              <p v-if="appError" class="sm:col-span-2 rounded-lg border border-[var(--color-danger)]/40 bg-[var(--color-danger)]/5 px-3 py-2 text-xs text-[var(--color-danger)]">
                {{ appError }}
              </p>

              <div class="flex flex-wrap items-center justify-end gap-2 sm:col-span-2">
                <label class="mr-auto flex items-center gap-1.5 text-xs text-[var(--color-text-secondary)]">
                  <input v-model="appForm.propagate" type="checkbox" />
                  保存后同步同包名的其他推送（只同步名称与多语言，日志各推送保留）
                </label>
                <button type="button" class="btn-secondary" @click="closeAppModal">取消</button>
                <button type="submit" class="btn-primary" :disabled="appSaving">
                  {{ appSaving ? '保存中…' : '保存' }}
                </button>
              </div>
            </form>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup>
const { getTableMeta, getRecord, deleteRecord } = useAdminApi()
const { push } = useToast()

const loading = ref(false)
const error = ref('')
const items = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const search = ref('')
const latestOnly = ref(false)
const version = ref('')
const versions = ref([])

const expandedId = ref(0)
const appsById = ref({})

const xmsColumns = ref([])
const pushModalOpen = ref(false)
const editingPush = ref(null)

const appModalOpen = ref(false)
const appSaving = ref(false)
const translating = ref(false)
const appError = ref('')
const jsonError = ref({})
const appForm = ref(emptyAppForm())

function emptyAppForm() {
  return {
    id: null,
    xms_id: 0,
    pkg: '',
    name: '',
    name_en: '',
    names: '',
    pkg_ver: '',
    apk_name: '',
    file_name: '',
    size: '',
    md5: '',
    sha256: '',
    logs: '',
    propagate: true,
  }
}

function osLabel(value) {
  const first = String(value || '').trim().charAt(0).toUpperCase()
  if (!/^[A-Z]$/.test(first)) return ''
  return `OS${first.charCodeAt(0) - 64}`
}

function deviceName(item) {
  const name = item?.deviceName
  if (name && typeof name === 'object') {
    return name.zh || name.en || item.device
  }
  return item?.device || ''
}

function formatSize(size) {
  const value = Number(size)
  if (!value) return '—'
  const units = ['B', 'KB', 'MB', 'GB']
  let n = value
  let unit = 0
  while (n >= 1024 && unit < units.length - 1) {
    n /= 1024
    unit += 1
  }
  return `${n >= 100 || unit === 0 ? Math.round(n) : n.toFixed(1)} ${units[unit]}`
}

function prettyJson(value) {
  try {
    return JSON.stringify(value, null, 2)
  } catch {
    return String(value ?? '')
  }
}

function appsOf(id) {
  return appsById.value[id] || { loading: false, error: '', items: [], push: null }
}

const load = async () => {
  loading.value = true
  error.value = ''
  try {
    const res = await $fetch('/api/xms/list', {
      params: {
        page: page.value,
        pageSize: pageSize.value,
        search: search.value,
        latest: latestOnly.value ? 1 : 0,
        version: version.value,
      },
    })
    items.value = res.items
    total.value = res.total
    versions.value = res.versions
  } catch (e) {
    error.value = errorMessage(e)
  } finally {
    loading.value = false
  }
}

const reload = () => {
  page.value = 1
  load()
  if (expandedId.value) loadApps(expandedId.value)
}

let searchTimer = null
const onSearchInput = () => {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    page.value = 1
    load()
  }, 250)
}

const onPageChange = (next) => {
  page.value = next
  load()
}

const onPageSizeChange = () => {
  page.value = 1
  load()
}

const toggleLatest = () => {
  latestOnly.value = !latestOnly.value
  page.value = 1
  load()
}

const loadApps = async (id) => {
  appsById.value = { ...appsById.value, [id]: { loading: true, error: '', items: [], push: null } }
  try {
    const res = await $fetch('/api/xms/apps', { params: { xmsId: id } })
    appsById.value = { ...appsById.value, [id]: { loading: false, error: '', items: res.items, push: res.push } }
  } catch (e) {
    appsById.value = { ...appsById.value, [id]: { loading: false, error: errorMessage(e), items: [], push: null } }
  }
}

const toggleExpand = async (item) => {
  if (expandedId.value === item.id) {
    expandedId.value = 0
    return
  }
  expandedId.value = item.id
  if (!appsById.value[item.id] || appsById.value[item.id].error) {
    await loadApps(item.id)
  }
}

const openPushEdit = async (item) => {
  try {
    if (!xmsColumns.value.length) {
      const meta = await getTableMeta('xms')
      xmsColumns.value = meta.columns
    }
    const record = await getRecord('xms', item.id)
    let changelog = record.changelog
    if (typeof changelog === 'string' && changelog.trim()) {
      try {
        changelog = JSON.parse(changelog)
      } catch {
        /* 非法 JSON 原样展示，交给弹窗校验 */
      }
    }
    editingPush.value = {
      ...record,
      push_date: record.push_date ? String(record.push_date).slice(0, 10) : '',
      changelog: changelog ? (typeof changelog === 'string' ? changelog : prettyJson(changelog)) : '',
    }
    pushModalOpen.value = true
  } catch (e) {
    push('error', errorMessage(e))
  }
}

const onPushSaved = async () => {
  pushModalOpen.value = false
  push('success', 'XMS 推送已更新')
  await load()
  if (expandedId.value) await loadApps(expandedId.value)
}

const openCreateApp = (xmsId) => {
  appForm.value = { ...emptyAppForm(), xms_id: xmsId }
  appError.value = ''
  jsonError.value = {}
  appModalOpen.value = true
}

const openEditApp = (xmsId, app) => {
  appForm.value = {
    id: app.id,
    xms_id: xmsId,
    pkg: app.pkg || '',
    name: app.name || '',
    name_en: app.name_en || '',
    names: app.namesObj ? prettyJson(app.namesObj) : (typeof app.names === 'string' ? app.names : ''),
    pkg_ver: app.pkg_ver || '',
    apk_name: app.apk_name || '',
    file_name: app.file_name || '',
    size: app.size === null || app.size === undefined ? '' : String(app.size),
    md5: app.md5 || '',
    sha256: app.sha256 || '',
    logs: Array.isArray(app.logsObj) ? prettyJson(app.logsObj) : (typeof app.logs === 'string' ? app.logs : ''),
    propagate: true,
  }
  appError.value = ''
  jsonError.value = {}
  appModalOpen.value = true
}

const closeAppModal = () => {
  appModalOpen.value = false
  appSaving.value = false
}

/** 校验 JSON 文本框；空值允许 */
const validateJsonField = (key, label) => {
  const raw = String(appForm.value[key] ?? '').trim()
  if (!raw) return null
  try {
    return JSON.parse(raw)
  } catch {
    jsonError.value = { ...jsonError.value, [key]: `${label} 不是合法的 JSON` }
    return null
  }
}

const completeTranslations = async () => {
  const name = appForm.value.name.trim()
  const nameEn = appForm.value.name_en.trim()
  if (!name && !nameEn) {
    push('info', '请先填写中文名或英文名，再用 logs 表匹配')
    return
  }

  translating.value = true
  try {
    const res = await $fetch('/api/xms/translate', { params: { name, nameEn } })
    if (!res?.found) {
      // logs 里还没有这个应用名：先写一条 module 占位，之后可在「日志录入」补翻译
      const ensured = await $fetch('/api/xms/module', { method: 'POST', body: { name, nameEn } })
      push('info', ensured?.created
        ? 'logs 表中没有该模块，已先写入占位，待补翻译'
        : 'logs 表已有该模块，但还没有译文')
      return
    }

    let current = {}
    if (appForm.value.names.trim()) {
      try {
        current = JSON.parse(appForm.value.names)
      } catch {
        jsonError.value = { ...jsonError.value, names: 'names 不是合法的 JSON，无法补全' }
        return
      }
    }
    if (!current || typeof current !== 'object' || Array.isArray(current)) {
      jsonError.value = { ...jsonError.value, names: 'names 必须是 JSON 对象' }
      return
    }

    let added = 0
    for (const [key, value] of Object.entries(res.translations || {})) {
      if (!current[key]) {
        current[key] = value
        added += 1
      }
    }
    if (name) current.zh = name
    if (nameEn) current.en = nameEn
    if (!appForm.value.name_en && current.en) appForm.value.name_en = current.en

    appForm.value.names = JSON.stringify(current, null, 2)
    jsonError.value = { ...jsonError.value, names: '' }
    push('success', added ? `已补全 ${added} 种语言` : '没有缺失的语种')
  } catch (e) {
    push('error', errorMessage(e))
  } finally {
    translating.value = false
  }
}

const saveApp = async () => {
  appError.value = ''
  jsonError.value = {}

  if (!appForm.value.pkg) {
    appError.value = '包名不能为空'
    return
  }

  let names = validateJsonField('names', 'names')
  if (appForm.value.names.trim() && names === null) return
  if (names && (typeof names !== 'object' || Array.isArray(names))) {
    jsonError.value = { names: 'names 必须是 JSON 对象' }
    return
  }
  const logsRaw = String(appForm.value.logs ?? '').trim()
  const logs = validateJsonField('logs', '更新日志')
  if (logsRaw && logs === null) return
  if (logs !== null && !Array.isArray(logs)) {
    jsonError.value = { logs: '更新日志必须是 JSON 数组' }
    return
  }

  if (!names) {
    const fallback = {}
    if (appForm.value.name) fallback.zh = appForm.value.name
    if (appForm.value.name_en) fallback.en = appForm.value.name_en
    // 中文 / 英文都为空时写 NULL，避免留下一个空的 {} 对象
    names = Object.keys(fallback).length ? fallback : null
  }

  const payload = {
    xms_id: appForm.value.xms_id,
    pkg: appForm.value.pkg,
    name: appForm.value.name,
    name_en: appForm.value.name_en,
    names: names ? JSON.stringify(names) : null,
    pkg_ver: appForm.value.pkg_ver,
    apk_name: appForm.value.apk_name,
    file_name: appForm.value.file_name,
    size: appForm.value.size === '' ? null : Number(appForm.value.size),
    md5: appForm.value.md5,
    sha256: appForm.value.sha256,
    logs: logs && logs.length ? JSON.stringify(logs) : null,
  }

  appSaving.value = true
  try {
    const res = await $fetch('/api/xms/app', {
      method: 'POST',
      body: { ...payload, id: appForm.value.id || null, propagate: appForm.value.propagate },
    })
    const propagated = Number(res?.propagated || 0)
    if (res?.logCreated) {
      push('info', `应用名「${res.name || appForm.value.name}」已写入 logs 表，可稍后补全翻译`)
    }
    if (appForm.value.id) {
      push('success', propagated > 1
        ? `应用 #${appForm.value.id} 已更新，并同步了同包名的 ${propagated} 条记录`
        : `应用 #${appForm.value.id} 已更新`)
    } else {
      push('success', '应用已新增')
    }
    await loadApps(appForm.value.xms_id)
    await load()
    appModalOpen.value = false
  } catch (e) {
    appError.value = errorMessage(e)
  } finally {
    appSaving.value = false
  }
}

const removeApp = async (xmsId, app) => {
  if (!window.confirm(`确认删除应用「${app.name || app.pkg}」（id=${app.id}）？`)) return
  try {
    await deleteRecord('xms_app', app.id)
    push('success', '应用已删除')
    await loadApps(xmsId)
    await load()
  } catch (e) {
    push('error', errorMessage(e))
  }
}

onMounted(load)
</script>
