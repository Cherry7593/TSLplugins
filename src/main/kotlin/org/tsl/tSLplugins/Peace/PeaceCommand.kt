package org.tsl.tSLplugins.Peace

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.tsl.tSLplugins.SubCommandHandler

/**
 * 伪和平模式命令处理器
 * /tsl peace set <玩家> <时间> - 设置和平模式（怪物不锁定）
 * /tsl peace nospawn <玩家> <时间> - 设置禁怪模式（附近禁止刷怪）
 * /tsl peace list [nospawn] - 列出玩家
 * /tsl peace clear <玩家|all> [nospawn] - 清除模式
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
            "nospawn" -> handleNoSpawn(sender, args)
            "list" -> handleList(sender, args)
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
     * 设置玩家禁怪模式
     * /tsl peace nospawn <玩家> <时间>
     */
    private fun handleNoSpawn(sender: CommandSender, args: Array<out String>): Boolean {
        if (!sender.hasPermission("tsl.peace.nospawn")) {
            sender.sendMessage(colorize(manager.getMessage("no_permission")))
            return true
        }

        if (args.size < 3) {
            sender.sendMessage(colorize("&c用法: /tsl peace nospawn <玩家> <时间>"))
            sender.sendMessage(colorize("&7时间格式: 60 (秒), 5m (分), 2h (时), 1d (天)"))
            sender.sendMessage(colorize("&7效果: 玩家附近 ${manager.getNoSpawnRadius()} 格内禁止刷怪"))
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

        val success = manager.setNoSpawn(target, durationMs, sender.name)

        if (success) {
            val formattedTime = manager.formatDuration(durationMs)
            sender.sendMessage(colorize(manager.getMessage("nospawn_set_success", 
                "player" to target.name, 
                "time" to formattedTime,
                "radius" to manager.getNoSpawnRadius().toString()
            )))
            target.sendMessage(colorize(manager.getMessage("nospawn_gained", 
                "time" to formattedTime,
                "radius" to manager.getNoSpawnRadius().toString()
            )))
        } else {
            sender.sendMessage(colorize(manager.getMessage("set_failed")))
        }

        return true
    }

    /**
     * 列出所有和平/禁怪玩家
     * /tsl peace list [nospawn]
     */
    private fun handleList(sender: CommandSender, args: Array<out String>): Boolean {
        if (!sender.hasPermission("tsl.peace.list")) {
            sender.sendMessage(colorize(manager.getMessage("no_permission")))
            return true
        }

        // 检查是否查看禁怪列表
        val isNoSpawn = args.size >= 2 && args[1].equals("nospawn", ignoreCase = true)
        
        if (isNoSpawn) {
            val entries = manager.listNoSpawnPlayers()
            if (entries.isEmpty()) {
                sender.sendMessage(colorize(manager.getMessage("nospawn_list_empty")))
                return true
            }

            sender.sendMessage(colorize("&b===== 禁怪模式玩家列表 ====="))
            sender.sendMessage(colorize("&7禁止刷怪半径: &f${manager.getNoSpawnRadius()} &7格"))
            entries.forEach { entry ->
                val remaining = manager.formatDuration(entry.remainingMs)
                val online = if (Bukkit.getPlayer(entry.uuid)?.isOnline == true) "&a在线" else "&7离线"
                sender.sendMessage(colorize("&f${entry.playerName} &7- $online &7- 剩余: &e$remaining"))
            }
            sender.sendMessage(colorize("&7共 &f${entries.size} &7人"))
        } else {
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
        }

        return true
    }

    /**
     * 清除玩家和平/禁怪模式
     * /tsl peace clear <玩家|all> [nospawn]
     */
    private fun handleClear(sender: CommandSender, args: Array<out String>): Boolean {
        if (!sender.hasPermission("tsl.peace.clear")) {
            sender.sendMessage(colorize(manager.getMessage("no_permission")))
            return true
        }

        if (args.size < 2) {
            sender.sendMessage(colorize("&c用法: /tsl peace clear <玩家|all> [nospawn]"))
            return true
        }

        val targetArg = args[1]
        val isNoSpawn = args.size >= 3 && args[2].equals("nospawn", ignoreCase = true)

        if (targetArg.equals("all", ignoreCase = true)) {
            val count = if (isNoSpawn) {
                manager.clearAllNoSpawn()
            } else {
                manager.clearAll()
            }
            val msgKey = if (isNoSpawn) "nospawn_clear_all" else "clear_all"
            sender.sendMessage(colorize(manager.getMessage(msgKey, "count" to count.toString())))
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

        val success = if (isNoSpawn) {
            manager.clearNoSpawn(offlinePlayer.uniqueId)
        } else {
            manager.clearPeace(offlinePlayer.uniqueId)
        }

        if (success) {
            val msgKey = if (isNoSpawn) "nospawn_clear_success" else "clear_success"
            sender.sendMessage(colorize(manager.getMessage(msgKey, "player" to (offlinePlayer.name ?: targetArg))))
            
            // 通知在线玩家
            val onlinePlayer = Bukkit.getPlayer(offlinePlayer.uniqueId)
            val notifyKey = if (isNoSpawn) "nospawn_cleared" else "cleared"
            onlinePlayer?.sendMessage(colorize(manager.getMessage(notifyKey)))
        } else {
            val msgKey = if (isNoSpawn) "not_nospawn" else "not_peaceful"
            sender.sendMessage(colorize(manager.getMessage(msgKey, "player" to (offlinePlayer.name ?: targetArg))))
        }

        return true
    }

    /**
     * 显示帮助
     */
    private fun showHelp(sender: CommandSender): Boolean {
        sender.sendMessage(colorize("&a===== 伪和平模式帮助 ====="))
        sender.sendMessage(colorize(""))
        sender.sendMessage(colorize("&e&l[和平模式] &7怪物不会发现/锁定玩家"))
        sender.sendMessage(colorize("&e/tsl peace set <玩家> <时间> &7- 设置和平模式"))
        sender.sendMessage(colorize("&e/tsl peace list &7- 查看和平玩家"))
        sender.sendMessage(colorize("&e/tsl peace clear <玩家|all> &7- 清除和平模式"))
        sender.sendMessage(colorize(""))
        sender.sendMessage(colorize("&b&l[禁怪模式] &7玩家附近禁止刷怪 (半径: ${manager.getNoSpawnRadius()} 格)"))
        sender.sendMessage(colorize("&b/tsl peace nospawn <玩家> <时间> &7- 设置禁怪模式"))
        sender.sendMessage(colorize("&b/tsl peace list nospawn &7- 查看禁怪玩家"))
        sender.sendMessage(colorize("&b/tsl peace clear <玩家|all> nospawn &7- 清除禁怪模式"))
        sender.sendMessage(colorize(""))
        sender.sendMessage(colorize("&7时间格式: 60 (秒), 5m (分), 2h (时), 1d (天)"))
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
            1 -> listOf("set", "nospawn", "list", "clear", "help").filter { 
                it.startsWith(args[0], ignoreCase = true) 
            }
            2 -> when (args[0].lowercase()) {
                "set", "nospawn" -> Bukkit.getOnlinePlayers().map { it.name }.filter { 
                    it.startsWith(args[1], ignoreCase = true) 
                }
                "list" -> listOf("nospawn").filter { 
                    it.startsWith(args[1], ignoreCase = true) 
                }
                "clear" -> {
                    val suggestions = mutableListOf("all")
                    suggestions.addAll(manager.listPeacePlayers().map { it.playerName })
                    suggestions.addAll(manager.listNoSpawnPlayers().map { it.playerName })
                    suggestions.distinct().filter { it.startsWith(args[1], ignoreCase = true) }
                }
                else -> emptyList()
            }
            3 -> when (args[0].lowercase()) {
                "set", "nospawn" -> listOf("60", "5m", "30m", "1h", "2h", "1d").filter { 
                    it.startsWith(args[2], ignoreCase = true) 
                }
                "clear" -> listOf("nospawn").filter { 
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
