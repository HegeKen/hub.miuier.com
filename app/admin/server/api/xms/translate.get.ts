import { moduleTranslations } from '../../utils/xms'

/**
 * 用 logs 表补全 XMS 应用名的其它语言。
 *
 * GET /api/xms/translate?name=手机管家&nameEn=Security
 * → { found, translations: { zh, zh_tw, en, ja, ... } }
 */
export default defineEventHandler(async (event) => {
  const query = getQuery(event)
  const name = typeof query.name === 'string' ? query.name.trim() : ''
  const nameEn = typeof query.nameEn === 'string' ? query.nameEn.trim() : ''

  if (!name && !nameEn) {
    throw createError({ statusCode: 400, message: '请先填写中文名或英文名' })
  }

  const translations = await moduleTranslations(name, nameEn)
  return {
    found: Object.keys(translations).length > 0,
    translations,
  }
})
