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
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable

private const val OTA_URL = "https://update.miui.com/updates/miotaV3.php"

/**
 * **必须** encodeDefaults = true。
 *
 * kotlinx.serialization 的 Json 默认 `encodeDefaults = false`，会把「值等于默认值」的字段
 * 整个省掉。而这个表单的意义就在于把 HyperOSForm 的一堆固定字段原样发给小米，
 * 省掉之后服务端认不出请求，只会回一个没有 LatestRom 的响应
 * （表现为「接口没有返回这个版本的更新信息」）。
 */
private val OtaJson = Json { encodeDefaults = true }

/**
 * 请求表单。字段与 `data/scripts/miroms/constants.py` 的 `HyperOSForm` 模板一致，
 * 只覆盖与机型/版本相关的那些。
 *
 * 注意 `d` 必须是**分支代号**（`mist_eea_global` 这种，即 v3 数据里的 `branch.id`），
 * 只给机型代号（`mist`）接口不会返回任何 ROM——已实测。
 */
@Serializable
private data class OtaForm(
    val obv: String = "OS1.0",
    val channel: String = "",
    val sys: String = "0",
    val bv: String = "816",
    val id: String = "",
    val sn: String = "0x0000043b716a25f1",
    val a: String = "0",
    val b: String = "F",
    val c: String = "14",
    val unlock: String = "0",
    val d: String = "marble",
    val lockZoneChannel: String = "normal",
    val f: String = "1",
    val ov: String = "",
    val g: String = "9b65722a06722e8d8dffa35a9fd58586",
    val i: String = "14db85f96df2efc324323fa7679f0d847ff53f3bff7179ea0c778ce5d980bc03",
    val i2: String = "2cd7c24f21e33b236fc63f26d044227b96d8b39a80400654f88182322688793b",
    val isR: String = "0",
    val l: String = "zh_CN",
    val n: String = "",
    val p: String = "marble",
    val pb: String = "Redmi",
    val r: String = "CN",
    val v: String = "",
    val sdk: String = "34",
    val pn: String = "marble",
    val options: OtaOptions = OtaOptions(),
)

@Serializable
private data class OtaOptions(
    val zone: Int = 1,
    val hashId: String = "dae7d50f696d7403",
    val ab: String = "1",
    val previewPlan: String = "0",
    val sv: Int = 3,
    val av: String = "8.8.8",
    val cv: String = "",
)

@Serializable
private data class OtaResponse(
    @SerialName("MirrorList") val mirrorList: List<String> = emptyList(),
    @SerialName("LatestRom") val latestRom: OtaRom? = null,
    @SerialName("CurrentRom") val currentRom: OtaRom? = null,
)

@Serializable
private data class OtaRom(
    val version: String = "",
    /** 老版本没有签名；有签名时形如 `xxx.zip?t=1789907786&s=559a...` */
    val filename: String = "",
    val filesize: String = "",
    val md5: String = "",
)

/** 一次请求所需的机型/版本信息，都来自 v3 数据 */
data class OtaRequest(
    /** 分支代号，对应表单的 d；即 v3 数据里的 `branch.id` */
    val code: String,
    /** 机型代号，对应表单的 p */
    val device: String,
    /** 分支标记 F / X，对应表单的 b */
    val branchTag: String,
    val region: String,
    val zone: Int,
    val android: String,
    /** 版本号，对应表单的 v / ov / options.cv */
    val version: String,
)

data class HighSpeedLink(val mirror: String, val url: String)

sealed interface HighSpeedResult {
    /** 拿到签名链接。mirror 只保留 superota / ultimateota */
    data class Available(val version: String, val links: List<HighSpeedLink>) : HighSpeedResult

    /** 小米只对「该分支最新版」签发链接；请求的版本已经不是最新版 */
    data class Outdated(val requested: String, val latestVersion: String) : HighSpeedResult

    /** 接口没有返回这个版本的任何 ROM 信息 */
    data object NotFound : HighSpeedResult

    /** 返回了文件名但没有签名——这种直链在 mirror 上会 403 */
    data object Unsigned : HighSpeedResult

    data class Failed(val message: String) : HighSpeedResult
}

