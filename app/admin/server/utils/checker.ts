import mysql from 'mysql2/promise'
import { getPool, ALLOWED_TABLES, type AllowedTable } from './db'

/**
 * 数据自查引擎：依据 data/db_structure/*.sql 中每列的注释与示例，
 * 定义「不符合逻辑」的判定规则（SQL WHERE 片段，仅引用固定列名），
 * 逐表扫描并返回违规计数与样本。
 *
 * 支持「忽略（dismiss）」：特殊情况可忽略单条记录（row_id）或整条规则
 * （row_id = 0），忽略后不再计入违规。忽略记录持久化在 check_dismissals 表。
 */

export type CheckSeverity = 'error' | 'warning'

export interface CheckRule {
  id: string
  name: string
  column: string
  description: string
  severity: CheckSeverity
  /** WHERE 片段（不含 WHERE 关键字），命中即视为违规 */
  violation: string
  /** 样本查询额外展示的列（默认取 column） */
  sampleCols?: string[]
}

export interface CheckResult extends CheckRule {
  total: number
  samples: Array<Record<string, unknown>>
  /** 整条规则被忽略 */
  dismissed?: boolean
}

export interface TableCheckReport {
  table: AllowedTable
  executedAt: number
  summary: { total: number; errors: number; warnings: number; dismissed: number }
  rules: CheckResult[]
  dismissals: Dismissal[]
}

export interface Dismissal extends mysql.RowDataPacket {
  id: number
  table_name: string
  rule_id: string
  /** 0 表示整条规则，>0 表示单条记录 */
  row_id: number
  reason: string | null
  created_at: number
}

/** 违规计数查询结果行（COUNT(*) 返回字符串） */
interface CountRow extends mysql.RowDataPacket {
  total: string
}

/** 样本查询结果行（列不固定，动态拼接） */
interface SampleRow extends mysql.RowDataPacket {}

export const DISMISSALS_TABLE = 'check_dismissals'

/** 各表样本上下文字段（便于识别具体记录） */
const CONTEXT_COLS: Record<AllowedTable, string[]> = {
  devices: ['device', 'code'],
  branches: ['branch', 'name_zh'],
  roms: ['device', 'version'],
  series: ['brand', 'name_zh'],
}

/* ---------------- 规则定义 ---------------- */

const RULE_TAG =
  "tag IS NOT NULL AND tag NOT REGEXP '^[A-Z][A-Z0-9]{2,4}$' AND tag NOT REGEXP '^Cn[A-Z0-9]{2,3}$' AND tag NOT IN ('Dev','GDev','Beta','STAN') AND tag NOT REGEXP '^EP'"

const RULE_REGION = "region IS NOT NULL AND region <> '' AND region <> 'None' AND region NOT REGEXP '^[a-z]{2,6}$'"

const RULE_DEVICE_NAME = "device IS NOT NULL AND device NOT REGEXP '^[a-z0-9_]+$'"

const RULE_CODE_NAME = "code IS NOT NULL AND code NOT REGEXP '^[a-z0-9_]+$'"

const RULE_CARRIER = "carrier IS NOT NULL AND NOT (LEFT(carrier,1)='[' AND RIGHT(carrier,1)=']')"

const JSON_RULE = (col: string, desc: string): CheckRule => ({
  id: `json_${col}`,
  name: `${col} 应为合法 JSON`,
  column: col,
  description: desc,
  severity: 'error',
  violation: `${col} IS NOT NULL AND ${col} <> '' AND JSON_VALID(${col})=0`,
})

