import mysql from 'mysql2/promise'

/**
 * 验证数据库连接：使用提供的配置（或默认配置）尝试连接并查询 VERSION()。
 * 返回 { ok, version, config } 供客户端存储。
 */
export default defineEventHandler(async (event) => {
  const body = await readBody(event)
  if (!body || typeof body !== 'object') {
    throw createError({ statusCode: 400, message: '请求体无效' })
  }

  const config = {
    host: String(body.host || 'localhost'),
    port: Number(body.port || 3306),
    user: String(body.user || 'root'),
    password: String(body.password || ''),
    database: String(body.database || 'miroms'),
  }

  try {
    // 尝试用提供的配置创建临时连接池并验证
    const testPool = mysql.createPool({
      ...config,
      connectionLimit: 1,
      connectTimeout: 5_000,
      dateStrings: true,
      charset: 'utf8mb4',
    })
    const [rows] = await testPool.query('SELECT VERSION() AS version') as [Array<{ version: string }>, unknown]
    await testPool.end()

    return {
      ok: true,
      version: rows[0]?.version,
      config,
    }
  } catch (e) {
    throw createError({
      statusCode: 401,
      message: `数据库连接失败：${e instanceof Error ? e.message : String(e)}`,
    })
  }
})
