package org.tsl.tSLplugins

import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import org.tsl.tSLplugins.Advancement.AdvancementMessage
import org.tsl.tSLplugins.Advancement.AdvancementCount
import org.tsl.tSLplugins.Advancement.AdvancementCommand
import org.tsl.tSLplugins.Motd.FakePlayerMotd
import org.tsl.tSLplugins.Visitor.VisitorEffect
import org.tsl.tSLplugins.Visitor.VisitorCommand
import org.tsl.tSLplugins.Farmprotect.FarmProtect
import org.tsl.tSLplugins.Permission.PermissionChecker
import org.tsl.tSLplugins.Alias.AliasManager
import org.tsl.tSLplugins.Alias.AliasCommand
import org.tsl.tSLplugins.Maintenance.MaintenanceManager
import org.tsl.tSLplugins.Maintenance.MaintenanceCommand
import org.tsl.tSLplugins.Maintenance.MaintenanceLoginListener
import org.tsl.tSLplugins.Maintenance.MaintenanceMotdListener
import org.tsl.tSLplugins.Maintenance.MaintenancePermissionListener
import org.tsl.tSLplugins.Scale.ScaleManager
import org.tsl.tSLplugins.Scale.ScaleCommand
import org.tsl.tSLplugins.Hat.HatManager
import org.tsl.tSLplugins.Hat.HatCommand
import org.tsl.tSLplugins.Ping.PingManager
import org.tsl.tSLplugins.Ping.PingCommand
import org.tsl.tSLplugins.Toss.TossManager
import org.tsl.tSLplugins.Toss.TossCommand
import org.tsl.tSLplugins.Toss.TossListener
import org.tsl.tSLplugins.Ride.RideManager
import org.tsl.tSLplugins.Ride.RideCommand
import org.tsl.tSLplugins.Ride.RideListener
import org.tsl.tSLplugins.BabyLock.BabyLockManager
import org.tsl.tSLplugins.BabyLock.BabyLockListener
import org.tsl.tSLplugins.Kiss.KissManager
import org.tsl.tSLplugins.Kiss.KissCommand
import org.tsl.tSLplugins.Kiss.KissExecutor
import org.tsl.tSLplugins.Kiss.KissListener
import org.tsl.tSLplugins.Freeze.FreezeManager
import org.tsl.tSLplugins.Freeze.FreezeCommand
import org.tsl.tSLplugins.Freeze.FreezeListener
import org.tsl.tSLplugins.BlockStats.BlockStatsManager
import org.tsl.tSLplugins.ChatBubble.ChatBubbleManager
import org.tsl.tSLplugins.ChatBubble.ChatBubbleCommand
import org.tsl.tSLplugins.ChatBubble.ChatBubbleListener
import org.tsl.tSLplugins.Speed.SpeedManager
import org.tsl.tSLplugins.Speed.SpeedCommand
import org.tsl.tSLplugins.FixGhost.FixGhostManager
import org.tsl.tSLplugins.FixGhost.FixGhostCommand
import org.tsl.tSLplugins.PlayerList.PlayerListCommand
import org.tsl.tSLplugins.Near.NearManager
import org.tsl.tSLplugins.Near.NearCommand
import org.tsl.tSLplugins.Phantom.PhantomManager
import org.tsl.tSLplugins.Phantom.PhantomCommand
import org.tsl.tSLplugins.NewbieTag.NewbieTagManager
import org.tsl.tSLplugins.Spec.SpecManager
import org.tsl.tSLplugins.Spec.SpecCommand
import org.tsl.tSLplugins.Spec.SpecListener
import org.tsl.tSLplugins.Patrol.PatrolManager
import org.tsl.tSLplugins.Patrol.PatrolCommand
import org.tsl.tSLplugins.WebBridge.WebBridgeManager
import org.tsl.tSLplugins.WebBridge.WebBridgeCommand
import org.tsl.tSLplugins.EndDragon.EndDragonManager
import org.tsl.tSLplugins.EndDragon.EndDragonCommand
import org.tsl.tSLplugins.EndDragon.EndDragonListener
import org.tsl.tSLplugins.PlayerCountCmd.PlayerCountCmdController
import org.tsl.tSLplugins.Ignore.IgnoreManager
import org.tsl.tSLplugins.Ignore.IgnoreCommand
import org.tsl.tSLplugins.Ignore.IgnoreChatListener
import org.tsl.tSLplugins.SnowAntiMelt.SnowAntiMeltListener
import org.tsl.tSLplugins.TimedAttribute.TimedAttributeManager
import org.tsl.tSLplugins.TimedAttribute.TimedAttributeCommand
import org.tsl.tSLplugins.TimedAttribute.TimedAttributeListener
import org.tsl.tSLplugins.Neko.NekoManager
import org.tsl.tSLplugins.Neko.NekoCommand
import org.tsl.tSLplugins.Neko.NekoChatListener
import org.tsl.tSLplugins.Mcedia.McediaManager
import org.tsl.tSLplugins.Mcedia.McediaCommand
import org.tsl.tSLplugins.Mcedia.McediaGUI
import org.tsl.tSLplugins.Mcedia.McediaListener
import org.tsl.tSLplugins.RandomVariable.RandomVariableManager
import org.tsl.tSLplugins.Peace.PeaceManager
import org.tsl.tSLplugins.Peace.PeaceCommand
import org.tsl.tSLplugins.Peace.PeaceListener
import org.tsl.tSLplugins.SuperSnowball.SuperSnowballManager
import org.tsl.tSLplugins.SuperSnowball.SuperSnowballCommand
import org.tsl.tSLplugins.SuperSnowball.SuperSnowballListener
import org.tsl.tSLplugins.RedstoneFreeze.RedstoneFreezeManager
import org.tsl.tSLplugins.RedstoneFreeze.RedstoneFreezeCommand
import org.tsl.tSLplugins.RedstoneFreeze.RedstoneFreezeListener
import org.tsl.tSLplugins.PapiAlias.PapiAliasManager
import org.tsl.tSLplugins.PapiAlias.PapiAliasCommand
import org.tsl.tSLplugins.PlayTime.PlayTimeManager
import org.tsl.tSLplugins.PlayTime.PlayTimeCommand
import org.tsl.tSLplugins.PlayTime.PlayTimeListener

