import mysql from 'mysql2/promise'
import { createError } from 'h3'

/**
 * 数据库访问层：直连 MySQL（miroms 库），提供参数化查询与表结构元数据。
 * 表名白名单 + 列名校验，防止 SQL 注入与越权操作。
 */

export const ALLOWED_TABLES = ['devices', 'branches', 'roms'] as const
export type AllowedTable = (typeof ALLOWED_TABLES)[number]

export interface ColumnMeta {
  name: string
  dataType: string
  columnType: string
  nullable: boolean
  defaultValue: string | null
  comment: string
  isPrimary: boolean
  isJson: boolean
  isDate: boolean
  isNumber: boolean
  isLong: boolean
}

let pool: mysql.Pool | null = null

export function getPool(): mysql.Pool {
  if (!pool) {
    const cfg = useRuntimeConfig().db as {
      host: string
      port: number
      user: string
      password: string
      database: string
    }
    pool = mysql.createPool({
      host: cfg.host,
      port: cfg.port,
      user: cfg.user,
      password: cfg.password,
      database: cfg.database,
      connectionLimit: 10,
      connectTimeout: 10_000,
      dateStrings: true,
      charset: 'utf8mb4',
    })
  }
  return pool
}

/** 校验表名是否在白名单内，非法表名直接 400 */
export function assertTable(table: string): asserts table is AllowedTable {
  if (!(ALLOWED_TABLES as readonly string[]).includes(table)) {
    throw createError({ statusCode: 400, message: `表 ${table} 不在白名单中` })
  }
}

/* ---------------- 表结构元数据（带缓存） ---------------- */

const META_CACHE_TTL = 60_000
const metaCache = new Map<string, { columns: ColumnMeta[]; expires: number }>()

/**
 * JSON 列识别（该库注释常为空，需多重手段）：
 * 1. information_schema.CHECK_CONSTRAINTS 中的 json_valid(`col`) 约束
 * 2. 列名启发式（数据模型内已知 JSON 字段）
 */
const JSON_NAME_HINT = /^(full_brands|full_names|names|xiaomi|redmi|poco|logs_zh|logs_en)$/i

let jsonColsCache: { map: Map<string, Set<string>>; expires: number } | null = null

