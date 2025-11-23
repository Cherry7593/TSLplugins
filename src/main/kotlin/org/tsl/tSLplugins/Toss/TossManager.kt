package org.tsl.tSLplugins.Toss

import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.tsl.tSLplugins.PlayerDataManager
import java.util.UUID

/**
 * Toss 功能管理器
 * 负责管理生物举起功能的配置和玩家状态
 */
class TossManager(
    private val plugin: JavaPlugin,
    private val dataManager: PlayerDataManager
) {

    private var enabled: Boolean = true
    private var showMessages: Boolean = false
    private var maxLiftCount: Int = 3
    private var defaultEnabled: Boolean = true
    private var throwVelocityMin: Double = 1.0
    private var throwVelocityMax: Double = 3.0
    private val blacklist: MutableSet<EntityType> = mutableSetOf()
    private val messages: MutableMap<String, String> = mutableMapOf()


    init {
        loadConfig()
    }

    /**
     * 加载配置
     */
    fun loadConfig() {
        val config = plugin.config

        // 读取是否启用
        enabled = config.getBoolean("toss.enabled", true)

        // 读取是否显示消息
        showMessages = config.getBoolean("toss.show_messages", false)

        // 读取最大举起数量
        maxLiftCount = config.getInt("toss.max_lift_count", 3)

        // 读取默认启用状态
        defaultEnabled = config.getBoolean("toss.default_enabled", true)

        // 读取投掷速度范围
        throwVelocityMin = config.getDouble("toss.throw_velocity.min", 1.0)
        throwVelocityMax = config.getDouble("toss.throw_velocity.max", 3.0)

        // 读取黑名单
        val blacklistStrings = config.getStringList("toss.blacklist")
        blacklist.clear()
        blacklistStrings.forEach { entityName ->
            try {
                val entityType = EntityType.valueOf(entityName.uppercase())
                blacklist.add(entityType)
            } catch (e: IllegalArgumentException) {
                plugin.logger.warning("[Toss] 无效的实体类型: $entityName")
            }
        }

        // 读取消息配置
        val prefix = config.getString("toss.messages.prefix", "&6[TSL喵]&r ")
        messages.clear()
        val messagesSection = config.getConfigurationSection("toss.messages")
        if (messagesSection != null) {
            for (key in messagesSection.getKeys(false)) {
                if (key == "prefix") continue
                val rawMessage = messagesSection.getString(key) ?: ""
                val processedMessage = rawMessage.replace("%prefix%", prefix ?: "")
                messages[key] = processedMessage
            }
        }

        plugin.logger.info("[Toss] 已加载配置 - 最大举起数: $maxLiftCount, 速度范围: $throwVelocityMin-$throwVelocityMax")
    }

    /**
     * 检查功能是否启用
     */
    fun isEnabled(): Boolean = enabled

    /**
     * 是否显示消息
     */
    fun isShowMessages(): Boolean = showMessages

    /**
     * 获取最大举起数量
     */
    fun getMaxLiftCount(): Int = maxLiftCount

    /**
     * 获取默认启用状态
     */
    fun isDefaultEnabled(): Boolean = defaultEnabled

    /**
     * 获取投掷速度最小值
     */
    fun getThrowVelocityMin(): Double = throwVelocityMin

    /**
     * 获取投掷速度最大值
     */
    fun getThrowVelocityMax(): Double = throwVelocityMax

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
     * 检查玩家是否启用了举起功能（从 PDC 读取）
     */
    fun isPlayerEnabled(player: Player): Boolean {
        return dataManager.getTossToggle(player, defaultEnabled)
    }

    /**
     * 切换玩家的开关状态（写入 PDC）
     */
    fun togglePlayer(player: Player): Boolean {
        val currentStatus = isPlayerEnabled(player)
        val newStatus = !currentStatus
        dataManager.setTossToggle(player, newStatus)
        return newStatus
    }

    /**
     * 获取玩家的投掷速度（从 PDC 读取）
     */
    fun getPlayerThrowVelocity(player: Player): Double {
        return dataManager.getTossVelocity(player, throwVelocityMin)
    }

    /**
     * 设置玩家的投掷速度（受配置限制）
     */
    fun setPlayerThrowVelocity(player: Player, velocity: Double): Boolean {
        if (velocity < throwVelocityMin || velocity > throwVelocityMax) {
            return false
        }
        dataManager.setTossVelocity(player, velocity)
        return true
    }

    /**
     * 设置玩家的投掷速度（不受配置限制，用于 OP/管理员）
     * 仍然会进行基本验证（0.0-10.0 范围）
     */
    fun setPlayerThrowVelocityUnrestricted(player: Player, velocity: Double) {
        // 只进行基本的合理性检查
        val clampedVelocity = velocity.coerceIn(0.0, 10.0)
        dataManager.setTossVelocity(player, clampedVelocity)
    }

    /**
     * 玩家离线时清理数据（PDC 自动保留）
     */
    @Suppress("UNUSED_PARAMETER")
    fun cleanupPlayer(uuid: UUID) {
        // PDC 数据自动保留，无需清理
    }
}

