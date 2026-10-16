<template>
  <div class="container-page py-10 sm:py-14">
    <header class="mb-8">
      <h1 class="text-2xl font-semibold tracking-tight sm:text-3xl">{{ $t('tools') }}</h1>
      <p class="mt-2 text-sm text-[var(--color-text-secondary)]">{{ $t('toolsSub') }}</p>
    </header>

    <!-- Loading -->
    <div v-if="pending" class="flex justify-center py-20">
      <span class="spinner" role="status" aria-label="Loading"></span>
    </div>

    <!-- Error -->
    <div v-else-if="error" class="py-20 text-center">
      <p class="text-sm text-[var(--color-text-secondary)]">{{ $t('loadFailed') }}</p>
      <button type="button" class="pill mt-4" @click="refresh()">{{ $t('retry') }}</button>
    </div>

    <!-- 每个工具一块，块内按版本列出下载包（数据见 data/api/v3/tools.json） -->
    <template v-else>
      <section v-for="tool in tools" :key="tool.tool" class="mb-10">
        <div class="mb-3 flex flex-wrap items-baseline gap-x-3">
          <h2 class="text-lg font-semibold text-[var(--color-text)]">{{ pick(tool.name) }}</h2>
          <span class="text-xs text-[var(--color-text-tertiary)]">
            {{ $t('toolVersions', { count: tool.packages.length }) }}
          </span>
        </div>

        <div class="overflow-hidden rounded-xl border border-[var(--color-border)]">
          <div
            v-for="(pkg, index) in tool.packages"
            :key="pkg.version + '-' + index"
            class="border-b border-[var(--color-border)] bg-[var(--color-bg-surface)] px-4 py-3 last:border-b-0"
          >
            <div class="flex flex-wrap items-center gap-x-3 gap-y-1.5">
              <span class="font-mono text-sm font-semibold text-[var(--color-text)]">{{ pkg.version || '—' }}</span>
              <span v-if="pkg.date" class="text-xs text-[var(--color-text-tertiary)]">{{ pkg.date }}</span>
              <span class="grow" />
              <a
                v-if="pkg.url"
                :href="pkg.url"
                class="pill"
                target="_blank"
                rel="noopener noreferrer"
              >
                {{ $t('download') }}
                <span v-if="pkg.filename" class="font-mono text-xs text-[var(--color-text-tertiary)]">{{ pkg.filename }}</span>
              </a>
              <span v-else-if="pkg.filename" class="font-mono text-xs text-[var(--color-text-tertiary)]">{{ pkg.filename }}</span>
            </div>

            <!-- 注意事项 / 更新内容按需展开，避免长列表挤占版面 -->
            <details
              v-if="pkg.attentions?.length || pkg.logs?.length"
              class="mt-2"
            >
              <summary class="cursor-pointer text-xs text-[var(--color-text-secondary)]">
                {{ pkg.attentions?.length ? $t('attentions') : '' }}
                <template v-if="pkg.attentions?.length && pkg.logs?.length"> · </template>
                {{ pkg.logs?.length ? $t('logs') : '' }}
              </summary>
              <div v-if="pkg.attentions?.length" class="mt-2">
                <p class="mb-1 text-xs font-medium text-[var(--color-text-secondary)]">{{ $t('attentions') }}</p>
                <ul class="space-y-1">
                  <li
                    v-for="(item, i) in pkg.attentions"
                    :key="'a' + i"
                    class="flex gap-2.5 text-xs leading-relaxed text-[var(--color-text-secondary)]"
                  >
                    <span class="mt-[7px] h-1 w-1 shrink-0 rounded-full bg-[var(--color-border-strong)]" aria-hidden="true" />
                    <span>{{ pick(item) }}</span>
                  </li>
                </ul>
              </div>
              <div v-if="pkg.logs?.length" class="mt-2">
                <p class="mb-1 text-xs font-medium text-[var(--color-text-secondary)]">{{ $t('logs') }}</p>
                <ul class="space-y-1">
                  <li
                    v-for="(item, i) in pkg.logs"
                    :key="'l' + i"
                    class="flex gap-2.5 text-xs leading-relaxed text-[var(--color-text-secondary)]"
                  >
                    <span class="mt-[7px] h-1 w-1 shrink-0 rounded-full bg-[var(--color-border-strong)]" aria-hidden="true" />
                    <span>{{ pick(item) }}</span>
                  </li>
                </ul>
              </div>
            </details>
          </div>
        </div>
      </section>
    </template>
  </div>
</template>

<script setup>
const { locale, t } = useI18n()
const { buildToolsUrl } = useApi()

// 数据里只有 zh / en 两列：中文（含 zh-tw）用 zh，其余用 en 兜底 zh
const lang = computed(() => (locale.value.startsWith('zh') ? 'zh' : 'en'))
const pick = (item) => item?.[lang.value] || item?.zh || item?.en || ''

const { data, pending, error, refresh } = await useAsyncData(
  'tools',
  () => $fetch(buildToolsUrl())
)

const tools = computed(() => data.value?.tools || [])

useHead({
  title: computed(() => `${t('tools')} - ${t('site')}`),
})
</script>
