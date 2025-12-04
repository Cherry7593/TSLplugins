package org.tsl.tSLplugins.WebBridge

import io.papermc.paper.event.player.AsyncChatEvent
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin
import java.util.logging.Level

/**
 * WebBridge 聊天监听器
 *
 * 监听玩家聊天事件，将消息发送到 Web 后端
 */
class WebBridgeChatListener(
    private val plugin: Plugin,
    private val manager: WebBridgeManager
) : Listener {

    private val json = Json {
        prettyPrint = false
        encodeDefaults = true
    }

    /**
     * 监听玩家异步聊天事件
     *
     * 使用 MONITOR 优先级，确保在所有其他插件处理后再发送
     * 使用 ignoreCancelled = true，只发送未被取消的消息
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onAsyncChat(event: AsyncChatEvent) {
        try {
            val player = event.player

            // 获取消息内容（转换为纯文本）
            val messageComponent = event.message()
            val message = PlainTextComponentSerializer.plainText().serialize(messageComponent)

            // 构造聊天消息载荷
            val chatPayload = ChatPayload(
                playerName = player.name,
                playerUuid = player.uniqueId.toString(),
                serverName = getServerName(),
                message = message,
                channel = "global"
            )

            // 构造完整消息
            val bridgeMessage = BridgeMessage(
                type = "chat",
                payload = chatPayload
            )

            // 序列化为 JSON
            val jsonString = json.encodeToString(bridgeMessage)

            // 加入发送队列
            manager.sendMessage(jsonString)

        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, "[WebBridge] 处理聊天消息时出错", e)
        }
    }

    /**
     * 获取服务器名称
     */
    private fun getServerName(): String {
        // 尝试从系统属性或配置中获取服务器名称
        // 如果未配置，使用默认值
        return System.getProperty("server.name")
            ?: Bukkit.getMotd()
            ?: "Minecraft Server"
    }
}

