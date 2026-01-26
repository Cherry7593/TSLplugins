package org.tsl.tSLplugins

import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import org.tsl.tSLplugins.service.MessageManager
import org.tsl.tSLplugins.service.DatabaseManager
import org.tsl.tSLplugins.service.PlayerDataManager
import org.tsl.tSLplugins.modules.alias.AliasManager
import org.tsl.tSLplugins.modules.playerlist.PlayerListCommand
import org.tsl.tSLplugins.modules.playerlist.PlayerListModule
// 新架构导入
import org.tsl.tSLplugins.core.ModuleRegistry
import org.tsl.tSLplugins.modules.freeze.FreezeModule
import org.tsl.tSLplugins.modules.scale.ScaleModule
import org.tsl.tSLplugins.modules.hat.HatModule
import org.tsl.tSLplugins.modules.kiss.KissModule
import org.tsl.tSLplugins.modules.toss.TossModule
import org.tsl.tSLplugins.modules.ride.RideModule
import org.tsl.tSLplugins.modules.ping.PingModule
import org.tsl.tSLplugins.modules.babylock.BabyLockModule
import org.tsl.tSLplugins.modules.phantom.PhantomModule
import org.tsl.tSLplugins.modules.near.NearModule
import org.tsl.tSLplugins.modules.speed.SpeedModule
import org.tsl.tSLplugins.modules.fixghost.FixGhostModule
import org.tsl.tSLplugins.modules.chatbubble.ChatBubbleModule
import org.tsl.tSLplugins.modules.spec.SpecModule
import org.tsl.tSLplugins.modules.peace.PeaceModule
import org.tsl.tSLplugins.modules.ignore.IgnoreModule
import org.tsl.tSLplugins.modules.vote.VoteModule
import org.tsl.tSLplugins.modules.minecartboost.MinecartBoostModule
import org.tsl.tSLplugins.modules.supersnowball.SuperSnowballModule
import org.tsl.tSLplugins.modules.deathpenalty.DeathPenaltyModule
import org.tsl.tSLplugins.modules.playtime.PlayTimeModule
import org.tsl.tSLplugins.modules.patrol.PatrolModule
import org.tsl.tSLplugins.modules.visitor.VisitorModule
import org.tsl.tSLplugins.modules.redstonefreeze.RedstoneFreezeModule
import org.tsl.tSLplugins.modules.enddragon.EndDragonModule
import org.tsl.tSLplugins.modules.vault.VaultModule
import org.tsl.tSLplugins.modules.neko.NekoModule
import org.tsl.tSLplugins.modules.maintenance.MaintenanceModule
import org.tsl.tSLplugins.modules.mcedia.McediaModule
import org.tsl.tSLplugins.modules.landmark.LandmarkModule
import org.tsl.tSLplugins.modules.title.TitleModule
import org.tsl.tSLplugins.modules.townphome.TownPHomeModule
import org.tsl.tSLplugins.modules.webbridge.WebBridgeModule
import org.tsl.tSLplugins.modules.advancement.AdvancementModule
import org.tsl.tSLplugins.modules.alias.AliasModule
import org.tsl.tSLplugins.modules.timedattribute.TimedAttributeModule
import org.tsl.tSLplugins.modules.farmprotect.FarmProtectModule
import org.tsl.tSLplugins.modules.snowantimelt.SnowAntiMeltModule
import org.tsl.tSLplugins.modules.fakemotd.FakeMotdModule
import org.tsl.tSLplugins.modules.permissionchecker.PermissionCheckerModule
import org.tsl.tSLplugins.modules.playercountcmd.PlayerCountCmdModule
import org.tsl.tSLplugins.modules.xconomytrigger.XconomyTriggerModule
// PAPI 相关模块
import org.tsl.tSLplugins.modules.blockstats.BlockStatsModule
import org.tsl.tSLplugins.modules.newbietag.NewbieTagModule
import org.tsl.tSLplugins.modules.papialias.PapiAliasModule
import org.tsl.tSLplugins.modules.randomvariable.RandomVariableModule

