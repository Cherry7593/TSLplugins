package org.tsl.tSLplugins.modules.landmark

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.tsl.tSLplugins.TSLplugins
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 地标系统管理器
 */
class LandmarkManager(private val plugin: JavaPlugin) {

    private var enabled: Boolean = false
    private val msg get() = (plugin as TSLplugins).messageManager

    // 存储
    val storage = LandmarkStorage(plugin)

    // 玩家当前所在地标缓存 (UUID -> LandmarkId)
    private val playerCurrentLandmark: MutableMap<UUID, String?> = ConcurrentHashMap()

    // 传送请求（吟唱中）
    private val teleportRequests: MutableMap<UUID, TeleportRequest> = ConcurrentHashMap()

    // 传送冷却
    private val teleportCooldowns: MutableMap<UUID, Long> = ConcurrentHashMap()

    // 进入提示冷却
    private val enterMessageCooldowns: MutableMap<UUID, MutableMap<String, Long>> = ConcurrentHashMap()

    // 离开提示冷却
    private val leaveMessageCooldowns: MutableMap<UUID, MutableMap<String, Long>> = ConcurrentHashMap()

    // 配置项
    var maxRegionVolume: Long = 1000000L
    var enterMessageCooldownSeconds: Int = 10
    var showCoordsInGUI: Boolean = false
    var requireInsideLandmark: Boolean = true
    var safeLanding: Boolean = true
    var castTimeSeconds: Int = 0
    var cancelOnMove: Boolean = true
    var cooldownSeconds: Int = 0

    init {
        loadConfig()
        storage.loadAll()
    }

    /**
     * 加载配置
     */
    fun loadConfig() {
        val config = plugin.config
        enabled = config.getBoolean("landmark.enabled", false)
        maxRegionVolume = config.getLong("landmark.max-region-volume", 1000000L)
        enterMessageCooldownSeconds = config.getInt("landmark.enter-message-cooldown-seconds", 10)
        showCoordsInGUI = config.getBoolean("landmark.show-coords-in-gui", false)
        requireInsideLandmark = config.getBoolean("landmark.teleport.require-inside-landmark", true)
        safeLanding = config.getBoolean("landmark.teleport.safe-landing", true)
        castTimeSeconds = config.getInt("landmark.teleport.cast-time-seconds", 0)
        cancelOnMove = config.getBoolean("landmark.teleport.cancel-on-move", true)
        cooldownSeconds = config.getInt("landmark.teleport.cooldown-seconds", 0)
    }

    fun isEnabled(): Boolean = enabled

    fun getMessage(key: String, vararg replacements: Pair<String, String>): String {
        return msg.getModule("landmark", key, *replacements)
    }

    // ========== 地标管理 ==========

    fun createLandmark(name: String, world: String, region: LandmarkRegion): Landmark? {
        if (storage.landmarkExists(name)) return null
        if (region.volume() > maxRegionVolume) return null

        val landmark = Landmark(
            id = UUID.randomUUID().toString(),
            name = name,
            world = world,
            region = region
        )

        return if (storage.addLandmark(landmark)) landmark else null
    }

    fun deleteLandmark(name: String): Boolean {
        val landmark = storage.getLandmarkByName(name) ?: return false
        return storage.removeLandmark(landmark.id)
    }

    fun getLandmark(name: String): Landmark? = storage.getLandmarkByName(name)

    fun getAllLandmarks(): Collection<Landmark> = storage.getAllLandmarks()

    // ========== 区域检测 ==========

    fun getLandmarkAt(location: Location): Landmark? {
        val world = location.world?.name ?: return null
        return storage.getLandmarksByWorld(world).find { landmark ->
            landmark.region.contains(location.x, location.y, location.z)
        }
    }

    fun isInsideAnyLandmark(player: Player): Boolean {
        return getLandmarkAt(player.location) != null
    }

    fun getPlayerCurrentLandmark(playerUuid: UUID): String? {
        return playerCurrentLandmark[playerUuid]
    }

    fun setPlayerCurrentLandmark(playerUuid: UUID, landmarkId: String?) {
        if (landmarkId == null) {
            playerCurrentLandmark.remove(playerUuid)
        } else {
            playerCurrentLandmark[playerUuid] = landmarkId
        }
    }

    // ========== 解锁系统 ==========

    fun isUnlocked(playerUuid: UUID, landmark: Landmark): Boolean {
        if (landmark.defaultUnlocked) return true
        return storage.isUnlocked(playerUuid.toString(), landmark.id)
    }

    fun unlockLandmark(playerUuid: UUID, landmark: Landmark): Boolean {
        return storage.unlockLandmark(playerUuid.toString(), landmark.id)
    }

    fun getUnlockedLandmarks(playerUuid: UUID): List<Landmark> {
        val unlockedIds = storage.getUnlockedLandmarks(playerUuid.toString())
        return storage.getAllLandmarks().filter { 
            it.defaultUnlocked || unlockedIds.contains(it.id) 
        }
    }

    // ========== 传送系统 ==========

    fun canTeleport(player: Player, target: Landmark): TeleportResult {
        // 检查是否在地标内
        if (requireInsideLandmark && !isInsideAnyLandmark(player)) {
            return TeleportResult.NOT_IN_LANDMARK
        }

        // 检查目标是否解锁
        if (!isUnlocked(player.uniqueId, target)) {
            return TeleportResult.NOT_UNLOCKED
        }

        // 检查冷却
        if (cooldownSeconds > 0) {
            val lastTp = teleportCooldowns[player.uniqueId] ?: 0
            val remaining = (lastTp + cooldownSeconds * 1000L - System.currentTimeMillis()) / 1000
            if (remaining > 0) {
                return TeleportResult.ON_COOLDOWN
            }
        }

        return TeleportResult.SUCCESS
    }

