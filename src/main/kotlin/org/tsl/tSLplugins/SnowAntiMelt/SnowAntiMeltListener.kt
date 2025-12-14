package org.tsl.tSLplugins.SnowAntiMelt

import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockFadeEvent
import org.bukkit.plugin.java.JavaPlugin

/**
 * 雪防融化监听器
 *
 * 功能：
 * - 阻止雪层（SNOW）和雪块（SNOW_BLOCK）因光照融化成空气
 * - 不影响冰块的融化行为
 * - 不影响玩家破坏雪方块的行为
 */
class SnowAntiMeltListener(private val plugin: JavaPlugin) : Listener {

    private var enabled = true

    /**
     * 加载配置
     */
    fun loadConfig() {
        val config = plugin.config.getConfigurationSection("snow-anti-melt")
        enabled = config?.getBoolean("enabled", true) ?: true

        if (enabled) {
            plugin.logger.info("[SnowAntiMelt] 雪防融化功能已启用")
        } else {
            plugin.logger.info("[SnowAntiMelt] 雪防融化功能未启用")
        }
    }

    /**
     * 检查模块是否启用
     */
    fun isEnabled(): Boolean = enabled

    /**
     * 监听方块消退事件（融化事件）
     *
     * 使用 HIGHEST 优先级确保在其他插件处理后执行
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onBlockFade(event: BlockFadeEvent) {
        if (!enabled) return

        val blockType = event.block.type
        val newType = event.newState.type

        // 如果是雪层或雪块，且要变成空气，则阻止
        if ((blockType == Material.SNOW || blockType == Material.SNOW_BLOCK) && newType == Material.AIR) {
            event.isCancelled = true
        }
    }
}

