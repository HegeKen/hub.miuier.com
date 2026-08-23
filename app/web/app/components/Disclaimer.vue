<template>
  <p class="pb-3 text-center text-xs leading-relaxed text-[var(--color-text-tertiary)] sm:pb-6">
    <ClientOnly>
      <template v-if="$device.isDesktop">
        {{ $t('dispc') }}
      </template>
      <template v-else>
        {{ $t('dismob') }}
      </template>
    </ClientOnly>
  </p>
</template>
