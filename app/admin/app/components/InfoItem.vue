<template>
  <div>
    <p class="form-label !mb-1">
      {{ label }}
      <span v-if="desc" class="ml-1 text-xs font-normal text-[var(--color-text-tertiary)]">{{ desc }}</span>
    </p>
    <p class="font-mono text-xs text-[var(--color-text)]">{{ value ?? '—' }}</p>
  </div>
</template>

<script setup>
defineProps({
  label: { type: String, required: true },
  value: { type: [String, Number], default: null },
  desc: { type: String, default: '' },
})
</script>
