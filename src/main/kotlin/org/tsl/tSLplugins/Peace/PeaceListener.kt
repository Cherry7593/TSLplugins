package org.tsl.tSLplugins.Peace

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityTargetEvent
import org.bukkit.event.entity.EntityTargetLivingEntityEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.java.JavaPlugin

/**
 * 伪和平模式事件监听器
 * 阻止怪物锁定处于和平模式的玩家
 */
class PeaceListener(
    private val plugin: JavaPlugin,
    private val manager: PeaceManager
) : Listener {

    /**
     * 监听实体锁定目标事件
     * 阻止怪物锁定处于和平模式的玩家
     */
    @EventHandler(priority = EventPriority.HIGH)
    fun onEntityTarget(event: EntityTargetEvent) {
        if (!manager.isEnabled()) return

        val target = event.target as? Player ?: return

        // 检查玩家是否处于和平模式
        if (manager.isPeaceful(target.uniqueId)) {
            event.isCancelled = true
        }
    }

    /**
     * 监听实体锁定生物事件（更具体的事件）
     */
    @EventHandler(priority = EventPriority.HIGH)
    fun onEntityTargetLivingEntity(event: EntityTargetLivingEntityEvent) {
        if (!manager.isEnabled()) return

        val target = event.target as? Player ?: return

        // 检查玩家是否处于和平模式
        if (manager.isPeaceful(target.uniqueId)) {
            event.isCancelled = true
        }
    }

    /**
     * 玩家加入时加载和平状态
     */
    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        manager.onPlayerJoin(event.player)
    }

    /**
     * 玩家退出时处理
     */
    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        manager.onPlayerQuit(event.player)
    }
}
