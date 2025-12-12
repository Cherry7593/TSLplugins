package org.tsl.tSLplugins.EndDragon

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.tsl.tSLplugins.SubCommand

/**
 * 末影龙命令处理器
 * /tsl enddragon on - 启用末影龙控制
 * /tsl enddragon off - 禁用末影龙控制
 * /tsl enddragon status - 查看当前状态
 */
class EndDragonCommand(private val manager: EndDragonManager) : SubCommand {

    override fun handle(sender: CommandSender, args: Array<out String>): Boolean {
        // 只允许玩家使用
        if (sender !is Player) {
            sender.sendMessage(Component.text("此命令只能由玩家使用！", NamedTextColor.RED))
            return true
        }

        // 权限检查
        if (!sender.hasPermission("tsl.enddragon")) {
            sender.sendMessage(Component.text("你没有权限使用此命令！", NamedTextColor.RED))
            return true
        }

        // 检查功能是否启用
        if (!manager.isEnabled()) {
            sender.sendMessage(Component.text("末影龙控制功能未启用！", NamedTextColor.RED))
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
     * 处理 on 命令 - 启用末影龙控制
     */
    private fun handleOn(sender: Player) {
        sender.sendMessage(
            Component.text("✓ ", NamedTextColor.GREEN)
                .append(Component.text("末影龙控制已启用", NamedTextColor.YELLOW))
        )
        sender.sendMessage(
            Component.text("  末影龙将不会破坏方块", NamedTextColor.GRAY)
        )
    }

    /**
     * 处理 off 命令 - 禁用末影龙控制
     */
    private fun handleOff(sender: Player) {
        sender.sendMessage(
            Component.text("✓ ", NamedTextColor.GREEN)
                .append(Component.text("末影龙控制已禁用", NamedTextColor.YELLOW))
        )
        sender.sendMessage(
            Component.text("  末影龙行为恢复正常", NamedTextColor.GRAY)
        )
    }

    /**
     * 处理 status 命令 - 查看当前状态
     */
    private fun handleStatus(sender: Player) {
        sender.sendMessage(
            Component.text("═══ 末影龙控制状态 ═══", NamedTextColor.AQUA)
        )

        val damageStat = if (manager.isDisableDamage()) "✓ 已禁止" else "✗ 已启用"
        val damageLore = Component.text("禁止破坏方块: ", NamedTextColor.YELLOW)
            .append(Component.text(damageStat, if (manager.isDisableDamage()) NamedTextColor.GREEN else NamedTextColor.RED))

        sender.sendMessage(damageLore)
    }

    /**
     * 显示帮助信息
     */
    private fun showHelp(sender: Player) {
        sender.sendMessage(Component.text("═══ 末影龙控制命令 ═══", NamedTextColor.AQUA))
        sender.sendMessage(
            Component.text("  /tsl enddragon on", NamedTextColor.YELLOW)
                .append(Component.text(" - 启用末影龙控制", NamedTextColor.GRAY))
        )
        sender.sendMessage(
            Component.text("  /tsl enddragon off", NamedTextColor.YELLOW)
                .append(Component.text(" - 禁用末影龙控制", NamedTextColor.GRAY))
        )
        sender.sendMessage(
            Component.text("  /tsl enddragon status", NamedTextColor.YELLOW)
                .append(Component.text(" - 查看当前状态", NamedTextColor.GRAY))
        )
    }

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        return when {
            args.isEmpty() -> listOf("on", "off", "status")
            args.size == 1 -> listOf("on", "off", "status").filter { it.startsWith(args[0].lowercase()) }
            else -> emptyList()
        }
    }
}

