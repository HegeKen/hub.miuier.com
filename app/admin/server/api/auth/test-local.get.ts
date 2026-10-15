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
    const detail = e instanceof Error ? e.message : String(e)
    throw createError({
      statusCode: 401,
      message:
        `本地数据库连接失败：${detail}\n` +
        '本地默认连接按「NUXT_DB_* 环境变量 → data/scripts/config.py → 内置默认值」取值。' +
        '请在 app/admin/.env 里配置 NUXT_DB_HOST / NUXT_DB_USER / NUXT_DB_PASSWORD / NUXT_DB_NAME' +
        '（模板见 app/admin/.env.example），或在数据库连接的另一项「远程数据库 / 自定义连接」中填写完整参数。',
    })
  }
})
