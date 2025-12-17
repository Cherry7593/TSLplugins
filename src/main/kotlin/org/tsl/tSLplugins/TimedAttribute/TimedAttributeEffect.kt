package org.tsl.tSLplugins.TimedAttribute

import java.util.UUID

/**
 * 操作类型枚举
 */
enum class EffectType {
    SET,  // 直接设置属性值，覆盖之前所有操作
    ADD   // 在当前值上增减
}

/**
 * 计时属性效果数据模型
 *
 * @param playerUuid 玩家 UUID
 * @param attributeKey 属性键名（如 "GENERIC_MAX_HEALTH"）
 * @param modifierUuid 修改器 UUID（唯一标识符）
 * @param amount 修改数量（SET: 目标值，ADD: 增减量）
 * @param effectType 效果类型：SET 或 ADD
 * @param createdAt 创建时间戳（毫秒），用于确定操作顺序
 * @param expireAt 过期时间戳（毫秒）
 * @param baseValue 触发时捕获的玩家默认值（仅第一个效果需要记录）
 * @param source 来源标识（如 "command"）
 */
data class TimedAttributeEffect(
    val playerUuid: UUID,
    val attributeKey: String,
    val modifierUuid: UUID,
    val amount: Double,
    val effectType: EffectType = EffectType.ADD,
    val createdAt: Long = System.currentTimeMillis(),
    val expireAt: Long,
    val baseValue: Double? = null,
    val source: String? = null
) {
    /**
     * 检查效果是否已过期
     */
    fun isExpired(): Boolean = System.currentTimeMillis() > expireAt

    /**
     * 获取剩余时间（毫秒）
     * @return 剩余时间，已过期返回 0
     */
    fun getRemainingMs(): Long {
        val remaining = expireAt - System.currentTimeMillis()
        return if (remaining > 0) remaining else 0
    }

    /**
     * 获取剩余时间（秒）
     */
    fun getRemainingSeconds(): Int = (getRemainingMs() / 1000).toInt()

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

