import { assertTable, ALLOWED_TABLES } from '../../../utils/db'
import { runTableCheck, runAllSummary } from '../../../utils/checker'

/**
 * 数据自查：
 * - GET /api/db/check/:table          → 单表完整自查（含样本）
 * - GET /api/db/check/:table?summary=1 → 仅返回违规计数（不取样本）
 * - GET /api/db/check                  → 全表自查计数汇总
 */
export default defineEventHandler(async (event) => {
  const tableParam = getRouterParam(event, 'table')

  // 无 table 参数：全库汇总
  if (!tableParam) {
    const summary = await runAllSummary()
    return { executedAt: Date.now(), tables: summary }
  }

  assertTable(tableParam)
  const query = getQuery(event)
  const summaryOnly = query.summary === '1' || query.summary === 'true'

  // 兼容旧式路径 /api/db/check?table=xxx（可选）
  if (tableParam === 'all') {
    const summary = await runAllSummary()
    return { executedAt: Date.now(), tables: summary }
  }

  return await runTableCheck(tableParam, summaryOnly)
})

// 供 /api/db/check/summary 使用的导出（静态路由优先于 [table]）
export const summaryTables = ALLOWED_TABLES
