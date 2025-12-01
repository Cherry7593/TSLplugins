package org.tsl.tSLplugins.Near

import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import kotlin.math.roundToInt

/**
 * Near 附近玩家管理器
 * 负责查找并显示附近的玩家及距离
 */
class NearManager(val plugin: JavaPlugin) {

    // ===== 配置缓存 =====
    private var enabled: Boolean = true
    private var defaultRadius: Int = 1000
    private var maxRadius: Int = 1000

    init {
        loadConfig()
    }

    /**
     * 加载配置
     */
    fun loadConfig() {
        val config = plugin.config

        enabled = config.getBoolean("near.enabled", true)
        defaultRadius = config.getInt("near.defaultRadius", 1000)
        maxRadius = config.getInt("near.maxRadius", 1000)

        plugin.logger.info("[Near] 配置已加载 - 启用: $enabled, 默认范围: $defaultRadius, 最大范围: $maxRadius")
    }

    /**
     * 功能是否启用
     */
    fun isEnabled(): Boolean = enabled

    /**
     * 获取默认搜索半径
     */
    fun getDefaultRadius(): Int = defaultRadius

    /**
     * 获取最大搜索半径
     */
    fun getMaxRadius(): Int = maxRadius

    /**
     * 查找附近的玩家
     * 使用 Folia 友好的方式获取附近玩家
     *
     * @param player 查询的玩家
     * @param radius 搜索半径
     * @return 玩家和距离的列表，按距离排序
     */
    fun findNearbyPlayers(player: Player, radius: Int): List<Pair<Player, Double>> {
        val playerLocation = player.location
        val radiusSquared = radius * radius.toDouble()

        // 获取同一世界的所有玩家
        // 在 Folia 中，这是线程安全的操作
        val nearbyPlayers = player.world.players
            .filter { other ->
                // 排除自己
                if (other.uniqueId == player.uniqueId) {
                    return@filter false
                }

                // 快速距离检查（使用平方距离避免开方运算）
                val distanceSquared = playerLocation.distanceSquared(other.location)
                distanceSquared <= radiusSquared
            }
            .map { other ->
                // 计算实际距离
                val distance = playerLocation.distance(other.location)
                Pair(other, distance)
            }
            .sortedBy { it.second } // 按距离排序

        return nearbyPlayers
    }

    /**
     * 格式化距离显示
     * @param distance 距离（方块）
     * @return 格式化的距离字符串
     */
    fun formatDistance(distance: Double): String {
        return "${distance.roundToInt()} 米"
    }
}

