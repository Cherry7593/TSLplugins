package org.tsl.tSLplugins.PlayerCountCmd

import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.java.JavaPlugin
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * 命令执行状态
 */
enum class CmdState {
    /** 已执行高人数命令 */
    HIGH,
    /** 已执行低人数命令 */
    LOW
}

/**
 * 人数控制命令模块
 * 根据在线人数自动执行控制台命令
 *
 * 适用场景：
 * - Chunky 预加载控制（chunky pause / continue）
 * - Bluemap 渲染控制（bluemap start / stop）
 * - 视距调整
 * - 其他需要根据人数控制的功能
 *
 * 特性：
 * - 事件驱动（不轮询）
 * - 状态机 + 回差逻辑防抖
 * - Folia 线程安全
 * - 配置化
 *
 * @param plugin 插件实例
 * @param upperThreshold 上阈值：在线人数 >= 此值时执行高人数命令
 * @param lowerThreshold 下阈值：在线人数 <= 此值时执行低人数命令
 * @param minIntervalMs 最小执行间隔（毫秒）
 * @param cmdWhenLow 人数低时执行的命令
 * @param cmdWhenHigh 人数高时执行的命令
 */
class PlayerCountCmdController(
    private val plugin: JavaPlugin,
    private val upperThreshold: Int,
    private val lowerThreshold: Int,
    private val minIntervalMs: Long,
    private val cmdWhenLow: String,
    private val cmdWhenHigh: String
) : Listener {

    /** 当前状态（线程安全） */
    private val currentState = AtomicReference(CmdState.LOW)

    /** 上次执行命令的时间（毫秒，线程安全） */
    private val lastExecTime = AtomicLong(0L)

    /**
     * 初始化控制器
     * - 注册事件监听器
     * - 根据当前在线人数确定初始状态
     */
    fun init() {
        // 注册事件监听器
        Bukkit.getPluginManager().registerEvents(this, plugin)

        // 初始化时检查当前状态（使用异步调度，避免阻塞 onEnable）
        Bukkit.getAsyncScheduler().runNow(plugin) { _ ->
            val onlineCount = Bukkit.getOnlinePlayers().size
            initializeState(onlineCount)
        }

        plugin.logger.info("[PlayerCountCmd] 已初始化 - 上阈值: $upperThreshold, 下阈值: $lowerThreshold, 最小间隔: ${minIntervalMs}ms")
    }

    /**
     * 初始化状态（仅在启动时调用一次）
     * 不执行命令，仅设置初始状态
     */
    private fun initializeState(onlineCount: Int) {
        val initialState = when {
            onlineCount >= upperThreshold -> CmdState.HIGH
            onlineCount <= lowerThreshold -> CmdState.LOW
            else -> CmdState.LOW  // 在中间区域默认为 LOW
        }
        currentState.set(initialState)
        plugin.logger.info("[PlayerCountCmd] 初始状态: $initialState (在线: $onlineCount)")
    }

    /**
     * 玩家加入事件
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        scheduleCheck()
    }

    /**
     * 玩家退出事件
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        // 退出时人数计算需要 -1（因为事件触发时玩家还在列表中）
        scheduleCheck(adjustCount = -1)
    }

    /**
     * 调度检查任务
     * 使用 AsyncScheduler 避免阻塞事件线程
     *
     * @param adjustCount 人数调整值（用于 quit 事件）
     */
    private fun scheduleCheck(adjustCount: Int = 0) {
        Bukkit.getAsyncScheduler().runNow(plugin) { _ ->
            checkAndUpdate(adjustCount)
        }
    }

    /**
     * 核心逻辑：检查在线人数并更新状态
     *
     * @param adjustCount 人数调整值
     */
    private fun checkAndUpdate(adjustCount: Int = 0) {
        val onlineCount = Bukkit.getOnlinePlayers().size + adjustCount
        val current = currentState.get()
        val now = System.currentTimeMillis()

        // 确定目标状态（带回差逻辑）
        val targetState: CmdState? = when {
            // 人数 >= 上阈值 → 应该执行高人数命令
            onlineCount >= upperThreshold && current == CmdState.LOW -> CmdState.HIGH
            // 人数 <= 下阈值 → 应该执行低人数命令
            onlineCount <= lowerThreshold && current == CmdState.HIGH -> CmdState.LOW
            // 在中间区域，保持当前状态（回差逻辑）
            else -> null
        }

        // 如果不需要切换状态，直接返回
        if (targetState == null) {
            return
        }

        // 检查最小间隔
        val lastTime = lastExecTime.get()
        if (now - lastTime < minIntervalMs) {
            plugin.logger.fine("[PlayerCountCmd] 距上次执行不足 ${minIntervalMs}ms，跳过")
            return
        }

        // 尝试更新状态（CAS 保证线程安全）
        if (!currentState.compareAndSet(current, targetState)) {
            // 状态已被其他线程更新，跳过
            return
        }

        // 更新执行时间
        lastExecTime.set(now)

        // 执行对应命令
        val command = when (targetState) {
            CmdState.HIGH -> cmdWhenHigh
            CmdState.LOW -> cmdWhenLow
        }

        executeCommand(command, onlineCount, targetState)
    }

    /**
     * 执行控制台命令
     * 必须使用 GlobalRegionScheduler（Folia 要求）
     */
    private fun executeCommand(command: String, onlineCount: Int, newState: CmdState) {
        Bukkit.getGlobalRegionScheduler().run(plugin) { _ ->
            try {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)
                plugin.logger.info("[PlayerCountCmd] 执行命令: $command (在线: $onlineCount, 状态: $newState)")
            } catch (e: Exception) {
                plugin.logger.warning("[PlayerCountCmd] 命令执行失败: $command - ${e.message}")
            }
        }
    }

    /**
     * 获取当前状态
     */
    fun getCurrentState(): CmdState = currentState.get()

    /**
     * 获取上阈值
     */
    fun getUpperThreshold(): Int = upperThreshold

    /**
     * 获取下阈值
     */
    fun getLowerThreshold(): Int = lowerThreshold
}

