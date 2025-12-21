package org.tsl.tSLplugins.RedstoneFreeze

import org.bukkit.entity.EntityType
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockPhysicsEvent
import org.bukkit.event.block.BlockPistonExtendEvent
import org.bukkit.event.block.BlockPistonRetractEvent
import org.bukkit.event.block.BlockRedstoneEvent
import org.bukkit.event.block.TNTPrimeEvent
import org.bukkit.event.entity.EntitySpawnEvent
import org.bukkit.event.entity.ExplosionPrimeEvent

/**
 * 红石冻结监听器
 * 使用三级过滤机制高效拦截红石、活塞、物理事件
 */
class RedstoneFreezeListener(
    private val manager: RedstoneFreezeManager
) : Listener {

    /**
     * 监听红石信号变化事件
     * 优先级设为 LOWEST 以最早拦截
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onBlockRedstone(event: BlockRedstoneEvent) {
        // 快速过滤：冻结未激活时直接返回（性能优化）
        if (!manager.isFreezeActive()) return

        // 检查 Chunk 是否在冻结名单
        val chunkKey = event.block.chunk.chunkKey
        if (!manager.isChunkFrozen(chunkKey)) return

        // 检查元件开关
        if (!manager.isRedstoneSignalAffected()) return

        // 阻止红石信号变化
        event.newCurrent = event.oldCurrent
    }

    /**
     * 监听活塞伸出事件
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onPistonExtend(event: BlockPistonExtendEvent) {
        if (!manager.isFreezeActive()) return

        val chunkKey = event.block.chunk.chunkKey
        if (!manager.isChunkFrozen(chunkKey)) return

        if (!manager.isPistonExtendAffected()) return

        event.isCancelled = true
    }

    /**
     * 监听活塞收回事件
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onPistonRetract(event: BlockPistonRetractEvent) {
        if (!manager.isFreezeActive()) return

        val chunkKey = event.block.chunk.chunkKey
        if (!manager.isChunkFrozen(chunkKey)) return

        if (!manager.isPistonRetractAffected()) return

        event.isCancelled = true
    }

    /**
     * 监听方块物理事件（包括侦测器、重力方块等）
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onBlockPhysics(event: BlockPhysicsEvent) {
        if (!manager.isFreezeActive()) return

        val chunkKey = event.block.chunk.chunkKey
        if (!manager.isChunkFrozen(chunkKey)) return

        if (!manager.isBlockPhysicsAffected()) return

        // 如果活塞不受影响，则允许活塞相关的物理更新
        val blockType = event.block.type
        if (!manager.isPistonExtendAffected() && !manager.isPistonRetractAffected()) {
            if (blockType == org.bukkit.Material.PISTON ||
                blockType == org.bukkit.Material.STICKY_PISTON ||
                blockType == org.bukkit.Material.PISTON_HEAD ||
                blockType == org.bukkit.Material.MOVING_PISTON) {
                return  // 允许活塞物理更新
            }
        }

        event.isCancelled = true
    }

    /**
     * 监听 TNT 被点燃事件
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onTNTPrime(event: TNTPrimeEvent) {
        if (!manager.isFreezeActive()) return

        val chunkKey = event.block.chunk.chunkKey
        if (!manager.isChunkFrozen(chunkKey)) return

        if (!manager.isTntPrimeAffected()) return

        event.isCancelled = true
    }

    /**
     * 监听爆炸准备事件（TNT、苦力怕、末影水晶等）
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onExplosionPrime(event: ExplosionPrimeEvent) {
        if (!manager.isFreezeActive()) return

        val chunkKey = event.entity.location.chunk.chunkKey
        if (!manager.isChunkFrozen(chunkKey)) return

        if (!manager.isExplosionAffected()) return

        event.isCancelled = true
    }

    /**
     * 监听实体生成事件（拦截 TNT 实体生成）
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onEntitySpawn(event: EntitySpawnEvent) {
        // 只拦截 TNT 实体
        if (event.entityType != EntityType.TNT) return

        if (!manager.isFreezeActive()) return

        val chunkKey = event.location.chunk.chunkKey
        if (!manager.isChunkFrozen(chunkKey)) return

        if (!manager.isTntSpawnAffected()) return

        event.isCancelled = true
    }

}
