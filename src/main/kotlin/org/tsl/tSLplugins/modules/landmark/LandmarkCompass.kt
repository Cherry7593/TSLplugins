package org.tsl.tSLplugins.modules.landmark

import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Particle
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.CompassMeta
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.atan2
import kotlin.math.roundToInt

/**
 * 地标导航指南针管理器
 * 混合引导方案：ActionBar方向指示 + 磁石指南针 + 可选粒子
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

    // 导航任务 (UUID -> ScheduledTask)
    private val navigationTasks: MutableMap<UUID, ScheduledTask> = ConcurrentHashMap()

    // 配置项
    private var enableParticle: Boolean = false  // 默认关闭粒子
    private var particleType: Particle = Particle.END_ROD
    private var particleIntervalTicks: Long = 10L
    private var particleLength: Double = 5.0
    private var particleDensity: Int = 1
    private var particleStartDistance: Double = 2.0
    private var actionBarIntervalTicks: Long = 10L

    init {
        loadConfig()
    }

    /**
     * 加载配置
     */
    fun loadConfig() {
        val config = plugin.config
        enableParticle = config.getBoolean("landmark.compass.enable-particle", false)
        particleType = try {
            Particle.valueOf(config.getString("landmark.compass.particle-type", "END_ROD")!!)
        } catch (e: Exception) {
            Particle.END_ROD
        }
        particleIntervalTicks = config.getLong("landmark.compass.particle-interval-ticks", 10L)
        particleLength = config.getDouble("landmark.compass.particle-length", 5.0)
        particleDensity = config.getInt("landmark.compass.particle-density", 1)
        particleStartDistance = config.getDouble("landmark.compass.particle-start-distance", 2.0)
        actionBarIntervalTicks = config.getLong("landmark.compass.actionbar-interval-ticks", 10L)
    }

    /**
     * 创建导航指南针物品
     */
    fun createCompassItem(player: Player): ItemStack {
        val item = ItemStack(Material.COMPASS)
        val meta = item.itemMeta as CompassMeta

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

        // 更新手中指南针（lore + 磁石指向）
        updateHeldCompass(player, landmark)

        // 发送消息
        val msg = manager.getMessage("compass_tracking", "landmark" to landmark.name)
        player.sendMessage(serializer.deserialize(msg))

        // 检查是否需要启动导航任务
        checkAndStartNavigation(player)
    }

    /**
     * 停止追踪
     */
    fun stopTracking(player: Player) {
        playerTracking.remove(player.uniqueId)
        stopNavigationTask(player.uniqueId)
        resetHeldCompass(player)
    }

    /**
     * 更新手持指南针（lore + 磁石指向）
     */
    private fun updateHeldCompass(player: Player, landmark: Landmark) {
        val item = player.inventory.itemInMainHand
        if (!isLandmarkCompass(item)) return

        val meta = item.itemMeta as? CompassMeta ?: return
        
        // 更新 lore
        updateCompassLore(meta, player, landmark.name)
        
        // 设置磁石指向（让指南针指向目标）
        val world = Bukkit.getWorld(landmark.world)
        if (world != null) {
            val center = landmark.region.center()
            val targetLocation = Location(world, center.first, center.second, center.third)
            meta.lodestone = targetLocation
            meta.isLodestoneTracked = false  // 不需要真实磁石
        }
        
        item.itemMeta = meta
    }

    /**
     * 重置指南针（清除磁石指向）
     */
    private fun resetHeldCompass(player: Player) {
        val item = player.inventory.itemInMainHand
        if (!isLandmarkCompass(item)) return

        val meta = item.itemMeta as? CompassMeta ?: return
        updateCompassLore(meta, player, null)
        meta.lodestone = null
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
     * 检查并启动导航任务（ActionBar + 可选粒子）
     */
    fun checkAndStartParticles(player: Player) {
        checkAndStartNavigation(player)
    }

    /**
     * 检查并启动导航任务
     */
    private fun checkAndStartNavigation(player: Player) {
        val item = player.inventory.itemInMainHand
        if (!isLandmarkCompass(item)) {
            stopNavigationTask(player.uniqueId)
            return
        }

        val trackingLandmark = getTrackingLandmark(player)
        if (trackingLandmark == null) {
            stopNavigationTask(player.uniqueId)
            return
        }

        // 更新指南针指向
        updateHeldCompass(player, trackingLandmark)

        // 如果已有任务运行则不重复创建
        if (navigationTasks.containsKey(player.uniqueId)) return

        startNavigationTask(player)
    }

    /**
     * 启动导航任务（ActionBar显示 + 可选粒子）
     */
    private fun startNavigationTask(player: Player) {
        val task = player.scheduler.runAtFixedRate(plugin, { scheduledTask ->
            if (!player.isOnline) {
                scheduledTask.cancel()
                navigationTasks.remove(player.uniqueId)
                return@runAtFixedRate
            }

            // 检查是否仍持有指南针
            val item = player.inventory.itemInMainHand
            if (!isLandmarkCompass(item)) {
                scheduledTask.cancel()
                navigationTasks.remove(player.uniqueId)
                return@runAtFixedRate
            }

            // 获取追踪地标
            val landmark = getTrackingLandmark(player)
            if (landmark == null) {
                scheduledTask.cancel()
                navigationTasks.remove(player.uniqueId)
                return@runAtFixedRate
            }

            // 检查维度
            if (player.world.name != landmark.world) {
                checkDimensionAndNotify(player)
                return@runAtFixedRate
            }

            // 显示 ActionBar 方向指示
            showActionBarNavigation(player, landmark)
            
            // 可选：渲染粒子引导线
            if (enableParticle) {
                renderParticleTrail(player, landmark)
            }

        }, null, 1L, actionBarIntervalTicks)

        if (task != null) {
            navigationTasks[player.uniqueId] = task
        }
    }

    /**
     * 停止导航任务
     */
    private fun stopNavigationTask(playerUuid: UUID) {
        navigationTasks.remove(playerUuid)?.cancel()
    }

    /**
     * 显示 ActionBar 方向导航
     */
    private fun showActionBarNavigation(player: Player, landmark: Landmark) {
        val playerLoc = player.location
        val center = landmark.region.center()
        val targetX = center.first
        val targetZ = center.third

        // 计算距离
        val dx = targetX - playerLoc.x
        val dz = targetZ - playerLoc.z
        val distance = Math.sqrt(dx * dx + dz * dz).roundToInt()

        // 计算方向（角度）
        val targetAngle = Math.toDegrees(atan2(-dx, dz)).let { if (it < 0) it + 360 else it }
        val playerYaw = ((playerLoc.yaw % 360) + 360) % 360
        
        // 计算相对角度（玩家视角到目标的角度差）
        var relativeAngle = targetAngle - playerYaw
        if (relativeAngle > 180) relativeAngle -= 360
        if (relativeAngle < -180) relativeAngle += 360

        // 获取方向指示器
        val directionIndicator = getDirectionIndicator(relativeAngle)
        
        // 获取方位名称
        val directionName = getDirectionName(targetAngle)
        
        // 格式化距离
        val distanceText = if (distance >= 1000) {
            "${(distance / 1000.0 * 10).roundToInt() / 10.0}km"
        } else {
            "${distance}m"
        }

        // 构建 ActionBar 消息
        val actionBarMsg = manager.getMessage("compass_actionbar",
            "indicator" to directionIndicator,
            "direction" to directionName,
            "landmark" to landmark.name,
            "distance" to distanceText
        )
        
        player.sendActionBar(serializer.deserialize(actionBarMsg))
    }

    /**
     * 获取方向指示器（箭头）
     */
    private fun getDirectionIndicator(relativeAngle: Double): String {
        return when {
            relativeAngle >= -22.5 && relativeAngle < 22.5 -> "⬆"      // 正前方
            relativeAngle >= 22.5 && relativeAngle < 67.5 -> "⬈"      // 右前
            relativeAngle >= 67.5 && relativeAngle < 112.5 -> "➡"     // 右
            relativeAngle >= 112.5 && relativeAngle < 157.5 -> "⬊"    // 右后
            relativeAngle >= 157.5 || relativeAngle < -157.5 -> "⬇"   // 后方
            relativeAngle >= -157.5 && relativeAngle < -112.5 -> "⬋"  // 左后
            relativeAngle >= -112.5 && relativeAngle < -67.5 -> "⬅"   // 左
            relativeAngle >= -67.5 && relativeAngle < -22.5 -> "⬉"    // 左前
            else -> "•"
        }
    }

    /**
     * 获取方位名称
     */
    private fun getDirectionName(angle: Double): String {
        return when {
            angle >= 337.5 || angle < 22.5 -> "南"
            angle >= 22.5 && angle < 67.5 -> "西南"
            angle >= 67.5 && angle < 112.5 -> "西"
            angle >= 112.5 && angle < 157.5 -> "西北"
            angle >= 157.5 && angle < 202.5 -> "北"
            angle >= 202.5 && angle < 247.5 -> "东北"
            angle >= 247.5 && angle < 292.5 -> "东"
            angle >= 292.5 && angle < 337.5 -> "东南"
            else -> ""
        }
    }

    /**
     * 渲染粒子引导线（可选，默认关闭）
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

        if (distance < 5) return // 太近不显示

        // 归一化方向
        val dirX = dx / distance
        val dirZ = dz / distance

        // 从玩家前方开始渲染粒子（地面高度）
        val startX = playerLoc.x + dirX * particleStartDistance
        val startZ = playerLoc.z + dirZ * particleStartDistance
        val y = playerLoc.y + 0.1 // 贴地显示

        // 渲染粒子线
        val actualLength = minOf(particleLength, distance - 2)
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
        stopNavigationTask(playerUuid)
        dimensionMessageCooldowns.remove(playerUuid)
    }

    /**
     * 关闭所有任务
     */
    fun shutdown() {
        navigationTasks.values.forEach { it.cancel() }
        navigationTasks.clear()
    }
}
