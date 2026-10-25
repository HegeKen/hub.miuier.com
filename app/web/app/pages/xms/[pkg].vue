<template>
  <div class="container-page py-10 sm:py-14">
    <!-- Loading -->
    <div v-if="pending" class="flex justify-center py-20">
      <span class="spinner" role="status" aria-label="Loading"></span>
    </div>

    <!-- Error / empty -->
    <div v-else-if="error || !app" class="py-20 text-center">
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
          <li class="truncate font-mono text-xs font-medium text-[var(--color-text)]">{{ app.pkg }}</li>
        </ol>
      </nav>

      <!-- Title -->
      <header class="mb-6">
        <h1 class="text-2xl font-semibold tracking-tight sm:text-3xl">
          {{ pickXmsText(app.name, locale) || app.pkg }}
        </h1>
        <p class="mt-1.5 break-all font-mono text-xs text-[var(--color-text-tertiary)]">
          <span class="sr-only">{{ $t('xmsPackage') }}: </span>{{ app.pkg }}
        </p>
        <p class="mt-2 text-xs text-[var(--color-text-tertiary)]">
          {{ $t('totalDevices', { count: app.deviceCount }) }}
          <span class="mx-1.5">·</span>
          {{ $t('xmsRecords') }} {{ app.recordCount }}
        </p>
      </header>

      <p v-if="!groups.length" class="py-16 text-center text-sm text-[var(--color-text-secondary)]">
        {{ $t('xmsEmpty') }}
      </p>

      <!-- 按「XMS 版本 + 应用版本」归组：版本 / 安装包 / 日志只展示一次，下面只列机型与系统版本 -->
      <section class="space-y-4">
        <article
          v-for="group in groups"
          :key="group.version + '|' + group.appVersion"
          class="overflow-hidden rounded-xl border border-[var(--color-border)] bg-[var(--color-bg-surface)]"
        >
          <!-- 统一版本区域 -->
          <div class="flex flex-wrap items-center gap-x-3 gap-y-1.5 border-b border-[var(--color-border)] px-4 py-3">
            <span class="rounded border border-[var(--color-border)] bg-[var(--color-bg-subtle)] px-2 py-0.5 font-mono text-xs font-semibold text-[var(--color-text)]">
              {{ group.version }}
            </span>
            <span
              v-if="group.os"
              class="rounded-full border border-[var(--color-border)] px-2 py-0.5 text-[11px] font-medium text-[var(--color-text-secondary)]"
            >
              {{ group.os }}
            </span>
            <span class="text-xs text-[var(--color-text-tertiary)]">{{ $t('xmsAppVersion') }}</span>
            <span class="font-mono text-sm font-semibold text-[var(--color-text)]">{{ group.appVersion || '—' }}</span>
            <span
              v-if="group.isLatest"
              class="rounded-full bg-[var(--color-accent-soft)] px-2 py-0.5 text-[11px] font-medium text-[var(--color-accent)]"
            >
              {{ $t('xmsLatest') }}
            </span>
            <span class="grow" />
            <span class="text-xs text-[var(--color-text-tertiary)]">
              {{ $t('totalDevices', { count: group.devices.length }) }}
              <span class="mx-1.5">·</span>
              {{ $t('xmsRecords') }} {{ group.records.length }}
            </span>
          </div>

          <!-- 安装包（同一版本只在顶部给一次） -->
          <div v-if="group.file" class="flex flex-wrap items-center gap-x-3 gap-y-2 border-b border-[var(--color-border)] px-4 py-2.5">
            <span class="rounded border border-[var(--color-border)] px-1.5 py-0.5 font-mono text-[10px] font-semibold text-[var(--color-text-secondary)]">APK</span>
            <span class="break-all font-mono text-xs text-[var(--color-text-secondary)]">{{ group.file }}</span>
            <span v-if="group.size" class="text-xs tabular-nums text-[var(--color-text-tertiary)]">{{ formatBytes(group.size) }}</span>
            <span class="grow" />
            <XmsDownloadButton
              :pkg="app.pkg"
              :code="group.best.code"
              :device="group.best.device"
              :region="group.best.region"
              :zone="group.best.zone ?? 1"
              :android="group.best.android"
              :rom-version="group.best.romVersion"
              :xms-version="group.version"
              :size="group.size"
            />
          </div>
          <p v-if="group.md5" class="border-b border-[var(--color-border)] px-4 py-2 break-all font-mono text-[11px] text-[var(--color-text-tertiary)]">
            MD5: {{ group.md5 }}
          </p>

          <!-- 更新日志（同一版本共用） -->
          <div class="border-b border-[var(--color-border)] px-4 py-3">
            <XmsLogList v-if="group.logs.length" :logs="group.logs" />
            <p v-else class="text-xs text-[var(--color-text-tertiary)]">{{ $t('noLogs') }}</p>
          </div>

          <!-- 机型 + 对应系统版本 -->
          <div class="divide-y divide-[var(--color-border)]">
            <div
              v-for="device in group.devices"
              :key="device.device"
              class="flex flex-wrap items-center gap-x-3 gap-y-1 px-4 py-2.5"
            >
              <NuxtLink
                :to="'/' + locale + '/xms/device/' + encodeURIComponent(device.device)"
                class="text-sm font-medium text-[var(--color-text)] transition-colors hover:text-[var(--color-accent)]"
              >
                {{ pickXmsText(device.name, locale) || device.device }}
              </NuxtLink>
              <span class="font-mono text-[11px] text-[var(--color-text-tertiary)]">{{ device.device }}</span>
              <span
                v-if="device.isLatest"
                class="rounded-full bg-[var(--color-accent-soft)] px-2 py-0.5 text-[11px] font-medium text-[var(--color-accent)]"
              >
                {{ $t('xmsLatest') }}
              </span>
              <span class="grow" />
              <span class="flex flex-wrap items-center gap-1">
                <span
                  v-for="romVersion in device.versions"
                  :key="romVersion"
                  class="rounded border border-[var(--color-border)] px-1.5 py-0.5 font-mono text-[10px] text-[var(--color-text-secondary)]"
                >
                  {{ romVersion }}
                </span>
              </span>
              <span class="text-xs tabular-nums text-[var(--color-text-tertiary)]" :title="$t('xmsPushDate')">
                {{ device.pushDate }}
              </span>
            </div>
          </div>
        </article>
      </section>
    </template>
  </div>
