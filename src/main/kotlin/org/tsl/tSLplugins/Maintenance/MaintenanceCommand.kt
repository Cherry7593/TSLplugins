package org.tsl.tSLplugins.Maintenance

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.tsl.tSLplugins.SubCommandHandler

/**
 * 维护模式命令处理器
 * 处理 /tsl maintenance 命令及其子命令
 */
class MaintenanceCommand(
    private val manager: MaintenanceManager,
    private val permissionListener: MaintenancePermissionListener
) : SubCommandHandler {

    private val serializer = LegacyComponentSerializer.legacyAmpersand()

    override fun handle(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        // 检查功能是否启用
        if (!manager.isFeatureEnabled()) {
            sender.sendMessage(serializer.deserialize("&c维护模式功能已禁用"))
            return true
        }

        if (!sender.hasPermission("tsl.maintenance.manage")) {
            sender.sendMessage(serializer.deserialize("&c你没有权限使用此命令！"))
            return true
        }

        if (args.isEmpty()) {
            sendHelp(sender)
            return true
        }

        when (args[0].lowercase()) {
            "on" -> handleOn(sender)
            "off" -> handleOff(sender)
            "add" -> handleAdd(sender, args)
            "remove" -> handleRemove(sender, args)
            "whitelist", "list" -> handleWhitelist(sender)
            else -> sendHelp(sender)
        }

        return true
    }

    private fun handleOn(sender: CommandSender) {
        if (manager.isMaintenanceEnabled()) {
            sender.sendMessage(serializer.deserialize("&e维护模式已经是开启状态！"))
            return
        }

        manager.setMaintenanceEnabled(true)
        val message = manager.getConfig().getString("maintenance.messages.enabled",
            "&a[维护模式] &7服务器维护模式已启用！") ?: "&a[维护模式] &7服务器维护模式已启用！"
        sender.sendMessage(serializer.deserialize(message))

        // 立即检查在线玩家，踢出没有权限的玩家
        permissionListener.checkOnlinePlayers()
    }

    private fun handleOff(sender: CommandSender) {
        if (!manager.isMaintenanceEnabled()) {
            sender.sendMessage(serializer.deserialize("&e维护模式已经是关闭状态！"))
            return
        }

        manager.setMaintenanceEnabled(false)
        val message = manager.getConfig().getString("maintenance.messages.disabled",
            "&c[维护模式] &7服务器维护模式已关闭！") ?: "&c[维护模式] &7服务器维护模式已关闭！"
        sender.sendMessage(serializer.deserialize(message))
    }

    private fun handleAdd(sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) {
            sender.sendMessage(serializer.deserialize("&c用法: /tsl maintenance add <玩家名>"))
            return
        }

        val playerName = args[1]

        // 尝试从在线玩家获取 UUID
        val onlinePlayer = Bukkit.getPlayerExact(playerName)
        if (onlinePlayer != null) {
            manager.addToWhitelist(onlinePlayer.uniqueId, onlinePlayer.name)
            sender.sendMessage(serializer.deserialize("&a已将玩家 &f$playerName &a添加到维护模式白名单！"))
            return
        }

        // 尝试从离线玩家获取 UUID
        @Suppress("DEPRECATION")
        val offlinePlayer = Bukkit.getOfflinePlayer(playerName)
        if (offlinePlayer.hasPlayedBefore() || offlinePlayer.name != null) {
            manager.addToWhitelist(offlinePlayer.uniqueId, playerName)
            sender.sendMessage(serializer.deserialize("&a已将玩家 &f$playerName &a添加到维护模式白名单！"))
        } else {
            sender.sendMessage(serializer.deserialize("&c找不到玩家: $playerName"))
        }
    }

    private fun handleRemove(sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) {
            sender.sendMessage(serializer.deserialize("&c用法: /tsl maintenance remove <玩家名>"))
            return
        }

        val playerName = args[1]

        // 尝试从在线玩家获取 UUID
        val onlinePlayer = Bukkit.getPlayerExact(playerName)
        if (onlinePlayer != null) {
            if (manager.removeFromWhitelist(onlinePlayer.uniqueId)) {
                sender.sendMessage(serializer.deserialize("&a已将玩家 &f$playerName &a从维护模式白名单移除！"))
                // 如果维护模式开启，立即检查该玩家是否还有权限
                if (manager.isMaintenanceEnabled()) {
                    permissionListener.checkOnlinePlayers()
                }
            } else {
                sender.sendMessage(serializer.deserialize("&c玩家 &f$playerName &c不在白名单中！"))
            }
            return
        }

        // 尝试从离线玩家获取 UUID
        @Suppress("DEPRECATION")
        val offlinePlayer = Bukkit.getOfflinePlayer(playerName)
        if (offlinePlayer.hasPlayedBefore() || offlinePlayer.name != null) {
            if (manager.removeFromWhitelist(offlinePlayer.uniqueId)) {
                sender.sendMessage(serializer.deserialize("&a已将玩家 &f$playerName &a从维护模式白名单移除！"))
            } else {
                sender.sendMessage(serializer.deserialize("&c玩家 &f$playerName &c不在白名单中！"))
            }
        } else {
            sender.sendMessage(serializer.deserialize("&c找不到玩家: $playerName"))
        }
    }

    private fun handleWhitelist(sender: CommandSender) {
        val whitelist = manager.getWhitelistNames()

        if (whitelist.isEmpty()) {
            sender.sendMessage(serializer.deserialize("&e维护模式白名单为空"))
            return
        }

        sender.sendMessage(serializer.deserialize("&a维护模式白名单 &7(共 ${whitelist.size} 人):"))
        whitelist.forEach { name ->
            sender.sendMessage(serializer.deserialize("  &7- &f$name"))
        }
    }

    private fun sendHelp(sender: CommandSender) {
        sender.sendMessage(serializer.deserialize("&a&l维护模式命令帮助:"))
        sender.sendMessage(serializer.deserialize("&7/tsl maintenance on &f- 开启维护模式"))
        sender.sendMessage(serializer.deserialize("&7/tsl maintenance off &f- 关闭维护模式"))
        sender.sendMessage(serializer.deserialize("&7/tsl maintenance add <玩家名> &f- 添加玩家到白名单"))
        sender.sendMessage(serializer.deserialize("&7/tsl maintenance remove <玩家名> &f- 从白名单移除玩家"))
        sender.sendMessage(serializer.deserialize("&7/tsl maintenance whitelist &f- 查看白名单列表"))
    }

    override fun getDescription(): String {
        return "管理服务器维护模式"
    }

    override fun tabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): List<String> {
        // 检查功能是否启用
        if (!manager.isFeatureEnabled()) {
            return emptyList()
        }

        // 检查权限
        if (!sender.hasPermission("tsl.maintenance.manage")) {
            return emptyList()
        }

        return when (args.size) {
            1 -> {
                // 第一级：子命令补全
                listOf("on", "off", "add", "remove", "whitelist", "list")
                    .filter { it.startsWith(args[0].lowercase()) }
            }
            2 -> {
                // 第二级：根据子命令补全
                when (args[0].lowercase()) {
                    "add" -> {
                        // add 命令：补全所有在线玩家
                        Bukkit.getOnlinePlayers()
                            .map { it.name }
                            .filter { it.lowercase().startsWith(args[1].lowercase()) }
                            .sorted()
                    }
                    "remove" -> {
                        // remove 命令：补全白名单中的玩家
                        manager.getWhitelistNames()
                            .filter { it.lowercase().startsWith(args[1].lowercase()) }
                            .sorted()
                    }
                    else -> emptyList()
                }
            }
            else -> emptyList()
        }
    }
}

