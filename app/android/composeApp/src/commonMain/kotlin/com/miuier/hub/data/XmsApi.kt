package com.miuier.hub.data

import com.miuier.hub.platform.miuiDecrypt
import com.miuier.hub.platform.miuiEncrypt
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.random.Random
import kotlin.time.Clock

private const val XMS_URL = "https://update.miui.com/api/v3/xms/getXmsVer"

/**
 * **必须** encodeDefaults = true。表单里 `b` / `f` / `l` 的值恰好等于默认值，
 * kotlinx.serialization 默认会把它们整个省掉，接口就认不出这是一次 getXmsVer 请求。
 */
private val XmsJson = Json { encodeDefaults = true }

@Serializable
private data class XmsForm(
    val b: String = "F",
    val c: String = "",
    val d: String = "",
    val f: String = "1",
    val l: String = "zh_CN",
    val lsv: String = "",
    val r: String = "",
    val id: String = "",
    val rv: String = "",
    val pkgs: List<XmsPkgRef> = emptyList(),
)

@Serializable
private data class XmsPkgRef(val pkg: String, val pkgVer: String = "1")

/** 只声明用得到的字段：接口还会带 size / md5 / sha256 / lastVerCode，那些数据里已经有了 */
@Serializable
private data class XmsResponse(
    @SerialName("apkLists") val apkLists: List<XmsApkItem> = emptyList(),
)

@Serializable
private data class XmsApkItem(
    val packName: String = "",
    val fileName: String = "",
    val downloadUrls: List<String> = emptyList(),
)

class XmsApi(private val client: HttpClient = createOtaHttpClient()) {

    /**
     * 换取 APK 当下的签名下载地址。
     *
     * 与 `data/scripts/miroms/xms.py` 以及网页端 `server/api/xms/download.post.ts` 用的是
     * 同一个接口（getXmsVer）：静态 JSON 里存不了带 `?t=&s=` 时效签名的直链，
     * 只能每次按「机型 + 系统版本 + XMS 版本」现问一次。
     */
    suspend fun download(request: XmsDownloadRequest): XmsDownloadResult {
        if (request.pkg.isBlank() || request.code.isBlank() || request.romVersion.isBlank()) {
            return XmsDownloadResult(XmsStatus.NOTFOUND)
        }
        return try {
            val body = buildString {
                append("q=").append(miuiEncrypt(XmsJson.encodeToString(buildForm(request))))
                append("&s=1&t=")
                append("&n=").append(requestId())
                append("&ts=").append(Clock.System.now().toEpochMilliseconds())
            }
            val response = client.post(XMS_URL) {
                // 与 miroms/network.py 的请求头保持一致
                header(HttpHeaders.UserAgent, "Dalvik/2.1.0 (Linux; U; Android 13; MI 9 Build/TKQ1.220829.002)")
                header(HttpHeaders.Cookie, "serviceToken=;")
                // 必须显式声明；application/json 会被接口回 406
                header(HttpHeaders.Accept, "*/*")
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(body)
            }
            parse(response.bodyAsText(), request.pkg)
        } catch (t: Throwable) {
            XmsDownloadResult(XmsStatus.FAILED, message = t.message ?: t::class.simpleName ?: "unknown")
        }
    }

    private fun parse(raw: String, pkg: String): XmsDownloadResult {
        val text = raw.trim()
        // 出错时接口回的是明文 JSON（含 code 字段），不是密文
        if (text.isEmpty() || text.startsWith("{")) return XmsDownloadResult(XmsStatus.NOTFOUND)
        val cipher = text.substringBefore("q=").trim()
        if (cipher.isEmpty()) return XmsDownloadResult(XmsStatus.NOTFOUND)

        val parsed = runCatching { HubJson.decodeFromString<XmsResponse>(miuiDecrypt(cipher)) }.getOrNull()
            ?: return XmsDownloadResult(XmsStatus.NOTFOUND)
        val apk = parsed.apkLists.firstOrNull { it.packName == pkg }
            ?: return XmsDownloadResult(XmsStatus.NOTFOUND)

        val urls = apk.downloadUrls.filter(::isAllowedDownloadUrl).distinct()
        // 有文件名却没签名：这种直链拼到 CDN 上会 403，如实告诉用户
        if (urls.isEmpty()) return XmsDownloadResult(XmsStatus.UNSIGNED, fileName = apk.fileName)
        return XmsDownloadResult(XmsStatus.AVAILABLE, fileName = apk.fileName, urls = urls)
    }

    private fun buildForm(request: XmsDownloadRequest): XmsForm {
        val region = request.region.lowercase()
        return XmsForm(
            c = request.android.removeSuffix(".0"),
            d = request.code,
            l = if (region == "cn") "zh_CN" else "en_US",
            lsv = request.xmsVersion,
            r = REGION_FIELD_OVERRIDES[region] ?: request.region,
            rv = request.romVersion,
            pkgs = listOf(XmsPkgRef(request.pkg)),
        )
    }

    private companion object {
        /** 表单里接口实际读取的区域写法；其余区域直接用数据里的 region 值 */
        val REGION_FIELD_OVERRIDES = mapOf("cn" to "CN", "global" to "GL")

        /** 只接受小米自家 CDN 的 https 链接，避免把任意 URL 丢给下载器 */
        fun isAllowedDownloadUrl(value: String): Boolean {
            val url = runCatching { Url(value) }.getOrNull() ?: return false
            if (url.protocol != URLProtocol.HTTPS) return false
            val host = url.host.lowercase()
            return host == "miui.com" || host.endsWith(".miui.com")
        }

        /** 接口的 `n` 参数是个随机请求 id，格式沿用 UUID v4，不要求真的唯一 */
        fun requestId(): String {
            val alphabet = "0123456789abcdef"
            val hex = CharArray(32) { alphabet[Random.nextInt(16)] }
            hex[12] = '4'
            hex[16] = "89ab"[Random.nextInt(4)]
            val s = String(hex)
            return "${s.substring(0, 8)}-${s.substring(8, 12)}-${s.substring(12, 16)}-" +
                "${s.substring(16, 20)}-${s.substring(20)}"
        }
    }
}

/** 一次换取签名链接所需的机型 / 版本信息，全部来自 v3 的 `xms` 数据 */
data class XmsDownloadRequest(
    /** APK 包名 */
    val pkg: String,
    /** 分支代号（branch.id），对应表单 d —— 机型与区域都编码在这个字段里，不需要再单发 */
    val code: String,
    val region: String,
    /** Android 大版本，如 16.0，对应表单 c（去掉 .0） */
    val android: String,
    /** 该次推送所基于的系统版本，对应表单 rv */
    val romVersion: String,
    /** XMS 版本号，如 C11，对应表单 lsv */
    val xmsVersion: String,
)

data class XmsDownloadResult(
    val status: String,
    val fileName: String = "",
    val urls: List<String> = emptyList(),
    val message: String = "",
)

object XmsStatus {
    /** 拿到签名地址 */
    const val AVAILABLE = "available"

    /** 小米那边没有这条记录 */
    const val NOTFOUND = "notfound"

    /** 返回了文件名但没有签名，直链会 403 */
    const val UNSIGNED = "unsigned"

    /** 请求本身失败（网络、解密等） */
    const val FAILED = "failed"
}
