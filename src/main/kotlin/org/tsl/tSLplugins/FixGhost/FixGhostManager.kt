package org.tsl.tSLplugins.FixGhost

import org.tsl.tSLplugins.TSLplugins

/**
 * FixGhost 功能管理器
 * 负责加载配置和提供配置访问接口
 */
class FixGhostManager(private val plugin: TSLplugins) {

    private var enabled: Boolean = true
    private var defaultRadius: Int = 5
    private var maxRadius: Int = 5
    private var cooldown: Long = 0L
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
        enabled = config.getBoolean("fixghost.enabled", true)

        // 读取默认半径
        defaultRadius = config.getInt("fixghost.default-radius", 5).coerceIn(1, 10)

        // 读取最大半径
        maxRadius = config.getInt("fixghost.max-radius", 5).coerceIn(1, 10)

        // 读取冷却时间（秒转毫秒）
        cooldown = (config.getDouble("fixghost.cooldown", 5.0) * 1000).toLong()

        // 读取消息配置
        val messagesSection = config.getConfigurationSection("fixghost.messages")
        if (messagesSection != null) {
            val prefix = messagesSection.getString("prefix", "&e[FixGhost]&r ")
            for (key in messagesSection.getKeys(false)) {
                if (key == "prefix") continue
                val rawMessage = messagesSection.getString(key, "")
                val processedMessage = rawMessage?.replace("%prefix%", prefix ?: "") ?: ""
                messages[key] = processedMessage
            }
        }

        plugin.logger.info("FixGhost 功能已加载 (启用: $enabled, 默认半径: $defaultRadius, 最大半径: $maxRadius)")
    }

    /**
     * 检查功能是否启用
     */
    fun isEnabled(): Boolean = enabled

    /**
     * 获取默认半径
     */
    fun getDefaultRadius(): Int = defaultRadius

    /**
     * 获取最大半径
     */
    fun getMaxRadius(): Int = maxRadius

    /**
     * 获取冷却时间（毫秒）
     */
    fun getCooldown(): Long = cooldown

    /**
     * 获取消息
     */
    fun getMessage(key: String, placeholders: Map<String, String> = emptyMap()): String {
        var message = messages[key] ?: return "§c未知消息: $key"

        // 替换占位符
        placeholders.forEach { (placeholder, value) ->
            message = message.replace(placeholder, value)
        }

        // 转换颜色代码
        return message.replace('&', '§')
    }
}

