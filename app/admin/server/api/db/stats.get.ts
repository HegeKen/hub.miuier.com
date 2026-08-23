import mysql from 'mysql2/promise'
import { getPool, pingDatabase, ALLOWED_TABLES } from '../../utils/db'

interface CountRow {
  cnt: string
}

async function count(table: string): Promise<number> {
  const [rows] = await getPool().query<CountRow[]>(`SELECT COUNT(*) AS cnt FROM \`${table}\``)
  return Number(rows[0]?.cnt || 0)
}

export default defineEventHandler(async () => {
  const [ping, roms, devices, branches, tables] = await Promise.all([
    pingDatabase(),
    count('roms'),
    count('devices'),
    count('branches'),
    getPool().query<Array<{ TABLE_NAME: string; TABLE_ROWS: string; DATA_LENGTH: string }>>(
      `SELECT TABLE_NAME, TABLE_ROWS, DATA_LENGTH
       FROM information_schema.TABLES
       WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME IN (?, ?, ?)`,
      ALLOWED_TABLES,
    ),
  ])

  // 各表最近记录
  const recent = async (table: string) => {
    const [rows] = await getPool().query(`SELECT * FROM \`${table}\` ORDER BY id DESC LIMIT 5`)
    return rows
  }

  // ROM 表的日期统计
  const [dateRows] = await getPool().query<Array<{ today: string; maxIns: string | null; maxUpdate: string | null }>>(
    `SELECT
       (SELECT COUNT(*) FROM roms WHERE insdate >= CURDATE()) AS today,
       (SELECT MAX(insdate) FROM roms) AS maxIns,
       (SELECT MAX(update_date) FROM roms) AS maxUpdate`,
  )

  const sizes = Object.fromEntries(
    (tables[0] as Array<{ TABLE_NAME: string; TABLE_ROWS: string; DATA_LENGTH: string }>).map((t) => [
      t.TABLE_NAME,
      { rows: Number(t.TABLE_ROWS || 0), bytes: Number(t.DATA_LENGTH || 0) },
    ]),
  )

  return {
    ping,
    counts: { roms, devices, branches },
    sizes,
    recent: {
      roms: await recent('roms'),
      devices: await recent('devices'),
      branches: await recent('branches'),
    },
    romsStats: {
      todayNew: Number(dateRows[0]?.today || 0),
      latestInsdate: dateRows[0]?.maxIns || null,
    },
  }
})
