package org.tsl.tSLplugins.Vault

import org.bukkit.Bukkit
import org.bukkit.block.Block
import org.bukkit.plugin.java.JavaPlugin
import java.lang.reflect.Field

/**
 * Vault 修复助手
 * 使用 NMS 反射 + Folia RegionScheduler
 */
object VaultNBTHelper {

    private var plugin: JavaPlugin? = null
    
    fun init(plugin: JavaPlugin) {
        this.plugin = plugin
    }

    data class CheckResult(
        val needsRepair: Boolean,
        val resumesAt: Long
    )

    /**
     * 检查宝库是否需要修复
     */
    fun checkVault(block: Block, abnormalThreshold: Long, debug: Boolean): CheckResult {
        try {
            val currentGameTime = block.world.gameTime
            
            val serverLevel = block.world.javaClass.getMethod("getHandle").invoke(block.world)
            val blockPosClass = Class.forName("net.minecraft.core.BlockPos")
            val blockPos = blockPosClass.getConstructor(Int::class.java, Int::class.java, Int::class.java)
                .newInstance(block.x, block.y, block.z)
            
            val tileEntity = serverLevel.javaClass.getMethod("getBlockEntity", blockPosClass)
                .invoke(serverLevel, blockPos) ?: return CheckResult(false, 0)
            
            if (!tileEntity.javaClass.name.contains("VaultBlockEntity")) {
                return CheckResult(false, 0)
            }
            
            val serverDataField = getField(tileEntity.javaClass, "serverData")
            val serverData = serverDataField.get(tileEntity)
            val timeField = getField(serverData.javaClass, "stateUpdatingResumesAt")
            val resumesAt = timeField.getLong(serverData)
            
            if (debug) {
                Bukkit.getLogger().info("[Vault/Debug] resumesAt=$resumesAt, gameTime=$currentGameTime")
            }
            
            val isAbnormal = resumesAt > currentGameTime + abnormalThreshold
            return CheckResult(isAbnormal, resumesAt)
            
        } catch (e: Exception) {
            if (debug) Bukkit.getLogger().warning("[Vault/Debug] 检查失败: ${e.message}")
            return CheckResult(false, 0)
        }
    }

    /**
     * 修复宝库（使用 Folia RegionScheduler）
     */
    fun repairVault(block: Block, debug: Boolean): Boolean {
        val p = plugin ?: return false
        
        return try {
            val server = Bukkit.getServer()
            val regionScheduler = try {
                server.javaClass.getMethod("getRegionScheduler").invoke(server)
            } catch (e: Exception) { null }
            
            if (regionScheduler != null) {
                // Folia: 调度到区域线程
                val executeMethod = regionScheduler.javaClass.getMethod(
                    "execute",
                    Class.forName("org.bukkit.plugin.Plugin"),
                    Class.forName("org.bukkit.Location"),
                    Runnable::class.java
                )
                executeMethod.invoke(regionScheduler, p, block.location, Runnable {
                    doRepair(block.world, block.x, block.y, block.z, debug)
                })
            } else {
                // 非 Folia: 直接执行
                doRepair(block.world, block.x, block.y, block.z, debug)
            }
            true
        } catch (e: Exception) {
            if (debug) Bukkit.getLogger().warning("[Vault/Debug] 调度失败: ${e.message}")
            false
        }
    }
    
    /**
     * 实际修复逻辑 - NMS 反射
     */
    private fun doRepair(world: org.bukkit.World, x: Int, y: Int, z: Int, debug: Boolean) {
        try {
            val serverLevel = world.javaClass.getMethod("getHandle").invoke(world)
            val blockPosClass = Class.forName("net.minecraft.core.BlockPos")
            val blockPos = blockPosClass.getConstructor(Int::class.java, Int::class.java, Int::class.java)
                .newInstance(x, y, z)
            
            val tileEntity = serverLevel.javaClass.getMethod("getBlockEntity", blockPosClass)
                .invoke(serverLevel, blockPos)
            
            if (tileEntity == null || !tileEntity.javaClass.name.contains("VaultBlockEntity")) {
                if (debug) Bukkit.getLogger().warning("[Vault/Debug] 不是 VaultBlockEntity: $x, $y, $z")
                return
            }
            
            // 修改 serverData.stateUpdatingResumesAt
            val serverDataField = getField(tileEntity.javaClass, "serverData")
            val serverData = serverDataField.get(tileEntity)
            val timeField = getField(serverData.javaClass, "stateUpdatingResumesAt")
            timeField.setLong(serverData, 0L)
            
            // 清空 serverData.rewardedPlayers（已领取奖励的玩家）
            try {
                val rewardedField = getField(serverData.javaClass, "rewardedPlayers")
                val rewarded = rewardedField.get(serverData)
                if (rewarded is MutableCollection<*>) {
                    rewarded.clear()
                    if (debug) Bukkit.getLogger().info("[Vault/Debug] 已清空 rewardedPlayers")
                } else if (rewarded is MutableMap<*, *>) {
                    rewarded.clear()
                    if (debug) Bukkit.getLogger().info("[Vault/Debug] 已清空 rewardedPlayers (Map)")
                }
            } catch (e: Exception) {
                if (debug) Bukkit.getLogger().info("[Vault/Debug] 清空 rewardedPlayers 失败: ${e.message}")
            }
            
            // 清空 sharedData.connectedPlayers（当前连接的玩家）
            try {
                val sharedDataField = getField(tileEntity.javaClass, "sharedData")
                val sharedData = sharedDataField.get(tileEntity)
                val playersField = getField(sharedData.javaClass, "connectedPlayers")
                val players = playersField.get(sharedData)
                if (players is MutableCollection<*>) players.clear()
            } catch (_: Exception) {}
            
            // 持久化
            tileEntity.javaClass.getMethod("setChanged").invoke(tileEntity)
            
            if (debug) {
                Bukkit.getLogger().info("[Vault/Debug] 修复成功: $x, $y, $z")
            }
        } catch (e: Exception) {
            if (debug) Bukkit.getLogger().warning("[Vault/Debug] 修复失败: ${e.message}")
        }
    }
    
    private fun getField(clazz: Class<*>, fieldName: String): Field {
        var currentClass: Class<*>? = clazz
        while (currentClass != null) {
            try {
                val field = currentClass.getDeclaredField(fieldName)
                field.isAccessible = true
                return field
            } catch (e: NoSuchFieldException) {
                currentClass = currentClass.superclass
            }
        }
        throw NoSuchFieldException("Field $fieldName not found in $clazz")
    }
}
