import { readFile } from 'fs/promises'
import { join, resolve } from 'path'

const IMAGES_DIR = resolve(process.cwd(), '../../data/images')

export default defineEventHandler(async (event) => {
  const name = getRouterParam(event, 'name')

  if (!name) {
    throw createError({ statusCode: 400, statusMessage: 'Missing name' })
  }

  // Prevent path traversal
  if (name.includes('..')) {
    throw createError({ statusCode: 400, statusMessage: 'Invalid path' })
  }

  const filePath = join(IMAGES_DIR, name)

  // 根据扩展名设置内容类型，支持 png 与 svg（mi.svg 为 Xiaomi 默认图）
  const ext = name.split('.').pop()?.toLowerCase()
  const contentTypes: Record<string, string> = {
    png: 'image/png',
    svg: 'image/svg+xml',
    jpg: 'image/jpeg',
    jpeg: 'image/jpeg',
    webp: 'image/webp',
  }

  try {
    const content = await readFile(filePath)
    setResponseHeader(event, 'content-type', contentTypes[ext ?? ''] || 'application/octet-stream')
    setResponseHeader(event, 'cache-control', import.meta.dev ? 'no-store' : 'public, max-age=86400')
    return content
  } catch {
    throw createError({ statusCode: 404, statusMessage: 'Not found' })
  }
})
