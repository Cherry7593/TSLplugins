package org.tsl.tSLplugins.modules.playercountcmd

import org.bukkit.event.HandlerList
import org.tsl.tSLplugins.core.AbstractModule

/**
 * 人数控制命令模块
 * 根据在线人数自动执行控制台命令（如暂停/继续 Chunky 预加载）
 */
class PlayerCountCmdModule : AbstractModule() {
    override val id = "player-count-cmd"
    override val configPath = "player-count-cmd"
    override fun getDescription() = "人数控制命令"

    private var controller: PlayerCountCmdController? = null

    override fun loadConfig() {
        super.loadConfig()
    }

    override fun doEnable() {
        val config = context.plugin.config.getConfigurationSection(configPath) ?: return

        val upperThreshold = config.getInt("upper-threshold", 5)
        val lowerThreshold = config.getInt("lower-threshold", 2)
        val minIntervalMs = config.getLong("min-interval-ms", 10000L)
        val cmdWhenLow = config.getString("cmd-when-low", "chunky continue") ?: "chunky continue"
        val cmdWhenHigh = config.getString("cmd-when-high", "chunky pause") ?: "chunky pause"

        controller = PlayerCountCmdController(
            context.plugin,
            upperThreshold,
            lowerThreshold,
            minIntervalMs,
            cmdWhenLow,
            cmdWhenHigh
        )
        controller?.init()
    }

    override fun doDisable() {
        controller?.let { HandlerList.unregisterAll(it) }
        controller = null
    }

    override fun doReload() {
        // 需要重新创建 controller，因为配置可能变化
        doDisable()
        doEnable()
    }
}
