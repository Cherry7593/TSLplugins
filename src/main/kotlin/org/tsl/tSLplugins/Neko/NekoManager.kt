package org.tsl.tSLplugins.Neko

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 猫娘模式管理器
 * 负责管理玩家的猫娘状态、计时和配置
 */
class NekoManager(private val plugin: JavaPlugin) {

    private var enabled: Boolean = true
    private var suffix: String = "喵~"
    private var scanIntervalTicks: Long = 20L

    // 活跃的猫娘效果：playerUuid -> NekoEffect
    private val activeEffects: ConcurrentHashMap<UUID, NekoEffect> = ConcurrentHashMap()

    // 消息配置
    private val messages: MutableMap<String, String> = mutableMapOf()

    init {
        loadConfig()
    }

    /**
     * 加载配置
     */
    fun loadConfig() {
        val config = plugin.config

        enabled = config.getBoolean("neko.enabled", true)
        suffix = config.getString("neko.suffix", "喵~") ?: "喵~"
        scanIntervalTicks = config.getLong("neko.scan-interval-ticks", 20L)

        // 读取消息配置
        val prefix = config.getString("neko.messages.prefix", "&d[猫娘]&r ") ?: "&d[猫娘]&r "
        messages.clear()
        val messagesSection = config.getConfigurationSection("neko.messages")
        if (messagesSection != null) {
            for (key in messagesSection.getKeys(false)) {
                if (key == "prefix") continue
                val rawMessage = messagesSection.getString(key) ?: ""
                messages[key] = rawMessage.replace("%prefix%", prefix)
            }
        }

        if (enabled) {
            plugin.logger.info("[Neko] 猫娘模式已启用，后缀: $suffix")
        } else {
            plugin.logger.info("[Neko] 猫娘模式已禁用")
        }
    }

    /**
     * 启动过期扫描任务
     */
    fun startExpirationTask() {
        if (!enabled) return

        Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, { _ ->
            scanAndRemoveExpired()
        }, scanIntervalTicks, scanIntervalTicks)

        plugin.logger.info("[Neko] 过期扫描任务已启动，间隔: ${scanIntervalTicks} ticks")
    }

    /**
     * 扫描并移除过期效果
     */
    private fun scanAndRemoveExpired() {
        val expiredPlayers = mutableListOf<UUID>()

        activeEffects.forEach { (uuid, effect) ->
            if (effect.isExpired()) {
                expiredPlayers.add(uuid)
            }
        }

        expiredPlayers.forEach { uuid ->
            val effect = activeEffects.remove(uuid)
            if (effect != null) {
                plugin.logger.info("[Neko] ${effect.playerName} 的猫娘效果已过期")
            }
        }
    }

    /**
     * 设置玩家为猫娘
     *
     * @param player 目标玩家
     * @param durationMs 持续时间（毫秒），-1 表示永久
     * @param source 来源
     * @return 是否成功
     */
    fun setNeko(player: Player, durationMs: Long, source: String = "command"): Boolean {
        if (!enabled) return false

        val expireAt = if (durationMs == -1L) -1L else System.currentTimeMillis() + durationMs

        val effect = NekoEffect(
            playerUuid = player.uniqueId,
            playerName = player.name,
            expireAt = expireAt,
            source = source
        )

        activeEffects[player.uniqueId] = effect
        plugin.logger.info("[Neko] ${player.name} 已成为猫娘，持续: ${effect.formatRemainingTime()}")
        return true
    }

    /**
     * 取消玩家的猫娘状态
     *
     * @param player 目标玩家
     * @return 是否成功（玩家是否原本是猫娘）
     */
    fun resetNeko(player: Player): Boolean {
        val removed = activeEffects.remove(player.uniqueId)
        if (removed != null) {
            plugin.logger.info("[Neko] ${player.name} 已不再是猫娘")
            return true
        }
        return false
    }

    /**
     * 通过 UUID 取消猫娘状态
     */
    fun resetNeko(uuid: UUID): Boolean {
        val removed = activeEffects.remove(uuid)
        return removed != null
    }

    /**
     * 检查玩家是否是猫娘
     */
    fun isNeko(player: Player): Boolean {
        val effect = activeEffects[player.uniqueId] ?: return false
        if (effect.isExpired()) {
            activeEffects.remove(player.uniqueId)
            return false
        }
        return true
    }

    /**
     * 检查玩家是否是猫娘（通过 UUID）
     */
    fun isNeko(uuid: UUID): Boolean {
        val effect = activeEffects[uuid] ?: return false
        if (effect.isExpired()) {
            activeEffects.remove(uuid)
            return false
        }
        return true
    }

    /**
     * 获取玩家的猫娘效果
     */
    fun getNekoEffect(uuid: UUID): NekoEffect? {
        val effect = activeEffects[uuid] ?: return null
        if (effect.isExpired()) {
            activeEffects.remove(uuid)
            return null
        }
        return effect
    }

    /**
     * 获取所有猫娘玩家
     */
    fun getAllNekos(): List<NekoEffect> {
        return activeEffects.values.filter { !it.isExpired() }
    }

    /**
     * 获取猫娘后缀
     */
    fun getSuffix(): String = suffix

    /**
     * 检查功能是否启用
     */
    fun isEnabled(): Boolean = enabled

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
     * 解析持续时间字符串
     * 支持格式：10s, 5m, 2h, 1d, 纯数字（默认秒）
     *
     * @return 毫秒数，解析失败返回 null
     */
    fun parseDuration(input: String): Long? {
        val trimmed = input.trim().lowercase()

        // 纯数字，默认为秒
        trimmed.toLongOrNull()?.let { return it * 1000 }

        // 带单位的格式
        val regex = Regex("""^(\d+)([smhd])$""")
        val match = regex.matchEntire(trimmed) ?: return null

        val value = match.groupValues[1].toLongOrNull() ?: return null
        val unit = match.groupValues[2]

        return when (unit) {
            "s" -> value * 1000
            "m" -> value * 60 * 1000
            "h" -> value * 60 * 60 * 1000
            "d" -> value * 24 * 60 * 60 * 1000
            else -> null
        }
    }

    /**
     * 玩家下线时清理缓存（可选，效果会自动过期）
     */
    @Suppress("UNUSED_PARAMETER")
    fun onPlayerQuit(player: Player) {
        // 不移除效果，让效果持续到过期
        // 如果需要下线时移除效果，取消注释下面这行
        // activeEffects.remove(player.uniqueId)
    }

    /**
     * 关闭管理器
     */
    fun shutdown() {
        activeEffects.clear()
        plugin.logger.info("[Neko] 管理器已关闭")
    }
}