export const CHECK_RULES: Record<AllowedTable, CheckRule[]> = {
  devices: [
    {
      id: 'device_format',
      name: 'device 格式',
      column: 'device',
      description: '设备代号应为小写字母/数字/下划线，如 marble、mione_plus',
      severity: 'error',
      violation: RULE_DEVICE_NAME,
    },
    {
      id: 'code_format',
      name: 'code 格式',
      column: 'code',
      description: '设备代码应为小写字母/数字/下划线，如 marble_global、leedsa_in_global',
      severity: 'error',
      violation: RULE_CODE_NAME,
    },
    {
      id: 'devtag_format',
      name: 'devtag 格式',
      column: 'devtag',
      description: '设备内部标识如 MA、Mioneplus',
      severity: 'error',
      violation: "devtag IS NOT NULL AND devtag NOT REGEXP '^[A-Za-z0-9_]+$'",
    },
    {
      id: 'tag_format',
      name: 'tag 格式',
      column: 'tag',
      description: 'ROM 标签如 CnOO、MIXM、INXM（Dev/GDev/Beta 等开发标签、EP 政企版标签、STAN 原生安卓除外）',
      severity: 'error',
      violation: RULE_TAG,
    },
    {
      id: 'region_format',
      name: 'region 格式',
      column: 'region',
      description: '区域代号为小写字母，如 cn/global/in/eea/tw（Android One 机型为 None，属正常）',
      severity: 'error',
      violation: RULE_REGION,
    },
    {
      id: 'devcode_length',
      name: 'devcode 长度',
      column: 'devcode',
      description: '设备版本号后 6 位，如 AVCNXM',
      severity: 'error',
      violation: "devcode IS NOT NULL AND devcode NOT REGEXP '^[A-Z0-9]{6}$'",
    },
    {
      id: 'carrier_structure',
      name: 'carrier 结构',
      column: 'carrier',
      description: '运营商列表应为数组（兼容旧式 [\'\',...] 写法）',
      severity: 'warning',
      violation: RULE_CARRIER,
    },
    JSON_RULE('full_brands', '品牌全名应为 JSON 数组，如 ["Xiaomi","Redmi"]'),
    {
      id: 'json_brands',
      name: 'brands 应为合法 JSON',
      column: 'brands',
      description: '品牌简称应为 JSON 数组（空数组 [] 视为异常，设备至少属于一个品牌）',
      severity: 'error',
      violation: "brands IS NOT NULL AND brands <> '' AND (JSON_VALID(brands)=0 OR TRIM(brands)='[]')",
    },
    JSON_RULE('full_names', '设备全名应为 JSON 对象，如 {"zh":"小米14","en":"Xiaomi 14"}'),
    JSON_RULE('names', '设备简称应为 JSON 对象'),
    JSON_RULE('xiaomi', '小米品牌设备名应为 JSON 对象'),
    JSON_RULE('redmi', 'Redmi 品牌设备名应为 JSON 对象'),
    JSON_RULE('poco', 'POCO 品牌设备名应为 JSON 对象'),
    {
      id: 'image_path',
      name: 'image 路径',
      column: 'image',
      description: '设备图片应为路径（/ 开头或 http(s):// 开头），不宜为空字符串',
      severity: 'warning',
      violation: "image IS NOT NULL AND (image='' OR image NOT REGEXP '^(/|https?://)')",
    },
  ],

  branches: [
    {
      id: 'branch_enum',
      name: 'branch 枚举',
      column: 'branch',
      description: '分支类型：F=正式版、X=开发版、D=每日构建版',
      severity: 'error',
      violation: "branch IS NOT NULL AND branch NOT IN ('F','X','D')",
    },
    {
      id: 'tag_format',
      name: 'tag 格式',
      column: 'tag',
      description: '分支标签如 CnOO、MIXM（Dev/Beta 等开发标签、EP 政企版标签、STAN 原生安卓除外）',
      severity: 'error',
      violation: RULE_TAG,
    },
    {
      id: 'region_format',
      name: 'region 格式',
      column: 'region',
      description: '区域代号为小写字母，如 cn/global/in/eea（Android One 机型为 None，属正常）',
      severity: 'error',
      violation: RULE_REGION,
    },
    {
      id: 'zone_enum',
      name: 'zone 枚举',
      column: 'zone',
      description: '区域分区：1=中国、2=国际',
      severity: 'error',
      violation: 'zone IS NOT NULL AND zone NOT IN (1,2)',
    },
    {
      id: 'visibility_enum',
      name: 'visibility 枚举',
      column: 'visibility',
      description: '是否可见：1=可见、0=隐藏',
      severity: 'error',
      violation: 'visibility IS NOT NULL AND visibility NOT IN (0,1)',
    },
    {
      id: 'ep_enum',
      name: 'ep 枚举',
      column: 'ep',
      description: '是否政企版：1=是、0=否',
      severity: 'error',
      violation: 'ep IS NOT NULL AND ep NOT IN (0,1)',
    },
    {
      id: 'code_format',
      name: 'code 格式',
      column: 'code',
      description: '分支代码后缀以下划线开头，如 _global、_in_global、_ep_stdee',
      severity: 'warning',
      violation: "code IS NOT NULL AND code NOT REGEXP '^_[a-z0-9_]+$'",
    },
    {
      id: 'vercode_format',
      name: 'vercode 格式',
      column: 'vercode',
      description: '版本代码 4 位，如 CNAL、CNDM（用于匹配新设备分支）',
      severity: 'error',
      violation: "vercode IS NOT NULL AND vercode NOT REGEXP '^[A-Z0-9]{4}$'",
    },
    {
      id: 'carrier_structure',
      name: 'carrier 结构',
      column: 'carrier',
      description: '运营商列表应为数组（兼容旧式 [\'\',...] 写法）',
      severity: 'warning',
      violation: RULE_CARRIER,
    },
    {
      id: 'name_empty',
      name: '名称空字符串',
      column: 'name_zh/name_en',
      description: '分支中英文名称不应为空字符串（应使用 NULL）',
      severity: 'error',
      violation: "(name_zh IS NOT NULL AND name_zh='') OR (name_en IS NOT NULL AND name_en='')",
      sampleCols: ['name_zh', 'name_en'],
    },
  ],

  roms: [
    {
      id: 'device_format',
      name: 'device 格式',
      column: 'device',
      description: '设备代号应为小写字母/数字/下划线，如 taiko、leedsa',
      severity: 'error',
      violation: RULE_DEVICE_NAME,
    },
    {
      id: 'code_format',
      name: 'code 格式',
      column: 'code',
      description: '设备代码应为小写字母/数字/下划线，如 taiko_global',
      severity: 'error',
      violation: RULE_CODE_NAME,
    },
    {
      id: 'type_enum',
      name: 'type 枚举',
      column: 'type',
      description: '系统类型：MIUI / HyperOS、STAN=原生安卓（Android One）',
      severity: 'error',
      violation: "type IS NOT NULL AND type NOT IN ('MIUI','HyperOS','STAN')",
    },
    {
      id: 'bigver_format',
      name: 'bigver 格式',
      column: 'bigver',
      description: '大版本号如 HyperOS 3、MIUI 14（兼容 MIUI V3、STAN A15、Stock）',
      severity: 'error',
      violation:
        "bigver IS NOT NULL AND bigver <> '' AND bigver NOT REGEXP '^((MIUI|HyperOS) ?[0-9]+(\\.[0-9]+)?|MIUI V[0-9]+|STAN A[0-9]+|Stock)$'",
    },
    {
      id: 'branch_enum',
      name: 'branch 枚举',
      column: 'branch',
      description: '分支类型：F=正式版、X=开发版、D=每日构建版',
      severity: 'error',
      violation: "branch IS NOT NULL AND branch NOT IN ('F','X','D')",
    },
    {
      id: 'zone_enum',
      name: 'zone 枚举',
      column: 'zone',
      description: '区域分区：1=中国、2=国际',
      severity: 'error',
      violation: 'zone IS NOT NULL AND zone NOT IN (1,2)',
    },
    {
      id: 'region_format',
      name: 'region 格式',
      column: 'region',
      description: '区域代号为小写字母，如 cn/global/in/eea（Android One 机型为 None，属正常）',
      severity: 'error',
      violation: RULE_REGION,
    },
    {
      id: 'tag_format',
      name: 'tag 格式',
      column: 'tag',
      description: 'ROM 标签如 CnOO、MIXM（Dev/GDev/Beta 等开发标签、EP 政企版标签、STAN 原生安卓除外）',
      severity: 'error',
      violation: RULE_TAG,
    },
    {
      id: 'version_format',
      name: 'version 格式',
      column: 'version',
      description:
        '完整版本号如 OS3.0.303.0.WOVCNXM、V14.0.2.0.TKFEUXM（政企版 EP、开发者预览版 DP、DEV/BETA 开发体验版、MIUI 前期纯数字开发版/体验版除外）',
      severity: 'warning',
      violation:
        "version IS NOT NULL AND version NOT REGEXP '^(V|OS|A)[0-9]+(\\.[0-9]+){2,}\\.[A-Z0-9]{4,}$' AND version NOT REGEXP '^[0-9.]+[a-z]?$' AND version NOT REGEXP '^[A-Z]{2}[A-Z0-9]+(\\.[0-9]+)?$' AND version NOT LIKE '%.DEV' AND version NOT LIKE '%.BETA' AND version NOT LIKE '%EP%' AND version NOT LIKE '%DP%'",
    },
    {
      id: 'android_format',
      name: 'android 格式',
      column: 'android',
      description: 'Android 版本号如 16.0、14.0、8.1',
      severity: 'error',
      violation: "android IS NOT NULL AND android NOT REGEXP '^[0-9]+(\\.[0-9]+){1,2}$'",
    },
    {
      id: 'empty_strings',
      name: '空字符串字段',
      column: '多字段',
      description: 'region/fastboot/recovery/ctelecom/cmobile/cunicom/others 不应为空字符串（应使用 NULL）',
      severity: 'error',
      violation:
        "region='' OR fastboot='' OR recovery='' OR ctelecom='' OR cmobile='' OR cunicom='' OR others=''",
      sampleCols: ['region', 'fastboot', 'recovery', 'ctelecom', 'cmobile', 'cunicom', 'others'],
    },
    JSON_RULE('logs_zh', '中文更新日志应为合法 JSON 对象'),
    JSON_RULE('logs_en', '英文更新日志应为合法 JSON 对象'),
    {
      id: 'package_filename',
      name: '包文件名',
      column: 'recovery/fastboot',
      description: 'Recovery / Fastboot 包文件名应以 .zip 或 .tgz 结尾（MIUI 早期曾发布 .exe 格式，属正常）',
      severity: 'warning',
      violation:
        "(recovery IS NOT NULL AND recovery <> '' AND recovery NOT LIKE '%.zip' AND recovery NOT LIKE '%.tgz' AND recovery NOT LIKE '%.exe') OR (fastboot IS NOT NULL AND fastboot <> '' AND fastboot NOT LIKE '%.zip' AND fastboot NOT LIKE '%.tgz' AND fastboot NOT LIKE '%.exe')",
      sampleCols: ['recovery', 'fastboot'],
    },
    {
      id: 'update_date',
      name: 'update_date 时间戳',
      column: 'update_date',
      description: '记录更新时间戳应为正数（bigint）',
      severity: 'error',
      violation: 'update_date IS NOT NULL AND update_date <= 0',
    },
  ],

  series: [
    {
      id: 'brand_enum',
      name: 'brand 枚举',
      column: 'brand',
      description: '系列所属品牌：xiaomi / redmi / poco',
      severity: 'error',
      violation: "brand IS NOT NULL AND brand NOT IN ('xiaomi','redmi','poco')",
    },
    {
      id: 'name_empty',
      name: '名称空字符串',
      column: 'name_zh/name_en',
      description: '系列中英文名称不应为空字符串（应使用 NULL）',
      severity: 'error',
      violation: "(name_zh IS NOT NULL AND name_zh='') OR (name_en IS NOT NULL AND name_en='')",
      sampleCols: ['name_zh', 'name_en'],
    },
    JSON_RULE('device_ids', 'device_ids 应为合法 JSON 数组，如 [12,34,56]'),
    {
      id: 'device_ids_empty',
      name: 'device_ids 为空数组',
      column: 'device_ids',
      description: '系列应至少归属一台设备；空数组 [] 属异常（可先建系列再补设备）',
      severity: 'warning',
      violation: "device_ids IS NOT NULL AND TRIM(device_ids) = '[]'",
    },
  ],
}

