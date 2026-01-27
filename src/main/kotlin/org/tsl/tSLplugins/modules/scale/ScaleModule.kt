package org.tsl.tSLplugins.modules.scale

import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import org.tsl.tSLplugins.SubCommandHandler
import org.tsl.tSLplugins.core.AbstractModule

/**
 * 玩家体型调整模块
 * 
 * 允许玩家调整自己的体型大小。
 * 
 * ## 功能
 * - 设置玩家体型（在配置范围内）
 * - 重置体型为默认值（1.0）
 * - 支持 bypass 权限绕过范围限制
 * 
 * ## 命令
 * - `/tsl scale <数值>` - 设置体型
 * - `/tsl scale reset` - 重置体型
 * 
 * ## 权限
 * - `tsl.scale.use` - 使用体型命令
 * - `tsl.scale.bypass` - 绕过体型范围限制
 * 
 * ## 配置
 * ```yaml
 * scale:
 *   enabled: true
 *   min: 0.8
 *   max: 1.1
 * ```
 */
class ScaleModule : AbstractModule() {
    
    override val id = "scale"
    override val configPath = "scale"
    
    // 配置项
    private var scaleMin: Double = 0.8
    private var scaleMax: Double = 1.1
    
    override fun loadConfig() {
        super.loadConfig()
        scaleMin = getConfigDouble("min", 0.8)
        scaleMax = getConfigDouble("max", 1.1)
    }
    
    override fun doEnable() {
        // Scale 模块不需要监听器，只有命令
        logInfo("体型范围: $scaleMin - $scaleMax")
    }
    
    override fun getCommandHandler(): SubCommandHandler {
        return ScaleModuleCommand(this)
    }
    
    override fun getDescription(): String = "调整玩家体型"
    
    // ==================== 体型管理 ====================
    
    /**
     * 设置玩家体型
     * 
     * @param player 玩家对象
     * @param scale 体型值
     */
    fun setPlayerScale(player: Player, scale: Double) {
        val attr = player.getAttribute(Attribute.SCALE)
        if (attr != null) {
            attr.baseValue = scale
        }
    }
    
    /**
     * 重置玩家体型为默认值（1.0）
     */
    fun resetPlayerScale(player: Player) {
        setPlayerScale(player, 1.0)
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
    
    /**
     * 获取模块消息
     */
    fun getModuleMessage(key: String, vararg replacements: Pair<String, String>): String {
        return getMessage(key, *replacements)
    }
}
