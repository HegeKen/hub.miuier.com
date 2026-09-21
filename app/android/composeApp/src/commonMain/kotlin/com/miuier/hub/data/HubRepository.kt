package com.miuier.hub.data

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 极简内存缓存。v3 是静态 JSON 文件，一次会话内没必要重复拉取，
 * 也顺带让「返回上一层再进去」不会重新加载。
 */
class HubRepository(
    private val api: HubApi = HubApi(),
    private val ota: OtaApi = OtaApi(),
    private val fastbootApi: FastbootApi = FastbootApi(),
) {

    private var devicesCache: List<DeviceSummary>? = null
    private var statsCache: HubStats? = null
    private var osIndexCache: List<OsIndex>? = null
    private val deviceCache = mutableMapOf<String, DeviceDetail>()
    private val osRomsCache = mutableMapOf<String, List<OsRom>>()
    private val changelogCache = mutableMapOf<String, ChangelogFile>()

    /**
     * 机型名单会被两处同时要：App 启动时为了判断「本机信息」tab 要不要出现，
     * 以及机型列表页本身。缓存命中前两处会并发进来，加把锁让它们共用同一次请求。
     */
    private val devicesLock = Mutex()

    suspend fun devices(): List<DeviceSummary> {
        devicesCache?.let { return it }
        return devicesLock.withLock {
            devicesCache ?: api.devices().also { devicesCache = it }
        }
    }

    suspend fun stats(): HubStats {
        statsCache?.let { return it }
        return api.stats().also { statsCache = it }
    }

    suspend fun osIndex(): List<OsIndex> {
        osIndexCache?.let { return it }
        return api.osIndex().also { osIndexCache = it }
    }

    suspend fun device(code: String): DeviceDetail {
        deviceCache[code]?.let { return it }
        return api.device(code).also { deviceCache[code] = it }
    }

    suspend fun osRoms(os: String): List<OsRom> {
        osRomsCache[os]?.let { return it }
        return api.osRoms(os).also { osRomsCache[os] = it }
    }

    /**
     * 更新日志体积不小（单文件 33KB 左右），且只在卡片展开时才需要，
     * 所以单独缓存、key 到具体版本，重复展开同一条不会二次请求。
     */
    suspend fun changelog(device: String, region: String, version: String): ChangelogFile {
        val key = "$device/$region/$version"
        changelogCache[key]?.let { return it }
        return api.changelog(device, region, version).also { changelogCache[key] = it }
    }

    /**
     * 高速下载链接（superota / ultimateota）。
     * **故意不缓存**：接口返回的地址带时效签名（`?t=<时间戳>&s=...`），
     * 缓存下来过一会儿就失效了，必须每次实打实去问一次。
     */
    suspend fun highSpeed(request: OtaRequest): HighSpeedResult = ota.highSpeed(request)

    /**
     * 分支的线刷包，按运营商逐条去问（[FastbootApi.query] 的语义）。
     * 同样**故意不缓存**：拿的是「此刻最新的包」，缓存住反而会给出过期结果。
     */
    suspend fun fastboot(
        branchId: String,
        branchTag: String,
        region: String,
        carriers: List<String>,
    ): FastbootResult = fastbootApi.query(branchId, branchTag, region, carriers)

    /**
     * 下拉刷新用：清掉全部内存缓存，下一次读取重新打接口。
     *
     * 不做按页面的精细失效 —— 这是一次**手动**刷新，用户明确表达了「要最新的」，
     * 顺手清干净比为了省几个请求去维护「哪个缓存归哪个页面」更省事也更不容易错。
     * 各页面下次进入时会各自重新拉取。
     */
    fun invalidate() {
        devicesCache = null
        statsCache = null
        osIndexCache = null
        deviceCache.clear()
        osRomsCache.clear()
        changelogCache.clear()
    }
}
