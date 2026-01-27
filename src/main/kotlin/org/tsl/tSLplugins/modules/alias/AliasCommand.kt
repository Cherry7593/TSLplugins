package org.tsl.tSLplugins.modules.alias

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin
import org.tsl.tSLplugins.SubCommandHandler

/**
 * 命令别名重载命令处理器
 * 处理 /tsl aliasreload 命令
 */
class AliasCommand(
    private val plugin: JavaPlugin,
    private val aliasManager: AliasManager
) : SubCommandHandler {

    override fun handle(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (!sender.hasPermission("tsl.alias.reload")) {
            sender.sendMessage("§c你没有权限执行此命令！")
            return true
        }

        try {
            aliasManager.reloadAliases()
            sender.sendMessage("§a[别名系统] 已重载 ${aliasManager.getAliasCount()} 个命令别名！")
            plugin.logger.info("${sender.name} 重载了命令别名配置")
        } catch (e: Exception) {
            sender.sendMessage("§c[别名系统] 重载失败: ${e.message}")
            plugin.logger.warning("重载别名配置时发生错误: ${e.message}")
        }
        return true
    }

    override fun getDescription(): String {
        return "重载命令别名配置"
    }
}
