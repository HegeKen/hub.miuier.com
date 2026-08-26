import mysql from 'mysql2/promise'
import { getPool } from '../utils/db'

/** 机型管理：设备列表 / 单机型分支详情 + 全部分支定义 */

interface BranchRow extends mysql.RowDataPacket {
  id: number
  device: string
  code: string | null
  tag: string | null
  region: string | null
  devtag: string | null
  devcode: string | null
  branchcode: string | null
  carrier: string | null
  full_brands: string | null
  brands: string | null
  full_names: string | null
  names: string | null
  xiaomi: string | null
  redmi: string | null
  poco: string | null
  image: string | null
  launch_date: string | null
  internal: string | null
  model: string | null
  branch_id: number | null
  branch_name_zh: string | null
  branch_name_en: string | null
  branch_zone: number | null
  branch_visibility: number | null
  branch_ep: number | null
  branch_code: string | null
}

interface DeviceRow extends mysql.RowDataPacket {
  ref_id: number
  device: string
  full_names: string | null
  names: string | null
  brands: string | null
  cnt: string
}

/** 解析 JSON 对象中的中英文名称（兼容非法 JSON 兜底） */
function pickName(fullNames: string | null, names: string | null, fallback: string): string {
  for (const raw of [fullNames, names]) {
    if (!raw) continue
    try {
      const obj = JSON.parse(raw)
      if (obj && typeof obj === 'object') {
        const v = obj.zh ?? obj.en
        if (typeof v === 'string' && v.trim()) return v.trim()
      }
    } catch {
      /* 忽略非法 JSON，继续兜底 */
    }
  }
  return fallback
}

