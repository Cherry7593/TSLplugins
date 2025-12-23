package org.tsl.tSLplugins.PapiAlias

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.tsl.tSLplugins.SubCommandHandler

/**
 * PapiAlias 命令处理器
 * 
 * 处理 /tsl papialias 相关命令
 * 
 * 命令：
 * - /tsl papialias reload - 重载配置
 * - /tsl papialias info - 显示当前配置信息
 * - /tsl papialias list [变量名] - 列出映射
 */
class PapiAliasCommand(
    private val manager: PapiAliasManager
) : SubCommandHandler {
    
    override fun handle(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (args.isEmpty()) {
            showUsage(sender)
            return true
        }
        
        when (args[0].lowercase()) {
            "reload" -> handleReload(sender)
            "info" -> handleInfo(sender)
            "list" -> handleList(sender, args.drop(1).toTypedArray())
            else -> showUsage(sender)
        }
        
        return true
    }
    
    /**
     * 处理重载命令
     */
    private fun handleReload(sender: CommandSender) {
        if (!sender.hasPermission("tsl.papialias.reload")) {
            sender.sendMessage(
                Component.text("你没有权限执行此命令").color(NamedTextColor.RED)
            )
            return
        }
        
        manager.reloadConfig()
        
        sender.sendMessage(
            Component.text("[PapiAlias] ").color(NamedTextColor.GOLD)
                .append(Component.text("配置已重载，共加载 ").color(NamedTextColor.GREEN))
                .append(Component.text("${manager.getVariableCount()}").color(NamedTextColor.AQUA))
                .append(Component.text(" 个变量，").color(NamedTextColor.GREEN))
                .append(Component.text("${manager.getMappingCount()}").color(NamedTextColor.AQUA))
                .append(Component.text(" 个映射").color(NamedTextColor.GREEN))
        )
    }
    
    /**
     * 处理信息命令
     */
    private fun handleInfo(sender: CommandSender) {
        if (!sender.hasPermission("tsl.papialias.info")) {
            sender.sendMessage(
                Component.text("你没有权限执行此命令").color(NamedTextColor.RED)
            )
            return
        }
        
        sender.sendMessage(
            Component.text("===== PapiAlias 信息 =====").color(NamedTextColor.GOLD)
        )
        sender.sendMessage(
            Component.text("状态: ").color(NamedTextColor.GRAY)
                .append(
                    if (manager.isEnabled()) 
                        Component.text("已启用").color(NamedTextColor.GREEN)
                    else 
                        Component.text("已禁用").color(NamedTextColor.RED)
                )
        )
        sender.sendMessage(
            Component.text("变量数量: ").color(NamedTextColor.GRAY)
                .append(Component.text("${manager.getVariableCount()}").color(NamedTextColor.AQUA))
        )
        sender.sendMessage(
            Component.text("映射总数: ").color(NamedTextColor.GRAY)
                .append(Component.text("${manager.getMappingCount()}").color(NamedTextColor.AQUA))
        )
        sender.sendMessage(
            Component.text("已配置变量: ").color(NamedTextColor.GRAY)
                .append(Component.text(manager.getMappedVariables().joinToString(", ")).color(NamedTextColor.WHITE))
        )
        sender.sendMessage(
            Component.text("使用方式: ").color(NamedTextColor.GRAY)
                .append(Component.text("%tsl_alias_<变量名>%").color(NamedTextColor.YELLOW))
        )
    }
    
    /**
     * 处理列表命令
     */
    private fun handleList(sender: CommandSender, args: Array<out String>) {
        if (!sender.hasPermission("tsl.papialias.list")) {
            sender.sendMessage(
                Component.text("你没有权限执行此命令").color(NamedTextColor.RED)
            )
            return
        }
        
        if (args.isEmpty()) {
            // 列出所有变量
            val variables = manager.getMappedVariables()
            if (variables.isEmpty()) {
                sender.sendMessage(
                    Component.text("[PapiAlias] ").color(NamedTextColor.GOLD)
                        .append(Component.text("暂无配置任何映射").color(NamedTextColor.YELLOW))
                )
                return
            }
            
            sender.sendMessage(
                Component.text("===== 已配置的变量 =====").color(NamedTextColor.GOLD)
            )
            for (variable in variables) {
                val mappings = manager.getVariableMappings(variable)
                sender.sendMessage(
                    Component.text("  $variable ").color(NamedTextColor.WHITE)
                        .append(Component.text("(${mappings.size} 个映射)").color(NamedTextColor.GRAY))
                )
            }
            sender.sendMessage(
                Component.text("使用 /tsl papialias list <变量名> 查看详细映射").color(NamedTextColor.GRAY)
            )
        } else {
            // 列出指定变量的映射
            val variableName = args[0]
            val mappings = manager.getVariableMappings(variableName)
            
            if (mappings.isEmpty()) {
                sender.sendMessage(
                    Component.text("[PapiAlias] ").color(NamedTextColor.GOLD)
                        .append(Component.text("变量 '$variableName' 没有配置映射").color(NamedTextColor.YELLOW))
                )
                return
            }
            
            sender.sendMessage(
                Component.text("===== $variableName 的映射 =====").color(NamedTextColor.GOLD)
            )
            for ((original, alias) in mappings) {
                sender.sendMessage(
                    Component.text("  \"$original\" ").color(NamedTextColor.WHITE)
                        .append(Component.text("→ ").color(NamedTextColor.GRAY))
                        .append(Component.text("\"$alias\"").color(NamedTextColor.GREEN))
                )
            }
        }
    }
    
    /**
     * 显示用法
     */
    private fun showUsage(sender: CommandSender) {
        sender.sendMessage(
            Component.text("===== PapiAlias 命令 =====").color(NamedTextColor.GOLD)
        )
        sender.sendMessage(
            Component.text("/tsl papialias reload").color(NamedTextColor.YELLOW)
                .append(Component.text(" - 重载配置").color(NamedTextColor.GRAY))
        )
        sender.sendMessage(
            Component.text("/tsl papialias info").color(NamedTextColor.YELLOW)
                .append(Component.text(" - 显示配置信息").color(NamedTextColor.GRAY))
        )
        sender.sendMessage(
            Component.text("/tsl papialias list [变量名]").color(NamedTextColor.YELLOW)
                .append(Component.text(" - 列出映射").color(NamedTextColor.GRAY))
        )
    }
    
    override fun tabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): List<String> {
        return when (args.size) {
            1 -> {
                listOf("reload", "info", "list")
                    .filter { it.startsWith(args[0], ignoreCase = true) }
            }
            2 -> {
                if (args[0].equals("list", ignoreCase = true)) {
                    manager.getMappedVariables()
                        .filter { it.startsWith(args[1], ignoreCase = true) }
                        .toList()
                } else {
                    emptyList()
                }
            }
            else -> emptyList()
        }
    }
    
    override fun getDescription(): String {
        return "变量映射管理"
    }
}
