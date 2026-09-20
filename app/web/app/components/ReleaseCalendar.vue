<template>
  <div ref="rootRef" class="relative">
    <button
      type="button"
      class="input-base flex w-auto items-center gap-2 text-start"
      :aria-label="$t('dateQuery')"
      aria-haspopup="dialog"
      :aria-expanded="open"
      @click="toggle"
    >
      <svg class="h-4 w-4 shrink-0 text-[var(--color-text-tertiary)]" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" aria-hidden="true">
        <path stroke-linecap="round" stroke-linejoin="round" d="M6.75 3v2.25M17.25 3v2.25M3 18.75V7.5a2.25 2.25 0 0 1 2.25-2.25h13.5A2.25 2.25 0 0 1 21 7.5v11.25m-18 0A2.25 2.25 0 0 0 5.25 21h13.5A2.25 2.25 0 0 0 21 18.75m-18 0v-7.5A2.25 2.25 0 0 1 5.25 9h13.5A2.25 2.25 0 0 1 21 11.25v7.5" />
      </svg>
      <span class="tabular-nums">{{ modelValue || placeholder }}</span>
    </button>

    <!-- 年月份片由父组件按需加载：本组件只负责日历 UI 与禁用逻辑 -->
    <div
      v-if="open"
      class="absolute start-0 top-full z-30 mt-2 w-[19rem] rounded-xl border border-[var(--color-border)] bg-[var(--color-bg-surface)] p-3 shadow-xl"
      role="dialog"
      @keydown.esc.stop="close"
    >
      <div class="mb-2 flex items-center justify-between gap-2">
        <button
          type="button"
          class="rounded-md p-1.5 text-[var(--color-text-secondary)] transition-colors hover:bg-[var(--color-bg-subtle)] hover:text-[var(--color-text)] disabled:cursor-not-allowed disabled:opacity-40 disabled:hover:bg-transparent"
          :aria-label="$t('prev')"
          :disabled="!canPrev"
          @click="shift(-1)"
        >
          <svg class="dir-flip h-4 w-4" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" aria-hidden="true">
            <path stroke-linecap="round" stroke-linejoin="round" d="M15.75 19.5 8.25 12l7.5-7.5" />
          </svg>
        </button>

        <!-- 点击标题在「日」/「月」两种视图间切换，月视图下可整年跳转 -->
        <button
          type="button"
          class="flex items-center gap-1.5 rounded-md px-2 py-1 text-sm font-medium text-[var(--color-text)] transition-colors hover:bg-[var(--color-bg-subtle)]"
          :aria-label="viewMode === 'day' ? $t('pickMonth') : $t('pickDate')"
          @click="toggleMode"
        >
          <span>{{ headerTitle }}</span>
          <svg
            class="h-3.5 w-3.5 shrink-0 text-[var(--color-text-tertiary)] transition-transform"
            :class="viewMode === 'month' ? 'rotate-180' : ''"
            xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" aria-hidden="true"
          >
            <path stroke-linecap="round" stroke-linejoin="round" d="m19.5 8.25-7.5 7.5-7.5-7.5" />
          </svg>
          <span v-if="loading" class="spinner !h-3.5 !w-3.5" role="status"></span>
        </button>

        <button
          type="button"
          class="rounded-md p-1.5 text-[var(--color-text-secondary)] transition-colors hover:bg-[var(--color-bg-subtle)] hover:text-[var(--color-text)] disabled:cursor-not-allowed disabled:opacity-40 disabled:hover:bg-transparent"
          :aria-label="$t('next')"
          :disabled="!canNext"
          @click="shift(1)"
        >
          <svg class="dir-flip h-4 w-4" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" aria-hidden="true">
            <path stroke-linecap="round" stroke-linejoin="round" d="m8.25 4.5 7.5 7.5-7.5 7.5" />
          </svg>
        </button>
      </div>

      <template v-if="viewMode === 'day'">
        <div class="grid grid-cols-7 gap-0.5 text-center text-xs text-[var(--color-text-tertiary)]">
          <span v-for="(w, i) in weekdays" :key="i" class="py-1">{{ w }}</span>
        </div>

        <div class="grid grid-cols-7 gap-0.5">
          <template v-for="(cell, i) in cells" :key="i">
            <span v-if="!cell" aria-hidden="true"></span>
            <button
              v-else
              type="button"
              class="inline-flex h-9 items-center justify-center rounded-md text-sm tabular-nums transition-colors"
              :class="dayClass(cell)"
              :disabled="!cell.enabled"
              :aria-current="cell.key === modelValue ? 'date' : undefined"
              @click="select(cell)"
            >
              {{ cell.d }}
            </button>
          </template>
        </div>
      </template>

      <!-- 月视图：整年 12 个月一览，无更新的月份同样不可选 -->
      <div v-else class="grid grid-cols-3 gap-1">
        <button
          v-for="m in months"
          :key="m.index"
          type="button"
          class="inline-flex h-9 items-center justify-center rounded-md text-sm transition-colors"
          :class="monthClass(m)"
          :disabled="!m.enabled"
          :aria-current="isCurrentMonth(m.index) && m.enabled ? 'true' : undefined"
          @click="selectMonth(m.index)"
        >
          {{ m.label }}
        </button>
      </div>

      <div class="mt-2 border-t border-[var(--color-border)] pt-2">
        <button
          type="button"
          class="rounded-md px-2 py-1 text-xs font-medium text-[var(--color-text-secondary)] transition-colors hover:bg-[var(--color-bg-subtle)] hover:text-[var(--color-text)]"
          @click="emitToday"
        >
          {{ $t('today') }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
const props = defineProps({
  modelValue: { type: String, default: '' },
  min: { type: String, default: '' },
  max: { type: String, default: '' },
  // 当前可见年份里有 ROM 更新的日期集合（Set<string>）；null 表示年份分片尚未就绪
  validDates: { type: Set, default: null },
  // validDates 对应的年份；与当前视图年份不一致时视为尚未就绪，避免用错年份的数据做禁用
  validYear: { type: String, default: '' },
  loading: { type: Boolean, default: false },
  placeholder: { type: String, default: '—' },
})

const emit = defineEmits(['update:modelValue', 'year-change', 'today'])

const { locale } = useI18n()

const rootRef = ref(null)
const open = ref(false)
const viewMode = ref('day')          // 'day' 日历视图 | 'month' 年份月份视图
const view = ref({ y: 0, m: 0 })

const pad = (n) => String(n).padStart(2, '0')
const toKey = (y, m, d) => `${y}-${pad(m + 1)}-${pad(d)}`

const parseKey = (key) => {
  const m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(key || '')
  return m ? { y: Number(m[1]), m: Number(m[2]) - 1 } : null
}

const daysInMonth = (y, m) => new Date(Date.UTC(y, m + 1, 0)).getUTCDate()

// 用 UTC 构造避免本地时区把日期整体偏移一天
const monthTitle = computed(() =>
  new Intl.DateTimeFormat(locale.value, { year: 'numeric', month: 'long', timeZone: 'UTC' })
    .format(new Date(Date.UTC(view.value.y, view.value.m, 1)))
)

const yearTitle = computed(() =>
  new Intl.DateTimeFormat(locale.value, { year: 'numeric', timeZone: 'UTC' })
    .format(new Date(Date.UTC(view.value.y, 0, 1)))
)

const headerTitle = computed(() => (viewMode.value === 'day' ? monthTitle.value : yearTitle.value))

// 2024-01-01 是周一，以此为基准取「周一起始」的星期短名，顺序与下面的网格一致
const weekdays = computed(() => {
  const fmt = new Intl.DateTimeFormat(locale.value, { weekday: 'narrow', timeZone: 'UTC' })
  const base = Date.UTC(2024, 0, 1)
  return Array.from({ length: 7 }, (_, i) => fmt.format(new Date(base + i * 86400000)))
})

// 仅当分片年份与视图年份一致时才用它判断有无数据，否则只按 min/max 约束
const yearSet = computed(() =>
  props.validDates && props.validYear === String(view.value.y) ? props.validDates : null
)

const isEnabled = (key) => {
  if (props.min && key < props.min) return false
  if (props.max && key > props.max) return false
  // 分片未就绪时先不禁用，选到无数据的日期会由结果区提示
  return yearSet.value ? yearSet.value.has(key) : true
}

const cells = computed(() => {
  const { y, m } = view.value
  const offset = (new Date(Date.UTC(y, m, 1)).getUTCDay() + 6) % 7
  const total = daysInMonth(y, m)
  const list = Array.from({ length: offset }, () => null)
  for (let d = 1; d <= total; d++) {
    const key = toKey(y, m, d)
    list.push({ key, d, enabled: isEnabled(key) })
  }
  return list
})

const months = computed(() => {
  const y = view.value.y
  const fmt = new Intl.DateTimeFormat(locale.value, { month: 'short', timeZone: 'UTC' })
  return Array.from({ length: 12 }, (_, index) => ({
    index,
    label: fmt.format(new Date(Date.UTC(y, index, 1))),
    enabled: monthHasData(y, index),
  }))
})

function monthHasData(y, index) {
  const first = toKey(y, index, 1)
  const last = toKey(y, index, daysInMonth(y, index))
  if (props.min && last < props.min) return false
  if (props.max && first > props.max) return false
  const set = yearSet.value
  if (!set) return true
  const prefix = `${y}-${pad(index + 1)}-`
  for (const key of set) {
    if (key.startsWith(prefix)) return true
  }
  return false
}

const canPrev = computed(() => {
  const min = parseKey(props.min)
  if (!min) return true
  return view.value.y > min.y || (view.value.y === min.y && view.value.m > min.m)
})

const canNext = computed(() => {
  const max = parseKey(props.max)
  if (!max) return true
  return view.value.y < max.y || (view.value.y === max.y && view.value.m < max.m)
})

const dayClass = (cell) => {
  if (!cell.enabled) return 'text-[var(--color-text-tertiary)] cursor-not-allowed'
  if (cell.key === props.modelValue) return 'bg-[var(--color-accent)] text-white hover:bg-[var(--color-accent-hover)]'
  return 'text-[var(--color-text)] hover:bg-[var(--color-bg-subtle)]'
}

const monthClass = (m) => {
  if (!m.enabled) return 'text-[var(--color-text-tertiary)] cursor-not-allowed'
  if (isCurrentMonth(m.index)) return 'bg-[var(--color-accent)] text-white hover:bg-[var(--color-accent-hover)]'
  return 'text-[var(--color-text)] hover:bg-[var(--color-bg-subtle)]'
}

// 仅当已选日期的年份就是当前视图年份时，才把对应月份标为当前，避免翻到别的年份仍高亮
const isCurrentMonth = (index) => {
  const sel = parseKey(props.modelValue)
  return !!sel && sel.y === view.value.y && sel.m === index
}

// 日视图下翻月、月视图下翻年
const shift = (step) => {
  const { y, m } = view.value
  if (viewMode.value === 'day') {
    const next = new Date(Date.UTC(y, m + step, 1))
    view.value = { y: next.getUTCFullYear(), m: next.getUTCMonth() }
  } else {
    view.value = { y: y + step, m }
  }
}

const toggleMode = () => {
  viewMode.value = viewMode.value === 'day' ? 'month' : 'day'
}

const select = (cell) => {
  if (!cell.enabled) return
  emit('update:modelValue', cell.key)
  open.value = false
}

const selectMonth = (index) => {
  view.value = { y: view.value.y, m: index }
  viewMode.value = 'day'
}

// 「今天」由父组件决定最终选中哪天（可能要退回到不晚于今天且有更新的日期）
const emitToday = () => {
  emit('today')
  close()
}

const toggle = () => {
  open.value = !open.value
}

const close = () => {
  open.value = false
  viewMode.value = 'day'
}

// 视图年月变化时通知父组件加载对应年份分片
watch(
  () => [open.value, view.value.y],
  ([isOpen, year]) => {
    if (isOpen) emit('year-change', String(year))
  }
)

// 已选日期决定初始视图；未选时回退到数据里最新的一天
watch(
  () => props.modelValue,
  (value) => {
    const parsed = parseKey(value) || parseKey(props.max) || parseKey(props.min)
    if (parsed) view.value = { y: parsed.y, m: parsed.m }
  },
  { immediate: true }
)

const onDocumentMousedown = (e) => {
  if (!open.value) return
  if (rootRef.value && !rootRef.value.contains(e.target)) close()
}

onMounted(() => document.addEventListener('mousedown', onDocumentMousedown))
onUnmounted(() => document.removeEventListener('mousedown', onDocumentMousedown))
</script>
