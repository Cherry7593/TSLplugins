package org.tsl.tSLplugins.Speed

import org.bukkit.entity.Player

/**
 * 速度管理器
 * 管理玩家的行走速度和飞行速度
 */
class SpeedManager {

    companion object {
        // Minecraft 默认速度
        const val DEFAULT_WALK_SPEED = 0.2f
        const val DEFAULT_FLY_SPEED = 0.1f

        // 允许的倍率范围
        const val MIN_MULTIPLIER = 0.1
        const val MAX_MULTIPLIER = 10.0
    }

    /**
     * 设置玩家行走速度
     * @param player 玩家
     * @param multiplier 速度倍率（0.1-10.0）
     * @return 是否设置成功
     */
    fun setWalkSpeed(player: Player, multiplier: Double): Boolean {
        if (multiplier < MIN_MULTIPLIER || multiplier > MAX_MULTIPLIER) {
            return false
        }

        // 计算实际速度值
        // Minecraft 速度范围：-1.0 到 1.0
        // 默认行走速度是 0.2
        val speed = (DEFAULT_WALK_SPEED * multiplier).toFloat().coerceIn(-1.0f, 1.0f)
        player.walkSpeed = speed

        return true
    }

    /**
     * 设置玩家飞行速度
     * @param player 玩家
     * @param multiplier 速度倍率（0.1-10.0）
     * @return 是否设置成功
     */
    fun setFlySpeed(player: Player, multiplier: Double): Boolean {
        if (multiplier < MIN_MULTIPLIER || multiplier > MAX_MULTIPLIER) {
            return false
        }

        // 计算实际速度值
        // 默认飞行速度是 0.1
        val speed = (DEFAULT_FLY_SPEED * multiplier).toFloat().coerceIn(-1.0f, 1.0f)
        player.flySpeed = speed

        return true
    }

    /**
     * 重置玩家行走速度为默认值
     */
    fun resetWalkSpeed(player: Player) {
        player.walkSpeed = DEFAULT_WALK_SPEED
    }

    /**
     * 重置玩家飞行速度为默认值
     */
    fun resetFlySpeed(player: Player) {
        player.flySpeed = DEFAULT_FLY_SPEED
    }

    /**
     * 获取当前行走速度倍率
     */
    fun getWalkSpeedMultiplier(player: Player): Double {
        return (player.walkSpeed / DEFAULT_WALK_SPEED).toDouble()
    }

    /**
     * 获取当前飞行速度倍率
     */
    fun getFlySpeedMultiplier(player: Player): Double {
        return (player.flySpeed / DEFAULT_FLY_SPEED).toDouble()
    }
}

