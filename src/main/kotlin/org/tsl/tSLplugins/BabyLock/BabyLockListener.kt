package org.tsl.tSLplugins.BabyLock

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.entity.Ageable
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityBreedEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.plugin.java.JavaPlugin

/**
 * BabyLock 监听器
 * 处理生物命名和年龄锁定逻辑
 */
class BabyLockListener(
    private val plugin: JavaPlugin,
    private val manager: BabyLockManager
) : Listener {

    private val serializer = LegacyComponentSerializer.legacyAmpersand()
    private val plainSerializer = PlainTextComponentSerializer.plainText()

    /**
     * 监听玩家与实体交互（命名牌命名）
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerInteractEntity(event: PlayerInteractEntityEvent) {
        // 只处理主手交互
        if (event.hand != EquipmentSlot.HAND) return

        // 检查功能是否启用
        if (!manager.isEnabled()) return

        val entity = event.rightClicked
        val player = event.player

        // 必须是 Ageable 生物
        if (entity !is Ageable) return

        // 延迟检查，等待名字更新
        entity.scheduler.run(plugin, { _ ->
            checkAndUpdateLock(entity, player)
        }, null)
    }

    /**
     * 监听生物繁殖事件（新生幼年生物）
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEntityBreed(event: EntityBreedEvent) {
        // 检查功能是否启用
        if (!manager.isEnabled()) return

        val entity = event.entity
        if (entity !is Ageable) return

        // 延迟检查，等待可能的命名
        entity.scheduler.runDelayed(plugin, { _ ->
            checkAndUpdateLock(entity, event.breeder as? Player)
        }, null, 10L)  // 延迟 0.5 秒
    }

    /**
     * 检查并更新生物的锁定状态
     */
    private fun checkAndUpdateLock(entity: Ageable, player: Player?) {
        // 验证实体仍然有效
        if (!entity.isValid) return

        val customName = entity.customName()
        if (customName == null) {
            // 没有名字，确保解锁
            if (entity.ageLock) {
                entity.ageLock = false
            }
            return
        }

        val plainName = plainSerializer.serialize(customName)

        // 检查是否应该锁定
        if (manager.shouldLock(entity)) {
            // 需要锁定
            if (!entity.ageLock) {
                manager.lockBaby(entity)

                // 发送成功消息
                player?.let {
                    val message = manager.getMessage("lock",
                        "entity" to plainName
                    )
                    it.sendMessage(serializer.deserialize(message))
                }

                plugin.logger.info("[BabyLock] 锁定生物: ${entity.type} - $plainName")
            }
        } else {
            // 需要解锁
            if (entity.ageLock) {
                manager.unlockBaby(entity)

                // 发送解锁消息
                player?.let {
                    val message = manager.getMessage("unlock",
                        "entity" to plainName
                    )
                    it.sendMessage(serializer.deserialize(message))
                }

                plugin.logger.info("[BabyLock] 解锁生物: ${entity.type} - $plainName")
            }
        }
    }
}

