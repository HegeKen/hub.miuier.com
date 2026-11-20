import { getPool } from '../../utils/db'

/**
 * 某一次 XMS 推送（xms.id）下的应用明细，供管理后台编辑。
 *
 * xms_app 已是推送 ↔ 发布物的薄关联表，元数据在 xms_app_release，
 * 这里 JOIN 出来返回发布物字段；id 为 xms_app 关联行 id（用于删除 / 定位），
 * release_id 指向共享发布物。names / logs 是 longtext 存的 JSON，原样返回字符串，
 * 前端在文本框里编辑；保存走 POST /api/xms/app（服务端 upsert 发布物 + 关联）。
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
    `SELECT a.\`id\`, a.\`xms_id\`, a.\`release_id\`, a.\`pkg\`,
            r.\`name\`, r.\`name_en\`, r.\`names\`, r.\`pkg_ver\`,
            r.\`apk_name\`, r.\`file_name\`, r.\`size\`, r.\`md5\`,
            r.\`sha256\`, r.\`logs\`, r.\`release_date\`
       FROM \`xms_app\` a
       JOIN \`xms_app_release\` r ON r.\`id\` = a.\`release_id\`
      WHERE a.\`xms_id\` = ?
      ORDER BY r.\`name\`, a.\`pkg\`, a.\`id\``,
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
