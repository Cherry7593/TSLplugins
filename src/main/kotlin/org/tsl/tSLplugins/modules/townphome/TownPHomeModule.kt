package org.tsl.tSLplugins.modules.townphome

import org.bukkit.plugin.java.JavaPlugin
import org.tsl.tSLplugins.SubCommandHandler
import org.tsl.tSLplugins.core.AbstractModule

/**
 * 小镇PHome模块
 * 基于 PAPI 变量的小镇传送系统
 */
class TownPHomeModule : AbstractModule() {
    override val id = "phome"  // 命令是 /tsl phome
    override val configPath = "town-phome"
    override fun getDescription() = "小镇PHome传送系统"

    lateinit var manager: TownPHomeManager
        private set
    private lateinit var gui: TownPHomeGUI

    override fun doEnable() {
        val javaPlugin = context.plugin
        
        manager = TownPHomeManager(javaPlugin)
        gui = TownPHomeGUI(javaPlugin, manager)
        
        // 注册 GUI 监听器，使 InventoryClickEvent 生效
        registerListener(gui)
    }

    override fun doDisable() {
        manager.shutdown()
    }

    override fun doReload() {
        manager.loadConfig()
        manager.storage.loadAll()
    }

    override fun getCommandHandler(): SubCommandHandler = TownPHomeCommand(manager, gui)
}
