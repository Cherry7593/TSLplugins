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
import org.bukkit.plugin.java.JavaPlugin

/**
 * 红石冻结监听器
 * 使用三级过滤机制高效拦截红石、活塞、物理事件
 */
class RedstoneFreezeListener(
    private val plugin: JavaPlugin,
    private val manager: RedstoneFreezeManager
) : Listener {

    /**
     * 监听红石信号变化事件
     * 优先级设为 LOWEST 以最早拦截
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onBlockRedstone(event: BlockRedstoneEvent) {
        // 第一级过滤：全局标记位
        if (!manager.isFreezeActive()) return
        if (!manager.isEnabled()) return

        // 第二级过滤：检查 Chunk 是否在冻结名单
        val chunkKey = event.block.chunk.chunkKey
        if (!manager.isChunkFrozen(chunkKey)) return

        // 第三级：阻止红石信号变化
        event.newCurrent = event.oldCurrent
    }

    /**
     * 监听活塞伸出事件
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onPistonExtend(event: BlockPistonExtendEvent) {
        // 第一级过滤
        if (!manager.isFreezeActive()) return
        if (!manager.isEnabled()) return

        // 第二级过滤
        val chunkKey = event.block.chunk.chunkKey
        if (!manager.isChunkFrozen(chunkKey)) return

        // 第三级：取消活塞伸出
        event.isCancelled = true
    }

    /**
     * 监听活塞收回事件
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onPistonRetract(event: BlockPistonRetractEvent) {
        // 第一级过滤
        if (!manager.isFreezeActive()) return
        if (!manager.isEnabled()) return

        // 第二级过滤
        val chunkKey = event.block.chunk.chunkKey
        if (!manager.isChunkFrozen(chunkKey)) return

        // 第三级：取消活塞收回
        event.isCancelled = true
    }

    /**
     * 监听方块物理事件（包括侦测器、重力方块等）
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onBlockPhysics(event: BlockPhysicsEvent) {
        // 第一级过滤
        if (!manager.isFreezeActive()) return
        if (!manager.isEnabled()) return

        // 第二级过滤
        val chunkKey = event.block.chunk.chunkKey
        if (!manager.isChunkFrozen(chunkKey)) return

        // 第三级：取消物理更新
        event.isCancelled = true
    }

    /**
     * 监听 TNT 被点燃事件
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onTNTPrime(event: TNTPrimeEvent) {
        // 第一级过滤
        if (!manager.isFreezeActive()) return
        if (!manager.isEnabled()) return

        // 第二级过滤
        val chunkKey = event.block.chunk.chunkKey
        if (!manager.isChunkFrozen(chunkKey)) return

        // 第三级：取消 TNT 点燃
        event.isCancelled = true
    }

    /**
     * 监听爆炸准备事件（TNT、苦力怕、末影水晶等）
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onExplosionPrime(event: ExplosionPrimeEvent) {
        // 第一级过滤
        if (!manager.isFreezeActive()) return
        if (!manager.isEnabled()) return

        // 第二级过滤
        val chunkKey = event.entity.location.chunk.chunkKey
        if (!manager.isChunkFrozen(chunkKey)) return

        // 第三级：取消爆炸
        event.isCancelled = true
    }

    /**
     * 监听实体生成事件（拦截 TNT 实体生成）
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onEntitySpawn(event: EntitySpawnEvent) {
        // 只拦截 TNT 实体
        if (event.entityType != EntityType.TNT) return

        // 第一级过滤
        if (!manager.isFreezeActive()) return
        if (!manager.isEnabled()) return

        // 第二级过滤
        val chunkKey = event.location.chunk.chunkKey
        if (!manager.isChunkFrozen(chunkKey)) return

        // 第三级：取消 TNT 实体生成
        event.isCancelled = true
    }

}