/** Android 版本 → SDK_INT，与 constants.py 的 SDK_VERSIONS 对齐 */
private val SDK_BY_ANDROID = mapOf(
    "17.0" to "37", "16.0" to "36", "16" to "36", "15.0" to "35", "15" to "35",
    "14.0" to "34", "14" to "34", "13.0" to "33", "13" to "33",
    "12.0" to "31", "12" to "31", "11.0" to "30", "11" to "30", "10.0" to "29", "10" to "29",
)

/**
 * 小米 OTA 升级检查接口。
 *
 * 它的本职是「查有没有新版本」，因此只对**该分支最新版**签发带签名的下载地址；
 * 请求一个旧版本时返回的是「可以升级到的最新版」，而且旧版本自己的文件名没有签名，
 * 直接拼到 superota / ultimateota 上会 403（已实测）。
 * 这些结果都通过 [HighSpeedResult] 如实反馈给 UI，而不是假装成功。
 */
class OtaApi(private val client: HttpClient = createOtaHttpClient()) {

    suspend fun highSpeed(request: OtaRequest): HighSpeedResult {
        if (request.code.isBlank() || request.version.isBlank()) return HighSpeedResult.NotFound
        return try {
            val response = client.post(OTA_URL) {
                // 与 miroms/network.py 的请求头保持一致
                header(HttpHeaders.UserAgent, "Dalvik/2.1.0 (Linux; U; Android 13; MI 9 Build/TKQ1.220829.002)")
                header(HttpHeaders.Cookie, "serviceToken=;")
                // 必须显式声明；改成 application/json 会被接口回 406
                header(HttpHeaders.Accept, "*/*")
                contentType(ContentType.Application.FormUrlEncoded)
                setBody("q=${miuiEncrypt(OtaJson.encodeToString(buildForm(request)))}&s=1&t=")
            }
            if (response.status != HttpStatusCode.OK) {
                return HighSpeedResult.Failed("HTTP ${response.status.value}")
            }
            parse(miuiDecrypt(response.bodyAsText().substringBefore("q=").trim()), request)
        } catch (t: Throwable) {
            HighSpeedResult.Failed(t.message ?: t::class.simpleName ?: "unknown")
        }
    }

    private fun parse(decrypted: String, request: OtaRequest): HighSpeedResult {
        val parsed = HubJson.decodeFromString<OtaResponse>(decrypted)
        val latest = parsed.latestRom ?: return HighSpeedResult.NotFound
        val version = latest.version.ifBlank { return HighSpeedResult.NotFound }
        val filename = latest.filename
        if (filename.isBlank()) return HighSpeedResult.NotFound
        // 没有 `?t=&s=` 说明接口没给签名，拼出来的直链会 403
        if (!filename.contains('?')) return HighSpeedResult.Unsigned
        if (version != request.version) return HighSpeedResult.Outdated(request.version, version)

        val links = parsed.mirrorList
            .asSequence()
            .filter { it.startsWith("https://") }
            .filter { it.contains("superota") || it.contains("ultimateota") }
            .distinct()
            .map { mirror -> HighSpeedLink(mirror, "${mirror.trimEnd('/')}/$version/$filename") }
            .toList()
        return if (links.isEmpty()) HighSpeedResult.NotFound else HighSpeedResult.Available(version, links)
    }

    private fun buildForm(request: OtaRequest) = OtaForm(
        d = request.code,
        // 与 fetch_changelog.py 一致：pn 去掉 _global 后缀
        pn = request.code.substringBefore("_global"),
        p = request.device,
        b = request.branchTag.ifBlank { "F" },
        c = request.android,
        sdk = SDK_BY_ANDROID[request.android] ?: "36",
        r = REGION_FIELD_OVERRIDES[request.region.lowercase()] ?: request.region,
        v = request.version,
        ov = request.version,
        options = OtaOptions(zone = request.zone, cv = request.version),
    )

    private companion object {
        /** 表单里接口实际读取的区域写法；其余区域直接用 v3 的 region 值 */
        val REGION_FIELD_OVERRIDES = mapOf("cn" to "CN", "global" to "GL")
    }
}

/**
 * OTA 接口专用的 HttpClient：**不装 ContentNegotiation**。
 *
 * 装了它会自动加上 `Accept: application/json`，而小米这个接口对 application/json
 * 直接回 406（实测：不带 Accept、或带通配符的 Accept 都是 200）。
 * 这里的请求体是表单字符串、响应体是 base64 密文，本来也用不上内容协商。
 */
internal fun createOtaHttpClient(): HttpClient = HttpClient {
    expectSuccess = false
}
