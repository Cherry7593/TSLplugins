package org.tsl.tSLplugins.modules.webbridge

import kotlinx.serialization.Serializable

/**
 * WebBridge 通用消息结构
 *
 * @param type 消息类型（如 "chat"）
 * @param source 消息来源（固定为 "mc"）
 * @param timestamp 时间戳（毫秒）
 * @param payload 消息载荷（泛型，支持不同类型的消息内容）
 */
@Serializable
data class BridgeMessage<T>(
    val type: String,
    val source: String = "mc",
    val timestamp: Long = System.currentTimeMillis(),
    val payload: T
)

