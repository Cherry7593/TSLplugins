package org.tsl.tSLplugins.Hat

import org.bukkit.Material
import org.tsl.tSLplugins.TSLplugins

/**
 * Hat 功能管理器
 * 负责加载配置和提供配置访问接口
 */
class HatManager(private val plugin: TSLplugins) {

    private var enabled: Boolean = true
    private var blacklistedMaterials: Set<Material> = emptySet()
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
        enabled = config.getBoolean("hat.enabled", true)

        // 读取冷却时间（秒转毫秒）
        cooldown = (config.getDouble("hat.cooldown", 0.0) * 1000).toLong()

        // 读取黑名单物品
        val blacklistStrings = config.getStringList("hat.blacklist")
        blacklistedMaterials = blacklistStrings.mapNotNull { materialName ->
            try {
                Material.valueOf(materialName.uppercase())
            } catch (e: IllegalArgumentException) {
                plugin.logger.warning("无效的物品类型: $materialName")
                null
            }
        }.toSet()

        // 读取消息配置
        val messagesSection = config.getConfigurationSection("hat.messages")
        if (messagesSection != null) {
            val prefix = messagesSection.getString("prefix", "&c[Hat]&r ")
            for (key in messagesSection.getKeys(false)) {
                if (key == "prefix") continue
                val rawMessage = messagesSection.getString(key, "")
                val processedMessage = rawMessage?.replace("%prefix%", prefix ?: "") ?: ""
                messages[key] = processedMessage
            }
        }

        plugin.logger.info("Hat 功能已加载 (启用: $enabled, 黑名单物品: ${blacklistedMaterials.size} 个)")
    }

    /**
     * 检查功能是否启用
     */
    fun isEnabled(): Boolean = enabled

    /**
     * 检查物品是否被禁止
     */
    fun isBlacklisted(material: Material): Boolean = blacklistedMaterials.contains(material)

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

