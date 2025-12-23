package org.tsl.tSLplugins.PlayTime

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

/**
 * 在线时长事件监听器
 * 监听玩家加入和退出事件，通知 Manager 进行时长统计
 */
class PlayTimeListener(
    private val manager: PlayTimeManager
) : Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        if (!manager.isEnabled()) return
        manager.onPlayerJoin(event.player)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        if (!manager.isEnabled()) return
        manager.onPlayerQuit(event.player)
    }
}
