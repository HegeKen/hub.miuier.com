/**
 * 小米 OTA 报文加解密（AES-128-CBC + PKCS#7）。
 *
 * 使用 Web Crypto（globalThis.crypto.subtle）而非 Node 专用的 node:crypto：
 * 生产环境部署在 Cloudflare Pages Functions（workerd 运行时），开发环境是 Node，
 * Web Crypto 在两边都可用，同一份代码无需按运行时分支。
 */

const KEY = 'miuiotavalided11' // 16 字节 ASCII
const IV = '0102030405060708' // 16 字节 ASCII

let cryptoKeyPromise: Promise<CryptoKey> | null = null

function getCryptoKey(): Promise<CryptoKey> {
  if (!cryptoKeyPromise) {
    cryptoKeyPromise = crypto.subtle.importKey(
      'raw',
      new TextEncoder().encode(KEY),
      { name: 'AES-CBC' },
      false,
      ['encrypt', 'decrypt'],
    )
  }
  return cryptoKeyPromise
}

const ivBytes = new TextEncoder().encode(IV)

// Python urllib.parse.quote() 的 unreserved 集合；其余字节一律 %XX 转义（大写）
const UNRESERVED = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_.-~'
const HEX = '0123456789ABCDEF'

function percentEncode(value: string): string {
  let out = ''
  for (const byte of new TextEncoder().encode(value)) {
    const ch = String.fromCharCode(byte)
    if (UNRESERVED.includes(ch)) {
      out += ch
    } else {
      out += '%' + HEX[(byte >> 4) & 0xf] + HEX[byte & 0xf]
    }
  }
  return out
}

function bytesToBase64(bytes: Uint8Array): string {
  let binary = ''
  for (let i = 0; i < bytes.length; i++) binary += String.fromCharCode(bytes[i] ?? 0)
  // btoa 在 Node 18+ 与 workerd 均为全局
  return btoa(binary)
}

function base64ToBytes(b64: string): Uint8Array {
  // 容忍换行等空白（等价于 Android 的 Base64.getMimeDecoder）
  const binary = atob(b64.replace(/\s+/g, ''))
  const bytes = new Uint8Array(binary.length)
  for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i)
  return bytes
}

/**
 * 加密请求 JSON → base64 → URL 编码。
 * base64 字母表里的 `/` `+` `=` 都会被 percentEncode 转义（`/` 即 %2F，
 * 与官方客户端及 Python quote(...).replace("/", "%2F") 的结果一致）。
 */
export async function miuiEncrypt(json: string): Promise<string> {
  const encrypted = await crypto.subtle.encrypt(
    { name: 'AES-CBC', iv: ivBytes },
    await getCryptoKey(),
    new TextEncoder().encode(json) as BufferSource,
  )
  return percentEncode(bytesToBase64(new Uint8Array(encrypted)))
}

/**
 * 解密接口返回的 base64 密文 → UTF-8 明文。
 * 返回尾部可能有填充乱码，统一截到最后一个 `}`。
 */
export async function miuiDecrypt(base64Cipher: string): Promise<string> {
  const decrypted = await crypto.subtle.decrypt(
    { name: 'AES-CBC', iv: ivBytes },
    await getCryptoKey(),
    base64ToBytes(base64Cipher) as BufferSource,
  )
  const plain = new TextDecoder().decode(decrypted).trim()
  const end = plain.lastIndexOf('}')
  return end >= 0 ? plain.slice(0, end + 1) : plain
}
