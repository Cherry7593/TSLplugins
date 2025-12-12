package org.tsl.tSLplugins.EndDragon

import org.bukkit.plugin.java.JavaPlugin

/**
 * 末影龙控制管理器
 * 管理末影龙的破坏行为
 * 仅支持禁止破坏方块功能
 */
class EndDragonManager(private val plugin: JavaPlugin) {

    // ===== 配置缓存 =====
    private var enabled: Boolean = true
    private var disableDamage: Boolean = true      // 禁止末影龙破坏方块

    init {
        loadConfig()
    }

    /**
     * 加载配置
     */
    fun loadConfig() {
        val config = plugin.config

        enabled = config.getBoolean("enddragon.enabled", true)
        disableDamage = config.getBoolean("enddragon.disable-damage", true)

        plugin.logger.info("[EndDragon] 配置已加载 - 启用: $enabled, 禁止破坏: $disableDamage")
    }

    /**
     * 功能是否启用
     */
    fun isEnabled(): Boolean = enabled

    /**
     * 是否禁止末影龙破坏方块
     */
    fun isDisableDamage(): Boolean = disableDamage && enabled
}

