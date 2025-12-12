package org.tsl.tSLplugins.Ignore

import io.papermc.paper.event.player.AsyncChatEvent
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin

/**
 * 聊天屏蔽监听器
 *
 * 监听聊天事件，过滤掉屏蔽了发送者的接收者
 */
class IgnoreChatListener(
    private val plugin: JavaPlugin,
    private val manager: IgnoreManager
) : Listener {

    /**
     * 监听异步聊天事件
     *
     * 使用 HIGHEST 优先级确保在大多数插件处理后再过滤
     * 但在 MONITOR 之前，避免影响日志记录
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onAsyncChat(event: AsyncChatEvent) {
        if (!manager.isEnabled()) return

        val senderUuid = event.player.uniqueId

        // 从接收者列表中移除屏蔽了发送者的玩家
        event.viewers().removeIf { audience ->
            if (audience is Player) {
                val viewerUuid = audience.uniqueId
                // 检查这个接收者是否屏蔽了发送者
                manager.isIgnoring(viewerUuid, senderUuid)
            } else {
                false
            }
        }
    }
}

