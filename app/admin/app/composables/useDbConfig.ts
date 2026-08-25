const STORAGE_KEY = 'miroms_db_config'

export interface DbConfig {
  host: string
  port: number
  user: string
  password: string
  database: string
}

export function useDbConfig() {
  const getConfig = (): DbConfig | null => {
    if (typeof window === 'undefined') return null
    try {
      const raw = localStorage.getItem(STORAGE_KEY)
      return raw ? JSON.parse(raw) : null
    } catch {
      return null
    }
  }

  const saveConfig = (config: DbConfig) => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(config))
  }

  const clearConfig = () => {
    localStorage.removeItem(STORAGE_KEY)
  }

  const isLoggedIn = (): boolean => getConfig() !== null

  return { getConfig, saveConfig, clearConfig, isLoggedIn }
}