class TSLplugins : JavaPlugin() {

    private lateinit var playerDataManager: PlayerDataManager
    private lateinit var countHandler: AdvancementCount
    private lateinit var aliasManager: AliasManager
    private lateinit var hatManager: HatManager
    private lateinit var maintenanceManager: MaintenanceManager
    private lateinit var scaleManager: ScaleManager
    private lateinit var pingManager: PingManager
    private lateinit var tossManager: TossManager
    private lateinit var rideManager: RideManager
    private lateinit var babyLockManager: BabyLockManager
    private lateinit var kissManager: KissManager
    private lateinit var freezeManager: FreezeManager
    private lateinit var blockStatsManager: BlockStatsManager
    private lateinit var chatBubbleManager: ChatBubbleManager
    private lateinit var speedManager: SpeedManager
    private lateinit var fixGhostManager: FixGhostManager
    private lateinit var nearManager: NearManager
    private lateinit var phantomManager: PhantomManager
    private lateinit var newbieTagManager: NewbieTagManager
    private lateinit var specManager: SpecManager
    private lateinit var patrolManager: PatrolManager
    private lateinit var webBridgeManager: WebBridgeManager
    private lateinit var endDragonManager: EndDragonManager
    private lateinit var ignoreManager: IgnoreManager
    private lateinit var snowAntiMeltListener: SnowAntiMeltListener
    private lateinit var timedAttributeManager: TimedAttributeManager
    private lateinit var nekoManager: NekoManager
    private lateinit var mcediaManager: McediaManager
    private lateinit var mcediaGUI: McediaGUI
    private lateinit var randomVariableManager: RandomVariableManager
    private lateinit var peaceManager: PeaceManager
    private lateinit var superSnowballManager: SuperSnowballManager
    private lateinit var redstoneFreezeManager: RedstoneFreezeManager
    private lateinit var papiAliasManager: PapiAliasManager
    private lateinit var playTimeManager: PlayTimeManager
    private lateinit var advancementMessage: AdvancementMessage
    private lateinit var farmProtect: FarmProtect
    private lateinit var visitorEffect: VisitorEffect
    private lateinit var permissionChecker: PermissionChecker
    
