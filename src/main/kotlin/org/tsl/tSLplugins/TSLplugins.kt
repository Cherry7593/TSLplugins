package org.tsl.tSLplugins

import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import org.tsl.tSLplugins.Advancement.AdvancementMessage
import org.tsl.tSLplugins.Advancement.AdvancementCount
import org.tsl.tSLplugins.Advancement.AdvancementCommand
import org.tsl.tSLplugins.Advancement.TSLPlaceholderExpansion
import org.tsl.tSLplugins.Motd.FakePlayerMotd
import org.tsl.tSLplugins.Visitor.VisitorEffect
import org.tsl.tSLplugins.Farmprotect.FarmProtect
import org.tsl.tSLplugins.Permission.PermissionChecker
import org.tsl.tSLplugins.Alias.AliasManager
import org.tsl.tSLplugins.Alias.AliasCommand
import org.tsl.tSLplugins.Maintenance.MaintenanceManager
import org.tsl.tSLplugins.Maintenance.MaintenanceCommand
import org.tsl.tSLplugins.Maintenance.MaintenanceLoginListener
import org.tsl.tSLplugins.Maintenance.MaintenanceMotdListener
import org.tsl.tSLplugins.Maintenance.MaintenancePermissionListener
import org.tsl.tSLplugins.Bossvoice.BossvoiceListener

class TSLplugins : JavaPlugin() {

    private lateinit var countHandler: AdvancementCount
    private lateinit var aliasManager: AliasManager
    private lateinit var maintenanceManager: MaintenanceManager
    private lateinit var bossvoiceListener: BossvoiceListener

    override fun onEnable() {
        // 检查并更新配置文件
        val configUpdateManager = ConfigUpdateManager(this)
        configUpdateManager.checkAndUpdate()

        // 重新加载配置（确保获取最新的配置）
        reloadConfig()

        val pm = server.pluginManager

        // 注册事件监听器
        pm.registerEvents(AdvancementMessage(), this)
        pm.registerEvents(FakePlayerMotd(this), this)
        pm.registerEvents(VisitorEffect(this), this)
        pm.registerEvents(FarmProtect(), this)
        pm.registerEvents(PermissionChecker(this), this)

        // 初始化 Bossvoice 监听器（使用 ProtocolLib）
        bossvoiceListener = BossvoiceListener(this)
        bossvoiceListener.enable()

        // 初始化成就统计系统
        countHandler = AdvancementCount(this)
        pm.registerEvents(countHandler, this)

        // 初始化命令别名系统（动态注册别名命令）
        aliasManager = AliasManager(this)

        // 初始化维护模式系统
        maintenanceManager = MaintenanceManager(this)
        val maintenancePermissionListener = MaintenancePermissionListener(this, maintenanceManager)
        pm.registerEvents(MaintenanceLoginListener(maintenanceManager), this)
        pm.registerEvents(MaintenanceMotdListener(maintenanceManager), this)
        pm.registerEvents(maintenancePermissionListener, this)

        // 注册命令 - 使用新的命令分发架构
        getCommand("tsl")?.let { command ->
            val dispatcher = TSLCommand()

            // 注册各个功能模块的命令处理器
            dispatcher.registerSubCommand("advcount", AdvancementCommand(this, countHandler))
            dispatcher.registerSubCommand("aliasreload", AliasCommand(this, aliasManager))
            dispatcher.registerSubCommand("maintenance", MaintenanceCommand(maintenanceManager, maintenancePermissionListener))
            dispatcher.registerSubCommand("reload", ReloadCommand(this))

            command.setExecutor(dispatcher)
            command.tabCompleter = dispatcher
        }


        // 注册 PlaceholderAPI 扩展（如果 PlaceholderAPI 已加载）
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            TSLPlaceholderExpansion(this, countHandler).register()
            logger.info("PlaceholderAPI 扩展已注册！")
        } else {
            logger.warning("未检测到 PlaceholderAPI，占位符功能将不可用。")
        }

        // 启动定期清理过期缓存的任务（每 5 分钟清理一次）
        Bukkit.getGlobalRegionScheduler().runAtFixedRate(this, { _ ->
            countHandler.cleanExpiredCache()
        }, 6000L, 6000L) // 延迟 5 分钟，每 5 分钟执行一次

        logger.info("TSL插件启动成功！")
        logger.info("命令别名系统已加载 ${aliasManager.getAliasCount()} 个别名")
        logger.info("维护模式状态: ${if (maintenanceManager.isMaintenanceEnabled()) "已启用" else "未启用"}")
    }

    override fun onDisable() {
        // 停用 Bossvoice 监听器
        if (::bossvoiceListener.isInitialized) {
            bossvoiceListener.disable()
        }
    }

    /**
     * 重新加载别名管理器
     * @return 重载的别名数量
     */
    fun reloadAliasManager(): Int {
        aliasManager.reloadAliases()
        return aliasManager.getAliasCount()
    }

    /**
     * 重新加载维护模式管理器
     */
    fun reloadMaintenanceManager() {
        // 维护模式会自动从配置文件读取
        // 这里只需要确保配置已重载
    }
}