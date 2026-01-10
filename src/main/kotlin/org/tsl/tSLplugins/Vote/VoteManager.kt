package org.tsl.tSLplugins.Vote

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.tsl.tSLplugins.TSLplugins
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.ceil

/**
 * 指令投票管理器
 */
class VoteManager(private val plugin: JavaPlugin) {

    private var enabled = false
    private var cooldownSeconds = 60
    private var defaultDurationSeconds = 30
    private var defaultPercentage = 0.5

    private val msg get() = (plugin as TSLplugins).messageManager

    // 投票配置: key -> VoteConfig
    private val voteConfigs = mutableMapOf<String, VoteConfig>()
    
    // 当前进行中的投票
    private var activeVote: ActiveVote? = null
    
    // 冷却时间记录: key -> lastVoteEndTime
    private val cooldowns = ConcurrentHashMap<String, Long>()

    init {
        loadConfig()
    }

    fun loadConfig() {
        val config = plugin.config
        enabled = config.getBoolean("vote.enabled", false)
        cooldownSeconds = config.getInt("vote.cooldown-seconds", 60)
        defaultDurationSeconds = config.getInt("vote.default-duration-seconds", 30)
        defaultPercentage = config.getDouble("vote.default-percentage", 0.5)

        voteConfigs.clear()
        val votesSection = config.getConfigurationSection("vote.votes")
        if (votesSection != null) {
            for (key in votesSection.getKeys(false)) {
                val section = votesSection.getConfigurationSection(key) ?: continue
                val command = section.getString("command") ?: continue
                val percentage = section.getDouble("percentage", defaultPercentage)
                val duration = section.getInt("duration", defaultDurationSeconds)
                val description = section.getString("description", key)
                val permission = section.getString("permission", "")
                
                voteConfigs[key] = VoteConfig(key, command, percentage, duration, description ?: key, permission ?: "")
            }
        }

        if (enabled) {
            plugin.logger.info("[Vote] 已加载 ${voteConfigs.size} 个投票配置")
        }
    }

    /**
     * 发起投票
     */
    fun startVote(initiator: Player, key: String): VoteResult {
        if (!enabled) return VoteResult.DISABLED
        
        // 检查是否有投票进行中
        if (activeVote != null) return VoteResult.ALREADY_ACTIVE
        
        // 检查投票 key 是否存在
        val voteConfig = voteConfigs[key] ?: return VoteResult.INVALID_KEY
        
        // 检查权限
        if (voteConfig.permission.isNotEmpty() && !initiator.hasPermission(voteConfig.permission)) {
            return VoteResult.NO_PERMISSION
        }
        
        // 检查冷却
        val lastEnd = cooldowns[key] ?: 0L
        val now = System.currentTimeMillis()
        val cooldownMs = cooldownSeconds * 1000L
        if (now < lastEnd + cooldownMs) {
            val remaining = (lastEnd + cooldownMs - now) / 1000
            return VoteResult.ON_COOLDOWN.also { it.cooldownRemaining = remaining }
        }
        
        // 创建投票
        val vote = ActiveVote(
            key = key,
            config = voteConfig,
            initiator = initiator.uniqueId,
            initiatorName = initiator.name,
            startTime = now,
            endTime = now + voteConfig.duration * 1000L
        )
        activeVote = vote
        
        // 广播投票开始
        broadcastVoteStart(vote)
        
        // 启动倒计时任务
        startCountdown(vote)
        
        plugin.logger.info("[Vote] ${initiator.name} 发起投票: $key")
        return VoteResult.SUCCESS
    }

    /**
     * 玩家投票
     */
    fun castVote(player: Player): VoteResult {
        if (!enabled) return VoteResult.DISABLED
        
        val vote = activeVote ?: return VoteResult.NO_ACTIVE_VOTE
        
        // 检查是否已投票
        if (vote.voters.contains(player.uniqueId)) {
            return VoteResult.ALREADY_VOTED
        }
        
        // 记录投票
        vote.voters.add(player.uniqueId)
        
        // 发送确认消息
        player.sendMessage(colorize(getMessage("vote_cast")))
        
        // 检查是否通过
        checkVotePass(vote)
        
        return VoteResult.SUCCESS
    }

    /**
     * 检查投票是否通过
     */
    private fun checkVotePass(vote: ActiveVote) {
        val onlineCount = Bukkit.getOnlinePlayers().size
        val required = ceil(onlineCount * vote.config.percentage).toInt().coerceAtLeast(1)
        val current = vote.voters.size
        
        if (current >= required) {
            // 投票通过
            passVote(vote)
        }
    }

