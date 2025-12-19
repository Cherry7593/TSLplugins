package org.tsl.tSLplugins.RedstoneFreeze

import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.util.concurrent.ConcurrentHashMap

/**
 * 红石冻结管理器
 * 负责管理冻结区域的 Chunk 坐标和配置
 */
class RedstoneFreezeManager(private val plugin: JavaPlugin) {

    // 配置项
    private var enabled: Boolean = true
    private var maxRadius: Int = 32
    private var bossBarTitle: String = "§c❄ 当前区域物理已冻结 ❄"

    // 冻结状态（全局标记位）
    @Volatile
    private var freezeActive: Boolean = false

    // 冻结的 Chunk Key 集合（线程安全）
    private val frozenChunks: MutableSet<Long> = ConcurrentHashMap.newKeySet()

    // 冻结区域的世界（用于清理）
    @Volatile
    private var frozenWorld: World? = null

    // 冻结区域的中心和半径（用于显示信息）
    @Volatile
    private var freezeCenterX: Int = 0
    @Volatile
    private var freezeCenterZ: Int = 0
    @Volatile
    private var freezeRadius: Int = 0

    // 缓存的区块数量（用于取消时返回）
    @Volatile
    private var cachedChunkCount: Int = 0

    // BossBar
    private var freezeBossBar: BossBar? = null

    // BossBar 更新任务
    private var bossBarTask: ScheduledTask? = null

    // 当前显示 BossBar 的玩家
    private val bossBarPlayers: MutableSet<Player> = ConcurrentHashMap.newKeySet()

    init {
        loadConfig()
    }

    fun loadConfig() {
        val config = plugin.config
        val section = config.getConfigurationSection("redstone-freeze") ?: return

        enabled = section.getBoolean("enabled", true)
        maxRadius = section.getInt("max-radius", 32)
        bossBarTitle = section.getString("bossbar-title", "§c❄ 当前区域物理已冻结 ❄") ?: "§c❄ 当前区域物理已冻结 ❄"

        plugin.logger.info("[RedstoneFreeze] 配置已加载 - 最大半径: $maxRadius")
    }

    fun isEnabled(): Boolean = enabled
    fun getMaxRadius(): Int = maxRadius

    /**
     * 检查冻结是否激活（第一级过滤）
     */
    fun isFreezeActive(): Boolean = freezeActive

    /**
     * 检查 Chunk 是否被冻结（第二级过滤）
     */
    fun isChunkFrozen(chunkKey: Long): Boolean = frozenChunks.contains(chunkKey)

    /**
     * 检查 Chunk 是否被冻结（通过 Chunk 对象）
     */
    fun isChunkFrozen(chunk: Chunk): Boolean = frozenChunks.contains(chunk.chunkKey)

    /**
     * 激活冻结区域
     * @param player 执行命令的玩家
     * @param radius 冻结半径（以区块为单位）
     * @return 冻结的区块数量
     */
    fun activateFreeze(player: Player, radius: Int): Int {
        // 先取消之前的冻结
        if (freezeActive) {
            cancelFreezeInternal()
        }

        val world = player.world
        val playerChunk = player.location.chunk
        val centerChunkX = playerChunk.x
        val centerChunkZ = playerChunk.z

        // 计算范围内的所有 Chunk
        for (cx in (centerChunkX - radius)..(centerChunkX + radius)) {
            for (cz in (centerChunkZ - radius)..(centerChunkZ + radius)) {
                // 圆形范围检查
                val dx = cx - centerChunkX
                val dz = cz - centerChunkZ
                if (dx * dx + dz * dz <= radius * radius) {
                    val chunkKey = Chunk.getChunkKey(cx, cz)
                    frozenChunks.add(chunkKey)
                }
            }
        }

        // 记录冻结信息
        frozenWorld = world
        freezeCenterX = centerChunkX
        freezeCenterZ = centerChunkZ
        freezeRadius = radius
        cachedChunkCount = frozenChunks.size

        // 激活冻结
        freezeActive = true

        // 创建 BossBar
        createBossBar()

        // 启动 BossBar 更新任务
        startBossBarTask()

        plugin.logger.info("[RedstoneFreeze] 已冻结 $cachedChunkCount 个区块（中心: $centerChunkX, $centerChunkZ, 半径: $radius）")
        return cachedChunkCount
    }

    /**
     * 取消冻结
     * @return 之前冻结的区块数量
     */
    fun cancelFreeze(): Int {
        val count = cachedChunkCount
        cancelFreezeInternal()
        plugin.logger.info("[RedstoneFreeze] 已取消冻结，释放 $count 个区块")
        return count
    }

    /**
     * 内部取消冻结（不输出日志）
     */
    private fun cancelFreezeInternal() {
        // 先关闭标记位
        freezeActive = false

        // 停止 BossBar 任务
        stopBossBarTask()

        // 移除 BossBar
        removeBossBar()

        // 清除数据
        frozenChunks.clear()
        frozenWorld = null
        freezeCenterX = 0
        freezeCenterZ = 0
        freezeRadius = 0
        cachedChunkCount = 0
    }

    /**
     * 创建 BossBar
     */
    private fun createBossBar() {
        freezeBossBar = BossBar.bossBar(
            Component.text(bossBarTitle.replace("§", "§").replace("&", "§"))
                .color(NamedTextColor.RED),
            1.0f,
            BossBar.Color.BLUE,
            BossBar.Overlay.PROGRESS
        )
    }

    /**
     * 移除 BossBar
     */
    private fun removeBossBar() {
        val bar = freezeBossBar ?: return
        // 从所有玩家移除
        bossBarPlayers.forEach { player ->
            try {
                player.hideBossBar(bar)
            } catch (_: Exception) {}
        }
        bossBarPlayers.clear()
        freezeBossBar = null
    }

    /**
     * 启动 BossBar 更新任务
     */
    private fun startBossBarTask() {
        bossBarTask = Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, { _ ->
            updateBossBarVisibility()
        }, 20L, 20L)  // 每秒更新一次
    }

    /**
     * 停止 BossBar 更新任务
     */
    private fun stopBossBarTask() {
        bossBarTask?.cancel()
        bossBarTask = null
    }

    /**
     * 更新 BossBar 可见性
     */
    private fun updateBossBarVisibility() {
        val bar = freezeBossBar ?: return
        val world = frozenWorld ?: return

        // 遍历该世界的所有在线玩家
        world.players.forEach { player ->
            val chunkKey = player.location.chunk.chunkKey
            val isInFrozenArea = frozenChunks.contains(chunkKey)

            if (isInFrozenArea && !bossBarPlayers.contains(player)) {
                // 玩家进入冻结区域，显示 BossBar
                player.showBossBar(bar)
                bossBarPlayers.add(player)
            } else if (!isInFrozenArea && bossBarPlayers.contains(player)) {
                // 玩家离开冻结区域，隐藏 BossBar
                player.hideBossBar(bar)
                bossBarPlayers.remove(player)
            }
        }

        // 清理已离线的玩家
        bossBarPlayers.removeIf { !it.isOnline }
    }

    /**
     * 获取冻结信息
     */
    fun getFreezeInfo(): String? {
        if (!freezeActive) return null
        val worldName = frozenWorld?.name ?: "未知"
        return "世界: $worldName, 中心: ($freezeCenterX, $freezeCenterZ), 半径: $freezeRadius, 区块数: $cachedChunkCount"
    }

    /**
     * 获取冻结的区块数量
     */
    fun getFrozenChunkCount(): Int = cachedChunkCount

    /**
     * 清理资源（插件卸载时调用）
     */
    fun cleanup() {
        cancelFreezeInternal()
    }
}