    // 消息管理器（全局单例）
    lateinit var messageManager: MessageManager
        private set

    override fun onEnable() {
        // 首先预验证和修复配置文件（在任何 YAML 解析之前）
        val configUpdateManager = ConfigUpdateManager(this)
        if (!configUpdateManager.preValidateAndRepair()) {
            logger.severe("配置文件无法加载，插件将禁用部分功能")
        }
        
        // 检查并更新配置文件版本
        val configUpdated = configUpdateManager.checkAndUpdate()

        // 如果配置文件被更新，重新加载配置（确保获取最新的配置）
        if (configUpdated) {
            // 再次验证配置文件，防止合并后出现问题
            if (!configUpdateManager.preValidateAndRepair()) {
                logger.warning("合并后的配置文件仍有问题，尝试使用默认配置")
            }
            try {
                reloadConfig()
            } catch (e: Exception) {
                logger.severe("重新加载配置失败: ${e.message}")
                logger.info("尝试重置为默认配置...")
                saveResource("config.yml", true)
                reloadConfig()
            }
        }

        // 初始化消息管理器（最先初始化，供其他模块使用）
        messageManager = MessageManager(this)
        
        // 初始化全局数据库管理器
        DatabaseManager.init(this)

        // 初始化玩家数据管理器（YAML 存储 + PDC 迁移）
        playerDataManager = PlayerDataManager(this)

        val pm = server.pluginManager

        // 注册玩家数据加载/保存监听器
        pm.registerEvents(object : org.bukkit.event.Listener {
            @org.bukkit.event.EventHandler
            fun onPlayerJoin(event: org.bukkit.event.player.PlayerJoinEvent) {
                playerDataManager.onPlayerJoin(event.player)

                // 加载屏蔽数据到 IgnoreManager（需要在 ignoreManager 初始化后）
                if (::ignoreManager.isInitialized && ignoreManager.isEnabled()) {
                    val ignoreList = playerDataManager.getIgnoreList(event.player)
                    ignoreManager.loadPlayerData(event.player.uniqueId, ignoreList)
                }
            }

            @org.bukkit.event.EventHandler
            fun onPlayerQuit(event: org.bukkit.event.player.PlayerQuitEvent) {
                // 先保存屏幕数据
                if (::ignoreManager.isInitialized && ignoreManager.isEnabled()) {
                    val ignoreList = ignoreManager.getPlayerData(event.player.uniqueId)
                    playerDataManager.setIgnoreList(event.player, ignoreList)
                    ignoreManager.unloadPlayerData(event.player.uniqueId)
                }

                playerDataManager.onPlayerQuit(event.player)
            }
        }, this)

        // 注册事件监听器
        advancementMessage = AdvancementMessage(this)
        pm.registerEvents(advancementMessage, this)
        pm.registerEvents(FakePlayerMotd(this), this)
        visitorEffect = VisitorEffect(this)
        pm.registerEvents(visitorEffect, this)
        farmProtect = FarmProtect(this)
        pm.registerEvents(farmProtect, this)
        permissionChecker = PermissionChecker(this)
        pm.registerEvents(permissionChecker, this)


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

        // 初始化 Hat 系统
        hatManager = HatManager(this)


        // 初始化体型调整系统
        scaleManager = ScaleManager(this)

        // 初始化 Ping 系统
        pingManager = PingManager(this)

        // 初始化 Toss 系统
        tossManager = TossManager(this, playerDataManager)
        val tossListener = TossListener(this, tossManager)
        pm.registerEvents(tossListener, this)

        // 初始化 Ride 系统
        rideManager = RideManager(this, playerDataManager)
        val rideListener = RideListener(this, rideManager)
        pm.registerEvents(rideListener, this)

        // 初始化 BabyLock 系统
        babyLockManager = BabyLockManager(this)
        val babyLockListener = BabyLockListener(this, babyLockManager)
        pm.registerEvents(babyLockListener, this)

        // 初始化 Kiss 系统
        kissManager = KissManager(this, playerDataManager)
        val kissExecutor = KissExecutor(this, kissManager)
        val kissListener = KissListener(this, kissManager, kissExecutor)
        pm.registerEvents(kissListener, this)

        // 初始化 Freeze 系统
        freezeManager = FreezeManager(this)
        val freezeListener = FreezeListener(this, freezeManager)
        pm.registerEvents(freezeListener, this)

        // 初始化 BlockStats 系统
        blockStatsManager = BlockStatsManager(this)

        // 初始化 ChatBubble 系统
        chatBubbleManager = ChatBubbleManager(this)
        val chatBubbleListener = ChatBubbleListener(this, chatBubbleManager)
        pm.registerEvents(chatBubbleListener, this)

        // 初始化 Speed 系统
        speedManager = SpeedManager()

        // 初始化 FixGhost 系统
        fixGhostManager = FixGhostManager(this)


        // 初始化 Near 系统
        nearManager = NearManager(this)

        // 初始化 Phantom 系统
        phantomManager = PhantomManager(this)
        phantomManager.setProfileStore(playerDataManager.getProfileStore())
        // 启动定时任务
        phantomManager.startTask()

        // 初始化 NewbieTag 系统
        newbieTagManager = NewbieTagManager(this)

        // 初始化 Spec 系统
        specManager = SpecManager(this)
        val specListener = SpecListener(this, specManager)
        pm.registerEvents(specListener, this)

        // 初始化 Patrol 系统
        patrolManager = PatrolManager(this)

        // 初始化 WebBridge 系统
        webBridgeManager = WebBridgeManager(this)
        webBridgeManager.initialize()

        // 初始化末影龙控制系统
        endDragonManager = EndDragonManager(this)
        val endDragonListener = EndDragonListener(this, endDragonManager)
        pm.registerEvents(endDragonListener, this)

        // 初始化 Ignore 聊天屏蔽系统
        ignoreManager = IgnoreManager(this)
        ignoreManager.loadConfig()
        val ignoreChatListener = IgnoreChatListener(this, ignoreManager)
        pm.registerEvents(ignoreChatListener, this)

        // 初始化雪防融化系统
        snowAntiMeltListener = SnowAntiMeltListener(this)
        snowAntiMeltListener.loadConfig()
        pm.registerEvents(snowAntiMeltListener, this)

        // 初始化人数控制命令系统
        initPlayerCountCmd()

        // 初始化计时属性效果系统
        timedAttributeManager = TimedAttributeManager(this)
        val timedAttributeListener = TimedAttributeListener(this, timedAttributeManager)
        pm.registerEvents(timedAttributeListener, this)
        timedAttributeManager.startExpirationTask()

        // 初始化猫娘模式系统
        nekoManager = NekoManager(this)
        val nekoChatListener = NekoChatListener(this, nekoManager)
        pm.registerEvents(nekoChatListener, this)
        nekoManager.startExpirationTask()

        // 初始化 Mcedia 视频播放器系统
        mcediaManager = McediaManager(this)
        mcediaGUI = McediaGUI(this, mcediaManager)
        val mcediaListener = McediaListener(this, mcediaManager, mcediaGUI)
        pm.registerEvents(mcediaGUI, this)
        pm.registerEvents(mcediaListener, this)

        // 初始化 RandomVariable 混合分布随机数系统
        randomVariableManager = RandomVariableManager(this)

        // 初始化 Peace 伪和平模式系统
        peaceManager = PeaceManager(this)
        val peaceListener = PeaceListener(this, peaceManager)
        pm.registerEvents(peaceListener, this)
        peaceManager.startExpirationTask()

        // 初始化 SuperSnowball 超级大雪球系统
        superSnowballManager = SuperSnowballManager(this)
        val superSnowballListener = SuperSnowballListener(this, superSnowballManager)
        pm.registerEvents(superSnowballListener, this)

        // 初始化 RedstoneFreeze 红石冻结系统
        redstoneFreezeManager = RedstoneFreezeManager(this)
        val redstoneFreezeListener = RedstoneFreezeListener(redstoneFreezeManager)
        pm.registerEvents(redstoneFreezeListener, this)

        // 初始化 PapiAlias 变量映射系统
        papiAliasManager = PapiAliasManager(this)

        // 初始化 PlayTime 在线时长统计系统
        playTimeManager = PlayTimeManager(this)
        val playTimeListener = PlayTimeListener(playTimeManager)
        pm.registerEvents(playTimeListener, this)
        playTimeManager.startSaveTask()

        // 注册命令 - 使用新的命令分发架构
        getCommand("tsl")?.let { command ->
            val dispatcher = TSLCommand()

            // 注册各个功能模块的命令处理器
            dispatcher.registerSubCommand("advcount", AdvancementCommand(this, countHandler))
            dispatcher.registerSubCommand("aliasreload", AliasCommand(this, aliasManager))
            dispatcher.registerSubCommand("maintenance", MaintenanceCommand(maintenanceManager, maintenancePermissionListener))
            dispatcher.registerSubCommand("scale", ScaleCommand(this, scaleManager))
            dispatcher.registerSubCommand("hat", HatCommand(this, hatManager))
            dispatcher.registerSubCommand("ping", PingCommand(pingManager))
            dispatcher.registerSubCommand("toss", TossCommand(tossManager))
            dispatcher.registerSubCommand("ride", RideCommand(rideManager))
            dispatcher.registerSubCommand("kiss", KissCommand(kissManager, kissExecutor))
            dispatcher.registerSubCommand("freeze", FreezeCommand(freezeManager))
            dispatcher.registerSubCommand("chatbubble", ChatBubbleCommand(chatBubbleManager))
            dispatcher.registerSubCommand("visitor", VisitorCommand(visitorEffect))
            dispatcher.registerSubCommand("speed", SpeedCommand(speedManager))
            dispatcher.registerSubCommand("fixghost", FixGhostCommand(this, fixGhostManager))
            dispatcher.registerSubCommand("list", PlayerListCommand())
            dispatcher.registerSubCommand("near", NearCommand(nearManager))
            dispatcher.registerSubCommand("phantom", PhantomCommand(phantomManager))
            dispatcher.registerSubCommand("spec", SpecCommand(specManager))
            dispatcher.registerSubCommand("patrol", PatrolCommand(patrolManager))
            dispatcher.registerSubCommand("webbridge", WebBridgeCommand(webBridgeManager))
            dispatcher.registerSubCommand("enddragon", EndDragonCommand(endDragonManager))
            dispatcher.registerSubCommand("ignore", IgnoreCommand(ignoreManager, playerDataManager))
            dispatcher.registerSubCommand("attr", TimedAttributeCommand(timedAttributeManager))
            dispatcher.registerSubCommand("neko", NekoCommand(nekoManager))
            dispatcher.registerSubCommand("mcedia", McediaCommand(mcediaManager, mcediaGUI))
            dispatcher.registerSubCommand("peace", PeaceCommand(peaceManager))
            dispatcher.registerSubCommand("ss", SuperSnowballCommand(superSnowballManager))
            dispatcher.registerSubCommand("redfreeze", RedstoneFreezeCommand(this, redstoneFreezeManager))
            dispatcher.registerSubCommand("papialias", PapiAliasCommand(papiAliasManager))
            dispatcher.registerSubCommand("playtime", PlayTimeCommand(playTimeManager))
            dispatcher.registerSubCommand("reload", ReloadCommand(this))

            command.setExecutor(dispatcher)
            command.tabCompleter = dispatcher
        }


        // 注册 PlaceholderAPI 扩展（如果 PlaceholderAPI 已加载）
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            // 注册统一的 PAPI 扩展，包含所有变量
            TSLPlaceholderExpansion(
                this,
                countHandler,
                pingManager,
                kissManager,
                rideManager,
                tossManager,
                blockStatsManager,
                newbieTagManager,
                randomVariableManager,
                papiAliasManager,
                playTimeManager
            ).register()
            
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
        // 清理 Alias 系统（注销动态命令）
        if (::aliasManager.isInitialized) {
            aliasManager.cleanup()
        }

        // 停止 Phantom 定时任务
        if (::phantomManager.isInitialized) {
            phantomManager.stopTask()
        }

        // 保存所有玩家数据
        if (::playerDataManager.isInitialized) {
            playerDataManager.saveAll()
        }

        // 清理 Spec 系统
        if (::specManager.isInitialized) {
            specManager.cleanup()
        }

        // 清理 Patrol 系统
        if (::patrolManager.isInitialized) {
            patrolManager.cleanup()
        }

        // 清理 ChatBubble 系统
        if (::chatBubbleManager.isInitialized) {
            chatBubbleManager.cleanupAll()
        }

        // 清理 WebBridge 系统
        if (::webBridgeManager.isInitialized) {
            webBridgeManager.shutdown()
        }

        // 清理 TimedAttribute 系统
        if (::timedAttributeManager.isInitialized) {
            timedAttributeManager.shutdown()
        }

        // 清理 Neko 系统
        if (::nekoManager.isInitialized) {
            nekoManager.shutdown()
        }

        // 清理 Mcedia 系统
        if (::mcediaManager.isInitialized) {
            mcediaManager.shutdown()
        }

        // 清理 RedstoneFreeze 系统
        if (::redstoneFreezeManager.isInitialized) {
            redstoneFreezeManager.cleanup()
        }

        // 清理 PlayTime 在线时长系统
        if (::playTimeManager.isInitialized) {
            playTimeManager.shutdown()
        }

        // 关闭全局数据库管理器
        DatabaseManager.shutdown()

        logger.info("TSL插件已卸载！")
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
        maintenanceManager.loadConfig()
    }

