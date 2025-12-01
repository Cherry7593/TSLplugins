package org.tsl.tSLplugins.Patrol

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.tsl.tSLplugins.SubCommand

/**
 * Patrol 命令处理器
 * /tsl patrol - 随机传送到玩家位置进行巡查
 */
class PatrolCommand(private val manager: PatrolManager) : SubCommand {

    override fun handle(sender: CommandSender, args: Array<out String>): Boolean {
        // 只允许玩家使用
        if (sender !is Player) {
            sender.sendMessage(Component.text("此命令只能由玩家使用！", NamedTextColor.RED))
            return true
        }

        // 权限检查
        if (!sender.hasPermission("tsl.patrol.use")) {
            sender.sendMessage(Component.text("你没有权限使用此命令！", NamedTextColor.RED))
            return true
        }

        // 执行巡逻
        when (val result = manager.patrol(sender)) {
            is PatrolResult.Success -> {
                sender.sendMessage(
                    Component.text("✓ ", NamedTextColor.GREEN)
                        .append(Component.text("已传送到 ", NamedTextColor.YELLOW))
                        .append(Component.text(result.target.name, NamedTextColor.WHITE))
                        .append(Component.text(" 的位置", NamedTextColor.YELLOW))
                )

                // 如果是冷却期内的二次巡逻，显示上次时间
                result.timeSinceLastPatrol?.let { time ->
                    sender.sendMessage(
                        Component.text("  上次巡逻为 ", NamedTextColor.GRAY)
                            .append(Component.text(time, NamedTextColor.AQUA))
                            .append(Component.text(" 前", NamedTextColor.GRAY))
                    )
                }
            }
            is PatrolResult.NoPlayers -> {
                sender.sendMessage(Component.text("没有可巡逻的玩家！", NamedTextColor.RED))
            }
        }

        return true
    }

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        // 无参数，返回空列表
        return emptyList()
    }
}

