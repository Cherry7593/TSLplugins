package org.tsl.tSLplugins.Vote

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.tsl.tSLplugins.SubCommandHandler
import kotlin.math.ceil

/**
 * 指令投票命令处理器
 * /tsl vote start <key> - 发起投票
 * /tsl vote yes - 投票赞成
 * /tsl vote status - 查看当前投票状态
 * /tsl vote list - 列出可用投票
 * /tsl vote cancel - 取消当前投票（管理员）
 */
class VoteCommand(private val manager: VoteManager) : SubCommandHandler {

    override fun handle(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!manager.isEnabled()) {
            sender.sendMessage(colorize(manager.getMessage("disabled")))
            return true
        }

        if (args.isEmpty()) {
            return showHelp(sender)
        }

        return when (args[0].lowercase()) {
            "start" -> handleStart(sender, args)
            "yes", "agree", "y" -> handleYes(sender)
            "status" -> handleStatus(sender)
            "list" -> handleList(sender)
            "cancel", "end" -> handleCancel(sender)
            "help" -> showHelp(sender)
            else -> showHelp(sender)
        }
    }

    /**
     * 发起投票
     * /tsl vote start <key>
     */
    private fun handleStart(sender: CommandSender, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage(colorize("&c该命令只能由玩家执行"))
            return true
        }

        if (!sender.hasPermission("tsl.vote.start")) {
            sender.sendMessage(colorize(manager.getMessage("no_permission")))
            return true
        }

        if (args.size < 2) {
            sender.sendMessage(colorize("&c用法: /tsl vote start <投票类型>"))
            sender.sendMessage(colorize("&7使用 /tsl vote list 查看可用投票"))
            return true
        }

        val key = args[1].lowercase()
        
        when (val result = manager.startVote(sender, key)) {
            VoteManager.VoteResult.SUCCESS -> {
                // 成功消息已在 manager 中广播
            }
            VoteManager.VoteResult.ALREADY_ACTIVE -> {
                sender.sendMessage(colorize(manager.getMessage("already_active")))
            }
            VoteManager.VoteResult.INVALID_KEY -> {
                sender.sendMessage(colorize(manager.getMessage("invalid_key", "key" to key)))
            }
            VoteManager.VoteResult.NO_PERMISSION -> {
                sender.sendMessage(colorize(manager.getMessage("no_permission")))
            }
            VoteManager.VoteResult.ON_COOLDOWN -> {
                sender.sendMessage(colorize(manager.getMessage("on_cooldown", "seconds" to result.cooldownRemaining.toString())))
            }
            else -> {}
        }

        return true
    }

    /**
     * 投票赞成
     * /tsl vote yes
     */
    private fun handleYes(sender: CommandSender): Boolean {
        if (sender !is Player) {
            sender.sendMessage(colorize("&c该命令只能由玩家执行"))
            return true
        }

        when (manager.castVote(sender)) {
            VoteManager.VoteResult.SUCCESS -> {
                // 成功消息已在 manager 中发送
            }
            VoteManager.VoteResult.NO_ACTIVE_VOTE -> {
                sender.sendMessage(colorize(manager.getMessage("no_active_vote")))
            }
            VoteManager.VoteResult.ALREADY_VOTED -> {
                sender.sendMessage(colorize(manager.getMessage("already_voted")))
            }
            else -> {}
        }

        return true
    }

    /**
     * 查看投票状态
     * /tsl vote status
     */
    private fun handleStatus(sender: CommandSender): Boolean {
        val vote = manager.getActiveVote()
        
        if (vote == null) {
            sender.sendMessage(colorize(manager.getMessage("no_active_vote")))
            return true
        }

        val onlineCount = Bukkit.getOnlinePlayers().size
        val required = ceil(onlineCount * vote.config.percentage).toInt().coerceAtLeast(1)
        val remaining = ((vote.endTime - System.currentTimeMillis()) / 1000).coerceAtLeast(0)

        sender.sendMessage(colorize("&a===== 当前投票 ====="))
        sender.sendMessage(colorize("&7投票项目: &f${vote.config.description}"))
        sender.sendMessage(colorize("&7发起者: &f${vote.initiatorName}"))
        sender.sendMessage(colorize("&7当前票数: &a${vote.voters.size} &7/ &e$required"))
        sender.sendMessage(colorize("&7剩余时间: &f${remaining}秒"))

        return true
    }

    /**
     * 列出可用投票
     * /tsl vote list
     */
    private fun handleList(sender: CommandSender): Boolean {
        val configs = manager.getVoteConfigs()
        
        if (configs.isEmpty()) {
            sender.sendMessage(colorize("&7没有配置任何投票项目"))
            return true
        }

        sender.sendMessage(colorize("&a===== 可用投票 ====="))
        configs.forEach { (key, config) ->
            val permStatus = if (config.permission.isEmpty() || sender.hasPermission(config.permission)) {
                "&a✓"
            } else {
                "&c✗"
            }
            sender.sendMessage(colorize("$permStatus &e$key &7- ${config.description} &8(${config.duration}秒, ${(config.percentage * 100).toInt()}%)"))
        }

        return true
    }

    /**
     * 取消投票
     * /tsl vote cancel
     */
    private fun handleCancel(sender: CommandSender): Boolean {
        if (!sender.hasPermission("tsl.vote.admin")) {
            sender.sendMessage(colorize(manager.getMessage("no_permission")))
            return true
        }

        if (manager.forceEnd()) {
            sender.sendMessage(colorize(manager.getMessage("vote_force_cancelled")))
        } else {
            sender.sendMessage(colorize(manager.getMessage("no_active_vote")))
        }

        return true
    }

    /**
     * 显示帮助
     */
    private fun showHelp(sender: CommandSender): Boolean {
        sender.sendMessage(colorize("&a===== 指令投票帮助 ====="))
        sender.sendMessage(colorize("&e/tsl vote start <类型> &7- 发起投票"))
        sender.sendMessage(colorize("&e/tsl vote yes &7- 投票赞成"))
        sender.sendMessage(colorize("&e/tsl vote status &7- 查看当前投票"))
        sender.sendMessage(colorize("&e/tsl vote list &7- 列出可用投票"))
        if (sender.hasPermission("tsl.vote.admin")) {
            sender.sendMessage(colorize("&e/tsl vote cancel &7- 取消当前投票"))
        }
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
            1 -> {
                val suggestions = mutableListOf("start", "yes", "status", "list", "help")
                if (sender.hasPermission("tsl.vote.admin")) {
                    suggestions.add("cancel")
                }
                suggestions.filter { it.startsWith(args[0], ignoreCase = true) }
            }
            2 -> when (args[0].lowercase()) {
                "start" -> {
                    manager.getVoteConfigs().keys
                        .filter { key ->
                            val config = manager.getVoteConfigs()[key]
                            config?.permission?.isEmpty() == true || sender.hasPermission(config?.permission ?: "")
                        }
                        .filter { it.startsWith(args[1], ignoreCase = true) }
                }
                else -> emptyList()
            }
            else -> emptyList()
        }
    }

    override fun getDescription(): String {
        return "指令投票系统"
    }

    private fun colorize(text: String): String {
        return text.replace("&", "§")
    }
}
