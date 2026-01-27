package org.tsl.tSLplugins.modules.phantom

import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Statistic
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.tsl.tSLplugins.SubCommandHandler
import org.tsl.tSLplugins.service.TSLPlayerProfileStore
import org.tsl.tSLplugins.core.AbstractModule

/**
 * Phantom 模块 - 幻翼控制
 * 
 * 通过定期重置 TIME_SINCE_REST 统计来控制幻翼是否出现
 * 
 * ## 功能
 * - 允许/禁止幻翼骚扰
 * - 定时检查并重置统计
 * 
 * ## 命令
 * - `/tsl phantom on` - 允许幻翼骚扰
 * - `/tsl phantom off` - 禁止幻翼骚扰
 * - `/tsl phantom status` - 查看当前状态
 * 
 * ## 权限
 * - `tsl.phantom.toggle` - 切换幻翼状态
 */
class PhantomModule : AbstractModule() {

    override val id = "phantom"
    override val configPath = "phantom"

    // 配置项
    private var checkInterval: Long = 300L  // 检查间隔（秒）

    // 外部依赖
    private var profileStore: TSLPlayerProfileStore? = null

    // 定时任务引用
    private var scheduledTask: ScheduledTask? = null

    override fun doEnable() {
        loadPhantomConfig()
        
        // 从 context 获取 profileStore
        profileStore = context.playerDataManager.getProfileStore()
        
        startTask()
        
        // 热重载时立即处理所有在线玩家，确保禁用幻翼的玩家统计被重置
        Bukkit.getGlobalRegionScheduler().runDelayed(context.plugin, { _ ->
            processAllPlayers()
        }, 20L)  // 延迟 1 秒确保 ProfileStore 已初始化
    }

    override fun doDisable() {
        stopTask()
    }

    override fun doReload() {
        loadPhantomConfig()
        // 确保 profileStore 已初始化
        if (profileStore == null) {
            profileStore = context.playerDataManager.getProfileStore()
        }
        // 重启任务以应用新配置
        stopTask()
        startTask()
        
        // 热重载时立即处理所有在线玩家，确保禁用幻翼的玩家统计被重置
        Bukkit.getGlobalRegionScheduler().runDelayed(context.plugin, { _ ->
            processAllPlayers()
        }, 20L)
    }

    override fun getCommandHandler(): SubCommandHandler = PhantomModuleCommand(this)
    
    override fun getDescription(): String = "幻翼控制"

    /**
     * 加载配置
     */
    private fun loadPhantomConfig() {
        checkInterval = getConfigLong("checkInterval", 300L)
    }

    /**
     * 启动定时任务
     */
    private fun startTask() {
        stopTask()

        if (!isEnabled()) {
            logInfo("功能未启用，跳过启动定时任务")
            return
        }

        val intervalTicks = checkInterval * 20L

        scheduledTask = Bukkit.getGlobalRegionScheduler().runAtFixedRate(context.plugin, { _ ->
            processAllPlayers()
        }, intervalTicks, intervalTicks)

        logInfo("定时任务已启动 - 间隔: $checkInterval 秒")
    }

    /**
     * 停止定时任务
     */
    private fun stopTask() {
        scheduledTask?.cancel()
        scheduledTask = null
    }

