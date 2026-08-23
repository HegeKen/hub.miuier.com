#!/usr/bin/env node

/**
 * Generate v3/index.json + v3/stats.json from individual device JSON files.
 * Reads from ../../data/api/v3/devices/ and outputs to ../../data/api/v3/
 */

import { readdir, readFile, writeFile } from 'fs/promises'
import { join, dirname } from 'path'
import { fileURLToPath } from 'url'

const __dirname = dirname(fileURLToPath(import.meta.url))
const DEVICES_DIR = join(__dirname, '../../../data/api/v3/devices')
const INDEX_FILE = join(__dirname, '../../../data/api/v3/index.json')
const STATS_FILE = join(__dirname, '../../../data/api/v3/stats.json')

// 近 N 天窗口（ROM 版本按 release 日期统计）
const RECENT_DAYS = 7

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

    for (const file of jsonFiles) {
      try {
        const content = await readFile(join(DEVICES_DIR, file), 'utf-8')
        const data = JSON.parse(content)

        // Extract only the fields needed for the index
        devices.push({
          device: data.device,
          name: data.name,
          brand: data.brand,
          code: data.code,
          android: data.android,
          supports: data.supports,
          branchCount: data.branches?.length || 0,
          romCount: data.branches?.reduce((sum, b) => sum + (b.roms?.length || 0), 0) || 0,
        })

        // Collect ROM versions released within the recent window
        for (const branch of data.branches || []) {
          for (const rom of branch.roms || []) {
            const release = rom.release || ''
            if (release && release >= sinceStr) {
              recent.push({
                device: data.device,
                name: data.name,
                brand: data.brand,
                version: rom.miui || '',
                android: rom.android || '',
                release,
                region: branch.region || '',
                branchName: branch.name || {},
              })
            }
          }
        }
      } catch (e) {
        console.warn(`Skipping ${file}: ${e.message}`)
      }
    }

    // Sort by device codename
    devices.sort((a, b) => a.device.localeCompare(b.device))

    // Sort recent ROMs by release date (newest first), then brand order (Xiaomi > REDMI > POCO), then device + version
    const brandOrder = { xiaomi: 0, redmi: 1, poco: 2 }
    const primaryBrand = (b) => {
      const brands = b.brand || []
      for (const name of brands) {
        const key = name.toLowerCase()
        if (key in brandOrder) return brandOrder[key]
      }
      return 99
    }
    recent.sort((a, b) => {
      if (a.release !== b.release) return a.release < b.release ? 1 : -1
      const ba = primaryBrand(a)
      const bb = primaryBrand(b)
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
  } catch (e) {
    console.error('Failed to generate index:', e.message)
    process.exit(1)
  }
}

generateIndex()
