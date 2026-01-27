package org.tsl.tSLplugins.modules.webbridge

import kotlinx.serialization.Serializable

/**
 * 账号绑定请求消息（Web 端发起的绑定验证）
 */
@Serializable
data class BindAccountRequest(
    val type: String = "request",
    val action: String = "BIND_ACCOUNT",
    val requestId: String,
    val data: BindAccountRequestData
)

@Serializable
data class BindAccountRequestData(
    val playerUuid: String,
    val playerName: String,
    val code: String
)
