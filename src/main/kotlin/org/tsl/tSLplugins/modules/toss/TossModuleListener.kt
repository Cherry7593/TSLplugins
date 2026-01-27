package org.tsl.tSLplugins.modules.toss

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerGameModeChangeEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerQuitEvent

/**
 * Toss 监听器
 * 处理玩家举起、投掷、放下生物的逻辑
 */
class TossModuleListener(
    private val module: TossModule
) : Listener {

    private val serializer = LegacyComponentSerializer.legacyAmpersand()
    private val plugin get() = module.getPlugin()

    /**
     * 发送消息的辅助方法
     */
    private fun sendMessage(player: Player, messageKey: String, vararg replacements: Pair<String, String>) {
        if (module.isShowMessages()) {
            val message = module.getModuleMessage(messageKey, *replacements)
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

        // 快速检查
        if (!player.isSneaking) return
        if (entity !is LivingEntity || entity is Player) return
        if (!module.isEnabled()) return

        // 权限检查
        if (!player.hasPermission("tsl.toss.use")) {
            sendMessage(player, "no_permission")
            return
        }

        // 玩家开关状态检查
        if (!module.isPlayerEnabled(player)) {
            sendMessage(player, "player_disabled")
            return
        }

        // 黑名单检查
        if (module.isEntityBlacklisted(entity.type) &&
            !player.hasPermission("tsl.toss.bypass")) {
            sendMessage(player, "entity_blacklisted")
            event.isCancelled = true
            return
        }

        // 检查实体是否已被其他玩家持有
        if (isEntityHeldByPlayer(entity)) {
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

        if (!player.isSneaking) return
        if (!module.isEnabled()) return
        if (!player.hasPermission("tsl.toss.use")) return
        if (!module.isPlayerEnabled(player)) return
        if (!hasPassengers(player)) return

        // 只处理左键投掷
        if (event.action == Action.LEFT_CLICK_AIR || event.action == Action.LEFT_CLICK_BLOCK) {
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
    }

    /**
     * 玩家切换游戏模式时清理举起的生物
     */
    @EventHandler
    fun onGameModeChange(event: PlayerGameModeChangeEvent) {
        val player = event.player
        if (hasPassengers(player)) {
            cleanupPlayerEntities(player)
        }
    }

    /**
     * 举起实体（叠罗汉效果）- 使用 Folia 调度器
     */
    private fun pickupEntity(player: Player, entity: LivingEntity) {
        player.scheduler.run(plugin, { _ ->
            if (!entity.isValid || !player.isOnline) return@run

            // 如果实体当前有载具，先将其移除
            entity.vehicle?.removePassenger(entity)

            val currentCount = getPassengerChainCount(player)

            if (currentCount >= module.getMaxLiftCount()) {
                sendMessage(player, "max_limit_reached",
                    "max" to module.getMaxLiftCount().toString())
                return@run
            }

            val topEntity = getTopPassenger(player)

            if (topEntity != null) {
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
                player.addPassenger(entity)
            }

            sendMessage(player, "pickup_success",
                "entity" to getEntityDisplayName(entity),
                "current" to (currentCount + 1).toString(),
                "max" to module.getMaxLiftCount().toString()
            )
        }, null)
    }

    /**
     * 投掷顶端的实体 - 使用 Folia 调度器
     */
    private fun throwTopEntity(player: Player) {
        player.scheduler.run(plugin, { _ ->
            val topEntity = getTopPassenger(player)
            if (topEntity == null) {
                sendMessage(player, "no_entity_lifted")
                return@run
            }

            val parent = topEntity.vehicle
            parent?.removePassenger(topEntity)

            val direction = player.location.direction.normalize()
            val velocity = module.getPlayerThrowVelocity(player)
            val throwVelocity = direction.multiply(velocity)
            throwVelocity.setY(throwVelocity.y + 0.3)

            topEntity.velocity = throwVelocity

            sendMessage(player, "throw_success",
                "entity" to getEntityDisplayName(topEntity),
                "remaining" to getPassengerChainCount(player).toString()
            )
        }, null)
    }

    /**
     * 清理玩家举起的所有实体 - 使用 Folia 调度器
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

    private fun hasPassengers(entity: Entity): Boolean = entity.passengers.isNotEmpty()

    private fun getPassengerChainCount(entity: Entity): Int {
        var count = 0
        fun countPassengers(current: Entity) {
            current.passengers.forEach { passenger ->
                count++
                countPassengers(passenger)
            }
        }
        countPassengers(entity)
        return count
    }

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

    private fun getTopPassenger(entity: Entity): Entity? {
        val passengers = entity.passengers
        if (passengers.isEmpty()) return null
        var current: Entity = passengers.first()
        while (current.passengers.isNotEmpty()) {
            current = current.passengers.first()
        }
        return current
    }

    private fun isEntityHeldByPlayer(entity: Entity): Boolean {
        var current: Entity? = entity
        while (current?.vehicle != null) {
            current = current.vehicle
        }
        return current is Player
    }

    private fun isEntityInPlayerPassengerChain(player: Player, target: Entity): Boolean {
        fun searchInChain(current: Entity): Boolean {
            for (passenger in current.passengers) {
                if (passenger == target || searchInChain(passenger)) return true
            }
            return false
        }
        return searchInChain(player)
    }

    private fun getEntityDisplayName(entity: Entity): String {
        return entity.customName()?.let { serializer.serialize(it) } ?: entity.type.name
    }
}
