package org.tsl.tSLplugins.Visitor

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.tsl.tSLplugins.SubCommandHandler

/**
 * 访客模式管理命令
 * /tsl visitor <set|remove|check|list|reload> [玩家名]
 */
class VisitorCommand(
    private val visitorEffect: VisitorEffect
) : SubCommandHandler {

    override fun handle(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        // 检查权限
        if (!sender.hasPermission("tsl.visitor.admin")) {
            sender.sendMessage("§c你没有权限使用此命令！")
            return true
        }

        if (args.isEmpty()) {
            sendHelp(sender)
            return true
        }

        when (args[0].lowercase()) {
            "set" -> {
                if (args.size < 2) {
                    sender.sendMessage("§c用法: /tsl visitor set <玩家名>")
                    return true
                }
                val target = Bukkit.getPlayer(args[1])
                if (target == null) {
                    sender.sendMessage("§c玩家 ${args[1]} 不在线！")
                    return true
                }
                visitorEffect.setVisitor(target, true, false)
                sender.sendMessage("§a已将玩家 ${target.name} 设置为访客")
            }

            "remove" -> {
                if (args.size < 2) {
                    sender.sendMessage("§c用法: /tsl visitor remove <玩家名>")
                    return true
                }
                val target = Bukkit.getPlayer(args[1])
                if (target == null) {
                    sender.sendMessage("§c玩家 ${args[1]} 不在线！")
                    return true
                }
                visitorEffect.setVisitor(target, false, false)
                sender.sendMessage("§a已移除玩家 ${target.name} 的访客身份")
            }

            "check" -> {
                if (args.size < 2) {
                    sender.sendMessage("§c用法: /tsl visitor check <玩家名>")
                    return true
                }
                val target = Bukkit.getPlayer(args[1])
                if (target == null) {
                    sender.sendMessage("§c玩家 ${args[1]} 不在线！")
                    return true
                }
                val isVisitor = visitorEffect.isVisitor(target)
                if (isVisitor) {
                    sender.sendMessage("§a玩家 ${target.name} 当前是访客")
                } else {
                    sender.sendMessage("§7玩家 ${target.name} 当前不是访客")
                }
            }

            "list" -> {
                val onlineVisitors = Bukkit.getOnlinePlayers().filter { visitorEffect.isVisitor(it) }
                if (onlineVisitors.isEmpty()) {
                    sender.sendMessage("§7当前没有在线的访客")
                } else {
                    sender.sendMessage("§a在线访客列表 (${onlineVisitors.size}):")
                    onlineVisitors.forEach {
                        sender.sendMessage("§7  - ${it.name}")
                    }
                }
            }

            "reload" -> {
                visitorEffect.loadConfig()
                sender.sendMessage("§a访客模式配置已重新加载！")
            }

            else -> {
                sendHelp(sender)
            }
        }

        return true
    }

    override fun getDescription(): String {
        return "访客模式管理命令"
    }

    override fun tabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): List<String> {
        // 检查权限
        if (!sender.hasPermission("tsl.visitor.admin")) {
            return emptyList()
        }

        return when (args.size) {
            1 -> {
                // 第一级：子命令补全
                listOf("set", "remove", "check", "list", "reload")
                    .filter { it.startsWith(args[0].lowercase()) }
            }
            2 -> {
                // 第二级：玩家名补全
                when (args[0].lowercase()) {
                    "set", "check" -> {
                        // set 和 check：补全所有在线玩家
                        Bukkit.getOnlinePlayers()
                            .map { it.name }
                            .filter { it.lowercase().startsWith(args[1].lowercase()) }
                            .sorted()
                    }
                    "remove" -> {
                        // remove：只补全在线的访客玩家
                        Bukkit.getOnlinePlayers()
                            .filter { visitorEffect.isVisitor(it) }
                            .map { it.name }
                            .filter { it.lowercase().startsWith(args[1].lowercase()) }
                            .sorted()
                    }
                    else -> emptyList()
                }
            }
            else -> emptyList()
        }
    }

    private fun sendHelp(sender: CommandSender) {
        sender.sendMessage("§6§l=== 访客模式管理 ===")
        sender.sendMessage("§e/tsl visitor set <玩家名> §7- 设置玩家为访客")
        sender.sendMessage("§e/tsl visitor remove <玩家名> §7- 移除玩家的访客身份")
        sender.sendMessage("§e/tsl visitor check <玩家名> §7- 检查玩家是否是访客")
        sender.sendMessage("§e/tsl visitor list §7- 列出所有在线访客")
        sender.sendMessage("§e/tsl visitor reload §7- 重新加载配置")
    }
}

