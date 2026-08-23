<template>
  <footer :aria-label="$t('footernav')">
    <div class="container-page pb-6 pt-4 sm:py-10">
      <Disclaimer />
      <div class="flex flex-col items-center gap-2 text-sm text-[var(--color-text-tertiary)] sm:flex-row sm:justify-between sm:gap-3 sm:border-t sm:border-[var(--color-border)] sm:pt-6">
        <p>&copy; 2019 - {{ new Date().getFullYear() }} MiROMS HUB.</p> 
        <div class="flex items-center gap-5">
          <a
            href="https://github.com/HegeKen/hub.miuier.com"
            target="_blank"
            rel="noopener noreferrer"
            class="transition-colors hover:text-[var(--color-text)]"
          >
            GitHub
          </a>
          <a href="#top" class="transition-colors hover:text-[var(--color-text)]">
            {{ $t('totop') }}
          </a>
        </div>
      </div>
    </div>
  </footer>
</template>
