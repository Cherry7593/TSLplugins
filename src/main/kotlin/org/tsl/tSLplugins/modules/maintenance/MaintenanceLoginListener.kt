package org.tsl.tSLplugins.modules.maintenance

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer

/**
 * 维护模式登录监听器
 * 在登录前就拒绝玩家，避免区块加载和实体注册
 */
class MaintenanceLoginListener(private val manager: MaintenanceManager) : Listener {

    private val serializer = LegacyComponentSerializer.legacyAmpersand()

    @EventHandler(priority = EventPriority.LOWEST)
    fun onPreLogin(event: AsyncPlayerPreLoginEvent) {
        // 如果功能被禁用，直接返回
        if (!manager.isFeatureEnabled()) {
            return
        }

        // 如果维护模式未启用，允许所有玩家登录
        if (!manager.isMaintenanceEnabled()) {
            return
        }

        // 检查玩家是否在白名单中
        if (manager.isWhitelisted(event.uniqueId)) {
            return
        }

        // 获取踢出消息
        val kickMessages = manager.getConfig().getStringList("maintenance.kick-message")

        // 使用 Component 进行消息发送
        val kickComponent = if (kickMessages.isNotEmpty()) {
            val builder = Component.text()
            kickMessages.forEachIndexed { index, line ->
                if (index > 0) builder.append(Component.newline())
                builder.append(serializer.deserialize(line))
            }
            builder.build()
        } else {
            Component.text("⚠ 服务器维护中 ⚠")
        }

        // 使用 disallow 拒绝登录，避免玩家进入世界
        event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, kickComponent)
    }
}
