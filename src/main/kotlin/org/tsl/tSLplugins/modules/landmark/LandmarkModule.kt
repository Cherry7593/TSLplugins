package org.tsl.tSLplugins.modules.landmark

import org.bukkit.plugin.java.JavaPlugin
import org.tsl.tSLplugins.SubCommandHandler
import org.tsl.tSLplugins.core.AbstractModule

/**
 * 地标系统模块
 * 管理传送地标、区域检测和GUI
 */
class LandmarkModule : AbstractModule() {
    override val id = "landmark"
    override val configPath = "landmark"
    override fun getDescription() = "地标传送系统"

    lateinit var manager: LandmarkManager
        private set
    private lateinit var listener: LandmarkListener
    private lateinit var gui: LandmarkGUI
    private lateinit var opkTool: LandmarkOPKTool
    private lateinit var compass: LandmarkCompass

    override fun doEnable() {
        val javaPlugin = context.plugin
        
        manager = LandmarkManager(javaPlugin)
        compass = LandmarkCompass(javaPlugin, manager)
        listener = LandmarkListener(javaPlugin, manager, compass)
        gui = LandmarkGUI(javaPlugin, manager, listener)
        opkTool = LandmarkOPKTool(javaPlugin, manager)
        
        registerListener(listener)
        registerListener(gui)  // GUI 也是 Listener
        registerListener(opkTool)  // OPK 工具也是 Listener
    }

    override fun doDisable() {
        compass.shutdown()
        manager.shutdown()
    }

    override fun doReload() {
        manager.loadConfig()
        manager.storage.loadAll()
        compass.loadConfig()
    }

    override fun getCommandHandler(): SubCommandHandler = 
        LandmarkCommand(context.plugin, manager, gui, opkTool, compass)
}
