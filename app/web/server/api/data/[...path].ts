import { readFile } from 'fs/promises'
import { join, resolve } from 'path'

const DATA_DIR = resolve(process.cwd(), '../../data/api')

export default defineEventHandler(async (event) => {
  const path = getRouterParam(event, 'path')

  if (!path) {
    throw createError({ statusCode: 400, statusMessage: 'Missing path' })
  }

  // Prevent path traversal
  if (path.includes('..')) {
    throw createError({ statusCode: 400, statusMessage: 'Invalid path' })
  }

  const filePath = join(DATA_DIR, path)

  try {
    const content = await readFile(filePath, 'utf-8')
    setResponseHeader(event, 'content-type', 'application/json')
    // dev 下禁用缓存，避免浏览器拿到旧版 stats.json（如缺 recent 列表的旧格式）
    setResponseHeader(event, 'cache-control', import.meta.dev ? 'no-store' : 'public, max-age=300')
    return content
  } catch {
    throw createError({ statusCode: 404, statusMessage: 'Not found' })
  }
})
