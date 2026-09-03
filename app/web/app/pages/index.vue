<template>
  <div class="container-page">
    <!-- Hero -->
    <section class="pb-14 pt-16 text-center sm:pb-16 sm:pt-24">
      <h1 class="text-center">
        <img
          :src="logoUrl"
          :alt="$t('site')"
          class="mx-auto h-24 w-auto sm:h-32"
          width="708"
          height="340"
        />
      </h1>
      <p class="mt-3 text-base text-[var(--color-text-secondary)]">
        {{ $t('devicesSub') }}
      </p>
    </section>

    <!-- Stats -->
    <section v-if="statistics" class="mx-auto mb-14 grid max-w-3xl grid-cols-2 gap-y-8 py-2 sm:grid-cols-4" aria-label="Statistics">
      <div class="text-center">
        <div class="text-3xl font-semibold tabular-nums">{{ totalDevices }}</div>
        <div class="mt-1 text-sm text-[var(--color-text-secondary)]">{{ $t('devices') }}</div>
      </div>
      <div class="text-center">
        <div class="text-3xl font-semibold tabular-nums">{{ totalBranches }}</div>
        <div class="mt-1 text-sm text-[var(--color-text-secondary)]">{{ $t('branches') }}</div>
      </div>
      <div class="text-center">
        <div class="text-3xl font-semibold tabular-nums">{{ totalRoms }}</div>
        <div class="mt-1 text-sm text-[var(--color-text-secondary)]">ROMs</div>
      </div>
      <div class="text-center">
        <div class="text-3xl font-semibold tabular-nums">{{ todayNewRoms }}</div>
        <div class="mt-1 text-sm text-[var(--color-text-secondary)]">{{ $t('todayNew') }}</div>
      </div>
    </section>

    <!-- Feedback -->
    <section class="mx-auto mb-10 flex flex-col items-center gap-3" :aria-label="$t('feedback')">
      <p class="text-center text-sm text-[var(--color-text-secondary)]">
        {{ $t('feedbackNote') }}
      </p>
      <div class="flex flex-wrap items-center justify-center gap-3">
        <a
          href="https://github.com/HegeKen/hub.miuier.com/issues"
          target="_blank"
          rel="noopener noreferrer"
          class="inline-flex items-center gap-2 rounded-lg border border-[var(--color-border)] bg-[var(--color-bg-surface)] px-4 py-2 text-sm font-medium text-[var(--color-text)] transition-colors hover:border-[var(--color-border-strong)] hover:bg-[var(--color-bg-subtle)]"
        >
          <svg viewBox="0 0 16 16" fill="currentColor" class="h-4 w-4" aria-hidden="true">
            <path d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27s1.36.09 2 .27c1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.01 8.01 0 0 0 16 8c0-4.42-3.58-8-8-8Z"/>
          </svg>
          {{ $t('feedbackIssues') }}
        </a>
        <a
          href="mailto:hegeken@foxmail.com"
          class="inline-flex items-center gap-2 rounded-lg border border-[var(--color-border)] bg-[var(--color-bg-surface)] px-4 py-2 text-sm font-medium text-[var(--color-text)] transition-colors hover:border-[var(--color-border-strong)] hover:bg-[var(--color-bg-subtle)]"
        >
          <svg viewBox="0 0 20 20" fill="currentColor" class="h-4 w-4" aria-hidden="true">
            <path d="M3 4a2 2 0 0 0-2 2v1.161l8.441 4.221a1.25 1.25 0 0 0 1.118 0L19 7.162V6a2 2 0 0 0-2-2H3Z"/>
            <path d="M19 8.839l-7.77 3.885a2.75 2.75 0 0 1-2.46 0L1 8.839V14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V8.839Z"/>
          </svg>
          {{ $t('feedbackEmail') }}
        </a>
      </div>
    </section>

    <!-- Recent summary -->
    <section v-if="stats" class="mx-auto mb-10 max-w-xl text-center">
      <p class="text-sm text-[var(--color-text-secondary)]">
        {{ $t('recent7d', { count: stats.recentRoms.toLocaleString() }) }}
      </p>
    </section>

    <!-- Recent 7-day ROM updates -->
      <div class="rounded-xl border border-[var(--color-border)] bg-[var(--color-bg-surface)]"v-if="stats?.recent?.length" aria-label="Recent updates">
        <div class="flex items-center justify-between gap-3 border-b border-[var(--color-border)] px-5 py-3.5">
          <div class="min-w-0">
            <h2 class="text-sm font-semibold text-[var(--color-text)]">{{ $t('recent7dList') }}</h2>
            <p class="mt-0.5 text-xs tabular-nums text-[var(--color-text-tertiary)]">
              {{ $t('statsGeneratedAt') }} {{ generatedAtText }}
            </p>
          </div>
          <span class="shrink-0 rounded-full bg-[var(--color-accent-soft)] px-2.5 py-0.5 text-xs font-semibold tabular-nums text-[var(--color-accent)]">
            {{ stats.recentRoms.toLocaleString() }}
          </span>
        </div>
        <div>
          <!-- Mobile: compact list -->
          <div class="sm:hidden divide-y divide-[var(--color-border)]">
            <NuxtLink
              v-for="item in stats.recent"
              :key="item.device + ':' + item.version"
              :to="'/' + locale + '/devices/' + item.device + '#rom-' + item.version"
              class="flex items-center justify-between gap-3 px-4 py-3 transition-colors hover:bg-[var(--color-bg-subtle)]"
            >
              <div class="min-w-0">
                <div class="font-mono text-xs text-[var(--color-text)]">{{ item.version }}</div>
                <div class="mt-0.5 text-xs text-[var(--color-text-tertiary)]">
                  {{ item.name?.[localeKey] || item.name?.en || item.device }} ({{ item.device }})
                </div>
              </div>
              <span class="shrink-0 text-xs tabular-nums text-[var(--color-text-secondary)]">{{ item.release }}</span>
            </NuxtLink>
          </div>

          <!-- Desktop: full table -->
          <table class="hidden w-full text-sm sm:table">
            <thead>
              <tr class="border-b border-[var(--color-border)] text-left text-xs text-[var(--color-text-tertiary)]">
                <th class="px-5 py-2.5 font-medium">{{ $t('device') }}</th>
                <th class="px-5 py-2.5 font-medium">{{ $t('version') }}</th>
                <th class="px-5 py-2.5 font-medium">{{ $t('region') }}</th>
                <th class="px-5 py-2.5 font-medium">{{ $t('release') }}</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-[var(--color-border)]">
              <tr
                v-for="item in stats.recent"
                :key="item.device + ':' + item.version"
                class="transition-colors hover:bg-[var(--color-bg-subtle)]"
              >
                <td class="px-5 py-2.5">
                  <NuxtLink :to="'/' + locale + '/devices/' + item.device" class="group block">
                    <span class="font-medium text-[var(--color-text)] group-hover:text-[var(--color-accent)]">
                      {{ item.name?.[localeKey] || item.name?.en || item.device }}
                    </span>
                    <span class="block font-mono text-xs text-[var(--color-text-tertiary)]">{{ item.device }}</span>
                  </NuxtLink>
                </td>
                <td class="px-5 py-2.5 font-mono text-xs">
                  <NuxtLink
                    :to="'/' + locale + '/devices/' + item.device + '#rom-' + item.version"
                    class="text-[var(--color-text)] hover:text-[var(--color-accent)] hover:underline"
                  >
                    {{ item.version }}
                  </NuxtLink>
                </td>
                <td class="px-5 py-2.5 text-[var(--color-text-secondary)]">
                  <span class="rounded border border-[var(--color-border)] px-1.5 py-0.5 text-xs font-medium uppercase">
                    {{ item.region || '—' }}
                  </span>
                </td>
                <td class="px-5 py-2.5 tabular-nums text-[var(--color-text-secondary)]">{{ item.release }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
  </div>
</template>

<script setup>
import logoUrl from '~/assets/images/words.svg'

const { locale } = useI18n()
const { t } = useI18n()
const { buildStatsUrl, buildStatisticsUrl } = useApi()

// 近 7 日更新的 ROM 版本列表（由 generate-index.mjs 生成 v3/stats.json）
// cache: 'no-store' 绕过浏览器对旧版 stats.json 的缓存
const { data: stats } = await useAsyncData(
  'devices-stats',
  () => $fetch(buildStatsUrl(), { cache: 'no-store' }).catch(() => null)
)

// 顶部统计卡片数据来自数据库真实统计（v3/statistics.json，口径与后台一致）
const { data: statistics } = await useAsyncData(
  'devices-statistics',
  () => $fetch(buildStatisticsUrl(), { cache: 'no-store' }).catch(() => null)
)

const totalDevices = computed(() => statistics.value?.deviceCount || 0)
const totalBranches = computed(() => statistics.value?.branchCount || 0)
const totalRoms = computed(() => statistics.value?.romCount || 0)
const todayNewRoms = computed(() => statistics.value?.todayNewRoms || 0)

// stats.json 的生成时间，格式化为本地时区 YYYY-MM-DD HH:mm:ss
const generatedAtText = computed(() => {
  const ts = stats.value?.generatedAt
  if (!ts) return ''
  const d = new Date(ts)
  if (Number.isNaN(d.getTime())) return ''
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
})

useHead({
  title: `${t('site')} - ${t('devicesSub')}`,
})
</script>
