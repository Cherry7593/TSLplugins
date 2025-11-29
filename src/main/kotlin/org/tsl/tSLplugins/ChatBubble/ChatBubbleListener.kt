package org.tsl.tSLplugins.ChatBubble

import io.papermc.paper.event.player.AsyncChatEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.plugin.java.JavaPlugin

/**
 * ChatBubble 事件监听器
 * 监听聊天、世界切换、玩家退出等事件
 */
class ChatBubbleListener(
    private val plugin: JavaPlugin,
    private val manager: ChatBubbleManager
) : Listener {

    /**
     * 监听玩家聊天事件
     * 优先级 HIGHEST，忽略已取消的事件
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onAsyncChat(event: AsyncChatEvent) {
        if (!manager.isEnabled()) return

        val player = event.player
        val message = event.message()

        // 使用玩家调度器在主线程创建气泡（Folia 兼容）
        player.scheduler.run(plugin, { _ ->
            manager.createOrUpdateBubble(player, message)
        }, null)
    }

    /**
     * 监听玩家传送事件
     * 传送时直接清除气泡，避免跨区域线程安全问题
     */
    @EventHandler
    fun onPlayerTeleport(event: PlayerTeleportEvent) {
        if (!manager.isEnabled()) return

        val player = event.player

        // 传送时清除当前气泡（Folia 线程安全的最佳实践）
        manager.cleanupPlayer(player)
    }

    /**
     * 监听玩家切换世界
     * 确保气泡在新世界中的可见性正确
     */
    @EventHandler
    fun onWorldChange(event: PlayerChangedWorldEvent) {
        if (!manager.isEnabled()) return

        val player = event.player

        // 如果玩家未启用自我显示，需要重新隐藏所有气泡
        if (!manager.getSelfDisplayEnabled(player)) {
            player.scheduler.execute(plugin, {
                // 获取玩家的所有气泡并隐藏
                val bubbles = manager.getBubbles(player)
                bubbles.forEach { bubble ->
                    player.hideEntity(plugin, bubble)
                }
            }, null, 2L) // 延迟 2 tick 确保世界切换完成
        }
    }

    /**
     * 监听玩家退出
     * 清理气泡实体和数据
     */
    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        manager.cleanupPlayer(event.player)
    }
}

