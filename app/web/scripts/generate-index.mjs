#!/usr/bin/env node

/**
 * Generate v3/index.json + v3/stats.json from individual device JSON files.
 * Reads from ../../data/api/v3/devices/ and outputs to ../../data/api/v3/
 *
 * Also generates v3/roms/index.json + v3/roms/{os}.json, a flat list of all
 * ROM packages grouped by OS major version (OS3 / V14 / ...), so the frontend
 * can list every recovery/package of a given OS version via a single file.
 */

import { readdir, readFile, writeFile, mkdir } from 'fs/promises'
import { join, dirname } from 'path'
import { fileURLToPath } from 'url'

const __dirname = dirname(fileURLToPath(import.meta.url))
const DEVICES_DIR = join(__dirname, '../../../data/api/v3/devices')
const INDEX_FILE = join(__dirname, '../../../data/api/v3/index.json')
const STATS_FILE = join(__dirname, '../../../data/api/v3/stats.json')
const ROMS_DIR = join(__dirname, '../../../data/api/v3/roms')
const ROMS_INDEX_FILE = join(ROMS_DIR, 'index.json')
const SERIES_FILE = join(__dirname, '../../../data/api/v3/series.json')

// 近 N 天窗口（ROM 版本按 release 日期统计）
const RECENT_DAYS = 7

// 品牌优先排序：Xiaomi > REDMI > POCO
const brandOrder = { xiaomi: 0, redmi: 1, poco: 2 }
const primaryBrand = (b) => {
  const brands = b || []
  for (const name of brands) {
    const key = name.toLowerCase()
    if (key in brandOrder) return brandOrder[key]
  }
  return 99
}

// 从版本号前缀提取 OS 大版本标识（OS3 / V14 / V816 ...），非现代大版本前缀返回空串
function extractOsVersion(version) {
  const m = String(version || '').match(/^(OS\d+|V\d+)/)
  return m ? m[0] : ''
}

// 优先使用设备 JSON 中显式写入的 os 字段（更准确），否则回退到版本号前缀推断
function resolveOs(rom, version) {
  const explicit = String(rom.os || '').trim()
  if (explicit) return explicit
  return extractOsVersion(version)
}

// OS 大版本排序：OS 系在前，然后各自按数字降序；非数字字母类别（Stock / STAN）排最末并按字母序
const osSort = (a, b) => {
  const isOsA = a.startsWith('OS')
  const isOsB = b.startsWith('OS')
  if (isOsA !== isOsB) return isOsA ? -1 : 1
  const numA = parseInt(a.replace(/\D/g, ''), 10) || 0
  const numB = parseInt(b.replace(/\D/g, ''), 10) || 0
  if (numA !== numB) return numB - numA
  return a.localeCompare(b)
}