/**
 * TSLplugins 主类
 * 
 * 采用新架构设计：
 * - 使用 ModuleRegistry 管理所有功能模块
 * - 模块自动处理命令注册、监听器注册、配置加载
 * - 统一的生命周期管理
 */
class TSLplugins : JavaPlugin() {

    // ========== 核心服务 ==========
    private lateinit var playerDataManager: PlayerDataManager
    
    /** 消息管理器（全局单例） */
    lateinit var messageManager: MessageManager
        private set
    
    /** 模块注册器 */
    private lateinit var moduleRegistry: ModuleRegistry
    
    // ========== 特殊管理器 ==========
    private lateinit var aliasManager: AliasManager  // 动态命令别名

    override fun onEnable() {
        // 首先预验证和修复配置文件
        val configUpdateManager = ConfigUpdateManager(this)
        if (!configUpdateManager.preValidateAndRepair()) {
            logger.severe("配置文件无法加载，插件将禁用部分功能")
        }
        
        // 检查并更新配置文件版本
        val configUpdated = configUpdateManager.checkAndUpdate()
        if (configUpdated) {
            if (!configUpdateManager.preValidateAndRepair()) {
                logger.warning("合并后的配置文件仍有问题，尝试使用默认配置")
            }
            try {
                reloadConfig()
            } catch (e: Exception) {
                logger.severe("重新加载配置失败: ${e.message}")
                saveResource("config.yml", true)
                reloadConfig()
            }
        }

        // 初始化核心服务
        messageManager = MessageManager(this)
        DatabaseManager.init(this)
        playerDataManager = PlayerDataManager(this)

        // ========== 初始化模块注册器并注册所有模块 ==========
        moduleRegistry = ModuleRegistry(this, messageManager, playerDataManager)
        registerAllModules()
        moduleRegistry.enableAll()
        logger.info("[新架构] 已注册 ${moduleRegistry.getModuleCount()} 个模块，${moduleRegistry.getEnabledCount()} 个已启用")

        // 注册玩家数据监听器
        registerPlayerDataListener()
        
        // 初始化命令别名系统
        aliasManager = AliasManager(this)

        // 注册命令
        registerCommands()

        // 注册 PlaceholderAPI 扩展
        registerPlaceholderAPI()

        // 启动定期清理任务
        startCleanupTasks()

        logger.info("TSL插件启动成功！")
        logger.info("命令别名系统已加载 ${aliasManager.getAliasCount()} 个别名")
    }

    override fun onDisable() {
        // 禁用所有模块（自动处理资源清理）
        if (::moduleRegistry.isInitialized) {
            moduleRegistry.disableAll()
        }

        // 清理命令别名系统
        if (::aliasManager.isInitialized) {
            aliasManager.cleanup()
        }

        // 保存所有玩家数据
        if (::playerDataManager.isInitialized) {
            playerDataManager.saveAll()
        }

        // 关闭数据库
        DatabaseManager.shutdown()

        logger.info("TSL插件已卸载！")
    }

    // ==================== 模块注册 ====================

