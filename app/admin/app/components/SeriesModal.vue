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
      <div v-if="open" class="fixed inset-0 z-[90] overflow-y-auto" role="dialog" aria-modal="true" aria-label="系列编辑">
        <div class="absolute inset-0 bg-black/40 backdrop-blur-sm" @click="onClose"></div>
        <div class="relative mx-auto my-8 w-full max-w-3xl rounded-xl border border-[var(--color-border)] bg-[var(--color-bg-surface)] p-6 shadow-xl">
          <!-- 标题 -->
          <div class="mb-5 flex items-start justify-between gap-4">
            <div>
              <h3 class="text-base font-semibold text-[var(--color-text)]">{{ isEdit ? '编辑系列' : '新增系列' }}</h3>
              <p class="mt-0.5 text-xs text-[var(--color-text-secondary)]">
                用设备 id 有序数组控制该系列内机型的展示顺序；同品牌多马甲机型归入同一系列
              </p>
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

          <!-- 主键提示 -->
          <div v-if="isEdit" class="mb-4 flex items-center gap-2 rounded-lg border border-[var(--color-border)] bg-[var(--color-bg-subtle)] px-3 py-2 font-mono text-xs text-[var(--color-text-secondary)]">
            <span class="font-medium">id</span>
            <span class="text-[var(--color-text-tertiary)]">=</span>
            <span class="text-[var(--color-text)]">{{ form.id }}</span>
            <span class="text-[var(--color-text-tertiary)]">（主键，不可修改）</span>
          </div>

          <!-- 基础字段 -->
          <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div>
              <label class="form-label !mb-1">品牌 <span class="text-[var(--color-danger)]">*</span></label>
              <select v-model="form.brand" class="input-base text-sm">
                <option value="" disabled>选择品牌</option>
                <option v-for="b in brands" :key="b.key" :value="b.key">{{ b.label }}</option>
              </select>
            </div>
            <div>
              <label class="form-label !mb-1">排序权重 <span class="text-xs font-normal text-[var(--color-text-tertiary)]">越小越靠前</span></label>
              <input v-model="form.sort_order" type="number" step="1" class="input-base font-mono text-sm" />
            </div>
            <div>
              <label class="form-label !mb-1">系列中文名</label>
              <input v-model="form.name_zh" type="text" class="input-base text-sm" placeholder="如 红米 K 系列" />
            </div>
            <div>
              <label class="form-label !mb-1">系列英文名</label>
              <input v-model="form.name_en" type="text" class="input-base text-sm" placeholder="如 Redmi K Series" />
            </div>
          </div>

          <!-- 设备排序器 -->
          <div class="mt-5 rounded-lg border border-[var(--color-border)] p-4">
            <div class="mb-3 flex flex-wrap items-center justify-between gap-2">
              <h4 class="text-sm font-semibold text-[var(--color-text)]">
                系列内设备（{{ orderedDevices.length }}）
                <span class="ml-1.5 text-xs font-normal text-[var(--color-text-tertiary)]">数组顺序即展示顺序</span>
              </h4>
              <div class="relative w-56">
                <input
                  v-model="poolSearch"
                  type="search"
                  class="input-base !py-1.5 text-xs"
                  placeholder="搜索候选设备代号/名称…"
                />
              </div>
            </div>

            <!-- 已选设备（有序） -->
            <div class="mb-3">
              <p class="mb-1.5 text-xs font-medium text-[var(--color-text-secondary)]">已选设备</p>
              <div v-if="orderedDevices.length" class="max-h-[50vh] space-y-1.5 overflow-y-auto pr-1">
                <div
                  v-for="(d, idx) in orderedDevices"
                  :key="d.id"
                  class="flex items-center gap-2 rounded-lg border border-[var(--color-border)] bg-[var(--color-bg-subtle)] px-3 py-2"
                >
                  <span class="w-6 shrink-0 text-center font-mono text-xs text-[var(--color-text-tertiary)]">{{ idx + 1 }}</span>
                  <span class="min-w-0 flex-1 truncate text-sm font-medium text-[var(--color-text)]">{{ d.name }}</span>
                  <span class="font-mono text-xs text-[var(--color-text-tertiary)]">{{ d.device }}</span>
                  <div class="flex shrink-0 items-center gap-1">
                    <button type="button" class="btn-secondary !px-1.5 !py-0.5 text-xs" :disabled="idx === 0" @click="move(idx, -1)">↑</button>
                    <button type="button" class="btn-secondary !px-1.5 !py-0.5 text-xs" :disabled="idx === orderedDevices.length - 1" @click="move(idx, 1)">↓</button>
                    <button type="button" class="btn-secondary !px-1.5 !py-0.5 text-xs !text-[var(--color-danger)]" @click="removeAt(idx)">✕</button>
                  </div>
                </div>
              </div>
              <p v-else class="rounded-lg border border-dashed border-[var(--color-border)] px-3 py-4 text-center text-xs text-[var(--color-text-tertiary)]">
                尚未添加设备，从下方候选列表点击「＋」加入
              </p>
            </div>

            <!-- 候选设备 -->
            <div>
              <p class="mb-1.5 text-xs font-medium text-[var(--color-text-secondary)]">候选设备（品牌内）</p>
              <div class="max-h-56 overflow-y-auto rounded-lg border border-[var(--color-border)]">
                <button
                  v-for="d in poolDevices"
                  :key="d.id"
                  type="button"
                  class="flex w-full items-center gap-2 border-b border-[var(--color-border)] px-3 py-2 text-left transition-colors last:border-b-0 hover:bg-[var(--color-bg-subtle)]"
                  :disabled="selectedIds.includes(d.id)"
                  @click="addDevice(d)"
                >
                  <span class="min-w-0 flex-1 truncate text-sm text-[var(--color-text)]">{{ d.name }}</span>
                  <span class="font-mono text-xs text-[var(--color-text-tertiary)]">{{ d.device }}</span>
                  <span class="w-8 shrink-0 text-right text-xs" :class="selectedIds.includes(d.id) ? 'text-[var(--color-text-tertiary)]' : 'text-[var(--color-accent)]'">
                    {{ selectedIds.includes(d.id) ? '已加' : '＋' }}
                  </span>
                </button>
                <p v-if="poolDevices.length === 0" class="px-3 py-6 text-center text-xs text-[var(--color-text-tertiary)]">
                  没有候选设备（请先选择品牌，或该品牌下无设备）
                </p>
              </div>
            </div>
          </div>

          <!-- 底部操作 -->
          <div class="mt-6 flex flex-wrap items-center justify-between gap-3 border-t border-[var(--color-border)] pt-4">
            <span class="text-xs text-[var(--color-text-tertiary)]">
              保存后 device_ids 将以 JSON 数组写入
            </span>
            <div class="flex gap-2">
              <button type="button" class="btn-secondary" @click="onClose">取消</button>
              <button type="button" class="btn-primary" :disabled="saving || !canSave" @click="onSave">
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
  record: { type: Object, default: null },
  brands: { type: Array, default: () => [] },
  devices: { type: Array, default: () => [] },
})
const emit = defineEmits(['close', 'saved'])

