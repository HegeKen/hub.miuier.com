<template>
  <div class="flex min-h-screen items-center justify-center bg-[var(--color-bg)] px-4">
    <div class="w-full max-w-md">
      <!-- Logo -->
      <div class="mb-8 text-center">
        <div class="mx-auto mb-3 flex h-14 w-14 items-center justify-center rounded-2xl bg-[var(--color-accent)] text-2xl font-bold text-white">
          M
        </div>
        <h1 class="text-xl font-bold tracking-tight text-[var(--color-text)]">MiROMS HUB</h1>
        <p class="mt-1 text-sm text-[var(--color-text-secondary)]">数据管理后台</p>
      </div>

      <!-- 卡片 -->
      <div class="rounded-xl border border-[var(--color-border)] bg-[var(--color-bg-surface)] p-6 shadow-lg">
        <!-- 选择连接方式 -->
        <div v-if="!mode" class="space-y-3">
          <h2 class="text-center text-sm font-medium text-[var(--color-text-secondary)]">选择数据库连接方式</h2>
          <button
            type="button"
            class="flex w-full items-center gap-4 rounded-lg border border-[var(--color-border)] p-4 text-left transition-colors hover:border-[var(--color-accent)] hover:bg-[var(--color-bg-subtle)]"
            @click="selectMode('local')"
          >
            <div class="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-green-500/10 text-green-600 dark:bg-green-400/10 dark:text-green-400">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" d="M5.25 14.25h13.5m-13.5 0a3 3 0 0 1-3-3m3 3a3 3 0 1 0 0 6h13.5a3 3 0 1 0 0-6m-16.5-3a3 3 0 0 1 3-3h13.5a3 3 0 0 1 3 3m-19.5 0a4.5 4.5 0 0 1 .9-2.7L5.737 5.1a3.375 3.375 0 0 1 2.7-1.35h7.126c1.062 0 2.062.5 2.7 1.35l2.587 3.45a4.5 4.5 0 0 1 .9 2.7m0 0a3 3 0 0 1-3 3m0 3h.008v.008h-.008v-.008Zm0-6h.008v.008h-.008v-.008Zm-3 6h.008v.008h-.008v-.008Zm0-6h.008v.008h-.008v-.008Z" />
              </svg>
            </div>
            <div>
              <p class="text-sm font-medium text-[var(--color-text)]">本地数据库</p>
              <p class="mt-0.5 text-xs text-[var(--color-text-tertiary)]">使用默认配置连接本地 MySQL</p>
            </div>
          </button>
          <button
            type="button"
            class="flex w-full items-center gap-4 rounded-lg border border-[var(--color-border)] p-4 text-left transition-colors hover:border-[var(--color-accent)] hover:bg-[var(--color-bg-subtle)]"
            @click="selectMode('remote')"
          >
            <div class="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-blue-500/10 text-blue-600 dark:bg-blue-400/10 dark:text-blue-400">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" d="M12 21a9.004 9.004 0 0 0 8.716-6.747M12 21a9.004 9.004 0 0 1-8.716-6.747M12 21c2.485 0 4.5-4.03 4.5-9S14.485 3 12 3m0 18c-2.485 0-4.5-4.03-4.5-9S9.515 3 12 3m0 0a8.997 8.997 0 0 1 7.843 4.582M12 3a8.997 8.997 0 0 0-7.843 4.582m15.686 0A11.953 11.953 0 0 1 12 10.5c-2.998 0-5.74-1.1-7.843-2.918m15.686 0A8.959 8.959 0 0 1 21 12c0 .778-.099 1.533-.284 2.253m0 0A17.919 17.919 0 0 1 12 16.5a17.92 17.92 0 0 1-8.716-2.247m0 0A9.015 9.015 0 0 1 3 12c0-1.605.42-3.113 1.157-4.418" />
              </svg>
            </div>
            <div>
              <p class="text-sm font-medium text-[var(--color-text)]">远程数据库</p>
              <p class="mt-0.5 text-xs text-[var(--color-text-tertiary)]">填写远程服务器地址连接</p>
            </div>
          </button>
        </div>

        <!-- 远程数据库表单 -->
        <form v-else-if="mode === 'remote'" class="space-y-4" @submit.prevent="doLogin">
          <div>
            <button type="button" class="mb-3 flex items-center gap-1 text-xs text-[var(--color-text-tertiary)] transition-colors hover:text-[var(--color-text)]" @click="mode = null">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" d="M15.75 19.5 8.25 12l7.5-7.5" />
              </svg>
              返回
            </button>
            <h3 class="text-sm font-semibold text-[var(--color-text)]">远程数据库连接</h3>
          </div>
          <div class="grid grid-cols-3 gap-3">
            <div class="col-span-2">
              <label for="r-host" class="form-label">主机地址</label>
              <input id="r-host" v-model="form.host" type="text" class="input-base font-mono text-sm" placeholder="如 192.168.1.100" required />
            </div>
            <div>
              <label for="r-port" class="form-label">端口</label>
              <input id="r-port" v-model.number="form.port" type="number" class="input-base font-mono text-sm" placeholder="3306" required />
            </div>
          </div>
          <div>
            <label for="r-user" class="form-label">用户名</label>
            <input id="r-user" v-model="form.user" type="text" class="input-base font-mono text-sm" placeholder="root" required />
          </div>
          <div>
            <label for="r-password" class="form-label">密码</label>
            <input id="r-password" v-model="form.password" type="password" class="input-base font-mono text-sm" placeholder="密码" />
          </div>
          <div>
            <label for="r-database" class="form-label">数据库名</label>
            <input id="r-database" v-model="form.database" type="text" class="input-base font-mono text-sm" placeholder="miroms" required />
          </div>
          <p v-if="error" class="text-xs text-[var(--color-danger)]">{{ error }}</p>
          <button type="submit" class="btn-primary w-full" :disabled="connecting">
            {{ connecting ? '连接中…' : '连接并进入' }}
          </button>
        </form>
      </div>

      <p class="mt-6 text-center text-xs text-[var(--color-text-tertiary)]">
        数据直接读写 MySQL 数据库
      </p>
    </div>
  </div>
</template>

<script setup>
// 无需 layout: false，app.vue 已根据登录状态条件渲染

const router = useRouter()
const { saveConfig } = useDbConfig()

const mode = ref(null)
const connecting = ref(false)
const error = ref('')

const form = reactive({
  host: '',
  port: 3306,
  user: '',
  password: '',
  database: '',
})

const selectMode = async (m) => {
  if (m === 'local') {
    connecting.value = true
    error.value = ''
    try {
      const res = await $fetch('/api/auth/test-local')
      if (res.ok) {
        saveConfig(res.config)
        router.push('/')
      }
    } catch (e) {
      error.value = e?.data?.message || e?.message || '连接失败'
      connecting.value = false
    }
  } else {
    mode.value = 'remote'
  }
}

const doLogin = async () => {
  connecting.value = true
  error.value = ''
  try {
    const res = await $fetch('/api/auth/login', {
      method: 'POST',
      body: { ...form },
    })
    if (res.ok) {
      saveConfig(res.config)
      router.push('/')
    }
  } catch (e) {
    error.value = e?.data?.message || e?.message || '连接失败'
  } finally {
    connecting.value = false
  }
}

onMounted(() => {
  const { getConfig } = useDbConfig()
  if (getConfig()) router.push('/')
})
</script>
