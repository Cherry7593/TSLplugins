package org.tsl.tSLplugins.modules.timedattribute

import org.bukkit.plugin.java.JavaPlugin
import org.tsl.tSLplugins.SubCommandHandler
import org.tsl.tSLplugins.core.AbstractModule

/**
 * 计时属性效果模块
 * 临时修改玩家属性（如体型、生命值等）
 */
class TimedAttributeModule : AbstractModule() {
    override val id = "timed-attribute"
    override val configPath = "timed-attribute"
    override fun getDescription() = "计时属性效果系统"

    lateinit var manager: TimedAttributeManager
        private set
    private lateinit var listener: TimedAttributeListener

    override fun doEnable() {
        val javaPlugin = context.plugin
        
        manager = TimedAttributeManager(javaPlugin)
        manager.startExpirationTask()
        
        listener = TimedAttributeListener(javaPlugin, manager)
        registerListener(listener)
    }

    override fun doDisable() {
        manager.shutdown()
    }

    override fun doReload() {
        manager.loadConfig()
    }

    override fun getCommandHandler(): SubCommandHandler = TimedAttributeCommand(manager)
    
    override fun getAdditionalCommandHandlers(): Map<String, SubCommandHandler> {
        return mapOf(
            "attr" to TimedAttributeCommand(manager)
        )
    }
}
