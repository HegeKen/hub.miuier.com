const CODENAME_PATTERN = /^[a-z0-9_]+$/

export function validateCodename(codename: string): boolean {
  if (!codename || typeof codename !== 'string') return false

  const sanitized = codename.toLowerCase().trim()
  if (sanitized.length === 0 || sanitized.length > 50) return false
  if (!CODENAME_PATTERN.test(sanitized)) return false

  const dangerousPatterns = ['..', '/', '\\', '\x00']
  if (dangerousPatterns.some((pattern) => sanitized.includes(pattern))) return false

  return true
}

export function sanitizeString(input: string): string {
  return input.toLowerCase().trim()
}