/* ---------------- 忽略（dismiss）管理 ---------------- */

let dismissalsTableEnsured = false

export async function ensureDismissalsTable(): Promise<void> {
  if (dismissalsTableEnsured) return
  await getPool().query(
    `CREATE TABLE IF NOT EXISTS \`${DISMISSALS_TABLE}\` (
       id BIGINT NOT NULL AUTO_INCREMENT,
       table_name VARCHAR(64) NOT NULL,
       rule_id VARCHAR(64) NOT NULL,
       row_id BIGINT NOT NULL DEFAULT 0,
       reason VARCHAR(500) DEFAULT NULL,
       created_at BIGINT NOT NULL,
       PRIMARY KEY (id),
       UNIQUE KEY uq_dismiss (table_name, rule_id, row_id)
     ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin`,
  )
  dismissalsTableEnsured = true
}

/** 忽略：rowId 缺省为 0（整条规则），>0 为单条记录 */
export async function addDismissal(
  table: AllowedTable,
  ruleId: string,
  rowId = 0,
  reason = '',
): Promise<Dismissal> {
  const rule = CHECK_RULES[table]?.find((r) => r.id === ruleId)
  if (!rule) {
    throw createError({ statusCode: 400, message: `表 ${table} 不存在规则 ${ruleId}` })
  }
  await ensureDismissalsTable()
  await getPool().query(
    `INSERT INTO \`${DISMISSALS_TABLE}\` (table_name, rule_id, row_id, reason, created_at)
     VALUES (?, ?, ?, ?, ?)
     ON DUPLICATE KEY UPDATE reason = VALUES(reason)`,
    [table, ruleId, Number(rowId) || 0, String(reason || '').slice(0, 500), Date.now()],
  )
  // upsert 刚写入成功，该记录必然存在
  return (await listDismissals(table, ruleId, Number(rowId) || 0))[0]!
}

