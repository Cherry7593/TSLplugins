package org.tsl.tSLplugins

import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.OfflinePlayer
import org.bukkit.plugin.java.JavaPlugin
import org.tsl.tSLplugins.core.ModuleRegistry
import org.tsl.tSLplugins.service.PlayerDataManager
import org.tsl.tSLplugins.modules.advancement.AdvancementModule
import org.tsl.tSLplugins.modules.ping.PingModule
import org.tsl.tSLplugins.modules.kiss.KissModule
import org.tsl.tSLplugins.modules.ride.RideModule
import org.tsl.tSLplugins.modules.toss.TossModule
import org.tsl.tSLplugins.modules.playtime.PlayTimeModule
import org.tsl.tSLplugins.modules.blockstats.BlockStatsModule
import org.tsl.tSLplugins.modules.newbietag.NewbieTagModule
import org.tsl.tSLplugins.modules.papialias.PapiAliasModule
import org.tsl.tSLplugins.modules.randomvariable.RandomVariableModule

/**
 * TSLplugins PlaceholderAPI 扩展
 *
 * 这是一个核心系统文件，整合了所有模块的 PAPI 变量。
 * 所有模块均通过 ModuleRegistry 获取。
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
    private val moduleRegistry: ModuleRegistry,
    private val playerDataManager: PlayerDataManager?
) : PlaceholderExpansion() {

    // 缓存模块引用（懒加载）
    private val advancementModule: AdvancementModule? by lazy { moduleRegistry.getModule("advancement") }
    private val pingModule: PingModule? by lazy { moduleRegistry.getModule("ping") }
    private val kissModule: KissModule? by lazy { moduleRegistry.getModule("kiss") }
    private val rideModule: RideModule? by lazy { moduleRegistry.getModule("ride") }
    private val tossModule: TossModule? by lazy { moduleRegistry.getModule("toss") }
    private val playTimeModule: PlayTimeModule? by lazy { moduleRegistry.getModule("playtime") }
    private val blockStatsModule: BlockStatsModule? by lazy { moduleRegistry.getModule("blockstats") }
    private val newbieTagModule: NewbieTagModule? by lazy { moduleRegistry.getModule("newbietag") }
    private val papiAliasModule: PapiAliasModule? by lazy { moduleRegistry.getModule("papialias") }
    private val randomVariableModule: RandomVariableModule? by lazy { moduleRegistry.getModule("randomvariable") }

    override fun getIdentifier(): String = "tsl"

    override fun getAuthor(): String = "TSL"

    override fun getVersion(): String = "2.0"

    override fun persist(): Boolean = true

    override fun onRequest(player: OfflinePlayer?, params: String): String? {
        // === Ping 变量 ===
        // %tsl_ping% - 不需要玩家，显示服务器平均延迟
        if (params.equals("ping", ignoreCase = true)) {
            val module = pingModule
            return if (module != null && module.isEnabled()) {
                String.format("%.1f", module.getAveragePing())
            } else "N/A"
        }

        // === RandomVariable 变量 ===
        // %tsl_random_变量名% - 混合分布随机数（不需要玩家）
        if (params.startsWith("random_", ignoreCase = true)) {
            val module = randomVariableModule
            if (module != null && module.isEnabled()) {
                val varName = params.substring(7) // 移除 "random_" 前缀
                return module.getRandomValue(varName)
            }
        }

        // 以下变量需要玩家
        if (player == null) return null

        // === Advancement 变量 ===
        // %tsl_adv_count% - 玩家成就数量
        if (params.equals("adv_count", ignoreCase = true)) {
            val onlinePlayer = player.player ?: return null
            val module = advancementModule
            return if (module != null && module.isEnabled()) {
                module.countHandler.getAdvancementCount(onlinePlayer).toString()
            } else null
        }

        // === Kiss 变量 ===
        val kiss = kissModule
        if (kiss != null && kiss.isEnabled()) {
            when (params) {
                "kiss_count" -> {
                    val profile = playerDataManager?.getProfileStore()?.get(player.uniqueId)
                    return (profile?.kissCount ?: 0).toString()
                }
                "kissed_count" -> {
                    val profile = playerDataManager?.getProfileStore()?.get(player.uniqueId)
                    return (profile?.kissedCount ?: 0).toString()
                }
                "kiss_toggle" -> {
                    val onlinePlayer = player.player ?: return null
                    return if (kiss.isPlayerEnabled(onlinePlayer)) "启用" else "禁用"
                }
            }
        }

        // === Ride 变量 ===
        val ride = rideModule
        if (ride != null && ride.isEnabled() && params == "ride_toggle") {
            val onlinePlayer = player.player ?: return null
            return if (ride.isPlayerEnabled(onlinePlayer)) "启用" else "禁用"
        }

        // === Toss 变量 ===
        val toss = tossModule
        if (toss != null && toss.isEnabled()) {
            when (params) {
                "toss_toggle" -> {
                    val onlinePlayer = player.player ?: return null
                    return if (toss.isPlayerEnabled(onlinePlayer)) "启用" else "禁用"
                }
                "toss_velocity" -> {
                    val onlinePlayer = player.player ?: return null
                    return String.format("%.1f", toss.getPlayerThrowVelocity(onlinePlayer))
                }
            }
        }

        // === BlockStats 变量 ===
        // %tsl_blocks_placed_total% - 玩家放置方块总数
        if (params.equals("blocks_placed_total", ignoreCase = true)) {
            val module = blockStatsModule
            if (module != null && module.isEnabled()) {
                val onlinePlayer = player.player ?: return null
                return module.getTotalBlocksPlaced(onlinePlayer).toString()
            }
        }

        // === NewbieTag 变量 ===
        // %tsl_newbie_tag% - 萌新标志（根据在线时长）
        if (params.equals("newbie_tag", ignoreCase = true)) {
            val module = newbieTagModule
            if (module != null && module.isEnabled()) {
                val onlinePlayer = player.player ?: return null
                return module.getPlayerTag(onlinePlayer)
            }
        }

        // === PapiAlias 变量 ===
        // %tsl_alias_变量名% - 变量值映射（将原值映射为简写）
        if (params.startsWith("alias_", ignoreCase = true)) {
            val module = papiAliasModule
            if (module != null && module.isEnabled()) {
                val variableName = params.substring(6) // 移除 "alias_" 前缀
                if (variableName.isEmpty()) return null

                // 使用 PlaceholderAPI 解析原始变量值
                val originalPlaceholder = "%$variableName%"
                val originalValue = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, originalPlaceholder)

                // 如果解析结果仍然是占位符本身，说明变量不存在
                if (originalValue == originalPlaceholder) return null

                // 查找映射并返回
                return module.getAliasValue(variableName, originalValue)
            }
        }

        // === PlayTime 变量 ===
        val playTime = playTimeModule
        if (playTime != null && playTime.isEnabled()) {
            val onlinePlayer = player.player
            when {
                // %tsl_playtime% - 今日在线时长（格式化字符串）
                params.equals("playtime", ignoreCase = true) -> {
                    if (onlinePlayer == null) return "0秒"
                    return playTime.getTodayPlayTimeFormatted(onlinePlayer.uniqueId)
                }
                // %tsl_playtime_seconds% - 今日在线时长（秒）
                params.equals("playtime_seconds", ignoreCase = true) -> {
                    if (onlinePlayer == null) return "0"
                    return playTime.getTodayPlayTime(onlinePlayer.uniqueId).toString()
                }
                // %tsl_playtime_minutes% - 今日在线时长（分钟，整数）
                params.equals("playtime_minutes", ignoreCase = true) -> {
                    if (onlinePlayer == null) return "0"
                    val seconds = playTime.getTodayPlayTime(onlinePlayer.uniqueId)
                    return (seconds / 60).toString()
                }
                // %tsl_playtime_hours% - 今日在线时长（小时，带一位小数）
                params.equals("playtime_hours", ignoreCase = true) -> {
                    if (onlinePlayer == null) return "0.0"
                    val seconds = playTime.getTodayPlayTime(onlinePlayer.uniqueId)
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
