package org.tsl.tSLplugins.modules.webbridge

import kotlinx.serialization.Serializable

/**
 * 聊天消息载荷
 *
 * @param playerName 玩家名称
 * @param playerUuid 玩家 UUID
 * @param serverName 服务器名称
 * @param message 消息内容
 * @param channel 频道名称（默认为 "global"）
 */
@Serializable
data class ChatPayload(
    val playerName: String,
    val playerUuid: String,
    val serverName: String,
    val message: String,
    val channel: String = "global"
)

