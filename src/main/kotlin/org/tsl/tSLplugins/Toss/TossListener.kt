package org.tsl.tSLplugins.Toss

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.java.JavaPlugin

/**
 * Toss 监听器
 * 处理玩家举起、投掷、放下生物的逻辑
 */
class TossListener(
    private val plugin: JavaPlugin,
    private val manager: TossManager
) : Listener {

    private val serializer = LegacyComponentSerializer.legacyAmpersand()

    /**
     * 处理与实体的交互（举起生物）
     */
    @EventHandler
    fun onPlayerInteractEntity(event: PlayerInteractEntityEvent) {
        val player = event.player
        val entity = event.rightClicked

        // 检查是否是可举起的实体
        if (entity !is LivingEntity || entity is Player) {
            return
        }

        // 必须按住 Shift
        if (!player.isSneaking) {
            return
        }

        // 检查功能是否启用
        if (!manager.isEnabled()) {
            return
        }

        // 检查权限
        if (!player.hasPermission("tsl.toss.use")) {
            if (manager.isShowMessages()) {
                player.sendMessage(serializer.deserialize(manager.getMessage("no_permission")))
            }
            return
        }

        // 检查玩家是否启用了功能
        if (!manager.isPlayerEnabled(player.uniqueId)) {
            if (manager.isShowMessages()) {
                player.sendMessage(serializer.deserialize(manager.getMessage("player_disabled")))
            }
            return
        }

        // 检查实体是否在黑名单中
        if (manager.isEntityBlacklisted(entity.type) && !player.hasPermission("tsl.toss.bypass")) {
            if (manager.isShowMessages()) {
                player.sendMessage(serializer.deserialize(manager.getMessage("entity_blacklisted")))
            }
            return
        }

        // 检查实体是否已经在乘客链中
        if (isEntityInPassengerChain(entity)) {
            if (manager.isShowMessages()) {
                player.sendMessage(serializer.deserialize(manager.getMessage("entity_already_lifted")))
            }
            return
        }

        // 举起生物
        pickupEntity(player, entity)
        event.isCancelled = true
    }

    /**
     * 处理玩家交互（投掷和放下）
     */
    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val player = event.player
        val action = event.action

        // 必须按住 Shift
        if (!player.isSneaking) {
            return
        }

        // 检查功能是否启用
        if (!manager.isEnabled()) {
            return
        }

        // 检查权限
        if (!player.hasPermission("tsl.toss.use")) {
            return
        }

        // 检查玩家是否启用了功能
        if (!manager.isPlayerEnabled(player.uniqueId)) {
            return
        }

        // 检查玩家是否举起了生物
        val passengerCount = getPassengerChainCount(player)
        if (passengerCount == 0) {
            return
        }

        when (action) {
            Action.LEFT_CLICK_AIR, Action.LEFT_CLICK_BLOCK -> {
                // Shift + 左键：投掷生物
                throwTopEntity(player)
                event.isCancelled = true
            }
            Action.RIGHT_CLICK_AIR, Action.RIGHT_CLICK_BLOCK -> {
                // Shift + 右键空气/地面：放下所有生物
                dropAllEntities(player)
                event.isCancelled = true
            }
            else -> {}
        }
    }

    /**
     * 玩家退出时清理数据
     */
    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        cleanupPlayerEntities(event.player)
        manager.cleanupPlayer(event.player.uniqueId)
    }

    /**
     * 举起实体（叠罗汉效果）
     */
    private fun pickupEntity(player: Player, entity: LivingEntity) {
        player.scheduler.run(plugin, { _ ->
            // 获取当前乘客链数量
            val currentCount = getPassengerChainCount(player)

            // 检查是否已达上限
            if (currentCount >= manager.getMaxLiftCount()) {
                if (manager.isShowMessages()) {
                    val message = manager.getMessage(
                        "max_limit_reached",
                        "max" to manager.getMaxLiftCount().toString()
                    )
                    player.sendMessage(serializer.deserialize(message))
                }
                return@run
            }

            // 找到乘客链的顶端
            val topEntity = getTopPassenger(player)

            // 将新实体添加到顶端
            if (topEntity != null) {
                // 安全检查：避免循环引用
                if (topEntity == entity) {
                    if (manager.isShowMessages()) {
                        player.sendMessage(serializer.deserialize(manager.getMessage("circular_reference")))
                    }
                    return@run
                }

                // 检查实体是否已在链中
                if (isEntityInPlayerPassengerChain(player, entity)) {
                    if (manager.isShowMessages()) {
                        player.sendMessage(serializer.deserialize(manager.getMessage("entity_in_chain")))
                    }
                    return@run
                }

                topEntity.addPassenger(entity)
            } else {
                // 没有乘客，直接骑在玩家头上
                player.addPassenger(entity)
            }

            val newCount = currentCount + 1
            if (manager.isShowMessages()) {
                val message = manager.getMessage(
                    "pickup_success",
                    "entity" to getEntityDisplayName(entity),
                    "current" to newCount.toString(),
                    "max" to manager.getMaxLiftCount().toString()
                )
                player.sendMessage(serializer.deserialize(message))
            }
        }, null)
    }

    /**
     * 投掷顶端的实体
     */
    private fun throwTopEntity(player: Player) {
        player.scheduler.run(plugin, { _ ->
            val topEntity = getTopPassenger(player)
            if (topEntity == null) {
                if (manager.isShowMessages()) {
                    player.sendMessage(serializer.deserialize(manager.getMessage("no_entity_lifted")))
                }
                return@run
            }

            // 移除顶端实体
            val parent = topEntity.vehicle
            parent?.removePassenger(topEntity)

            // 计算投掷方向和速度
            val direction = player.location.direction.normalize()
            val velocity = manager.getPlayerThrowVelocity(player.uniqueId)
            val throwVelocity = direction.multiply(velocity)

            // 添加向上的分量
            throwVelocity.y = throwVelocity.y + 0.3

            // 设置实体速度
            topEntity.velocity = throwVelocity

            val remainingCount = getPassengerChainCount(player)
            if (manager.isShowMessages()) {
                val message = manager.getMessage(
                    "throw_success",
                    "entity" to getEntityDisplayName(topEntity),
                    "remaining" to remainingCount.toString()
                )
                player.sendMessage(serializer.deserialize(message))
            }
        }, null)
    }

    /**
     * 放下所有举起的实体
     */
    private fun dropAllEntities(player: Player) {
        player.scheduler.run(plugin, { _ ->
            val allPassengers = getAllPassengers(player)
            if (allPassengers.isEmpty()) {
                if (manager.isShowMessages()) {
                    player.sendMessage(serializer.deserialize(manager.getMessage("no_entity_lifted")))
                }
                return@run
            }

            // 移除所有乘客关系并轻柔放下
            for (entity in allPassengers) {
                if (entity.isValid) {
                    val vehicle = entity.vehicle
                    vehicle?.removePassenger(entity)

                    // 给实体一个轻微的向前速度
                    val direction = player.location.direction.normalize().multiply(0.2)
                    entity.velocity = direction
                }
            }

            val count = allPassengers.size
            if (manager.isShowMessages()) {
                val message = manager.getMessage("drop_all_success", "count" to count.toString())
                player.sendMessage(serializer.deserialize(message))
            }
        }, null)
    }

    /**
     * 清理玩家举起的所有实体
     */
    private fun cleanupPlayerEntities(player: Player) {
        player.scheduler.run(plugin, { _ ->
            val allPassengers = getAllPassengers(player)
            for (entity in allPassengers) {
                if (entity.isValid) {
                    val vehicle = entity.vehicle
                    vehicle?.removePassenger(entity)

                    // 给实体一个轻微的速度避免卡在玩家身体里
                    val direction = player.location.direction.normalize().multiply(0.2)
                    entity.velocity = direction
                }
            }
        }, null)
    }

    /**
     * 获取玩家乘客链的数量
     */
    private fun getPassengerChainCount(player: Player): Int {
        return getAllPassengers(player).size
    }

    /**
     * 获取玩家的所有乘客（递归）
     */
    private fun getAllPassengers(entity: Entity): List<Entity> {
        val passengers = mutableListOf<Entity>()
        for (passenger in entity.passengers) {
            passengers.add(passenger)
            passengers.addAll(getAllPassengers(passenger))
        }
        return passengers
    }

    /**
     * 获取乘客链的顶端实体
     */
    private fun getTopPassenger(entity: Entity): Entity? {
        val passengers = entity.passengers
        if (passengers.isEmpty()) {
            return null
        }

        var top = passengers[0]
        while (top.passengers.isNotEmpty()) {
            top = top.passengers[0]
        }
        return top
    }

    /**
     * 检查实体是否在任何乘客链中
     */
    private fun isEntityInPassengerChain(entity: Entity): Boolean {
        // 检查实体是否有乘客或是否是乘客
        return entity.vehicle != null || entity.passengers.isNotEmpty()
    }

    /**
     * 检查实体是否在玩家的乘客链中
     */
    private fun isEntityInPlayerPassengerChain(player: Player, entity: Entity): Boolean {
        return getAllPassengers(player).contains(entity)
    }

    /**
     * 获取实体的显示名称
     */
    private fun getEntityDisplayName(entity: Entity): String {
        return if (entity.customName() != null) {
            entity.customName()?.let { serializer.serialize(it) } ?: entity.type.name
        } else {
            entity.type.name
        }
    }
}

