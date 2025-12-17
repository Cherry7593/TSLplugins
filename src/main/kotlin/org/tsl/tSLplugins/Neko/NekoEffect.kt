package org.tsl.tSLplugins.Neko

import java.util.UUID

/**
 * 猫娘效果数据类
 * 记录玩家的猫娘状态
 */
data class NekoEffect(
    val playerUuid: UUID,
    val playerName: String,
    val expireAt: Long,  // 过期时间戳（毫秒），-1 表示永久
    val source: String = "command"
) {
    /**
     * 检查效果是否已过期
     */
    fun isExpired(): Boolean {
        if (expireAt == -1L) return false  // 永久效果不会过期
        return System.currentTimeMillis() >= expireAt
    }

    /**
     * 获取剩余时间（毫秒）
     */
    fun getRemainingTime(): Long {
        if (expireAt == -1L) return Long.MAX_VALUE
        val remaining = expireAt - System.currentTimeMillis()
        return if (remaining > 0) remaining else 0
    }

    /**
     * 格式化剩余时间
     */
    fun formatRemainingTime(): String {
        if (expireAt == -1L) return "永久"
        val remaining = getRemainingTime()
        if (remaining <= 0) return "已过期"

        val seconds = (remaining / 1000).toInt()
        return when {
            seconds < 60 -> "${seconds}秒"
            seconds < 3600 -> "${seconds / 60}分${seconds % 60}秒"
            seconds < 86400 -> "${seconds / 3600}小时${(seconds % 3600) / 60}分"
            else -> "${seconds / 86400}天${(seconds % 86400) / 3600}小时"
        }
    }
}

