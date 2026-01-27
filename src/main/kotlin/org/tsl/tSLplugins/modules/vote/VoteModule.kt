package org.tsl.tSLplugins.modules.vote

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.tsl.tSLplugins.SubCommandHandler
import org.tsl.tSLplugins.core.AbstractModule
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.ceil

/**
 * Vote 模块 - 指令投票系统
 */
class VoteModule : AbstractModule() {

    override val id = "vote"
    override val configPath = "vote"

    private var cooldownSeconds = 60
    private var defaultDurationSeconds = 30
    private var defaultPercentage = 0.5

    private val voteConfigs = mutableMapOf<String, VoteConfig>()
    private var activeVote: ActiveVote? = null
    private val cooldowns = ConcurrentHashMap<String, Long>()

    override fun doEnable() {
        loadVoteConfig()
    }

    override fun doReload() {
        loadVoteConfig()
    }

    override fun getCommandHandler(): SubCommandHandler = VoteModuleCommand(this)
    override fun getDescription(): String = "指令投票系统"

    private fun loadVoteConfig() {
        cooldownSeconds = getConfigInt("cooldown-seconds", 60)
        defaultDurationSeconds = getConfigInt("default-duration-seconds", 30)
        defaultPercentage = getConfigDouble("default-percentage", 0.5)

        voteConfigs.clear()
        context.plugin.config.getConfigurationSection("vote.votes")?.getKeys(false)?.forEach { key ->
            val section = context.plugin.config.getConfigurationSection("vote.votes.$key") ?: return@forEach
            val command = section.getString("command") ?: return@forEach
            voteConfigs[key] = VoteConfig(
                key = key,
                command = command,
                percentage = section.getDouble("percentage", defaultPercentage),
                duration = section.getInt("duration", defaultDurationSeconds),
                description = section.getString("description", key) ?: key,
                permission = section.getString("permission", "") ?: ""
            )
        }
        logInfo("已加载 ${voteConfigs.size} 个投票配置")
    }

    fun startVote(initiator: Player, key: String): VoteResult {
        if (activeVote != null) return VoteResult.ALREADY_ACTIVE
        val voteConfig = voteConfigs[key] ?: return VoteResult.INVALID_KEY
        if (voteConfig.permission.isNotEmpty() && !initiator.hasPermission(voteConfig.permission)) return VoteResult.NO_PERMISSION

        val now = System.currentTimeMillis()
        val lastEnd = cooldowns[key] ?: 0L
        val cooldownMs = cooldownSeconds * 1000L
        if (now < lastEnd + cooldownMs) {
            return VoteResult.ON_COOLDOWN.also { it.cooldownRemaining = (lastEnd + cooldownMs - now) / 1000 }
        }

        val vote = ActiveVote(key, voteConfig, initiator.uniqueId, initiator.name, now, now + voteConfig.duration * 1000L)
        activeVote = vote
        broadcastVoteStart(vote)
        startCountdown(vote)
        return VoteResult.SUCCESS
    }

    fun castVote(player: Player): VoteResult {
        val vote = activeVote ?: return VoteResult.NO_ACTIVE_VOTE
        if (vote.voters.contains(player.uniqueId)) return VoteResult.ALREADY_VOTED
        vote.voters.add(player.uniqueId)
        player.sendMessage(colorize(getModuleMessage("vote_cast")))
        checkVotePass(vote)
        return VoteResult.SUCCESS
    }

    private fun checkVotePass(vote: ActiveVote) {
        val onlineCount = Bukkit.getOnlinePlayers().size
        val required = ceil(onlineCount * vote.config.percentage).toInt().coerceAtLeast(1)
        if (vote.voters.size >= required) passVote(vote)
    }

