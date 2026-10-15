<template>
  <Teleport to="body">
    <div v-if="rom" class="fixed inset-0 z-50 flex items-center justify-center p-4" @click.self="emit('close')">
      <div class="fixed inset-0 bg-black/50" @click="emit('close')"></div>
      <div class="relative z-10 max-h-[85vh] w-full max-w-3xl overflow-y-auto rounded-2xl border border-[var(--color-border)] bg-[var(--color-bg-surface)] shadow-2xl">
        <!-- Header -->
        <div class="sticky top-0 flex items-center justify-between border-b border-[var(--color-border)] bg-[var(--color-bg-surface)] px-6 py-4">
          <div>
            <h3 class="font-semibold text-[var(--color-text)]">{{ title || $t('devinfo') }}</h3>
            <p class="mt-0.5 font-mono text-xs text-[var(--color-text-tertiary)]">{{ version }}</p>
          </div>
          <button
            type="button"
            class="rounded-lg p-1.5 transition-colors hover:bg-[var(--color-bg-subtle)]"
            :aria-label="$t('close')"
            @click="emit('close')"
          >
            <svg class="h-5 w-5 text-[var(--color-text-tertiary)]" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" d="M6 18 18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <div class="space-y-5 px-6 py-5">
          <!-- Info Grid -->
          <dl class="grid grid-cols-1 gap-3 text-sm sm:grid-cols-2">
            <div class="rounded-lg bg-[var(--color-bg-subtle)] px-3 py-2.5">
              <dt class="text-xs text-[var(--color-text-secondary)]">{{ $t('version') }}</dt>
              <dd class="mt-0.5 font-mono text-xs break-all text-[var(--color-text)]">{{ version }}</dd>
            </div>
            <div class="rounded-lg bg-[var(--color-bg-subtle)] px-3 py-2.5">
              <dt class="text-xs text-[var(--color-text-secondary)]">{{ $t('android') }}</dt>
              <dd class="mt-0.5 text-xs text-[var(--color-text)]">{{ rom.android || $t('na') }}</dd>
            </div>
            <div class="rounded-lg bg-[var(--color-bg-subtle)] px-3 py-2.5">
              <dt class="text-xs text-[var(--color-text-secondary)]">{{ $t('release') }}</dt>
              <dd class="mt-0.5 text-xs text-[var(--color-text)]">{{ rom.release || $t('na') }}</dd>
            </div>
            <div class="rounded-lg bg-[var(--color-bg-subtle)] px-3 py-2.5">
              <dt class="text-xs text-[var(--color-text-secondary)]">{{ $t('aspatch') }}</dt>
              <dd class="mt-0.5 text-xs text-[var(--color-text)]">{{ rom.aspatch || $t('na') }}</dd>
            </div>
          </dl>

          <!-- Downloads -->
          <div v-if="packages.length">
            <h4 class="mb-2 text-xs font-semibold uppercase tracking-wide text-[var(--color-text-secondary)]">{{ $t('download') }}</h4>
            <div class="flex flex-col gap-1.5">
              <div v-for="item in packages" :key="item.key" class="flex gap-1.5">
                <a
                  :href="item.url"
                  class="inline-flex flex-1 items-center gap-2 rounded-lg px-3 py-2 text-sm font-medium transition-colors"
                  :class="item.primary
                    ? 'bg-[var(--color-accent)] text-white hover:opacity-90'
                    : 'border border-[var(--color-border)] text-[var(--color-text)] hover:bg-[var(--color-bg-subtle)]'"
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  <svg class="h-4 w-4" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" d="M3 16.5v2.25A2.25 2.25 0 0 0 5.25 21h13.5A2.25 2.25 0 0 0 21 18.75V16.5M16.5 12 12 16.5m0 0L7.5 12m4.5 4.5V3" /></svg>
                  {{ item.label }}
                </a>
                <button
                  type="button"
                  :aria-label="$t('copy')"
                  class="inline-flex shrink-0 items-center justify-center gap-1 rounded-lg border border-[var(--color-border)] px-2.5 text-sm text-[var(--color-text)] transition-colors hover:bg-[var(--color-bg-subtle)]"
                  @click="copyLink(item.url)"
                >
                  <svg v-if="copiedUrl === item.url" class="h-4 w-4 text-[var(--color-accent)]" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" d="m4.5 12.75 6 6 9-13.5" /></svg>
                  <svg v-else class="h-4 w-4" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" d="M8 16H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h8a2 2 0 0 1 2 2v2m-6 12h8a2 2 0 0 0 2-2v-8a2 2 0 0 0-2-2h-8a2 2 0 0 0-2 2v8a2 2 0 0 0 2 2Z" /></svg>
                </button>
              </div>
            </div>
          </div>

          <!-- High-speed Download（需要分支代号，缺参数时整块不展示） -->
          <div v-if="highSpeedParams">
            <h4 class="mb-2 text-xs font-semibold uppercase tracking-wide text-[var(--color-text-secondary)]">{{ $t('highSpeed') }}</h4>
            <button
              type="button"
              class="flex w-full items-center justify-center gap-2 rounded-lg bg-[var(--color-accent)] px-3 py-2 text-sm font-medium text-white transition-opacity hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-60"
              :disabled="highSpeedLoading"
              @click="onFetchHighSpeed"
            >
              <span v-if="highSpeedLoading" class="spinner" role="status"></span>
              <svg v-else class="h-4 w-4" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" d="M3.75 13.5l10.5-11.25L12 10.5h8.25L9.75 21.75 12 13.5H3.75z" />
              </svg>
              {{ highSpeedLoading ? $t('fetching') : $t('getHighSpeed') }}
            </button>

            <div v-if="highSpeedResult" class="mt-2 space-y-1.5">
              <template v-if="highSpeedResult.status === 'available'">
                <div v-for="link in highSpeedResult.links" :key="link.url" class="flex gap-1.5">
                  <a
                    :href="link.url"
                    class="inline-flex flex-1 items-center gap-2 rounded-lg border border-[var(--color-border)] px-3 py-2 text-sm font-medium text-[var(--color-text)] transition-colors hover:bg-[var(--color-bg-subtle)]"
                    target="_blank"
                    rel="noopener noreferrer"
                  >
                    <svg class="h-4 w-4" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" d="M3 16.5v2.25A2.25 2.25 0 0 0 5.25 21h13.5A2.25 2.25 0 0 0 21 18.75V16.5M16.5 12 12 16.5m0 0L7.5 12m4.5 4.5V3" /></svg>
                    <span class="truncate">{{ mirrorLabel(link.mirror) }}</span>
                  </a>
                  <button
                    type="button"
                    :aria-label="$t('copy')"
                    class="inline-flex shrink-0 items-center justify-center gap-1 rounded-lg border border-[var(--color-border)] px-2.5 text-sm text-[var(--color-text)] transition-colors hover:bg-[var(--color-bg-subtle)]"
                    @click="copyLink(link.url)"
                  >
                    <svg v-if="copiedUrl === link.url" class="h-4 w-4 text-[var(--color-accent)]" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" d="m4.5 12.75 6 6 9-13.5" /></svg>
                    <svg v-else class="h-4 w-4" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" d="M8 16H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h8a2 2 0 0 1 2 2v2m-6 12h8a2 2 0 0 0 2-2v-8a2 2 0 0 0-2-2h-8a2 2 0 0 0-2 2v8a2 2 0 0 0 2 2Z" /></svg>
                  </button>
                </div>
              </template>
              <p v-else-if="highSpeedResult.status === 'outdated'" class="text-xs leading-relaxed text-[var(--color-text-secondary)]">
                {{ $t('otaOutdated', { latest: highSpeedResult.latestVersion }) }}
              </p>
              <p v-else-if="highSpeedResult.status === 'unsigned'" class="text-xs leading-relaxed text-[var(--color-text-secondary)]">
                {{ $t('otaUnsigned') }}
              </p>
              <p v-else-if="highSpeedResult.status === 'notfound'" class="text-xs leading-relaxed text-[var(--color-text-secondary)]">
                {{ $t('otaNotFound') }}
              </p>
              <p v-else class="text-xs leading-relaxed text-[var(--color-warn)]">
                {{ $t('requestFailed') }}{{ highSpeedResult.message }}
              </p>
            </div>
          </div>

          <!-- Changelog -->
          <div v-if="logsLoading" class="flex justify-center py-4">
            <span class="spinner" role="status"></span>
          </div>
          <div v-else-if="logs" class="space-y-4">
            <h4 class="text-xs font-semibold uppercase tracking-wide text-[var(--color-text-secondary)]">{{ $t('logs') }}</h4>
            <div v-for="(items, category) in logs" :key="category">
              <h5 class="mb-1 text-xs font-semibold text-[var(--color-text)]">{{ category }}</h5>
              <ul class="space-y-1 text-sm text-[var(--color-text)]">
                <li v-for="(item, i) in items" :key="i" class="flex gap-2">
                  <span class="mt-[7px] h-1 w-1 shrink-0 rounded-full bg-[var(--color-text-tertiary)]" aria-hidden="true"></span>
                  <span>{{ item }}</span>
                </li>
              </ul>
            </div>
          </div>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
