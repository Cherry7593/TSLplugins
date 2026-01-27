package org.tsl.tSLplugins.modules.xconomytrigger

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * 玩家触发状态
 */
enum class TriggerState {
    /** 余额在正常区间 */
    NORMAL,
    /** 已触发低余额命令 */
    LOW_FIRED,
    /** 已触发高余额命令 */
    HIGH_FIRED
}

/**
 * 玩家状态数据
 */
data class PlayerTriggerData(
    var state: TriggerState = TriggerState.NORMAL,
    var lastTriggerTime: Long = 0L
)

/**
 * XConomy 余额触发器管理器
 * 监控玩家余额并在达到阈值时执行控制台命令
 */
class XconomyTriggerManager(private val plugin: JavaPlugin) {

    // 配置项
    private var enabled = false
    private var scanIntervalSeconds = 60L
    private var hysteresis = 100.0
    private var playerCooldownSeconds = 300L

    // 低余额触发配置
    private var lowBalanceEnabled = true
    private var lowBalanceThreshold = 1000.0
    private var lowBalanceCommands = listOf<String>()

    // 高余额触发配置
    private var highBalanceEnabled = true
    private var highBalanceThreshold = 100000.0
    private var highBalanceCommands = listOf<String>()

    // 玩家状态追踪（线程安全）
    private val playerStates = ConcurrentHashMap<UUID, PlayerTriggerData>()

    // 异步扫描任务
    private var scanTask: ScheduledTask? = null

    // XConomy API 封装
    private val xconomyApi = XconomyApi(plugin)

    /**
     * 加载配置
     */
    fun loadConfig() {
        val config = plugin.config
        val section = config.getConfigurationSection("xconomy-trigger")

        if (section == null) {
            enabled = false
            plugin.logger.info("[XconomyTrigger] 配置节不存在，模块已禁用")
            return
        }

        enabled = section.getBoolean("enabled", false)
        scanIntervalSeconds = section.getLong("scan-interval-seconds", 60L)
        hysteresis = section.getDouble("hysteresis", 100.0)
        playerCooldownSeconds = section.getLong("player-cooldown-seconds", 300L)

        // 低余额配置
        val lowSection = section.getConfigurationSection("low-balance")
        if (lowSection != null) {
            lowBalanceEnabled = lowSection.getBoolean("enabled", true)
            lowBalanceThreshold = lowSection.getDouble("threshold", 1000.0)
            lowBalanceCommands = lowSection.getStringList("commands")
        }

        // 高余额配置
        val highSection = section.getConfigurationSection("high-balance")
        if (highSection != null) {
            highBalanceEnabled = highSection.getBoolean("enabled", true)
            highBalanceThreshold = highSection.getDouble("threshold", 100000.0)
            highBalanceCommands = highSection.getStringList("commands")
        }

        plugin.logger.info("[XconomyTrigger] 配置已加载 - 启用: $enabled, 扫描间隔: ${scanIntervalSeconds}s")
    }

    /**
     * 初始化模块
     */
    fun initialize() {
        loadConfig()

        if (!enabled) {
            plugin.logger.info("[XconomyTrigger] 模块未启用")
            return
        }

        // 检查 XConomy 是否可用
        if (!xconomyApi.isAvailable()) {
            enabled = false
            plugin.logger.warning("[XconomyTrigger] XConomy 未安装或不可用，模块已禁用")
            return
        }

        startScanTask()
        plugin.logger.info("[XconomyTrigger] 模块已初始化")
        plugin.logger.info("[XconomyTrigger] 低余额阈值: $lowBalanceThreshold, 高余额阈值: $highBalanceThreshold")
    }

    /**
     * 启动异步扫描任务
     */
    private fun startScanTask() {
        stopScanTask()

        scanTask = Bukkit.getAsyncScheduler().runAtFixedRate(
            plugin,
            { _ -> scanOnlinePlayers() },
            scanIntervalSeconds,
            scanIntervalSeconds,
            TimeUnit.SECONDS
        )

        plugin.logger.info("[XconomyTrigger] 扫描任务已启动，间隔: ${scanIntervalSeconds}s")
    }

    /**
     * 停止扫描任务
     */
    private fun stopScanTask() {
        scanTask?.cancel()
        scanTask = null
    }

