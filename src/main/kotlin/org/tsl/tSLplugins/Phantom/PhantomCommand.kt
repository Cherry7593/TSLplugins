package org.tsl.tSLplugins.Phantom

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.tsl.tSLplugins.SubCommand

/**
 * Phantom 命令处理器
 * /tsl phantom - 切换是否允许幻翼骚扰
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

        // 获取当前状态
        val currentState = manager.isPhantomAllowed(sender)

        // 切换状态
        val newState = !currentState
        manager.setPhantomAllowed(sender, newState)

        // 发送消息
        if (newState) {
            sender.sendMessage(
                Component.text("✓ ", NamedTextColor.GREEN)
                    .append(Component.text("已允许幻翼骚扰", NamedTextColor.YELLOW))
            )
            sender.sendMessage(
                Component.text("  长时间不睡觉会出现幻翼", NamedTextColor.GRAY)
            )
        } else {
            sender.sendMessage(
                Component.text("✓ ", NamedTextColor.GREEN)
                    .append(Component.text("已禁止幻翼骚扰", NamedTextColor.YELLOW))
            )
            sender.sendMessage(
                Component.text("  幻翼将不会出现", NamedTextColor.GRAY)
            )
        }

        return true
    }

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        // 无参数，返回空列表
        return emptyList()
    }
}