async function generateIndex() {
  try {
    const files = await readdir(DEVICES_DIR)
    const jsonFiles = files.filter((f) => f.endsWith('.json'))

    // 近 7 日窗口起点：按自然日计算（今天 - 6 天，含今天共 7 天）
    const now = new Date()
    const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
    const since = new Date(today.getTime() - (RECENT_DAYS - 1) * 24 * 60 * 60 * 1000)
    const sinceStr = since.toISOString().slice(0, 10)

    const devices = []
    const recent = []

    // 读取设备系列排序索引（series.json），用于控制设备列表顺序（品牌 → 系列 → 系列内序号）
    let seriesRank = new Map()
    try {
      const raw = await readFile(SERIES_FILE, 'utf-8')
      const seriesData = JSON.parse(raw)
      if (Array.isArray(seriesData.order)) {
        seriesRank = new Map(seriesData.order.map((d, i) => [d, i]))
      }
    } catch (e) {
      console.warn(`Skipping series.json: ${e.message}`)
    }

    // 按 OS 大版本分组的扁平 ROM 列表
    const romsByOs = new Map()             // os -> rom array
    const osDeviceCount = new Map()        // os -> Set<device>

    for (const file of jsonFiles) {
      try {
        const content = await readFile(join(DEVICES_DIR, file), 'utf-8')
        const data = JSON.parse(content)

        // Extract only the fields needed for the index
        devices.push({
          device: data.device,
          name: data.name,
          brand: data.brand,
          series: data.series || [],
          code: data.code,
          android: data.android,
          supports: data.supports,
          branchCount: data.branches?.length || 0,
          romCount: data.branches?.reduce((sum, b) => sum + (b.roms?.length || 0), 0) || 0,
        })

        // Collect ROM versions released within the recent window
        for (const branch of data.branches || []) {
          const region = branch.region || ''
          const zone = branch.zone || ''
          const branchName = branch.name || {}
          for (const rom of branch.roms || []) {
            const version = rom.miui || ''
            const release = rom.release || ''

            // 按 OS 大版本分组（优先使用显式 os 字段，回退到版本号推断）
            const os = resolveOs(rom, version)
            if (os) {
              if (!romsByOs.has(os)) romsByOs.set(os, [])
              romsByOs.get(os).push({
                os,
                bigver: rom.bigver || '',
                device: data.device,
                name: data.name,
                brand: data.brand,
                version,
                android: rom.android || '',
                region,
                zone,
                branchName,
                release,
                aspatch: rom.aspatch || '',
                recovery: rom.recovery || '',
                fastboot: rom.fastboot || '',
              })
              if (!osDeviceCount.has(os)) osDeviceCount.set(os, new Set())
              osDeviceCount.get(os).add(data.device)
            }

            if (release && release >= sinceStr) {
              recent.push({
                device: data.device,
                name: data.name,
                brand: data.brand,
                version,
                android: rom.android || '',
                release,
                region,
                branchName,
              })
            }
          }
        }
      } catch (e) {
        console.warn(`Skipping ${file}: ${e.message}`)
      }
    }

    // 设备列表排序：优先按系列排序索引（品牌 → 系列 → 系列内序号），未入系列则按代号排末尾
    devices.sort((a, b) => {
      const ra = seriesRank.has(a.device) ? seriesRank.get(a.device) : Number.MAX_SAFE_INTEGER
      const rb = seriesRank.has(b.device) ? seriesRank.get(b.device) : Number.MAX_SAFE_INTEGER
      if (ra !== rb) return ra - rb
      return a.device.localeCompare(b.device)
    })

    // Sort recent ROMs by release date (newest first), then brand order (Xiaomi > REDMI > POCO), then device + version
    recent.sort((a, b) => {
      if (a.release !== b.release) return a.release < b.release ? 1 : -1
      const ba = primaryBrand(a.brand)
      const bb = primaryBrand(b.brand)
      if (ba !== bb) return ba - bb
      const dev = a.device.localeCompare(b.device)
      return dev !== 0 ? dev : a.version.localeCompare(b.version)
    })

    await writeFile(INDEX_FILE, JSON.stringify(devices, null, 2), 'utf-8')
    console.log(`Generated index with ${devices.length} devices -> ${INDEX_FILE}`)

    const stats = {
      generatedAt: now.toISOString(),
      recentDays: RECENT_DAYS,
      since: sinceStr,
      recentRoms: recent.length,
      recent,
    }
    await writeFile(STATS_FILE, JSON.stringify(stats, null, 2), 'utf-8')
    console.log(`Generated stats (${recent.length} ROMs updated since ${sinceStr}) -> ${STATS_FILE}`)

    // ---- 生成按 OS 大版本拆分的 ROM 列表 ----
    await mkdir(ROMS_DIR, { recursive: true })

    for (const [os, roms] of romsByOs) {
      // 排序：发布时间（新→旧、空值置后）→ 品牌 → 设备代号 → 区域 → 版本号（新→旧）
      roms.sort((a, b) => {
        const ra = a.release || ''
        const rb = b.release || ''
        if (ra !== rb) {
          if (!ra) return 1
          if (!rb) return -1
          return ra < rb ? 1 : -1
        }
        const ba = primaryBrand(a.brand)
        const bb = primaryBrand(b.brand)
        if (ba !== bb) return ba - bb
        const dev = a.device.localeCompare(b.device)
        if (dev !== 0) return dev
        const rg = a.region.localeCompare(b.region)
        if (rg !== 0) return rg
        return b.version.localeCompare(a.version)
      })
      await writeFile(join(ROMS_DIR, `${os}.json`), JSON.stringify(roms, null, 2), 'utf-8')
    }

    const osIndex = Array.from(romsByOs.keys())
      .sort(osSort)
      .map((os) => ({ os, count: romsByOs.get(os).length, deviceCount: osDeviceCount.get(os).size }))
    await writeFile(ROMS_INDEX_FILE, JSON.stringify(osIndex, null, 2), 'utf-8')
    console.log(`Generated ROM index (${osIndex.length} OS versions, ${osIndex.reduce((s, o) => s + o.count, 0)} ROMs) -> ${ROMS_INDEX_FILE}`)
  } catch (e) {
    console.error('Failed to generate index:', e.message)
    process.exit(1)
  }
}

generateIndex()
