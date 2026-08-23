<template>
  <span
    class="inline-flex items-center gap-2 rounded-full border border-[var(--color-border)] bg-[var(--color-bg-surface)] px-3 py-1.5 text-xs"
    :title="status.error || '数据库连接正常'"
  >
    <span
      class="dot"
      :class="status.ok ? 'bg-[var(--color-ok)]' : 'bg-[var(--color-danger)]'"
      aria-hidden="true"
    ></span>
    <span v-if="status.ok" class="font-medium text-[var(--color-text-secondary)]">
      数据库已连接<template v-if="status.version"> · MySQL {{ status.version }}</template>
    </span>
    <span v-else class="font-medium text-[var(--color-danger)]">数据库连接失败</span>
  </span>
</template>

<script setup>
const { getStats } = useAdminApi()

const status = ref({ ok: false, version: '', error: '' })

const check = async () => {
  try {
    const data = await getStats()
    status.value = {
      ok: !!data.ping?.ok,
      version: data.ping?.version || '',
      error: data.ping?.error || '',
    }
  } catch {
    status.value = { ok: false, version: '', error: '无法访问 API' }
  }
}

onMounted(() => {
  check()
  // 每 30 秒轮询一次连接状态
  const timer = setInterval(check, 30_000)
  onBeforeUnmount(() => clearInterval(timer))
})
</script>