export async function removeDismissal(table: AllowedTable, ruleId: string, rowId = 0): Promise<boolean> {
  await ensureDismissalsTable()
  const [result] = await getPool().query<mysql.ResultSetHeader>(
    `DELETE FROM \`${DISMISSALS_TABLE}\` WHERE table_name = ? AND rule_id = ? AND row_id = ?`,
    [table, ruleId, Number(rowId) || 0],
  )
  return result.affectedRows > 0
}

export async function listDismissals(table?: AllowedTable, ruleId?: string, rowId?: number): Promise<Dismissal[]> {
  await ensureDismissalsTable()
  const conditions: string[] = []
  const params: unknown[] = []
  if (table) {
    conditions.push('table_name = ?')
    params.push(table)
  }
  if (ruleId) {
    conditions.push('rule_id = ?')
    params.push(ruleId)
  }
  if (rowId !== undefined) {
    conditions.push('row_id = ?')
    params.push(rowId)
  }
  const where = conditions.length ? `WHERE ${conditions.join(' AND ')}` : ''
  const [rows] = await getPool().query<Dismissal[]>(
    `SELECT id, table_name, rule_id, row_id, reason, created_at
     FROM \`${DISMISSALS_TABLE}\` ${where}
     ORDER BY id DESC`,
    params,
  )
  return rows
}

