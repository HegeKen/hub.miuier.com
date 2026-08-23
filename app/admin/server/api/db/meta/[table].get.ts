import { getTableMeta } from '../../../utils/db'

export default defineEventHandler(async (event) => {
  const table = getRouterParam(event, 'table') || ''
  return { table, columns: await getTableMeta(table) }
})
