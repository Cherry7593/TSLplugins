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
    private val randomVariableManager: RandomVariableManager?
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

        return null
    }
}

