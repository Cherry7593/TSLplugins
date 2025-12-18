package org.tsl.tSLplugins.Peace

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.tsl.tSLplugins.SubCommandHandler

/**
 * 伪和平模式命令处理器
 * /tsl peace set <玩家> <时间>
 * /tsl peace list
 * /tsl peace clear <玩家>
 * /tsl peace help
 */
class PeaceCommand(private val manager: PeaceManager) : SubCommandHandler {

    override fun handle(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!manager.isEnabled()) {
            sender.sendMessage(colorize(manager.getMessage("disabled")))
            return true
        }

        if (args.isEmpty()) {
            return showHelp(sender)
        }

        return when (args[0].lowercase()) {
            "set" -> handleSet(sender, args)
            "list" -> handleList(sender)
            "clear" -> handleClear(sender, args)
            "help" -> showHelp(sender)
            else -> showHelp(sender)
        }
    }

    /**
     * 设置玩家和平模式
     * /tsl peace set <玩家> <时间>
     */
    private fun handleSet(sender: CommandSender, args: Array<out String>): Boolean {
        if (!sender.hasPermission("tsl.peace.set")) {
            sender.sendMessage(colorize(manager.getMessage("no_permission")))
            return true
        }

        if (args.size < 3) {
            sender.sendMessage(colorize("&c用法: /tsl peace set <玩家> <时间>"))
            sender.sendMessage(colorize("&7时间格式: 60 (秒), 5m (分), 2h (时), 1d (天)"))
            return true
        }

        val targetName = args[1]
        val target = Bukkit.getPlayer(targetName)

        if (target == null || !target.isOnline) {
            sender.sendMessage(colorize(manager.getMessage("player_not_found", "player" to targetName)))
            return true
        }

        val durationStr = args[2]
        val durationMs = manager.parseDuration(durationStr)

        if (durationMs == null || durationMs <= 0) {
            sender.sendMessage(colorize(manager.getMessage("invalid_duration")))
            return true
        }

        val success = manager.setPeace(target, durationMs, sender.name)

        if (success) {
            val formattedTime = manager.formatDuration(durationMs)
            sender.sendMessage(colorize(manager.getMessage("set_success", 
                "player" to target.name, 
                "time" to formattedTime
            )))
            target.sendMessage(colorize(manager.getMessage("gained", "time" to formattedTime)))
        } else {
            sender.sendMessage(colorize(manager.getMessage("set_failed")))
        }

        return true
    }

    /**
     * 列出所有和平玩家
     * /tsl peace list
     */
    private fun handleList(sender: CommandSender): Boolean {
        if (!sender.hasPermission("tsl.peace.list")) {
            sender.sendMessage(colorize(manager.getMessage("no_permission")))
            return true
        }

        val entries = manager.listPeacePlayers()

        if (entries.isEmpty()) {
            sender.sendMessage(colorize(manager.getMessage("list_empty")))
            return true
        }

        sender.sendMessage(colorize("&a===== 和平模式玩家列表 ====="))
        entries.forEach { entry ->
            val remaining = manager.formatDuration(entry.remainingMs)
            val online = if (Bukkit.getPlayer(entry.uuid)?.isOnline == true) "&a在线" else "&7离线"
            sender.sendMessage(colorize("&f${entry.playerName} &7- $online &7- 剩余: &e$remaining"))
        }
        sender.sendMessage(colorize("&7共 &f${entries.size} &7人"))

        return true
    }

    /**
     * 清除玩家和平模式
     * /tsl peace clear <玩家|all>
     */
    private fun handleClear(sender: CommandSender, args: Array<out String>): Boolean {
        if (!sender.hasPermission("tsl.peace.clear")) {
            sender.sendMessage(colorize(manager.getMessage("no_permission")))
            return true
        }

        if (args.size < 2) {
            sender.sendMessage(colorize("&c用法: /tsl peace clear <玩家|all>"))
            return true
        }

        val targetArg = args[1]

        if (targetArg.equals("all", ignoreCase = true)) {
            val count = manager.clearAll()
            sender.sendMessage(colorize(manager.getMessage("clear_all", "count" to count.toString())))
            return true
        }

        // 尝试通过名称查找玩家 UUID
        val offlinePlayer = Bukkit.getOfflinePlayers().find { 
            it.name?.equals(targetArg, ignoreCase = true) == true 
        } ?: Bukkit.getPlayer(targetArg)

        if (offlinePlayer == null) {
            sender.sendMessage(colorize(manager.getMessage("player_not_found", "player" to targetArg)))
            return true
        }

        val success = manager.clearPeace(offlinePlayer.uniqueId)

        if (success) {
            sender.sendMessage(colorize(manager.getMessage("clear_success", "player" to (offlinePlayer.name ?: targetArg))))
            
            // 通知在线玩家
            val onlinePlayer = Bukkit.getPlayer(offlinePlayer.uniqueId)
            onlinePlayer?.sendMessage(colorize(manager.getMessage("cleared")))
        } else {
            sender.sendMessage(colorize(manager.getMessage("not_peaceful", "player" to (offlinePlayer.name ?: targetArg))))
        }

        return true
    }

    /**
     * 显示帮助
     */
    private fun showHelp(sender: CommandSender): Boolean {
        sender.sendMessage(colorize("&a===== 伪和平模式帮助 ====="))
        sender.sendMessage(colorize("&e/tsl peace set <玩家> <时间> &7- 设置玩家和平模式"))
        sender.sendMessage(colorize("&e/tsl peace list &7- 查看所有和平玩家"))
        sender.sendMessage(colorize("&e/tsl peace clear <玩家|all> &7- 清除和平模式"))
        sender.sendMessage(colorize("&e/tsl peace help &7- 显示此帮助"))
        sender.sendMessage(colorize("&7时间格式: 60 (秒), 5m (分), 2h (时), 1d (天)"))
        sender.sendMessage(colorize("&7效果: 怪物不会发现/锁定玩家"))
        return true
    }

    override fun tabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): List<String> {
        if (!manager.isEnabled()) return emptyList()

        return when (args.size) {
            1 -> listOf("set", "list", "clear", "help").filter { 
                it.startsWith(args[0], ignoreCase = true) 
            }
            2 -> when (args[0].lowercase()) {
                "set" -> Bukkit.getOnlinePlayers().map { it.name }.filter { 
                    it.startsWith(args[1], ignoreCase = true) 
                }
                "clear" -> {
                    val suggestions = mutableListOf("all")
                    suggestions.addAll(manager.listPeacePlayers().map { it.playerName })
                    suggestions.filter { it.startsWith(args[1], ignoreCase = true) }
                }
                else -> emptyList()
            }
            3 -> when (args[0].lowercase()) {
                "set" -> listOf("60", "5m", "30m", "1h", "2h", "1d").filter { 
                    it.startsWith(args[2], ignoreCase = true) 
                }
                else -> emptyList()
            }
            else -> emptyList()
        }
    }

    override fun getDescription(): String {
        return "伪和平模式管理"
    }

    private fun colorize(text: String): String {
        return text.replace("&", "§")
    }
}
