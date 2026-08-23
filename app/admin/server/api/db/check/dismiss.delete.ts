import { assertTable } from '../../../utils/db'
import { removeDismissal, invalidateSummaryCache } from '../../../utils/checker'

/** 取消忽略：body = { table, ruleId, rowId? }（rowId 缺省为 0 = 整条规则） */
export default defineEventHandler(async (event) => {
  const body = await readBody(event)
  const table = typeof body?.table === 'string' ? body.table : ''
  const ruleId = typeof body?.ruleId === 'string' ? body.ruleId : ''
  if (!table || !ruleId) {
    throw createError({ statusCode: 400, message: '缺少 table 或 ruleId' })
  }
  assertTable(table)

  const rowId = body.rowId === undefined || body.rowId === null ? 0 : Number(body.rowId)
  const removed = await removeDismissal(table, ruleId, rowId)
  invalidateSummaryCache()
  return { ok: true, removed }
})
