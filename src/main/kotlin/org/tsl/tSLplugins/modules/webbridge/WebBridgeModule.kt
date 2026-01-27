package org.tsl.tSLplugins.modules.webbridge

import org.bukkit.plugin.java.JavaPlugin
import org.tsl.tSLplugins.SubCommandHandler
import org.tsl.tSLplugins.core.AbstractModule

/**
 * WebBridge 模块
 * WebSocket 双向通信桥接
 */
class WebBridgeModule : AbstractModule() {
    override val id = "webbridge"
    override val configPath = "webbridge"
    override fun getDescription() = "Web 通信桥接"

    lateinit var manager: WebBridgeManager
        private set

    override fun doEnable() {
        manager = WebBridgeManager(context.plugin)
        manager.initialize()
    }

    override fun doDisable() {
        manager.shutdown()
    }

    override fun doReload() {
        manager.reload()
    }

    override fun getCommandHandler(): SubCommandHandler = WebBridgeCommand(manager)
    
    override fun getAdditionalCommandHandlers(): Map<String, SubCommandHandler> {
        return mapOf(
            "bind" to BindCommand(manager)
        )
    }
}
