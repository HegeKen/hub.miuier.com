<template>
  <Teleport to="body">
    <div class="pointer-events-none fixed right-4 top-4 z-[100] flex w-80 max-w-[calc(100vw-2rem)] flex-col gap-2">
      <TransitionGroup
        enter-active-class="transition duration-200 ease-out"
        enter-from-class="opacity-0 -translate-x-2"
        enter-to-class="opacity-100 translate-x-0"
        leave-active-class="transition duration-150 ease-in"
        leave-from-class="opacity-100 translate-x-0"
        leave-to-class="opacity-0 -translate-x-2"
      >
        <div
          v-for="toast in toasts"
          :key="toast.id"
          class="pointer-events-auto flex items-start gap-2.5 rounded-lg border bg-[var(--color-bg-surface)] p-3 text-sm shadow-lg"
          :class="borderClass(toast.type)"
          role="status"
        >
          <span class="mt-0.5 shrink-0" :class="dotClass(toast.type)" aria-hidden="true"></span>
          <span class="min-w-0 break-words text-[var(--color-text)]">{{ toast.message }}</span>
          <button
            type="button"
            class="ml-auto shrink-0 text-[var(--color-text-tertiary)] transition-colors hover:text-[var(--color-text)]"
            :aria-label="'关闭通知'"
            @click="dismiss(toast.id)"
          >
            <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" aria-hidden="true">
              <path stroke-linecap="round" stroke-linejoin="round" d="M6 18 18 6M6 6l12 12" />
            </svg>
          </button>
        </div>
      </TransitionGroup>
    </div>
  </Teleport>
</template>

<script setup>
const { toasts, dismiss } = useToast()

const colorMap = {
  success: 'var(--color-ok)',
  error: 'var(--color-danger)',
  info: 'var(--color-info)',
}

const borderClass = (type) => ({ 'border-[var(--color-border)]': true })
const dotClass = (type) => ({ 'dot': true, 'bg-[var(--color-ok)]': type === 'success', 'bg-[var(--color-danger)]': type === 'error', 'bg-[var(--color-info)]': type === 'info' })
</script>