    private fun passVote(vote: ActiveVote) {
        activeVote = null
        cooldowns[vote.key] = System.currentTimeMillis()
        val onlineCount = Bukkit.getOnlinePlayers().size
        val required = ceil(onlineCount * vote.config.percentage).toInt().coerceAtLeast(1)
        Bukkit.broadcast(Component.text(colorize(getModuleMessage("vote_passed",
            "key" to vote.config.description, "votes" to vote.voters.size.toString(), "required" to required.toString()))))
        Bukkit.getGlobalRegionScheduler().run(context.plugin) { _ ->
            val command = vote.config.command.replace("{initiator}", vote.initiatorName).replace("{votes}", vote.voters.size.toString())
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)
        }
    }

    private fun failVote(vote: ActiveVote) {
        activeVote = null
        cooldowns[vote.key] = System.currentTimeMillis()
        val onlineCount = Bukkit.getOnlinePlayers().size
        val required = ceil(onlineCount * vote.config.percentage).toInt().coerceAtLeast(1)
        Bukkit.broadcast(Component.text(colorize(getModuleMessage("vote_failed",
            "key" to vote.config.description, "votes" to vote.voters.size.toString(), "required" to required.toString()))))
    }

    private fun broadcastVoteStart(vote: ActiveVote) {
        val onlineCount = Bukkit.getOnlinePlayers().size
        val required = ceil(onlineCount * vote.config.percentage).toInt().coerceAtLeast(1)
        val prefix = Component.text(colorize(getModuleMessage("vote_broadcast_prefix",
            "initiator" to vote.initiatorName, "key" to vote.config.description,
            "duration" to vote.config.duration.toString(), "required" to required.toString())))
        val agreeButton = Component.text(" [赞成] ").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD)
            .clickEvent(ClickEvent.runCommand("/tsl vote yes")).hoverEvent(HoverEvent.showText(Component.text("点击投票赞成")))
        val suffix = Component.text(colorize(getModuleMessage("vote_broadcast_suffix")))
        Bukkit.getOnlinePlayers().forEach { it.sendMessage(prefix.append(agreeButton).append(suffix)) }
    }

    private fun startCountdown(vote: ActiveVote) {
        Bukkit.getGlobalRegionScheduler().runDelayed(context.plugin, { _ ->
            if (activeVote == vote) failVote(vote)
        }, vote.config.duration * 20L)
    }

    fun getActiveVote(): ActiveVote? = activeVote
    fun getVoteConfigs(): Map<String, VoteConfig> = voteConfigs.toMap()
    fun forceEnd(): Boolean {
        val vote = activeVote ?: return false
        activeVote = null
        cooldowns[vote.key] = System.currentTimeMillis()
        Bukkit.broadcast(Component.text(colorize(getModuleMessage("vote_cancelled"))))
        return true
    }

    fun getModuleMessage(key: String, vararg replacements: Pair<String, String>): String = getMessage(key, *replacements)
    private fun colorize(text: String): String = text.replace("&", "§")

    data class VoteConfig(val key: String, val command: String, val percentage: Double, val duration: Int, val description: String, val permission: String)
    data class ActiveVote(val key: String, val config: VoteConfig, val initiator: UUID, val initiatorName: String,
                          val startTime: Long, val endTime: Long, val voters: MutableSet<UUID> = ConcurrentHashMap.newKeySet())
    enum class VoteResult { SUCCESS, DISABLED, ALREADY_ACTIVE, NO_ACTIVE_VOTE, INVALID_KEY, NO_PERMISSION, ON_COOLDOWN, ALREADY_VOTED; var cooldownRemaining: Long = 0 }
}

