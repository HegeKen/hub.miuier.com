import { getPool } from '../../utils/db'
import { zoneOf } from '../../utils/dev'

/**
 * 开发版绑定 / 解绑 ROM：POST /api/dev/bind
 *
 * body:
 *   week   目标周数
 *   region cn / global
 *   action bind | unbind
 *   romIds number[]（bind 必填；unbind 省略则解绑该周全部）
 *   force  true 时允许把已绑定到其他周的 ROM 抢过来（默认跳过并回报）
 *
 * 绑定即把 roms.dev_week 置为该周（分区不一致的行会回报而不是硬绑），
 * 解绑即置回 NULL——dev 表本身不存机型清单，导出时按 dev_week 现算。
 */
export default defineEventHandler(async (event) => {
  const body = await readBody(event)
  if (!body || typeof body !== 'object') {
    throw createError({ statusCode: 400, message: '请求体无效' })
  }
  const week = Number(body.week)
  const region = String(body.region || 'cn') === 'global' ? 'global' : 'cn'
  const action = String(body.action || 'bind')
  const force = body.force === true
  const romIds = Array.isArray(body.romIds)
    ? [...new Set(body.romIds.map((id: unknown) => Number(id)).filter((id: number) => Number.isFinite(id) && id > 0))]
    : []

  if (!Number.isFinite(week) || week <= 0) {
    throw createError({ statusCode: 400, message: 'week 参数无效' })
  }
  if (action !== 'bind' && action !== 'unbind') {
    throw createError({ statusCode: 400, message: 'action 只能是 bind 或 unbind' })
  }
  if (action === 'bind' && !romIds.length) {
    throw createError({ statusCode: 400, message: 'bind 需要提供 romIds' })
  }

  const pool = getPool()
  const [devRows] = await pool.query<Array<{ zone: unknown }>>(
    'SELECT `zone` FROM `dev` WHERE `week` = ? AND `region` = ? LIMIT 1',
    [week, region],
  )
  if (!devRows?.length) {
    throw createError({ statusCode: 404, message: `第 ${week} 周（${region}）的开发版记录不存在，请先导入该周数据` })
  }
  const zone = zoneOf(region, devRows[0]!.zone)

  if (action === 'unbind') {
    const filter = romIds.length ? ` AND \`id\` IN (${romIds.map(() => '?').join(',')})` : ''
    const [result] = await pool.query<{ affectedRows: number }>(
      `UPDATE \`roms\` SET \`dev_week\` = NULL WHERE \`dev_week\` = ? AND \`zone\` = ?${filter}`,
      [week, zone, ...romIds],
    )
    return { ok: true, action, updated: Number(result?.affectedRows ?? 0), skipped: 0 }
  }

  // bind：分区不一致的直接拒绝，已绑到别周的按 force 决定是覆盖还是跳过
  const [rows] = await pool.query<Array<{ id: number; zone: number; dev_week: number | null }>>(
    `SELECT \`id\`, \`zone\`, \`dev_week\` FROM \`roms\` WHERE \`id\` IN (${romIds.map(() => '?').join(',')})`,
    romIds,
  )
  const wrongZone: number[] = []
  const takenIds: number[] = []
  const bindIds: number[] = []
  for (const row of rows || []) {
    const id = Number(row.id)
    if (Number(row.zone ?? 1) !== zone) {
      wrongZone.push(id)
      continue
    }
    const owner = row.dev_week === null || row.dev_week === undefined ? null : Number(row.dev_week)
    if (owner !== null && owner !== week && !force) {
      takenIds.push(id)
      continue
    }
    bindIds.push(id)
  }

  if (bindIds.length) {
    await pool.query(
      `UPDATE \`roms\` SET \`dev_week\` = ? WHERE \`id\` IN (${bindIds.map(() => '?').join(',')})`,
      [week, ...bindIds],
    )
  }

  return {
    ok: true,
    action,
    updated: bindIds.length,
    skipped: takenIds.length + wrongZone.length,
    skippedTaken: takenIds,
    skippedZone: wrongZone,
  }
})
