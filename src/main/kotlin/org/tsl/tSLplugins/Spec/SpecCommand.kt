package org.tsl.tSLplugins.Spec

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.tsl.tSLplugins.SubCommandHandler

/**
 * Spec 命令处理器
 * /tsl spec start [延迟] - 开始循环观看
 * /tsl spec stop - 停止观看
 * /tsl spec add <player> - 添加到白名单
 * /tsl spec remove <player> - 从白名单移除
 * /tsl spec list - 查看白名单
 * /tsl spec reload - 重载配置
 */
class SpecCommand(private val manager: SpecManager) : SubCommandHandler {

    override fun handle(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        // 检查功能是否启用
        if (!manager.isEnabled()) {
            sender.sendMessage(Component.text("观众模式功能未启用！", NamedTextColor.RED))
            return true
        }

        // 权限检查
        if (!sender.hasPermission("tsl.spec.use")) {
            sender.sendMessage(Component.text("你没有权限使用此命令！", NamedTextColor.RED))
            return true
        }

        // 没有参数，显示帮助
        if (args.isEmpty()) {
            showHelp(sender)
            return true
        }

        // 处理子命令
        return when (args[0].lowercase()) {
            "start" -> handleStart(sender, args)
            "stop" -> handleStop(sender)
            "add" -> handleAdd(sender, args)
            "remove" -> handleRemove(sender, args)
            "list" -> handleList(sender)
            "reload" -> handleReload(sender)
            else -> {
                showHelp(sender)
                true
            }
        }
    }

    /**
     * 处理 start 命令
     */
    private fun handleStart(sender: CommandSender, args: Array<out String>): Boolean {
        // 只允许玩家使用
        if (sender !is Player) {
            sender.sendMessage(Component.text("此命令只能由玩家使用！", NamedTextColor.RED))
            return true
        }

        // 解析延迟参数
        val delay = if (args.size >= 2) {
            try {
                args[1].toInt()
            } catch (e: NumberFormatException) {
                sender.sendMessage(Component.text("无效的延迟时间！请输入数字。", NamedTextColor.RED))
                return true
            }
        } else {
            manager.getDefaultDelay()
        }

        // 验证延迟
        val validDelay = manager.validateDelay(delay)
        if (validDelay != delay) {
            sender.sendMessage(
                Component.text("延迟已调整为 $validDelay 秒", NamedTextColor.YELLOW)
            )
        }

        // 开始观看
        val success = manager.startSpectating(sender, validDelay)

        if (success) {
            sender.sendMessage(
                Component.text("✓ ", NamedTextColor.GREEN)
                    .append(Component.text("已开始循环观看模式", NamedTextColor.YELLOW))
            )
            sender.sendMessage(
                Component.text("  每 $validDelay 秒切换下一个玩家", NamedTextColor.GRAY)
            )
            sender.sendMessage(
                Component.text("  使用 /tsl spec stop 停止观看", NamedTextColor.GRAY)
            )
        } else {
            sender.sendMessage(Component.text("你已经在观看模式中！", NamedTextColor.RED))
        }

        return true
    }

    /**
     * 处理 stop 命令
     */
    private fun handleStop(sender: CommandSender): Boolean {
        // 只允许玩家使用
        if (sender !is Player) {
            sender.sendMessage(Component.text("此命令只能由玩家使用！", NamedTextColor.RED))
            return true
        }

        // 停止观看
        val success = manager.stopSpectating(sender)

        if (success) {
            sender.sendMessage(
                Component.text("✓ ", NamedTextColor.GREEN)
                    .append(Component.text("已停止观看模式", NamedTextColor.YELLOW))
            )
        } else {
            sender.sendMessage(Component.text("你没有在观看模式中！", NamedTextColor.RED))
        }

        return true
    }

    /**
     * 处理 add 命令
     */
    private fun handleAdd(sender: CommandSender, args: Array<out String>): Boolean {
        if (args.size < 2) {
            sender.sendMessage(Component.text("用法: /tsl spec add <玩家名>", NamedTextColor.RED))
            return true
        }

        // 查找玩家
        @Suppress("DEPRECATION")
        val target = Bukkit.getOfflinePlayer(args[1])

        if (!target.hasPlayedBefore() && !target.isOnline) {
            sender.sendMessage(Component.text("找不到玩家: ${args[1]}", NamedTextColor.RED))
            return true
        }

        // 添加到白名单
        val added = manager.addToWhitelist(target.uniqueId)

        if (added) {
            sender.sendMessage(
                Component.text("✓ ", NamedTextColor.GREEN)
                    .append(Component.text("已将 ", NamedTextColor.YELLOW))
                    .append(Component.text(target.name ?: args[1], NamedTextColor.WHITE))
                    .append(Component.text(" 添加到白名单", NamedTextColor.YELLOW))
            )
        } else {
            sender.sendMessage(Component.text("该玩家已在白名单中！", NamedTextColor.RED))
        }

        return true
    }

