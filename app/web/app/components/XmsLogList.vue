<template>
  <ul v-if="logs && logs.length" class="space-y-1.5">
    <li
      v-for="(item, index) in logs"
      :key="index"
      class="flex gap-2.5 text-sm leading-relaxed text-[var(--color-text-secondary)]"
    >
      <span class="mt-2 h-1 w-1 shrink-0 rounded-full bg-[var(--color-border-strong)]" aria-hidden="true" />
      <span>{{ pickXmsText(item, locale) }}</span>
    </li>
  </ul>
</template>

<script setup>
import { pickXmsText } from '~/utils/xms'

defineProps({
  /** [{ zh, en }, ...]：更新日志条目 */
  logs: { type: Array, default: () => [] },
})

const { locale } = useI18n()
</script>
