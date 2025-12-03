package org.tsl.tSLplugins.NewbieTag

import org.bukkit.Statistic
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

/**
 * 萌新标志管理器
 * 根据玩家在线时长显示不同标志
 * 性能优先：使用玩家统计数据，无定时任务
 */
class NewbieTagManager(private val plugin: JavaPlugin) {

    // ===== 配置缓存 =====
    private var enabled: Boolean = true
    private var thresholdHours: Int = 24
    private var newbieTag: String = "✨"
    private var veteranTag: String = "⚡"

    init {
        loadConfig()
    }

    /**
     * 加载配置
     */
    fun loadConfig() {
        val config = plugin.config

        enabled = config.getBoolean("newbieTag.enabled", true)
        thresholdHours = config.getInt("newbieTag.thresholdHours", 24)
        newbieTag = config.getString("newbieTag.newbieTag", "✨") ?: "✨"
        veteranTag = config.getString("newbieTag.veteranTag", "⚡") ?: "⚡"

        plugin.logger.info("[NewbieTag] 配置已加载 - 启用: $enabled, 阈值: $thresholdHours 小时")
    }

    /**
     * 功能是否启用
     */
    fun isEnabled(): Boolean = enabled

    /**
     * 获取玩家的标志
     * 根据玩家在线时长（PLAY_ONE_MINUTE 统计）判断
     *
     * @param player 玩家
     * @return 标志字符串
     */
    fun getPlayerTag(player: Player): String {
        if (!enabled) {
            return ""
        }

        try {
            // 获取玩家总游玩时间（单位：tick）
            // 注意：PLAY_ONE_MINUTE 的单位是 tick，不是分钟！
            // 1 分钟 = 1200 tick (20 tick/秒 × 60 秒)
            val playTimeTicks = player.getStatistic(Statistic.PLAY_ONE_MINUTE)

            // 转换为小时
            // tick -> 分钟 -> 小时
            val playTimeHours = playTimeTicks / 1200.0 / 60.0

            // 判断是萌新还是老玩家
            return if (playTimeHours < thresholdHours) {
                newbieTag  // 萌新标志
            } else {
                veteranTag  // 老玩家标志
            }
        } catch (e: Exception) {
            plugin.logger.warning("[NewbieTag] 获取玩家标志失败: ${player.name} - ${e.message}")
            return ""
        }
    }

    /**
     * 获取玩家的游玩时长（小时）
     *
     * @param player 玩家
     * @return 游玩时长（小时）
     */
    fun getPlayTimeHours(player: Player): Double {
        return try {
            // 获取游玩时间（tick）
            val playTimeTicks = player.getStatistic(Statistic.PLAY_ONE_MINUTE)
            // 转换为小时：tick -> 分钟 -> 小时
            playTimeTicks / 1200.0 / 60.0
        } catch (e: Exception) {
            0.0
        }
    }

    /**
     * 检查玩家是否为萌新
     *
     * @param player 玩家
     * @return true = 萌新, false = 老玩家
     */
    fun isNewbie(player: Player): Boolean {
        return getPlayTimeHours(player) < thresholdHours
    }
}

