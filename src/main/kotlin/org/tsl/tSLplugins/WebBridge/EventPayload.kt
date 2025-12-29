package org.tsl.tSLplugins.WebBridge

import kotlinx.serialization.Serializable

/**
 * 事件消息结构（用于 PLAYER_LIST 等事件）
 */
@Serializable
data class EventMessage(
    val type: String = "event",
    val source: String = "mc",
    val timestamp: Long = System.currentTimeMillis(),
    val data: EventData
)

/**
 * 事件数据
 */
@Serializable
data class EventData(
    val event: String,
    val id: String,
    val online: Int? = null,
    val max: Int? = null,
    val tps: Double? = null,
    val players: List<PlayerInfo>? = null
)

/**
 * 玩家信息
 */
@Serializable
data class PlayerInfo(
    val uuid: String,
    val name: String
)

/**
 * 心跳消息
 */
@Serializable
data class HeartbeatMessage(
    val type: String = "heartbeat",
    val timestamp: Long = System.currentTimeMillis()
)
