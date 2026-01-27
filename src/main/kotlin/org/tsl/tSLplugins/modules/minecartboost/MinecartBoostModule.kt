package org.tsl.tSLplugins.modules.minecartboost

import org.bukkit.Material
import org.bukkit.entity.Minecart
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.vehicle.VehicleExitEvent
import org.bukkit.event.vehicle.VehicleMoveEvent
import org.tsl.tSLplugins.core.AbstractModule

/**
 * MinecartBoost 模块 - 矿车加速
 * 通过设置 minecart.maxSpeed 来实现加速
 */
class MinecartBoostModule : AbstractModule() {

    override val id = "minecart-boost"
    override val configPath = "minecart-boost"

    private val blockMaxSpeedMap: MutableMap<Material, Double> = mutableMapOf()
    private lateinit var listener: MinecartBoostModuleListener

    companion object {
        private const val VANILLA_MAX_SPEED = 0.4
        private val RAIL_TYPES = listOf(Material.RAIL, Material.POWERED_RAIL, Material.DETECTOR_RAIL, Material.ACTIVATOR_RAIL)
    }

    override fun doEnable() {
        loadBoostConfig()
        listener = MinecartBoostModuleListener(this)
        registerListener(listener)
    }

    override fun doReload() {
        loadBoostConfig()
    }

    override fun getDescription(): String = "矿车加速"

    private fun loadBoostConfig() {
        blockMaxSpeedMap.clear()
        context.plugin.config.getConfigurationSection("minecart-boost.blocks")?.getKeys(false)?.forEach { key ->
            val materialName = key.uppercase()
            val maxSpeed = context.plugin.config.getDouble("minecart-boost.blocks.$key")
            try {
                val material = Material.valueOf(materialName)
                blockMaxSpeedMap[material] = maxSpeed
            } catch (e: IllegalArgumentException) {
                logWarning("无效的方块类型: $key")
            }
        }
        logInfo("配置已加载 - 方块映射: ${blockMaxSpeedMap.size} 种")
    }

    fun getMaxSpeedForBlock(material: Material): Double? = blockMaxSpeedMap[material]
    fun getVanillaMaxSpeed(): Double = VANILLA_MAX_SPEED
    fun getRailTypes(): List<Material> = RAIL_TYPES
}

class MinecartBoostModuleListener(private val module: MinecartBoostModule) : Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onVehicleMove(event: VehicleMoveEvent) {
        if (!module.isEnabled()) return
        val vehicle = event.vehicle
        if (vehicle !is Minecart) return
        if (vehicle.isEmpty) return
        if (vehicle.passengers.firstOrNull() !is Player) return

        val railBlock = vehicle.location.block
        if (railBlock.type !in module.getRailTypes()) return

        val blockBelow = railBlock.getRelative(0, -1, 0)
        val maxSpeed = module.getMaxSpeedForBlock(blockBelow.type) ?: module.getVanillaMaxSpeed()
        vehicle.maxSpeed = maxSpeed
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onVehicleExit(event: VehicleExitEvent) {
        if (!module.isEnabled()) return
        val vehicle = event.vehicle
        if (vehicle !is Minecart) return
        if (event.exited !is Player) return
        if (vehicle.maxSpeed > module.getVanillaMaxSpeed()) {
            vehicle.maxSpeed = module.getVanillaMaxSpeed()
        }
    }
}
