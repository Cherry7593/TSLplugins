package org.tsl.tSLplugins.Title

import kotlinx.serialization.Serializable

/**
 * 获取称号请求消息
 */
@Serializable
data class GetTitleRequest(
    val type: String = "request",
    val source: String = "mc",
    val timestamp: Long = System.currentTimeMillis(),
    val data: GetTitleRequestData
)

@Serializable
data class GetTitleRequestData(
    val action: String = "GET_TITLE",
    val id: String,
    val playerUuid: String
)

/**
 * 兑换码请求消息
 */
@Serializable
data class RedeemCodeRequest(
    val type: String = "request",
    val source: String = "mc",
    val timestamp: Long = System.currentTimeMillis(),
    val data: RedeemCodeRequestData
)

@Serializable
data class RedeemCodeRequestData(
    val action: String = "REDEEM_CODE",
    val id: String,
    val playerUuid: String,
    val playerName: String,
    val code: String
)
