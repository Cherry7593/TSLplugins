package org.tsl.tSLplugins.Spec

import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Spec 观众模式管理器
 * 允许管理员以旁观者模式循环浏览玩家视角
 * Folia 完全兼容
 */
class SpecManager(private val plugin: JavaPlugin) {

    /** 白名单 UUID 集合（不会被浏览的玩家） */
    private val whitelist = ConcurrentHashMap.newKeySet<UUID>()

    /** 正在观看的玩家及其状态 */
    private val spectatingPlayers = ConcurrentHashMap<UUID, SpectatorState>()

    // ===== 配置缓存 =====
    private var enabled: Boolean = true
    private var defaultDelay: Int = 5
    private var minDelay: Int = 1
    private var maxDelay: Int = 60

    init {
        loadConfig()
    }

    /**
     * 加载配置
     */
    fun loadConfig() {
        val config = plugin.config

        enabled = config.getBoolean("spec.enabled", true)
        defaultDelay = config.getInt("spec.defaultDelay", 5)
        minDelay = config.getInt("spec.minDelay", 1)
        maxDelay = config.getInt("spec.maxDelay", 60)

        // 加载白名单
        whitelist.clear()
        val whitelistNames = config.getStringList("spec.whitelist")
        whitelistNames.forEach { name ->
            try {
                val uuid = UUID.fromString(name)
                whitelist.add(uuid)
            } catch (e: IllegalArgumentException) {
                // 尝试通过玩家名获取 UUID
                @Suppress("DEPRECATION")
                val offlinePlayer = Bukkit.getOfflinePlayer(name)
                if (offlinePlayer.hasPlayedBefore()) {
                    whitelist.add(offlinePlayer.uniqueId)
                }
            }
        }

        plugin.logger.info("[Spec] 配置已加载 - 启用: $enabled, 默认延迟: $defaultDelay 秒, 白名单: ${whitelist.size} 人")
    }

    /**
     * 保存配置
     */
    fun saveConfig() {
        val config = plugin.config

        // 保存白名单
        val whitelistStrings = whitelist.map { it.toString() }
        config.set("spec.whitelist", whitelistStrings)

        plugin.saveConfig()
    }

    /**
     * 功能是否启用
     */
    fun isEnabled(): Boolean = enabled

    /**
     * 获取默认延迟
     */
    fun getDefaultDelay(): Int = defaultDelay

    /**
     * 验证延迟是否有效
     */
    fun validateDelay(delay: Int): Int {
        return delay.coerceIn(minDelay, maxDelay)
    }

    /**
     * 开始循环观看
     */
    fun startSpectating(player: Player, delay: Int): Boolean {
        if (isSpectating(player)) {
            return false // 已经在观看中
        }

        // 保存原始状态
        val originalGameMode = player.gameMode
        val originalLocation = player.location.clone()

        // 切换到旁观者模式
        player.scheduler.run(plugin, { _ ->
            try {
                player.gameMode = GameMode.SPECTATOR
            } catch (e: Exception) {
                plugin.logger.warning("[Spec] 设置游戏模式失败: ${e.message}")
            }
        }, null)

        // 创建观看状态
        val state = SpectatorState(
            spectator = player,
            originalGameMode = originalGameMode,
            originalLocation = originalLocation,
            delay = delay,
            currentIndex = 0
        )

        spectatingPlayers[player.uniqueId] = state

        // 启动循环任务
        startCycleTask(state)

        return true
    }

    /**
     * 停止循环观看
     */
    fun stopSpectating(player: Player): Boolean {
        val state = spectatingPlayers.remove(player.uniqueId) ?: return false

        // 取消任务
        state.cancelTask()

        // 恢复原始状态
        player.scheduler.run(plugin, { _ ->
            try {
                // 恢复游戏模式
                player.gameMode = state.originalGameMode

                // 使用异步传送（Folia 线程安全）
                player.teleportAsync(state.originalLocation).thenAccept { success ->
                    if (!success) {
                        plugin.logger.warning("[Spec] 传送玩家 ${player.name} 失败")
                    }
                }
            } catch (e: Exception) {
                plugin.logger.warning("[Spec] 恢复玩家状态失败: ${e.message}")
            }
        }, null)

        return true
    }

    /**
     * 检查玩家是否正在观看
     */
    fun isSpectating(player: Player): Boolean {
        return spectatingPlayers.containsKey(player.uniqueId)
    }