    /**
     * 扫描所有在线玩家余额
     */
    private fun scanOnlinePlayers() {
        if (!enabled) return

        val players = Bukkit.getOnlinePlayers()
        for (player in players) {
            try {
                checkPlayerBalance(player)
            } catch (e: Exception) {
                plugin.logger.warning("[XconomyTrigger] 检查玩家 ${player.name} 余额时出错: ${e.message}")
            }
        }
    }

    /**
     * 检查单个玩家余额并处理触发逻辑
     */
    private fun checkPlayerBalance(player: Player) {
        val uuid = player.uniqueId
        val balance = xconomyApi.getBalance(player) ?: return

        val data = playerStates.getOrPut(uuid) { PlayerTriggerData() }
        val now = System.currentTimeMillis()
        val cooldownMs = playerCooldownSeconds * 1000

        when (data.state) {
            TriggerState.NORMAL -> {
                // 检查是否需要触发低余额命令
                if (lowBalanceEnabled && balance < lowBalanceThreshold) {
                    if (now - data.lastTriggerTime >= cooldownMs) {
                        data.state = TriggerState.LOW_FIRED
                        data.lastTriggerTime = now
                        executeCommands(lowBalanceCommands, player, balance)
                        plugin.logger.info("[XconomyTrigger] 玩家 ${player.name} 触发低余额命令 (余额: $balance)")
                    }
                }
                // 检查是否需要触发高余额命令
                else if (highBalanceEnabled && balance > highBalanceThreshold) {
                    if (now - data.lastTriggerTime >= cooldownMs) {
                        data.state = TriggerState.HIGH_FIRED
                        data.lastTriggerTime = now
                        executeCommands(highBalanceCommands, player, balance)
                        plugin.logger.info("[XconomyTrigger] 玩家 ${player.name} 触发高余额命令 (余额: $balance)")
                    }
                }
            }

            TriggerState.LOW_FIRED -> {
                // 检查是否恢复正常（需要超过阈值 + 回差）
                if (balance >= lowBalanceThreshold + hysteresis) {
                    data.state = TriggerState.NORMAL
                    plugin.logger.fine("[XconomyTrigger] 玩家 ${player.name} 余额恢复正常 (余额: $balance)")
                }
            }

            TriggerState.HIGH_FIRED -> {
                // 检查是否恢复正常（需要低于阈值 - 回差）
                if (balance <= highBalanceThreshold - hysteresis) {
                    data.state = TriggerState.NORMAL
                    plugin.logger.fine("[XconomyTrigger] 玩家 ${player.name} 余额恢复正常 (余额: $balance)")
                }
            }
        }
    }

    /**
     * 执行命令列表
     * 替换占位符并在 GlobalRegionScheduler 上执行
     */
    private fun executeCommands(commands: List<String>, player: Player, balance: Double) {
        if (commands.isEmpty()) return

        Bukkit.getGlobalRegionScheduler().run(plugin) { _ ->
            for (cmd in commands) {
                try {
                    val processedCmd = cmd
                        .replace("%player%", player.name)
                        .replace("%uuid%", player.uniqueId.toString())
                        .replace("%balance%", String.format("%.2f", balance))

                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedCmd)
                    plugin.logger.info("[XconomyTrigger] 执行命令: $processedCmd")
                } catch (e: Exception) {
                    plugin.logger.warning("[XconomyTrigger] 命令执行失败: $cmd - ${e.message}")
                }
            }
        }
    }

    /**
     * 重新加载配置
     */
    fun reload() {
        stopScanTask()
        playerStates.clear()
        loadConfig()

        if (enabled && xconomyApi.isAvailable()) {
            startScanTask()
            plugin.logger.info("[XconomyTrigger] 模块已重新加载")
        } else if (enabled) {
            enabled = false
            plugin.logger.warning("[XconomyTrigger] XConomy 不可用，模块已禁用")
        }
    }

    /**
     * 关闭模块
     */
    fun shutdown() {
        stopScanTask()
        playerStates.clear()
        plugin.logger.info("[XconomyTrigger] 模块已关闭")
    }

    /**
     * 清理玩家数据（玩家退出时调用）
     */
    fun onPlayerQuit(uuid: UUID) {
        playerStates.remove(uuid)
    }

    /**
     * 检查模块是否启用
     */
    fun isEnabled(): Boolean = enabled

    /**
     * 获取玩家当前状态
     */
    fun getPlayerState(uuid: UUID): TriggerState? = playerStates[uuid]?.state
}
