package org.tsl.tSLplugins.modules.webbridge

import kotlinx.serialization.Serializable

/**
 * QQ 群指令执行结果响应消息
 */
@Serializable
data class QQCommandResultResponse(
    val type: String = "response",
    val source: String = "minecraft",
    val timestamp: Long = System.currentTimeMillis(),
    val requestId: String,
    val data: QQCommandResultData
)

@Serializable
data class QQCommandResultData(
    val action: String = "QQ_COMMAND_RESULT",
    val success: Boolean,
    val output: String? = null,
    val error: String? = null,
    val executedAt: Long
)