    /**
     * 重新加载体型管理器
     */
    fun reloadScaleManager() {
        scaleManager.loadConfig()
    }

    /**
     * 重新加载 Hat 管理器
     */
    fun reloadHatManager() {
        hatManager.loadConfig()
    }

    /**
     * 重新加载成就消息过滤器
     */
    fun reloadAdvancementMessage() {
        advancementMessage.loadConfig()
    }

    /**
     * 重新加载农田保护
     */
    fun reloadFarmProtect() {
        farmProtect.loadConfig()
    }

    /**
     * 重新加载访客模式
     */
    fun reloadVisitorEffect() {
        visitorEffect.loadConfig()
    }

    /**
     * 重新加载权限检测器
     */
    fun reloadPermissionChecker() {
        permissionChecker.reload()
    }


    /**
     * 重新加载 Ping 管理器
     */
    fun reloadPingManager() {
        pingManager.loadConfig()
    }

    /**
     * 重新加载 Toss 管理器
     */
    fun reloadTossManager() {
        tossManager.loadConfig()
    }

    /**
     * 重新加载 Ride 管理器
     */
    fun reloadRideManager() {
        rideManager.loadConfig()
    }

    /**
     * 重新加载 BabyLock 管理器
     */
    fun reloadBabyLockManager() {
        babyLockManager.loadConfig()
    }