    private fun registerAllModules() {
        // 基础功能模块
        moduleRegistry.register(FreezeModule())
        moduleRegistry.register(ScaleModule())
        moduleRegistry.register(HatModule())
        moduleRegistry.register(KissModule())
        moduleRegistry.register(TossModule())
        moduleRegistry.register(RideModule())
        moduleRegistry.register(PingModule())
        moduleRegistry.register(BabyLockModule())
        moduleRegistry.register(PhantomModule())
        moduleRegistry.register(NearModule())
        moduleRegistry.register(SpeedModule())
        moduleRegistry.register(FixGhostModule())
        moduleRegistry.register(ChatBubbleModule())
        moduleRegistry.register(SpecModule())
        moduleRegistry.register(PeaceModule())
        moduleRegistry.register(IgnoreModule())
        moduleRegistry.register(VoteModule())
        
        // 游戏玩法模块
        moduleRegistry.register(MinecartBoostModule())
        moduleRegistry.register(SuperSnowballModule())
        moduleRegistry.register(DeathPenaltyModule())
        moduleRegistry.register(PlayTimeModule())
        moduleRegistry.register(PatrolModule())
        moduleRegistry.register(VisitorModule())
        moduleRegistry.register(RedstoneFreezeModule())
        moduleRegistry.register(EndDragonModule())
        moduleRegistry.register(VaultModule())
        moduleRegistry.register(NekoModule())
        
        // 系统管理模块
        moduleRegistry.register(MaintenanceModule())
        moduleRegistry.register(McediaModule())
        moduleRegistry.register(LandmarkModule())
        moduleRegistry.register(TitleModule())
        moduleRegistry.register(TownPHomeModule())
        moduleRegistry.register(WebBridgeModule())
        moduleRegistry.register(AdvancementModule())
        moduleRegistry.register(AliasModule())
        moduleRegistry.register(TimedAttributeModule())
        
        // 被动功能模块（无命令）
        moduleRegistry.register(FarmProtectModule())
        moduleRegistry.register(SnowAntiMeltModule())
        moduleRegistry.register(FakeMotdModule())
        moduleRegistry.register(PermissionCheckerModule())
        moduleRegistry.register(PlayerCountCmdModule())
        moduleRegistry.register(XconomyTriggerModule())
        
        // PAPI 数据提供模块
        moduleRegistry.register(BlockStatsModule())
        moduleRegistry.register(NewbieTagModule())
        moduleRegistry.register(PapiAliasModule())
        moduleRegistry.register(RandomVariableModule())
        
        // 工具命令模块
        moduleRegistry.register(PlayerListModule())
    }

    private fun registerPlayerDataListener() {
        server.pluginManager.registerEvents(object : org.bukkit.event.Listener {
            @org.bukkit.event.EventHandler
            fun onPlayerJoin(event: org.bukkit.event.player.PlayerJoinEvent) {
                playerDataManager.onPlayerJoin(event.player)
            }

            @org.bukkit.event.EventHandler
            fun onPlayerQuit(event: org.bukkit.event.player.PlayerQuitEvent) {
                playerDataManager.onPlayerQuit(event.player)
            }
        }, this)
    }

    private fun registerCommands() {
        getCommand("tsl")?.let { command ->
            val dispatcher = TSLCommand()

            // 注册固定命令
            dispatcher.registerSubCommand("list", PlayerListCommand())
            dispatcher.registerSubCommand("reload", ReloadCommand(this))

            // 注册所有模块命令（自动覆盖）
            moduleRegistry.registerCommands(dispatcher)

            command.setExecutor(dispatcher)
            command.tabCompleter = dispatcher
        }
    }

    private fun registerPlaceholderAPI() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            TSLPlaceholderExpansion(
                this,
                moduleRegistry,
                playerDataManager
            ).register()
            logger.info("PlaceholderAPI 扩展已注册！")
        } else {
            logger.warning("未检测到 PlaceholderAPI，占位符功能将不可用。")
        }
    }

    private fun startCleanupTasks() {
        // 成就缓存清理（每 5 分钟）
        moduleRegistry.getModule<AdvancementModule>("advancement")?.let { module ->
            Bukkit.getGlobalRegionScheduler().runAtFixedRate(this, { _ ->
                module.cleanExpiredCache()
            }, 6000L, 6000L)
        }
    }

    // ==================== 公共 API ====================

    /**
     * 重新加载所有模块
     * @return 重载后启用的模块数量
     */
    fun reloadModules(): Int {
        return if (::moduleRegistry.isInitialized) {
            moduleRegistry.reloadAll()
        } else {
            0
        }
    }

    /**
     * 重新加载命令别名
     * @return 重载的别名数量
     */
    fun reloadAliasManager(): Int {
        aliasManager.reloadAliases()
        return aliasManager.getAliasCount()
    }

    /**
     * 重新加载在线玩家数据
     */
    fun reloadPlayerData() {
        playerDataManager.reloadOnlinePlayers()
    }
}
