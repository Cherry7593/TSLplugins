package org.tsl.tSLplugins.Motd

import com.destroystokyo.paper.event.server.PaperServerListPingEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin

class FakePlayerMotd(private val plugin: JavaPlugin) : Listener {

    @EventHandler
    fun onServerListPing(event: PaperServerListPingEvent) {
        // 检查假玩家功能是否启用
        if (!plugin.config.getBoolean("fakeplayer.enabled", false)) {
            return
        }

        val base = event.numPlayers
        val delta = plugin.config.getInt("fakeplayer.count", 0)
        val shown = maxOf(0, base + delta)
        event.numPlayers = shown
    }
}

