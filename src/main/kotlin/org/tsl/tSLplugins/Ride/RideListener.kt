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
    @EventHandler(priority = EventPriority.NORMAL)
    fun onPlayerInteractEntity(event: PlayerInteractEntityEvent) {
        val player = event.player
        val entity = event.rightClicked

        // 检查功能是否启用
        if (!manager.isEnabled()) {
            return
        }

        // 检查基本权限
        if (!player.hasPermission("tsl.ride.use")) {
            return
        }

        // 检查玩家是否启用了骑乘功能
        if (!manager.isPlayerEnabled(player.uniqueId)) {
            return
        }

        // 检查玩家是否空手（主手必须为空）
        val mainHandItem = player.inventory.itemInMainHand
        if (mainHandItem.type != Material.AIR) {
            return
        }

        // 检查是否为生物实体（排除物品框等）
        if (!entity.type.isAlive) {
            return
        }

        // 检查黑名单
        if (manager.isEntityBlacklisted(entity.type)) {
            // 检查是否有绕过黑名单的权限
            if (!player.hasPermission("tsl.ride.bypass")) {
                // 静默返回，不显示任何提示消息
                return
            }
        }

        // 检查实体是否已经有乘客
        if (entity.passengers.isNotEmpty()) {
            return
        }

        // 检查玩家是否已经在骑乘其他实体
        if (player.vehicle != null) {
            return
        }

        // 使用 Folia 的实体调度器执行骑乘操作
        entity.scheduler.run(plugin, { _ ->
            try {
                entity.addPassenger(player)
            } catch (_: Exception) {
                // 静默处理异常
            }
        }, null)

        // 取消默认的交互行为（防止打开GUI等）
        event.isCancelled = true
    }

    /**
     * 玩家退出时清理数据
     */
    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        manager.cleanupPlayer(event.player.uniqueId)
    }
}

