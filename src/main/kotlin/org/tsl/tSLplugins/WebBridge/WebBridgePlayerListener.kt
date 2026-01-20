package org.tsl.tSLplugins.WebBridge

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

/**
 * WebBridge 玩家事件监听器
 * 
 * 监听玩家进出事件，触发玩家列表推送
 * 使用 Folia 兼容的异步调度方式
 */
class WebBridgePlayerListener(
    private val manager: WebBridgeManager
) : Listener {

    /**
     * 玩家加入服务器
     * 延迟 1 秒后推送玩家列表（确保玩家完全加入）
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        if (!manager.isEnabled() || !manager.isConnected()) return

        val player = event.player
        
        // 使用 Folia 全局调度器延迟推送
        org.bukkit.Bukkit.getGlobalRegionScheduler().runDelayed(
            getPlugin(),
            { _ -> 
                manager.sendPlayerList()
                
                // 请求玩家称号
                val titleManager = manager.getTitleManager()
                if (titleManager?.isEnabled() == true) {
                    manager.requestPlayerTitle(player.uniqueId.toString())
                }
                
                // 请求玩家绑定状态
                manager.requestBindStatus(player.uniqueId.toString(), player.name)
            },
            manager.getTitleManager()?.getJoinDelay() ?: 20L
        )
    }

    /**
     * 玩家离开服务器
     * 立即推送玩家列表
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        if (!manager.isEnabled() || !manager.isConnected()) return

        // 使用 Folia 全局调度器立即推送
        org.bukkit.Bukkit.getGlobalRegionScheduler().run(
            getPlugin()
        ) { _ -> manager.sendPlayerList() }
    }

    /**
     * 获取插件实例
     */
    private fun getPlugin(): org.bukkit.plugin.Plugin {
        return org.bukkit.Bukkit.getPluginManager().getPlugin("TSLplugins")
            ?: throw IllegalStateException("TSLplugins not found")
    }
}
