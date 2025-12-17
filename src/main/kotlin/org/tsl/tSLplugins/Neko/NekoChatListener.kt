package org.tsl.tSLplugins.Neko

import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin

/**
 * 猫娘聊天监听器
 * 监听玩家聊天消息，如果玩家是猫娘则在消息末尾添加后缀
 */
class NekoChatListener(
    private val plugin: JavaPlugin,
    private val manager: NekoManager
) : Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onAsyncChat(event: AsyncChatEvent) {
        // 检查功能是否启用
        if (!manager.isEnabled()) return

        val player = event.player

        // 检查玩家是否是猫娘
        if (!manager.isNeko(player)) return

        // 获取后缀
        val suffix = manager.getSuffix()
        if (suffix.isEmpty()) return

        // 获取原始消息
        val originalMessage = event.message()

        // 将原始消息转换为纯文本，检查是否已经包含后缀（避免重复添加）
        val plainText = PlainTextComponentSerializer.plainText().serialize(originalMessage)
        if (plainText.endsWith(suffix)) return

        // 在消息末尾添加后缀
        val newMessage = originalMessage.append(Component.text(suffix))

        // 设置新消息
        event.message(newMessage)
    }
}

