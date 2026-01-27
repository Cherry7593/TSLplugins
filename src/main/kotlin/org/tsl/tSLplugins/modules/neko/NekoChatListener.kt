package org.tsl.tSLplugins.modules.neko

import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin

class NekoChatListener(
    private val plugin: JavaPlugin,
    private val manager: NekoManager
) : Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onAsyncChat(event: AsyncChatEvent) {
        if (!manager.isEnabled()) return
        val player = event.player
        if (!manager.isNeko(player)) return
        val suffix = manager.getSuffix()
        if (suffix.isEmpty()) return
        val originalMessage = event.message()
        val plainText = PlainTextComponentSerializer.plainText().serialize(originalMessage)
        if (plainText.endsWith(suffix)) return
        val newMessage = originalMessage.append(Component.text(suffix))
        event.message(newMessage)
    }
}
