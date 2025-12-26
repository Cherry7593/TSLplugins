package org.tsl.tSLplugins.Scale

import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.tsl.tSLplugins.TSLplugins

/**
 * 玩家体型管理器
 * 负责处理玩家体型调整的配置和实现
 */
class ScaleManager(private val plugin: JavaPlugin) {

    private var scaleMin: Double = 0.8
    private var scaleMax: Double = 1.1

    init {
        loadConfig()
    }

    private val msg get() = (plugin as TSLplugins).messageManager

    /**
     * 从配置文件加载设置
     */
    fun loadConfig() {
        val config = plugin.config
        scaleMin = config.getDouble("scale.min", 0.8)
        scaleMax = config.getDouble("scale.max", 1.1)
    }

    /**
     * 检查体型调整功能是否启用
     */
    fun isEnabled(): Boolean {
        return plugin.config.getBoolean("scale.enabled", true)
    }

    /**
     * 获取配置的最小体型
     */
    fun getScaleMin(): Double = scaleMin

    /**
     * 获取配置的最大体型
     */
    fun getScaleMax(): Double = scaleMax

    /**
     * 设置玩家体型
     * @param player 玩家对象
     * @param scale 体型值（通常在 0.1 到 2.0 之间）
     */
    fun setPlayerScale(player: Player, scale: Double) {
        val attr = player.getAttribute(Attribute.SCALE)
        if (attr != null) {
            attr.baseValue = scale
        }
    }

    /**
     * 重置玩家体型为默认值（1.0）
     * @param player 玩家对象
     */
    fun resetPlayerScale(player: Player) {
        setPlayerScale(player, 1.0)
    }

    /**
     * 获取消息
     */
    fun getMessage(key: String, vararg replacements: Pair<String, String>): String {
        return msg.getModule("scale", key, *replacements)
    }

    /**
     * 验证体型值是否在允许范围内
     */
    fun isValidScale(scale: Double): Boolean {
        return scale >= scaleMin && scale <= scaleMax
    }

    /**
     * 获取所有有效的体型值列表（用于 Tab 补全）
     */
    fun getValidScales(): List<String> {
        val list = mutableListOf<String>()
        var v = scaleMin
        while (v <= scaleMax + 0.0001) {
            list.add(String.format("%.1f", v))
            v += 0.1
        }
        return list
    }
}

