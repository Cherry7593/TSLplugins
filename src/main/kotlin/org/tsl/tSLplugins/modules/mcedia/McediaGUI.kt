package org.tsl.tSLplugins.modules.mcedia

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Mcedia GUI 管理器
 * 处理所有 GUI 界面
 *
 * 权限节点：
 * - tsl.mcedia.admin - 管理员权限（主菜单、列表、扫描、传送、返回按钮）
 * - tsl.mcedia.use - 普通玩家权限（编辑单个播放器）
 */
class McediaGUI(
    private val plugin: JavaPlugin,
    private val manager: McediaManager
) : Listener {

    companion object {
        // 权限节点
        const val PERM_ADMIN = "tsl.mcedia.admin"
        const val PERM_USE = "tsl.mcedia.use"
    }

    // GUI 状态缓存
    private val playerGUIState: ConcurrentHashMap<UUID, GUIState> = ConcurrentHashMap()

    // GUI 类型
    enum class GUIType {
        MAIN_MENU,          // 主菜单
        PLAYER_LIST,        // 播放器列表
        PLAYER_EDIT,        // 编辑播放器
        CREATE_PLAYER,      // 创建播放器
        VIDEO_SELECT,       // 视频设置
        AUDIO_CONFIG,       // 音频配置
        DISPLAY_CONFIG,     // 显示配置
        CONFIRM_DELETE      // 确认删除
    }

    // GUI 状态
    data class GUIState(
        val type: GUIType,
        val page: Int = 0,
        val editingPlayerUUID: UUID? = null,
        val tempData: MutableMap<String, Any> = mutableMapOf()
    )

    // GUI 标题常量
    private val GUI_TITLE_PREFIX = "§6§lMcedia "
    private val MAIN_MENU_TITLE = "${GUI_TITLE_PREFIX}§f主菜单"
    private val PLAYER_LIST_TITLE = "${GUI_TITLE_PREFIX}§f播放器列表"
    private val PLAYER_EDIT_TITLE = "${GUI_TITLE_PREFIX}§f编辑播放器"
    private val CREATE_PLAYER_TITLE = "${GUI_TITLE_PREFIX}§f创建播放器"
    private val VIDEO_SELECT_TITLE = "${GUI_TITLE_PREFIX}§f设置视频"
    private val AUDIO_CONFIG_TITLE = "${GUI_TITLE_PREFIX}§f音频配置"
    private val DISPLAY_CONFIG_TITLE = "${GUI_TITLE_PREFIX}§f显示配置"
    private val CONFIRM_DELETE_TITLE = "${GUI_TITLE_PREFIX}§c确认删除"

    /**
     * 检查玩家是否有管理员权限
     */
    private fun hasAdminPermission(player: Player): Boolean {
        return player.isOp || player.hasPermission(PERM_ADMIN)
    }

    /**
     * 检查玩家是否有使用权限
     */
    private fun hasUsePermission(player: Player): Boolean {
        return player.isOp || player.hasPermission(PERM_ADMIN) || player.hasPermission(PERM_USE)
    }

    /**
     * 打开主菜单（仅管理员可用）
     */
    fun openMainMenu(player: Player) {
        // 权限检查：只有管理员可以打开主菜单
        if (!hasAdminPermission(player)) {
            player.sendMessage("§6[Mcedia] §c你没有权限打开主菜单！")
            return
        }

        val holder = McediaInventoryHolder(GUIType.MAIN_MENU)
        val inv = Bukkit.createInventory(holder, 27, Component.text(MAIN_MENU_TITLE))

        // 创建播放器
        inv.setItem(11, createMenuItem(
            Material.ARMOR_STAND,
            "§a创建播放器",
            listOf("§7在当前位置创建一个", "§7新的视频播放器", "", "§e点击创建")
        ))

        // 播放器列表
        inv.setItem(13, createMenuItem(
            Material.BOOK,
            "§b播放器列表",
            listOf("§7查看和管理所有", "§7已创建的播放器", "", "§7当前数量: §f${manager.getAllPlayers().size}", "", "§e点击查看")
        ))

        // 扫描现有播放器
        inv.setItem(15, createMenuItem(
            Material.ENDER_EYE,
            "§e扫描播放器",
            listOf("§7扫描世界中已存在的", "§7mcedia 盔甲架", "", "§e点击扫描")
        ))

        // 填充边框
        fillBorder(inv, Material.GRAY_STAINED_GLASS_PANE)

        playerGUIState[player.uniqueId] = GUIState(GUIType.MAIN_MENU)
        player.openInventory(inv)
    }

    /**
     * 打开播放器列表（仅管理员可用）
     */
    fun openPlayerList(player: Player, page: Int = 0) {
        // 权限检查：只有管理员可以打开播放器列表
        if (!hasAdminPermission(player)) {
            player.sendMessage("§6[Mcedia] §c你没有权限查看播放器列表！")
            return
        }

        val holder = McediaInventoryHolder(GUIType.PLAYER_LIST, page = page)
        val inv = Bukkit.createInventory(holder, 54, Component.text(PLAYER_LIST_TITLE))

        val players = manager.getAllPlayers()
        val pageSize = 45
        val totalPages = (players.size + pageSize - 1) / pageSize
        val startIndex = page * pageSize
        val endIndex = minOf(startIndex + pageSize, players.size)

        // 显示播放器
        for (i in startIndex until endIndex) {
            val mcediaPlayer = players[i]
            val platform = VideoPlatform.detect(mcediaPlayer.videoUrl)

            val item = createMenuItem(
                Material.JUKEBOX,
                "§f${mcediaPlayer.name}",
                listOf(
                    "§7视频: §b${if (mcediaPlayer.videoUrl.isEmpty()) "未设置" else platform.displayName}",
                    "§7位置: §f${mcediaPlayer.location.blockX}, ${mcediaPlayer.location.blockY}, ${mcediaPlayer.location.blockZ}",
                    "§7循环: ${if (mcediaPlayer.looping) "§a是" else "§c否"}",
                    "",
                    "§e左键 §7- 编辑",
                    "§c右键 §7- 删除"
                )
            )
            inv.setItem(i - startIndex, item)
        }

        // 底部导航栏
        fillRow(inv, 5, Material.BLACK_STAINED_GLASS_PANE)

        // 上一页
        if (page > 0) {
            inv.setItem(45, createMenuItem(Material.ARROW, "§a上一页", listOf("§7第 ${page} 页")))
        }

        // 返回
        inv.setItem(49, createMenuItem(Material.BARRIER, "§c返回", listOf("§7返回主菜单")))

        // 下一页
        if (page < totalPages - 1) {
            inv.setItem(53, createMenuItem(Material.ARROW, "§a下一页", listOf("§7第 ${page + 2} 页")))
        }

        // 页码显示
        inv.setItem(50, createMenuItem(
            Material.PAPER,
            "§f第 ${page + 1}/$totalPages 页",
            listOf("§7共 ${players.size} 个播放器")
        ))

        playerGUIState[player.uniqueId] = GUIState(GUIType.PLAYER_LIST, page)
        player.openInventory(inv)
    }

    /**
     * 打开播放器编辑界面
     * 管理员：显示所有按钮
     * 普通玩家：隐藏传送按钮和返回按钮
     */
    fun openPlayerEdit(player: Player, mcediaPlayer: McediaPlayer) {
        val holder = McediaInventoryHolder(GUIType.PLAYER_EDIT, editingPlayerUUID = mcediaPlayer.uuid)
        val inv = Bukkit.createInventory(holder, 45, Component.text(PLAYER_EDIT_TITLE))

        val platform = VideoPlatform.detect(mcediaPlayer.videoUrl)
        val isAdmin = hasAdminPermission(player)

        // 播放器信息
        inv.setItem(4, createMenuItem(
            Material.ARMOR_STAND,
            "§f${mcediaPlayer.name}",
            listOf(
                "§7UUID: §8${mcediaPlayer.uuid.toString().take(8)}...",
                "§7创建时间: §f${formatTime(mcediaPlayer.createdAt)}"
            )
        ))

        // 设置视频 (Row 1)
        inv.setItem(10, createMenuItem(
            Material.MUSIC_DISC_CAT,
            "§b设置视频",
            listOf(
                "§7当前: ${if (mcediaPlayer.videoUrl.isEmpty()) "§c未设置" else "§a${platform.displayName}"}",
                if (mcediaPlayer.videoUrl.isNotEmpty()) "§8${mcediaPlayer.videoUrl.take(40)}..." else "",
                "",
                "§e点击设置视频链接"
            )
        ))

        // 显示配置 (Row 1)
        inv.setItem(12, createMenuItem(
            Material.SPYGLASS,
            "§e显示配置",
            listOf(
                "§7缩放: §f${mcediaPlayer.scale}",
                "§7偏移: §f${mcediaPlayer.offsetX}, ${mcediaPlayer.offsetY}, ${mcediaPlayer.offsetZ}",
                "",
                "§e点击配置显示参数"
            )
        ))

        // 音频配置 (Row 1)
        inv.setItem(14, createMenuItem(
            Material.NOTE_BLOCK,
            "§d音频配置",
            listOf(
                "§7音量: §f${mcediaPlayer.volume}",
                "§7最大范围: §f${mcediaPlayer.maxVolumeRange}",
                "§7可听范围: §f${mcediaPlayer.hearingRange}",
                "",
                "§e点击配置音频参数"
            )
        ))

        // 标签配置 (Row 1)
        inv.setItem(16, createMenuItem(
            Material.NAME_TAG,
            "§a标签配置",
            listOf(
                "§7循环播放: ${if (mcediaPlayer.looping) "§a开启" else "§c关闭"}",
                "§7关闭弹幕: ${if (mcediaPlayer.noDanmaku) "§a开启" else "§c关闭"}",
                "",
                "§e点击切换标签"
            )
        ))

        // 传送到播放器（仅管理员可见）(Row 2)
        if (isAdmin) {
            inv.setItem(19, createMenuItem(
                Material.ENDER_PEARL,
                "§b传送到播放器",
                listOf("§7传送到播放器位置")
            ))
        }

        // 保存为模板 (Row 2)
        inv.setItem(21, createMenuItem(
            Material.WRITABLE_BOOK,
            "§a保存为模板",
            listOf("§7将当前配置保存到模板栏位", "§7每个玩家最多保存 7 个模板")
        ))

        // 返回（仅管理员可见）(Row 2)
        if (isAdmin) {
            inv.setItem(23, createMenuItem(
                Material.BARRIER,
                "§c返回",
                listOf("§7返回播放器列表")
            ))
        }

        // 删除播放器 (Row 2)
        inv.setItem(25, createMenuItem(
            Material.TNT,
            "§c删除播放器",
            listOf("§7永久删除此播放器")
        ))

        // 模板栏位 (Row 3, slots 28-34，共7个)
        val templates = manager.getTemplates(player.uniqueId)
        val templateMap = templates.associateBy { it.id }

        for (i in 1..7) {
            val slot = 27 + i  // slots 28-34
            val template = templateMap[i]

            if (template != null) {
                inv.setItem(slot, createMenuItem(
                    Material.FILLED_MAP,
                    "§6模板: §f${template.name}",
                    listOf(
                        "§7缩放: §f${template.scale}",
                        "§7音量: §f${template.volume}",
                        "§7可听: §f${template.hearingRange}",
                        "§7偏移: §f${template.offsetX}, ${template.offsetY}, ${template.offsetZ}",
                        "§7循环: ${if (template.looping) "§a是" else "§c否"}",
                        "§7弹幕: ${if (template.noDanmaku) "§c关" else "§a开"}",
                        "",
                        "§e左键 §7- 应用模板",
                        "§c右键 §7- 删除模板"
                    )
                ))
            } else {
                inv.setItem(slot, createMenuItem(
                    Material.MAP,
                    "§8空模板栏位 #$i",
                    listOf("§7点击保存按钮保存当前配置")
                ))
            }
        }

        // 填充边框（Row 0 边框 + Row 4 全部玻璃板）
        for (slot in listOf(0, 1, 2, 3, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44)) {
            inv.setItem(slot, createMenuItem(Material.GRAY_STAINED_GLASS_PANE, " ", listOf()))
        }

        playerGUIState[player.uniqueId] = GUIState(GUIType.PLAYER_EDIT, editingPlayerUUID = mcediaPlayer.uuid)
        player.openInventory(inv)
    }

    /**
     * 打开视频设置界面
     */
    fun openVideoSelect(player: Player, mcediaPlayer: McediaPlayer) {
        val holder = McediaInventoryHolder(GUIType.VIDEO_SELECT, editingPlayerUUID = mcediaPlayer.uuid)
        val inv = Bukkit.createInventory(holder, 36, Component.text(VIDEO_SELECT_TITLE))

        // 平台图标
        inv.setItem(10, createMenuItem(Material.RED_CONCRETE, "§c哔哩哔哩", listOf("§7支持普通视频/直播/番剧")))
        inv.setItem(12, createMenuItem(Material.PINK_CONCRETE, "§d抖音", listOf("§7支持抖音分享链接")))
        inv.setItem(14, createMenuItem(Material.MAGENTA_CONCRETE, "§5樱花动漫", listOf("§7需要开启 yhdm 选项")))
        inv.setItem(16, createMenuItem(Material.CYAN_CONCRETE, "§b直链", listOf("§7直接播放 URL")))

        // 当前视频
        inv.setItem(22, createMenuItem(
            Material.PAPER,
            "§f当前视频",
            listOf(
                if (mcediaPlayer.videoUrl.isEmpty()) "§c未设置" else "§a${mcediaPlayer.videoUrl.take(50)}...",
                "",
                "§e在聊天框输入视频链接"
            )
        ))

        // 起始时间
        inv.setItem(31, createMenuItem(
            Material.CLOCK,
            "§e起始时间",
            listOf(
                "§7当前: §f${mcediaPlayer.startTime.ifEmpty { "从头开始" }}",
                "",
                "§7格式: 小时:分钟:秒",
                "§7示例: 0:30:00"
            )
        ))

        // 返回
        inv.setItem(27, createMenuItem(Material.ARROW, "§c返回", listOf()))

        fillBorder(inv, Material.GRAY_STAINED_GLASS_PANE)

        playerGUIState[player.uniqueId] = GUIState(
            GUIType.VIDEO_SELECT,
            editingPlayerUUID = mcediaPlayer.uuid
        )
        player.openInventory(inv)
    }

    /**
     * 打开音频配置界面
     */
    fun openAudioConfig(player: Player, mcediaPlayer: McediaPlayer) {
        val holder = McediaInventoryHolder(GUIType.AUDIO_CONFIG, editingPlayerUUID = mcediaPlayer.uuid)
        val inv = Bukkit.createInventory(holder, 36, Component.text(AUDIO_CONFIG_TITLE))

        // 音量
        inv.setItem(10, createMenuItem(Material.NOTE_BLOCK, "§f音量 -", listOf("§7当前: ${mcediaPlayer.volume}", "§c减少 0.1")))
        inv.setItem(11, createMenuItem(Material.NOTE_BLOCK, "§f音量: §b${mcediaPlayer.volume}", listOf()))
        inv.setItem(12, createMenuItem(Material.NOTE_BLOCK, "§f音量 +", listOf("§a增加 0.1")))

        // 最大音量范围
        inv.setItem(14, createMenuItem(Material.BELL, "§f范围 -", listOf("§7当前: ${mcediaPlayer.maxVolumeRange}", "§c减少 1")))
        inv.setItem(15, createMenuItem(Material.BELL, "§f范围: §b${mcediaPlayer.maxVolumeRange}", listOf()))
        inv.setItem(16, createMenuItem(Material.BELL, "§f范围 +", listOf("§a增加 1")))

        // 可听范围
        inv.setItem(21, createMenuItem(Material.SCULK_SENSOR, "§f可听 -", listOf("§7当前: ${mcediaPlayer.hearingRange}", "§c减少 50")))
        inv.setItem(22, createMenuItem(Material.SCULK_SENSOR, "§f可听: §b${mcediaPlayer.hearingRange}", listOf()))
        inv.setItem(23, createMenuItem(Material.SCULK_SENSOR, "§f可听 +", listOf("§a增加 50")))

        // 返回
        inv.setItem(31, createMenuItem(Material.ARROW, "§c返回", listOf()))

        fillBorder(inv, Material.GRAY_STAINED_GLASS_PANE)

        playerGUIState[player.uniqueId] = GUIState(
            GUIType.AUDIO_CONFIG,
            editingPlayerUUID = mcediaPlayer.uuid
        )
        player.openInventory(inv)
    }

    /**
     * 打开显示配置界面
     */
    fun openDisplayConfig(player: Player, mcediaPlayer: McediaPlayer) {
        val holder = McediaInventoryHolder(GUIType.DISPLAY_CONFIG, editingPlayerUUID = mcediaPlayer.uuid)
        val inv = Bukkit.createInventory(holder, 36, Component.text(DISPLAY_CONFIG_TITLE))

        // 缩放
        inv.setItem(10, createMenuItem(Material.RABBIT_HIDE, "§f缩放 -", listOf("§7当前: ${mcediaPlayer.scale}", "§c减少 0.1")))
        inv.setItem(11, createMenuItem(Material.LEATHER, "§f缩放: §b${mcediaPlayer.scale}", listOf()))
        inv.setItem(12, createMenuItem(Material.PHANTOM_MEMBRANE, "§f缩放 +", listOf("§a增加 0.1")))

        // X 偏移
        inv.setItem(19, createMenuItem(Material.RED_DYE, "§cX -", listOf("§7当前: ${mcediaPlayer.offsetX}", "§c减少 0.5")))
        inv.setItem(20, createMenuItem(Material.RED_WOOL, "§fX: §c${mcediaPlayer.offsetX}", listOf()))
        inv.setItem(21, createMenuItem(Material.RED_DYE, "§cX +", listOf("§a增加 0.5")))

        // Y 偏移
        inv.setItem(22, createMenuItem(Material.LIME_DYE, "§aY -", listOf("§7当前: ${mcediaPlayer.offsetY}", "§c减少 0.5")))
        inv.setItem(23, createMenuItem(Material.LIME_WOOL, "§fY: §a${mcediaPlayer.offsetY}", listOf()))
        inv.setItem(24, createMenuItem(Material.LIME_DYE, "§aY +", listOf("§a增加 0.5")))

        // Z 偏移
        inv.setItem(28, createMenuItem(Material.BLUE_DYE, "§9Z -", listOf("§7当前: ${mcediaPlayer.offsetZ}", "§c减少 0.5")))
        inv.setItem(29, createMenuItem(Material.BLUE_WOOL, "§fZ: §9${mcediaPlayer.offsetZ}", listOf()))
        inv.setItem(30, createMenuItem(Material.BLUE_DYE, "§9Z +", listOf("§a增加 0.5")))

        // 返回
        inv.setItem(31, createMenuItem(Material.ARROW, "§c返回", listOf()))

        fillBorder(inv, Material.GRAY_STAINED_GLASS_PANE)

        playerGUIState[player.uniqueId] = GUIState(
            GUIType.DISPLAY_CONFIG,
            editingPlayerUUID = mcediaPlayer.uuid
        )
        player.openInventory(inv)
    }

    /**
     * 打开确认删除界面
     */
    fun openConfirmDelete(player: Player, mcediaPlayer: McediaPlayer) {
        val holder = McediaInventoryHolder(GUIType.CONFIRM_DELETE, editingPlayerUUID = mcediaPlayer.uuid)
        val inv = Bukkit.createInventory(holder, 27, Component.text(CONFIRM_DELETE_TITLE))

        // 确认
        inv.setItem(11, createMenuItem(
            Material.LIME_CONCRETE,
            "§a确认删除",
            listOf("§7删除播放器: §f${mcediaPlayer.name}", "§c此操作不可撤销！")
        ))

        // 取消
        inv.setItem(15, createMenuItem(
            Material.RED_CONCRETE,
            "§c取消",
            listOf("§7返回编辑界面")
        ))

        fillBorder(inv, Material.RED_STAINED_GLASS_PANE)

        playerGUIState[player.uniqueId] = GUIState(
            GUIType.CONFIRM_DELETE,
            editingPlayerUUID = mcediaPlayer.uuid
        )
        player.openInventory(inv)
    }

    /**
     * 处理 GUI 点击
     */
    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST, ignoreCancelled = false)
    fun onInventoryClick(event: InventoryClickEvent) {
        // 使用 InventoryHolder 来识别是否是 Mcedia GUI
        val holder = event.inventory.holder
        if (holder !is McediaInventoryHolder) return

        // 取消事件，防止物品被拿走
        event.isCancelled = true

        val player = event.whoClicked as? Player ?: return

        // 确保点击的是 GUI 内的槽位，而不是玩家背包
        val slot = event.rawSlot
        if (slot < 0 || slot >= event.inventory.size) return

        // 点击的物品
        val clickedItem = event.currentItem

        // 如果点击的是填充物（玻璃板），忽略
        if (clickedItem != null && clickedItem.type.name.contains("GLASS_PANE")) return

        // 如果是空槽位，忽略
        if (clickedItem == null || clickedItem.type.isAir) return

        // 直接从 holder 获取状态信息，不依赖 playerGUIState
        when (holder.guiType) {
            GUIType.MAIN_MENU -> handleMainMenuClick(player, slot)
            GUIType.PLAYER_LIST -> handlePlayerListClick(player, slot, holder.page)
            GUIType.PLAYER_EDIT -> handlePlayerEditClick(player, slot, holder.editingPlayerUUID, event.isRightClick)
            GUIType.VIDEO_SELECT -> handleVideoSelectClick(player, slot, holder.editingPlayerUUID)
            GUIType.AUDIO_CONFIG -> handleAudioConfigClick(player, slot, holder.editingPlayerUUID)
            GUIType.DISPLAY_CONFIG -> handleDisplayConfigClick(player, slot, holder.editingPlayerUUID)
            GUIType.CONFIRM_DELETE -> handleConfirmDeleteClick(player, slot, holder.editingPlayerUUID)
            else -> {}
        }
    }

    private fun handleMainMenuClick(player: Player, slot: Int) {
        when (slot) {
            11 -> {
                // 创建播放器
                player.closeInventory()
                player.sendMessage("§6[Mcedia] §7请输入播放器名称（在聊天框输入）：")
                playerGUIState[player.uniqueId] = GUIState(GUIType.CREATE_PLAYER)
            }
            13 -> openPlayerList(player)
            15 -> {
                // 扫描播放器
                manager.scanExistingPlayers()
                player.sendMessage("§6[Mcedia] §a扫描完成！")
                openMainMenu(player)
            }
        }
    }

    private fun handlePlayerListClick(player: Player, slot: Int, page: Int) {
        val players = manager.getAllPlayers()
        val pageSize = 45
        val startIndex = page * pageSize

        when {
            slot < pageSize -> {
                // 点击播放器槽位
                val playerIndex = startIndex + slot
                if (playerIndex < players.size) {
                    val mcediaPlayer = players[playerIndex]
                    openPlayerEdit(player, mcediaPlayer)
                }
            }
            slot == 45 && page > 0 -> openPlayerList(player, page - 1)
            slot == 49 -> openMainMenu(player)
            slot == 53 -> {
                val totalPages = (players.size + pageSize - 1) / pageSize
                if (page < totalPages - 1) {
                    openPlayerList(player, page + 1)
                }
            }
        }
    }

    private fun handlePlayerEditClick(player: Player, slot: Int, editingPlayerUUID: UUID?, isRightClick: Boolean) {
        val mcediaPlayer = editingPlayerUUID?.let { manager.getPlayer(it) } ?: return

        when (slot) {
            10 -> openVideoSelect(player, mcediaPlayer)
            12 -> openDisplayConfig(player, mcediaPlayer)
            14 -> openAudioConfig(player, mcediaPlayer)
            16 -> {
                // 切换标签
                manager.updatePlayerConfig(mcediaPlayer.uuid) {
                    if (isRightClick) {
                        noDanmaku = !noDanmaku
                    } else {
                        looping = !looping
                    }
                }
                openPlayerEdit(player, manager.getPlayer(mcediaPlayer.uuid)!!)
            }
            19 -> {
                // 传送 - 使用 teleportAsync 兼容 Folia（仅管理员）
                if (!hasAdminPermission(player)) return
                player.closeInventory()
                player.teleportAsync(mcediaPlayer.location).thenAccept { success ->
                    if (success) {
                        player.sendMessage("§6[Mcedia] §a已传送到播放器位置")
                    } else {
                        player.sendMessage("§6[Mcedia] §c传送失败！")
                    }
                }
            }
            21 -> {
                // 保存为模板
                val templateId = manager.saveAsTemplate(player.uniqueId, mcediaPlayer)
                if (templateId != null) {
                    player.sendMessage("§6[Mcedia] §a已保存为模板 #$templateId")
                    openPlayerEdit(player, mcediaPlayer)
                } else {
                    player.sendMessage("§6[Mcedia] §c模板栏位已满！请先删除一个模板")
                }
            }
            23 -> openPlayerList(player)  // 返回（仅管理员可见）
            25 -> openConfirmDelete(player, mcediaPlayer)
            // 模板栏位 28-34
            in 28..34 -> {
                val templateId = slot - 27  // 1-7
                val templates = manager.getTemplates(player.uniqueId)
                val template = templates.find { it.id == templateId }

                if (template != null) {
                    if (isRightClick) {
                        // 右键删除模板
                        manager.deleteTemplate(player.uniqueId, templateId)
                        player.sendMessage("§6[Mcedia] §c已删除模板 #$templateId")
                        openPlayerEdit(player, mcediaPlayer)
                    } else {
                        // 左键应用模板
                        manager.applyTemplate(mcediaPlayer.uuid, template)
                        player.sendMessage("§6[Mcedia] §a已应用模板: §f${template.name}")
                        openPlayerEdit(player, manager.getPlayer(mcediaPlayer.uuid)!!)
                    }
                }
            }
        }
    }

    private fun handleVideoSelectClick(player: Player, slot: Int, editingPlayerUUID: UUID?) {
        when (slot) {
            27 -> {
                val mcediaPlayer = editingPlayerUUID?.let { manager.getPlayer(it) }
                if (mcediaPlayer != null) {
                    openPlayerEdit(player, mcediaPlayer)
                }
            }
            // 平台图标点击 - 提示输入对应平台链接
            10 -> {
                // 哔哩哔哩
                player.closeInventory()
                player.sendMessage("§6[Mcedia] §c哔哩哔哩 §7- 请在聊天框输入视频链接：")
                player.sendMessage("§7支持格式: §fhttps://www.bilibili.com/video/... §7或 §fhttps://b23.tv/...")
                playerGUIState[player.uniqueId] = GUIState(
                    GUIType.VIDEO_SELECT,
                    editingPlayerUUID = editingPlayerUUID,
                    tempData = mutableMapOf("awaiting_input" to "video_url")
                )
            }
            12 -> {
                // 抖音
                player.closeInventory()
                player.sendMessage("§6[Mcedia] §d抖音 §7- 请在聊天框粘贴抖音分享文本：")
                playerGUIState[player.uniqueId] = GUIState(
                    GUIType.VIDEO_SELECT,
                    editingPlayerUUID = editingPlayerUUID,
                    tempData = mutableMapOf("awaiting_input" to "video_url")
                )
            }
            14 -> {
                // 樱花动漫
                player.closeInventory()
                player.sendMessage("§6[Mcedia] §5樱花动漫 §7- 请在聊天框输入视频链接：")
                player.sendMessage("§7格式: §fhttps://yhdm.one/vod-play/...")
                playerGUIState[player.uniqueId] = GUIState(
                    GUIType.VIDEO_SELECT,
                    editingPlayerUUID = editingPlayerUUID,
                    tempData = mutableMapOf("awaiting_input" to "video_url")
                )
            }
            16 -> {
                // 直链
                player.closeInventory()
                player.sendMessage("§6[Mcedia] §b直链 §7- 请在聊天框输入视频直链：")
                playerGUIState[player.uniqueId] = GUIState(
                    GUIType.VIDEO_SELECT,
                    editingPlayerUUID = editingPlayerUUID,
                    tempData = mutableMapOf("awaiting_input" to "video_url")
                )
            }
            22 -> {
                // 输入视频链接（当前视频按钮）
                player.closeInventory()
                player.sendMessage("§6[Mcedia] §7请在聊天框输入视频链接：")
                playerGUIState[player.uniqueId] = GUIState(
                    GUIType.VIDEO_SELECT,
                    editingPlayerUUID = editingPlayerUUID,
                    tempData = mutableMapOf("awaiting_input" to "video_url")
                )
            }
            31 -> {
                // 输入起始时间
                player.closeInventory()
                player.sendMessage("§6[Mcedia] §7请输入起始时间（格式 h:mm:ss，输入 0 从头开始）：")
                playerGUIState[player.uniqueId] = GUIState(
                    GUIType.VIDEO_SELECT,
                    editingPlayerUUID = editingPlayerUUID,
                    tempData = mutableMapOf("awaiting_input" to "start_time")
                )
            }
        }
    }

    private fun handleAudioConfigClick(player: Player, slot: Int, editingPlayerUUID: UUID?) {
        val mcediaPlayer = editingPlayerUUID?.let { manager.getPlayer(it) } ?: return

        when (slot) {
            10 -> manager.updatePlayerConfig(mcediaPlayer.uuid) { volume = maxOf(0.0, volume - 0.1) }
            12 -> manager.updatePlayerConfig(mcediaPlayer.uuid) { volume = minOf(2.0, volume + 0.1) }
            14 -> manager.updatePlayerConfig(mcediaPlayer.uuid) { maxVolumeRange = maxOf(1.0, maxVolumeRange - 1) }
            16 -> manager.updatePlayerConfig(mcediaPlayer.uuid) { maxVolumeRange += 1 }
            21 -> manager.updatePlayerConfig(mcediaPlayer.uuid) { hearingRange = maxOf(50.0, hearingRange - 50) }
            23 -> manager.updatePlayerConfig(mcediaPlayer.uuid) { hearingRange += 50 }
            31 -> {
                openPlayerEdit(player, manager.getPlayer(mcediaPlayer.uuid)!!)
                return
            }
        }
        openAudioConfig(player, manager.getPlayer(mcediaPlayer.uuid)!!)
    }

    private fun handleDisplayConfigClick(player: Player, slot: Int, editingPlayerUUID: UUID?) {
        val mcediaPlayer = editingPlayerUUID?.let { manager.getPlayer(it) } ?: return

        when (slot) {
            10 -> manager.updatePlayerConfig(mcediaPlayer.uuid) { scale = maxOf(0.1, scale - 0.1) }
            12 -> manager.updatePlayerConfig(mcediaPlayer.uuid) { scale += 0.1 }
            19 -> manager.updatePlayerConfig(mcediaPlayer.uuid) { offsetX -= 0.5 }
            21 -> manager.updatePlayerConfig(mcediaPlayer.uuid) { offsetX += 0.5 }
            22 -> manager.updatePlayerConfig(mcediaPlayer.uuid) { offsetY -= 0.5 }
            24 -> manager.updatePlayerConfig(mcediaPlayer.uuid) { offsetY += 0.5 }
            28 -> manager.updatePlayerConfig(mcediaPlayer.uuid) { offsetZ -= 0.5 }
            30 -> manager.updatePlayerConfig(mcediaPlayer.uuid) { offsetZ += 0.5 }
            31 -> {
                openPlayerEdit(player, manager.getPlayer(mcediaPlayer.uuid)!!)
                return
            }
        }
        openDisplayConfig(player, manager.getPlayer(mcediaPlayer.uuid)!!)
    }

    private fun handleConfirmDeleteClick(player: Player, slot: Int, editingPlayerUUID: UUID?) {
        when (slot) {
            11 -> {
                // 确认删除
                editingPlayerUUID?.let { uuid ->
                    manager.deletePlayer(uuid)
                    player.sendMessage("§6[Mcedia] §c播放器已删除")
                }
                openPlayerList(player)
            }
            15 -> {
                // 取消
                val mcediaPlayer = editingPlayerUUID?.let { manager.getPlayer(it) }
                if (mcediaPlayer != null) {
                    openPlayerEdit(player, mcediaPlayer)
                } else {
                    openPlayerList(player)
                }
            }
        }
    }

    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        val player = event.player as? Player ?: return
        val state = playerGUIState[player.uniqueId] ?: return

        // 如果不是等待输入状态，移除状态
        if (state.tempData["awaiting_input"] == null && state.type != GUIType.CREATE_PLAYER) {
            playerGUIState.remove(player.uniqueId)
        }
    }

    /**
     * 处理聊天输入
     */
    fun handleChatInput(player: Player, message: String): Boolean {
        val state = playerGUIState[player.uniqueId] ?: return false

        when (state.type) {
            GUIType.CREATE_PLAYER -> {
                // 创建播放器
                val name = message.trim()
                if (name.isEmpty()) {
                    player.sendMessage("§6[Mcedia] §c名称不能为空！")
                    return true
                }

                // 在玩家位置创建
                player.scheduler.run(plugin, { _ ->
                    val mcediaPlayer = manager.createPlayerSync(player.location, name, player.uniqueId)
                    if (mcediaPlayer != null) {
                        player.sendMessage("§6[Mcedia] §a播放器创建成功！")
                        openPlayerEdit(player, mcediaPlayer)
                    } else {
                        player.sendMessage("§6[Mcedia] §c创建失败！")
                    }
                }, null)

                playerGUIState.remove(player.uniqueId)
                return true
            }
            GUIType.VIDEO_SELECT -> {
                val awaiting = state.tempData["awaiting_input"] as? String
                val uuid = state.editingPlayerUUID ?: return false
                val inputMessage = message.trim()

                // 必须在主线程/区域线程中执行，因为 setVideo 和 updatePlayerConfig 会调用 Bukkit.getEntity()
                player.scheduler.run(plugin, { _ ->
                    when (awaiting) {
                        "video_url" -> {
                            manager.setVideo(uuid, inputMessage)
                            player.sendMessage("§6[Mcedia] §a视频链接已设置！")
                        }
                        "start_time" -> {
                            val time = if (inputMessage == "0") "" else inputMessage
                            manager.updatePlayerConfig(uuid) { startTime = time }
                            player.sendMessage("§6[Mcedia] §a起始时间已设置！")
                        }
                    }

                    val mcediaPlayer = manager.getPlayer(uuid)
                    if (mcediaPlayer != null) {
                        openVideoSelect(player, mcediaPlayer)
                    }
                }, null)

                playerGUIState.remove(player.uniqueId)
                return true
            }
            else -> return false
        }
    }

    /**
     * 获取玩家 GUI 状态
     */
    fun getPlayerState(player: Player): GUIState? = playerGUIState[player.uniqueId]

    // ========== 工具方法 ==========

    private fun createMenuItem(material: Material, name: String, lore: List<String>): ItemStack {
        val item = ItemStack(material)
        val meta = item.itemMeta
        meta.displayName(Component.text(name).decoration(TextDecoration.ITALIC, false))
        meta.lore(lore.map { Component.text(it).decoration(TextDecoration.ITALIC, false) })
        item.itemMeta = meta
        return item
    }

    private fun fillBorder(inv: Inventory, material: Material) {
        val filler = ItemStack(material)
        val meta = filler.itemMeta
        meta.displayName(Component.text(" "))
        filler.itemMeta = meta

        val size = inv.size
        val rows = size / 9

        for (i in 0 until 9) {
            if (inv.getItem(i) == null) inv.setItem(i, filler)
            if (inv.getItem(size - 9 + i) == null) inv.setItem(size - 9 + i, filler)
        }
        for (i in 0 until rows) {
            if (inv.getItem(i * 9) == null) inv.setItem(i * 9, filler)
            if (inv.getItem(i * 9 + 8) == null) inv.setItem(i * 9 + 8, filler)
        }
    }

    private fun fillRow(inv: Inventory, row: Int, material: Material) {
        val filler = ItemStack(material)
        val meta = filler.itemMeta
        meta.displayName(Component.text(" "))
        filler.itemMeta = meta

        for (i in 0 until 9) {
            val slot = row * 9 + i
            if (inv.getItem(slot) == null) inv.setItem(slot, filler)
        }
    }

    private fun formatTime(timestamp: Long): String {
        val date = java.util.Date(timestamp)
        val format = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm")
        return format.format(date)
    }
}

