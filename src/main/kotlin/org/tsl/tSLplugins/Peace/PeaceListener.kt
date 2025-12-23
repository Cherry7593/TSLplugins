package org.tsl.tSLplugins.Peace

import org.bukkit.Bukkit
import org.bukkit.entity.Monster
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.event.entity.EntityTargetEvent
import org.bukkit.event.entity.EntityTargetLivingEntityEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.java.JavaPlugin

/**
 * 伪和平模式事件监听器
 * 模式1: 阻止怪物锁定处于和平模式的玩家
 * 模式2: 阻止敌对生物在禁怪玩家附近自然生成
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
     * 监听生物生成事件
     * 阻止敌对生物在禁怪玩家附近自然生成
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onCreatureSpawn(event: CreatureSpawnEvent) {
        if (!manager.isEnabled()) return
        
        // 只处理自然生成的敌对生物
        if (event.spawnReason != CreatureSpawnEvent.SpawnReason.NATURAL) return
        if (event.entity !is Monster) return
        
        val spawnLocation = event.location
        val radius = manager.getNoSpawnRadius()
        val radiusSquared = radius * radius
        
        // 获取所有禁怪模式玩家
        val noSpawnPlayerUuids = manager.getNoSpawnPlayerUuids()
        if (noSpawnPlayerUuids.isEmpty()) return
        
        // 检查是否有禁怪玩家在范围内
        for (uuid in noSpawnPlayerUuids) {
            val player = Bukkit.getPlayer(uuid) ?: continue
            if (!player.isOnline) continue
            
            // 必须在同一世界
            if (player.world != spawnLocation.world) continue
            
            // 检查距离（使用平方距离提高性能）
            val distanceSquared = player.location.distanceSquared(spawnLocation)
            if (distanceSquared <= radiusSquared) {
                event.isCancelled = true
                return
            }
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
