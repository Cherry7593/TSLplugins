package org.tsl.tSLplugins.modules.mcedia

import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.EntityType
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BookMeta
import org.bukkit.plugin.java.JavaPlugin
import org.tsl.tSLplugins.TSLplugins
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Mcedia 播放器管理器
 * 负责管理视频播放器盔甲架
 */
class McediaManager(private val plugin: JavaPlugin) {

    private var enabled: Boolean = true
    private var playerNamePrefix: String = "mcedia"
    private var defaultScale: Double = 1.0
    private var defaultVolume: Double = 1.0
    private var maxPlayers: Int = 50
    private var triggerItem: Material? = null

    private val msg get() = (plugin as TSLplugins).messageManager

    // 缓存的播放器列表：uuid -> McediaPlayer
    private val players: ConcurrentHashMap<UUID, McediaPlayer> = ConcurrentHashMap()

    // SQLite 存储
    private var storage: McediaStorage? = null

    init {
        loadConfig()
        initStorage()
        loadPlayersFromDatabase()
    }

    /**
     * 初始化存储
     */
    private fun initStorage() {
        if (!enabled) return

        // 使用全局 DatabaseManager
        storage = SQLiteMcediaStorage(plugin)
    }

    /**
     * 从数据库加载所有播放器
     */
    private fun loadPlayersFromDatabase() {
        storage?.loadAll()?.thenAccept { loadedPlayers ->
            players.clear()
            loadedPlayers.forEach { player ->
                players[player.uuid] = player
            }
            plugin.logger.info("[Mcedia] 从数据库加载了 ${loadedPlayers.size} 个播放器")
        }
    }

