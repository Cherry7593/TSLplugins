package org.tsl.tSLplugins.modules.landmark

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * 地标数据类
 */
@Serializable
data class Landmark(
    val id: String,
    var name: String,
    val world: String,
    var region: LandmarkRegion,
    var warpPoint: WarpPoint? = null,
    var icon: String = "ENDER_PEARL",
    var lore: List<String> = emptyList(),
    var defaultUnlocked: Boolean = false,
    var visibility: LandmarkVisibility = LandmarkVisibility.PUBLIC,
    val maintainers: MutableSet<String> = mutableSetOf(),
    val createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis()
)

/**
 * AABB 区域（只考虑 X/Z 坐标，Y 轴跨越整个世界高度）
 */
@Serializable
data class LandmarkRegion(
    val minX: Int,
    val minY: Int,
    val minZ: Int,
    val maxX: Int,
    val maxY: Int,
    val maxZ: Int
) {
    fun contains(x: Int, y: Int, z: Int): Boolean {
        // 只检查 X 和 Z，忽略 Y 轴
        return x in minX..maxX && z in minZ..maxZ
    }

    fun contains(x: Double, y: Double, z: Double): Boolean {
        return contains(x.toInt(), y.toInt(), z.toInt())
    }

    fun volume(): Long {
        // 只计算 X*Z 面积
        return (maxX - minX + 1).toLong() * (maxZ - minZ + 1)
    }

    fun center(): Triple<Double, Double, Double> {
        return Triple(
            (minX + maxX) / 2.0 + 0.5,
            64.0, // Y 轴使用默认值
            (minZ + maxZ) / 2.0 + 0.5
        )
    }

    companion object {
        // 从两个点创建区域（只使用 X/Z，Y 设置为世界边界）
        fun fromTwoPoints(x1: Int, z1: Int, x2: Int, z2: Int, minY: Int = -64, maxY: Int = 320): LandmarkRegion {
            return LandmarkRegion(
                minOf(x1, x2), minY, minOf(z1, z2),
                maxOf(x1, x2), maxY, maxOf(z1, z2)
            )
        }

        // 兼容旧版本（保留 Y 轴参数但忽略）
        fun fromTwoPointsLegacy(x1: Int, y1: Int, z1: Int, x2: Int, y2: Int, z2: Int): LandmarkRegion {
            return fromTwoPoints(x1, z1, x2, z2)
        }
    }
}

/**
 * 传送落点
 */
@Serializable
data class WarpPoint(
    val x: Double,
    val y: Double,
    val z: Double,
    val yaw: Float = 0f,
    val pitch: Float = 0f
)

/**
 * 地标可见性
 */
@Serializable
enum class LandmarkVisibility {
    PUBLIC,
    PRIVATE,
    HIDDEN
}

/**
 * 玩家解锁数据
 */
@Serializable
data class PlayerUnlocks(
    val playerUuid: String,
    val unlockedLandmarks: MutableSet<String> = mutableSetOf(),
    var lastUnlockTime: Long = 0
)

/**
 * 传送请求（用于吟唱时间）
 */
data class TeleportRequest(
    val playerUuid: UUID,
    val targetLandmark: Landmark,
    val startTime: Long = System.currentTimeMillis(),
    val startLocation: Triple<Double, Double, Double>
)
