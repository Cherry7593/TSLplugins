package org.tsl.tSLplugins.Alias

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

/**
 * 动态别名命令
 * 在 Bukkit 命令系统中注册的实际命令对象
 */
class DynamicAliasCommand(
    name: String,
    private val targetCommand: String,
    private val plugin: JavaPlugin
) : Command(name) {

    init {
        description = "别名命令 -> $targetCommand"
        usage = "/$name [参数]"
    }

    override fun execute(sender: CommandSender, commandLabel: String, args: Array<out String>): Boolean {
        // 构建完整的原命令
        val fullCommand = if (args.isNotEmpty()) {
            "$targetCommand ${args.joinToString(" ")}"
        } else {
            targetCommand
        }

        // 如果是玩家，使用实体调度器（Folia 兼容）
        if (sender is Player) {
            sender.scheduler.run(plugin, { _ ->
                Bukkit.dispatchCommand(sender, fullCommand)
            }, null)
        } else {
            // 控制台或命令方块直接执行
            Bukkit.dispatchCommand(sender, fullCommand)
        }

        return true
    }

    override fun tabComplete(sender: CommandSender, alias: String, args: Array<out String>): List<String> {
        // 获取目标命令的 tab 补全
        val targetCmd = Bukkit.getPluginCommand(targetCommand.split(" ")[0])
            ?: Bukkit.getServer().getPluginCommand(targetCommand.split(" ")[0])

        if (targetCmd != null) {
            try {
                // 尝试使用目标命令的 tab 补全
                val tabCompleter = targetCmd.tabCompleter
                if (tabCompleter != null) {
                    return tabCompleter.onTabComplete(sender, targetCmd, alias, args) ?: emptyList()
                }
            } catch (e: Exception) {
                plugin.logger.fine("获取 $targetCommand 的 tab 补全时出错: ${e.message}")
            }
        }

        // 如果无法获取目标命令的补全，返回在线玩家列表
        if (sender is Player) {
            return Bukkit.getOnlinePlayers()
                .map { it.name }
                .filter { it.startsWith(args.lastOrNull() ?: "", ignoreCase = true) }
        }

        return emptyList()
    }
}