class VoteModuleCommand(private val module: VoteModule) : SubCommandHandler {
    override fun handle(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!module.isEnabled()) { sender.sendMessage(colorize(module.getModuleMessage("disabled"))); return true }
        if (args.isEmpty()) return showHelp(sender)
        return when (args[0].lowercase()) {
            "start" -> handleStart(sender, args)
            "yes", "agree", "y" -> handleYes(sender)
            "status" -> handleStatus(sender)
            "list" -> handleList(sender)
            "cancel" -> handleCancel(sender)
            else -> showHelp(sender)
        }
    }

    private fun handleStart(sender: CommandSender, args: Array<out String>): Boolean {
        if (sender !is Player) { sender.sendMessage("§c该命令只能由玩家执行"); return true }
        if (!sender.hasPermission("tsl.vote.start")) { sender.sendMessage(colorize(module.getModuleMessage("no_permission"))); return true }
        if (args.size < 2) { sender.sendMessage("§c用法: /tsl vote start <投票类型>"); return true }
        when (val result = module.startVote(sender, args[1].lowercase())) {
            VoteModule.VoteResult.ALREADY_ACTIVE -> sender.sendMessage(colorize(module.getModuleMessage("already_active")))
            VoteModule.VoteResult.INVALID_KEY -> sender.sendMessage(colorize(module.getModuleMessage("invalid_key", "key" to args[1])))
            VoteModule.VoteResult.NO_PERMISSION -> sender.sendMessage(colorize(module.getModuleMessage("no_permission")))
            VoteModule.VoteResult.ON_COOLDOWN -> sender.sendMessage(colorize(module.getModuleMessage("on_cooldown", "seconds" to result.cooldownRemaining.toString())))
            else -> {}
        }
        return true
    }

    private fun handleYes(sender: CommandSender): Boolean {
        if (sender !is Player) { sender.sendMessage("§c该命令只能由玩家执行"); return true }
        when (module.castVote(sender)) {
            VoteModule.VoteResult.NO_ACTIVE_VOTE -> sender.sendMessage(colorize(module.getModuleMessage("no_active_vote")))
            VoteModule.VoteResult.ALREADY_VOTED -> sender.sendMessage(colorize(module.getModuleMessage("already_voted")))
            else -> {}
        }
        return true
    }

    private fun handleStatus(sender: CommandSender): Boolean {
        val vote = module.getActiveVote()
        if (vote == null) { sender.sendMessage(colorize(module.getModuleMessage("no_active_vote"))); return true }
        val onlineCount = Bukkit.getOnlinePlayers().size
        val required = ceil(onlineCount * vote.config.percentage).toInt().coerceAtLeast(1)
        val remaining = ((vote.endTime - System.currentTimeMillis()) / 1000).coerceAtLeast(0)
        sender.sendMessage("§a===== 当前投票 =====")
        sender.sendMessage("§7投票项目: §f${vote.config.description}")
        sender.sendMessage("§7发起者: §f${vote.initiatorName}")
        sender.sendMessage("§7当前票数: §a${vote.voters.size} §7/ §e$required")
        sender.sendMessage("§7剩余时间: §f${remaining}秒")
        return true
    }

    private fun handleList(sender: CommandSender): Boolean {
        val configs = module.getVoteConfigs()
        if (configs.isEmpty()) { sender.sendMessage("§7没有配置任何投票项目"); return true }
        sender.sendMessage("§a===== 可用投票 =====")
        configs.forEach { (key, config) ->
            val permStatus = if (config.permission.isEmpty() || sender.hasPermission(config.permission)) "§a✓" else "§c✗"
            sender.sendMessage("$permStatus §e$key §7- ${config.description} §8(${config.duration}秒, ${(config.percentage * 100).toInt()}%)")
        }
        return true
    }

    private fun handleCancel(sender: CommandSender): Boolean {
        if (!sender.hasPermission("tsl.vote.admin")) { sender.sendMessage(colorize(module.getModuleMessage("no_permission"))); return true }
        if (module.forceEnd()) sender.sendMessage(colorize(module.getModuleMessage("vote_force_cancelled")))
        else sender.sendMessage(colorize(module.getModuleMessage("no_active_vote")))
        return true
    }

    private fun showHelp(sender: CommandSender): Boolean {
        sender.sendMessage("§a===== 指令投票帮助 =====")
        sender.sendMessage("§e/tsl vote start <类型> §7- 发起投票")
        sender.sendMessage("§e/tsl vote yes §7- 投票赞成")
        sender.sendMessage("§e/tsl vote status §7- 查看当前投票")
        sender.sendMessage("§e/tsl vote list §7- 列出可用投票")
        if (sender.hasPermission("tsl.vote.admin")) sender.sendMessage("§e/tsl vote cancel §7- 取消当前投票")
        return true
    }

    override fun tabComplete(sender: CommandSender, command: Command, label: String, args: Array<out String>): List<String> {
        return when (args.size) {
            1 -> listOf("start", "yes", "status", "list").filter { it.startsWith(args[0], ignoreCase = true) }
            2 -> if (args[0].lowercase() == "start") module.getVoteConfigs().keys.filter { it.startsWith(args[1], ignoreCase = true) } else emptyList()
            else -> emptyList()
        }
    }

    override fun getDescription(): String = "指令投票系统"
    private fun colorize(text: String): String = text.replace("&", "§")
}
