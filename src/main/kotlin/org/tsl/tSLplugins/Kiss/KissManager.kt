package org.tsl.tSLplugins.Kiss

import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.tsl.tSLplugins.PlayerDataManager
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Kiss 功能管理器
 * 负责管理亲吻功能的配置和状态
 */
class KissManager(
    private val plugin: JavaPlugin,
    private val dataManager: PlayerDataManager
) {

    private var enabled: Boolean = true
    private var cooldown: Long = 1000 // 默认 1 秒冷却
    private val messages: MutableMap<String, String> = mutableMapOf()


    // 玩家冷却时间（UUID -> 最后使用时间戳）
    private val playerCooldowns: MutableMap<UUID, Long> = ConcurrentHashMap()

    // 统计数据：亲吻次数（UUID -> 次数）
    private val kissCount: MutableMap<UUID, Int> = ConcurrentHashMap()

    // 统计数据：被亲吻次数（UUID -> 次数）
    private val kissedCount: MutableMap<UUID, Int> = ConcurrentHashMap()

    init {
        loadConfig()
    }

    /**
     * 加载配置
     */
    fun loadConfig() {
        val config = plugin.config

        // 读取是否启用
        enabled = config.getBoolean("kiss.enabled", true)

        // 读取冷却时间（秒）
        val cooldownSeconds = config.getDouble("kiss.cooldown", 1.0)
        cooldown = (cooldownSeconds * 1000).toLong()

        // 读取消息配置
        val prefix = config.getString("kiss.messages.prefix", "&6[TSL喵]&r ")
        messages.clear()
        val messagesSection = config.getConfigurationSection("kiss.messages")
        if (messagesSection != null) {
            for (key in messagesSection.getKeys(false)) {
                if (key == "prefix") continue
                val rawMessage = messagesSection.getString(key) ?: ""
                val processedMessage = rawMessage.replace("%prefix%", prefix ?: "")
                messages[key] = processedMessage
            }
        }

        plugin.logger.info("[Kiss] 已加载配置 - 冷却时间: ${cooldownSeconds}秒")
    }

    /**
     * 检查功能是否启用
     */
    fun isEnabled(): Boolean = enabled

    /**
     * 获取冷却时间（毫秒）
     */
    fun getCooldown(): Long = cooldown

    /**
     * 检查玩家是否启用了功能（从 PDC 读取）
     */
    fun isPlayerEnabled(player: Player): Boolean {
        return dataManager.getKissToggle(player, true)
    }

    /**
     * 切换玩家的功能开关状态（写入 PDC）
     * @return 切换后的状态
     */
    fun togglePlayer(player: Player): Boolean {
        val currentStatus = isPlayerEnabled(player)
        val newStatus = !currentStatus
        dataManager.setKissToggle(player, newStatus)
        return newStatus
    }

    /**
     * 检查玩家是否在冷却中
     */
    fun isInCooldown(uuid: UUID): Boolean {
        val lastUsed = playerCooldowns[uuid] ?: return false
        return System.currentTimeMillis() - lastUsed < cooldown
    }

    /**
     * 获取玩家剩余冷却时间（秒）
     */
    fun getRemainingCooldown(uuid: UUID): Double {
        val lastUsed = playerCooldowns[uuid] ?: return 0.0
        val remaining = cooldown - (System.currentTimeMillis() - lastUsed)
        return if (remaining > 0) remaining / 1000.0 else 0.0
    }

    /**
     * 设置玩家冷却时间
     */
    fun setCooldown(uuid: UUID) {
        playerCooldowns[uuid] = System.currentTimeMillis()
    }

    /**
     * 增加玩家亲吻次数
     */
    fun incrementKissCount(uuid: UUID) {
        kissCount[uuid] = kissCount.getOrDefault(uuid, 0) + 1
    }

    /**
     * 增加玩家被亲吻次数
     */
    fun incrementKissedCount(uuid: UUID) {
        kissedCount[uuid] = kissedCount.getOrDefault(uuid, 0) + 1
    }

    /**
     * 获取玩家亲吻次数
     */
    fun getKissCount(uuid: UUID): Int {
        return kissCount.getOrDefault(uuid, 0)
    }

    /**
     * 获取玩家被亲吻次数
     */
    fun getKissedCount(uuid: UUID): Int {
        return kissedCount.getOrDefault(uuid, 0)
    }

    /**
     * 清理玩家数据（仅清理冷却，PDC 和统计数据保留）
     */
    fun cleanupPlayer(uuid: UUID) {
        playerCooldowns.remove(uuid)
        // PDC 数据和统计数据保留
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
}

