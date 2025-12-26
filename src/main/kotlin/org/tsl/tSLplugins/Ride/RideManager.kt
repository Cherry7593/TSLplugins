package org.tsl.tSLplugins.Ride

import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.tsl.tSLplugins.PlayerDataManager
import org.tsl.tSLplugins.TSLplugins
import java.util.UUID

/**
 * Ride 功能管理器
 * 负责管理骑乘功能的配置和玩家状态
 */
class RideManager(
    private val plugin: JavaPlugin,
    private val dataManager: PlayerDataManager
) {

    private var enabled: Boolean = true
    private var defaultEnabled: Boolean = true
    private val blacklist: MutableSet<EntityType> = mutableSetOf()

    private val msg get() = (plugin as TSLplugins).messageManager

    init {
        loadConfig()
    }

    /**
     * 加载配置
     */
    fun loadConfig() {
        val config = plugin.config
        enabled = config.getBoolean("ride.enabled", true)
        defaultEnabled = config.getBoolean("ride.default_enabled", true)

        val blacklistStrings = config.getStringList("ride.blacklist")
        blacklist.clear()
        blacklistStrings.forEach { entityName ->
            try {
                blacklist.add(EntityType.valueOf(entityName.uppercase()))
            } catch (e: IllegalArgumentException) {
                plugin.logger.warning("[Ride] 无效的实体类型: $entityName")
            }
        }
    }

    /**
     * 检查功能是否启用
     */
    fun isEnabled(): Boolean = enabled

    /**
     * 获取默认启用状态
     */
    fun isDefaultEnabled(): Boolean = defaultEnabled

    /**
     * 检查实体是否在黑名单中
     */
    fun isEntityBlacklisted(entityType: EntityType): Boolean {
        return blacklist.contains(entityType)
    }

    /**
     * 获取消息文本
     */
    fun getMessage(key: String, vararg replacements: Pair<String, String>): String {
        return msg.getModule("ride", key, *replacements)
    }

    /**
     * 检查玩家是否启用了骑乘功能（从 PDC 读取）
     */
    fun isPlayerEnabled(player: Player): Boolean {
        return dataManager.getRideToggle(player, defaultEnabled)
    }

    /**
     * 切换玩家的开关状态（写入 PDC）
     */
    fun togglePlayer(player: Player): Boolean {
        val currentStatus = isPlayerEnabled(player)
        val newStatus = !currentStatus
        dataManager.setRideToggle(player, newStatus)
        return newStatus
    }

    /**
     * 玩家离线时清理数据（PDC 自动保留）
     */
    @Suppress("UNUSED_PARAMETER")
    fun cleanupPlayer(uuid: UUID) {
        // PDC 数据自动保留，无需清理
    }
}

