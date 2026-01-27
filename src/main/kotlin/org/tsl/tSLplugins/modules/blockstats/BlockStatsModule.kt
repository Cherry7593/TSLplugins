package org.tsl.tSLplugins.modules.blockstats

import org.bukkit.Material
import org.bukkit.Statistic
import org.bukkit.entity.Player
import org.tsl.tSLplugins.core.AbstractModule

/**
 * BlockStats 模块 - 方块放置统计
 * 
 * 基于原版统计系统（Statistic.USE_ITEM）计算玩家放置的方块总数。
 * 轻量级实现，无监听器，实时计算。
 */
class BlockStatsModule : AbstractModule() {

    override val id = "blockstats"
    override val configPath = "blockstats"
    override fun getDescription() = "方块放置统计"

    override fun doEnable() {
        // 无需初始化，纯计算模块
    }

    /**
     * 计算玩家放置方块总数
     * 
     * 遍历所有 Material，筛选 isBlock() 为 true 的材料，
     * 累加对应的 USE_ITEM 统计值。
     *
     * @param player 玩家
     * @return 放置方块总数
     */
    fun getTotalBlocksPlaced(player: Player): Long {
        var total = 0L

        for (material in Material.entries) {
            if (material.isBlock) {
                try {
                    val count = player.getStatistic(Statistic.USE_ITEM, material)
                    total += count
                } catch (e: Exception) {
                    // 忽略异常（某些材料可能不支持统计）
                }
            }
        }

        return total
    }
}
