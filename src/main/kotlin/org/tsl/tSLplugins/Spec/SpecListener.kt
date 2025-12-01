package org.tsl.tSLplugins.Spec

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.java.JavaPlugin

/**
 * Spec 事件监听器
 * 处理玩家退出等事件
 */
class SpecListener(
    private val plugin: JavaPlugin,
    private val manager: SpecManager
) : Listener {

    /**
     * 玩家退出事件
     * 清理观看状态
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        manager.onPlayerQuit(event.player)
    }
}

