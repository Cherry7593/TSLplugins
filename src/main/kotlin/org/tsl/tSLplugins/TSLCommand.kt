package org.tsl.tSLplugins

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

/**
 * TSL 插件主命令分发器
 * 负责将子命令分发到各个功能模块
 */
class TSLCommand : CommandExecutor, TabCompleter {

    private val subCommands = mutableMapOf<String, SubCommandHandler>()

    /**
     * 注册子命令处理器
     */
    fun registerSubCommand(name: String, handler: SubCommandHandler) {
        subCommands[name.lowercase()] = handler
    }

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (args.isEmpty()) {
            showHelp(sender)
            return true
        }

        val subCommandName = args[0].lowercase()
        val handler = subCommands[subCommandName]

        if (handler == null) {
            sender.sendMessage("§c未知的子命令: ${args[0]}")
            showHelp(sender)
            return true
        }

        // 将剩余参数传递给子命令处理器
        val subArgs = args.drop(1).toTypedArray()
        return handler.handle(sender, command, label, subArgs)
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): List<String> {
        if (args.size == 1) {
            // 第一级：显示所有子命令
            return subCommands.keys
                .filter { it.startsWith(args[0].lowercase()) }
                .sorted()
        }

        if (args.size > 1) {
            // 第二级及以后：委托给对应的子命令处理器
            val subCommandName = args[0].lowercase()
            val handler = subCommands[subCommandName]
            if (handler != null) {
                val subArgs = args.drop(1).toTypedArray()
                return handler.tabComplete(sender, command, label, subArgs)
            }
        }

        return emptyList()
    }

    private fun showHelp(sender: CommandSender) {
        sender.sendMessage("§e§l===== TSL 插件命令 =====")
        subCommands.entries.sortedBy { it.key }.forEach { (name, handler) ->
            sender.sendMessage("§e/tsl $name §7- ${handler.getDescription()}")
        }
    }
}

/**
 * 子命令处理器接口
 * 每个功能模块实现此接口来处理自己的命令
 */
interface SubCommandHandler {
    /**
     * 处理命令
     */
    fun handle(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean

    /**
     * Tab 补全
     */
    fun tabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): List<String> = emptyList()

    /**
     * 获取命令描述
     */
    fun getDescription(): String
}

