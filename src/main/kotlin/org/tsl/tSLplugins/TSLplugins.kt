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
    private lateinit var advancementMessage: AdvancementMessage
    private lateinit var farmProtect: FarmProtect
    private lateinit var visitorEffect: VisitorEffect
    private lateinit var permissionChecker: PermissionChecker

    override fun onEnable() {
        // 检查并更新配置文件
        val configUpdateManager = ConfigUpdateManager(this)
        val configUpdated = configUpdateManager.checkAndUpdate()

        // 如果配置文件被更新，重新加载配置（确保获取最新的配置）
        if (configUpdated) {
            reloadConfig()
        }

        // 初始化玩家数据管理器（YAML 存储 + PDC 迁移）
        playerDataManager = PlayerDataManager(this)

        val pm = server.pluginManager

        // 注册玩家数据加载/保存监听器
        pm.registerEvents(object : org.bukkit.event.Listener {
            @org.bukkit.event.EventHandler
            fun onPlayerJoin(event: org.bukkit.event.player.PlayerJoinEvent) {
                playerDataManager.onPlayerJoin(event.player)
            }

            @org.bukkit.event.EventHandler
            fun onPlayerQuit(event: org.bukkit.event.player.PlayerQuitEvent) {
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
            dispatcher.registerSubCommand("reload", ReloadCommand(this))

            command.setExecutor(dispatcher)
            command.tabCompleter = dispatcher
        }


        // 注册 PlaceholderAPI 扩展（如果 PlaceholderAPI 已加载）
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            // 只注册一个统一的扩展，包含所有变量
            TSLPlaceholderExpansion(
                this,
                countHandler,
                pingManager,
                kissManager,
                rideManager,
                tossManager,
                blockStatsManager,
                newbieTagManager
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
}