package org.tsl.tSLplugins.Ride

import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.java.JavaPlugin

/**
 * Ride 监听器
 * 处理玩家右键骑乘生物的逻辑
 */
class RideListener(
    private val plugin: JavaPlugin,
    private val manager: RideManager
) : Listener {

    /**
     * 处理玩家右键实体事件
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onPlayerInteractEntity(event: PlayerInteractEntityEvent) {
        val player = event.player
        val entity = event.rightClicked

        // 快速检查：功能是否启用
        if (!manager.isEnabled()) return

        // 快速检查：玩家是否按住 Shift（避免与 Toss 功能冲突）
        if (player.isSneaking) return

        // 快速检查：是否为可骑乘的生物实体
        if (!entity.type.isAlive) return

        // 快速检查：玩家主手是否为空
        if (player.inventory.itemInMainHand.type != Material.AIR) return

        // 权限检查
        if (!player.hasPermission("tsl.ride.use")) return

        // 玩家开关状态检查
        if (!manager.isPlayerEnabled(player)) return

        // 黑名单检查
        if (manager.isEntityBlacklisted(entity.type) &&
            !player.hasPermission("tsl.ride.bypass")) {
            return
        }

        // 状态检查：实体已有乘客或玩家已在骑乘
        if (entity.passengers.isNotEmpty() || player.vehicle != null) return

        // 取消默认交互行为（必须在执行骑乘前取消）
        event.isCancelled = true

        // 执行骑乘操作
        entity.scheduler.run(plugin, { _ ->
            // 二次验证（防止并发问题）
            if (entity.isValid && player.isOnline &&
                entity.passengers.isEmpty() && player.vehicle == null) {
                entity.addPassenger(player)
            }
        }, null)
    }

    /**
     * 玩家退出时清理数据
     */
    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        manager.cleanupPlayer(event.player.uniqueId)
    }
}

