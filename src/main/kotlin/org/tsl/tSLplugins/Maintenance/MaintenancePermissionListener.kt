package org.tsl.tSLplugins.Maintenance

import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.plugin.java.JavaPlugin
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer

/**
 * 维护模式权限变更监听器
 * 监听玩家权限变更事件，当玩家失去管理权限时踢出
 */
class MaintenancePermissionListener(
    private val plugin: JavaPlugin,
    private val manager: MaintenanceManager
) : Listener {

    private val serializer = LegacyComponentSerializer.legacyAmpersand()

    /**
     * 检查在线玩家的权限
     * 在权限可能发生变更时调用
     */
    fun checkOnlinePlayers() {
        if (!manager.isMaintenanceEnabled()) {
            return
        }

        Bukkit.getScheduler().runTask(plugin, Runnable {
            Bukkit.getOnlinePlayers()
                .filter { !shouldAllowPlayer(it.uniqueId, it.hasPermission("tsl.maintenance.bypass")) }
                .forEach { player ->
                    // 获取踢出消息
                    val kickMessages = manager.getConfig().getStringList("maintenance.kick-message")
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

                    player.kick(kickComponent)
                }
        })
    }

    /**
     * 监听玩家切换世界事件（可能触发权限变更）
     */
    @EventHandler
    fun onWorldChange(event: PlayerChangedWorldEvent) {
        if (!manager.isMaintenanceEnabled()) {
            return
        }

        val player = event.player
        if (!shouldAllowPlayer(player.uniqueId, player.hasPermission("tsl.maintenance.bypass"))) {
            Bukkit.getScheduler().runTask(plugin, Runnable {
                val kickMessages = manager.getConfig().getStringList("maintenance.kick-message")
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

                player.kick(kickComponent)
            })
        }
    }

    /**
     * 判断是否应该允许玩家留在服务器
     */
    private fun shouldAllowPlayer(uuid: java.util.UUID, hasBypassPermission: Boolean): Boolean {
        return manager.isWhitelisted(uuid) || hasBypassPermission
    }
}

