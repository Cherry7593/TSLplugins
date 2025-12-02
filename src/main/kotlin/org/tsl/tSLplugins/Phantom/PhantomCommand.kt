package org.tsl.tSLplugins.Phantom

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.tsl.tSLplugins.SubCommand

/**
 * Phantom 命令处理器
 * /tsl phantom on - 允许幻翼骚扰
 * /tsl phantom off - 禁止幻翼骚扰
 * /tsl phantom status - 查看当前状态
 */
class PhantomCommand(private val manager: PhantomManager) : SubCommand {

    override fun handle(sender: CommandSender, args: Array<out String>): Boolean {
        // 检查功能是否启用
        if (!manager.isEnabled()) {
            sender.sendMessage(Component.text("幻翼控制功能未启用！", NamedTextColor.RED))
            return true
        }

        // 只允许玩家使用
        if (sender !is Player) {
            sender.sendMessage(Component.text("此命令只能由玩家使用！", NamedTextColor.RED))
            return true
        }

        // 权限检查
        if (!sender.hasPermission("tsl.phantom.toggle")) {
            sender.sendMessage(Component.text("你没有权限使用此命令！", NamedTextColor.RED))
            return true
        }

        // 检查子命令
        if (args.isEmpty()) {
            showHelp(sender)
            return true
        }

        when (args[0].lowercase()) {
            "on" -> handleOn(sender)
            "off" -> handleOff(sender)
            "status" -> handleStatus(sender)
            else -> showHelp(sender)
        }

        return true
    }

    /**
     * 处理 on 命令 - 允许幻翼骚扰
     */
    private fun handleOn(sender: Player) {
        val currentState = manager.isPhantomAllowed(sender)

        if (currentState) {
            sender.sendMessage(Component.text("幻翼骚扰已经是开启状态！", NamedTextColor.YELLOW))
            return
        }

        manager.setPhantomAllowed(sender, true)

        sender.sendMessage(
            Component.text("✓ ", NamedTextColor.GREEN)
                .append(Component.text("已允许幻翼骚扰", NamedTextColor.YELLOW))
        )
        sender.sendMessage(
            Component.text("  长时间不睡觉会出现幻翼", NamedTextColor.GRAY)
        )
    }

    /**
     * 处理 off 命令 - 禁止幻翼骚扰
     */
    private fun handleOff(sender: Player) {
        val currentState = manager.isPhantomAllowed(sender)

        if (!currentState) {
            sender.sendMessage(Component.text("幻翼骚扰已经是关闭状态！", NamedTextColor.YELLOW))
            return
        }

        manager.setPhantomAllowed(sender, false)

        sender.sendMessage(
            Component.text("✓ ", NamedTextColor.GREEN)
                .append(Component.text("已禁止幻翼骚扰", NamedTextColor.YELLOW))
        )
        sender.sendMessage(
            Component.text("  幻翼将不会出现", NamedTextColor.GRAY)
        )
    }

    /**
     * 处理 status 命令 - 查看当前状态
     */
    private fun handleStatus(sender: Player) {
        val currentState = manager.isPhantomAllowed(sender)

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
            sender.sendMessage(
                Component.text("  长时间不睡觉会出现幻翼", NamedTextColor.GRAY)
            )
        } else {
            sender.sendMessage(
                Component.text("当前状态: ", NamedTextColor.GRAY)
                    .append(Component.text("禁止", NamedTextColor.RED))
            )
            sender.sendMessage(
                Component.text("  幻翼不会出现", NamedTextColor.GRAY)
            )
        }
    }

    /**
     * 显示帮助信息
     */
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
    }

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        if (!sender.hasPermission("tsl.phantom.toggle")) {
            return emptyList()
        }

        return when (args.size) {
            1 -> {
                // 第一个参数：子命令
                listOf("on", "off", "status")
                    .filter { it.startsWith(args[0], ignoreCase = true) }
            }
            else -> emptyList()
        }
    }
}

