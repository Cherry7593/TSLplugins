package org.tsl.tSLplugins.modules.mcedia

import org.bukkit.plugin.java.JavaPlugin
import org.tsl.tSLplugins.SubCommandHandler
import org.tsl.tSLplugins.core.AbstractModule

/**
 * Mcedia 视频播放器模块
 * 管理盔甲架视频播放器
 */
class McediaModule : AbstractModule() {
    override val id = "mcedia"
    override val configPath = "mcedia"
    override fun getDescription() = "视频播放器管理"

    lateinit var manager: McediaManager
        private set
    private lateinit var gui: McediaGUI
    private lateinit var listener: McediaListener

    override fun doEnable() {
        val javaPlugin = context.plugin
        
        manager = McediaManager(javaPlugin)
        gui = McediaGUI(javaPlugin, manager)
        
        // 扫描现有播放器
        manager.scanExistingPlayers()
        
        listener = McediaListener(javaPlugin, manager, gui)
        registerListener(listener)
        registerListener(gui)  // 注册 GUI 监听器，使 InventoryClickEvent 生效
    }

    override fun doDisable() {
        manager.shutdown()
    }

    override fun doReload() {
        manager.loadConfig()
    }

    override fun getCommandHandler(): SubCommandHandler = McediaCommand(manager, gui)
}
