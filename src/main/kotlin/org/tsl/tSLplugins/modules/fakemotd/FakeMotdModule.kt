package org.tsl.tSLplugins.modules.fakemotd

import com.destroystokyo.paper.event.server.PaperServerListPingEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.tsl.tSLplugins.core.AbstractModule

/**
 * 假玩家人数模块
 * 修改服务器列表显示的在线人数
 */
class FakeMotdModule : AbstractModule() {
    override val id = "fakeplayer"
    override val configPath = "fakeplayer"
    override fun getDescription() = "假玩家人数"

    private var deltaCount: Int = 0

    override fun loadConfig() {
        super.loadConfig()
        deltaCount = getConfigInt("count", 0)
    }

    override fun doEnable() {
        registerListener(FakeMotdListener())
    }

    override fun doReload() {
        // 配置已在 loadConfig 中重新加载
    }

    /**
     * MOTD 监听器
     */
    private inner class FakeMotdListener : Listener {

        @EventHandler
        fun onServerListPing(event: PaperServerListPingEvent) {
            val base = event.numPlayers
            val shown = maxOf(0, base + deltaCount)
            event.numPlayers = shown
        }
    }
}