</template>

<script setup>
import { formatBytes, pickXmsText, xmsOsOf } from '~/utils/xms'

const { locale, t } = useI18n()
const route = useRoute()
const { buildXmsAppUrl } = useApi()

const pkg = computed(() => String(route.params.pkg || ''))

const { data, pending, error } = await useAsyncData(
  'xms-app',
  () => $fetch(buildXmsAppUrl(pkg.value)),
  { watch: [pkg] }
)

const app = computed(() => data.value || null)

const deviceLabel = (device) => pickXmsText(device?.name, locale.value) || device?.device || ''

// 系统版本号按数字段降序（OS3.0.305 排在 OS3.0.304 前面）
const romVersionKey = (version) => String(version || '').match(/\d+/g)?.slice(0, 4).map(Number) || []
const compareRomVersionDesc = (a, b) => {
  const left = romVersionKey(a)
  const right = romVersionKey(b)
  for (let index = 0; index < 4; index += 1) {
    const diff = (right[index] || 0) - (left[index] || 0)
    if (diff) return diff
  }
  return 0
}

/**
 * 同一「XMS 版本 + 应用版本」只保留一份版本信息（安装包 / 日志 / 校验值），
 * 设备列表里只列机型与对应系统版本，避免同一份内容在每台机型下重复。
 */
const groups = computed(() => {
  const records = app.value?.records || []
  const map = new Map()

  for (const record of records) {
    const key = `${record.version}|${record.appVersion}`
    let group = map.get(key)
    if (!group) {
      group = {
        version: record.version,
        os: record.os || xmsOsOf(record.version),
        appVersion: record.appVersion,
        records: [],
        best: null,
        isLatest: false,
      }
      map.set(key, group)
    }
    group.records.push(record)
    if (record.isLatest) group.isLatest = true
    // 顶部展示用的记录：优先「最新 + 有安装包」
    if (
      !group.best ||
      (record.isLatest && !group.best.isLatest) ||
      (!group.best.file && record.file)
    ) {
      group.best = record
    }
  }

  const list = []
  for (const group of map.values()) {
    const devices = new Map()
    for (const record of group.records) {
      let device = devices.get(record.device)
      if (!device) {
        device = {
          device: record.device,
          name: record.deviceName,
          versions: [],
          pushDate: record.pushDate,
          isLatest: false,
        }
        devices.set(record.device, device)
      }
      if (!device.versions.includes(record.romVersion)) {
        device.versions.push(record.romVersion)
      }
      if (record.isLatest) device.isLatest = true
      if (String(record.pushDate || '') > String(device.pushDate || '')) {
        device.pushDate = record.pushDate
      }
    }

    for (const device of devices.values()) {
      device.versions.sort(compareRomVersionDesc)
    }

    group.devices = [...devices.values()].sort(
      (a, b) => (Number(b.isLatest) - Number(a.isLatest)) || deviceLabel(a).localeCompare(deviceLabel(b))
    )
    const withLogs = group.records.find((record) => record.logs?.length)
    group.logs = group.best?.logs?.length ? group.best.logs : (withLogs?.logs || [])
    group.file = group.best?.file || ''
    group.size = group.best?.size || 0
    group.md5 = group.best?.md5 || ''
    list.push(group)
  }

  // 最新版本在前，其次按推送日期、版本号降序
  list.sort(
    (a, b) =>
      (Number(b.isLatest) - Number(a.isLatest)) ||
      String(b.best?.pushDate || '').localeCompare(String(a.best?.pushDate || '')) ||
      String(b.version).localeCompare(String(a.version))
  )
  return list
})

useHead({
  title: computed(() => {
    const name = pickXmsText(app.value?.name, locale.value) || pkg.value
    return `${name} - ${t('xms')} - ${t('site')}`
  }),
})
</script>
