import { assertTable } from '../../../utils/db'
import { listDismissals } from '../../../utils/checker'

/** 已忽略列表：GET /api/db/check/dismissals?table=xxx（可选按表过滤） */
export default defineEventHandler(async (event) => {
  const query = getQuery(event)
  const table = typeof query.table === 'string' && query.table ? query.table : undefined
  if (table) assertTable(table)
  return { dismissals: await listDismissals(table as never) }
})
