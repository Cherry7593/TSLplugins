package org.tsl.tSLplugins.modules.xconomytrigger

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import org.tsl.tSLplugins.core.AbstractModule

/**
 * XConomy 余额触发器模块
 * 监控玩家余额并在达到阈值时执行控制台命令
 */
class XconomyTriggerModule : AbstractModule() {
    override val id = "xconomy-trigger"
    override val configPath = "xconomy-trigger"
    override fun getDescription() = "经济触发器"

    lateinit var manager: XconomyTriggerManager
        private set

    override fun doEnable() {
        manager = XconomyTriggerManager(context.plugin)
        manager.initialize()
        
        // 注册退出监听器以清理玩家数据
        registerListener(QuitListener())
    }

    override fun doDisable() {
        manager.shutdown()
    }

    override fun doReload() {
        manager.reload()
    }

    /**
     * 退出监听器 - 清理玩家状态
     */
    private inner class QuitListener : Listener {
        @EventHandler(priority = EventPriority.MONITOR)
        fun onPlayerQuit(event: PlayerQuitEvent) {
            manager.onPlayerQuit(event.player.uniqueId)
        }
    }
}