/** brands/carrier 存储为 JSON 或 Python 风格列表字符串，统一解析为可读文本 */
function cleanJsonStr(raw: string | null): string | null {
  if (!raw) return null
  const trimmed = raw.trim()
  // Python 列表风格 "['a','b']" → 去方括号
  if (trimmed.startsWith('[') && trimmed.endsWith(']')) {
    try {
      const arr = JSON.parse(trimmed.replace(/'/g, '"'))
      if (Array.isArray(arr)) return arr.filter(Boolean).join(', ')
    } catch {
      // 兜底：直接去方括号和引号
      return trimmed.slice(1, -1).replace(/['"]/g, '').split(',').map((s) => s.trim()).filter(Boolean).join(', ')
    }
  }
  // JSON 字符串风格
  try {
    const v = JSON.parse(trimmed)
    if (typeof v === 'string' && v.trim()) return v.trim()
    if (Array.isArray(v) && v.length > 0) return v.filter(Boolean).join(', ')
  } catch {
    /* 忽略 */
  }
  return raw
}

export default defineEventHandler(async (event) => {
  const query = getQuery(event)
  const device = typeof query.device === 'string' ? query.device.trim() : ''

  // 未指定机型：返回全部机型列表（去重）
  if (!device) {
    const [rows] = await getPool().query<DeviceRow[]>(
      `SELECT d.device,
              COALESCE(MAX(CASE WHEN d.tag = 'CnOO' THEN d.id END), MIN(d.id)) AS ref_id,
              MAX(d.full_names) AS full_names,
              MAX(d.names) AS names,
              MAX(d.brands) AS brands,
              COUNT(*) AS cnt
       FROM devices d
       GROUP BY d.device
       ORDER BY d.device`,
    )
    return {
      devices: rows.map((r) => ({
        id: Number(r.ref_id || 0),
        device: r.device,
        name: pickName(r.full_names, r.names, r.device),
        brands: cleanJsonStr(r.brands),
        count: Number(r.cnt || 0),
      })),
    }
  }

  // 指定机型：按 branches 表顺序列出该机型全部分支
  const [rows] = await getPool().query<BranchRow[]>(
    `SELECT d.id, d.device, d.code, d.tag, d.region, d.devtag, d.devcode, d.branchcode,
            d.carrier, d.full_brands, d.brands, d.full_names, d.names,
            d.xiaomi, d.redmi, d.poco, d.image, d.launch_date, d.internal, d.model,
            MIN(b.id) AS branch_id,
            MIN(b.name_zh) AS branch_name_zh,
            MIN(b.name_en) AS branch_name_en,
            MIN(b.zone) AS branch_zone,
            MIN(b.visibility) AS branch_visibility,
            MIN(b.ep) AS branch_ep
     FROM devices d
     LEFT JOIN branches b ON d.tag = b.tag AND (d.region = b.region OR b.region IS NULL)
     WHERE d.device = ?
     GROUP BY d.id
     ORDER BY (branch_id IS NULL), branch_id ASC, d.id ASC`,
    [device],
  )

  if (rows.length === 0) {
    throw createError({ statusCode: 404, message: `机型不存在: ${device}` })
  }

  const decorate = (r: BranchRow) => ({
    id: r.id,
    device: r.device,
    code: r.code,
    tag: r.tag,
    region: r.region,
    devtag: r.devtag,
    devcode: r.devcode,
    branchcode: r.branchcode,
    carrier: cleanJsonStr(r.carrier),
    carrierRaw: r.carrier,
    full_brands: r.full_brands,
    brands: cleanJsonStr(r.brands),
    full_names: r.full_names,
    names: r.names,
    xiaomi: r.xiaomi,
    redmi: r.redmi,
    poco: r.poco,
    name: pickName(r.full_names, r.names, r.device),
    image: r.image,
    launch_date: r.launch_date,
    internal: r.internal,
    model: r.model,
    branchId: r.branch_id,
    branchNameZh: r.branch_name_zh,
    branchNameEn: r.branch_name_en,
    branchZone: r.branch_zone,
    branchVisibility: r.branch_visibility,
    branchEp: r.branch_ep,
  })

  // 基准：优先大陆正式版（tag=CnOO），缺失时退回第一条分支
  const baseline = rows.find((r) => r.tag === 'CnOO') ?? rows[0]!
  const rest = rows.filter((r) => r.id !== baseline.id)
  const baselineDecorated = { ...decorate(baseline), isBaseline: true }

  // 每个分支的品牌/名称原始数据：用于全局 full_brands / full_names 生成
  const allBranchesBrandData = rows.map((r) => ({
    id: r.id,
    xiaomi: r.xiaomi,
    redmi: r.redmi,
    poco: r.poco,
  }))

  // 全部分支定义（branches 表）：用于"新增分支"时展示可选分支
  interface BranchDefRow extends mysql.RowDataPacket {
    id: number
    branch: string | null
    name_zh: string | null
    name_en: string | null
    tag: string | null
    code: string | null
    vercode: string | null
    region: string | null
    carrier: string | null
  }
  const [allBranches] = await getPool().query<BranchDefRow[]>(
    `SELECT id, branch, name_zh, name_en, tag, code, vercode, region, carrier
     FROM branches ORDER BY id`,
  )

  // 已收录的 tag+region 组合（用于过滤可新增分支）
  const existingPairs = new Set(
    rows.map((r) => `${r.tag}|${r.region}`),
  )

  const availableBranches = allBranches
    .filter((b) => b.tag && !existingPairs.has(`${b.tag}|${b.region}`))
    .map((b) => ({
      id: b.id,
      branch: b.branch,
      nameZh: b.name_zh,
      nameEn: b.name_en,
      tag: b.tag,
      code: b.code,
      vercode: b.vercode,
      region: b.region,
      carrier: cleanJsonStr(b.carrier),
      carrierRaw: b.carrier,
    }))

  return {
    device,
    baseline: baselineDecorated,
    branches: [baselineDecorated, ...rest.map(decorate)],
    allBranches: allBranches.map((b) => ({
      id: b.id,
      branch: b.branch,
      nameZh: b.name_zh,
      nameEn: b.name_en,
      tag: b.tag,
      code: b.code,
      vercode: b.vercode,
      region: b.region,
      carrier: cleanJsonStr(b.carrier),
      carrierRaw: b.carrier,
    })),
    availableBranches,
    allBranchesBrandData,
  }
})
