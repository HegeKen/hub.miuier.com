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
      <div v-if="open" class="fixed inset-0 z-[90] overflow-y-auto" role="dialog" aria-modal="true" :aria-label="title">
        <div class="absolute inset-0 bg-black/40 backdrop-blur-sm" @click="onClose"></div>
        <div class="relative mx-auto my-8 w-full max-w-3xl rounded-xl border border-[var(--color-border)] bg-[var(--color-bg-surface)] p-6 shadow-xl">
          <!-- 标题 -->
          <div class="mb-5 flex items-start justify-between gap-4">
            <div>
              <h3 class="text-base font-semibold text-[var(--color-text)]">{{ title }}</h3>
              <p class="mt-0.5 font-mono text-xs text-[var(--color-text-tertiary])">表 {{ table }}</p>
            </div>
            <button
              type="button"
              class="flex h-8 w-8 shrink-0 items-center justify-center rounded-md text-[var(--color-text-tertiary)] transition-colors hover:bg-[var(--color-bg-subtle)] hover:text-[var(--color-text)]"
              aria-label="关闭"
              @click="onClose"
            >
              <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" aria-hidden="true">
                <path stroke-linecap="round" stroke-linejoin="round" d="M6 18 18 6M6 6l12 12" />
              </svg>
            </button>
          </div>

          <!-- 主键只读提示 -->
          <div v-if="primaryCol" class="mb-4 flex items-center gap-2 rounded-lg border border-[var(--color-border)] bg-[var(--color-bg-subtle)] px-3 py-2 font-mono text-xs text-[var(--color-text-secondary)]">
            <span class="font-medium">id</span>
            <span class="text-[var(--color-text-tertiary)]">=</span>
            <span class="text-[var(--color-text)]">{{ record ? String(record[primaryCol.name]) : '自动生成' }}</span>
            <span class="text-[var(--color-text-tertiary)]">（主键，不可修改）</span>
          </div>

          <!-- 表单 -->
          <form id="record-form" class="grid grid-cols-1 gap-4 sm:grid-cols-2" @submit.prevent="onSave">
            <template v-for="col in editableColumns" :key="col.name">
              <div :class="col.isLong || col.isJson || isFilenameCol(col) ? 'sm:col-span-2' : ''">
                <label :for="'f-' + col.name" class="form-label" :title="col.comment || ''">
                  <span class="font-mono">{{ col.name }}</span>
                  <span v-if="col.comment" class="ml-1.5 text-xs font-normal text-[var(--color-text-tertiary)]">{{ col.comment }}</span>
                </label>

                <!-- 长文本 / JSON / 包文件名 -->
                <textarea
                  v-if="col.isLong || col.isJson || isFilenameCol(col)"
                  :id="'f-' + col.name"
                  v-model="form[col.name]"
                  :rows="col.isJson ? 6 : col.isLong ? 3 : 2"
                  class="input-base font-mono text-xs"
                  :class="jsonInvalid[col.name] ? '!border-[var(--color-danger)]' : ''"
                  :placeholder="col.nullable ? '留空则写入 NULL' : ''"
                  spellcheck="false"
                ></textarea>

                <!-- 数字 -->
                <input
                  v-else-if="col.isNumber"
                  :id="'f-' + col.name"
                  v-model="form[col.name]"
                  type="number"
                  step="1"
                  class="input-base font-mono text-sm"
                  :placeholder="col.nullable ? '留空则写入 NULL' : ''"
                />

                <!-- 日期 -->
                <input
                  v-else-if="col.isDate"
                  :id="'f-' + col.name"
                  v-model="form[col.name]"
                  type="date"
                  class="input-base font-mono text-sm"
                />

                <!-- 普通文本 -->
                <input
                  v-else
                  :id="'f-' + col.name"
                  v-model="form[col.name]"
                  type="text"
                  class="input-base text-sm"
                  :placeholder="col.nullable ? '留空则写入 NULL' : ''"
                />

                <p v-if="jsonInvalid[col.name]" class="mt-1 text-xs text-[var(--color-danger)]">
                  JSON 格式无效：{{ jsonInvalid[col.name] }}
                </p>
              </div>
            </template>
          </form>

          <!-- 底部操作 -->
          <div class="mt-6 flex flex-wrap items-center justify-between gap-3 border-t border-[var(--color-border)] pt-4">
            <div class="flex gap-2">
              <button
                type="button"
                class="btn-secondary !py-1.5 text-xs"
                @click="formatAllJson"
              >
                格式化 JSON 字段
              </button>
              <span class="self-center text-xs text-[var(--color-text-tertiary)]">
                <span class="mr-1 inline-block h-2 w-2 rounded-full bg-[var(--color-accent)]" aria-hidden="true"></span>
                带注释的字段为 JSON 格式，保存前会校验
              </span>
            </div>
            <div class="flex gap-2">
              <button type="button" class="btn-secondary" @click="onClose">取消</button>
              <button type="submit" form="record-form" class="btn-primary" :disabled="saving">
                {{ saving ? '保存中…' : '保存' }}
              </button>
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
  columns: { type: Array, default: () => [] },
  record: { type: Object, default: null },
  title: { type: String, default: '' },
})
const emit = defineEmits(['close', 'saved'])

