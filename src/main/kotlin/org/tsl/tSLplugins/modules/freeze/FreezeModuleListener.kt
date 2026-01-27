package org.tsl.tSLplugins.modules.freeze

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.*
import org.bukkit.plugin.java.JavaPlugin

/**
 * Freeze 监听器（新架构版本）
 * 
 * 阻止被冻结的玩家执行各种操作
 */
class FreezeModuleListener(
    private val plugin: JavaPlugin,
    private val module: FreezeModule
) : Listener {
    
    private val serializer = LegacyComponentSerializer.legacyAmpersand()
    
    /**
     * 阻止玩家移动
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onPlayerMove(event: PlayerMoveEvent) {
        val player = event.player
        
        if (!module.isFrozen(player.uniqueId)) return
        if (player.hasPermission("tsl.freeze.bypass")) return
        
        val from = event.from
        val to = event.to
        
        // 只阻止位置移动，允许视角转动
        if (from.x != to.x || from.y != to.y || from.z != to.z) {
            event.isCancelled = true
        }
    }
    
    /**
     * 阻止破坏方块
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onBlockBreak(event: BlockBreakEvent) {
        val player = event.player
        
        if (!module.isFrozen(player.uniqueId)) return
        if (player.hasPermission("tsl.freeze.bypass")) return
        
        event.isCancelled = true
    }
    
    /**
     * 阻止放置方块
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onBlockPlace(event: BlockPlaceEvent) {
        val player = event.player
        
        if (!module.isFrozen(player.uniqueId)) return
        if (player.hasPermission("tsl.freeze.bypass")) return
        
        event.isCancelled = true
    }
    
    /**
     * 阻止交互
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val player = event.player
        
        if (!module.isFrozen(player.uniqueId)) return
        if (player.hasPermission("tsl.freeze.bypass")) return
        
        event.isCancelled = true
    }
    
    /**
     * 阻止与实体交互
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onPlayerInteractEntity(event: PlayerInteractEntityEvent) {
        val player = event.player
        
        if (!module.isFrozen(player.uniqueId)) return
        if (player.hasPermission("tsl.freeze.bypass")) return
        
        event.isCancelled = true
    }
    
    /**
     * 阻止使用指令
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerCommandPreprocess(event: PlayerCommandPreprocessEvent) {
        val player = event.player
        
        if (!module.isFrozen(player.uniqueId)) return
        if (player.hasPermission("tsl.freeze.bypass")) return
        
        event.isCancelled = true
        player.sendMessage(serializer.deserialize(module.getModuleMessage("cannot_use_commands")))
    }
    
    /**
     * 阻止丢弃物品
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onPlayerDropItem(event: PlayerDropItemEvent) {
        val player = event.player
        
        if (!module.isFrozen(player.uniqueId)) return
        if (player.hasPermission("tsl.freeze.bypass")) return
        
        event.isCancelled = true
    }
    
    /**
     * 阻止捡起物品
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onPlayerPickupItem(event: PlayerAttemptPickupItemEvent) {
        val player = event.player
        
        if (!module.isFrozen(player.uniqueId)) return
        if (player.hasPermission("tsl.freeze.bypass")) return
        
        event.isCancelled = true
    }
    
    /**
     * 阻止切换物品
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onPlayerItemHeld(event: PlayerItemHeldEvent) {
        val player = event.player
        
        if (!module.isFrozen(player.uniqueId)) return
        if (player.hasPermission("tsl.freeze.bypass")) return
        
        event.isCancelled = true
    }
    
    /**
     * 玩家加入时启动 ActionBar 任务
     */
    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        startActionBarTask(event.player)
    }
    
    /**
     * 启动 ActionBar 提示任务
     */
    private fun startActionBarTask(player: Player) {
        player.scheduler.runAtFixedRate(plugin, { task ->
            // 检查玩家是否仍在线
            if (!player.isOnline) {
                task.cancel()
                return@runAtFixedRate
            }
            
            // 检查模块是否启用
            if (!module.isEnabled()) {
                return@runAtFixedRate
            }
            
            // 检查是否被冻结
            if (!module.isFrozen(player.uniqueId)) {
                return@runAtFixedRate
            }
            
            // 检查是否有 bypass 权限
            if (player.hasPermission("tsl.freeze.bypass")) {
                return@runAtFixedRate
            }
            
            // 显示 ActionBar
            val remaining = module.getRemainingTime(player.uniqueId)
            val timeText = if (remaining < 0) "永久冻结" else "剩余: ${formatTime(remaining)}"
            
            val actionBarText = module.getModuleMessage("actionbar", "time" to timeText)
            player.sendActionBar(serializer.deserialize(actionBarText))
        }, null, 1L, 20L)
    }
    
    private fun formatTime(seconds: Int): String {
        return when {
            seconds < 60 -> "${seconds}秒"
            seconds < 3600 -> "${seconds / 60}分${seconds % 60}秒"
            else -> "${seconds / 3600}小时${(seconds % 3600) / 60}分"
        }
    }
}
