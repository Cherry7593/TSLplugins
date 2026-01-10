package org.tsl.tSLplugins.Vault

import org.bukkit.Material
import org.bukkit.block.TileState
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin

/**
 * Vault 方块交互监听器
 * 双重检测：epoch 周期 + resumesAt 异常值
 */
class VaultListener(
    private val plugin: JavaPlugin,
    private val manager: VaultManager
) : Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (!manager.isEnabled()) return
        if (!event.action.name.contains("RIGHT_CLICK")) return
        
        val block = event.clickedBlock ?: return
        if (block.type != Material.VAULT) return
        
        val blockKey = "${block.world.name}:${block.x}:${block.y}:${block.z}"
        
        // 防止高频点击
        if (!manager.tryAcquireLock(blockKey)) return
        
        try {
            manager.checkAndUpdateEpoch()
            
            val blockState = block.state as? TileState ?: return
            val pdc = blockState.persistentDataContainer
            val currentEpoch = manager.getCurrentEpoch()
            val blockEpoch = pdc.get(manager.epochKey, PersistentDataType.INTEGER) ?: 0
            
            // 检查是否需要修复（反射读取 resumesAt）
            val checkResult = VaultNBTHelper.checkVault(
                block, 
                manager.getAbnormalThreshold(), 
                manager.isDebugMode()
            )
            
            // 双重触发：epoch 过期 OR resumesAt 异常
            val needsEpochReset = blockEpoch < currentEpoch
            val needsAbnormalRepair = checkResult.needsRepair
            
            if (needsEpochReset || needsAbnormalRepair) {
                manager.debug("修复宝库 $blockKey (epoch=${needsEpochReset}, abnormal=${needsAbnormalRepair})")
                
                if (VaultNBTHelper.repairVault(block, manager.isDebugMode())) {
                    pdc.set(manager.epochKey, PersistentDataType.INTEGER, currentEpoch)
                    blockState.update(true, false)
                    
                    if (needsAbnormalRepair) {
                        plugin.logger.info("[Vault] 已修复宝库 @ $blockKey (resumesAt: ${checkResult.resumesAt} -> 0)")
                    }
                    if (needsEpochReset) {
                        plugin.logger.info("[Vault] 已重置周期 @ $blockKey (epoch: $blockEpoch -> $currentEpoch)")
                    }
                }
            }
        } finally {
            manager.releaseLock(blockKey)
        }
    }
}