const { createRecord, updateRecord } = useAdminApi()
const { push } = useToast()

const form = ref({})
const saving = ref(false)
const jsonInvalid = ref({})

const primaryCol = computed(() => props.columns.find((c) => c.isPrimary) || null)

const editableColumns = computed(() => props.columns.filter((c) => !c.isPrimary))

/** 包文件名相关字段：文件名较长，独占一行并支持换行显示 */
const FILENAME_COLS = new Set(['recovery', 'fastboot', 'ctelecom', 'cmobile', 'cunicom', 'others'])
const isFilenameCol = (col) => FILENAME_COLS.has(col.name)

/** 初始化表单 */
const initForm = () => {
  jsonInvalid.value = {}
  const init = {}
  for (const col of props.columns) {
    if (col.isPrimary) continue
    const v = props.record?.[col.name]
    init[col.name] = v === null || v === undefined ? '' : String(v)
  }
  form.value = init
}

watch(
  () => props.open,
  (open) => {
    if (open) initForm()
  },
)

/** 校验 JSON 字段 */
const validateJson = () => {
  const errors = {}
  for (const col of props.columns) {
    if (!col.isJson) continue
    const v = form.value[col.name]
    if (v === '' || v === null || v === undefined) continue
    try {
      JSON.parse(v)
    } catch (e) {
      errors[col.name] = e instanceof Error ? e.message : '解析失败'
    }
  }
  jsonInvalid.value = errors
  return Object.keys(errors).length === 0
}

/** 格式化所有 JSON 字段为缩进 JSON */
const formatAllJson = () => {
  for (const col of props.columns) {
    if (!col.isJson) continue
    const v = form.value[col.name]
    if (!v) continue
    try {
      form.value[col.name] = JSON.stringify(JSON.parse(v), null, 2)
    } catch {
      /* 忽略无法解析的字段，由提交时校验提示 */
    }
  }
}

const onSave = async () => {
  if (!validateJson()) {
    push('error', '存在 JSON 格式错误的字段，请修正后再保存')
    return
  }

  saving.value = true
  try {
    const payload = { ...form.value }
    if (props.record) {
      const id = props.record[primaryCol.value.name]
      await updateRecord(props.table, id, payload)
      push('success', `记录 #${id} 更新成功`)
    } else {
      const res = await createRecord(props.table, payload)
      push('success', `新记录已创建（id=${res.id}）`)
    }
    emit('saved')
    emit('close')
  } catch (e) {
    push('error', errorMessage(e))
  } finally {
    saving.value = false
  }
}

const onClose = () => {
  if (!saving.value) emit('close')
}

// Esc 关闭
const onKeydown = (e) => {
  if (e.key === 'Escape' && props.open) onClose()
}
onMounted(() => window.addEventListener('keydown', onKeydown))
onBeforeUnmount(() => window.removeEventListener('keydown', onKeydown))
</script>
