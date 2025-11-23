package org.tsl.tSLplugins.Kiss

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

/**
 * Kiss 执行器
 * 负责执行亲吻动作：粒子效果、音效、消息
 */
class KissExecutor(
    private val plugin: JavaPlugin,
    private val manager: KissManager
) {

    private val serializer = LegacyComponentSerializer.legacyAmpersand()

    /**
     * 执行亲吻动作
     */
    fun executeKiss(sender: Player, target: Player) {
        // 增加统计数据
        manager.incrementKissCount(sender.uniqueId)
        manager.incrementKissedCount(target.uniqueId)

        // 发送消息给发起者
        sender.sendMessage(serializer.deserialize(
            manager.getMessage("kiss_sent", "player" to target.name)
        ))

        // 发送消息给目标
        target.sendMessage(serializer.deserialize(
            manager.getMessage("kiss_received", "player" to sender.name)
        ))

        // 在目标玩家位置生成粒子效果和音效
        target.scheduler.run(plugin, { _ ->
            if (target.isValid && target.isOnline) {
                spawnHeartParticles(target.location)
                playKissSound(target)
            }
        }, null)
    }

    /**
     * 生成爱心粒子效果
     */
    private fun spawnHeartParticles(location: Location) {
        val headLocation = location.clone().add(0.0, 2.0, 0.0)

        // 生成大量爱心粒子
        location.world.spawnParticle(
            Particle.HEART,
            headLocation,
            20,              // 粒子数量
            0.5,             // X 偏移
            0.5,             // Y 偏移
            0.5,             // Z 偏移
            0.1              // 速度
        )
    }

    /**
     * 播放亲吻音效
     */
    private fun playKissSound(player: Player) {
        // 播放悦耳的音效
        player.playSound(
            player.location,
            Sound.ENTITY_PLAYER_LEVELUP,
            1.0f,            // 音量
            1.5f             // 音调（提高音调让声音更甜美）
        )
    }
}

