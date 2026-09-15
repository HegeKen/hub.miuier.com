import mysql from 'mysql2/promise'
import { getPool } from './db'

/**
 * 固件文件名解析（TypeScript 版，对齐 data/scripts/miroms/firmware.py 中 FirmwareParser.parse_filename）
 *
 * 从 recovery / fastboot 包文件名中切分出：
 *   version / android / code / device / type / bigver / region / tag / zone / branch
 *
 * 解析结果用于「一键修复」：以文件名为准回填 roms 表中对应字段。
 */

export interface ParsedRom {
  filetype: 'recovery' | 'fastboot' | ''
  android: string
  version: string
  code: string
  device: string
  type: 'MIUI' | 'HyperOS' | 'STAN' | ''
  bigver: string
  region: string
  tag: string
  zone: number
  branch: string
}

const EMPTY: ParsedRom = {
  filetype: '',
  android: '',
  version: '',
  code: '',
  device: '',
  type: '',
  bigver: '',
  region: '',
  tag: '',
  zone: 1,
  branch: 'F',
}

/** 从版本号提取系统类型与大版本 */
export function extractType(version: string): { type: ParsedRom['type']; bigver: string } {
  if (!version) return { type: '', bigver: '' }
  if (version.startsWith('V')) {
    // MIUI：V12.5.x → 'MIUI 12.5'，V14.0.x → 'MIUI 14'（保留 .5 子版本）
    const major = version.slice(1).split('.').slice(0, 2).join('.').replace(/\.0$/, '')
    return { type: 'MIUI', bigver: `MIUI ${major}` }
  }
  if (version.startsWith('OS')) {
    const major = version.slice(2).split('.')[0]
    return { type: 'HyperOS', bigver: `HyperOS ${major}` }
  }
  if (version.startsWith('A')) {
    return { type: 'STAN', bigver: `STAN ${version.split('.')[0]}` }
  }
  return { type: '', bigver: '' }
}

/**
 * 解析文件名。若无法解析（无 recovery/fastboot 或格式不识别），返回 filetype 为空的对象。
 */
export async function parseFilename(filename: string): Promise<ParsedRom> {
  const result: ParsedRom = { ...EMPTY }
  if (!filename) return result

  const pool = getPool()

  // ---- miui_*.zip（旧版卡刷） ----
  if (filename.includes('miui') && filename.endsWith('.zip')) {
    result.filetype = 'recovery'
    const parts = filename.split('_')
    if (parts.length > 4) {
      result.android = (parts[4] ?? '').replace('.zip', '')
      result.version = parts[2] ?? ''
      const branchCode = parts[1] ?? ''
      const [rows] = await pool.query<mysql.RowDataPacket[]>(
        'SELECT code, device FROM devices WHERE branchcode = ? LIMIT 1',
        [branchCode],
      )
      if (rows[0]) {
        result.code = String(rows[0].code ?? '')
        result.device = String(rows[0].device ?? '')
      }
    }
  }
  // ---- .tgz 线刷 ----
  else if (filename.endsWith('.tgz')) {
    result.filetype = 'fastboot'
    if (filename.includes('-images')) {
      const parts = (filename.split('images-')[1] ?? '').split('-')
      if (parts.length > 3) {
        result.android = parts[3] ?? ''
        result.version = parts[0] ?? ''
      }
      result.code = filename.split('-images')[0] ?? ''
    } else {
      const parts = (filename.split('images_')[1] ?? '').split('_')
      if (parts.length > 2) {
        result.android = parts[2] ?? ''
        result.version = parts[0] ?? ''
      }
      result.code = filename.split('_images')[0] ?? ''
    }
  }
  // ---- 现代卡刷包（-ota_full-） ----
  else if (filename.endsWith('.zip')) {
    result.filetype = 'recovery'
    const after = filename.split('ota_full-')[1]
    if (after) {
      // version 取 -user 之前的部分；android 取 -user- 之后第一段
      // （version 内部可能含 '-'，如 STABLE-DPP / PRE-DPP，不能直接 split('-') 取固定下标）
      const parts = after.split('-user')
      result.version = parts[0] ?? ''
      const afterUser = parts.slice(1).join('-user')
      result.android = afterUser ? (afterUser.split('-')[0] ?? '') : ''
    }
    result.code = filename.split('-ota_full')[0] ?? ''
  }

  // ---- 补充 device：按 code 查 roms / devices ----
  if (result.code) {
    const [romRows] = await pool.query<mysql.RowDataPacket[]>(
      'SELECT device FROM roms WHERE code = ? LIMIT 1',
      [result.code],
    )
    if (romRows[0]?.device) {
      result.device = String(romRows[0].device)
    } else {
      const [devRows] = await pool.query<mysql.RowDataPacket[]>(
        'SELECT device FROM devices WHERE code = ? LIMIT 1',
        [result.code],
      )
      if (devRows[0]?.device) {
        result.device = String(devRows[0].device)
      }
    }
  }

  // ---- type / bigver ----
  if (result.version) {
    const { type, bigver } = extractType(result.version)
    result.type = type
    result.bigver = bigver
  }

  // ---- region / tag / zone ----
  if (result.version && result.version.includes('CNXM')) {
    result.region = 'cn'
    result.zone = 1
    const verParts = result.version.split('.')
    result.tag = verParts.length > 3 && verParts[3] === '0' ? 'CnOO' : 'CnOB'
  } else if (result.code) {
    const [romRows] = await pool.query<mysql.RowDataPacket[]>(
      'SELECT region, tag, zone FROM roms WHERE code = ? LIMIT 1',
      [result.code],
    )
    if (romRows[0]) {
      result.region = String(romRows[0].region ?? '')
      result.tag = String(romRows[0].tag ?? '')
      result.zone = Number(romRows[0].zone ?? 1)
    } else {
      const [devRows] = await pool.query<mysql.RowDataPacket[]>(
        'SELECT region, tag FROM devices WHERE code = ? LIMIT 1',
        [result.code],
      )
      if (devRows[0]) {
        result.region = String(devRows[0].region ?? '')
        result.tag = String(devRows[0].tag ?? '')
        result.zone = result.region === 'cn' ? 1 : 2
      }
    }
  }

  return result
}
