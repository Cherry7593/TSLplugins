package org.tsl.tSLplugins.modules.alias

import org.bukkit.plugin.java.JavaPlugin
import org.tsl.tSLplugins.SubCommandHandler
import org.tsl.tSLplugins.core.AbstractModule

/**
 * 命令别名模块
 * 动态注册命令别名
 */
class AliasModule : AbstractModule() {
    override val id = "aliasreload"
    override val configPath = "alias"
    override fun getDescription() = "命令别名系统"

    lateinit var manager: AliasManager
        private set

    override fun doEnable() {
        manager = AliasManager(context.plugin)
    }

    override fun doDisable() {
        manager.cleanup()
    }

    override fun doReload() {
        manager.reloadAliases()
    }

    override fun getCommandHandler(): SubCommandHandler = 
        AliasCommand(context.plugin, manager)

    fun getAliasCount(): Int = manager.getAliasCount()
}
