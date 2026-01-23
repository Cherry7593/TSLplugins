package org.tsl.tSLplugins.Landmark

import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Particle
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 地标导航指南针管理器
 */
class LandmarkCompass(
    private val plugin: JavaPlugin,
    private val manager: LandmarkManager
) {
    private val serializer = LegacyComponentSerializer.legacyAmpersand()

    // PDC 键
    private val compassKey = NamespacedKey(plugin, "landmark_compass")
    private val trackingKey = NamespacedKey(plugin, "landmark_tracking")

    // 玩家追踪状态 (UUID -> 地标ID)
    private val playerTracking: MutableMap<UUID, String> = ConcurrentHashMap()

    // 粒子任务 (UUID -> ScheduledTask)
    private val particleTasks: MutableMap<UUID, ScheduledTask> = ConcurrentHashMap()

    // 配置项
    private var particleType: Particle = Particle.END_ROD
    private var particleIntervalTicks: Long = 5L
    private var particleLength: Double = 10.0
    private var particleDensity: Int = 2
    private var particleStartDistance: Double = 1.5

    init {
        loadConfig()
    }

    /**
     * 加载配置
     */
    fun loadConfig() {
        val config = plugin.config
        particleType = try {
            Particle.valueOf(config.getString("landmark.compass.particle-type", "END_ROD")!!)
        } catch (e: Exception) {
            Particle.END_ROD
        }
        particleIntervalTicks = config.getLong("landmark.compass.particle-interval-ticks", 5L)
        particleLength = config.getDouble("landmark.compass.particle-length", 10.0)
        particleDensity = config.getInt("landmark.compass.particle-density", 2)
        particleStartDistance = config.getDouble("landmark.compass.particle-start-distance", 1.5)
    }

    /**
     * 创建导航指南针物品
     */
    fun createCompassItem(player: Player): ItemStack {
        val item = ItemStack(Material.COMPASS)
        val meta = item.itemMeta

        // 设置名称
        val nameMsg = manager.getMessage("compass_item_name")
        meta.displayName(serializer.deserialize(nameMsg))

        // 设置 PDC 标识
        meta.persistentDataContainer.set(compassKey, PersistentDataType.BYTE, 1)

        // 设置 lore
        updateCompassLore(meta, player, null)

        item.itemMeta = meta
        return item
    }

    /**
     * 更新指南针 lore
     */
    private fun updateCompassLore(meta: org.bukkit.inventory.meta.ItemMeta, player: Player, trackingLandmarkName: String?) {
        val unlocked = manager.getUnlockedLandmarks(player.uniqueId).size
        val total = manager.getAllLandmarks().size

        val progressMsg = manager.getMessage("compass_lore_progress",
            "unlocked" to unlocked.toString(),
            "total" to total.toString()
        )
        val trackingMsg = if (trackingLandmarkName != null) {
            manager.getMessage("compass_lore_tracking", "landmark" to trackingLandmarkName)
        } else {
            manager.getMessage("compass_lore_none")
        }
        val tipMsg = manager.getMessage("compass_lore_tip")

        meta.lore(listOf(
            serializer.deserialize(progressMsg),
            serializer.deserialize(trackingMsg),
            serializer.deserialize(""),
            serializer.deserialize(tipMsg)
        ))
    }

    /**
     * 检查物品是否是导航指南针
     */
    fun isLandmarkCompass(item: ItemStack?): Boolean {
        if (item == null || item.type != Material.COMPASS) return false
        val meta = item.itemMeta ?: return false
        return meta.persistentDataContainer.has(compassKey, PersistentDataType.BYTE)
    }

    /**
     * 获取未解锁地标列表（用于切换）
     */
    private fun getLockedLandmarks(player: Player): List<Landmark> {
        return manager.getAllLandmarks().filter { !manager.isUnlocked(player.uniqueId, it) }
    }

    /**
     * 切换到下一个/上一个未解锁地标
     * @param forward true=下一个, false=上一个
     */
    fun switchTracking(player: Player, forward: Boolean) {
        val lockedLandmarks = getLockedLandmarks(player)

        if (lockedLandmarks.isEmpty()) {
            val msg = manager.getMessage("compass_all_unlocked")
            player.sendMessage(serializer.deserialize(msg))
            stopTracking(player)
            return
        }

        val currentTrackingId = playerTracking[player.uniqueId]
        val currentIndex = if (currentTrackingId != null) {
            lockedLandmarks.indexOfFirst { it.id == currentTrackingId }
        } else {
            -1
        }

        val nextIndex = if (currentIndex == -1) {
            // 没有追踪时，开始追踪第一个
            0
        } else if (forward) {
            // 下一个
            (currentIndex + 1) % lockedLandmarks.size
        } else {
            // 上一个
            (currentIndex - 1 + lockedLandmarks.size) % lockedLandmarks.size
        }

        val nextLandmark = lockedLandmarks[nextIndex]
        startTracking(player, nextLandmark)
    }

    /**
     * 开始追踪地标
     */
    fun startTracking(player: Player, landmark: Landmark) {
        playerTracking[player.uniqueId] = landmark.id

        // 更新手中指南针的 lore
        updateHeldCompassLore(player, landmark.name)

        // 发送消息
        val msg = manager.getMessage("compass_tracking", "landmark" to landmark.name)
        player.sendMessage(serializer.deserialize(msg))

        // 检查是否需要启动粒子
        checkAndStartParticles(player)
    }

    /**
     * 停止追踪
     */
    fun stopTracking(player: Player) {
        playerTracking.remove(player.uniqueId)
        stopParticleTask(player.uniqueId)
        updateHeldCompassLore(player, null)
    }

    /**
     * 更新手持指南针的 lore
     */
    private fun updateHeldCompassLore(player: Player, trackingLandmarkName: String?) {
        val item = player.inventory.itemInMainHand
        if (!isLandmarkCompass(item)) return

        val meta = item.itemMeta
        updateCompassLore(meta, player, trackingLandmarkName)
        item.itemMeta = meta
    }

    /**
     * 获取玩家当前追踪的地标
     */
    fun getTrackingLandmark(player: Player): Landmark? {
        val trackingId = playerTracking[player.uniqueId] ?: return null
        return manager.storage.getLandmark(trackingId)
    }

    /**
     * 检查并启动粒子效果
     */
    fun checkAndStartParticles(player: Player) {
        val item = player.inventory.itemInMainHand
        if (!isLandmarkCompass(item)) {
            stopParticleTask(player.uniqueId)
            return
        }

        val trackingLandmark = getTrackingLandmark(player)
        if (trackingLandmark == null) {
            stopParticleTask(player.uniqueId)
            return
        }

        // 如果已有任务运行则不重复创建
        if (particleTasks.containsKey(player.uniqueId)) return

        startParticleTask(player)
    }

    /**
     * 启动粒子渲染任务
     */
    private fun startParticleTask(player: Player) {
        val task = player.scheduler.runAtFixedRate(plugin, { scheduledTask ->
            if (!player.isOnline) {
                scheduledTask.cancel()
                particleTasks.remove(player.uniqueId)
                return@runAtFixedRate
            }

            // 检查是否仍持有指南针
            val item = player.inventory.itemInMainHand
            if (!isLandmarkCompass(item)) {
                scheduledTask.cancel()
                particleTasks.remove(player.uniqueId)
                return@runAtFixedRate
            }

            // 获取追踪地标
            val landmark = getTrackingLandmark(player)
            if (landmark == null) {
                scheduledTask.cancel()
                particleTasks.remove(player.uniqueId)
                return@runAtFixedRate
            }

            // 检查维度
            if (player.world.name != landmark.world) {
                // 跨维度时发送提示（但不频繁发送，每5秒一次）
                return@runAtFixedRate
            }

            // 渲染粒子引导线
            renderParticleTrail(player, landmark)

        }, null, 1L, particleIntervalTicks)

        if (task != null) {
            particleTasks[player.uniqueId] = task
        }
    }

    /**
     * 停止粒子任务
     */
    private fun stopParticleTask(playerUuid: UUID) {
        particleTasks.remove(playerUuid)?.cancel()
    }

    /**
     * 渲染粒子引导线
     */
    private fun renderParticleTrail(player: Player, landmark: Landmark) {
        val playerLoc = player.location
        val center = landmark.region.center()
        val targetX = center.first
        val targetZ = center.third

        // 计算方向向量
        val dx = targetX - playerLoc.x
        val dz = targetZ - playerLoc.z
        val distance = Math.sqrt(dx * dx + dz * dz)

        if (distance < 2) return // 太近不显示

        // 归一化方向
        val dirX = dx / distance
        val dirZ = dz / distance

        // 从玩家前方开始渲染粒子
        val startX = playerLoc.x + dirX * particleStartDistance
        val startZ = playerLoc.z + dirZ * particleStartDistance
        val y = playerLoc.y + 1.0 // 眼睛高度

        // 渲染粒子线
        val actualLength = minOf(particleLength, distance - 1)
        val step = 1.0 / particleDensity

        var currentDist = 0.0
        while (currentDist < actualLength) {
            val x = startX + dirX * currentDist
            val z = startZ + dirZ * currentDist

            // 只对该玩家显示粒子
            player.spawnParticle(particleType, x, y, z, 1, 0.0, 0.0, 0.0, 0.0)

            currentDist += step
        }
    }

    /**
     * 处理玩家到达追踪目标
     */
    fun handleArrival(player: Player, landmark: Landmark) {
        val trackingLandmark = getTrackingLandmark(player) ?: return

        // 检查是否是追踪的目标
        if (trackingLandmark.id != landmark.id) return

        // 发送到达提示
        val arrivalMsg = manager.getMessage("compass_arrived", "landmark" to landmark.name)
        player.sendMessage(serializer.deserialize(arrivalMsg))

        // 自动切换到下一个未解锁地标
        val lockedLandmarks = getLockedLandmarks(player)
        if (lockedLandmarks.isEmpty()) {
            val allDoneMsg = manager.getMessage("compass_all_unlocked")
            player.sendMessage(serializer.deserialize(allDoneMsg))
            stopTracking(player)
        } else {
            // 切换到下一个
            switchTracking(player, true)
        }
    }

    /**
     * 处理维度提示（避免频繁提示）
     */
    private val dimensionMessageCooldowns: MutableMap<UUID, Long> = ConcurrentHashMap()

    fun checkDimensionAndNotify(player: Player): Boolean {
        val landmark = getTrackingLandmark(player) ?: return true

        if (player.world.name == landmark.world) return true

        // 检查冷却（5秒）
        val lastTime = dimensionMessageCooldowns[player.uniqueId] ?: 0
        if (System.currentTimeMillis() - lastTime < 5000) return false

        dimensionMessageCooldowns[player.uniqueId] = System.currentTimeMillis()

        val dimensionName = when (landmark.world) {
            "world" -> "主世界"
            "world_nether" -> "下界"
            "world_the_end" -> "末地"
            else -> landmark.world
        }

        val msg = manager.getMessage("compass_different_dimension", "dimension" to dimensionName)
        player.sendMessage(serializer.deserialize(msg))
        return false
    }

    /**
     * 清理玩家数据
     */
    fun cleanupPlayer(playerUuid: UUID) {
        playerTracking.remove(playerUuid)
        stopParticleTask(playerUuid)
        dimensionMessageCooldowns.remove(playerUuid)
    }

    /**
     * 关闭所有任务
     */
    fun shutdown() {
        particleTasks.values.forEach { it.cancel() }
        particleTasks.clear()
    }
}