    /**
     * 投票通过
     */
    private fun passVote(vote: ActiveVote) {
        activeVote = null
        cooldowns[vote.key] = System.currentTimeMillis()
        
        // 广播通过消息
        val onlineCount = Bukkit.getOnlinePlayers().size
        val required = ceil(onlineCount * vote.config.percentage).toInt().coerceAtLeast(1)
        
        Bukkit.broadcast(Component.text(colorize(getMessage("vote_passed",
            "key" to vote.config.description,
            "votes" to vote.voters.size.toString(),
            "required" to required.toString()
        ))))
        
        // 执行控制台命令
        Bukkit.getGlobalRegionScheduler().run(plugin) { _ ->
            val command = vote.config.command
                .replace("{initiator}", vote.initiatorName)
                .replace("{votes}", vote.voters.size.toString())
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)
            plugin.logger.info("[Vote] 执行命令: $command")
        }
    }

    /**
     * 投票失败
     */
    private fun failVote(vote: ActiveVote) {
        activeVote = null
        cooldowns[vote.key] = System.currentTimeMillis()
        
        val onlineCount = Bukkit.getOnlinePlayers().size
        val required = ceil(onlineCount * vote.config.percentage).toInt().coerceAtLeast(1)
        
        Bukkit.broadcast(Component.text(colorize(getMessage("vote_failed",
            "key" to vote.config.description,
            "votes" to vote.voters.size.toString(),
            "required" to required.toString()
        ))))
    }

    /**
     * 广播投票开始
     */
    private fun broadcastVoteStart(vote: ActiveVote) {
        val onlineCount = Bukkit.getOnlinePlayers().size
        val required = ceil(onlineCount * vote.config.percentage).toInt().coerceAtLeast(1)
        
        // 创建可点击的消息
        val prefix = Component.text(colorize(getMessage("vote_broadcast_prefix",
            "initiator" to vote.initiatorName,
            "key" to vote.config.description,
            "duration" to vote.config.duration.toString(),
            "required" to required.toString()
        )))
        
        val agreeButton = Component.text(" [赞成] ")
            .color(NamedTextColor.GREEN)
            .decorate(TextDecoration.BOLD)
            .clickEvent(ClickEvent.runCommand("/tsl vote yes"))
            .hoverEvent(HoverEvent.showText(Component.text("点击投票赞成")))
        
        val suffix = Component.text(colorize(getMessage("vote_broadcast_suffix")))
        
        val message = prefix.append(agreeButton).append(suffix)
        
        Bukkit.getOnlinePlayers().forEach { player ->
            player.sendMessage(message)
        }
    }

    /**
     * 启动倒计时
     */
    private fun startCountdown(vote: ActiveVote) {
        val durationTicks = vote.config.duration * 20L
        
        Bukkit.getGlobalRegionScheduler().runDelayed(plugin, { _ ->
            // 检查投票是否还在进行
            if (activeVote == vote) {
                failVote(vote)
            }
        }, durationTicks)
    }

    /**
     * 获取当前投票状态
     */
    fun getActiveVote(): ActiveVote? = activeVote

    /**
     * 获取所有投票配置
     */
    fun getVoteConfigs(): Map<String, VoteConfig> = voteConfigs.toMap()

    /**
     * 强制结束当前投票
     */
    fun forceEnd(): Boolean {
        val vote = activeVote ?: return false
        activeVote = null
        cooldowns[vote.key] = System.currentTimeMillis()
        
        Bukkit.broadcast(Component.text(colorize(getMessage("vote_cancelled"))))
        return true
    }

    fun isEnabled() = enabled

    fun reload() {
        loadConfig()
        plugin.logger.info("[Vote] 配置已重载")
    }

    fun getMessage(key: String, vararg replacements: Pair<String, String>): String {
        return msg.getModule("vote", key, *replacements)
    }

    private fun colorize(text: String): String {
        return text.replace("&", "§")
    }

    /**
     * 投票配置
     */
    data class VoteConfig(
        val key: String,
        val command: String,
        val percentage: Double,
        val duration: Int,
        val description: String,
        val permission: String
    )

    /**
     * 活跃的投票
     */
    data class ActiveVote(
        val key: String,
        val config: VoteConfig,
        val initiator: UUID,
        val initiatorName: String,
        val startTime: Long,
        val endTime: Long,
        val voters: MutableSet<UUID> = ConcurrentHashMap.newKeySet()
    )

    /**
     * 投票结果
     */
    enum class VoteResult {
        SUCCESS,
        DISABLED,
        ALREADY_ACTIVE,
        NO_ACTIVE_VOTE,
        INVALID_KEY,
        NO_PERMISSION,
        ON_COOLDOWN,
        ALREADY_VOTED;
        
        var cooldownRemaining: Long = 0
    }
}
