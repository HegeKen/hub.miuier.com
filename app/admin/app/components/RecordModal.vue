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
            <span class="text-[var(--color-text)]">{{ primaryValue !== undefined ? String(primaryValue) : '自动生成' }}</span>
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
                <!-- brands 自动生成按钮 -->
                <button
                  v-if="col.name === 'brands'"
                  type="button"
                  class="btn-secondary mt-1.5 !py-1 text-xs"
                  @click="autoGenBrands(col.name)"
                >
                  从分支生成
                </button>

                <!-- 双语字段：zh / en 独立输入 -->
                <div v-if="isBilingual(col)" class="grid grid-cols-2 gap-3">
                  <div>
                    <label :for="'f-' + col.name + '_zh'" class="text-xs text-[var(--color-text-tertiary)]">中文</label>
                    <input
                      :id="'f-' + col.name + '_zh'"
                      v-model="form[col.name + '_zh']"
                      type="text"
                      class="input-base font-mono text-sm"
                      placeholder="中文名称"
                    />
                  </div>
                  <div>
                    <label :for="'f-' + col.name + '_en'" class="text-xs text-[var(--color-text-tertiary)]">English</label>
                    <input
                      :id="'f-' + col.name + '_en'"
                      v-model="form[col.name + '_en']"
                      type="text"
                      class="input-base font-mono text-sm"
                      placeholder="English name"
                    />
                  </div>
                </div>
                <!-- names 自动生成按钮 -->
                <button
                  v-if="col.name === 'names'"
                  type="button"
                  class="btn-secondary mt-1.5 !py-1 text-xs"
                  @click="autoGenNames(col.name)"
                >
                  从分支生成
                </button>

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

                <!-- 下拉选项（formOptions 指定的字段） -->
                <select
                  v-else-if="formOptions[col.name]"
                  :id="'f-' + col.name"
                  v-model="form[col.name]"
                  class="input-base font-mono text-sm"
                >
                  <option v-if="col.nullable" value="">留空则写入 NULL</option>
                  <option
                    v-for="opt in formOptions[col.name]"
                    :key="opt.value"
                    :value="opt.value"
                  >{{ opt.label }}</option>
                </select>

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
                <p v-else-if="crossErrors[col.name]" class="mt-1 text-xs text-[var(--color-warn)]">
                  {{ crossErrors[col.name] }}
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
  /** 指定字段的下拉选项，格式 { fieldName: [{ value, label }] } */
  formOptions: { type: Object, default: () => ({}) },
})
const emit = defineEmits(['close', 'saved'])

const { createRecord, updateRecord } = useAdminApi()
const { push } = useToast()

const form = ref({})
const saving = ref(false)
const jsonInvalid = ref({})

const primaryCol = computed(() => props.columns.find((c) => c.isPrimary) || null)

/** 主键值：有值表示编辑，无值（undefined/null/空串）表示新增 */
const primaryValue = computed(() => {
  if (!primaryCol.value) return undefined
  const v = props.record?.[primaryCol.value.name]
  return v === null || v === undefined || v === '' ? undefined : v
})

const editableColumns = computed(() => props.columns.filter((c) => !c.isPrimary && !GLOBAL_COLS.has(c.name)))

/** 包文件名相关字段：文件名较长，独占一行并支持换行显示 */
const FILENAME_COLS = new Set(['recovery', 'fastboot', 'ctelecom', 'cmobile', 'cunicom', 'others'])
const isFilenameCol = (col) => FILENAME_COLS.has(col.name)

/** 双语字段：拆分为 _zh / _en 两个独立输入 */
const BILINGUAL_COLS = new Set(['names', 'xiaomi', 'redmi', 'poco'])
const isBilingual = (col) => BILINGUAL_COLS.has(col.name)

/** 设备级全局字段：不在分支编辑弹窗中显示 */
const GLOBAL_COLS = new Set(['full_brands', 'full_names'])

/** 解析 JSON 中的 zh/en 值 */
function parseZhEn(raw) {
  if (!raw) return { zh: '', en: '' }
  try {
    const obj = JSON.parse(raw)
    if (obj && typeof obj === 'object' && !Array.isArray(obj)) {
      return { zh: obj.zh || '', en: obj.en || '' }
    }
  } catch { /* 忽略 */ }
  return { zh: raw, en: '' }
}

/** 初始化表单 */
const initForm = () => {
  jsonInvalid.value = {}
  crossErrors.value = {}
  const init = {}
  for (const col of props.columns) {
    if (col.isPrimary || GLOBAL_COLS.has(col.name)) continue
    const v = props.record?.[col.name]
    const str = v === null || v === undefined ? '' : String(v)
    if (isBilingual(col)) {
      const { zh, en } = parseZhEn(str)
      init[col.name + '_zh'] = zh
      init[col.name + '_en'] = en
    } else {
      init[col.name] = str
    }
  }
  form.value = init
}

watch(
  () => props.open,
  (open) => {
    if (open) initForm()
  },
)

