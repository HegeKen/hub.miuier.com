export const useDarkMode = () => {
  const isDark = ref(false)

  const initDarkMode = () => {
    if (!import.meta.client) return

    const savedMode = localStorage.getItem('dark-mode')
    if (savedMode === 'true') {
      isDark.value = true
      document.documentElement.classList.add('dark')
    } else if (savedMode === 'false') {
      isDark.value = false
      document.documentElement.classList.remove('dark')
    } else {
      isDark.value = window.matchMedia('(prefers-color-scheme: dark)').matches
      if (isDark.value) {
        document.documentElement.classList.add('dark')
      }
    }
  }

  const toggleDarkMode = () => {
    isDark.value = !isDark.value
    if (isDark.value) {
      document.documentElement.classList.add('dark')
      localStorage.setItem('dark-mode', 'true')
    } else {
      document.documentElement.classList.remove('dark')
      localStorage.setItem('dark-mode', 'false')
    }
  }

  return {
    isDark,
    initDarkMode,
    toggleDarkMode,
  }
}