/* ---------------- 执行 ---------------- */

const SAMPLE_LIMIT = 50

export async function runTableCheck(table: AllowedTable, summaryOnly = false): Promise<TableCheckReport> {
  await ensureDismissalsTable()
  const rules = CHECK_RULES[table]
  const context = CONTEXT_COLS[table]
  const dismissals = await listDismissals(table)
  const ruleDismissed = new Set(dismissals.filter((d) => d.row_id === 0).map((d) => d.rule_id))

  // 行级忽略：为每个规则生成 NOT EXISTS 关联条件
  const dismissalClause = `AND NOT EXISTS (
    SELECT 1 FROM \`${DISMISSALS_TABLE}\` d
    WHERE d.table_name = ? AND d.rule_id = ? AND d.row_id = t.id
  )`

  const results: CheckResult[] = await Promise.all(
    rules.map(async (rule) => {
      if (ruleDismissed.has(rule.id)) {
        return { ...rule, total: 0, samples: [], dismissed: true }
      }

      const [countRows] = await getPool().query<CountRow[]>(
        `SELECT COUNT(*) AS total FROM \`${table}\` t WHERE ${rule.violation} ${dismissalClause}`,
        [table, rule.id],
      )
      const total = Number(countRows[0]?.total || 0)

      let samples: Array<Record<string, unknown>> = []
      if (!summaryOnly && total > 0) {
        const sampleCols = (rule.sampleCols || [rule.column]).map((c) => `\`${c}\``)
        const [rows] = await getPool().query<SampleRow[]>(
          `SELECT t.id, ${context.join(', ')}, ${sampleCols.join(', ')}
           FROM \`${table}\` t
           WHERE ${rule.violation} ${dismissalClause}
           ORDER BY t.id DESC
           LIMIT ${SAMPLE_LIMIT}`,
          [table, rule.id],
        )
        samples = rows
      }

      return { ...rule, total, samples }
    }),
  )

  const summary = results.reduce(
    (acc, r) => {
      if (r.dismissed) {
        acc.dismissed += 1
      } else {
        acc.total += r.total
        if (r.severity === 'error') acc.errors += r.total
        else acc.warnings += r.total
      }
      return acc
    },
    { total: 0, errors: 0, warnings: 0, dismissed: 0 },
  )

  return { table, executedAt: Date.now(), summary, rules: results, dismissals }
}

export async function runAllSummary(): Promise<
  Array<{ table: AllowedTable; summary: TableCheckReport['summary'] }>
> {
  const reports = await Promise.all(ALLOWED_TABLES.map((t) => runTableCheck(t, true)))
  return reports.map((r) => ({ table: r.table, summary: r.summary }))
}

/* ---------------- 汇总缓存（供 dismiss 操作失效） ---------------- */

let summaryCacheData: unknown = null
let summaryCacheExpires = 0
const SUMMARY_CACHE_TTL = 60_000

/** 全表自查计数汇总（带 60s 缓存） */
export async function getSummaryCached(): Promise<unknown> {
  if (summaryCacheData !== null && summaryCacheExpires > Date.now()) {
    return summaryCacheData
  }
  const tables = await runAllSummary()
  const data = { executedAt: Date.now(), tables }
  summaryCacheData = data
  summaryCacheExpires = Date.now() + SUMMARY_CACHE_TTL
  return data
}

/** 忽略/恢复后使汇总缓存失效 */
export function invalidateSummaryCache(): void {
  summaryCacheData = null
  summaryCacheExpires = 0
}
