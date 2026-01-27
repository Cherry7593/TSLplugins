package org.tsl.tSLplugins.modules.webbridge

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

/**
 * QQ 群绑定命令
 * /qqbind - 生成绑定验证码
 */
class QQBindCommand(
    private val qqBindManager: QQBindManager
) : CommandExecutor, TabCompleter {

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§c此命令只能由玩家执行")
            return true
        }

        // 检查权限
        if (!sender.hasPermission("tsl.qqbind")) {
            sender.sendMessage("§c你没有权限使用此命令")
            return true
        }

        // 请求绑定
        qqBindManager.requestQQBind(sender)
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): List<String> {
        return emptyList()
    }
}
