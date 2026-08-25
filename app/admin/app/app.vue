<template>
  <div class="min-h-screen">
    <!-- 未登录：仅渲染页面内容（登录页） -->
    <template v-if="!authed">
      <NuxtPage />
      <Toast />
    </template>

    <!-- 已登录：完整布局 -->
    <template v-else>
    <!-- 移动端遮罩 -->
    <Transition
      enter-active-class="transition-opacity duration-200"
      enter-from-class="opacity-0"
      enter-to-class="opacity-100"
      leave-active-class="transition-opacity duration-150"
      leave-from-class="opacity-100"
      leave-to-class="opacity-0"
    >
      <div
        v-if="sidebarOpen"
        class="fixed inset-0 z-40 bg-black/40 backdrop-blur-sm lg:hidden"
        @click="sidebarOpen = false"
      ></div>
    </Transition>

    <!-- 侧边栏 -->
    <Transition
      enter-active-class="transition-transform duration-200 ease-out"
      enter-from-class="-translate-x-full"
      enter-to-class="translate-x-0"
      leave-active-class="transition-transform duration-150 ease-in"
      leave-from-class="translate-x-0"
      leave-to-class="-translate-x-full"
    >
      <aside
        class="fixed inset-y-0 left-0 z-50 flex w-60 flex-col border-r border-[var(--color-border)] bg-[var(--color-bg-surface)] transition-transform lg:translate-x-0"
        :class="sidebarOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0'"
      >
        <!-- Logo -->
        <NuxtLink to="/" class="flex h-14 items-center gap-2.5 border-b border-[var(--color-border)] px-5" @click="sidebarOpen = false">
          <span class="flex h-8 w-8 items-center justify-center rounded-lg bg-[var(--color-accent)] text-base font-bold text-white">M</span>
          <span class="text-sm font-semibold tracking-tight text-[var(--color-text)]">
            MiROMS HUB<span class="text-[var(--color-accent)]"> 数据管理</span>
          </span>
        </NuxtLink>

        <!-- 导航 -->
        <nav class="flex-1 space-y-1 overflow-y-auto px-3 py-4" aria-label="主导航">
          <NuxtLink
            v-for="item in navItems"
            :key="item.to"
            :to="item.to"
            class="flex items-center gap-2.5 rounded-lg px-3 py-2 text-sm font-medium transition-colors"
            :class="isActive(item.to)
              ? 'bg-[var(--color-accent-soft)] text-[var(--color-accent)]'
              : 'text-[var(--color-text-secondary)] hover:bg-[var(--color-bg-subtle)] hover:text-[var(--color-text)]'"
            :aria-current="isActive(item.to) ? 'page' : undefined"
            @click="sidebarOpen = false"
          >
            <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 shrink-0" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" aria-hidden="true">
              <path v-if="item.icon === 'home'" stroke-linecap="round" stroke-linejoin="round" d="M2.25 12 11.204 3.045a1.126 1.126 0 0 1 1.592 0L21.75 12M4.5 9.75v10.125c0 .621.504 1.125 1.125 1.125H9.75v-4.875c0-.621.504-1.125 1.125-1.125h2.25c.621 0 1.125.504 1.125 1.125V21h4.125c.621 0 1.125-.504 1.125-1.125V9.75" />
              <path v-else-if="item.icon === 'device'" stroke-linecap="round" stroke-linejoin="round" d="M9 17.25v1.007a3 3 0 0 1-.879 2.122L7.5 21h9l-.621-.621A3 3 0 0 1 15 18.257V17.25m6-12V15a2.25 2.25 0 0 1-2.25 2.25H5.25A2.25 2.25 0 0 1 3 15V5.25m18 0A2.25 2.25 0 0 0 18.75 3H5.25A2.25 2.25 0 0 0 3 5.25m18 0V12a2.25 2.25 0 0 1-2.25 2.25H5.25A2.25 2.25 0 0 1 3 12V5.25" />
              <path v-else-if="item.icon === 'tag'" stroke-linecap="round" stroke-linejoin="round" d="M9.568 3H5.25A2.25 2.25 0 0 0 3 5.25v4.318c0 .597.237 1.17.659 1.591l9.581 9.581c.699.699 1.78.872 2.607.33a18.095 18.095 0 0 0 5.223-5.223c.542-.827.369-1.908-.33-2.607L11.16 3.66A2.25 2.25 0 0 0 9.568 3Z" />
              <path v-else-if="item.icon === 'archive'" stroke-linecap="round" stroke-linejoin="round" d="m20.25 7.5-.625 10.632a2.25 2.25 0 0 1-2.247 2.118H6.622a2.25 2.25 0 0 1-2.247-2.118L3.75 7.5M10 11.25h4M3.375 7.5h17.25c.621 0 1.125-.504 1.125-1.125v-1.5c0-.621-.504-1.125-1.125-1.125H3.375c-.621 0-1.125.504-1.125 1.125v1.5c0 .621.504 1.125 1.125 1.125Z" />
              <path v-else-if="item.icon === 'sql'" stroke-linecap="round" stroke-linejoin="round" d="m6.75 7.5 3 2.25-3 2.25m4.5 0h3m-9 8.25h13.5A2.25 2.25 0 0 0 21 18V6a2.25 2.25 0 0 0-2.25-2.25H5.25A2.25 2.25 0 0 0 3 6v12a2.25 2.25 0 0 0 2.25 2.25Z" />
              <path v-else-if="item.icon === 'table'" stroke-linecap="round" stroke-linejoin="round" d="M3.375 19.5h17.25m-17.25 0a1.125 1.125 0 0 1-1.125-1.125M3.375 19.5h7.5c.621 0 1.125-.504 1.125-1.125m-9.75 0V5.625m0 12.75v-1.5c0-.621.504-1.125 1.125-1.125m18.375 2.625V5.625m0 12.75c0 .621-.504 1.125-1.125 1.125m1.125-1.125v-1.5c0-.621-.504-1.125-1.125-1.125m0 3.75h-7.5A1.125 1.125 0 0 1 12 18.375m9.75-12.75c0-.621-.504-1.125-1.125-1.125H3.375c-.621 0-1.125.504-1.125 1.125m19.5 0v1.5c0 .621-.504 1.125-1.125 1.125M2.25 5.625v1.5c0 .621.504 1.125 1.125 1.125m0 0h17.25" />
            </svg>
            {{ item.label }}
          </NuxtLink>
        </nav>

        <!-- 底部 -->
        <div class="border-t border-[var(--color-border)] px-5 py-3">
          <p class="text-xs text-[var(--color-text-tertiary)]">
            MySQL 直连 · 数据管理后台
          </p>
          <div class="mt-2 flex items-center gap-3">
            <a
              href="https://hubman.miuier.com"
              target="_blank"
              rel="noopener noreferrer"
              class="text-xs font-medium text-[var(--color-accent)] hover:underline"
            >
              hubman.miuier.com ↗
            </a>
            <button
              type="button"
              class="text-xs text-[var(--color-text-tertiary)] transition-colors hover:text-[var(--color-danger)]"
              @click="doLogout"
            >
              断开连接
            </button>
          </div>
        </div>
      </aside>
    </Transition>

    <!-- 主区域 -->
    <div class="flex min-h-screen flex-col lg:pl-60">
      <!-- 顶部栏 -->
      <header class="sticky top-0 z-30 border-b border-[var(--color-border)] bg-[var(--color-bg)]/85 backdrop-blur-md">
        <div class="flex h-14 items-center justify-between gap-3 px-4 sm:px-6">
          <div class="flex items-center gap-3">
            <!-- 移动端菜单 -->
            <button
              type="button"
              class="flex h-9 w-9 items-center justify-center rounded-md text-[var(--color-text-secondary)] transition-colors hover:bg-[var(--color-bg-subtle)] hover:text-[var(--color-text)] lg:hidden"
              :aria-label="sidebarOpen ? '关闭菜单' : '打开菜单'"
              :aria-expanded="sidebarOpen"
              @click="sidebarOpen = !sidebarOpen"
            >
              <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" aria-hidden="true">
                <path v-if="!sidebarOpen" stroke-linecap="round" stroke-linejoin="round" d="M3.75 6.75h16.5M3.75 12h16.5M3.75 17.25h16.5" />
                <path v-else stroke-linecap="round" stroke-linejoin="round" d="M6 18 18 6M6 6l12 12" />
              </svg>
            </button>
            <h1 class="text-base font-semibold tracking-tight text-[var(--color-text)] sm:text-lg">{{ pageTitle }}</h1>
          </div>

          <div class="flex items-center gap-2">
            <DbStatus class="hidden sm:inline-flex" />
            <DarkModeToggle />
          </div>
        </div>
        <div class="border-t border-[var(--color-border)] px-4 py-1.5 sm:hidden">
          <DbStatus class="inline-flex" />
        </div>
      </header>

      <!-- 页面内容 -->
      <main id="main-content" class="flex-1" tabindex="-1">
        <NuxtPage />
      </main>

      <footer class="border-t border-[var(--color-border)] py-4 text-center text-xs text-[var(--color-text-tertiary)]">
        MiROMS HUB - 数据管理后台 · 数据直接读写 MySQL 数据库
      </footer>
    </div>

    <Toast />
    </template>
  </div>
