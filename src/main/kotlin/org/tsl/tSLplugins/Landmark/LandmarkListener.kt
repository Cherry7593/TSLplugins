package org.tsl.tSLplugins.Landmark

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.title.Title
import org.bukkit.Sound
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.plugin.java.JavaPlugin
import java.time.Duration

/**
 * 地标系统事件监听器
 */
class LandmarkListener(
    private val plugin: JavaPlugin,
    private val manager: LandmarkManager
) : Listener {

    private val serializer = LegacyComponentSerializer.legacyAmpersand()

    /**
     * 监听玩家移动，检测区域进入/离开
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerMove(event: PlayerMoveEvent) {
        if (!manager.isEnabled()) return

        val player = event.player
        val from = event.from
        val to = event.to ?: return

        // 仅在方块坐标变化时检测（性能优化）
        if (from.blockX == to.blockX && from.blockY == to.blockY && from.blockZ == to.blockZ) {
            return
        }

        val playerUuid = player.uniqueId
        val currentLandmarkId = manager.getPlayerCurrentLandmark(playerUuid)
        val newLandmark = manager.getLandmarkAt(to)

        // 检测进入/离开
        when {
            // 进入新地标
            newLandmark != null && currentLandmarkId != newLandmark.id -> {
                manager.setPlayerCurrentLandmark(playerUuid, newLandmark.id)

                // 检查进入提示冷却
                if (manager.canShowEnterMessage(playerUuid, newLandmark.id)) {
                    manager.setEnterMessageCooldown(playerUuid, newLandmark.id)

                    val isNewUnlock = manager.unlockLandmark(playerUuid, newLandmark)

                    // 发送增强提示
                    showEnterNotification(player, newLandmark, isNewUnlock)
                }
            }
            // 离开地标（到无地标区域，非切换到另一地标）
            newLandmark == null && currentLandmarkId != null -> {
                // 获取被离开的地标信息
                val leftLandmark = manager.storage.getLandmark(currentLandmarkId)
                manager.setPlayerCurrentLandmark(playerUuid, null)

                // 检查离开提示冷却
                if (leftLandmark != null && manager.canShowLeaveMessage(playerUuid, currentLandmarkId)) {
                    manager.setLeaveMessageCooldown(playerUuid, currentLandmarkId)
                    showLeaveNotification(player, leftLandmark)
                }
            }
        }

        // 检查传送吟唱是否被打断
        if (manager.castTimeSeconds > 0 && manager.cancelOnMove) {
            val request = manager.getTeleportRequest(playerUuid)
            if (request != null) {
                val start = request.startLocation
                val distance = Math.sqrt(
                    Math.pow(to.x - start.first, 2.0) +
                    Math.pow(to.y - start.second, 2.0) +
                    Math.pow(to.z - start.third, 2.0)
                )
                if (distance > 0.5) {
                    manager.cancelTeleportRequest(playerUuid)
                    val cancelMsg = manager.getMessage("teleport_cancelled_move")
                    player.sendMessage(serializer.deserialize(cancelMsg))
                }
            }
        }
    }

    /**
     * 监听玩家受伤，取消传送吟唱
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerDamage(event: EntityDamageEvent) {
        if (!manager.isEnabled()) return

        val player = event.entity as? org.bukkit.entity.Player ?: return
        val playerUuid = player.uniqueId

        val request = manager.getTeleportRequest(playerUuid)
        if (request != null) {
            manager.cancelTeleportRequest(playerUuid)
            val cancelMsg = manager.getMessage("teleport_cancelled_damage")
            player.sendMessage(serializer.deserialize(cancelMsg))
        }
    }

    /**
     * 玩家加入时初始化
     */
    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        if (!manager.isEnabled()) return

        val player = event.player

        // 检查玩家当前是否在地标内
        player.scheduler.runDelayed(plugin, { _ ->
            val landmark = manager.getLandmarkAt(player.location)
            if (landmark != null) {
                manager.setPlayerCurrentLandmark(player.uniqueId, landmark.id)
            }
        }, null, 5L)
    }

    /**
     * 玩家退出时清理
     */
    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        manager.cleanupPlayer(event.player.uniqueId)
    }

    /**
     * 显示进入地标的增强提示
     */
    private fun showEnterNotification(player: org.bukkit.entity.Player, landmark: Landmark, isNewUnlock: Boolean) {
        val landmarkName = landmark.name

        // 1. 大标题提示
        val titleText = if (isNewUnlock) {
            serializer.deserialize("&6★ &e${landmarkName} &6★")
        } else {
            serializer.deserialize("&a$landmarkName")
        }
        val subtitleText = if (isNewUnlock) {
            serializer.deserialize("&f已解锁新地标!")
        } else {
            serializer.deserialize("&7欢迎到达")
        }

        val titleTimes = Title.Times.times(
            Duration.ofMillis(200),   // fadeIn
            Duration.ofMillis(2000),  // stay
            Duration.ofMillis(500)    // fadeOut
        )
        val title = Title.title(titleText, subtitleText, titleTimes)
        player.showTitle(title)

        // 2. 音效反馈
        if (isNewUnlock) {
            // 新解锁使用更欢快的音效
            player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f)
            player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.5f)
        } else {
            // 普通进入使用轻柔的提示音
            player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_BELL, 0.7f, 1.2f)
        }

        // 3. ActionBar 提示
        val actionBarMsg = if (isNewUnlock) {
            serializer.deserialize("&6✦ &e已解锁: $landmarkName &6✦")
        } else {
            serializer.deserialize("&a▶ &f当前位置: &e$landmarkName")
        }
        player.sendActionBar(actionBarMsg)

        // 4. 聊天消息（保留原有功能）
        val enterMsg = manager.getMessage("enter", "landmark" to landmarkName)
        player.sendMessage(serializer.deserialize(enterMsg))

        if (isNewUnlock) {
            val unlockMsg = manager.getMessage("unlocked", "landmark" to landmarkName)
            player.sendMessage(serializer.deserialize(unlockMsg))
        }
    }

    /**
     * 显示离开地标的提示（较进入提示轻量：ActionBar + 聊天消息）
     */
    private fun showLeaveNotification(player: org.bukkit.entity.Player, landmark: Landmark) {
        val landmarkName = landmark.name

        // 1. ActionBar 提示
        val actionBarMsg = serializer.deserialize("&c◀ &7离开: &f$landmarkName")
        player.sendActionBar(actionBarMsg)

        // 2. 聊天消息
        val leaveMsg = manager.getMessage("leave", "landmark" to landmarkName)
        player.sendMessage(serializer.deserialize(leaveMsg))
    }

    /**
     * 启动传送吟唱检查任务（仅在发起传送时调用）
     */
    fun startCastTimeTask(player: org.bukkit.entity.Player) {
        if (manager.castTimeSeconds <= 0) return

        player.scheduler.runAtFixedRate(plugin, { task ->
            if (!player.isOnline) {
                task.cancel()
                return@runAtFixedRate
            }

            val request = manager.getTeleportRequest(player.uniqueId)
            if (request == null) {
                task.cancel()
                return@runAtFixedRate
            }

            val elapsed = (System.currentTimeMillis() - request.startTime) / 1000

            if (elapsed >= manager.castTimeSeconds) {
                task.cancel()
                // 吟唱完成，执行传送
                if (manager.completeTeleportRequest(player)) {
                    val successMsg = manager.getMessage("teleport_success",
                        "landmark" to request.targetLandmark.name)
                    player.sendMessage(serializer.deserialize(successMsg))
                }
            } else {
                // 显示吟唱进度
                val remaining = manager.castTimeSeconds - elapsed.toInt()
                val castingMsg = manager.getMessage("teleport_casting",
                    "seconds" to remaining.toString(),
                    "landmark" to request.targetLandmark.name)
                player.sendActionBar(serializer.deserialize(castingMsg))
            }
        }, null, 5L, 20L)
    }
}
