package org.tsl.tSLplugins.Phantom

import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import org.bukkit.Bukkit
import org.bukkit.Statistic
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.tsl.tSLplugins.TSLPlayerProfileStore

/**
 * 幻翼控制管理器
 * 通过定期重置 TIME_SINCE_REST 统计来控制幻翼是否出现
 * 性能优先：使用低频定时任务（每 300 秒）
 */
class PhantomManager(private val plugin: JavaPlugin) {

    /** Profile 存储（用于获取玩家配置） */
    private lateinit var profileStore: TSLPlayerProfileStore

    /** 定时任务引用（用于取消任务） */
    private var scheduledTask: ScheduledTask? = null

    // ===== 配置缓存 =====
    private var enabled: Boolean = true
    private var checkInterval: Long = 300L  // 检查间隔（秒）

    init {
        loadConfig()
    }

    /**
     * 设置 Profile Store（由主类调用）
     */
    fun setProfileStore(store: TSLPlayerProfileStore) {
        profileStore = store
    }

    /**
     * 加载配置
     */
    fun loadConfig() {
        val config = plugin.config

        enabled = config.getBoolean("phantom.enabled", true)
        checkInterval = config.getLong("phantom.checkInterval", 300L)

        plugin.logger.info("[Phantom] 配置已加载 - 启用: $enabled, 检查间隔: $checkInterval 秒")
    }

    /**
     * 功能是否启用
     */
    fun isEnabled(): Boolean = enabled

    /**
     * 获取检查间隔（秒）
     */
    fun getCheckInterval(): Long = checkInterval

    /**
     * 启动定时任务
     * 使用全局调度器，每 checkInterval 秒执行一次
     * 如果已有任务在运行，会先取消旧任务
     */
    fun startTask() {
        // 先取消旧任务（如果存在）
        stopTask()

        if (!enabled) {
            plugin.logger.info("[Phantom] 功能未启用，跳过启动定时任务")
            return
        }

        // 使用全局调度器（Folia 兼容）
        // 延迟 checkInterval 秒后首次执行，然后每 checkInterval 秒执行一次
        val intervalTicks = checkInterval * 20L  // 转换为 tick

        scheduledTask = Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, { _ ->
            processAllPlayers()
        }, intervalTicks, intervalTicks)

        plugin.logger.info("[Phantom] 定时任务已启动 - 间隔: $checkInterval 秒")
    }

    /**
     * 停止定时任务
     */
    fun stopTask() {
        scheduledTask?.cancel()
        scheduledTask = null
        plugin.logger.info("[Phantom] 定时任务已停止")
    }

    /**
     * 处理所有在线玩家
     * 遍历在线玩家，根据配置重置或保持 TIME_SINCE_REST 统计
     */
    private fun processAllPlayers() {
        if (!::profileStore.isInitialized) {
            plugin.logger.warning("[Phantom] ProfileStore 未初始化，跳过处理")
            return
        }

        try {
            val onlinePlayers = Bukkit.getOnlinePlayers()
            var processedCount = 0
            var resetCount = 0

            onlinePlayers.forEach { player ->
                // 使用玩家的调度器处理（Folia 线程安全）
                player.scheduler.run(plugin, { _ ->
                    try {
                        processPlayer(player)?.let { reset ->
                            if (reset) {
                                resetCount++
                            }
                        }
                    } catch (e: Exception) {
                        plugin.logger.warning("[Phantom] 处理玩家失败: ${player.name} - ${e.message}")
                    }
                }, null)

                processedCount++
            }

            if (processedCount > 0) {
                plugin.logger.info("[Phantom] 定时检查完成 - 处理: $processedCount 人, 重置: $resetCount 人")
            }
        } catch (e: Exception) {
            plugin.logger.severe("[Phantom] 定时任务执行失败: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * 处理单个玩家
     * 根据玩家的 allowPhantom 配置决定是否重置 TIME_SINCE_REST
     *
     * @param player 要处理的玩家
     * @return true = 重置了统计, false = 未重置, null = 未处理
     */
    private fun processPlayer(player: Player): Boolean? {
        if (!::profileStore.isInitialized) return null

        try {
            // 获取玩家配置
            val profile = profileStore.get(player.uniqueId) ?: return null

            if (!profile.allowPhantom) {
                // 不允许幻翼骚扰，重置 TIME_SINCE_REST 为 0
                player.setStatistic(Statistic.TIME_SINCE_REST, 0)
                return true
            } else {
                // 允许幻翼骚扰，不修改统计，让原版机制生效
                return false
            }
        } catch (e: Exception) {
            plugin.logger.warning("[Phantom] 处理玩家统计失败: ${player.name} - ${e.message}")
            return null
        }
    }

    /**
     * 获取玩家的幻翼允许状态
     */
    fun isPhantomAllowed(player: Player): Boolean {
        if (!::profileStore.isInitialized) return false

        return profileStore.get(player.uniqueId)?.allowPhantom ?: false
    }

    /**
     * 设置玩家的幻翼允许状态
     */
    fun setPhantomAllowed(player: Player, allowed: Boolean) {
        if (!::profileStore.isInitialized) {
            plugin.logger.warning("[Phantom] ProfileStore 未初始化")
            return
        }

        try {
            val profile = profileStore.getOrCreate(player.uniqueId, player.name)
            profile.allowPhantom = allowed

            // 如果禁用幻翼，立即重置统计
            if (!allowed) {
                player.scheduler.run(plugin, { _ ->
                    try {
                        player.setStatistic(Statistic.TIME_SINCE_REST, 0)
                        plugin.logger.info("[Phantom] 已重置 ${player.name} 的 TIME_SINCE_REST")
                    } catch (e: Exception) {
                        plugin.logger.warning("[Phantom] 重置统计失败: ${e.message}")
                    }
                }, null)
            }
        } catch (e: Exception) {
            plugin.logger.warning("[Phantom] 设置幻翼状态失败: ${e.message}")
        }
    }
}

