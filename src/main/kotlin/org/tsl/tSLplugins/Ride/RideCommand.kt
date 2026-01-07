package org.tsl.tSLplugins.Ride

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.tsl.tSLplugins.SubCommandHandler

/**
 * Ride 命令处理器
 * 处理 /tsl ride 相关命令
 * /tsl ride [toggle] - 切换自己的骑乘开关（无需权限）
 * /tsl ride toggle <玩家> - 切换他人的骑乘开关（需要 tsl.ride.toggle.others 权限）
 */
class RideCommand(
    private val manager: RideManager
) : SubCommandHandler {

    private val serializer = LegacyComponentSerializer.legacyAmpersand()

    override fun handle(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        // 检查功能是否启用
        if (!manager.isEnabled()) {
            sender.sendMessage(serializer.deserialize(manager.getMessage("disabled")))
            return true
        }

        // 必须是玩家
        if (sender !is Player) {
            sender.sendMessage(serializer.deserialize(manager.getMessage("console_only")))
            return true
        }

        when {
            args.isEmpty() || (args[0].equals("toggle", ignoreCase = true) && args.size == 1) -> {
                // /tsl ride 或 /tsl ride toggle - 切换自己的开关
                handleToggle(sender, sender)
            }
            args[0].equals("toggle", ignoreCase = true) && args.size >= 2 -> {
                // /tsl ride toggle <玩家> - 切换他人的开关
                handleToggleOther(sender, args[1])
            }
            else -> {
                // 显示用法
                showUsage(sender)
            }
        }

        return true
    }

    /**
     * 处理切换自己的开关
     */
    private fun handleToggle(sender: Player, target: Player) {
        val newStatus = manager.togglePlayer(target)

        val message = if (newStatus) {
            manager.getMessage("toggle_enabled")
        } else {
            manager.getMessage("toggle_disabled")
        }

        sender.sendMessage(serializer.deserialize(message))
    }

    /**
     * 处理切换他人的开关（需要权限）
     */
    private fun handleToggleOther(sender: Player, targetName: String) {
        // 权限检查
        if (!sender.hasPermission("tsl.ride.toggle.others")) {
            sender.sendMessage(serializer.deserialize("&c你没有权限切换他人的骑乘开关"))
            return
        }

        val target = Bukkit.getPlayer(targetName)
        if (target == null) {
            sender.sendMessage(serializer.deserialize("&c玩家 $targetName 不在线"))
            return
        }

        val newStatus = manager.togglePlayer(target)
        val statusText = if (newStatus) "&a开启" else "&c关闭"
        sender.sendMessage(serializer.deserialize("&a已将 &f${target.name} &a的骑乘功能$statusText"))
        
        // 通知目标玩家
        val targetMessage = if (newStatus) {
            manager.getMessage("toggle_enabled")
        } else {
            manager.getMessage("toggle_disabled")
        }
        target.sendMessage(serializer.deserialize(targetMessage))
    }

    /**
     * 显示用法
     */
    private fun showUsage(sender: CommandSender) {
        sender.sendMessage(serializer.deserialize(manager.getMessage("usage")))
    }

    override fun tabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): List<String> {
        if (!manager.isEnabled() || sender !is Player) {
            return emptyList()
        }

        return when (args.size) {
            1 -> {
                listOf("toggle").filter { it.startsWith(args[0], ignoreCase = true) }
            }
            2 -> {
                // 只有有权限的玩家才显示玩家列表
                if (args[0].equals("toggle", ignoreCase = true) && sender.hasPermission("tsl.ride.toggle.others")) {
                    Bukkit.getOnlinePlayers().map { it.name }.filter { it.startsWith(args[1], ignoreCase = true) }
                } else {
                    emptyList()
                }
            }
            else -> emptyList()
        }
    }

    override fun getDescription(): String {
        return "右键骑乘生物功能管理"
    }
}

