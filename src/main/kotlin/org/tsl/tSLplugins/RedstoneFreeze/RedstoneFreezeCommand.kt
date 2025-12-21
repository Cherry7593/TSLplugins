package org.tsl.tSLplugins.RedstoneFreeze

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.tsl.tSLplugins.SubCommandHandler

/**
 * 红石冻结命令处理器
 * 命令: /tsl redfreeze <radius> | cancel
 */
class RedstoneFreezeCommand(
    private val plugin: JavaPlugin,
    private val manager: RedstoneFreezeManager
) : SubCommandHandler {

    override fun handle(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!manager.isEnabled()) {
            sender.sendMessage(Component.text("红石冻结功能已禁用").color(NamedTextColor.RED))
            return true
        }

        if (args.isEmpty()) {
            sendUsage(sender)
            return true
        }

        when (args[0].lowercase()) {
            "cancel" -> handleCancel(sender)
            "info" -> handleInfo(sender)
            "update" -> handleUpdate(sender, args)
            else -> handleFreeze(sender, args[0])
        }

        return true
    }

    /**
     * 处理冻结命令
     */
    private fun handleFreeze(sender: CommandSender, radiusArg: String) {
        if (sender !is Player) {
            sender.sendMessage(Component.text("该命令只能由玩家执行").color(NamedTextColor.RED))
            return
        }

        if (!sender.hasPermission("tsl.redfreeze.use")) {
            sender.sendMessage(Component.text("你没有权限使用此命令").color(NamedTextColor.RED))
            return
        }

        val radius = radiusArg.toIntOrNull()
        if (radius == null || radius < 1) {
            sender.sendMessage(Component.text("请输入有效的半径（正整数）").color(NamedTextColor.RED))
            return
        }

        val maxRadius = manager.getMaxRadius()
        if (radius > maxRadius) {
            sender.sendMessage(Component.text("半径不能超过 $maxRadius").color(NamedTextColor.RED))
            return
        }

        // 使用玩家调度器确保在正确的区域线程执行
        sender.scheduler.run(plugin, { _ ->
            val frozenCount = manager.activateFreeze(sender, radius)
            sender.sendMessage(
                Component.text("已冻结 ").color(NamedTextColor.GREEN)
                    .append(Component.text("$frozenCount").color(NamedTextColor.AQUA))
                    .append(Component.text(" 个区块的红石活动").color(NamedTextColor.GREEN))
            )
        }, null)
    }

    /**
     * 处理取消冻结命令
     */
    private fun handleCancel(sender: CommandSender) {
        if (!sender.hasPermission("tsl.redfreeze.use")) {
            sender.sendMessage(Component.text("你没有权限使用此命令").color(NamedTextColor.RED))
            return
        }

        if (!manager.isFreezeActive()) {
            sender.sendMessage(Component.text("当前没有活跃的冻结区域").color(NamedTextColor.YELLOW))
            return
        }

        val releasedCount = manager.cancelFreeze()
        sender.sendMessage(
            Component.text("已取消冻结，释放 ").color(NamedTextColor.GREEN)
                .append(Component.text("$releasedCount").color(NamedTextColor.AQUA))
                .append(Component.text(" 个区块").color(NamedTextColor.GREEN))
        )
    }

    /**
     * 处理更新命令（触发活塞更新）
     */
    private fun handleUpdate(sender: CommandSender, args: Array<out String>) {
        if (sender !is Player) {
            sender.sendMessage(Component.text("该命令只能由玩家执行").color(NamedTextColor.RED))
            return
        }

        if (!sender.hasPermission("tsl.redfreeze.use")) {
            sender.sendMessage(Component.text("你没有权限使用此命令").color(NamedTextColor.RED))
            return
        }

        val radius = args.getOrNull(1)?.toIntOrNull() ?: 3
        val maxRadius = manager.getMaxRadius()
        if (radius < 1 || radius > maxRadius) {
            sender.sendMessage(Component.text("半径必须在 1-$maxRadius 之间").color(NamedTextColor.RED))
            return
        }

        sender.sendMessage(
            Component.text("正在更新半径 ").color(NamedTextColor.YELLOW)
                .append(Component.text("$radius").color(NamedTextColor.AQUA))
                .append(Component.text(" 区块内的活塞...").color(NamedTextColor.YELLOW))
        )

        manager.triggerPistonUpdates(sender, radius) { updatedCount ->
            sender.sendMessage(
                Component.text("已更新 ").color(NamedTextColor.GREEN)
                    .append(Component.text("$updatedCount").color(NamedTextColor.AQUA))
                    .append(Component.text(" 个活塞").color(NamedTextColor.GREEN))
            )
        }
    }

    /**
     * 显示当前冻结信息
     */
    private fun handleInfo(sender: CommandSender) {
        if (!sender.hasPermission("tsl.redfreeze.use")) {
            sender.sendMessage(Component.text("你没有权限使用此命令").color(NamedTextColor.RED))
            return
        }

        val info = manager.getFreezeInfo()
        if (info == null) {
            sender.sendMessage(Component.text("当前没有活跃的冻结区域").color(NamedTextColor.YELLOW))
        } else {
            sender.sendMessage(
                Component.text("当前冻结信息: ").color(NamedTextColor.GREEN)
                    .append(Component.text(info).color(NamedTextColor.AQUA))
            )
        }
    }

    /**
     * 发送用法提示
     */
    private fun sendUsage(sender: CommandSender) {
        sender.sendMessage(Component.text("用法:").color(NamedTextColor.YELLOW))
        sender.sendMessage(Component.text("  /tsl redfreeze <半径> - 冻结指定半径内的区块").color(NamedTextColor.GRAY))
        sender.sendMessage(Component.text("  /tsl redfreeze cancel - 取消冻结").color(NamedTextColor.GRAY))
        sender.sendMessage(Component.text("  /tsl redfreeze update [<半径>] - 触发活塞更新（默认3区块）").color(NamedTextColor.GRAY))
        sender.sendMessage(Component.text("  /tsl redfreeze info - 查看当前冻结信息").color(NamedTextColor.GRAY))
    }

    override fun tabComplete(sender: CommandSender, command: Command, label: String, args: Array<out String>): List<String> {
        if (args.size == 1) {
            val input = args[0].lowercase()
            val suggestions = mutableListOf("cancel", "info", "update", "1", "5", "10", "16", "32")
            return suggestions.filter { it.startsWith(input) }
        }
        if (args.size == 2 && args[0].lowercase() == "update") {
            val input = args[1]
            val suggestions = listOf("1", "3", "5", "8", "16")
            return suggestions.filter { it.startsWith(input) }
        }
        return emptyList()
    }

    override fun getDescription(): String = "冻结指定区域的红石活动"
}
