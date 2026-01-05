package org.tsl.tSLplugins.Peace

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.tsl.tSLplugins.SubCommandHandler

/**
 * 伪和平模式命令处理器
 * /tsl peace set <peace|nospawn> <时间> [玩家] - 设置模式
 * /tsl peace list [peace|nospawn] - 列出玩家
 * /tsl peace clear <玩家|all> [peace|nospawn] - 清除模式
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
            "list" -> handleList(sender, args)
            "clear" -> handleClear(sender, args)
            "help" -> showHelp(sender)
            else -> showHelp(sender)
        }
    }

    /**
     * 设置玩家模式
     * /tsl peace set <peace|nospawn> <时间> [玩家]
     */
    private fun handleSet(sender: CommandSender, args: Array<out String>): Boolean {
        if (!sender.hasPermission("tsl.peace.set")) {
            sender.sendMessage(colorize(manager.getMessage("no_permission")))
            return true
        }

        if (args.size < 3) {
            sender.sendMessage(colorize("&c用法: /tsl peace set <peace|nospawn> <时间> [玩家]"))
            sender.sendMessage(colorize("&7模式: peace(怪物不锁定), nospawn(禁止刷怪)"))
            sender.sendMessage(colorize("&7时间格式: 60 (秒), 5m (分), 2h (时), 1d (天)"))
            return true
        }

        val mode = args[1].lowercase()
        if (mode != "peace" && mode != "nospawn") {
            sender.sendMessage(colorize("&c无效模式: ${args[1]}"))
            sender.sendMessage(colorize("&7可用模式: peace, nospawn"))
            return true
        }

        val durationStr = args[2]
        val durationMs = manager.parseDuration(durationStr)
        if (durationMs == null || durationMs <= 0) {
            sender.sendMessage(colorize(manager.getMessage("invalid_duration")))
            return true
        }

        // 获取目标玩家
        val target: Player = if (args.size >= 4) {
            Bukkit.getPlayer(args[3]) ?: run {
                sender.sendMessage(colorize(manager.getMessage("player_not_found", "player" to args[3])))
                return true
            }
        } else if (sender is Player) {
            sender
        } else {
            sender.sendMessage(colorize("&c控制台必须指定玩家"))
            return true
        }

        val formattedTime = manager.formatDuration(durationMs)
        
        if (mode == "peace") {
            val success = manager.setPeace(target, durationMs, sender.name)
            if (success) {
                sender.sendMessage(colorize(manager.getMessage("set_success", 
                    "player" to target.name, 
                    "time" to formattedTime
                )))
                if (sender != target) {
                    target.sendMessage(colorize(manager.getMessage("gained", "time" to formattedTime)))
                }
            } else {
                sender.sendMessage(colorize(manager.getMessage("set_failed")))
            }
        } else {
            val success = manager.setNoSpawn(target, durationMs, sender.name)
            if (success) {
                sender.sendMessage(colorize(manager.getMessage("nospawn_set_success", 
                    "player" to target.name, 
                    "time" to formattedTime,
                    "radius" to manager.getNoSpawnRadius().toString()
                )))
                if (sender != target) {
                    target.sendMessage(colorize(manager.getMessage("nospawn_gained", 
                        "time" to formattedTime,
                        "radius" to manager.getNoSpawnRadius().toString()
                    )))
                }
            } else {
                sender.sendMessage(colorize(manager.getMessage("set_failed")))
            }
        }

        return true
    }

    /**
     * 列出所有和平/禁怪玩家
     * /tsl peace list [peace|nospawn]
     */
    private fun handleList(sender: CommandSender, args: Array<out String>): Boolean {
        if (!sender.hasPermission("tsl.peace.list")) {
            sender.sendMessage(colorize(manager.getMessage("no_permission")))
            return true
        }

        // 检查模式过滤
        val modeFilter = if (args.size >= 2) args[1].lowercase() else null
        
        val showPeace = modeFilter == null || modeFilter == "peace"
        val showNoSpawn = modeFilter == null || modeFilter == "nospawn"
        
        var totalCount = 0
        
        if (showPeace) {
            val entries = manager.listPeacePlayers()
            if (entries.isNotEmpty()) {
                sender.sendMessage(colorize("&a===== 和平模式 ====="))
                entries.forEach { entry ->
                    val remaining = manager.formatDuration(entry.remainingMs)
                    val online = if (Bukkit.getPlayer(entry.uuid)?.isOnline == true) "&a在线" else "&7离线"
                    sender.sendMessage(colorize("&f${entry.playerName} &7- $online &7- 剩余: &e$remaining"))
                }
                totalCount += entries.size
            } else if (modeFilter == "peace") {
                sender.sendMessage(colorize(manager.getMessage("list_empty")))
            }
        }
        
        if (showNoSpawn) {
            val entries = manager.listNoSpawnPlayers()
            if (entries.isNotEmpty()) {
                sender.sendMessage(colorize("&b===== 禁怪模式 (半径: ${manager.getNoSpawnRadius()}格) ====="))
                entries.forEach { entry ->
                    val remaining = manager.formatDuration(entry.remainingMs)
                    val online = if (Bukkit.getPlayer(entry.uuid)?.isOnline == true) "&a在线" else "&7离线"
                    sender.sendMessage(colorize("&f${entry.playerName} &7- $online &7- 剩余: &e$remaining"))
                }
                totalCount += entries.size
            } else if (modeFilter == "nospawn") {
                sender.sendMessage(colorize(manager.getMessage("nospawn_list_empty")))
            }
        }
        
        if (modeFilter == null && totalCount == 0) {
            sender.sendMessage(colorize("&7没有任何玩家处于和平/禁怪模式"))
        } else if (modeFilter == null) {
            sender.sendMessage(colorize("&7共 &f$totalCount &7人"))
        }

        return true
    }

    /**
     * 清除玩家和平/禁怪模式
     * /tsl peace clear <玩家|all> [peace|nospawn]
     * 不指定模式则清除两种
     */
    private fun handleClear(sender: CommandSender, args: Array<out String>): Boolean {
        if (!sender.hasPermission("tsl.peace.clear")) {
            sender.sendMessage(colorize(manager.getMessage("no_permission")))
            return true
        }

        if (args.size < 2) {
            sender.sendMessage(colorize("&c用法: /tsl peace clear <玩家|all> [peace|nospawn]"))
            sender.sendMessage(colorize("&7不指定模式则清除两种模式"))
            return true
        }

        val targetArg = args[1]
        val modeFilter = if (args.size >= 3) args[2].lowercase() else null
        val clearPeace = modeFilter == null || modeFilter == "peace"
        val clearNoSpawn = modeFilter == null || modeFilter == "nospawn"

        if (targetArg.equals("all", ignoreCase = true)) {
            var totalCleared = 0
            if (clearPeace) {
                val count = manager.clearAll()
                totalCleared += count
                if (modeFilter == "peace") {
                    sender.sendMessage(colorize(manager.getMessage("clear_all", "count" to count.toString())))
                }
            }
            if (clearNoSpawn) {
                val count = manager.clearAllNoSpawn()
                totalCleared += count
                if (modeFilter == "nospawn") {
                    sender.sendMessage(colorize(manager.getMessage("nospawn_clear_all", "count" to count.toString())))
                }
            }
            if (modeFilter == null) {
                sender.sendMessage(colorize("&a已清除所有玩家的和平/禁怪模式, 共 &f$totalCleared &a人"))
            }
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

        val playerName = offlinePlayer.name ?: targetArg
        var anyCleared = false
        
        if (clearPeace) {
            val success = manager.clearPeace(offlinePlayer.uniqueId)
            if (success) {
                anyCleared = true
                if (modeFilter == "peace") {
                    sender.sendMessage(colorize(manager.getMessage("clear_success", "player" to playerName)))
                }
                Bukkit.getPlayer(offlinePlayer.uniqueId)?.sendMessage(colorize(manager.getMessage("cleared")))
            } else if (modeFilter == "peace") {
                sender.sendMessage(colorize(manager.getMessage("not_peaceful", "player" to playerName)))
            }
        }
        
        if (clearNoSpawn) {
            val success = manager.clearNoSpawn(offlinePlayer.uniqueId)
            if (success) {
                anyCleared = true
                if (modeFilter == "nospawn") {
                    sender.sendMessage(colorize(manager.getMessage("nospawn_clear_success", "player" to playerName)))
                }
                Bukkit.getPlayer(offlinePlayer.uniqueId)?.sendMessage(colorize(manager.getMessage("nospawn_cleared")))
            } else if (modeFilter == "nospawn") {
                sender.sendMessage(colorize(manager.getMessage("not_nospawn", "player" to playerName)))
            }
        }
        
        if (modeFilter == null) {
            if (anyCleared) {
                sender.sendMessage(colorize("&a已清除 &f$playerName &a的所有模式"))
            } else {
                sender.sendMessage(colorize("&e$playerName &7没有任何模式需要清除"))
            }
        }

        return true
    }

    /**
     * 显示帮助
     */
    private fun showHelp(sender: CommandSender): Boolean {
        sender.sendMessage(colorize("&a===== 伪和平模式帮助 ====="))
        sender.sendMessage(colorize(""))
        sender.sendMessage(colorize("&e&l模式说明:"))
        sender.sendMessage(colorize("&a• peace &7- 怪物不会发现/锁定玩家"))
        sender.sendMessage(colorize("&b• nospawn &7- 玩家附近禁止刷怪 (半径: ${manager.getNoSpawnRadius()}格)"))
        sender.sendMessage(colorize(""))
        sender.sendMessage(colorize("&e&l命令:"))
        sender.sendMessage(colorize("&e/tsl peace set <peace|nospawn> <时间> [玩家]"))
        sender.sendMessage(colorize("&e/tsl peace list [peace|nospawn]"))
        sender.sendMessage(colorize("&e/tsl peace clear <玩家|all> [peace|nospawn]"))
        sender.sendMessage(colorize(""))
        sender.sendMessage(colorize("&7时间格式: 60 (秒), 5m (分), 2h (时), 1d (天)"))
        sender.sendMessage(colorize("&7不指定模式则操作两种模式"))
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
                "set" -> listOf("peace", "nospawn").filter { 
                    it.startsWith(args[1], ignoreCase = true) 
                }
                "list" -> listOf("peace", "nospawn").filter { 
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
                "set" -> listOf("60", "5m", "30m", "1h", "2h", "1d").filter { 
                    it.startsWith(args[2], ignoreCase = true) 
                }
                "clear" -> listOf("peace", "nospawn").filter { 
                    it.startsWith(args[2], ignoreCase = true) 
                }
                else -> emptyList()
            }
            4 -> when (args[0].lowercase()) {
                "set" -> Bukkit.getOnlinePlayers().map { it.name }.filter { 
                    it.startsWith(args[3], ignoreCase = true) 
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
