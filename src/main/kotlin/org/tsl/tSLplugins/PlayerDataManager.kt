package org.tsl.tSLplugins

import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin

/**
 * 玩家数据管理器
 * 使用 YAML 存储玩家个人配置，支持从 PDC 自动迁移
 */
class PlayerDataManager(private val plugin: JavaPlugin) {

    /** Profile 存储 */
    private val profileStore = TSLPlayerProfileStore(plugin)

    // PDC Keys（仅用于迁移）
    private val kissToggleKey = NamespacedKey(plugin, "kiss_toggle")
    private val rideToggleKey = NamespacedKey(plugin, "ride_toggle")
    private val tossToggleKey = NamespacedKey(plugin, "toss_toggle")
    private val tossVelocityKey = NamespacedKey(plugin, "toss_velocity")

    // ==================== 玩家生命周期 ====================

    /**
     * 玩家加入时加载配置
     * 如果未迁移则自动从 PDC 迁移数据
     */
    fun onPlayerJoin(player: Player) {
        val uuid = player.uniqueId
        val name = player.name

        // 加载配置（如果文件不存在会创建新的）
        val profile = profileStore.load(uuid, name)

        // 如果未迁移，从 PDC 读取旧数据
        if (!profile.migratedFromPdc) {
            migrateFromPdc(player, profile)
        }

        // 更新玩家名称（可能改名）
        profile.playerName = name
    }

    /**
     * 玩家退出时保存配置
     */
    fun onPlayerQuit(player: Player) {
        val uuid = player.uniqueId

        // 保存配置
        profileStore.save(uuid)

        // 从缓存移除（节省内存）
        profileStore.remove(uuid)
    }

    /**
     * 插件关闭时保存所有配置
     */
    fun saveAll() {
        profileStore.saveAll()
    }

    // ==================== PDC 迁移 ====================

    /**
     * 从 PDC 迁移玩家数据
     */
    private fun migrateFromPdc(player: Player, profile: TSLPlayerProfile) {
        val pdc = player.persistentDataContainer
        var migrated = false

        // 迁移 Kiss 开关
        if (pdc.has(kissToggleKey, PersistentDataType.BOOLEAN)) {
            profile.kissEnabled = pdc.get(kissToggleKey, PersistentDataType.BOOLEAN) ?: true
            pdc.remove(kissToggleKey)
            migrated = true
        }

        // 迁移 Ride 开关
        if (pdc.has(rideToggleKey, PersistentDataType.BOOLEAN)) {
            profile.rideEnabled = pdc.get(rideToggleKey, PersistentDataType.BOOLEAN) ?: true
            pdc.remove(rideToggleKey)
            migrated = true
        }

        // 迁移 Toss 开关
        if (pdc.has(tossToggleKey, PersistentDataType.BOOLEAN)) {
            profile.tossEnabled = pdc.get(tossToggleKey, PersistentDataType.BOOLEAN) ?: true
            pdc.remove(tossToggleKey)
            migrated = true
        }

        // 迁移 Toss 速度
        if (pdc.has(tossVelocityKey, PersistentDataType.DOUBLE)) {
            profile.tossVelocity = pdc.get(tossVelocityKey, PersistentDataType.DOUBLE) ?: 1.5
            pdc.remove(tossVelocityKey)
            migrated = true
        }

        // 标记已迁移
        if (migrated) {
            profile.migratedFromPdc = true
            profileStore.save(profile)
            plugin.logger.info("[PlayerData] 已从 PDC 迁移玩家数据: ${player.name}")
        } else {
            // 没有旧数据，直接标记已迁移
            profile.migratedFromPdc = true
        }
    }

    // ==================== Kiss 功能 ====================

    /**
     * 获取玩家的 Kiss 开关状态
     */
    fun getKissToggle(player: Player, defaultValue: Boolean = true): Boolean {
        return profileStore.get(player.uniqueId)?.kissEnabled ?: defaultValue
    }

    /**
     * 设置玩家的 Kiss 开关状态
     */
    fun setKissToggle(player: Player, enabled: Boolean) {
        val profile = profileStore.getOrCreate(player.uniqueId, player.name)
        profile.kissEnabled = enabled
        // 不立即保存，等玩家退出时保存
    }

    // ==================== Ride 功能 ====================

    /**
     * 获取玩家的 Ride 开关状态
     */
    fun getRideToggle(player: Player, defaultValue: Boolean = true): Boolean {
        return profileStore.get(player.uniqueId)?.rideEnabled ?: defaultValue
    }

    /**
     * 设置玩家的 Ride 开关状态
     */
    fun setRideToggle(player: Player, enabled: Boolean) {
        val profile = profileStore.getOrCreate(player.uniqueId, player.name)
        profile.rideEnabled = enabled
    }

    // ==================== Toss 功能 ====================

    /**
     * 获取玩家的 Toss 开关状态
     */
    fun getTossToggle(player: Player, defaultValue: Boolean = true): Boolean {
        return profileStore.get(player.uniqueId)?.tossEnabled ?: defaultValue
    }

    /**
     * 设置玩家的 Toss 开关状态
     */
    fun setTossToggle(player: Player, enabled: Boolean) {
        val profile = profileStore.getOrCreate(player.uniqueId, player.name)
        profile.tossEnabled = enabled
    }

    /**
     * 获取玩家的 Toss 投掷速度
     */
    fun getTossVelocity(player: Player, defaultValue: Double = 1.5): Double {
        return profileStore.get(player.uniqueId)?.tossVelocity ?: defaultValue
    }

    /**
     * 设置玩家的 Toss 投掷速度
     */
    fun setTossVelocity(player: Player, velocity: Double) {
        val profile = profileStore.getOrCreate(player.uniqueId, player.name)
        profile.tossVelocity = velocity
    }

    // ==================== Ignore 功能 ====================

    /**
     * 获取玩家的屏蔽列表
     */
    fun getIgnoreList(player: Player): Set<java.util.UUID> {
        return profileStore.get(player.uniqueId)?.ignoreList?.toSet() ?: emptySet()
    }

    /**
     * 设置玩家的屏蔽列表
     */
    fun setIgnoreList(player: Player, ignoreList: Set<java.util.UUID>) {
        val profile = profileStore.getOrCreate(player.uniqueId, player.name)
        profile.ignoreList = ignoreList.toMutableSet()
    }

    // ==================== 工具方法 ====================

    /**
     * 获取 Profile Store（供其他模块使用）
     */
    fun getProfileStore(): TSLPlayerProfileStore {
        return profileStore
    }

    /**
     * 清除玩家的所有数据（调试用）
     */
    fun clearPlayerData(player: Player) {
        val uuid = player.uniqueId
        val profile = profileStore.get(uuid)

        if (profile != null) {
            // 重置所有配置为默认值
            profile.kissEnabled = true
            profile.rideEnabled = true
            profile.tossEnabled = true
            profile.tossVelocity = 1.5
            profileStore.save(profile)
        }

        // 清除 PDC 数据
        val pdc = player.persistentDataContainer
        pdc.remove(kissToggleKey)
        pdc.remove(rideToggleKey)
        pdc.remove(tossToggleKey)
        pdc.remove(tossVelocityKey)

        plugin.logger.info("[PlayerData] 已清除玩家数据: ${player.name}")
    }
}

