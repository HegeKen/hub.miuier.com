package com.miuier.hub.platform

/**
 * 小米 OTA 接口（update.miui.com）的报文加解密：AES-128-CBC + PKCS#7，密钥固定。
 *
 * 请求体是 `q=<加密后的表单 JSON>`，响应体是同样方式加密的 JSON，
 * 与 `data/scripts/miroms/crypto.py` 的实现保持一致。
 *
 * 之所以做成 expect：`javax.crypto` 在 commonMain 里用不了。
 * 本项目只有 Android 和桌面（JVM）两个目标，所以实际实现放在 `jvmCommonMain` 里只写一份。
 */
expect fun miuiEncrypt(json: String): String

/** 解出来的是 JSON 文本；响应尾部可能有填充，取到最后一个 `}` 为止 */
expect fun miuiDecrypt(base64Cipher: String): String