/**
 * ROM 详情弹窗（机型详情页 / 刷机包页 / 开发版周报页共用）。
 *
 * - rom：ROM 对象；版本号字段名由 versionKey 指定（机型数据用 miui，开发版周报用 version）
 * - logs：更新日志对象 {模块: [条目,…]}，由调用方按当前语言取好；logsLoading 用于异步加载态
 * - highSpeedParams：高速下载所需参数；为 null 时整块不展示（开发版周报没有分支代号）
 */
const props = defineProps({
  rom: { type: Object, default: null },
  versionKey: { type: String, default: 'miui' },
  title: { type: String, default: '' },
  logs: { type: Object, default: null },
  logsLoading: { type: Boolean, default: false },
  highSpeedParams: { type: Object, default: null },
})
const emit = defineEmits(['close'])

const { buildDownloadLink, fetchHighSpeed } = useApi()

const version = computed(() => String(props.rom?.[props.versionKey] ?? ''))

// 下载包：卡刷包用强调色，其余（线刷包 / 运营商定制包）用描边按钮
const { t } = useI18n()
const packages = computed(() => {
  const rom = props.rom
  if (!rom || !version.value) return []
  const list = [
    { key: 'recovery', label: t('recovery'), primary: true },
    { key: 'fastboot', label: t('fastboot'), primary: false },
    { key: 'ctelecom', label: t('ctelecom'), primary: false },
    { key: 'cmobile', label: t('cmobile'), primary: false },
    { key: 'cunicom', label: t('cunicom'), primary: false },
  ]
  return list
    .filter((item) => rom[item.key])
    .map((item) => ({ ...item, url: buildDownloadLink(version.value, rom[item.key]) }))
})

