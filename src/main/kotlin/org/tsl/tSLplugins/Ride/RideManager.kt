package org.tsl.tSLplugins.Ride

import org.bukkit.entity.EntityType
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Ride 功能管理器
 * 负责管理骑乘功能的配置和玩家状态
 */
class RideManager(private val plugin: JavaPlugin) {

    private var enabled: Boolean = true
    private var defaultEnabled: Boolean = true
    private val blacklist: MutableSet<EntityType> = mutableSetOf()
    private val messages: MutableMap<String, String> = mutableMapOf()

    // 玩家开关状态（UUID -> 是否启用）
    private val playerToggleStatus = ConcurrentHashMap<UUID, Boolean>()

    init {
        loadConfig()
    }

    /**
     * 加载配置
     */
    fun loadConfig() {
        val config = plugin.config

        // 读取是否启用
        enabled = config.getBoolean("ride.enabled", true)

        // 读取默认启用状态
        defaultEnabled = config.getBoolean("ride.default_enabled", true)

        // 读取黑名单
        val blacklistStrings = config.getStringList("ride.blacklist")
        blacklist.clear()
        blacklistStrings.forEach { entityName ->
            try {
                val entityType = EntityType.valueOf(entityName.uppercase())
                blacklist.add(entityType)
            } catch (e: IllegalArgumentException) {
                plugin.logger.warning("[Ride] 无效的实体类型: $entityName")
            }
        }

        // 读取消息配置
        val prefix = config.getString("ride.messages.prefix", "&6[TSL喵]&r ")
        messages.clear()
        val messagesSection = config.getConfigurationSection("ride.messages")
        if (messagesSection != null) {
            for (key in messagesSection.getKeys(false)) {
                if (key == "prefix") continue
                val rawMessage = messagesSection.getString(key) ?: ""
                val processedMessage = rawMessage.replace("%prefix%", prefix ?: "")
                messages[key] = processedMessage
            }
        }

        plugin.logger.info("[Ride] 已加载配置 - 默认状态: ${if (defaultEnabled) "启用" else "禁用"}, 黑名单: ${blacklist.size} 种生物")
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
        var message = messages[key] ?: key
        for ((placeholder, value) in replacements) {
            message = message.replace("{$placeholder}", value)
        }
        return message
    }

    /**
     * 检查玩家是否启用了骑乘功能
     */
    fun isPlayerEnabled(uuid: UUID): Boolean {
        return playerToggleStatus.getOrDefault(uuid, defaultEnabled)
    }

    /**
     * 切换玩家的开关状态
     */
    fun togglePlayer(uuid: UUID): Boolean {
        val currentStatus = isPlayerEnabled(uuid)
        val newStatus = !currentStatus
        playerToggleStatus[uuid] = newStatus
        return newStatus
    }

    /**
     * 玩家离线时清理数据（保留开关状态）
     */
    @Suppress("UNUSED_PARAMETER")
    fun cleanupPlayer(uuid: UUID) {
        // 不清理 playerToggleStatus
        // 保持玩家的偏好设置
    }
}

