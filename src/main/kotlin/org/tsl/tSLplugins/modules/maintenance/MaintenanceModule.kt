package org.tsl.tSLplugins.modules.maintenance

import org.bukkit.plugin.java.JavaPlugin
import org.tsl.tSLplugins.SubCommandHandler
import org.tsl.tSLplugins.core.AbstractModule

/**
 * 维护模式模块
 * 管理服务器维护状态和白名单
 */
class MaintenanceModule : AbstractModule() {
    override val id = "maintenance"
    override val configPath = "maintenance"
    override fun getDescription() = "服务器维护模式管理"

    lateinit var manager: MaintenanceManager
        private set
    private lateinit var loginListener: MaintenanceLoginListener
    private lateinit var permissionListener: MaintenancePermissionListener
    private lateinit var motdListener: MaintenanceMotdListener

    override fun doEnable() {
        val javaPlugin = context.plugin
        
        manager = MaintenanceManager(javaPlugin)
        
        loginListener = MaintenanceLoginListener(manager)
        permissionListener = MaintenancePermissionListener(javaPlugin, manager)
        motdListener = MaintenanceMotdListener(manager)
        
        registerListener(loginListener)
        registerListener(permissionListener)
        registerListener(motdListener)
    }

    override fun doReload() {
        manager.loadConfig()
    }

    override fun getCommandHandler(): SubCommandHandler = MaintenanceCommand(manager, permissionListener)
}
