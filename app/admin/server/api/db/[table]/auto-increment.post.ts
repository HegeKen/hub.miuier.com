import type { PoolConnection } from 'mysql2/promise'
import { getPool, getTableMeta, assertTable } from '../../../utils/db'

/**
 * 重排某表的自增 ID：把已有 ID 压紧成 1..N，同步更新所有引用，并重置 AUTO_INCREMENT。
 *
 * body: { dryRun?: boolean }
 *
 * 引用关系：
 *   devices.id   → series.device_ids（JSON 数组）
 *   roms.id      → xms.rom_id
 *   xms.id       → xms_app.xms_id
 *   logs.id      → roms.logs / dev.logs（[[模块ID,[条目ID,...],掩码],...] 结构）
 * 其它表没有 ID 引用，直接重排即可。
 */

const JSON_TABLES: Record<string, Array<{ table: string; column: string }>> = {
  devices: [{ table: 'series', column: 'device_ids' }],
  logs: [
    { table: 'roms', column: 'logs' },
    { table: 'dev', column: 'logs' },
  ],
}

/** logs 引用结构只映射模块 ID 与条目 ID，第三位语言掩码保持原样 */
function mapLogsJson(raw: string, map: Map<number, number>): string | null {
  let parsed: unknown
  try {
    parsed = JSON.parse(raw)
  } catch {
    return null
  }
  if (!Array.isArray(parsed)) return null

  let changed = false
  const mapped = parsed.map((item) => {
    if (!Array.isArray(item)) return item
    const out = item.slice()
    if (typeof out[0] === 'number' && map.has(out[0])) {
      out[0] = map.get(out[0])!
      changed = true
    }
    if (Array.isArray(out[1])) {
      out[1] = out[1].map((id) => {
        if (typeof id === 'number' && map.has(id)) {
          changed = true
          return map.get(id)!
        }
        return id
      })
    }
    return out
  })
  return changed ? JSON.stringify(mapped) : null
}

function mapIdArray(raw: string, map: Map<number, number>): string | null {
  let parsed: unknown
  try {
    parsed = JSON.parse(raw)
  } catch {
    return null
  }
  if (!Array.isArray(parsed)) return null
  let changed = false
  const mapped = parsed.map((id) => {
    if (typeof id === 'number' && map.has(id)) {
      changed = true
      return map.get(id)!
    }
    return id
  })
  return changed ? JSON.stringify(mapped) : null
}

/** 分块更新一列 JSON（少量行也可用，避免一次拼太大的 SQL） */
async function updateJsonRows(
  conn: PoolConnection,
  table: string,
  column: string,
  rows: Array<{ id: number; value: string }>,
): Promise<number> {
  const chunkSize = 200
  for (let start = 0; start < rows.length; start += chunkSize) {
    const part = rows.slice(start, start + chunkSize)
    const cases = part.map(() => 'WHEN ? THEN ?').join(' ')
    const params: unknown[] = []
    for (const row of part) params.push(row.id, row.value)
    params.push(...part.map((row) => row.id))
    await conn.query(
      `UPDATE \`${table}\` SET \`${column}\` = CASE \`id\` ${cases} END `
      + `WHERE \`id\` IN (${part.map(() => '?').join(',')})`,
      params,
    )
  }
  return rows.length
}

/** 按表更新引用，返回受影响的引用条数 */
async function updateReferences(
  conn: PoolConnection,
  table: string,
  map: Map<number, number>,
): Promise<number> {
  let references = 0

  if (table === 'devices') {
    const [rows] = await conn.query(
      'SELECT `id`, `device_ids` FROM `series` WHERE `device_ids` IS NOT NULL AND `device_ids` <> \'\'',
    ) as [Array<{ id: number; device_ids: string }>, unknown]
    const changes: Array<{ id: number; value: string }> = []
    for (const row of rows || []) {
      const value = mapIdArray(String(row.device_ids), map)
      if (value !== null) changes.push({ id: Number(row.id), value })
    }
    references += await updateJsonRows(conn, 'series', 'device_ids', changes)
  }

  if (table === 'roms') {
    const [result] = await conn.query(
      'UPDATE `xms` x JOIN `_renumber_map` m ON x.`rom_id` = m.`old_id` SET x.`rom_id` = m.`new_id`',
    ) as [{ affectedRows: number }, unknown]
    references += Number(result?.affectedRows || 0)
  }

  if (table === 'xms') {
    const [result] = await conn.query(
      'UPDATE `xms_app` a JOIN `_renumber_map` m ON a.`xms_id` = m.`old_id` SET a.`xms_id` = m.`new_id`',
    ) as [{ affectedRows: number }, unknown]
    references += Number(result?.affectedRows || 0)
  }

  const jsonTargets = JSON_TABLES[table] || []
  for (const target of jsonTargets) {
    const [rows] = await conn.query(
      `SELECT \`id\`, \`${target.column}\` FROM \`${target.table}\` `
      + `WHERE \`${target.column}\` IS NOT NULL AND \`${target.column}\` <> \'\'`,
    ) as [Array<Record<string, unknown>>, unknown]
    const changes: Array<{ id: number; value: string }> = []
    for (const row of rows || []) {
      const raw = String(row[target.column] ?? '')
      const value = target.table === 'series'
        ? mapIdArray(raw, map)
        : mapLogsJson(raw, map)
      if (value !== null) changes.push({ id: Number(row.id), value })
    }
    references += await updateJsonRows(conn, target.table, target.column, changes)
  }

  return references
}

