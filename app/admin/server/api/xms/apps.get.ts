import { getPool } from '../../utils/db'

/**
 * 某一次 XMS 推送（xms.id）下的应用明细，供管理后台编辑。
 *
 * names / logs 是 longtext 存的 JSON，原样返回字符串，前端在文本框里编辑；
 * 保存仍走通用接口 PUT /api/db/xms_app/:id（服务端会校验 JSON 合法性）。
 */

function parseJson(value: unknown): unknown {
  if (typeof value !== 'string') return value ?? null
  if (!value.trim()) return null
  try {
    return JSON.parse(value)
  } catch {
    return value
  }
}

export default defineEventHandler(async (event) => {
  const query = getQuery(event)
  const xmsId = Number(query.xmsId)

  if (!Number.isInteger(xmsId) || xmsId <= 0) {
    throw createError({ statusCode: 400, message: 'xmsId 无效' })
  }

  const pool = getPool()

  const [pushRows] = await pool.query(
    'SELECT `id`, `device`, `code`, `version`, `rom_version`, `changelog` FROM `xms` WHERE `id` = ?',
    [xmsId],
  ) as [Array<Record<string, unknown>>, unknown]
  if (!pushRows.length) {
    throw createError({ statusCode: 404, message: `XMS 推送不存在 (id=${xmsId})` })
  }

  const [rows] = await pool.query(
    'SELECT * FROM `xms_app` WHERE `xms_id` = ? ORDER BY `name`, `pkg`, `id`',
    [xmsId],
  ) as [Array<Record<string, unknown>>, unknown]

  return {
    push: {
      id: Number(pushRows[0]!.id),
      device: String(pushRows[0]!.device ?? ''),
      code: String(pushRows[0]!.code ?? ''),
      version: String(pushRows[0]!.version ?? ''),
      romVersion: String(pushRows[0]!.rom_version ?? ''),
      changelog: parseJson(pushRows[0]!.changelog),
    },
    items: (rows || []).map((row) => ({
      ...row,
      id: Number(row.id),
      xms_id: Number(row.xms_id),
      size: row.size === null || row.size === undefined ? null : Number(row.size),
      namesObj: parseJson(row.names),
      logsObj: parseJson(row.logs),
    })),
  }
})
