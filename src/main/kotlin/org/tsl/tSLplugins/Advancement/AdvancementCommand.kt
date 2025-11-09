package org.tsl.tSLplugins.Advancement

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin
import org.tsl.tSLplugins.SubCommandHandler

/**
 * 成就系统命令处理器
 * 处理 /tsl advcount 命令
 */
class AdvancementCommand(
    private val plugin: JavaPlugin,
    private val countHandler: AdvancementCount
) : SubCommandHandler {

    override fun handle(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        // /tsl advcount refresh [player/all]
        if (args.isEmpty() || args[0].equals("refresh", ignoreCase = true)) {
            if (args.size < 2) {
                sender.sendMessage("§c用法: /tsl advcount refresh <player|all>")
                return true
            }

            val target = args[1]

            if (target.equals("all", ignoreCase = true)) {
                // 刷新所有在线玩家
                countHandler.refreshAllCounts()
                sender.sendMessage("§a已开始刷新所有在线玩家的成就统计！")
                return true
            } else {
                // 刷新指定玩家
                val targetPlayer = Bukkit.getPlayer(target)
                if (targetPlayer == null || !targetPlayer.isOnline) {
                    sender.sendMessage("§c玩家 $target 不在线或不存在！")
                    return true
                }

                // 使用实体调度器以兼容 Folia
                targetPlayer.scheduler.run(plugin, { _ ->
                    if (targetPlayer.isOnline) {
                        countHandler.refreshCount(targetPlayer)
                        sender.sendMessage("§a已刷新玩家 ${targetPlayer.name} 的成就统计！")
                    }
                }, null)

                return true
            }
        }

        sender.sendMessage("§c用法: /tsl advcount refresh <player|all>")
        return true
    }

    override fun tabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): List<String> {
        return when (args.size) {
            1 -> {
                // /tsl advcount <tab>
                listOf("refresh").filter { it.startsWith(args[0].lowercase()) }
            }
            2 -> {
                if (args[0].equals("refresh", ignoreCase = true)) {
                    // /tsl advcount refresh <tab>
                    val completions = mutableListOf("all")
                    completions.addAll(Bukkit.getOnlinePlayers().map { it.name })
                    completions.filter { it.lowercase().startsWith(args[1].lowercase()) }
                } else {
                    emptyList()
                }
            }
            else -> emptyList()
        }
    }

    override fun getDescription(): String {
        return "刷新成就统计数据"
    }
}