async function readAutoIncrement(table: string): Promise<number> {
  const [rows] = await getPool().query(`SHOW CREATE TABLE \`${table}\``) as [Array<Record<string, string>>, unknown]
  const ddl = rows?.[0]?.['Create Table'] || ''
  const match = ddl.match(/AUTO_INCREMENT=(\d+)/)
  return match ? Number(match[1]) : 0
}

export default defineEventHandler(async (event) => {
  const table = getRouterParam(event, 'table') || ''
  assertTable(table)

  const body = await readBody<{ dryRun?: boolean }>(event).catch(() => ({ dryRun: false }))
  const dryRun = Boolean(body?.dryRun)

  const columns = await getTableMeta(table)
  if (!columns.some((col) => col.isPrimary && col.name === 'id')) {
    throw createError({ statusCode: 400, message: `表 ${table} 没有 id 主键，无法重排自增` })
  }

  const beforeAutoIncrement = await readAutoIncrement(table)
  const pool = getPool()
  const conn = await pool.getConnection()
  let renumbered = 0
  let references = 0
  let count = 0

  try {
    await conn.beginTransaction()

    const [idRows] = await conn.query(
      `SELECT \`id\` FROM \`${table}\` ORDER BY \`id\` FOR UPDATE`,
    ) as [Array<{ id: number }>, unknown]

    const ids = (idRows || []).map((row) => Number(row.id))
    count = ids.length
    const map = new Map<number, number>()
    ids.forEach((oldId, index) => {
      const newId = index + 1
      map.set(oldId, newId)
      if (oldId !== newId) renumbered += 1
    })

    if (renumbered > 0) {
      await conn.query(
        'CREATE TEMPORARY TABLE `_renumber_map` (`old_id` BIGINT NOT NULL PRIMARY KEY, `new_id` BIGINT NOT NULL)',
      )
      const chunkSize = 1000
      for (let start = 0; start < ids.length; start += chunkSize) {
        const part = ids.slice(start, start + chunkSize)
        const values = part.map(() => '(?, ?)').join(', ')
        const params: unknown[] = []
        for (const oldId of part) params.push(oldId, map.get(oldId)!)
        await conn.query(`INSERT INTO \`_renumber_map\` (\`old_id\`, \`new_id\`) VALUES ${values}`, params)
      }

      const offset = (ids[ids.length - 1] ?? 0) + 1
      await conn.query(`UPDATE \`${table}\` SET \`id\` = \`id\` + ${offset}`)
      await conn.query(
        `UPDATE \`${table}\` t JOIN \`_renumber_map\` m ON t.\`id\` = m.\`old_id\` + ${offset} `
        + 'SET t.`id` = m.`new_id`',
      )

      references = await updateReferences(conn, table, map)
      await conn.query('DROP TEMPORARY TABLE `_renumber_map`')
    }

    if (dryRun) {
      await conn.rollback()
    } else {
      await conn.commit()
    }
  } catch (e) {
    try {
      await conn.rollback()
    } catch {
      /* ignore */
    }
    const message = e instanceof Error ? e.message : String(e)
    throw createError({ statusCode: 500, message: `重排失败：${message}` })
  } finally {
    conn.release()
  }

  let afterAutoIncrement = beforeAutoIncrement
  if (!dryRun) {
    // 重排后把计数器收到 N+1（ALTER 会隐式提交，所以放在事务外）
    await pool.query(`ALTER TABLE \`${table}\` AUTO_INCREMENT = ${count + 1}`)
    afterAutoIncrement = await readAutoIncrement(table)
  }

  return {
    ok: true,
    table,
    count,
    renumbered,
    references,
    beforeAutoIncrement,
    afterAutoIncrement,
    dryRun,
  }
})
