package com.miuier.hub.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/** 与 app/web 消费的是同一个数据接口（data 仓库部署在 api.miuier.com） */
const val API_BASE = "https://api.miuier.com/api"

/**
 * ROM 包文件的 CDN 基址，与 app/web 的 `buildDownloadLink()` 完全一致：
 * `<基址>/<版本号>/<文件名>`，其中版本号是数据里的 `rom.miui`
 * （如 `OS3.0.305.0.WPUEUXM`），文件名是 `recovery` / `fastboot` 字段。
 * 实测该地址返回 200 且 Content-Length 正常（线刷包单个可达 4~5GB）。
 */
const val DOWNLOAD_BASE = "https://bkt-sgp-miui-ota-update-alisgp.oss-ap-southeast-1.aliyuncs.com"

fun downloadUrl(version: String, filename: String): String = "$DOWNLOAD_BASE/$version/$filename"

/**
 * 机型图片在站点根域（去掉 `/api` 后缀），与网页端 `useApi.buildImageUrl()` 一致。
 * 实测采样 24 款机型有 23 款存在 `<device>.png`。
 */
val IMAGE_BASE = API_BASE.removeSuffix("/api") + "/images"

fun deviceImageUrl(device: String): String = "$IMAGE_BASE/$device.png"

/** 没有机型照片时按品牌兜底：Xiaomi → mi.svg，POCO → POCO.png，REDMI → REDMI.png */
fun brandImageUrl(brand: String?): String = when (brand?.lowercase()) {
    "poco" -> "$IMAGE_BASE/POCO.png"
    "redmi" -> "$IMAGE_BASE/REDMI.png"
    else -> "$IMAGE_BASE/mi.svg"
}

/**
 * v3 JSON 的宽容解析策略：
 * - 接口在持续迭代会新增字段 → 忽略未知字段
 * - 历史记录里个别字段可能是 null → coerceInputValues 把 null 当作默认值，
 *   避免一条脏数据让整个列表加载失败
 */
internal val HubJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
}

class HubApi(private val client: HttpClient = createHttpClient()) {
    suspend fun devices(): List<DeviceSummary> = client.get("$API_BASE/v3/index.json").body()

    suspend fun device(code: String): DeviceDetail = client.get("$API_BASE/v3/devices/$code.json").body()

    suspend fun osIndex(): List<OsIndex> = client.get("$API_BASE/v3/roms/index.json").body()

    suspend fun osRoms(os: String): List<OsRom> = client.get("$API_BASE/v3/roms/$os.json").body()

    suspend fun stats(): HubStats = client.get("$API_BASE/v3/stats.json").body()

    /**
     * 某个 ROM 的多语种更新日志。
     *
     * 只有一部分 ROM 导出过日志（老机型、部分冷门区域没有），接口会给 404；
     * 这里当成「没有日志」返回空表，而不是抛异常——毕竟它只是详情页里的附加信息，
     * 不该让整张卡片变成错误态。App 的默认 `expectSuccess=false`，
     * 所以 404 不会抛，手判状态码即可。
     */
    suspend fun changelog(device: String, region: String, version: String): ChangelogFile {
        val url = if (region.isNotBlank()) {
            "$API_BASE/v3/logs/$device/$region/$version.json"
        } else {
            "$API_BASE/v3/logs/$device/$version.json"
        }
        val response = client.get(url)
        if (response.status == HttpStatusCode.NotFound) return emptyMap()
        return response.body()
    }
}

/** 引擎由各平台依赖决定：androidMain = OkHttp，desktopMain = CIO */
internal fun createHttpClient(): HttpClient = HttpClient {
    install(ContentNegotiation) { json(HubJson) }
}
