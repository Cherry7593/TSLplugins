package org.tsl.tSLplugins.modules.neko

import org.bukkit.plugin.java.JavaPlugin
import org.tsl.tSLplugins.SubCommandHandler
import org.tsl.tSLplugins.core.AbstractModule

/**
 * 猫娘模式模块
 * 管理玩家的猫娘状态，聊天消息添加后缀
 */
class NekoModule : AbstractModule() {
    override val id = "neko"
    override val configPath = "neko"
    override fun getDescription() = "猫娘模式管理"

    lateinit var manager: NekoManager
        private set
    private lateinit var listener: NekoChatListener

    override fun doEnable() {
        manager = NekoManager(context.plugin)
        manager.startExpirationTask()
        
        listener = NekoChatListener(context.plugin, manager)
        registerListener(listener)
    }

    override fun doDisable() {
        manager.shutdown()
    }

    override fun doReload() {
        manager.loadConfig()
    }

    override fun getCommandHandler(): SubCommandHandler = NekoCommand(manager)
}
