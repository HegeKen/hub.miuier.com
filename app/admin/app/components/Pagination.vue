<template>
  <div class="flex flex-wrap items-center justify-between gap-3 border-t border-[var(--color-border)] px-4 py-3">
    <p class="text-xs text-[var(--color-text-tertiary)]">
      共 <span class="font-medium text-[var(--color-text-secondary)]">{{ total.toLocaleString() }}</span> 条记录
      <span class="mx-1">·</span>
      第 {{ page }} / {{ totalPages }} 页
    </p>

    <div class="flex items-center gap-1">
      <button
        type="button"
        class="btn-secondary !px-2.5 !py-1.5 text-xs"
        :disabled="page <= 1 || loading"
        @click="$emit('change', page - 1)"
      >
        上一页
      </button>
      <button
        type="button"
        class="btn-secondary !px-2.5 !py-1.5 text-xs"
        :disabled="page >= totalPages || loading"
        @click="$emit('change', page + 1)"
      >
        下一页
      </button>
    </div>
  </div>
</template>

<script setup>
const props = defineProps({
  page: { type: Number, required: true },
  pageSize: { type: Number, required: true },
  total: { type: Number, required: true },
  loading: { type: Boolean, default: false },
})
defineEmits(['change'])

const totalPages = computed(() => Math.max(1, Math.ceil(props.total / props.pageSize)))
</script>
