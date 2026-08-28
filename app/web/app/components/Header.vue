<template>
  <header id="top" class="sticky top-0 z-40 border-b border-[var(--color-border)] bg-[var(--color-bg)]/85 backdrop-blur-md">
    <a href="#main-content" class="skip-link">{{ $t('skiptocontent') }}</a>

    <div class="container-page flex h-14 items-center justify-between gap-4">
      <!-- Logo -->
      <NuxtLink
        :to="'/' + locale"
        class="flex shrink-0 items-center"
        :aria-label="$t('home')"
      >
        <img
          :src="logoUrl"
          :alt="$t('site')"
          class="h-10 w-auto"
          width="708"
          height="340"
        />
      </NuxtLink>

      <!-- Desktop nav -->
      <nav class="hidden items-center gap-1 md:flex" :aria-label="$t('mainnav')">
        <NuxtLink
          v-for="item in navItems"
          :key="item.path"
          :to="item.path"
          class="relative rounded-md px-3 py-1.5 text-sm font-medium transition-colors"
          :class="isCurrent(item.path) ? 'text-[var(--color-text)]' : 'text-[var(--color-text-secondary)] hover:text-[var(--color-text)]'"
          :aria-current="isCurrent(item.path) ? 'page' : undefined"
        >
          {{ $t(item.label) }}
          <span
            v-if="isCurrent(item.path)"
            class="absolute inset-x-3 -bottom-[11px] h-0.5 rounded-full bg-[var(--color-accent)]"
            aria-hidden="true"
          ></span>
        </NuxtLink>
      </nav>

      <!-- Actions -->
      <div class="flex shrink-0 items-center gap-1">
        <DarkModeToggle />
        <LanguageSwitcher />

        <!-- Mobile menu button -->
        <button
          type="button"
          class="flex h-9 w-9 items-center justify-center rounded-md text-[var(--color-text-secondary)] transition-colors hover:bg-[var(--color-bg-subtle)] hover:text-[var(--color-text)] md:hidden"
          :aria-label="mobileMenuOpen ? $t('closemenu') : $t('openmenu')"
          :aria-expanded="mobileMenuOpen"
          @click="mobileMenuOpen = !mobileMenuOpen"
        >
          <svg
            xmlns="http://www.w3.org/2000/svg"
            class="h-5 w-5"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
            stroke-width="1.5"
            aria-hidden="true"
          >
            <path
              v-if="!mobileMenuOpen"
              stroke-linecap="round"
              stroke-linejoin="round"
              d="M3.75 6.75h16.5M3.75 12h16.5M3.75 17.25h16.5"
            />
            <path v-else stroke-linecap="round" stroke-linejoin="round" d="M6 18 18 6M6 6l12 12" />
          </svg>
        </button>
      </div>
    </div>

    <!-- Mobile nav -->
    <Transition
      enter-active-class="transition duration-200 ease-out"
      enter-from-class="opacity-0 -translate-y-1"
      enter-to-class="opacity-100 translate-y-0"
      leave-active-class="transition duration-150 ease-in"
      leave-from-class="opacity-100 translate-y-0"
      leave-to-class="opacity-0 -translate-y-1"
    >
      <nav
        v-if="mobileMenuOpen"
        class="border-t border-[var(--color-border)] bg-[var(--color-bg)] px-4 py-2 md:hidden"
        :aria-label="$t('mainnav')"
      >
        <ul class="space-y-1">
          <li v-for="item in navItems" :key="item.path">
            <NuxtLink
              :to="item.path"
              class="block rounded-md px-3 py-2.5 text-sm font-medium transition-colors"
              :class="isCurrent(item.path) ? 'bg-[var(--color-bg-subtle)] text-[var(--color-text)]' : 'text-[var(--color-text-secondary)]'"
              :aria-current="isCurrent(item.path) ? 'page' : undefined"
              @click="mobileMenuOpen = false"
            >
              {{ $t(item.label) }}
            </NuxtLink>
          </li>
        </ul>
      </nav>
    </Transition>
  </header>
</template>

<script setup>
import logoUrl from '~/assets/images/logo.svg'

const { locale } = useI18n()
const route = useRoute()

const mobileMenuOpen = ref(false)

const navItems = computed(() => {
  const base = '/' + locale.value
  return [
    { label: 'home', path: base },
    { label: 'devices', path: base + '/devices' },
    { label: 'roms', path: base + '/roms' },
  ]
})

const isCurrent = (path) => {
  const current = route.path.replace(/\/+$/, '')
  const target = String(path).replace(/\/+$/, '')
  return current === target
}
</script>
