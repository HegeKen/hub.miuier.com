package com.miuier.hub.data

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

private const val FULLROM_URL = "https://update.intl.miui.com/updates/miota-fullrom.php"

@Serializable
private data class FullRomResponse(
    /**
     * 有线刷包时是对象，没有时是空数组（实测 `n=chinamobile` 回的就是 `[]`），
     * 所以按 [JsonElement] 收下来再判形态。
     */
    @SerialName("LatestFullRom") val latestFullRom: JsonElement? = null,
)

@Serializable
private data class FullRomFile(
    val filename: String = "",
    val version: String = "",
    val filesize: String = "",
)

/** 一条线刷包记录。[carrier] 为空串表示通用包（`n=` 留空那一次请求的结果） */
data class FastbootPackage(
    val carrier: String,
    val version: String,
    val filename: String,
    val filesize: String,
) {
    /** 与站内其它包同一个 CDN：`<基址>/<版本号>/<文件名>` */
    val url: String get() = downloadUrl(version, filename)
}

sealed interface FastbootResult {
    /** 至少查到一条，按请求时的运营商顺序排列 */
    data class Available(val packages: List<FastbootPackage>) : FastbootResult

    /** 所有运营商都没有包（数据里没有这个分支、或小米还没出线刷包） */
    data object NotFound : FastbootResult

    data class Failed(val message: String) : FastbootResult
}

/**
 * 分支「线刷包」查询（`update.intl.miui.com/updates/miota-fullrom.php`）。
 *
 * 与 OTA 那个接口不同，它是**按运营商发货**的：`n=` 决定返回通用包还是某个定制包，
 * 一次只能问一个。所以 [query] 对每个运营商各发一次请求、逐条返回结果
 * —— 与 data 仓库的 `scripts/get_current_fastboot.py` 做法一致。
 *
 * 注意 `d=` 必须是**分支代号**（v3 数据里的 `branch.id`，如 `kunzite_global`），
 * 只给机型代号（`kunzite`）问到的是另一条分支的包。
 */
class FastbootApi(private val client: HttpClient = createOtaHttpClient()) {

    suspend fun query(
        branchId: String,
        branchTag: String,
        region: String,
        carriers: List<String>,
    ): FastbootResult {
        if (branchId.isBlank()) return FastbootResult.NotFound
        // 与 data 仓库一致：没声明运营商时也要问一次通用包（n= 留空）
        val targets = carriers.ifEmpty { listOf("") }

        var lastError: String? = null
        val packages = mutableListOf<FastbootPackage>()
        for (carrier in targets) {
            try {
                val pkg = fetch(branchId, branchTag, region, carrier)
                if (pkg != null) packages.add(pkg)
            } catch (t: Throwable) {
                // 单个运营商失败不影响其余；全都失败时才把错误交给界面
                lastError = t.message ?: t::class.simpleName ?: "unknown"
            }
        }
        val error = lastError
        return when {
            packages.isNotEmpty() -> FastbootResult.Available(packages)
            error != null -> FastbootResult.Failed(error)
            else -> FastbootResult.NotFound
        }
    }

    private suspend fun fetch(
        branchId: String,
        branchTag: String,
        region: String,
        carrier: String,
    ): FastbootPackage? {
        val url = "$FULLROM_URL?d=$branchId&b=$branchTag&r=$region&n=$carrier"
        val response = client.post(url) {
            // 与 miroms/network.py 的请求头一致
            header(HttpHeaders.UserAgent, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            header(HttpHeaders.Connection, "close")
        }
        if (response.status != HttpStatusCode.OK) return null

        val parsed = HubJson.decodeFromString<FullRomResponse>(response.bodyAsText())
        val file = parsed.latestFullRom?.asFile() ?: return null
        if (file.filename.isBlank() || file.version.isBlank()) return null
        return FastbootPackage(carrier, file.version, file.filename, file.filesize)
    }

    /** `LatestFullRom` 是对象就取它，是数组就取第一条；空数组 / 空对象都当「没有」 */
    private fun JsonElement.asFile(): FullRomFile? = when (this) {
        is JsonObject -> HubJson.decodeFromJsonElement(FullRomFile.serializer(), this)
        is JsonArray -> firstOrNull()?.asFile()
        else -> null
    }
}