    /**
     * 处理所有在线玩家
     */
    private fun processAllPlayers() {
        val store = profileStore
        if (store == null) {
            logWarning("ProfileStore 未初始化，跳过处理")
            return
        }

        try {
            val onlinePlayers = Bukkit.getOnlinePlayers()

            onlinePlayers.forEach { player ->
                player.scheduler.run(context.plugin, { _ ->
                    try {
                        processPlayer(player, store)
                    } catch (e: Exception) {
                        logWarning("处理玩家失败: ${player.name} - ${e.message}")
                    }
                }, null)
            }
        } catch (e: Exception) {
            logSevere("定时任务执行失败: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * 处理单个玩家
     */
    private fun processPlayer(player: Player, store: TSLPlayerProfileStore) {
        val profile = store.get(player.uniqueId) ?: return

        if (!profile.allowPhantom) {
            player.setStatistic(Statistic.TIME_SINCE_REST, 0)
        }
    }

    // ============== 公开 API ==============

    /**
     * 获取玩家的幻翼允许状态
     */
    fun isPhantomAllowed(player: Player): Boolean {
        val store = profileStore ?: return false
        return store.get(player.uniqueId)?.allowPhantom ?: false
    }

    /**
     * 设置玩家的幻翼允许状态
     */
    fun setPhantomAllowed(player: Player, allowed: Boolean) {
        val store = profileStore
        if (store == null) {
            logWarning("ProfileStore 未初始化")
            return
        }

        try {
            val profile = store.getOrCreate(player.uniqueId, player.name)
            profile.allowPhantom = allowed

            if (!allowed) {
                player.scheduler.run(context.plugin, { _ ->
                    try {
                        player.setStatistic(Statistic.TIME_SINCE_REST, 0)
                    } catch (e: Exception) {
                        logWarning("重置统计失败: ${e.message}")
                    }
                }, null)
            }
        } catch (e: Exception) {
            logWarning("设置幻翼状态失败: ${e.message}")
        }
    }
}

/**
 * Phantom 命令处理器
 * 
 * 支持玩家自行切换和管理员修改他人状态
 */
class PhantomModuleCommand(private val module: PhantomModule) : SubCommandHandler {

    override fun handle(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (!module.isEnabled()) {
            sender.sendMessage(Component.text("幻翼控制功能未启用！", NamedTextColor.RED))
            return true
        }

        if (args.isEmpty()) {
            showHelp(sender)
            return true
        }

        val subCommand = args[0].lowercase()
        val targetPlayerName = args.getOrNull(1)

        // 如果指定了目标玩家，检查管理员权限
        if (targetPlayerName != null) {
            if (!sender.hasPermission("tsl.phantom.admin")) {
                sender.sendMessage(Component.text("你没有权限修改其他玩家的幻翼状态！", NamedTextColor.RED))
                return true
            }

            val targetPlayer = Bukkit.getPlayer(targetPlayerName)
            if (targetPlayer == null) {
                sender.sendMessage(Component.text("玩家 $targetPlayerName 不在线！", NamedTextColor.RED))
                return true
            }

            when (subCommand) {
                "on" -> handleOnAdmin(sender, targetPlayer)
                "off" -> handleOffAdmin(sender, targetPlayer)
                "status" -> handleStatusAdmin(sender, targetPlayer)
                else -> showHelp(sender)
            }
        } else {
            // 无目标玩家，修改自己
            if (sender !is Player) {
                sender.sendMessage(Component.text("控制台请指定目标玩家！用法: /tsl phantom on|off|status <玩家名>", NamedTextColor.RED))
                return true
            }

            if (!sender.hasPermission("tsl.phantom.toggle")) {
                sender.sendMessage(Component.text("你没有权限使用此命令！", NamedTextColor.RED))
                return true
            }

            when (subCommand) {
                "on" -> handleOn(sender)
                "off" -> handleOff(sender)
                "status" -> handleStatus(sender)
                else -> showHelp(sender)
            }
        }

        return true
    }

    // ============== 玩家自行操作 ==============

    private fun handleOn(sender: Player) {
        if (module.isPhantomAllowed(sender)) {
            sender.sendMessage(Component.text("幻翼骚扰已经是开启状态！", NamedTextColor.YELLOW))
            return
        }

        module.setPhantomAllowed(sender, true)
        sender.sendMessage(
            Component.text("✓ ", NamedTextColor.GREEN)
                .append(Component.text("已允许幻翼骚扰", NamedTextColor.YELLOW))
        )
        sender.sendMessage(Component.text("  长时间不睡觉会出现幻翼", NamedTextColor.GRAY))
    }

    private fun handleOff(sender: Player) {
        if (!module.isPhantomAllowed(sender)) {
            sender.sendMessage(Component.text("幻翼骚扰已经是关闭状态！", NamedTextColor.YELLOW))
            return
        }

        module.setPhantomAllowed(sender, false)
        sender.sendMessage(
            Component.text("✓ ", NamedTextColor.GREEN)
                .append(Component.text("已禁止幻翼骚扰", NamedTextColor.YELLOW))
        )
        sender.sendMessage(Component.text("  幻翼将不会出现", NamedTextColor.GRAY))
    }

    private fun handleStatus(sender: Player) {
        val currentState = module.isPhantomAllowed(sender)

        sender.sendMessage(
            Component.text("========== ", NamedTextColor.GRAY)
                .append(Component.text("幻翼控制状态", NamedTextColor.GOLD))
                .append(Component.text(" ==========", NamedTextColor.GRAY))
        )

        if (currentState) {
            sender.sendMessage(
                Component.text("当前状态: ", NamedTextColor.GRAY)
                    .append(Component.text("允许", NamedTextColor.GREEN))
            )
            sender.sendMessage(Component.text("  长时间不睡觉会出现幻翼", NamedTextColor.GRAY))
        } else {
            sender.sendMessage(
                Component.text("当前状态: ", NamedTextColor.GRAY)
                    .append(Component.text("禁止", NamedTextColor.RED))
            )
            sender.sendMessage(Component.text("  幻翼不会出现", NamedTextColor.GRAY))
        }
    }

    // ============== 管理员操作 ==============

    private fun handleOnAdmin(sender: CommandSender, target: Player) {
        if (module.isPhantomAllowed(target)) {
            sender.sendMessage(Component.text("玩家 ${target.name} 的幻翼骚扰已经是开启状态！", NamedTextColor.YELLOW))
            return
        }

        module.setPhantomAllowed(target, true)
        
        // 通知管理员
        sender.sendMessage(
            Component.text("✓ ", NamedTextColor.GREEN)
                .append(Component.text("已允许玩家 ", NamedTextColor.YELLOW))
                .append(Component.text(target.name, NamedTextColor.AQUA))
                .append(Component.text(" 的幻翼骚扰", NamedTextColor.YELLOW))
        )
        
        // 通知目标玩家（如果不是自己操作）
        if (sender != target) {
            target.sendMessage(
                Component.text("✓ ", NamedTextColor.GREEN)
                    .append(Component.text("管理员已允许你的幻翼骚扰", NamedTextColor.YELLOW))
            )
            target.sendMessage(Component.text("  长时间不睡觉会出现幻翼", NamedTextColor.GRAY))
        }
    }

    private fun handleOffAdmin(sender: CommandSender, target: Player) {
        if (!module.isPhantomAllowed(target)) {
            sender.sendMessage(Component.text("玩家 ${target.name} 的幻翼骚扰已经是关闭状态！", NamedTextColor.YELLOW))
            return
        }

        module.setPhantomAllowed(target, false)
        
        // 通知管理员
        sender.sendMessage(
            Component.text("✓ ", NamedTextColor.GREEN)
                .append(Component.text("已禁止玩家 ", NamedTextColor.YELLOW))
                .append(Component.text(target.name, NamedTextColor.AQUA))
                .append(Component.text(" 的幻翼骚扰", NamedTextColor.YELLOW))
        )
        
        // 通知目标玩家（如果不是自己操作）
        if (sender != target) {
            target.sendMessage(
                Component.text("✓ ", NamedTextColor.GREEN)
                    .append(Component.text("管理员已禁止你的幻翼骚扰", NamedTextColor.YELLOW))
            )
            target.sendMessage(Component.text("  幻翼将不会出现", NamedTextColor.GRAY))
        }
    }

    private fun handleStatusAdmin(sender: CommandSender, target: Player) {
        val currentState = module.isPhantomAllowed(target)

        sender.sendMessage(
            Component.text("========== ", NamedTextColor.GRAY)
                .append(Component.text("${target.name} 的幻翼状态", NamedTextColor.GOLD))
                .append(Component.text(" ==========", NamedTextColor.GRAY))
        )

        if (currentState) {
            sender.sendMessage(
                Component.text("当前状态: ", NamedTextColor.GRAY)
                    .append(Component.text("允许", NamedTextColor.GREEN))
            )
        } else {
            sender.sendMessage(
                Component.text("当前状态: ", NamedTextColor.GRAY)
                    .append(Component.text("禁止", NamedTextColor.RED))
            )
        }
    }

    // ============== 帮助和补全 ==============

    private fun showHelp(sender: CommandSender) {
        sender.sendMessage(
            Component.text("========== ", NamedTextColor.GRAY)
                .append(Component.text("幻翼控制命令", NamedTextColor.GOLD))
                .append(Component.text(" ==========", NamedTextColor.GRAY))
        )
        sender.sendMessage(
            Component.text("/tsl phantom on", NamedTextColor.AQUA)
                .append(Component.text(" - 允许幻翼骚扰", NamedTextColor.GRAY))
        )
        sender.sendMessage(
            Component.text("/tsl phantom off", NamedTextColor.AQUA)
                .append(Component.text(" - 禁止幻翼骚扰", NamedTextColor.GRAY))
        )
        sender.sendMessage(
            Component.text("/tsl phantom status", NamedTextColor.AQUA)
                .append(Component.text(" - 查看当前状态", NamedTextColor.GRAY))
        )
        
        // 如果有管理员权限，显示管理员命令
        if (sender.hasPermission("tsl.phantom.admin")) {
            sender.sendMessage(Component.text("--- 管理员命令 ---", NamedTextColor.GOLD))
            sender.sendMessage(
                Component.text("/tsl phantom on <玩家>", NamedTextColor.AQUA)
                    .append(Component.text(" - 允许指定玩家的幻翼", NamedTextColor.GRAY))
            )
            sender.sendMessage(
                Component.text("/tsl phantom off <玩家>", NamedTextColor.AQUA)
                    .append(Component.text(" - 禁止指定玩家的幻翼", NamedTextColor.GRAY))
            )
            sender.sendMessage(
                Component.text("/tsl phantom status <玩家>", NamedTextColor.AQUA)
                    .append(Component.text(" - 查看指定玩家状态", NamedTextColor.GRAY))
            )
        }
    }

    override fun tabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): List<String> {
        if (!sender.hasPermission("tsl.phantom.toggle") && !sender.hasPermission("tsl.phantom.admin")) {
            return emptyList()
        }

        return when (args.size) {
            1 -> listOf("on", "off", "status").filter { it.startsWith(args[0], ignoreCase = true) }
            2 -> {
                // 只有管理员才补全玩家名
                if (sender.hasPermission("tsl.phantom.admin")) {
                    Bukkit.getOnlinePlayers()
                        .map { it.name }
                        .filter { it.startsWith(args[1], ignoreCase = true) }
                } else {
                    emptyList()
                }
            }
            else -> emptyList()
        }
    }

    override fun getDescription(): String = "幻翼控制"
}
