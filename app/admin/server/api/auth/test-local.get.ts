import { getPool } from '../../utils/db'

/**
 * 验证本地默认数据库连接：使用 runtimeConfig.db 配置连接并查询 VERSION()。
 * 返回 { ok, version, config } 供客户端存储。
 */
export default defineEventHandler(async () => {
  try {
    const pool = getPool()
    const [rows] = await pool.query('SELECT VERSION() AS version') as [Array<{ version: string }>, unknown]

    // 返回 runtimeConfig 中的配置（客户端存储后用于后续请求）
    const cfg = useRuntimeConfig().db as {
      host: string
      port: number
      user: string
      password: string
      database: string
    }

    return {
      ok: true,
      version: rows[0]?.version,
      config: { ...cfg },
    }
  } catch (e) {
    throw createError({
      statusCode: 401,
      message: `本地数据库连接失败：${e instanceof Error ? e.message : String(e)}`,
    })
  }
})