async function getJsonColumns(): Promise<Map<string, Set<string>>> {
  if (jsonColsCache && jsonColsCache.expires > Date.now()) {
    return jsonColsCache.map
  }
  const [rows] = await getPool().query<Array<{ CONSTRAINT_NAME: string; CHECK_CLAUSE: string }>>(
    `SELECT CONSTRAINT_NAME, CHECK_CLAUSE
     FROM information_schema.CHECK_CONSTRAINTS
     WHERE CONSTRAINT_SCHEMA = DATABASE()`,
  )
  const map = new Map<string, Set<string>>()
  for (const r of rows) {
    // 约束名形如 roms_chk_1 → 表名 roms
    const tableMatch = /^([a-z0-9_]+)_chk_\d+$/i.exec(r.CONSTRAINT_NAME)
    if (!tableMatch) continue
    const colMatch = /json_valid\(`([^`]+)`\)/i.exec(r.CHECK_CLAUSE)
    if (!colMatch) continue
    const table = tableMatch[1]
    if (!map.has(table)) map.set(table, new Set())
    map.get(table)!.add(colMatch[1])
  }
  jsonColsCache = { map, expires: Date.now() + META_CACHE_TTL }
  return map
}

export async function getTableMeta(table: string): Promise<ColumnMeta[]> {
  assertTable(table)
  const cached = metaCache.get(table)
  if (cached && cached.expires > Date.now()) {
    return cached.columns
  }

  const jsonCols = await getJsonColumns()

  const [rows] = await getPool().query<
    Array<{
      COLUMN_NAME: string
      DATA_TYPE: string
      COLUMN_TYPE: string
      IS_NULLABLE: string
      COLUMN_DEFAULT: string | null
      COLUMN_COMMENT: string
      COLUMN_KEY: string
    }>
  >(
    `SELECT COLUMN_NAME, DATA_TYPE, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT, COLUMN_COMMENT, COLUMN_KEY
     FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?
     ORDER BY ORDINAL_POSITION`,
    [table],
  )

  const jsonColsOfTable = jsonCols.get(table) || new Set<string>()
  const columns: ColumnMeta[] = rows.map((r) => {
    const comment = r.COLUMN_COMMENT || ''
    const dataType = r.DATA_TYPE
    return {
      name: r.COLUMN_NAME,
      dataType,
      columnType: r.COLUMN_TYPE,
      nullable: r.IS_NULLABLE === 'YES',
      defaultValue: r.COLUMN_DEFAULT,
      comment,
      isPrimary: r.COLUMN_KEY === 'PRI',
      isJson: /json/i.test(comment) || jsonColsOfTable.has(r.COLUMN_NAME) || JSON_NAME_HINT.test(r.COLUMN_NAME),
      isDate: dataType === 'date' || dataType === 'datetime' || dataType === 'timestamp',
      isNumber: ['int', 'bigint', 'smallint', 'tinyint', 'decimal', 'float', 'double'].includes(dataType),
      isLong: dataType === 'longtext' || dataType === 'mediumtext' || dataType === 'text' || dataType === 'blob',
    }
  })

  metaCache.set(table, { columns, expires: Date.now() + META_CACHE_TTL })
  return columns
}

/** 可用于 LIKE 搜索的列（排除 longtext / blob，避免大字段拖慢查询） */
export function searchableColumns(meta: ColumnMeta[]): string[] {
  return meta
    .filter((c) => !c.isPrimary && !c.isLong && !c.isDate && !c.isNumber && ['varchar', 'char', 'text'].includes(c.dataType))
    .map((c) => c.name)
}

/** 过滤请求体：只保留真实列名，自动跳过主键；'' 转为 NULL */
export function sanitizeRecord(meta: ColumnMeta[], body: Record<string, unknown>, skipId = true): Record<string, unknown> {
  const valid = new Set(meta.map((c) => c.name))
  const out: Record<string, unknown> = {}

  for (const col of meta) {
    if (skipId && col.isPrimary) continue
    if (!(col.name in body)) continue
    if (col.name in body && body[col.name] === undefined) continue

    let value = body[col.name]

    // 空字符串 -> NULL（对 JSON CHECK 约束字段尤其必要）
    if (value === '' || value === null) {
      out[col.name] = null
      continue
    }

    // JSON 字段：客户端提交字符串，服务端做二次校验
    if (col.isJson && typeof value === 'string') {
      try {
        JSON.parse(value)
      } catch {
        throw createError({ statusCode: 400, message: `字段 ${col.name} 不是合法的 JSON` })
      }
    }

    // 数字字段
    if (col.isNumber && typeof value === 'string' && value !== '') {
      const num = Number(value)
      if (Number.isNaN(num)) {
        throw createError({ statusCode: 400, message: `字段 ${col.name} 必须是数字` })
      }
      value = num
    }

    out[col.name] = value
  }

  return out
}

/** 分页排序校验 */
export function sanitizeSort(meta: ColumnMeta[], sort?: string, order?: string) {
  const sortCol = sort && meta.some((c) => c.name === sort) ? sort : 'id'
  const sortOrder = order === 'asc' || order === 'desc' ? order : 'desc'
  return { sortCol, sortOrder }
}

export async function pingDatabase(): Promise<{ ok: boolean; version?: string; error?: string }> {
  try {
    const [rows] = await getPool().query<Array<{ 'VERSION()': string }>>('SELECT VERSION() AS version')
    return { ok: true, version: rows[0]?.version }
  } catch (e) {
    return { ok: false, error: e instanceof Error ? e.message : String(e) }
  }
}