    fun teleport(player: Player, target: Landmark): Boolean {
        val result = canTeleport(player, target)
        if (result != TeleportResult.SUCCESS) return false

        val world = Bukkit.getWorld(target.world) ?: return false
        val location = getWarpLocation(target, world) ?: return false

        // 如果有吟唱时间
        if (castTimeSeconds > 0) {
            startTeleportRequest(player, target)
            return true
        }

        // 直接传送
        executeTeleport(player, location)
        return true
    }

    private fun getWarpLocation(landmark: Landmark, world: World): Location? {
        val warp = landmark.warpPoint
        if (warp != null) {
            // warpPoint 由管理员/维护者设置时已验证安全，直接使用
            return Location(world, warp.x, warp.y, warp.z, warp.yaw, warp.pitch)
        }

        // 无 warpPoint 时使用中心点
        val center = landmark.region.center()
        return if (safeLanding) {
            // 使用世界最高方块 +1 作为安全落点（线程安全）
            val highestY = world.getHighestBlockYAt(center.first.toInt(), center.third.toInt()) + 1
            Location(world, center.first, highestY.toDouble(), center.third)
        } else {
            Location(world, center.first, center.second, center.third)
        }
    }

    private fun executeTeleport(player: Player, location: Location) {
        // 使用 teleportAsync 以兼容 Folia
        player.teleportAsync(location).thenAccept { success ->
            if (success) {
                teleportCooldowns[player.uniqueId] = System.currentTimeMillis()
            }
        }
    }

    // ========== 传送请求（吟唱）==========

    fun startTeleportRequest(player: Player, target: Landmark) {
        val loc = player.location
        teleportRequests[player.uniqueId] = TeleportRequest(
            playerUuid = player.uniqueId,
            targetLandmark = target,
            startLocation = Triple(loc.x, loc.y, loc.z)
        )
    }

    fun getTeleportRequest(playerUuid: UUID): TeleportRequest? {
        return teleportRequests[playerUuid]
    }

    fun cancelTeleportRequest(playerUuid: UUID) {
        teleportRequests.remove(playerUuid)
    }

    fun completeTeleportRequest(player: Player): Boolean {
        val request = teleportRequests.remove(player.uniqueId) ?: return false
        val world = Bukkit.getWorld(request.targetLandmark.world) ?: return false
        val location = getWarpLocation(request.targetLandmark, world) ?: return false
        executeTeleport(player, location)
        return true
    }

    // ========== 进入提示冷却 ==========

    fun canShowEnterMessage(playerUuid: UUID, landmarkId: String): Boolean {
        if (enterMessageCooldownSeconds <= 0) return true
        val cooldowns = enterMessageCooldowns.getOrPut(playerUuid) { mutableMapOf() }
        val lastTime = cooldowns[landmarkId] ?: 0
        return System.currentTimeMillis() - lastTime > enterMessageCooldownSeconds * 1000L
    }

    fun setEnterMessageCooldown(playerUuid: UUID, landmarkId: String) {
        val cooldowns = enterMessageCooldowns.getOrPut(playerUuid) { mutableMapOf() }
        cooldowns[landmarkId] = System.currentTimeMillis()
    }

    // ========== 离开提示冷却 ==========

    fun canShowLeaveMessage(playerUuid: UUID, landmarkId: String): Boolean {
        if (enterMessageCooldownSeconds <= 0) return true
        val cooldowns = leaveMessageCooldowns.getOrPut(playerUuid) { mutableMapOf() }
        val lastTime = cooldowns[landmarkId] ?: 0
        return System.currentTimeMillis() - lastTime > enterMessageCooldownSeconds * 1000L
    }

    fun setLeaveMessageCooldown(playerUuid: UUID, landmarkId: String) {
        val cooldowns = leaveMessageCooldowns.getOrPut(playerUuid) { mutableMapOf() }
        cooldowns[landmarkId] = System.currentTimeMillis()
    }

    // ========== 维护者 ==========

    fun isMaintainer(playerUuid: UUID, landmark: Landmark): Boolean {
        return landmark.maintainers.contains(playerUuid.toString())
    }

    fun addMaintainer(landmark: Landmark, playerUuid: UUID): Boolean {
        if (landmark.maintainers.contains(playerUuid.toString())) return false
        landmark.maintainers.add(playerUuid.toString())
        storage.updateLandmark(landmark)
        return true
    }

    fun removeMaintainer(landmark: Landmark, playerUuid: UUID): Boolean {
        val removed = landmark.maintainers.remove(playerUuid.toString())
        if (removed) storage.updateLandmark(landmark)
        return removed
    }

    // ========== 清理 ==========

    fun cleanupPlayer(playerUuid: UUID) {
        playerCurrentLandmark.remove(playerUuid)
        teleportRequests.remove(playerUuid)
        enterMessageCooldowns.remove(playerUuid)
        leaveMessageCooldowns.remove(playerUuid)
    }

    fun shutdown() {
        storage.forceSaveAll()
    }
}

enum class TeleportResult {
    SUCCESS,
    NOT_IN_LANDMARK,
    NOT_UNLOCKED,
    ON_COOLDOWN,
    UNSAFE_LOCATION,
    WORLD_NOT_FOUND
}
