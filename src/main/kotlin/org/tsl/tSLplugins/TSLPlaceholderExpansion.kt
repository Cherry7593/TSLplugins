package org.tsl.tSLplugins

import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.OfflinePlayer
import org.bukkit.plugin.java.JavaPlugin
import org.tsl.tSLplugins.Advancement.AdvancementCount
import org.tsl.tSLplugins.Ping.PingManager
import org.tsl.tSLplugins.Kiss.KissManager
import org.tsl.tSLplugins.Ride.RideManager
import org.tsl.tSLplugins.Toss.TossManager
import org.tsl.tSLplugins.BlockStats.BlockStatsManager
import org.tsl.tSLplugins.NewbieTag.NewbieTagManager
import org.tsl.tSLplugins.RandomVariable.RandomVariableManager
import org.tsl.tSLplugins.PapiAlias.PapiAliasManager
import org.tsl.tSLplugins.PlayTime.PlayTimeManager

/**
 * TSLplugins PlaceholderAPI 扩展
 *
 * 这是一个核心系统文件，位于根目录，整合了所有模块的 PAPI 变量。
 *
 * 支持的变量：
 * - %tsl_ping% - 服务器平均延迟
 * - %tsl_adv_count% - 玩家成就数量
 * - %tsl_kiss_count% - 亲吻次数
 * - %tsl_kissed_count% - 被亲吻次数
 * - %tsl_kiss_toggle% - Kiss 功能开关状态
 * - %tsl_ride_toggle% - Ride 功能开关状态
 * - %tsl_toss_toggle% - Toss 功能开关状态
 * - %tsl_toss_velocity% - Toss 投掷速度
 * - %tsl_blocks_placed_total% - 玩家放置方块总数
 * - %tsl_newbie_tag% - 萌新标志（根据在线时长）
 * - %tsl_random_变量名% - 混合分布随机数
 * - %tsl_alias_变量名% - 变量值映射（将原值映射为简写）
 * - %tsl_playtime% - 今日在线时长（格式化）
 * - %tsl_playtime_seconds% - 今日在线时长（秒）
 * - %tsl_playtime_minutes% - 今日在线时长（分钟）
 * - %tsl_playtime_hours% - 今日在线时长（小时，带小数）
 * - %tsl_bind% - QQ 绑定状态 (true/false)
 * - %tsl_bind_qq% - 绑定的 QQ 号码
 */
