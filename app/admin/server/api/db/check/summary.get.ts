import { getSummaryCached } from '../../../utils/checker'

/** 全表自查计数汇总（dashboard 用，60s 缓存；忽略/恢复操作会自动失效缓存） */
export default defineEventHandler(async () => {
  return await getSummaryCached()
})
