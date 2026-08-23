import { assertTable } from '../../../utils/db'
import { addDismissal, invalidateSummaryCache } from '../../../utils/checker'

/**
 * 忽略违规：body = { table, ruleId, rowId?, reason? }
 * - rowId 缺省为 0 → 忽略整条规则
 * - rowId > 0 → 忽略该规则下的单条记录
 */
export default defineEventHandler(async (event) => {
  const body = await readBody(event)
  const table = typeof body?.table === 'string' ? body.table : ''
  const ruleId = typeof body?.ruleId === 'string' ? body.ruleId : ''
  if (!table || !ruleId) {
    throw createError({ statusCode: 400, message: '缺少 table 或 ruleId' })
  }
  assertTable(table)

  const rowId = body.rowId === undefined || body.rowId === null ? 0 : Number(body.rowId)
  const reason = typeof body?.reason === 'string' ? body.reason : ''

  const dismissal = await addDismissal(table, ruleId, rowId, reason)
  invalidateSummaryCache()
  return { ok: true, dismissal }
})