// 复制下载链接到剪贴板
const copiedUrl = ref('')
const copyLink = async (url) => {
  try {
    await navigator.clipboard.writeText(url)
  } catch {
    const input = document.createElement('textarea')
    input.value = url
    document.body.appendChild(input)
    input.select()
    document.execCommand('copy')
    document.body.removeChild(input)
  }
  copiedUrl.value = url
  setTimeout(() => { copiedUrl.value = '' }, 1500)
}

// 高速下载：需要分支代号（branch.id），由调用方通过 highSpeedParams 传入
const highSpeedLoading = ref(false)
const highSpeedResult = ref(null)
const onFetchHighSpeed = async () => {
  if (!props.highSpeedParams || highSpeedLoading.value) return
  highSpeedLoading.value = true
  try {
    highSpeedResult.value = await fetchHighSpeed({ ...props.highSpeedParams, version: version.value })
  } catch (e) {
    highSpeedResult.value = { status: 'failed', message: e?.message || 'request failed' }
  } finally {
    highSpeedLoading.value = false
  }
}

// 换 ROM 时清掉上一次的高速下载结果
watch(() => props.rom, () => {
  highSpeedResult.value = null
  highSpeedLoading.value = false
})

// 链接行展示镜像主机名，比完整域名简洁
const mirrorLabel = (mirror) => {
  try {
    return new URL(mirror).hostname
  } catch {
    return mirror
  }
}
</script>
