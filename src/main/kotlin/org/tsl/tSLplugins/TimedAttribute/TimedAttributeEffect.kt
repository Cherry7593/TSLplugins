package org.tsl.tSLplugins.TimedAttribute

import java.util.UUID

/**
 * 计时属性效果数据模型（堆栈版 - 相对恢复）
 *
 * 设计原则：
 * - 每个玩家的每个属性可以有多个效果（堆栈）
 * - 新效果入栈时暂停旧效果，新效果结束后恢复旧效果
 * - 使用 delta（变化量）而非绝对值，效果结束时撤销变化
 * - 其他模块/插件的修改会被保留
 * - 上下线、死亡不影响计时
 *
 * 恢复逻辑：
 * - 效果开始：capturedValue = 当前值，应用 targetValue
 * - 效果结束：delta = targetValue - capturedValue，当前值 -= delta
 * - 这样其他模块的修改会被保留
 *
 * @param playerUuid 玩家 UUID
 * @param attributeKey 属性键名（如 "max_health"）
 * @param effectId 效果唯一标识符
 * @param targetValue 目标值（要设置的属性值）
 * @param capturedValue 捕获值（效果开始时的属性值，用于计算 delta）
 * @param remainingMs 剩余时间（毫秒），可暂停
 * @param stackIndex 堆栈索引（0=栈底，越大越新）
 * @param isPaused 是否被暂停（被更新的效果覆盖时暂停）
 * @param lastTickAt 上次计时时间戳（用于计算消耗的时间）
 * @param createdAt 创建时间戳
 * @param source 来源标识
 */
data class TimedAttributeEffect(
    val playerUuid: UUID,
    val attributeKey: String,
    val effectId: UUID = UUID.randomUUID(),
    val targetValue: Double,
    val capturedValue: Double,
    var remainingMs: Long,
    var stackIndex: Int = 0,
    var isPaused: Boolean = false,
    var lastTickAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val source: String? = null
) {
    /**
     * 获取变化量（效果结束时需要撤销的量）
     */
    val delta: Double get() = targetValue - capturedValue
    /**
     * 检查效果是否已过期
     */
    fun isExpired(): Boolean = remainingMs <= 0

    /**
     * 消耗时间（仅活跃效果调用）
     * @return 消耗后是否过期
     */
    fun tick(): Boolean {
        if (isPaused) return false
        
        val now = System.currentTimeMillis()
        val elapsed = now - lastTickAt
        lastTickAt = now
        remainingMs -= elapsed
        
        return remainingMs <= 0
    }

    /**
     * 暂停效果
     */
    fun pause() {
        if (!isPaused) {
            // 先结算当前时间
            val now = System.currentTimeMillis()
            val elapsed = now - lastTickAt
            remainingMs -= elapsed
            isPaused = true
        }
    }

    /**
     * 恢复效果
     */
    fun resume() {
        if (isPaused) {
            lastTickAt = System.currentTimeMillis()
            isPaused = false
        }
    }

    /**
     * 获取剩余时间（秒）
     */
    fun getRemainingSeconds(): Int = (remainingMs.coerceAtLeast(0) / 1000).toInt()

    /**
     * 格式化剩余时间为可读字符串
     */
    fun formatRemainingTime(): String {
        val seconds = getRemainingSeconds()
        return when {
            seconds <= 0 -> "已过期"
            seconds < 60 -> "${seconds}秒"
            seconds < 3600 -> "${seconds / 60}分${seconds % 60}秒"
            seconds < 86400 -> "${seconds / 3600}小时${(seconds % 3600) / 60}分"
            else -> "${seconds / 86400}天${(seconds % 86400) / 3600}小时"
        }
    }

}

