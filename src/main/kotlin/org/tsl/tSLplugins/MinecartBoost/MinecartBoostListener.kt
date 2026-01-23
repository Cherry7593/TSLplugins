package org.tsl.tSLplugins.MinecartBoost

import org.bukkit.Material
import org.bukkit.entity.Minecart
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.vehicle.VehicleExitEvent
import org.bukkit.event.vehicle.VehicleMoveEvent
import org.bukkit.plugin.java.JavaPlugin

/**
 * 矿车加速监听器
 * 通过设置 minecart.maxSpeed 来实现加速
 */
class MinecartBoostListener(
    private val plugin: JavaPlugin,
    private val manager: MinecartBoostManager
) : Listener {

    companion object {
        private const val VANILLA_MAX_SPEED = 0.4
        private val RAIL_TYPES = listOf(
            Material.RAIL, Material.POWERED_RAIL,
            Material.DETECTOR_RAIL, Material.ACTIVATOR_RAIL
        )
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onVehicleMove(event: VehicleMoveEvent) {
        if (!manager.isEnabled()) return

        val vehicle = event.vehicle

        // 只处理矿车
        if (vehicle !is Minecart) return

        // 只处理载人的矿车
        if (vehicle.isEmpty) return
        if (vehicle.passengers.firstOrNull() !is Player) return

        // 获取矿车所在位置的方块
        val railBlock = vehicle.location.block

        // 检查是否在铁轨上
        if (railBlock.type !in RAIL_TYPES) return

        // 获取铁轨下方的方块
        val blockBelow = railBlock.getRelative(0, -1, 0)

        // 获取对应的 maxSpeed 值，如果不在配置中则使用原版速度
        val maxSpeed = manager.getMaxSpeedForBlock(blockBelow.type) ?: VANILLA_MAX_SPEED

        // 设置矿车最大速度
        vehicle.maxSpeed = maxSpeed
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onVehicleExit(event: VehicleExitEvent) {
        if (!manager.isEnabled()) return

        val vehicle = event.vehicle

        // 只处理矿车
        if (vehicle !is Minecart) return

        // 只处理玩家下车
        if (event.exited !is Player) return

        // 重置矿车速度为原版
        if (vehicle.maxSpeed > VANILLA_MAX_SPEED) {
            vehicle.maxSpeed = VANILLA_MAX_SPEED
        }
    }
}
