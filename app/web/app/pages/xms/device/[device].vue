<template>
  <div class="container-page py-10 sm:py-14">
    <!-- Loading -->
    <div v-if="pending" class="flex justify-center py-20">
      <span class="spinner" role="status" aria-label="Loading"></span>
    </div>

    <!-- Error / empty -->
    <div v-else-if="error || !device" class="py-20 text-center">
      <p class="text-sm text-[var(--color-text-secondary)]">{{ $t('xmsEmpty') }}</p>
      <NuxtLink
        :to="'/' + locale + '/xms'"
        class="mt-3 inline-block text-sm font-medium text-[var(--color-accent)] hover:underline"
      >
        {{ $t('xms') }}
      </NuxtLink>
    </div>

    <template v-else>
      <!-- Breadcrumb -->
      <nav class="mb-6 text-sm" aria-label="Breadcrumb">
        <ol class="flex flex-wrap items-center gap-1.5 text-[var(--color-text-tertiary)]">
          <li>
            <NuxtLink :to="'/' + locale" class="transition-colors hover:text-[var(--color-text)]">
              {{ $t('home') }}
            </NuxtLink>
          </li>
          <li aria-hidden="true">/</li>
          <li>
            <NuxtLink :to="'/' + locale + '/xms'" class="transition-colors hover:text-[var(--color-text)]">
              {{ $t('xms') }}
            </NuxtLink>
          </li>
          <li aria-hidden="true">/</li>
          <li class="truncate font-medium text-[var(--color-text)]">
            {{ pickXmsText(device.name, locale) || device.device }}
          </li>
        </ol>
      </nav>

      <!-- Title -->
      <header class="mb-8">
        <h1 class="text-2xl font-semibold tracking-tight sm:text-3xl">
          {{ pickXmsText(device.name, locale) || device.device }}
        </h1>
        <p class="mt-1.5 flex flex-wrap items-center gap-x-3 gap-y-1 font-mono text-xs text-[var(--color-text-tertiary)]">
          <span>{{ device.device }}</span>
          <span v-if="device.region" class="rounded border border-[var(--color-border)] px-1.5 py-0.5">
            {{ regionName(device.region) }}
          </span>
          <NuxtLink
            :to="'/' + locale + '/devices/' + encodeURIComponent(device.device)"
            class="font-sans font-medium text-[var(--color-accent)] hover:underline"
          >
            {{ $t('devinfo') }}
          </NuxtLink>
        </p>
      </header>

      <!-- 推送列表：最新的展开，历史推送折叠 -->
      <details
        v-for="(push, pushIndex) in device.pushes"
        :key="push.version + '-' + pushIndex"
        :open="pushIndex === 0"
        class="mb-4 overflow-hidden rounded-xl border border-[var(--color-border)]"
      >
        <summary class="flex cursor-pointer list-none flex-wrap items-center gap-x-3 gap-y-1.5 bg-[var(--color-bg-subtle)] px-4 py-3 marker:hidden">
          <span class="rounded border border-[var(--color-border)] bg-[var(--color-bg-surface)] px-2 py-0.5 font-mono text-xs font-semibold text-[var(--color-text)]">
            {{ push.version }}
          </span>
          <span
            v-if="push.os"
            class="rounded-full border border-[var(--color-border)] px-2 py-0.5 text-[11px] text-[var(--color-text-secondary)]"
          >
            {{ push.os }}
          </span>
          <span
            v-for="romVersion in push.romVersions || []"
            :key="romVersion"
            class="rounded border border-[var(--color-border)] bg-[var(--color-bg-surface)] px-1.5 py-0.5 font-mono text-[10px] text-[var(--color-text-secondary)]"
          >
            {{ romVersion }}
          </span>
          <span v-if="push.isLatest" class="rounded-full bg-[var(--color-accent-soft)] px-2 py-0.5 text-[11px] font-medium text-[var(--color-accent)]">
            {{ $t('xmsLatest') }}
          </span>
          <span class="grow" />
          <span class="text-xs tabular-nums text-[var(--color-text-tertiary)]" :title="$t('xmsPushDate')">{{ push.pushDate }}</span>
          <span class="text-xs text-[var(--color-text-tertiary)]">{{ $t('xmsApps', { count: push.pkgCnt }) }}</span>
        </summary>

        <!-- 注意事项 -->
        <details v-if="push.notice" class="border-b border-[var(--color-border)] px-4 py-2.5">
          <summary class="cursor-pointer text-xs text-[var(--color-text-secondary)]">{{ $t('attentions') }}</summary>
          <ul class="mt-2 space-y-1">
            <li
              v-for="(line, lineIndex) in push.notice.split('\n')"
              :key="lineIndex"
              class="flex gap-2.5 text-xs leading-relaxed text-[var(--color-text-secondary)]"
            >
              <span class="mt-[7px] h-1 w-1 shrink-0 rounded-full bg-[var(--color-border-strong)]" aria-hidden="true" />
              <span>{{ line }}</span>
            </li>
          </ul>
        </details>

        <!-- 应用清单 -->
        <div class="divide-y divide-[var(--color-border)]">
          <div v-for="(app, appIndex) in push.apps" :key="app.pkg + '-' + appIndex" class="px-4 py-3">
            <div class="flex flex-wrap items-center gap-x-3 gap-y-1.5">
              <NuxtLink
                v-if="app.pkg"
                :to="'/' + locale + '/xms/' + encodeURIComponent(app.pkg)"
                class="text-sm font-medium text-[var(--color-text)] transition-colors hover:text-[var(--color-accent)]"
              >
                {{ pickXmsText(app.name, locale) || app.pkg }}
              </NuxtLink>
              <span v-else class="text-sm font-medium text-[var(--color-text)]">
                {{ pickXmsText(app.name, locale) }}
              </span>
              <span v-if="app.pkg" class="break-all font-mono text-[11px] text-[var(--color-text-tertiary)]">
                <span class="sr-only">{{ $t('xmsPackage') }}: </span>{{ app.pkg }}
              </span>
              <span
                v-if="app.type"
                class="rounded-full border border-[var(--color-border)] px-1.5 py-0.5 text-[10px] text-[var(--color-text-secondary)]"
              >
                {{ typeLabel(app.type) }}
              </span>
              <span class="grow" />
              <span v-if="app.releaseDate" class="text-[11px] tabular-nums text-[var(--color-text-tertiary)]">
                {{ app.releaseDate }}
              </span>
              <span class="font-mono text-xs text-[var(--color-text)]">{{ app.version || '—' }}</span>
              <XmsDownloadButton
                v-if="app.pkg"
                :pkg="app.pkg"
                :code="push.code"
                :device="push.device"
                :region="push.region"
                :zone="push.zone ?? 1"
                :android="push.android"
                :rom-version="push.romVersions?.[0] || ''"
                :xms-version="push.version"
                :size="app.size"
              />
            </div>

            <XmsLogList v-if="app.logs && app.logs.length" :logs="app.logs" class="mt-2" />
            <p
              v-if="app.file"
              class="mt-1.5 break-all font-mono text-[11px] text-[var(--color-text-tertiary)]"
              :title="app.file"
            >
              {{ xmsFileVersion(app.file, app.version) }}
            </p>
          </div>

          <!-- 没有包名的日志模块（如「小米超级岛」）：不是应用，只展示模块名与日志 -->
          <div v-for="(extra, extraIndex) in push.extra || []" :key="'extra-' + extraIndex" class="px-4 py-3">
            <p class="text-sm font-medium text-[var(--color-text)]">{{ pickXmsText(extra.name, locale) }}</p>
            <XmsLogList v-if="extra.logs && extra.logs.length" :logs="extra.logs" class="mt-2" />
          </div>
        </div>
      </details>
    </template>
  </div>
</template>

<script setup>
import { pickXmsText, xmsFileVersion } from '~/utils/xms'

const { locale, t } = useI18n()

const typeLabel = (type) =>
  type === 'phone' ? t('xmsTypePhone')
    : type === 'pad' ? t('xmsTypePad')
      : type === 'fold' ? t('xmsTypeFold')
        : type === 'universal' ? t('xmsTypeUniversal')
          : ''
const route = useRoute()
const { buildXmsDeviceUrl } = useApi()
const regionName = useRegionName()

const deviceCode = computed(() => String(route.params.device || ''))

const { data, pending, error } = await useAsyncData(
  'xms-device',
  () => $fetch(buildXmsDeviceUrl(deviceCode.value)),
  { watch: [deviceCode] }
)

const device = computed(() => data.value || null)

useHead({
  title: computed(() => {
    const name = pickXmsText(device.value?.name, locale.value) || deviceCode.value
    return `${name} - ${t('xms')} - ${t('site')}`
  }),
})
</script>