    /**
     * 重新加载 Kiss 管理器
     */
    fun reloadKissManager() {
        kissManager.loadConfig()
    }

    /**
     * 重新加载 Freeze 管理器
     */
    fun reloadFreezeManager() {
        freezeManager.loadConfig()
    }

    /**
     * 重新加载 BlockStats 管理器
     */
    fun reloadBlockStatsManager() {
        blockStatsManager.loadConfig()
    }

    /**
     * 重新加载 ChatBubble 管理器
     */
    fun reloadChatBubbleManager() {
        chatBubbleManager.loadConfig()
    }

    /**
     * 重新加载 FixGhost 管理器
     */
    fun reloadFixGhostManager() {
        fixGhostManager.loadConfig()
    }


    /**
     * 重新加载 Near 管理器
     */
    fun reloadNearManager() {
        nearManager.loadConfig()
    }

    /**
     * 重新加载 Phantom 管理器
     */
    fun reloadPhantomManager() {
        phantomManager.loadConfig()
        phantomManager.startTask()  // 重启定时任务以应用新的时间间隔
    }

    /**
     * 重新加载 NewbieTag 管理器
     */
    fun reloadNewbieTagManager() {
        newbieTagManager.loadConfig()
    }

    /**
     * 重新加载 Spec 管理器
     */
    fun reloadSpecManager() {
        specManager.loadConfig()
    }

