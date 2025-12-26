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

    private val msg get() = plugin.messageManager

    init {
        loadConfig()
    }

    /**
     * 加载配置
     */
    fun loadConfig() {
        val config = plugin.config
        enabled = config.getBoolean("fixghost.enabled", true)
        defaultRadius = config.getInt("fixghost.default-radius", 5).coerceIn(1, 10)
        maxRadius = config.getInt("fixghost.max-radius", 5).coerceIn(1, 10)
        cooldown = (config.getDouble("fixghost.cooldown", 5.0) * 1000).toLong()
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
    fun getMessage(key: String, vararg replacements: Pair<String, String>): String {
        return msg.getModule("fixghost", key, *replacements)
    }
}

