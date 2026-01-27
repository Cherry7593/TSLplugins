package org.tsl.tSLplugins.modules.newbietag

import org.bukkit.Statistic
import org.bukkit.entity.Player
import org.tsl.tSLplugins.core.AbstractModule

/**
 * NewbieTag 模块 - 萌新标志
 * 
 * 根据玩家在线时长显示不同标志。
 * 性能优先：使用玩家统计数据，无定时任务。
 */
class NewbieTagModule : AbstractModule() {

    override val id = "newbietag"
    override val configPath = "newbieTag"
    override fun getDescription() = "萌新标志"

    private var thresholdHours: Int = 24
    private var newbieTag: String = "✨"
    private var veteranTag: String = "⚡"

    override fun loadConfig() {
        super.loadConfig()
        thresholdHours = getConfigInt("thresholdHours", 24)
        newbieTag = getConfigString("newbieTag", "✨")
        veteranTag = getConfigString("veteranTag", "⚡")
    }

    override fun doEnable() {
        logInfo("阈值: $thresholdHours 小时")
    }

    /**
     * 获取玩家的标志
     * 
     * @param player 玩家
     * @return 标志字符串
     */
    fun getPlayerTag(player: Player): String {
        if (!isEnabled()) return ""

        return try {
            val playTimeHours = getPlayTimeHours(player)
            if (playTimeHours < thresholdHours) newbieTag else veteranTag
        } catch (e: Exception) {
            logWarning("获取玩家标志失败: ${player.name} - ${e.message}")
            ""
        }
    }

    /**
     * 获取玩家的游玩时长（小时）
     */
    fun getPlayTimeHours(player: Player): Double {
        return try {
            // PLAY_ONE_MINUTE 单位是 tick (1分钟 = 1200 tick)
            val playTimeTicks = player.getStatistic(Statistic.PLAY_ONE_MINUTE)
            playTimeTicks / 1200.0 / 60.0
        } catch (e: Exception) {
            0.0
        }
    }

    /**
     * 检查玩家是否为萌新
     */
    fun isNewbie(player: Player): Boolean {
        return getPlayTimeHours(player) < thresholdHours
    }
}
