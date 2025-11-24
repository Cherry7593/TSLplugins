package org.tsl.tSLplugins.BlockStats

import org.bukkit.Material
import org.bukkit.Statistic
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

/**
 * BlockStats 功能管理器
 *
 * 轻量级实现：
 * - 不使用事件监听
 * - 不持久化数据
 * - 实时计算（每次调用都重新计算）
 * - 基于原版统计系统（Statistic.USE_ITEM）
 */
class BlockStatsManager(private val plugin: JavaPlugin) {

    private var enabled: Boolean = true

    init {
        loadConfig()
    }

    /**
     * 加载配置
     */
    fun loadConfig() {
        val config = plugin.config
        enabled = config.getBoolean("blockstats.enabled", true)

        plugin.logger.info("[BlockStats] 配置已加载 - 启用: $enabled")
    }

    /**
     * 检查功能是否启用
     */
    fun isEnabled(): Boolean = enabled

    /**
     * 计算玩家放置方块总数
     *
     * 使用原版统计 Statistic.USE_ITEM：
     * - 遍历所有 Material
     * - 筛选出 isBlock() 为 true 的材料
     * - 累加对应的统计值
     *
     * @param player 玩家
     * @return 放置方块总数
     */
    fun getTotalBlocksPlaced(player: Player): Long {
        var total = 0L

        // 遍历所有材料
        for (material in Material.entries) {
            // 只统计方块类物品
            if (material.isBlock) {
                try {
                    // 获取该材料的使用次数（即放置次数）
                    val count = player.getStatistic(Statistic.USE_ITEM, material)
                    total += count
                } catch (e: Exception) {
                    // 忽略异常（某些材料可能不支持统计）
                    // 例如：AIR、某些技术性方块等
                }
            }
        }

        return total
    }

    /**
     * 获取玩家放置方块总数（字符串格式）
     *
     * 用于 PlaceholderAPI 返回
     *
     * @param player 玩家
     * @return 数字字符串，如果玩家为 null 返回 "0"
     */
    fun getTotalBlocksPlacedString(player: Player?): String {
        if (player == null) return "0"
        if (!isEnabled()) return "0"

        return getTotalBlocksPlaced(player).toString()
    }
}