    /**
     * 处理 remove 命令
     */
    private fun handleRemove(sender: CommandSender, args: Array<out String>): Boolean {
        if (args.size < 2) {
            sender.sendMessage(Component.text("用法: /tsl spec remove <玩家名>", NamedTextColor.RED))
            return true
        }

        // 查找玩家
        @Suppress("DEPRECATION")
        val target = Bukkit.getOfflinePlayer(args[1])

        if (!target.hasPlayedBefore() && !target.isOnline) {
            sender.sendMessage(Component.text("找不到玩家: ${args[1]}", NamedTextColor.RED))
            return true
        }

        // 从白名单移除
        val removed = manager.removeFromWhitelist(target.uniqueId)

        if (removed) {
            sender.sendMessage(
                Component.text("✓ ", NamedTextColor.GREEN)
                    .append(Component.text("已将 ", NamedTextColor.YELLOW))
                    .append(Component.text(target.name ?: args[1], NamedTextColor.WHITE))
                    .append(Component.text(" 从白名单移除", NamedTextColor.YELLOW))
            )
        } else {
            sender.sendMessage(Component.text("该玩家不在白名单中！", NamedTextColor.RED))
        }

        return true
    }

    /**
     * 处理 list 命令
     */
    private fun handleList(sender: CommandSender): Boolean {
        val whitelist = manager.getWhitelist()

        if (whitelist.isEmpty()) {
            sender.sendMessage(Component.text("白名单为空", NamedTextColor.YELLOW))
            return true
        }

        sender.sendMessage(
            Component.text("========== ", NamedTextColor.GRAY)
                .append(Component.text("观看白名单", NamedTextColor.GOLD))
                .append(Component.text(" ==========", NamedTextColor.GRAY))
        )

        whitelist.forEachIndexed { index, uuid ->
            @Suppress("DEPRECATION")
            val player = Bukkit.getOfflinePlayer(uuid)
            val playerName = player.name ?: uuid.toString()

            sender.sendMessage(
                Component.text("${index + 1}. ", NamedTextColor.GRAY)
                    .append(Component.text(playerName, NamedTextColor.WHITE))
            )
        }

        sender.sendMessage(
            Component.text("共 ${whitelist.size} 名玩家", NamedTextColor.GRAY)
        )

        return true
    }

    /**
     * 处理 reload 命令
     */
    private fun handleReload(sender: CommandSender): Boolean {
        try {
            manager.loadConfig()
            sender.sendMessage(
                Component.text("✓ ", NamedTextColor.GREEN)
                    .append(Component.text("配置已重载", NamedTextColor.YELLOW))
            )
        } catch (e: Exception) {
            sender.sendMessage(Component.text("重载配置失败: ${e.message}", NamedTextColor.RED))
        }

        return true
    }

    /**
     * 显示帮助信息
     */
    private fun showHelp(sender: CommandSender) {
        sender.sendMessage(
            Component.text("========== ", NamedTextColor.GRAY)
                .append(Component.text("观众模式命令", NamedTextColor.GOLD))
                .append(Component.text(" ==========", NamedTextColor.GRAY))
        )
        sender.sendMessage(
            Component.text("/tsl spec start [延迟]", NamedTextColor.AQUA)
                .append(Component.text(" - 开始循环观看", NamedTextColor.GRAY))
        )
        sender.sendMessage(
            Component.text("/tsl spec stop", NamedTextColor.AQUA)
                .append(Component.text(" - 停止观看", NamedTextColor.GRAY))
        )
        sender.sendMessage(
            Component.text("/tsl spec add <玩家>", NamedTextColor.AQUA)
                .append(Component.text(" - 添加到白名单", NamedTextColor.GRAY))
        )
        sender.sendMessage(
            Component.text("/tsl spec remove <玩家>", NamedTextColor.AQUA)
                .append(Component.text(" - 从白名单移除", NamedTextColor.GRAY))
        )
        sender.sendMessage(
            Component.text("/tsl spec list", NamedTextColor.AQUA)
                .append(Component.text(" - 查看白名单", NamedTextColor.GRAY))
        )
        sender.sendMessage(
            Component.text("/tsl spec reload", NamedTextColor.AQUA)
                .append(Component.text(" - 重载配置", NamedTextColor.GRAY))
        )
    }

    override fun tabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): List<String> {
        if (!sender.hasPermission("tsl.spec.use")) {
            return emptyList()
        }

        return when (args.size) {
            1 -> {
                // 第一个参数：子命令
                listOf("start", "stop", "add", "remove", "list", "reload")
                    .filter { it.startsWith(args[0], ignoreCase = true) }
            }
            2 -> {
                // 第二个参数
                when (args[0].lowercase()) {
                    "start" -> {
                        // 延迟提示
                        listOf("3", "5", "10", "15", "30")
                            .filter { it.startsWith(args[1]) }
                    }
                    "add", "remove" -> {
                        // 在线玩家名称
                        Bukkit.getOnlinePlayers()
                            .map { it.name }
                            .filter { it.startsWith(args[1], ignoreCase = true) }
                    }
                    else -> emptyList()
                }
            }
            else -> emptyList()
        }
    }

    override fun getDescription(): String {
        return "观众模式命令"
    }
}

