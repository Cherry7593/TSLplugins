package org.tsl.tSLplugins

import java.util.UUID

/**
 * TSL 玩家配置数据类
 * 存储玩家的所有个人配置
 */
data class TSLPlayerProfile(
    /** 玩家 UUID */
    val uuid: UUID,

    /** 玩家名称（仅用于识别，不作为主键） */
    var playerName: String = "",

    // ==================== 功能开关 ====================

    /** Kiss 功能开关 */
    var kissEnabled: Boolean = true,

    /** Ride 功能开关 */
    var rideEnabled: Boolean = true,

    /** Toss 功能开关 */
    var tossEnabled: Boolean = true,

    /** 是否允许幻翼骚扰 */
    var allowPhantom: Boolean = false,

    // ==================== 功能参数 ====================

    /** Toss 投掷速度 */
    var tossVelocity: Double = 1.5,

    // ==================== 统计数据 ====================

    /** Kiss 亲吻次数 */
    var kissCount: Int = 0,

    /** Kiss 被亲吻次数 */
    var kissedCount: Int = 0,

    // ==================== 屏蔽列表 ====================

    /** 聊天屏蔽列表（被屏蔽玩家的 UUID） */
    var ignoreList: MutableSet<UUID> = mutableSetOf(),

    // ==================== 迁移标记 ====================

    /** 是否已从 PDC 迁移 */
    var migratedFromPdc: Boolean = false,

    /** 最后保存时间 */
    var lastSaved: Long = System.currentTimeMillis()
) {
    /**
     * 更新最后保存时间
     */
    fun updateSaveTime() {
        lastSaved = System.currentTimeMillis()
    }
}

