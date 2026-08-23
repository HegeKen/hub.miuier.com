/** 管理后台 API 封装：全部直连 MySQL（经 Nitro 服务端路由） */

export interface ListParams {
  page?: number
  pageSize?: number
  search?: string
  sort?: string
  order?: 'asc' | 'desc'
}

export interface ListResult<T = Record<string, unknown>> {
  table: string
  rows: T[]
  total: number
  page: number
  pageSize: number
  sort: string
  order: string
}

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

export type CheckSeverity = 'error' | 'warning'

export interface CheckRuleResult {
  id: string
  name: string
  column: string
  description: string
  severity: CheckSeverity
  total: number
  samples: Array<Record<string, unknown>>
  sampleCols?: string[]
  /** 整条规则被忽略 */
  dismissed?: boolean
}

export interface Dismissal {
  id: number
  table_name: string
  rule_id: string
  /** 0 表示整条规则，>0 表示单条记录 */
  row_id: number
  reason: string | null
  created_at: number
}

export interface TableCheckReport {
  table: string
  executedAt: number
  summary: { total: number; errors: number; warnings: number; dismissed: number }
  rules: CheckRuleResult[]
  dismissals: Dismissal[]
}

export interface CheckSummary {
  executedAt: number
  tables: Array<{ table: string; summary: { total: number; errors: number; warnings: number; dismissed: number } }>
}

/** 从 $fetch 错误中提取可读信息 */
export function errorMessage(e: unknown): string {
  if (e && typeof e === 'object') {
    const data = (e as { data?: { statusMessage?: string; message?: string } }).data
    if (data?.message) return data.message
    if (data?.statusMessage) return data.statusMessage
    const msg = (e as { message?: string }).message
    if (msg) return msg
  }
  return String(e)
}

export function useAdminApi() {
  const listTable = async <T = Record<string, unknown>>(table: string, params: ListParams = {}) => {
    return $fetch<ListResult<T>>(`/api/db/${table}`, { params })
  }

  const getTableMeta = async (table: string) => {
    return $fetch<{ table: string; columns: ColumnMeta[] }>(`/api/db/meta/${table}`)
  }

  const getRecord = async (table: string, id: number | string) => {
    return $fetch<Record<string, unknown>>(`/api/db/${table}/${id}`)
  }

  const createRecord = async (table: string, data: Record<string, unknown>) => {
    return $fetch(`/api/db/${table}`, { method: 'POST', body: data })
  }

  const updateRecord = async (table: string, id: number | string, data: Record<string, unknown>) => {
    return $fetch(`/api/db/${table}/${id}`, { method: 'PUT', body: data })
  }

  const deleteRecord = async (table: string, id: number | string) => {
    return $fetch(`/api/db/${table}/${id}`, { method: 'DELETE' })
  }

  const runSql = async (sql: string) => {
    return $fetch<{ ok: boolean; duration: number; fields: string[]; rows: unknown[]; rowCount: number; affectedRows: number }>(
      '/api/db/sql',
      { method: 'POST', body: { sql } },
    )
  }

  const getStats = async () => {
    return $fetch('/api/db/stats')
  }

  const runCheck = async (table: string, summaryOnly = false) => {
    return $fetch<TableCheckReport>(`/api/db/check/${table}`, { params: summaryOnly ? { summary: '1' } : {} })
  }

  const getCheckSummary = async () => {
    return $fetch<CheckSummary>('/api/db/check/summary')
  }

  /** 忽略违规：rowId 缺省为 0 = 整条规则；>0 = 单条记录 */
  const dismissViolation = async (table: string, ruleId: string, opts: { rowId?: number | string; reason?: string } = {}) => {
    return $fetch('/api/db/check/dismiss', { method: 'POST', body: { table, ruleId, ...opts } })
  }

  /** 取消忽略 */
  const restoreViolation = async (table: string, ruleId: string, rowId?: number | string) => {
    return $fetch('/api/db/check/dismiss', { method: 'DELETE', body: { table, ruleId, rowId } })
  }

  /** 已忽略列表（可按表过滤） */
  const listDismissals = async (table?: string) => {
    return $fetch<{ dismissals: Dismissal[] }>('/api/db/check/dismissals', { params: table ? { table } : {} })
  }

  return {
    listTable,
    getTableMeta,
    getRecord,
    createRecord,
    updateRecord,
    deleteRecord,
    runSql,
    getStats,
    runCheck,
    getCheckSummary,
    dismissViolation,
    restoreViolation,
    listDismissals,
  }
}
