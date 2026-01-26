package org.tsl.tSLplugins

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer

/**
 * 重载配置命令处理器
 * 处理 /tsl reload 命令
 * 
 * 新架构下，所有模块的重载由 ModuleRegistry 统一处理
 */
class ReloadCommand(private val plugin: TSLplugins) : SubCommandHandler {

    private val serializer = LegacyComponentSerializer.legacyAmpersand()

    override fun handle(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        // 检查权限
        if (!sender.isOp && !sender.hasPermission("tsl.reload")) {
            sender.sendMessage(serializer.deserialize("&c你没有权限使用此命令！"))
            return true
        }

        sender.sendMessage(serializer.deserialize("&e正在重新加载配置文件..."))

        try {
            // 1. 重新加载主配置文件
            plugin.reloadConfig()

            // 2. 重新加载在线玩家数据
            plugin.reloadPlayerData()

            // 3. 重载所有模块（新架构统一处理）
            val enabledModules = plugin.reloadModules()

            // 4. 重载命令别名系统
            val aliasCount = plugin.reloadAliasManager()

            // 5. 重新加载消息管理器
            plugin.messageManager.reload()

            sender.sendMessage(serializer.deserialize("&a配置文件重载成功！"))
            sender.sendMessage(serializer.deserialize("&7- 主配置文件已重载"))
            sender.sendMessage(serializer.deserialize("&7- 功能模块已重载 ($enabledModules 个已启用)"))
            sender.sendMessage(serializer.deserialize("&7- 命令别名已重载 ($aliasCount 个别名)"))

            plugin.logger.info("${sender.name} 重新加载了配置文件")

        } catch (e: Exception) {
            sender.sendMessage(serializer.deserialize("&c重载配置文件时出错: ${e.message}"))
            plugin.logger.severe("重载配置文件时出错: ${e.message}")
            e.printStackTrace()
        }

        return true
    }

    override fun getDescription(): String {
        return "重新加载插件配置文件"
    }
}
