package org.tsl.tSLplugins.modules.alias

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

        // 如果是玩家，尝试使用实体调度器（Folia 兼容）
        if (sender is Player) {
            // 检查插件是否启用（热重载兼容）
            // 动态获取当前活跃的插件实例
            val activePlugin = Bukkit.getPluginManager().getPlugin("TSLplugins")
            if (activePlugin != null && activePlugin.isEnabled) {
                try {
                    sender.scheduler.run(activePlugin, { _ ->
                        Bukkit.dispatchCommand(sender, fullCommand)
                    }, null)
                } catch (_: Exception) {
                    // 如果调度失败，直接执行命令
                    Bukkit.dispatchCommand(sender, fullCommand)
                }
            } else {
                // 插件已禁用或未找到，直接执行命令
                Bukkit.dispatchCommand(sender, fullCommand)
            }
        } else {
            // 控制台或命令方块直接执行
            Bukkit.dispatchCommand(sender, fullCommand)
        }

        return true
    }

    override fun tabComplete(sender: CommandSender, alias: String, args: Array<out String>): List<String> {
        // 解析目标命令（可能包含子命令）
        val targetParts = targetCommand.split(" ")
        val baseCommand = targetParts[0]
        val subCommands = targetParts.drop(1)

        // 获取基础命令
        val targetCmd = Bukkit.getPluginCommand(baseCommand)
            ?: Bukkit.getServer().getPluginCommand(baseCommand)

        if (targetCmd != null) {
            try {
                val tabCompleter = targetCmd.tabCompleter
                if (tabCompleter != null) {
                    // 构建完整的参数数组（包含子命令）
                    val fullArgs = if (subCommands.isNotEmpty()) {
                        // 如果别名包含子命令，将子命令和用户输入的参数合并
                        (subCommands + args.toList()).toTypedArray()
                    } else {
                        args
                    }

                    // 使用目标命令的 tab 补全
                    val completions = tabCompleter.onTabComplete(sender, targetCmd, baseCommand, fullArgs)
                    if (completions != null && completions.isNotEmpty()) {
                        return completions
                    }
                }
            } catch (e: Exception) {
                plugin.logger.fine("获取 $targetCommand 的 tab 补全时出错: ${e.message}")
            }
        }

        // 如果无法获取目标命令的补全，返回在线玩家列表作为后备
        if (args.isNotEmpty()) {
            val lastArg = args.last()
            return Bukkit.getOnlinePlayers()
                .map { it.name }
                .filter { it.startsWith(lastArg, ignoreCase = true) }
                .sorted()
        }

        return emptyList()
    }
}
