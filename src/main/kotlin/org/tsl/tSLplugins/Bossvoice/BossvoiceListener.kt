package org.tsl.tSLplugins.Bossvoice

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.events.ListenerPriority
import com.comphenix.protocol.events.PacketAdapter
import com.comphenix.protocol.events.PacketEvent
import org.bukkit.Location
import org.bukkit.plugin.java.JavaPlugin

/**
 * Boss 声音监听器
 * 使用 ProtocolLib 拦截声音数据包，控制末影龙死亡、凋零生成、末地传送门激活的声音范围
 * 性能最优：每个声音只产生一次包判定，无需遍历玩家或循环播放声音
 */
class BossvoiceListener(private val plugin: JavaPlugin) {

    private val protocolManager = ProtocolLibrary.getProtocolManager()
    private var packetAdapter: PacketAdapter? = null

    /**
     * 启动声音包拦截器
     */
    fun enable() {
        if (packetAdapter != null) {
            plugin.logger.warning("BossvoiceListener 已经启用，跳过重复启用")
            return
        }

        packetAdapter = object : PacketAdapter(
            plugin,
            ListenerPriority.NORMAL,
            PacketType.Play.Server.NAMED_SOUND_EFFECT,
            PacketType.Play.Server.WORLD_EVENT
        ) {
            override fun onPacketSending(event: PacketEvent) {
                val packet = event.packet

                // 处理 NAMED_SOUND_EFFECT 包（命名声音效果）
                if (event.packetType == PacketType.Play.Server.NAMED_SOUND_EFFECT) {
                    handleNamedSoundPacket(event)
                }

                // 处理 WORLD_EVENT 包（世界事件，包括末地传送门激活）
                if (event.packetType == PacketType.Play.Server.WORLD_EVENT) {
                    handleWorldEventPacket(event)
                }
            }
        }

        protocolManager.addPacketListener(packetAdapter!!)
        plugin.logger.info("Bossvoice 声音拦截器已启用（使用 ProtocolLib）")
    }

    /**
     * 停止声音包拦截器
     */
    fun disable() {
        packetAdapter?.let {
            protocolManager.removePacketListener(it)
            packetAdapter = null
            plugin.logger.info("Bossvoice 声音拦截器已停用")
        }
    }

    /**
     * 处理命名声音效果包
     */
    private fun handleNamedSoundPacket(event: PacketEvent) {
        val packet = event.packet
        val player = event.player

        // 获取声音类型
        val soundEffect = packet.soundEffects.read(0)
        val soundKey = soundEffect.key.toString()

        // 获取声音位置
        val x = packet.integers.read(0) / 8.0
        val y = packet.integers.read(1) / 8.0
        val z = packet.integers.read(2) / 8.0
        val world = player.world
        val soundLocation = Location(world, x, y, z)

        // 判断是否是我们要控制的 Boss 声音
        val range = when {
            soundKey.contains("entity.ender_dragon.death") -> {
                plugin.config.getDouble("bossvoice.ender-dragon-death", -1.0)
            }
            soundKey.contains("entity.wither.spawn") -> {
                plugin.config.getDouble("bossvoice.wither-spawn", -1.0)
            }
            else -> return // 不是我们要控制的声音，放行
        }

        // -1 表示全服都能听到（默认行为），放行
        if (range == -1.0) return

        // 0 表示静音，取消发送
        if (range == 0.0) {
            event.isCancelled = true
            return
        }

        // 检查玩家是否在范围内
        val playerLocation = player.location
        if (!isInRange(playerLocation, soundLocation, range)) {
            // 玩家不在范围内，取消发送声音包
            event.isCancelled = true
        }
        // 在范围内的玩家会正常接收声音包
    }

    /**
     * 处理世界事件包（例如末地传送门激活）
     */
    private fun handleWorldEventPacket(event: PacketEvent) {
        val packet = event.packet
        val player = event.player

        // 获取事件类型
        val eventId = packet.integers.read(0)

        // 1038 = 末地传送门激活的事件ID
        if (eventId != 1038) return

        val range = plugin.config.getDouble("bossvoice.end-portal-activate", -1.0)

        // -1 表示全服都能听到（默认行为），放行
        if (range == -1.0) return

        // 获取事件位置
        val blockPosition = packet.blockPositionModifier.read(0)
        val world = player.world
        val eventLocation = Location(world, blockPosition.x.toDouble(), blockPosition.y.toDouble(), blockPosition.z.toDouble())

        // 0 表示静音，取消发送
        if (range == 0.0) {
            event.isCancelled = true
            return
        }

        // 检查玩家是否在范围内
        val playerLocation = player.location
        if (!isInRange(playerLocation, eventLocation, range)) {
            // 玩家不在范围内，取消发送事件包
            event.isCancelled = true
        }
    }

    /**
     * 检查玩家是否在范围内
     * @param playerLoc 玩家位置
     * @param targetLoc 目标位置
     * @param range 范围（格子）
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