</template>

<script setup>
import { useDarkMode } from '~/composables/useDarkMode'

const route = useRoute()
const router = useRouter()
const { initDarkMode } = useDarkMode()
const { isLoggedIn, clearConfig } = useDbConfig()
const sidebarOpen = ref(false)
const authed = ref(false)

onMounted(() => {
  initDarkMode()
  authed.value = isLoggedIn()
  if (!authed.value && route.path !== '/login') {
    router.push('/login')
  }
})

// 监听路由变化，更新登录状态
watch(() => route.path, () => {
  authed.value = isLoggedIn()
})

const doLogout = () => {
  clearConfig()
  router.push('/login')
}

const navItems = [
  { label: '仪表盘', to: '/', icon: 'home' },
  { label: '机型管理', to: '/devices-manager', icon: 'table' },
  { label: '设备系列', to: '/series-manager', icon: 'tag' },
  { label: '设备管理', to: '/devices', icon: 'device' },
  { label: '分支管理', to: '/branches', icon: 'tag' },
  { label: 'ROM 管理', to: '/roms', icon: 'archive' },
  { label: 'SQL 控制台', to: '/sql', icon: 'sql' },
]

const titleMap = {
  '/': '仪表盘',
  '/devices-manager': '机型管理',
  '/series-manager': '设备系列',
  '/devices': '设备管理',
  '/branches': '分支管理',
  '/roms': 'ROM 管理',
  '/sql': 'SQL 控制台',
}

const pageTitle = computed(() => titleMap[route.path] || '数据管理')

const isActive = (to) => {
  const current = route.path.replace(/\/+$/, '') || '/'
  const target = String(to).replace(/\/+$/, '') || '/'
  return current === target
}
</script>
