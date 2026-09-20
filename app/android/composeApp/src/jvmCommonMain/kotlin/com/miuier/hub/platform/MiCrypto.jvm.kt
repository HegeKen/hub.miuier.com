package com.miuier.hub.platform

import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

private val KEY = "miuiotavalided11".toByteArray(Charsets.US_ASCII)
private val IV = "0102030405060708".toByteArray(Charsets.US_ASCII)

/** Python `urllib.parse.quote()` 的等价实现：只保留 unreserved 字符，其余按 UTF-8 逐字节转义 */
private const val UNRESERVED =
    "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_.-~"

private fun percentEncode(value: String): String = buildString(value.length) {
    for (byte in value.toByteArray(Charsets.UTF_8)) {
        val ch = byte.toInt().toChar()
        if (ch in UNRESERVED) {
            append(ch)
        } else {
            append('%')
            append(HEX[(byte.toInt() shr 4) and 0xF])
            append(HEX[byte.toInt() and 0xF])
        }
    }
}

private const val HEX = "0123456789ABCDEF"

private fun cipher(mode: Int): Cipher = Cipher.getInstance("AES/CBC/PKCS5Padding").apply {
    init(mode, SecretKeySpec(KEY, "AES"), IvParameterSpec(IV))
}

actual fun miuiEncrypt(json: String): String {
    val encrypted = cipher(Cipher.ENCRYPT_MODE).doFinal(json.toByteArray(Charsets.US_ASCII))
    // base64 之后再做 URL 编码；'/' 保持与官方一致地写成 %2F
    return percentEncode(Base64.getEncoder().encodeToString(encrypted)).replace("/", "%2F")
}

actual fun miuiDecrypt(base64Cipher: String): String {
    // MIME 解码器容忍换行，比 basic 解码器更抗接口返回格式的小变化
    val decoded = Base64.getMimeDecoder().decode(base64Cipher)
    val plain = cipher(Cipher.DECRYPT_MODE).doFinal(decoded).toString(Charsets.UTF_8).trim()
    val end = plain.lastIndexOf('}')
    return if (end >= 0) plain.substring(0, end + 1) else plain
}
