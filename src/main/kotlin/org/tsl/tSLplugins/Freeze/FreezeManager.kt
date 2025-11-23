package org.tsl.tSLplugins.Freeze

import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Freeze 功能管理器
 * 负责管理玩家冻结状态
 */
class FreezeManager(private val plugin: JavaPlugin) {

    private var enabled: Boolean = true
    private val messages: MutableMap<String, String> = mutableMapOf()

    // 冻结的玩家（UUID -> 过期时间戳，-1表示永久）
    private val frozenPlayers: MutableMap<UUID, Long> = ConcurrentHashMap()

    init {
        loadConfig()
        startExpirationCheck()
    }

    /**
     * 加载配置
     */
    fun loadConfig() {
        val config = plugin.config

        // 读取是否启用
        enabled = config.getBoolean("freeze.enabled", true)

        // 读取消息配置
        val prefix = config.getString("freeze.messages.prefix", "&c[冻结]&r ")
        messages.clear()
        val messagesSection = config.getConfigurationSection("freeze.messages")
        if (messagesSection != null) {
            for (key in messagesSection.getKeys(false)) {
                if (key == "prefix") continue
                val rawMessage = messagesSection.getString(key) ?: ""
                val processedMessage = rawMessage.replace("%prefix%", prefix ?: "")
                messages[key] = processedMessage
            }
        }

        plugin.logger.info("[Freeze] 已加载配置")
    }

    /**
     * 检查功能是否启用
     */
    fun isEnabled(): Boolean = enabled

    /**
     * 冻结玩家
     * @param uuid 玩家 UUID
     * @param duration 持续时间（秒），-1 表示永久
     */
    fun freezePlayer(uuid: UUID, duration: Int = -1) {
        val expireTime = if (duration > 0) {
            System.currentTimeMillis() + (duration * 1000L)
        } else {
            -1L
        }
        frozenPlayers[uuid] = expireTime
    }

    /**
     * 解冻玩家
     */
    fun unfreezePlayer(uuid: UUID): Boolean {
        return frozenPlayers.remove(uuid) != null
    }

    /**
     * 检查玩家是否被冻结
     */
    fun isFrozen(uuid: UUID): Boolean {
        val expireTime = frozenPlayers[uuid] ?: return false

        // 检查是否过期
        if (expireTime > 0 && System.currentTimeMillis() > expireTime) {
            frozenPlayers.remove(uuid)
            return false
        }

        return true
    }

    /**
     * 获取被冻结的玩家列表
     */
    fun getFrozenPlayers(): Map<UUID, Long> {
        return frozenPlayers.toMap()
    }

    /**
     * 获取剩余冻结时间（秒）
     * @return 剩余时间，-1 表示永久，0 表示已过期
     */
    fun getRemainingTime(uuid: UUID): Int {
        val expireTime = frozenPlayers[uuid] ?: return 0

        if (expireTime < 0) {
            return -1 // 永久冻结
        }

        val remaining = (expireTime - System.currentTimeMillis()) / 1000
        return if (remaining > 0) remaining.toInt() else 0
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
     * 启动过期检查任务
     */
    private fun startExpirationCheck() {
        // 每秒检查一次过期的冻结
        Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, { _ ->
            val currentTime = System.currentTimeMillis()
            val expired = mutableListOf<UUID>()

            frozenPlayers.forEach { (uuid, expireTime) ->
                if (expireTime > 0 && currentTime > expireTime) {
                    expired.add(uuid)
                }
            }

            // 移除过期的冻结并通知玩家
            expired.forEach { uuid ->
                frozenPlayers.remove(uuid)
                val player = Bukkit.getPlayer(uuid)
                if (player != null && player.isOnline) {
                    player.sendMessage(getMessage("expired"))
                }
            }
        }, 20L, 20L) // 延迟1秒，每秒执行一次
    }

    /**
     * 清理玩家数据
     */
    fun cleanupPlayer(uuid: UUID) {
        // 冻结状态在玩家离线后��留
        // 不清理数据
    }
}

