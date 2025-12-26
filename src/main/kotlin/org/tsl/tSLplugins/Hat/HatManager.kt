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

    private val msg get() = plugin.messageManager

    init {
        loadConfig()
    }

    /**
     * 加载配置
     */
    fun loadConfig() {
        val config = plugin.config
        enabled = config.getBoolean("hat.enabled", true)
        cooldown = (config.getDouble("hat.cooldown", 0.0) * 1000).toLong()

        val blacklistStrings = config.getStringList("hat.blacklist")
        blacklistedMaterials = blacklistStrings.mapNotNull { materialName ->
            try {
                Material.valueOf(materialName.uppercase())
            } catch (e: IllegalArgumentException) {
                plugin.logger.warning("无效的物品类型: $materialName")
                null
            }
        }.toSet()
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
    fun getMessage(key: String, vararg replacements: Pair<String, String>): String {
        return msg.getModule("hat", key, *replacements)
    }
}

