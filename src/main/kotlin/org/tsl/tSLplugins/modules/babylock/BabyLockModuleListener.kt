package org.tsl.tSLplugins.modules.babylock

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.entity.Ageable
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityBreedEvent
import org.bukkit.event.entity.EntitySpawnEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.world.EntitiesLoadEvent
import org.bukkit.inventory.EquipmentSlot

/**
 * BabyLock 监听器
 * 处理生物命名和年龄锁定逻辑
 */
class BabyLockModuleListener(
    private val module: BabyLockModule
) : Listener {

    private val serializer = LegacyComponentSerializer.legacyAmpersand()
    private val plainSerializer = PlainTextComponentSerializer.plainText()
    private val plugin get() = module.getPlugin()

    /**
     * 监听玩家与实体交互（命名牌命名）
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerInteractEntity(event: PlayerInteractEntityEvent) {
        if (event.hand != EquipmentSlot.HAND) return
        if (!module.isEnabled()) return

        val entity = event.rightClicked
        val player = event.player

        if (entity !is Ageable) return

        // 延迟检查，等待名字更新 - 使用 Folia 实体调度器
        entity.scheduler.run(plugin, { _ ->
            checkAndUpdateLock(entity, player)
        }, null)
    }

    /**
     * 监听生物繁殖事件（新生幼年生物）
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEntityBreed(event: EntityBreedEvent) {
        if (!module.isEnabled()) return

        val entity = event.entity
        if (entity !is Ageable) return

        // 延迟检查 - 使用 Folia 实体调度器
        entity.scheduler.runDelayed(plugin, { _ ->
            checkAndUpdateLock(entity, event.breeder as? Player)
        }, null, 10L)  // 延迟 0.5 秒
    }

    /**
     * 监听实体生成事件
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEntitySpawn(event: EntitySpawnEvent) {
        if (!module.isEnabled()) return

        val entity = event.entity
        if (entity !is Ageable) return

        // 使用 Folia 实体调度器
        entity.scheduler.run(plugin, { _ ->
            checkAndReapplyLock(entity)
        }, null)
    }

    /**
     * 监听实体批量加载事件（区块加载）
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onEntitiesLoad(event: EntitiesLoadEvent) {
        if (!module.isEnabled()) return

        event.entities.forEach { entity ->
            if (entity is Ageable) {
                entity.scheduler.run(plugin, { _ ->
                    checkAndReapplyLock(entity)
                }, null)
            }
        }
    }

    /**
     * 检查并重新应用锁定（用于实体加载）
     */
    private fun checkAndReapplyLock(entity: Ageable) {
        if (!entity.isValid) return

        val customName = entity.customName() ?: return
        val plainName = plainSerializer.serialize(customName)

        if (module.hasLockPrefix(plainName)) {
            if (!module.isTypeEnabled(entity.type)) return

            val isBaby = entity.age < 0 || entity.ageLock

            if (isBaby && !entity.ageLock) {
                module.lockBaby(entity)
            }
        }
    }

    /**
     * 检查并更新生物的锁定状态
     */
    private fun checkAndUpdateLock(entity: Ageable, player: Player?) {
        if (!entity.isValid) return

        val customName = entity.customName()
        if (customName == null) {
            if (entity.ageLock) {
                entity.ageLock = false
            }
            return
        }

        val plainName = plainSerializer.serialize(customName)

        if (module.shouldLock(entity)) {
            if (!entity.ageLock) {
                module.lockBaby(entity)

                player?.let {
                    val message = module.getModuleMessage("lock",
                        "entity" to plainName
                    )
                    it.sendMessage(serializer.deserialize(message))
                }
            }
        } else {
            if (entity.ageLock) {
                module.unlockBaby(entity)

                player?.let {
                    val message = module.getModuleMessage("unlock",
                        "entity" to plainName
                    )
                    it.sendMessage(serializer.deserialize(message))
                }
            }
        }
    }
}