    /**
     * 重新加载 EndDragon 管理器
     */
    fun reloadEndDragonManager() {
        endDragonManager.loadConfig()
    }

    /**
     * 重新加载 WebBridge 管理器
     * 支持运行时动态启用/禁用
     */
    fun reloadWebBridgeManager() {
        webBridgeManager.reload()
    }

    /**
     * 重新加载 Ignore 管理器
     */
    fun reloadIgnoreManager() {
        ignoreManager.loadConfig()
    }

    /**
     * 重新加载雪防融化监听器
     */
    fun reloadSnowAntiMeltListener() {
        snowAntiMeltListener.loadConfig()
    }

    /**
     * 重新加载计时属性效果管理器
     */
    fun reloadTimedAttributeManager() {
        timedAttributeManager.loadConfig()
    }

    /**
     * 重新加载猫娘模式管理器
     */
    fun reloadNekoManager() {
        nekoManager.loadConfig()
    }

    /**
     * 重新加载 Mcedia 管理器
     */
    fun reloadMcediaManager() {
        mcediaManager.loadConfig()
    }

    /**
     * 重新加载 RandomVariable 管理器
     */
    fun reloadRandomVariableManager() {
        randomVariableManager.reload()
    }

    /**
     * 重新加载 Peace 管理器
     */
    fun reloadPeaceManager() {
        peaceManager.loadConfig()
    }