/** 校验 JSON 字段（跳过已拆分为 _zh/_en 的双语字段） */
const validateJson = () => {
  const errors = {}
  for (const col of props.columns) {
    if (!col.isJson || isBilingual(col)) continue
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

/** 跨字段逻辑验证（full_brands ↔ brands, names ⊆ full_names, names ⊆ brand fields） */
const crossErrors = ref({})

const validateCrossFields = () => {
  const errs = {}

  // full_brands (JSON 数组) ↔ brands (逗号分隔) 一致性
  const fb = form.value.full_brands
  const br = form.value.brands
  if (fb) {
    try {
      const arr = JSON.parse(fb)
      if (Array.isArray(arr)) {
        const fullSet = new Set(arr.filter(Boolean).map(String))
        const brSet = new Set(br ? br.split(',').map((s) => s.replace(/^"|"$/g, '').trim()).filter(Boolean) : [])
        if (fullSet.size > 0 && brSet.size > 0) {
          const missing = [...fullSet].filter((v) => !brSet.has(v))
          const extra = [...brSet].filter((v) => !fullSet.has(v))
          if (missing.length > 0) errs.full_brands = `full_brands 中的 ${missing.join(', ')} 未出现在 brands 中`
          if (extra.length > 0) errs.brands = `brands 中的 ${extra.join(', ')} 未出现在 full_brands 中`
        }
      }
    } catch { /* JSON 格式错误已由 validateJson 捕获 */ }
  }

  // full_names ↔ names：names 的 zh/en 应与 full_names 同 key 值一致（names ⊆ full_names）
  const fnZh = form.value.full_names_zh || ''
  const fnEn = form.value.full_names_en || ''
  const nmZh = form.value.names_zh || ''
  const nmEn = form.value.names_en || ''
  if (fnZh && nmZh && fnZh !== nmZh) {
    errs.names_zh = `names 中文与 full_names 中文值不一致（names 应为 full_names 的子集）`
  }
  if (fnEn && nmEn && fnEn !== nmEn) {
    errs.names_en = `names English 与 full_names English 值不一致`
  }

  // names ↔ xiaomi/redmi/poco：names 应为品牌字段的合集
  const brandFields = ['xiaomi', 'redmi', 'poco']
  // 收集所有品牌字段的 zh/en 值
  const brandZh = {}
  const brandEn = {}
  for (const f of brandFields) {
    if (form.value[f + '_zh']) brandZh[f] = form.value[f + '_zh']
    if (form.value[f + '_en']) brandEn[f] = form.value[f + '_en']
  }
  // names_zh 应包含所有品牌 zh 值
  if (nmZh) {
    for (const [k, v] of Object.entries(brandZh)) {
      if (nmZh !== v && !nmZh.includes(v)) {
        errs.names_zh = `names 中文未包含 ${k} 的值 "${v}"`
        break
      }
    }
  }
  // names_en 应包含所有品牌 en 值
  if (nmEn) {
    for (const [k, v] of Object.entries(brandEn)) {
      if (nmEn !== v && !nmEn.includes(v)) {
        errs.names_en = `names English 未包含 ${k} 的 value "${v}"`
        break
      }
    }
  }

  crossErrors.value = errs
  return Object.keys(errs).length === 0
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

/** 从 xiaomi / redmi / poco 自动生成 names（多品牌用 / 拼接，顺序：Xiaomi → Redmi → POCO） */
const autoGenNames = (target) => {
  const brandFields = ['xiaomi', 'redmi', 'poco']
  const namesZh = []
  const namesEn = []
  for (const field of brandFields) {
    const zh = form.value[field + '_zh'] || ''
    const en = form.value[field + '_en'] || ''
    if (zh) namesZh.push(zh)
    if (en) namesEn.push(en)
  }
  if (namesZh.length === 0 && namesEn.length === 0) {
    push('info', 'xiaomi / redmi / poco 均为空，无法生成')
    return
  }
  form.value[target + '_zh'] = namesZh.join(' / ')
  form.value[target + '_en'] = namesEn.join(' / ')
  push('success', `${target} 已从品牌字段自动生成`)
}

/** 从 xiaomi / redmi / poco 自动生成 full_brands 或 brands */
const autoGenBrands = (target) => {
  const brandMap = { xiaomi: 'Xiaomi', redmi: 'Redmi', poco: 'POCO' }
  const detected = []
  for (const [field, label] of Object.entries(brandMap)) {
    const zh = form.value[field + '_zh'] || ''
    const en = form.value[field + '_en'] || ''
    if (zh || en) detected.push(label)
  }
  if (detected.length === 0) {
    push('info', 'xiaomi / redmi / poco 均为空或无法解析，无法生成')
    return
  }
  if (target === 'full_brands') {
    form.value.full_brands = JSON.stringify(detected)
  } else {
    form.value.brands = `"${detected.join(', ')}"`
  }
  push('success', `${target} 已从品牌字段自动生成`)
}

const onSave = async () => {
  if (!validateJson()) {
    push('error', '存在 JSON 格式错误的字段，请修正后再保存')
    return
  }
  if (!validateCrossFields()) {
    push('error', '存在字段一致性错误，请修正后再保存')
    return
  }

  saving.value = true
  try {
    // 合并双语字段：_zh/_en → JSON
    const payload = {}
    for (const [k, v] of Object.entries(form.value)) {
      if (k.endsWith('_zh') || k.endsWith('_en')) continue
      payload[k] = v
    }
    for (const name of BILINGUAL_COLS) {
      const zh = form.value[name + '_zh'] || ''
      const en = form.value[name + '_en'] || ''
      if (zh || en) {
        const obj = {}
        if (zh) obj.zh = zh
        if (en) obj.en = en
        payload[name] = JSON.stringify(obj)
      } else {
        payload[name] = ''
      }
    }
    const id = primaryValue.value
    if (id !== undefined) {
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
