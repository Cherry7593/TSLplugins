package org.tsl.tSLplugins.Kiss

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.java.JavaPlugin

/**
 * Kiss 监听器
 * 处理 Shift + 右键玩家的亲吻交互
 */
class KissListener(
    private val plugin: JavaPlugin,
    private val manager: KissManager,
    private val executor: KissExecutor
) : Listener {

    private val serializer = LegacyComponentSerializer.legacyAmpersand()

    /**
     * 处理玩家右键实体事件
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onPlayerInteractEntity(event: PlayerInteractEntityEvent) {
        val player = event.player
        val entity = event.rightClicked

        // 快速检查：必须按住 Shift
        if (!player.isSneaking) return

        // 快速检查：目标必须是玩家
        if (entity !is Player) return

        // 快速检查：功能是否启用
        if (!manager.isEnabled()) return

        // 检查发起者是否启用了功能（静默检查，不提示）
        if (!manager.isPlayerEnabled(player)) {
            return
        }

        // 冷却检查（静默检查，不提示）
        if (manager.isInCooldown(player.uniqueId) && !player.hasPermission("tsl.kiss.bypass")) {
            return
        }

        // 不能亲自己
        if (player.uniqueId == entity.uniqueId) {
            sendMessage(player, "cannot_kiss_self")
            return
        }

        // 执行亲吻
        executor.executeKiss(player, entity)

        // 设置冷却
        if (!player.hasPermission("tsl.kiss.bypass")) {
            manager.setCooldown(player.uniqueId)
        }

        // 取消默认交互
        event.isCancelled = true
    }

    /**
     * 玩家退出时清理数据
     */
    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        manager.cleanupPlayer(event.player.uniqueId)
    }

    /**
     * 发送消息的辅助方法
     */
    private fun sendMessage(player: Player, messageKey: String, vararg replacements: Pair<String, String>) {
        val message = manager.getMessage(messageKey, *replacements)
        player.sendMessage(serializer.deserialize(message))
    }
}