    /**
     * 重新加载 SuperSnowball 管理器
     */
    fun reloadSuperSnowballManager() {
        superSnowballManager.loadConfig()
    }

    /**
     * 重新加载 RedstoneFreeze 管理器
     */
    fun reloadRedstoneFreezeManager() {
        redstoneFreezeManager.loadConfig()
    }

    /**
     * 重新加载 PapiAlias 管理器
     */
    fun reloadPapiAliasManager() {
        papiAliasManager.loadConfig()
    }

    /**
     * 初始化人数控制命令模块
     * 根据配置决定是否启用
     */
    private fun initPlayerCountCmd() {
        val cfg = config.getConfigurationSection("player-count-cmd")
        if (cfg == null || !cfg.getBoolean("enabled", true)) {
            logger.info("[PlayerCountCmd] 功能未启用")
            return
        }

        PlayerCountCmdController(
            plugin = this,
            upperThreshold = cfg.getInt("upper-threshold", 52),
            lowerThreshold = cfg.getInt("lower-threshold", 48),
            minIntervalMs = cfg.getLong("min-interval-ms", 10_000L),
            cmdWhenLow = cfg.getString("command-when-low", "chunky continue") ?: "chunky continue",
            cmdWhenHigh = cfg.getString("command-when-high", "chunky pause") ?: "chunky pause"
        ).init()
    }
}
