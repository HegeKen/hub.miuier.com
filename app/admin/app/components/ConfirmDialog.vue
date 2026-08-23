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
      <div v-if="open" class="fixed inset-0 z-[95] flex items-center justify-center p-4" role="dialog" aria-modal="true" :aria-label="title">
        <div class="absolute inset-0 bg-black/40 backdrop-blur-sm" @click="$emit('cancel')"></div>
        <div class="relative w-full max-w-md rounded-xl border border-[var(--color-border)] bg-[var(--color-bg-surface)] p-5 shadow-xl">
          <h3 class="text-base font-semibold text-[var(--color-text)]">{{ title }}</h3>
          <p class="mt-2 text-sm leading-relaxed text-[var(--color-text-secondary)]">{{ message }}</p>

          <div class="mt-5 flex justify-end gap-2">
            <button type="button" class="btn-secondary" @click="$emit('cancel')">取消</button>
            <button type="button" class="btn-danger" :disabled="loading" @click="$emit('confirm')">
              {{ loading ? '处理中…' : '确认删除' }}
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
defineProps({
  open: { type: Boolean, default: false },
  title: { type: String, default: '确认操作' },
  message: { type: String, default: '' },
  loading: { type: Boolean, default: false },
})
defineEmits(['confirm', 'cancel'])
</script>
