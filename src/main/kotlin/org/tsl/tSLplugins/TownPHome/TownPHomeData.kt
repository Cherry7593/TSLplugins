package org.tsl.tSLplugins.TownPHome

import kotlinx.serialization.Serializable

/**
 * 小镇PHome数据
 * 存储一个小镇的所有PHome点
 */
@Serializable
data class TownPHome(
    val townName: String,
    val homes: MutableMap<String, TownPHomeLocation> = mutableMapOf()
)

/**
 * PHome坐标数据
 */
@Serializable
data class TownPHomeLocation(
    val name: String,
    val world: String,
    val x: Double,
    val y: Double,
    val z: Double,
    val yaw: Float,
    val pitch: Float,
    val createdBy: String,
    val createdAt: Long = System.currentTimeMillis()
)