class TSLPlaceholderExpansion(
    private val plugin: JavaPlugin,
    private val countHandler: AdvancementCount,
    private val pingManager: PingManager?,
    private val kissManager: KissManager?,
    private val rideManager: RideManager?,
    private val tossManager: TossManager?,
    private val blockStatsManager: BlockStatsManager?,
    private val newbieTagManager: NewbieTagManager?,
    private val randomVariableManager: RandomVariableManager?,
    private val papiAliasManager: PapiAliasManager? = null,
    private val playTimeManager: PlayTimeManager? = null,
    private val playerDataManager: PlayerDataManager? = null
) : PlaceholderExpansion() {

    override fun getIdentifier(): String = "tsl"

    override fun getAuthor(): String = "TSL"

    override fun getVersion(): String = "1.0"

    override fun persist(): Boolean = true

    override fun onRequest(player: OfflinePlayer?, params: String): String? {
        // === Ping 变量 ===
        // %tsl_ping% - 不需要玩家，显示服务器平均延迟
        if (params.equals("ping", ignoreCase = true)) {
            return pingManager?.let {
                String.format("%.1f", it.getAveragePing())
            } ?: "N/A"
        }

        // 以下变量需要玩家
        if (player == null) return null

        // === Advancement 变量 ===
        // %tsl_adv_count% - 玩家成就数量
        if (params.equals("adv_count", ignoreCase = true)) {
            val onlinePlayer = player.player ?: return null
            return countHandler.getAdvancementCount(onlinePlayer).toString()
        }

        // === Kiss 变量 ===
        if (kissManager != null) {
            when (params) {
                "kiss_count" -> {
                    val onlinePlayer = player.player ?: return null
                    return kissManager.getKissCount(onlinePlayer.uniqueId).toString()
                }
                "kissed_count" -> {
                    val onlinePlayer = player.player ?: return null
                    return kissManager.getKissedCount(onlinePlayer.uniqueId).toString()
                }
                "kiss_toggle" -> {
                    val onlinePlayer = player.player ?: return null
                    return if (kissManager.isPlayerEnabled(onlinePlayer)) "启用" else "禁用"
                }
            }
        }

        // === Ride 变量 ===
        if (rideManager != null && params == "ride_toggle") {
            val onlinePlayer = player.player ?: return null
            return if (rideManager.isPlayerEnabled(onlinePlayer)) "启用" else "禁用"
        }

        // === Toss 变量 ===
        if (tossManager != null) {
            when (params) {
                "toss_toggle" -> {
                    val onlinePlayer = player.player ?: return null
                    return if (tossManager.isPlayerEnabled(onlinePlayer)) "启用" else "禁用"
                }
                "toss_velocity" -> {
                    val onlinePlayer = player.player ?: return null
                    return String.format("%.1f", tossManager.getPlayerThrowVelocity(onlinePlayer))
                }
            }
        }

        // === BlockStats 变量 ===
        // %tsl_blocks_placed_total% - 玩家放置方块总数
        if (blockStatsManager != null && params.equals("blocks_placed_total", ignoreCase = true)) {
            val onlinePlayer = player.player ?: return null
            return blockStatsManager.getTotalBlocksPlaced(onlinePlayer).toString()
        }

        // === NewbieTag 变量 ===
        // %tsl_newbie_tag% - 萌新标志（根据在线时长）
        if (newbieTagManager != null && params.equals("newbie_tag", ignoreCase = true)) {
            val onlinePlayer = player.player ?: return null
            return newbieTagManager.getPlayerTag(onlinePlayer)
        }

        // === RandomVariable 变量 ===
        // %tsl_random_变量名% - 混合分布随机数（不需要玩家）
        if (randomVariableManager != null && params.startsWith("random_", ignoreCase = true)) {
            val varName = params.substring(7) // 移除 "random_" 前缀
            return randomVariableManager.getRandomValue(varName)
        }

        // === PapiAlias 变量 ===
        // %tsl_alias_变量名% - 变量值映射（将原值映射为简写）
        if (papiAliasManager != null && params.startsWith("alias_", ignoreCase = true)) {
            if (!papiAliasManager.isEnabled()) return null
            
            val variableName = params.substring(6) // 移除 "alias_" 前缀
            if (variableName.isEmpty()) return null
            
            // 使用 PlaceholderAPI 解析原始变量值
            val originalPlaceholder = "%$variableName%"
            val originalValue = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, originalPlaceholder)
            
            // 如果解析结果仍然是占位符本身，说明变量不存在
            if (originalValue == originalPlaceholder) return null
            
            // 查找映射并返回
            return papiAliasManager.getAliasValue(variableName, originalValue)
        }

        // === PlayTime 变量 ===
        if (playTimeManager != null && playTimeManager.isEnabled()) {
            val onlinePlayer = player.player
            when {
                // %tsl_playtime% - 今日在线时长（格式化字符串）
                params.equals("playtime", ignoreCase = true) -> {
                    if (onlinePlayer == null) return "0秒"
                    return playTimeManager.getTodayPlayTimeFormatted(onlinePlayer.uniqueId)
                }
                // %tsl_playtime_seconds% - 今日在线时长（秒）
                params.equals("playtime_seconds", ignoreCase = true) -> {
                    if (onlinePlayer == null) return "0"
                    return playTimeManager.getTodayPlayTime(onlinePlayer.uniqueId).toString()
                }
                // %tsl_playtime_minutes% - 今日在线时长（分钟，整数）
                params.equals("playtime_minutes", ignoreCase = true) -> {
                    if (onlinePlayer == null) return "0"
                    val seconds = playTimeManager.getTodayPlayTime(onlinePlayer.uniqueId)
                    return (seconds / 60).toString()
                }
                // %tsl_playtime_hours% - 今日在线时长（小时，带一位小数）
                params.equals("playtime_hours", ignoreCase = true) -> {
                    if (onlinePlayer == null) return "0.0"
                    val seconds = playTimeManager.getTodayPlayTime(onlinePlayer.uniqueId)
                    return String.format("%.1f", seconds / 3600.0)
                }
            }
        }

        // === Bind 变量 ===
        if (playerDataManager != null) {
            val profile = playerDataManager.getProfileStore().get(player.uniqueId)
            when {
                // %tsl_bind% - QQ 绑定状态
                params.equals("bind", ignoreCase = true) -> {
                    return (profile?.bindStatus ?: false).toString()
                }
                // %tsl_bind_qq% - 绑定的 QQ 号码
                params.equals("bind_qq", ignoreCase = true) -> {
                    return profile?.bindQQ ?: ""
                }
            }
        }

        return null
    }
}

