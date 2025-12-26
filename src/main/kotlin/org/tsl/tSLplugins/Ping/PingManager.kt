package org.tsl.tSLplugins.Ping

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.tsl.tSLplugins.TSLplugins

/**
 * Ping 功能管理器
 * 负责处理玩家延迟查询和相关配置
 */
class PingManager(private val plugin: JavaPlugin) {

    private var enabled: Boolean = true
    private var entriesPerPage: Int = 10
    private var greenThreshold: Int = 100
    private var yellowThreshold: Int = 200

    private val msg get() = (plugin as TSLplugins).messageManager

    init {
        loadConfig()
    }

    /**
     * 加载配置
     */
    fun loadConfig() {
        val config = plugin.config
        enabled = config.getBoolean("ping.enabled", true)
        entriesPerPage = config.getInt("ping.entries_per_page", 10)
        greenThreshold = config.getInt("ping.ping_colors.green", 100)
        yellowThreshold = config.getInt("ping.ping_colors.yellow", 200)
    }

    /**
     * 检查功能是否启用
     */
    fun isEnabled(): Boolean = enabled

    /**
     * 获取每页显示的条目数
     */
    fun getEntriesPerPage(): Int = entriesPerPage

    /**
     * 获取绿色延迟阈值
     */
    fun getGreenThreshold(): Int = greenThreshold

    /**
     * 获取黄色延迟阈值
     */
    fun getYellowThreshold(): Int = yellowThreshold

    /**
     * 获取消息文本
     */
    fun getMessage(key: String, vararg replacements: Pair<String, String>): String {
        return msg.getModule("ping", key, *replacements)
    }

    /**
     * 获取指定玩家的延迟
     */
    fun getPlayerPing(player: Player): Int {
        return player.ping
    }

    /**
     * 获取所有在线玩家的延迟信息，按延迟从低到高排序
     */
    fun getAllPlayersPing(): List<PlayerPingInfo> {
        return Bukkit.getOnlinePlayers()
            .map { PlayerPingInfo(it.name, it.ping) }
            .sortedBy { it.ping }
    }

    /**
     * 计算服务器平均延迟
     */
    fun getAveragePing(): Double {
        val players = Bukkit.getOnlinePlayers()
        if (players.isEmpty()) return 0.0

        return players.map { it.ping }.average()
    }

    /**
     * 根据延迟值获取颜色代码
     */
    fun getPingColorCode(ping: Int): String {
        return when {
            ping < greenThreshold -> "&a"
            ping < yellowThreshold -> "&e"
            else -> "&c"
        }
    }

    /**
     * 玩家 Ping 信息数据类
     */
    data class PlayerPingInfo(
        val playerName: String,
        val ping: Int
    )
}

