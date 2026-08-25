import mysql from 'mysql2/promise'
import { getPool } from '../utils/db'

/**
 * 机型系列管理：聚合读取 series 表 + 设备基准行 id + 品牌归属。
 * - 返回全部 series（含 device_ids 解析后的设备详情）
 * - 返回全部设备（设备代号 + 基准行 id + 品牌 key），供系列编辑器按品牌选择/排序
 *
 * 写入走通用 CRUD：/api/db/series（POST/PUT/DELETE）。
 */

const BRAND_LABELS: Record<string, string> = {
  xiaomi: 'Xiaomi',
  redmi: 'Redmi',
  poco: 'POCO',
}

interface SeriesRow extends mysql.RowDataPacket {
  id: number
  brand: string | null
  name_zh: string | null
  name_en: string | null
  device_ids: string | null
  sort_order: number | null
}

interface DeviceRow extends mysql.RowDataPacket {
  ref_id: number
  device: string
  full_names: string | null
  names: string | null
  brands: string | null
  full_brands: string | null
}

/** JSON 字符串 → 对象，容错解析 */
function tryJson(raw: string | null): Record<string, unknown> | null {
  if (!raw) return null
  try {
    const obj = JSON.parse(raw)
    return obj && typeof obj === 'object' && !Array.isArray(obj) ? obj : null
  } catch {
    return null
  }
}

/** 品牌全称 → key：Xiaomi→xiaomi、Redmi→redmi、POCO→poco */
function brandKey(fullName: string): string | null {
  const v = fullName.toLowerCase()
  if (v.includes('xiaomi')) return 'xiaomi'
  if (v.includes('redmi')) return 'redmi'
  if (v.includes('poco')) return 'poco'
  return null
}

/** 从 brands / full_brands 推断设备的品牌 key 列表（去重保序） */
function brandKeysOf(row: DeviceRow): string[] {
  const out: string[] = []
  const add = (name: string) => {
    const key = brandKey(name)
    if (key && !out.includes(key)) out.push(key)
  }
  // full_brands: JSON 数组；brands: 逗号分隔字符串
  for (const raw of [row.full_brands, row.brands]) {
    if (!raw) continue
    const first = raw.trim()
    if (first.startsWith('[')) {
      try {
        const arr = JSON.parse(first.replace(/'/g, '"'))
        if (Array.isArray(arr)) arr.forEach((x) => typeof x === 'string' && add(x))
      } catch {
        /* 忽略 */
      }
    } else {
      first.split(',').forEach((x) => add(x.trim()))
    }
  }
  return out
}

function pickName(row: DeviceRow): string {
  for (const raw of [row.full_names, row.names]) {
    const obj = tryJson(raw)
    if (obj) {
      const v = obj.zh ?? obj.en
      if (typeof v === 'string' && v.trim()) return v.trim()
    }
  }
  return row.device
}

export default defineEventHandler(async () => {
  // 全部设备（每代号取基准行 id：优先 tag='CnOO'，否则最小 id）
  // 先按代号聚合出代表行 id，再回连 devices 取该行的业务字段，
  // 避免在 ONLY_FULL_GROUP_BY 模式下对非聚合列（full_names/brands 等）直接 SELECT
  const [devRows] = await getPool().query<DeviceRow[]>(
    `SELECT d.device, d.full_names, d.names, d.brands, d.full_brands, d.id AS ref_id
     FROM devices d
     JOIN (
       SELECT device,
              COALESCE(MAX(CASE WHEN tag = 'CnOO' THEN id END), MIN(id)) AS ref_id
       FROM devices
       GROUP BY device
     ) ref ON ref.device = d.device AND ref.ref_id = d.id
     ORDER BY d.device`,
  )

  // 基准行 id → 设备代号映射（用于解析 series.device_ids）
  // 由于 ref_id 是按代号聚合的，每代号只存一个代表 id
  const idToDevice = new Map<number, { id: number; device: string; name: string; brandKeys: string[] }>()
  for (const r of devRows) {
    const id = Number(r.ref_id)
    if (idToDevice.has(id)) continue
    idToDevice.set(id, {
      id,
      device: r.device,
      name: pickName(r),
      brandKeys: brandKeysOf(r),
    })
  }

  const devices = devRows.map((r) => ({
    id: Number(r.ref_id),
    device: r.device,
    name: pickName(r),
    brandKeys: brandKeysOf(r),
    brands: r.brands || '',
  }))

  // 全部 series，按品牌 → sort_order → id 排序
  const [seriesRows] = await getPool().query<SeriesRow[]>(
    `SELECT id, brand, name_zh, name_en, device_ids, sort_order
     FROM series
     ORDER BY sort_order, id`,
  )

  const series = seriesRows.map((s) => {
    let ids: number[] = []
    if (s.device_ids) {
      try {
        const arr = JSON.parse(s.device_ids)
        if (Array.isArray(arr)) ids = arr.map((x) => Number(x)).filter((x) => Number.isFinite(x))
      } catch {
        /* 忽略非法 JSON */
      }
    }
    const resolvedDevices = ids
      .map((id) => idToDevice.get(id))
      .filter((x): x is NonNullable<typeof x> => Boolean(x))
    return {
      id: s.id,
      brand: s.brand || '',
      name_zh: s.name_zh || '',
      name_en: s.name_en || '',
      sort_order: Number(s.sort_order || 0),
      device_ids: ids,
      devices: resolvedDevices,
    }
  })

  return {
    brands: Object.keys(BRAND_LABELS).map((key) => ({ key, label: BRAND_LABELS[key] })),
    devices,
    series,
  }
})
