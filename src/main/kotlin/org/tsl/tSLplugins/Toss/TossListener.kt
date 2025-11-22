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
     * 发送消息的辅助方法（减少重复代码）
     */
    private fun sendMessage(player: Player, messageKey: String, vararg replacements: Pair<String, String>) {
        if (manager.isShowMessages()) {
            val message = manager.getMessage(messageKey, *replacements)
            player.sendMessage(serializer.deserialize(message))
        }
    }

    /**
     * 处理与实体的交互（举起生物）
     */
    @EventHandler(ignoreCancelled = true)
    fun onPlayerInteractEntity(event: PlayerInteractEntityEvent) {
        val player = event.player
        val entity = event.rightClicked

        // 快速检查：必须按住 Shift
        if (!player.isSneaking) return

        // 快速检查：是否是可举起的生物实体
        if (entity !is LivingEntity || entity is Player) return

        // 快速检查：功能是否启用
        if (!manager.isEnabled()) return

        // 权限检查
        if (!player.hasPermission("tsl.toss.use")) {
            sendMessage(player, "no_permission")
            return
        }

        // 玩家开关状态检查
        if (!manager.isPlayerEnabled(player.uniqueId)) {
            sendMessage(player, "player_disabled")
            return
        }

        // 黑名单检查
        if (manager.isEntityBlacklisted(entity.type) &&
            !player.hasPermission("tsl.toss.bypass")) {
            sendMessage(player, "entity_blacklisted")
            event.isCancelled = true
            return
        }

        // 检查实体是否已在乘客链中
        if (isEntityInPassengerChain(entity)) {
            sendMessage(player, "entity_already_lifted")
            return
        }

        // 举起生物
        pickupEntity(player, entity)
        event.isCancelled = true
    }

    /**
     * 处理玩家交互（投掷生物）
     */
    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val player = event.player

        // 快速检查：必须按住 Shift
        if (!player.isSneaking) return

        // 快速检查：功能是否启用
        if (!manager.isEnabled()) return

        // 权限检查
        if (!player.hasPermission("tsl.toss.use")) return

        // 玩家开关状态检查
        if (!manager.isPlayerEnabled(player.uniqueId)) return

        // 检查玩家是否举起了生物
        if (getPassengerChainCount(player) == 0) return

        // 只处理左键投掷
        if (event.action == Action.LEFT_CLICK_AIR || event.action == Action.LEFT_CLICK_BLOCK) {
            // Shift + 左键：投掷顶端生物
            throwTopEntity(player)
            event.isCancelled = true
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
            // 验证实体和玩家仍然有效
            if (!entity.isValid || !player.isOnline) return@run

            // 获取当前乘客链数量
            val currentCount = getPassengerChainCount(player)

            // 检查是否已达上限
            if (currentCount >= manager.getMaxLiftCount()) {
                sendMessage(player, "max_limit_reached",
                    "max" to manager.getMaxLiftCount().toString())
                return@run
            }

            // 找到乘客链的顶端
            val topEntity = getTopPassenger(player)

            if (topEntity != null) {
                // 安全检查：避免循环引用和重复添加
                when {
                    topEntity == entity -> {
                        sendMessage(player, "circular_reference")
                        return@run
                    }
                    isEntityInPlayerPassengerChain(player, entity) -> {
                        sendMessage(player, "entity_in_chain")
                        return@run
                    }
                }
                topEntity.addPassenger(entity)
            } else {
                // 没有乘客，直接骑在玩家头上
                player.addPassenger(entity)
            }

            // 发送成功消息
            sendMessage(player, "pickup_success",
                "entity" to getEntityDisplayName(entity),
                "current" to (currentCount + 1).toString(),
                "max" to manager.getMaxLiftCount().toString()
            )
        }, null)
    }

    /**
     * 投掷顶端的实体
     */
    private fun throwTopEntity(player: Player) {
        player.scheduler.run(plugin, { _ ->
            val topEntity = getTopPassenger(player)
            if (topEntity == null) {
                sendMessage(player, "no_entity_lifted")
                return@run
            }

            // 移除顶端实体
            val parent = topEntity.vehicle
            parent?.removePassenger(topEntity)

            // 计算投掷方向和速度
            val direction = player.location.direction.normalize()
            val velocity = manager.getPlayerThrowVelocity(player.uniqueId)
            val throwVelocity = direction.multiply(velocity)

            // 添加向上的分量（使用 setY 方法）
            throwVelocity.setY(throwVelocity.y + 0.3)

            // 设置实体速度
            topEntity.velocity = throwVelocity

            // 发送成功消息
            sendMessage(player, "throw_success",
                "entity" to getEntityDisplayName(topEntity),
                "remaining" to getPassengerChainCount(player).toString()
            )
        }, null)
    }

    /**
     * 放下所有举起的实体
     */
    private fun dropAllEntities(player: Player) {
        player.scheduler.run(plugin, { _ ->
            val allPassengers = getAllPassengers(player)

            if (allPassengers.isEmpty()) {
                sendMessage(player, "no_entity_lifted")
                return@run
            }

            // 移除所有乘客并轻柔放下
            val direction = player.location.direction.normalize().multiply(0.2)
            allPassengers.forEach { entity ->
                if (entity.isValid) {
                    entity.vehicle?.removePassenger(entity)
                    entity.velocity = direction
                }
            }

            // 发送成功消息
            sendMessage(player, "drop_all_success",
                "count" to allPassengers.size.toString())
        }, null)
    }

    /**
     * 清理玩家举起的所有实体
     */
    private fun cleanupPlayerEntities(player: Player) {
        player.scheduler.run(plugin, { _ ->
            val direction = player.location.direction.normalize().multiply(0.2)
            getAllPassengers(player).forEach { entity ->
                if (entity.isValid) {
                    entity.vehicle?.removePassenger(entity)
                    entity.velocity = direction
                }
            }
        }, null)
    }

    /**
     * 获取玩家乘客链的数量
     */
    private fun getPassengerChainCount(player: Player): Int = getAllPassengers(player).size

    /**
     * 获取实体的所有乘客（递归，尾递归优化）
     */
    private fun getAllPassengers(entity: Entity): List<Entity> {
        val result = mutableListOf<Entity>()

        fun collectPassengers(current: Entity) {
            current.passengers.forEach { passenger ->
                result.add(passenger)
                collectPassengers(passenger)
            }
        }

        collectPassengers(entity)
        return result
    }

    /**
     * 获取乘客链的顶端实体
     */
    private fun getTopPassenger(entity: Entity): Entity? {
        val passengers = entity.passengers
        if (passengers.isEmpty()) return null

        var current: Entity = passengers.first()
        while (current.passengers.isNotEmpty()) {
            current = current.passengers.first()
        }
        return current
    }

    /**
     * 检查实体是否在任何乘客链中
     */
    private fun isEntityInPassengerChain(entity: Entity): Boolean =
        entity.vehicle != null || entity.passengers.isNotEmpty()

    /**
     * 检查实体是否在玩家的乘客链中
     */
    private fun isEntityInPlayerPassengerChain(player: Player, entity: Entity): Boolean =
        getAllPassengers(player).contains(entity)

    /**
     * 获取实体的显示名称
     */
    private fun getEntityDisplayName(entity: Entity): String {
        return entity.customName()?.let { serializer.serialize(it) } ?: entity.type.name
    }
}

