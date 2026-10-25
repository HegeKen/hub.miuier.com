import { ensureModuleLog } from '../../utils/xms'

/**
 * 把 XMS 应用名写入 logs 的 module 行（不存在时创建）。
 *
 * POST /api/xms/module  { name, nameEn? }
 * → { ok, created, id }
 *
 * 应用名在 logs 里查不到时先占位，之后可在「日志录入」里补全其它语种。
 */
export default defineEventHandler(async (event) => {
  const body = await readBody<{ name?: string; nameEn?: string }>(event)
  const name = String(body?.name || '').trim()
  const nameEn = String(body?.nameEn || '').trim()

  if (!name) {
    throw createError({ statusCode: 400, message: '缺少应用名 name' })
  }

  const result = await ensureModuleLog(name, nameEn)
  return { ok: true, created: result.created, id: result.id }
})
