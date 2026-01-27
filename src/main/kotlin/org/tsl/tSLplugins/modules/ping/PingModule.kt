package org.tsl.tSLplugins.modules.ping

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.tsl.tSLplugins.SubCommandHandler
import org.tsl.tSLplugins.core.AbstractModule

/**
 * Ping 模块 - 延迟查询功能
 * 
 * 查询玩家延迟信息，支持分页显示
 * 
 * ## 功能
 * - 查看自己的延迟
 * - 查看指定玩家的延迟
 * - 查看所有玩家延迟（分页）
 * - 根据延迟值显示颜色
 * 
 * ## 命令
 * - `/tsl ping` - 查看自己的延迟
 * - `/tsl ping <玩家>` - 查看指定玩家延迟
 * - `/tsl ping all [页码]` - 查看所有玩家延迟
 * 
 * ## 权限
 * - `tsl.ping.use` - 使用延迟查询
 * - `tsl.ping.all` - 查看所有玩家延迟
 */
class PingModule : AbstractModule() {

    override val id = "ping"
    override val configPath = "ping"

    // 配置项
    private var entriesPerPage: Int = 10
    private var greenThreshold: Int = 100
    private var yellowThreshold: Int = 200

    override fun doEnable() {
        loadPingConfig()
    }

    override fun doDisable() {
        // 无需清理
    }

    override fun doReload() {
        loadPingConfig()
    }

    override fun getCommandHandler(): SubCommandHandler = PingModuleCommand(this)
    
    override fun getDescription(): String = "延迟查询功能"

    /**
     * 加载 Ping 配置
     */
    private fun loadPingConfig() {
        entriesPerPage = getConfigInt("entries_per_page", 10)
        greenThreshold = getConfigInt("ping_colors.green", 100)
        yellowThreshold = getConfigInt("ping_colors.yellow", 200)
    }

    // ============== 公开 API ==============

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
     * 获取指定玩家的延迟
     */
    fun getPlayerPing(player: Player): Int = player.ping

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
     * 获取模块消息
     */
    fun getModuleMessage(key: String, vararg replacements: Pair<String, String>): String {
        return getMessage(key, *replacements)
    }

    /**
     * 玩家 Ping 信息数据类
     */
    data class PlayerPingInfo(
        val playerName: String,
        val ping: Int
    )
}