    /**
     * 加载配置
     */
    fun loadConfig() {
        val config = plugin.config

        enabled = config.getBoolean("mcedia.enabled", true)
        playerNamePrefix = config.getString("mcedia.player-name-prefix", "mcedia") ?: "mcedia"
        defaultScale = config.getDouble("mcedia.default-scale", 1.0)
        defaultVolume = config.getDouble("mcedia.default-volume", 1.0)
        maxPlayers = config.getInt("mcedia.max-players", 50)

        val triggerItemStr = config.getString("mcedia.trigger-item", "") ?: ""
        triggerItem = if (triggerItemStr.isBlank() || triggerItemStr.equals("AIR", ignoreCase = true)) {
            null
        } else {
            try {
                Material.valueOf(triggerItemStr.uppercase())
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }

    /**
     * 创建播放器
     *
     * @param location 位置
     * @param name 播放器名称
     * @param createdBy 创建者 UUID
     * @return 创建的播放器，失败返回 null
     */
    fun createPlayer(location: Location, name: String, createdBy: UUID): McediaPlayer? {
        if (!enabled) return null
        if (players.size >= maxPlayers) return null

        val world = location.world ?: return null

        // 使用 Folia 兼容的方式在指定区域创建实体
        var createdPlayer: McediaPlayer? = null

        try {
            // 在主线程/区域线程中生成盔甲架
            Bukkit.getRegionScheduler().run(plugin, location) { _ ->
                val armorStand = world.spawnEntity(location, EntityType.ARMOR_STAND) as ArmorStand

                // 配置盔甲架
                armorStand.customName(Component.text("$playerNamePrefix:$name"))
                armorStand.isCustomNameVisible = true
                armorStand.setGravity(false)
                armorStand.isVisible = false  // 盔甲架不可见，只显示名称
                armorStand.isSmall = false
                armorStand.setArms(true)
                armorStand.isMarker = false
                armorStand.isInvulnerable = false  // 可被破坏

                // 创建播放器数据
                val player = McediaPlayer(
                    uuid = armorStand.uniqueId,
                    name = name,
                    location = location.clone(),
                    scale = defaultScale,
                    volume = defaultVolume,
                    createdBy = createdBy
                )

                players[armorStand.uniqueId] = player
                createdPlayer = player

                plugin.logger.info("[Mcedia] 创建播放器: $name at ${location.blockX}, ${location.blockY}, ${location.blockZ}")
            }
        } catch (e: Exception) {
            plugin.logger.warning("[Mcedia] 创建播放器失败: ${e.message}")
            return null
        }

        return createdPlayer
    }

    /**
     * 同步创建播放器（必须在正确的线程中调用）
     */
    fun createPlayerSync(location: Location, name: String, createdBy: UUID): McediaPlayer? {
        if (!enabled) return null
        if (players.size >= maxPlayers) return null

        val world = location.world ?: return null

        val armorStand = world.spawnEntity(location, EntityType.ARMOR_STAND) as ArmorStand

        // 配置盔甲架
        armorStand.customName(Component.text("$playerNamePrefix:$name"))
        armorStand.isCustomNameVisible = false  // 默认不显示名称
        armorStand.setGravity(false)
        armorStand.isVisible = false  // 盔甲架不可见
        armorStand.isSmall = false
        armorStand.setArms(false)  // 不显示手臂
        armorStand.isMarker = false
        armorStand.isInvulnerable = false  // 可被破坏
        // 清空手持物品
        armorStand.equipment.setItemInMainHand(null)
        armorStand.equipment.setItemInOffHand(null)

        // 创建播放器数据
        val player = McediaPlayer(
            uuid = armorStand.uniqueId,
            name = name,
            location = location.clone(),
            scale = defaultScale,
            volume = defaultVolume,
            createdBy = createdBy
        )

        players[armorStand.uniqueId] = player

        // 保存到数据库
        storage?.save(player)

        plugin.logger.info("[Mcedia] 创建播放器: $name at ${location.blockX}, ${location.blockY}, ${location.blockZ}")

        return player
    }

    /**
     * 删除播放器
     * 如果区块未加载，会添加待处理删除操作，等区块加载时执行
     */
    fun deletePlayer(uuid: UUID): Boolean {
        val player = players.remove(uuid) ?: return false

        // 从数据库删除播放器数据
        storage?.delete(uuid)

        // 查找并移除盔甲架
        val entity = Bukkit.getEntity(uuid)
        if (entity is ArmorStand) {
            // 实体存在，直接删除
            entity.scheduler.run(plugin, { _ ->
                entity.remove()
            }, null)
            plugin.logger.info("[Mcedia] 删除播放器: ${player.name}")
        } else {
            // 实体不存在（区块可能未加载），添加待处理删除操作
            val pendingOp = PendingOperation(
                uuid = uuid,
                operationType = PendingOperationType.DELETE,
                worldName = player.location.world?.name ?: "world",
                x = player.location.x,
                y = player.location.y,
                z = player.location.z
            )
            storage?.addPendingOperation(pendingOp)
            plugin.logger.info("[Mcedia] 播放器 ${player.name} 区块未加载，已添加待处理删除操作")
        }

        return true
    }

    /**
     * 设置播放器视频链接
     */
    fun setVideo(uuid: UUID, videoUrl: String, startTime: String = ""): Boolean {
        val player = players[uuid] ?: return false
        player.videoUrl = videoUrl
        player.startTime = startTime

        // 保存到数据库
        storage?.save(player)

        // 更新盔甲架上的书
        updateArmorStandBooks(uuid)
        return true
    }

    /**
     * 更新播放器配置
     */
    fun updatePlayerConfig(uuid: UUID, config: McediaPlayer.() -> Unit): Boolean {
        val player = players[uuid] ?: return false
        player.config()

        // 保存到数据库
        storage?.save(player)

        updateArmorStandBooks(uuid)
        return true
    }

    /**
     * 更新盔甲架上的书
     * 如果实体不存在（区块未加载），会添加待处理更新操作
     */
    private fun updateArmorStandBooks(uuid: UUID) {
        val player = players[uuid] ?: return
        val entity = Bukkit.getEntity(uuid) as? ArmorStand

        if (entity != null) {
            // 实体存在，直接更新
            entity.scheduler.run(plugin, { _ ->
                // 更新主手书（视频链接）
                if (player.videoUrl.isNotEmpty()) {
                    val mainHandBook = createBook(player.generateMainHandBookContent(), "Video")
                    entity.equipment.setItemInMainHand(mainHandBook)
                }

                // 更新副手书（配置）
                val offHandBook = createBook(player.generateOffHandBookContent(), "Config")
                entity.equipment.setItemInOffHand(offHandBook)
            }, null)
        } else {
            // 实体不存在（区块可能未加载），添加待处理更新操作
            val pendingOp = PendingOperation(
                uuid = uuid,
                operationType = PendingOperationType.UPDATE,
                worldName = player.location.world?.name ?: "world",
                x = player.location.x,
                y = player.location.y,
                z = player.location.z
            )
            storage?.addPendingOperation(pendingOp)
            plugin.logger.info("[Mcedia] 播放器 ${player.name} 区块未加载，已添加待处理更新操作")
        }
    }

    /**
     * 创建书与笔
     */
    private fun createBook(pages: List<String>, title: String): ItemStack {
        val book = ItemStack(Material.WRITABLE_BOOK)
        val meta = book.itemMeta as BookMeta

        pages.forEach { page ->
            meta.addPages(Component.text(page))
        }

        meta.displayName(Component.text(title + ":" + System.currentTimeMillis()))
        book.itemMeta = meta
        return book
    }

    /**
     * 处理待处理操作（区块加载时调用）
     */
    fun processPendingOperations(worldName: String, chunkX: Int, chunkZ: Int) {
        storage?.getPendingOperations(worldName, chunkX, chunkZ)?.thenAccept { operations ->
            if (operations.isEmpty()) return@thenAccept

            plugin.logger.info("[Mcedia] 区块 ($worldName, $chunkX, $chunkZ) 有 ${operations.size} 个待处理操作")

            operations.forEach { op ->
                when (op.operationType) {
                    PendingOperationType.DELETE -> {
                        // 查找并删除盔甲架
                        val entity = Bukkit.getEntity(op.uuid)
                        if (entity is ArmorStand) {
                            entity.scheduler.run(plugin, { _ ->
                                entity.remove()
                                plugin.logger.info("[Mcedia] 已执行延迟删除操作: ${op.uuid}")
                            }, null)
                        }
                        // 无论是否找到实体，都删除待处理记录
                        storage?.removePendingOperation(op.uuid)
                    }
                    PendingOperationType.UPDATE -> {
                        // 更新盔甲架配置
                        val mcediaPlayer = players[op.uuid]
                        if (mcediaPlayer != null) {
                            updateArmorStandBooks(op.uuid)
                            plugin.logger.info("[Mcedia] 已执行延迟更新操作: ${mcediaPlayer.name}")
                        }
                        storage?.removePendingOperation(op.uuid)
                    }
                }
            }
        }
    }

    /**
     * 获取播放器
     */
    fun getPlayer(uuid: UUID): McediaPlayer? = players[uuid]

    /**
     * 获取所有播放器
     */
    fun getAllPlayers(): List<McediaPlayer> = players.values.toList()

    /**
     * 根据名称查找播放器
     */
    fun findPlayerByName(name: String): McediaPlayer? {
        return players.values.find { it.name.equals(name, ignoreCase = true) }
    }

    /**
     * 添加播放器到缓存（用于扫描时发现的现有播放器）
     */
    fun addPlayer(player: McediaPlayer) {
        players[player.uuid] = player
        // 保存到数据库
        storage?.save(player)
    }

    /**
     * 扫描世界中的 mcedia 盔甲架并加入缓存
     * 使用 Folia 兼容的方式：为每个实体在其所属区域调度任务
     */
    fun scanExistingPlayers() {
        Bukkit.getWorlds().forEach { world ->
            world.entities.filterIsInstance<ArmorStand>().forEach { armorStand ->
                // Folia 兼容：在实体所属的区域线程中执行
                armorStand.scheduler.run(plugin, { _ ->
                    try {
                        val customName = armorStand.customName()?.let {
                            net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(it)
                        } ?: ""

                        if (customName.startsWith(playerNamePrefix)) {
                            val name = customName.removePrefix("$playerNamePrefix:").ifEmpty { customName }

                            if (!players.containsKey(armorStand.uniqueId)) {
                                val player = McediaPlayer(
                                    uuid = armorStand.uniqueId,
                                    name = name,
                                    location = armorStand.location,
                                    createdBy = UUID.randomUUID() // 未知创建者
                                )
                                players[armorStand.uniqueId] = player
                            }
                        }
                    } catch (e: Exception) {
                        // 忽略无法访问的实体
                    }
                }, null)
            }
        }

        // 延迟输出日志，等待异步任务完成
        Bukkit.getGlobalRegionScheduler().runDelayed(plugin, { _ ->
            plugin.logger.info("[Mcedia] 扫描完成，发现 ${players.size} 个播放器")
        }, 40L) // 2秒后输出
    }

    /**
     * 检查功能是否启用
     */
    fun isEnabled(): Boolean = enabled

    /**
     * 获取消息文本
     */
    fun getMessage(key: String, vararg replacements: Pair<String, String>): String {
        return msg.getModule("mcedia", key, *replacements)
    }

    /**
     * 获取播放器名称前缀
     */
    fun getPlayerNamePrefix(): String = playerNamePrefix

    /**
     * 获取最大播放器数量
     */
    fun getMaxPlayers(): Int = maxPlayers

    /**
     * 获取触发物品
     * @return 触发物品，null 表示空手
     */
    fun getTriggerItem(): Material? = triggerItem

    /**
     * 获取插件实例
     */
    fun getPlugin(): JavaPlugin = plugin

    // ==================== 模板管理 ====================

    /**
     * 获取玩家的所有模板
     */
    fun getTemplates(playerUUID: UUID): List<McediaTemplate> {
        return storage?.getTemplates(playerUUID)?.join() ?: emptyList()
    }

    /**
     * 保存当前播放器配置为模板
     * @return 成功返回模板ID，失败返回 null（如模板已满）
     */
    fun saveAsTemplate(playerUUID: UUID, mcediaPlayer: McediaPlayer): Int? {
        val nextId = storage?.getNextTemplateId(playerUUID)?.join() ?: return null

        val template = McediaTemplate(
            id = nextId,
            playerUUID = playerUUID,
            name = mcediaPlayer.name,
            scale = mcediaPlayer.scale,
            volume = mcediaPlayer.volume,
            maxVolumeRange = mcediaPlayer.maxVolumeRange,
            hearingRange = mcediaPlayer.hearingRange,
            offsetX = mcediaPlayer.offsetX,
            offsetY = mcediaPlayer.offsetY,
            offsetZ = mcediaPlayer.offsetZ,
            looping = mcediaPlayer.looping,
            noDanmaku = mcediaPlayer.noDanmaku
        )

        val success = storage?.saveTemplate(template)?.join() ?: false
        return if (success) nextId else null
    }

    /**
     * 应用模板到播放器（不更改视频链接）
     */
    fun applyTemplate(mcediaPlayerUUID: UUID, template: McediaTemplate): Boolean {
        val mcediaPlayer = players[mcediaPlayerUUID] ?: return false

        mcediaPlayer.scale = template.scale
        mcediaPlayer.volume = template.volume
        mcediaPlayer.maxVolumeRange = template.maxVolumeRange
        mcediaPlayer.hearingRange = template.hearingRange
        mcediaPlayer.offsetX = template.offsetX
        mcediaPlayer.offsetY = template.offsetY
        mcediaPlayer.offsetZ = template.offsetZ
        mcediaPlayer.looping = template.looping
        mcediaPlayer.noDanmaku = template.noDanmaku

        // 保存到数据库
        storage?.save(mcediaPlayer)

        // 更新盔甲架
        updateArmorStandBooks(mcediaPlayerUUID)

        return true
    }

    /**
     * 删除模板
     */
    fun deleteTemplate(playerUUID: UUID, templateId: Int): Boolean {
        return storage?.deleteTemplate(playerUUID, templateId)?.join() ?: false
    }

    /**
     * 关闭管理器
     */
    fun shutdown() {
        // 关闭存储连接
        storage?.close()
        storage = null

        players.clear()
        plugin.logger.info("[Mcedia] 管理器已关闭")
    }
}

