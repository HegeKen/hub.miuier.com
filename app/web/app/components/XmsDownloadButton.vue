<template>
  <span class="inline-flex flex-wrap items-center gap-2">
    <button
      type="button"
      class="pill"
      :class="loading ? 'pointer-events-none opacity-60' : ''"
      :disabled="loading || !pkg"
      :aria-busy="loading"
      @click="download"
    >
      <span v-if="loading" class="spinner !h-3.5 !w-3.5" role="status" aria-hidden="true" />
      <template v-else>
        {{ $t('download') }}
        <span v-if="sizeLabel" class="font-mono text-xs text-[var(--color-text-tertiary)]">
          {{ sizeLabel }}
        </span>
      </template>
    </button>
    <span v-if="failed" class="text-xs text-[var(--color-danger)]">{{ $t('xmsDownloadFailed') }}</span>
  </span>
</template>

<script setup>
import { formatBytes } from '~/utils/xms'

const props = defineProps({
  /** 应用包名 */
  pkg: { type: String, required: true },
  /** 分支代号（XMS 记录的 code） */
  code: { type: String, default: '' },
  /** 机型代号 */
  device: { type: String, default: '' },
  region: { type: String, default: 'cn' },
  zone: { type: Number, default: 1 },
  /** Android 版本，如 16.0 */
  android: { type: String, default: '' },
  /** 对应系统版本，如 OS3.0.304.0.WOTCNXM */
  romVersion: { type: String, default: '' },
  /** XMS 版本号，如 C11 */
  xmsVersion: { type: String, default: '' },
  /** 已知 APK 体积（字节），仅用于按钮上的展示 */
  size: { type: Number, default: 0 },
})

const loading = ref(false)
const failed = ref(false)

const sizeLabel = computed(() => formatBytes(props.size))

const download = async () => {
  if (!props.pkg || loading.value) return
  loading.value = true
  failed.value = false
  try {
    const result = await $fetch('/api/xms/download', {
      method: 'POST',
      body: {
        pkg: props.pkg,
        code: props.code,
        device: props.device,
        region: props.region,
        zone: props.zone,
        android: props.android,
        romVersion: props.romVersion,
        xmsVersion: props.xmsVersion,
      },
    })
    const url = result?.status === 'available' ? result.urls?.[0] : ''
    if (!url) {
      failed.value = true
      return
    }
    // 签名直链会直接触发下载；用临时 <a> 打开，避免异步请求被弹窗拦截
    const anchor = document.createElement('a')
    anchor.href = url
    anchor.rel = 'noopener noreferrer'
    anchor.target = '_blank'
    anchor.click()
  } catch {
    failed.value = true
  } finally {
    loading.value = false
  }
}
</script>
