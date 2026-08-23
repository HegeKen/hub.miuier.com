import mysql from 'mysql2/promise'
import { getPool } from '../../utils/db'

/**
 * SQL 控制台：仅允许只读语句（SELECT / SHOW / DESCRIBE / EXPLAIN / WITH），
 * 自动追加 LIMIT 防止误拉全表。写操作请走表格编辑器。
 */
const READ_ONLY_RE = /^\s*(select|show|describe|desc|explain|with)\b/i
const MAX_ROWS = 1000

export default defineEventHandler(async (event) => {
  const body = await readBody(event)
  let sql = typeof body?.sql === 'string' ? body.sql.trim() : ''

  if (!sql) {
    throw createError({ statusCode: 400, message: 'SQL 不能为空' })
  }

  // 去掉末尾分号与注释行
  sql = sql.replace(/;\s*$/, '')

  if (!READ_ONLY_RE.test(sql)) {
    throw createError({
      statusCode: 400,
      message: '仅允许只读语句（SELECT / SHOW / DESCRIBE / EXPLAIN / WITH），数据修改请使用表格编辑功能',
    })
  }

  // SELECT / WITH 查询未指定 LIMIT 时自动限制行数
  if (/^\s*(select|with)\b/i.test(sql) && !/\blimit\s+\d+/i.test(sql)) {
    sql += ` LIMIT ${MAX_ROWS}`
  }

  const start = performance.now()
  try {
    const [rows, fields] = await getPool().query(sql)
    const duration = Math.round((performance.now() - start) * 1000) / 1000

    return {
      ok: true,
      duration,
      fields: fields ? (fields as mysql.FieldPacket[]).map((f) => f.name) : [],
      rows: rows as unknown[],
      rowCount: Array.isArray(rows) ? rows.length : 0,
      affectedRows: (rows as mysql.ResultSetHeader).affectedRows ?? 0,
    }
  } catch (e) {
    const message = e instanceof Error ? e.message : String(e)
    throw createError({ statusCode: 400, message: message })
  }
})
