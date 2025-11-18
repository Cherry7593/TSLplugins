package org.tsl.tSLplugins

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer

/**
 * 重载配置命令处理器
 * 处理 /tsl reload 命令
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
            // 重新加载配置文件
            plugin.reloadConfig()

            // 重新加载别名系统
            val reloadedAliases = plugin.reloadAliasManager()

            // 重新加载维护模式配置
            plugin.reloadMaintenanceManager()

            // 重新加载体型调整配置
            plugin.reloadScaleManager()

            // 重新加载 Hat 配置
            plugin.reloadHatManager()

            // 重新加载成就消息过滤器
            plugin.reloadAdvancementMessage()

            // 重新加载农田保护
            plugin.reloadFarmProtect()

            // 重新加载访客效果
            plugin.reloadVisitorEffect()

            // 重新加载 Ping 功能
            plugin.reloadPingManager()

            // 重新加载 Toss 功能
            plugin.reloadTossManager()


            sender.sendMessage(serializer.deserialize("&a配置文件重载成功！"))
            sender.sendMessage(serializer.deserialize("&7- 主配置文件已重载"))
            sender.sendMessage(serializer.deserialize("&7- 命令别名已重载 ($reloadedAliases 个别名)"))
            sender.sendMessage(serializer.deserialize("&7- 所有功能模块配置已重载"))

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

