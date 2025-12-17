package org.tsl.tSLplugins.Mcedia

import org.bukkit.Location
import org.bukkit.entity.ArmorStand
import java.util.UUID

/**
 * Mcedia 播放器数据类
 * 存储播放器的配置信息
 */
data class McediaPlayer(
    val uuid: UUID,                          // 盔甲架 UUID
    val name: String,                        // 播放器名称
    val location: Location,                  // 位置
    var videoUrl: String = "",               // 视频链接
    var startTime: String = "",              // 起始时间 (格式: h:mm:ss)
    var scale: Double = 1.0,                 // 缩放
    var volume: Double = 1.0,                // 音量
    var maxVolumeRange: Double = 5.0,        // 最大音量范围
    var hearingRange: Double = 500.0,        // 可听范围
    var offsetX: Double = 0.0,               // X 偏移
    var offsetY: Double = 0.0,               // Y 偏移
    var offsetZ: Double = 0.0,               // Z 偏移
    var looping: Boolean = false,            // 循环播放
    var noDanmaku: Boolean = false,          // 关闭弹幕
    val createdBy: UUID,                     // 创建者 UUID
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * 生成主手书内容（视频链接）
     */
    fun generateMainHandBookContent(): List<String> {
        val pages = mutableListOf<String>()

        // 第一页：视频链接
        val page1 = buildString {
            append(videoUrl)
            if (startTime.isNotEmpty()) {
                append("\n$startTime")
            }
        }
        pages.add(page1)

        return pages
    }

    /**
     * 生成副手书内容（配置参数）
     */
    fun generateOffHandBookContent(): List<String> {
        val pages = mutableListOf<String>()

        // 第一页：画面偏移和缩放
        val page1 = buildString {
            appendLine(offsetX.toString())
            appendLine(offsetY.toString())
            appendLine(offsetZ.toString())
            append(scale.toString())
        }
        pages.add(page1)

        // 第二页：声音配置
        val page2 = buildString {
            appendLine("0")  // 声源 X 偏移
            appendLine("0")  // 声源 Y 偏移
            appendLine("0")  // 声源 Z 偏移
            appendLine(maxVolumeRange.toString())
            appendLine("1")  // 声音最大值范围
            append(hearingRange.toString())
        }
        pages.add(page2)

        // 第三页：标签
        val page3 = buildString {
            if (looping) appendLine("looping")
            if (noDanmaku) append("nodanmaku")
        }
        if (page3.isNotEmpty()) {
            pages.add(page3)
        }

        return pages
    }

    /**
     * 获取简短描述
     */
    fun getShortDescription(): String {
        val urlPreview = if (videoUrl.length > 30) {
            videoUrl.take(27) + "..."
        } else {
            videoUrl.ifEmpty { "未设置" }
        }
        return "§f$name §7| §b$urlPreview"
    }
}

/**
 * 视频平台类型
 */
enum class VideoPlatform(val displayName: String, val prefix: String) {
    BILIBILI("哔哩哔哩", "https://www.bilibili.com/"),
    BILIBILI_LIVE("B站直播", "https://live.bilibili.com/"),
    BILIBILI_BANGUMI("B站番剧", "https://www.bilibili.com/bangumi/play/"),
    BILIBILI_SHORT("B站短链", "https://b23.tv/"),
    DOUYIN("抖音", "https://v.douyin.com/"),
    YHDM("樱花动漫", "https://yhdm.one/vod-play/"),
    DIRECT("直链", ""),
    UNKNOWN("未知", "");

    companion object {
        fun detect(url: String): VideoPlatform {
            return when {
                url.startsWith("https://www.bilibili.com/bangumi/play/") -> BILIBILI_BANGUMI
                url.startsWith("https://www.bilibili.com/") -> BILIBILI
                url.startsWith("https://live.bilibili.com/") -> BILIBILI_LIVE
                url.contains("b23.tv") -> BILIBILI_SHORT
                url.contains("v.douyin.com") || url.contains("抖音") -> DOUYIN
                url.startsWith("https://yhdm.one/vod-play/") -> YHDM
                url.startsWith("http://") || url.startsWith("https://") -> DIRECT
                else -> UNKNOWN
            }
        }
    }
}