const { push } = useToast()
const { createRecord, updateRecord } = useAdminApi()

const form = ref({})
const poolSearch = ref('')
const saving = ref(false)

const isEdit = computed(() => form.value.id !== undefined && form.value.id !== null)

/** 当前选中的设备 id 集合（保序） */
const selectedIds = computed(() => form.value.device_ids || [])

/** 已选设备对应的详情对象（按 device id 有序） */
const orderedDevices = computed(() => {
  const map = new Map(props.devices.map((d) => [d.id, d]))
  return selectedIds.value
    .map((id) => map.get(id))
    .filter((d) => Boolean(d))
})

/** 候选池：按当前品牌过滤 + 搜索关键词过滤 */
const poolDevices = computed(() => {
  const k = poolSearch.value.trim().toLowerCase()
  return props.devices
    .filter((d) => {
      if (form.value.brand && !(d.brandKeys || []).includes(form.value.brand)) return false
      if (k) {
        return d.device.toLowerCase().includes(k) || (d.name && d.name.toLowerCase().includes(k))
      }
      return true
    })
    .sort((a, b) => {
      const aSel = selectedIds.value.includes(a.id)
      const bSel = selectedIds.value.includes(b.id)
      if (aSel !== bSel) return aSel ? 1 : -1
      return a.device.localeCompare(b.device)
    })
})

const canSave = computed(() => form.value.brand && (form.value.name_zh || form.value.name_en))

/** 根据 record 初始化表单 */
const initForm = () => {
  const r = props.record || {}
  form.value = {
    id: r.id !== undefined ? Number(r.id) : undefined,
    brand: r.brand || '',
    name_zh: r.name_zh || '',
    name_en: r.name_en || '',
    sort_order: r.sort_order !== undefined ? Number(r.sort_order) : 0,
    device_ids: Array.isArray(r.device_ids) ? r.device_ids.map((x) => Number(x)) : [],
  }
  poolSearch.value = ''
}

watch(
  () => props.open,
  (open) => {
    if (open) initForm()
  },
)

const addDevice = (d) => {
  if (selectedIds.value.includes(d.id)) return
  const next = [...selectedIds.value, d.id]
  form.value.device_ids = next
}

const removeAt = (idx) => {
  const next = [...selectedIds.value]
  next.splice(idx, 1)
  form.value.device_ids = next
}

const move = (idx, dir) => {
  const next = [...selectedIds.value]
  const target = idx + dir
  if (target < 0 || target >= next.length) return
  const tmp = next[idx]
  next[idx] = next[target]
  next[target] = tmp
  form.value.device_ids = next
}

const onSave = async () => {
  if (!canSave.value) return
  saving.value = true
  try {
    const payload = {
      brand: form.value.brand,
      name_zh: form.value.name_zh,
      name_en: form.value.name_en,
      sort_order: Number(form.value.sort_order || 0),
      device_ids: JSON.stringify(selectedIds.value),
    }
    if (isEdit.value) {
      await updateRecord('series', form.value.id, payload)
      push('success', `系列 #${form.value.id} 更新成功`)
    } else {
      const res = await createRecord('series', payload)
      push('success', `新系列已创建（id=${res.id}）`)
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

const onKeydown = (e) => {
  if (e.key === 'Escape' && props.open) onClose()
}
onMounted(() => window.addEventListener('keydown', onKeydown))
onBeforeUnmount(() => window.removeEventListener('keydown', onKeydown))
</script>
