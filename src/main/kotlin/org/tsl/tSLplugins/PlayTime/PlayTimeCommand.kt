package org.tsl.tSLplugins.PlayTime

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.tsl.tSLplugins.SubCommandHandler

/**
 * 在线时长命令处理器
 * 
 * 命令：/tsl playtime [子命令]
 * 子命令：
 *   - (无) - 查看自己今日在线时长
 *   - check <玩家> - 查看指定玩家今日在线时长
 *   - top [数量] - 查看今日在线时长排行榜
 *   - help - 显示帮助
 */
class PlayTimeCommand(
    private val manager: PlayTimeManager
) : SubCommandHandler {

    override fun handle(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (!manager.isEnabled()) {
            sender.sendMessage("§c在线时长模块未启用")
            return true
        }

        when {
            args.isEmpty() -> handleSelf(sender)
            args[0].equals("check", true) -> handleCheck(sender, args)
            args[0].equals("top", true) -> handleTop(sender, args)
            args[0].equals("help", true) -> showHelp(sender)
            else -> showHelp(sender)
        }

        return true
    }

    /**
     * 查看自己的在线时长
     */
    private fun handleSelf(sender: CommandSender) {
        if (sender !is Player) {
            sender.sendMessage("§c该命令只能由玩家执行")
            return
        }

        val playTime = manager.getTodayPlayTimeFormatted(sender.uniqueId)
        val message = manager.getMessage("self",
            "%player%" to sender.name,
            "%time%" to playTime
        ).ifEmpty { "§e你今日的在线时长: §a$playTime" }
        
        sender.sendMessage(message)
    }

    /**
     * 查看指定玩家的在线时长
     */
    private fun handleCheck(sender: CommandSender, args: Array<out String>) {
        if (!sender.hasPermission("tsl.playtime.check")) {
            sender.sendMessage("§c你没有权限查看其他玩家的在线时长")
            return
        }

        if (args.size < 2) {
            sender.sendMessage("§c用法: /tsl playtime check <玩家>")
            return
        }

        val targetName = args[1]
        val target = Bukkit.getOfflinePlayer(targetName)
        
        if (!target.hasPlayedBefore() && !target.isOnline) {
            sender.sendMessage("§c玩家 $targetName 不存在或从未加入过服务器")
            return
        }

        val playTime = manager.getTodayPlayTimeFormatted(target.uniqueId)
        val displayName = target.name ?: targetName
        
        val message = manager.getMessage("check",
            "%player%" to displayName,
            "%time%" to playTime
        ).ifEmpty { "§e${displayName} 今日的在线时长: §a$playTime" }
        
        sender.sendMessage(message)
    }

    /**
     * 查看排行榜
     */
    private fun handleTop(sender: CommandSender, args: Array<out String>) {
        if (!sender.hasPermission("tsl.playtime.top")) {
            sender.sendMessage("§c你没有权限查看在线时长排行榜")
            return
        }

        val limit = if (args.size > 1) {
            args[1].toIntOrNull()?.coerceIn(1, 20) ?: 10
        } else {
            10
        }

        val leaderboard = manager.getTodayLeaderboard(limit)
        
        if (leaderboard.isEmpty()) {
            sender.sendMessage("§e今日暂无在线时长数据")
            return
        }

        val headerMessage = manager.getMessage("top-header")
            .ifEmpty { "§6========== 今日在线时长排行榜 ==========" }
        sender.sendMessage(headerMessage)

        leaderboard.forEachIndexed { index, (uuid, seconds) ->
            val playerName = Bukkit.getOfflinePlayer(uuid).name ?: "Unknown"
            val playTime = manager.formatDuration(seconds)
            
            val entryMessage = manager.getMessage("top-entry",
                "%rank%" to (index + 1).toString(),
                "%player%" to playerName,
                "%time%" to playTime
            ).ifEmpty { "§e${index + 1}. §a$playerName §7- §f$playTime" }
            
            sender.sendMessage(entryMessage)
        }

        val footerMessage = manager.getMessage("top-footer")
            .ifEmpty { "§6=======================================" }
        sender.sendMessage(footerMessage)
    }

    /**
     * 显示帮助信息
     */
    private fun showHelp(sender: CommandSender) {
        sender.sendMessage("§6========== 在线时长命令帮助 ==========")
        sender.sendMessage("§e/tsl playtime §7- 查看自己今日在线时长")
        sender.sendMessage("§e/tsl playtime check <玩家> §7- 查看指定玩家在线时长")
        sender.sendMessage("§e/tsl playtime top [数量] §7- 查看今日排行榜")
        sender.sendMessage("§e/tsl playtime help §7- 显示此帮助")
        sender.sendMessage("§6======================================")
        sender.sendMessage("")
        sender.sendMessage("§7PAPI 变量:")
        sender.sendMessage("§e%tsl_playtime% §7- 今日在线时长（格式化）")
        sender.sendMessage("§e%tsl_playtime_seconds% §7- 今日在线时长（秒）")
        sender.sendMessage("§e%tsl_playtime_minutes% §7- 今日在线时长（分钟）")
        sender.sendMessage("§e%tsl_playtime_hours% §7- 今日在线时长（小时）")
    }

    override fun tabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): List<String> {
        if (!manager.isEnabled()) return emptyList()

        return when (args.size) {
            1 -> {
                listOf("check", "top", "help").filter { 
                    it.startsWith(args[0], ignoreCase = true) 
                }
            }
            2 -> {
                when (args[0].lowercase()) {
                    "check" -> {
                        Bukkit.getOnlinePlayers()
                            .map { it.name }
                            .filter { it.startsWith(args[1], ignoreCase = true) }
                    }
                    "top" -> {
                        listOf("5", "10", "15", "20").filter {
                            it.startsWith(args[1])
                        }
                    }
                    else -> emptyList()
                }
            }
            else -> emptyList()
        }
    }

    override fun getDescription(): String {
        return "在线时长统计"
    }
}
