package org.tsl.tSLplugins.Peace

import org.bukkit.Bukkit
import org.bukkit.entity.Monster
import org.bukkit.entity.Player
import org.bukkit.entity.Slime
import org.bukkit.entity.MagmaCube
import org.bukkit.entity.Phantom
import org.bukkit.entity.Ghast
import org.bukkit.entity.Shulker
import org.bukkit.entity.Hoglin
import org.bukkit.entity.Piglin
import org.bukkit.entity.PiglinBrute
import org.bukkit.entity.Zoglin
import org.bukkit.entity.Warden
import org.bukkit.entity.Breeze
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
        
        // 只处理自然生成和史莱姆分裂
        val reason = event.spawnReason
        if (reason != CreatureSpawnEvent.SpawnReason.NATURAL && 
            reason != CreatureSpawnEvent.SpawnReason.SLIME_SPLIT) return
        
        // 检查是否是敌对生物
        // Monster 包括: Zombie, Skeleton, Creeper, Spider, Enderman, Witch, 
        //                Stray, Husk, Drowned, Bogged, 等所有普通敌对怪物
        val entity = event.entity
        val isHostile = entity is Monster ||    // 包含所有骷髅变种(Stray/Bogged/Wither Skeleton)
                        entity is Slime ||      // 史莱姆
                        entity is MagmaCube ||  // 岩浆怪
                        entity is Phantom ||    // 幻翼
                        entity is Ghast ||      // 恶魂
                        entity is Shulker ||    // 潜影贝
                        entity is Hoglin ||     // 疣猕兽
                        entity is Piglin ||     // 猪灵
                        entity is PiglinBrute ||
                        entity is Zoglin ||     // 僵尸疣猕兽
                        entity is Warden ||     // 监守者
                        entity is Breeze        // 旋风人
        
        if (!isHostile) return
        
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