    /**
     * 启动循环任务
     */
    private fun startCycleTask(state: SpectatorState) {
        val delayTicks = state.delay * 20L

        // 使用全局调度器执行循环任务
        Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, { _ ->
            try {
                // 检查玩家是否还在线
                val spectator = Bukkit.getPlayer(state.spectator.uniqueId)
                if (spectator == null || !spectator.isOnline) {
                    stopSpectating(state.spectator)
                    return@runAtFixedRate
                }

                // 检查状态是否还存在
                if (!spectatingPlayers.containsKey(spectator.uniqueId)) {
                    return@runAtFixedRate
                }

                // 切换到下一个玩家
                switchToNextPlayer(spectator, state)
            } catch (e: Exception) {
                plugin.logger.warning("[Spec] 循环任务执行失败: ${e.message}")
            }
        }, delayTicks, delayTicks)
    }

    /**
     * 切换到下一个玩家
     */
    private fun switchToNextPlayer(spectator: Player, state: SpectatorState) {
        // 获取可观看的玩家列表
        val allViewablePlayers = getViewablePlayers(spectator)

        if (allViewablePlayers.isEmpty()) {
            // 没有可观看的玩家
            spectator.scheduler.run(plugin, { _ ->
                spectator.sendMessage("§e[Spec] 没有可观看的玩家")
            }, null)
            return
        }

        // 过滤出未观看过的玩家
        var availablePlayers = allViewablePlayers.filter { player ->
            !state.viewedPlayers.contains(player.uniqueId)
        }

        // 如果所有玩家都观看过了，开始新的循环
        if (availablePlayers.isEmpty()) {
            state.viewedPlayers.clear()
            availablePlayers = allViewablePlayers
            plugin.logger.info("[Spec] ${spectator.name} 开始新的观看循环")
        }

        // 随机选择一个玩家（避免总是按顺序观看）
        val targetPlayer = availablePlayers.random()

        // 记录已观看
        state.viewedPlayers.add(targetPlayer.uniqueId)

        // 切换视角
        spectator.scheduler.run(plugin, { _ ->
            try {
                spectator.spectatorTarget = targetPlayer
                spectator.sendMessage("§a[Spec] 正在观看: §f${targetPlayer.name}")
            } catch (e: Exception) {
                plugin.logger.warning("[Spec] 切换视角失败: ${e.message}")
            }
        }, null)
    }

    /**
     * 获取可观看的玩家列表
     */
    private fun getViewablePlayers(spectator: Player): List<Player> {
        return Bukkit.getOnlinePlayers()
            .filter { player ->
                // 排除自己
                player.uniqueId != spectator.uniqueId &&
                // 排除白名单玩家
                !whitelist.contains(player.uniqueId) &&
                // 排除其他正在观看的玩家
                !spectatingPlayers.containsKey(player.uniqueId)
            }
            .sortedBy { it.name } // 按名称排序
    }

    /**
     * 添加玩家到白名单
     */
    fun addToWhitelist(uuid: UUID): Boolean {
        val added = whitelist.add(uuid)
        if (added) {
            saveConfig()
        }
        return added
    }

    /**
     * 从白名单移除玩家
     */
    fun removeFromWhitelist(uuid: UUID): Boolean {
        val removed = whitelist.remove(uuid)
        if (removed) {
            saveConfig()
        }
        return removed
    }

    /**
     * 检查玩家是否在白名单
     */
    fun isInWhitelist(uuid: UUID): Boolean {
        return whitelist.contains(uuid)
    }

    /**
     * 获取白名单
     */
    fun getWhitelist(): Set<UUID> {
        return whitelist.toSet()
    }

    /**
     * 玩家退出时清理
     */
    fun onPlayerQuit(player: Player) {
        // 如果玩家正在观看，停止观看
        if (isSpectating(player)) {
            val state = spectatingPlayers.remove(player.uniqueId)
            state?.cancelTask()
        }
    }

    /**
     * 清理所有数据
     */
    fun cleanup() {
        // 停止所有观看任务
        spectatingPlayers.values.forEach { state ->
            try {
                state.cancelTask()
                // 尝试恢复玩家状态
                val player = Bukkit.getPlayer(state.spectator.uniqueId)
                player?.let { p ->
                    p.scheduler.run(plugin, { _ ->
                        try {
                            p.gameMode = state.originalGameMode
                            // 使用异步传送（Folia 线程安全）
                            p.teleportAsync(state.originalLocation)
                        } catch (e: Exception) {
                            // 忽略清理时的错误
                        }
                    }, null)
                }
            } catch (e: Exception) {
                plugin.logger.warning("[Spec] 清理失败: ${e.message}")
            }
        }

        spectatingPlayers.clear()
    }
}

/**
 * 观看者状态数据类
 */
data class SpectatorState(
    val spectator: Player,
    val originalGameMode: GameMode,
    val originalLocation: Location,
    val delay: Int,
    var currentIndex: Int,
    val viewedPlayers: MutableSet<UUID> = mutableSetOf() // 已观看过的玩家
) {
    private var taskCancelled = false

    fun cancelTask() {
        taskCancelled = true
    }

    fun isTaskCancelled(): Boolean = taskCancelled
}

