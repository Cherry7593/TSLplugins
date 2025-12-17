package org.tsl.tSLplugins.TimedAttribute

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.java.JavaPlugin

/**
 * 计时属性效果监听器
 * 处理玩家上下线时的效果加载和清理
 */
class TimedAttributeListener(
    private val plugin: JavaPlugin,
    private val manager: TimedAttributeManager
) : Listener {

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        if (!manager.isEnabled()) return

        // 延迟一点加载，确保玩家完全加入
        event.player.scheduler.runDelayed(plugin, { _ ->
            manager.onPlayerJoin(event.player)
        }, null, 10L)
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        if (!manager.isEnabled()) return

        manager.onPlayerQuit(event.player)
    }
}

