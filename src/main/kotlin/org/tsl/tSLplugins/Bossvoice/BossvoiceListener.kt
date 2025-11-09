package org.tsl.tSLplugins.Bossvoice

import org.bukkit.Location
import org.bukkit.Sound
import org.bukkit.entity.EnderDragon
import org.bukkit.entity.Wither
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.EntitySpawnEvent
import org.bukkit.event.world.PortalCreateEvent
import org.bukkit.plugin.java.JavaPlugin

/**
 * Boss 声音监听器
 * 控制末影龙死亡、凋零生成、末地传送门激活的声音范围
 */
class BossvoiceListener(private val plugin: JavaPlugin) : Listener {

    /**
     * 监听末影龙死亡
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onEnderDragonDeath(event: EntityDeathEvent) {
        if (event.entity !is EnderDragon) return

        val range = plugin.config.getDouble("bossvoice.ender-dragon-death", -1.0)

        // -1 表示全服都能听到（默认行为，不做处理）
        if (range == -1.0) return

        // 0 表示静音（取消所有声音）
        if (range == 0.0) {
            // 末影龙死亡时的默认声音由客户端处理，这里播放自定义范围声音
            playRangedSound(event.entity.location, Sound.ENTITY_ENDER_DRAGON_DEATH, range, 1.0f, 1.0f)
            return
        }

        // 播放指定范围的声音
        playRangedSound(event.entity.location, Sound.ENTITY_ENDER_DRAGON_DEATH, range, 1.0f, 1.0f)
    }

    /**
     * 监听凋零生成
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onWitherSpawn(event: EntitySpawnEvent) {
        if (event.entity !is Wither) return

        val range = plugin.config.getDouble("bossvoice.wither-spawn", -1.0)

        // -1 表示全服都能听到（默认行为，不做处理）
        if (range == -1.0) return

        // 0 表示静音
        if (range == 0.0) {
            return
        }

        // 播放指定范围的声音
        playRangedSound(event.location, Sound.ENTITY_WITHER_SPAWN, range, 1.0f, 1.0f)
    }

    /**
     * 监听末地传送门激活
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onPortalCreate(event: PortalCreateEvent) {
        // 只处理末地传送门
        if (event.reason != PortalCreateEvent.CreateReason.END_PLATFORM) return

        val range = plugin.config.getDouble("bossvoice.end-portal-activate", -1.0)

        // -1 表示全服都能听到（默认行为，不做处理）
        if (range == -1.0) return

        // 获取传送门中心位置
        val blocks = event.blocks
        if (blocks.isEmpty()) return

        val centerLocation = blocks.first().location

        // 0 表示静音
        if (range == 0.0) {
            return
        }

        // 播放指定范围的声音
        playRangedSound(centerLocation, Sound.BLOCK_END_PORTAL_SPAWN, range, 1.0f, 1.0f)
    }

    /**
     * 播放指定范围的声音
     * @param location 声音位置
     * @param sound 声音类型
     * @param range 声音范围（格子）
     * @param volume 音量
     * @param pitch 音调
     */
    private fun playRangedSound(
        location: Location,
        sound: Sound,
        range: Double,
        volume: Float,
        pitch: Float
    ) {
        val world = location.world ?: return

        // 获取范围内的所有玩家
        world.players.forEach { player ->
            if (isInRange(player.location, location, range)) {
                player.playSound(location, sound, volume, pitch)
            }
        }
    }

    /**
     * 检查玩家是否在范围内
     * @param playerLoc 玩家位置
     * @param targetLoc 目标位置
     * @param range 范围
     * @return 是否在范围内
     */
    private fun isInRange(playerLoc: Location, targetLoc: Location, range: Double): Boolean {
        // 必须在同一个世界
        if (playerLoc.world != targetLoc.world) return false

        // 计算距离
        val distance = playerLoc.distance(targetLoc)
        return distance <= range
    }
}

