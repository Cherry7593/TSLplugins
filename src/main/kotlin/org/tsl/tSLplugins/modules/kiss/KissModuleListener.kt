package org.tsl.tSLplugins.modules.kiss

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerQuitEvent

/**
 * Kiss 监听器（新架构版本）
 * 
 * 处理 Shift + 右键玩家的亲吻交互
 */
class KissModuleListener(
    private val module: KissModule
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
        if (!module.isEnabled()) return
        
        // 检查发起者是否启用了功能（静默）
        if (!module.isPlayerEnabled(player)) return
        
        // 冷却检查（静默）
        if (module.isInCooldown(player.uniqueId) && !player.hasPermission("tsl.kiss.bypass")) {
            return
        }
        
        // 不能亲自己
        if (player.uniqueId == entity.uniqueId) {
            player.sendMessage(serializer.deserialize(module.getModuleMessage("cannot_kiss_self")))
            return
        }
        
        // 执行亲吻
        module.executeKiss(player, entity)
        
        // 设置冷却
        if (!player.hasPermission("tsl.kiss.bypass")) {
            module.setCooldown(player.uniqueId)
        }
        
        // 取消默认交互
        event.isCancelled = true
    }
    
    /**
     * 玩家退出时清理数据
     */
    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        module.cleanupPlayer(event.player.uniqueId)
    }
}
