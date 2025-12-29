package org.tsl.tSLplugins.WebBridge

import kotlinx.serialization.Serializable

/**
 * 账号绑定请求消息
 */
@Serializable
data class BindAccountRequest(
    val type: String = "request",
    val source: String = "mc",
    val timestamp: Long = System.currentTimeMillis(),
    val data: BindAccountRequestData
)

@Serializable
data class BindAccountRequestData(
    val action: String = "BIND_ACCOUNT",
    val id: String,
    val playerUuid: String,
    val playerName: String,
    val code: String
)
