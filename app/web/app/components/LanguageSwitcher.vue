<template>
  <div class="relative">
    <button
      type="button"
      class="flex h-9 items-center gap-1.5 rounded-md px-2.5 text-sm font-medium text-[var(--color-text-secondary)] transition-colors hover:bg-[var(--color-bg-subtle)] hover:text-[var(--color-text)]"
      :aria-label="$t('langswitch')"
      @click="open = !open"
    >
      <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" aria-hidden="true">
        <path stroke-linecap="round" stroke-linejoin="round" d="M10.5 21l5.25-11.25L21 21m-9-3h7.5M3 5.621a48.474 48.474 0 0 1 6-.371m0 0c1.12 0 2.233.038 3.334.114M9 5.25V3m3.334 2.364C11.176 10.658 7.69 15.08 3 17.502m9.334-12.138c.896.061 1.785.147 2.666.257m-4.589 8.495a18.023 18.023 0 0 1-3.827-5.802" />
      </svg>
      <span>{{ shortLabel }}</span>
    </button>
    <Transition
      enter-active-class="transition duration-100 ease-out"
      enter-from-class="opacity-0 scale-95"
      enter-to-class="opacity-100 scale-100"
      leave-active-class="transition duration-75 ease-in"
      leave-from-class="opacity-100 scale-100"
      leave-to-class="opacity-0 scale-95"
    >
      <div
        v-if="open"
        class="absolute right-0 top-full z-50 mt-1.5 w-36 overflow-hidden rounded-lg border border-[var(--color-border)] bg-[var(--color-bg-surface)] py-1 shadow-lg shadow-black/5"
      >
        <NuxtLink
          v-for="loc in availableLocales"
          :key="loc.code"
          :to="switchLocalePath(loc.code)"
          class="block px-3 py-2 text-sm text-[var(--color-text)] transition-colors hover:bg-[var(--color-bg-subtle)]"
          @click="open = false"
        >
          {{ loc.name }}
        </NuxtLink>
      </div>
    </Transition>
  </div>
</template>

<script setup>
const { locale, locales } = useI18n()
const switchLocalePath = useSwitchLocalePath()

const open = ref(false)

const shortLabel = computed(() =>
  String(locale.value).startsWith('zh') ? '中文' : 'EN'
)

const currentLocale = computed(() =>
  locales.value.find((l) => l.code === locale.value)
)

const availableLocales = computed(() => {
  const currentLen = locale.value.length
  return locales.value.filter(
    (l) => l.code !== locale.value && l.code.length === currentLen
  )
})

onMounted(() => {
  document.addEventListener('click', (e) => {
    if (!e.target.closest('.relative')) {
      open.value = false
    }
  })
})
</script>
