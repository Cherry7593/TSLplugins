package org.tsl.tSLplugins.TownPHome

import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 小镇PHome GUI 菜单
 * 
 * 权限逻辑：
 * - 普通玩家（未加入小镇）：提示加入小镇
 * - 有小镇的玩家：查看本小镇 PHome，左键传送，管理职位可右键删除
 * - OP：可查看所有小镇的 PHome
 */
class TownPHomeGUI(
    private val plugin: JavaPlugin,
    private val manager: TownPHomeManager
) : Listener {

    private val serializer = LegacyComponentSerializer.legacyAmpersand()

    private val ITEMS_PER_PAGE = 45
    private val GUI_SIZE = 54

    // 等待聊天输入的玩家（用于创建 PHome）
    // 存储 townName 和 isOpMode
    private data class AwaitingInputData(val townName: String, val isOpMode: Boolean)
    private val awaitingInput: MutableMap<UUID, AwaitingInputData> = ConcurrentHashMap()

    enum class MenuType {
        TOWN_SELECTOR,  // OP 小镇选择界面
        PHOME_LIST      // PHome 列表界面
    }

    /**
     * 自定义 InventoryHolder，存储 GUI 状态信息
     */
    private inner class TownPHomeInventoryHolder(
        val menuType: MenuType,
        val townName: String? = null,
        val page: Int = 0,
        val isOpMode: Boolean = false  // 是否是 OP 模式查看
    ) : InventoryHolder {
        override fun getInventory(): Inventory {
            throw UnsupportedOperationException("This is a virtual holder")
        }
    }

    /**
     * 打开 GUI（入口方法）
     * 根据玩家权限决定显示内容
     */
    fun open(player: Player) {
        // OP 可以查看所有小镇
        if (player.isOp) {
            openTownSelector(player)
            return
        }

        // 普通玩家必须有小镇
        val townName = manager.getPlayerTownName(player)
        if (townName == null) {
            player.sendMessage(serializer.deserialize(manager.getMessage("not_in_town")))
            return
        }

        openPHomeList(player, townName, 0, false)
    }

    /**
     * OP 小镇选择界面
     */
    private fun openTownSelector(player: Player, page: Int = 0) {
        val towns = manager.storage.getAllTownNames()

        val totalPages = maxOf(1, (towns.size + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE)
        val currentPage = page.coerceIn(0, totalPages - 1)

        val holder = TownPHomeInventoryHolder(MenuType.TOWN_SELECTOR, page = currentPage, isOpMode = true)
        val title = serializer.deserialize("&6&l选择小镇 &7(${towns.size}个) &f- ${currentPage + 1}/$totalPages")
        val inventory = Bukkit.createInventory(holder, GUI_SIZE, title)

        // 填充小镇物品
        val startIndex = currentPage * ITEMS_PER_PAGE
        val endIndex = minOf(startIndex + ITEMS_PER_PAGE, towns.size)

        for (i in startIndex until endIndex) {
            val town = towns[i]
            val slot = i - startIndex
            inventory.setItem(slot, createTownItem(town))
        }

        // 底部导航栏
        fillTownSelectorNavBar(inventory, currentPage, totalPages)

        player.openInventory(inventory)
    }

    /**
     * 创建小镇选择物品
     */
    private fun createTownItem(townName: String): ItemStack {
        val homeCount = manager.getHomes(townName).size
        val item = ItemStack(Material.BEACON)
        val meta = item.itemMeta

        meta.displayName(serializer.deserialize("&e$townName"))
        meta.lore(listOf(
            serializer.deserialize("&7PHome 数量: &f$homeCount"),
            serializer.deserialize(""),
            serializer.deserialize("&e点击查看")
        ))
        item.itemMeta = meta

        return item
    }

    /**
     * 填充小镇选择界面导航栏
     */
    private fun fillTownSelectorNavBar(inventory: Inventory, currentPage: Int, totalPages: Int) {
        val filler = ItemStack(Material.GRAY_STAINED_GLASS_PANE)
        val fillerMeta = filler.itemMeta
        fillerMeta.displayName(serializer.deserialize(" "))
        filler.itemMeta = fillerMeta

        for (i in 45 until 54) {
            inventory.setItem(i, filler)
        }

        // 上一页
        if (currentPage > 0) {
            val prevItem = ItemStack(Material.ARROW)
            val prevMeta = prevItem.itemMeta
            prevMeta.displayName(serializer.deserialize("&e上一页"))
            prevItem.itemMeta = prevMeta
            inventory.setItem(45, prevItem)
        }

        // 关闭按钮
        val closeItem = ItemStack(Material.BARRIER)
        val closeMeta = closeItem.itemMeta
        closeMeta.displayName(serializer.deserialize("&c关闭"))
        closeItem.itemMeta = closeMeta
        inventory.setItem(49, closeItem)

        // 下一页
        if (currentPage < totalPages - 1) {
            val nextItem = ItemStack(Material.ARROW)
            val nextMeta = nextItem.itemMeta
            nextMeta.displayName(serializer.deserialize("&e下一页"))
            nextItem.itemMeta = nextMeta
            inventory.setItem(53, nextItem)
        }
    }

    /**
     * 打开 PHome 列表 GUI
     * @param isOpMode OP 模式查看其他小镇时为 true
     */
    fun openPHomeList(player: Player, townName: String, page: Int = 0, isOpMode: Boolean = false) {
        val homes = manager.getHomes(townName)
        val homeList = homes.values.toList()
        
        // 获取小镇等级（OP 模式下使用默认值）
        val townLevel = if (isOpMode) 1 else manager.getPlayerTownLevel(player)
        val limit = manager.getLimit(townLevel)

        val totalPages = maxOf(1, (homeList.size + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE)
        val currentPage = page.coerceIn(0, totalPages - 1)

        val holder = TownPHomeInventoryHolder(MenuType.PHOME_LIST, townName, currentPage, isOpMode)
        val title = serializer.deserialize("&6$townName PHome &7(${homeList.size}/$limit) &f- ${currentPage + 1}/$totalPages")
        val inventory = Bukkit.createInventory(holder, GUI_SIZE, title)

        // 填充 PHome 物品
        val startIndex = currentPage * ITEMS_PER_PAGE
        val endIndex = minOf(startIndex + ITEMS_PER_PAGE, homeList.size)

        // 判断是否有管理权限
        val canManage = isOpMode || manager.canManage(player)

        for (i in startIndex until endIndex) {
            val home = homeList[i]
            val slot = i - startIndex
            inventory.setItem(slot, createHomeItem(home, canManage))
        }

        // 底部导航栏（OP 模式下不受数量限制）
        val canCreate = isOpMode || homeList.size < limit
        fillPHomeListNavBar(inventory, currentPage, totalPages, canManage, homeList.size, limit, isOpMode, canCreate)

        player.openInventory(inventory)
    }

    /**
     * 为兼容旧调用保留的方法
     */
    fun openMainMenu(player: Player, townName: String, page: Int = 0) {
        openPHomeList(player, townName, page, false)
    }

    /**
     * 创建 PHome 物品
     */
    private fun createHomeItem(home: TownPHomeLocation, canManage: Boolean): ItemStack {
        val item = ItemStack(Material.ENDER_PEARL)
        val meta = item.itemMeta

        meta.displayName(serializer.deserialize("&a${home.name}"))

        val lore = mutableListOf<Component>()
        lore.add(serializer.deserialize("&7世界: &f${home.world}"))
        lore.add(serializer.deserialize("&7坐标: &f${home.x.toInt()}, ${home.y.toInt()}, ${home.z.toInt()}"))
        lore.add(serializer.deserialize(""))
        lore.add(serializer.deserialize("&e左键传送"))

        if (canManage) {
            lore.add(serializer.deserialize("&c右键删除"))
        }

        meta.lore(lore)
        item.itemMeta = meta

        return item
    }

    /**
     * 填充 PHome 列表导航栏
     */
    private fun fillPHomeListNavBar(
        inventory: Inventory,
        currentPage: Int,
        totalPages: Int,
        canManage: Boolean,
        currentCount: Int,
        limit: Int,
        isOpMode: Boolean,
        canCreate: Boolean
    ) {
        val filler = ItemStack(Material.GRAY_STAINED_GLASS_PANE)
        val fillerMeta = filler.itemMeta
        fillerMeta.displayName(serializer.deserialize(" "))
        filler.itemMeta = fillerMeta

        for (i in 45 until 54) {
            inventory.setItem(i, filler)
        }

        // 上一页
        if (currentPage > 0) {
            val prevItem = ItemStack(Material.ARROW)
            val prevMeta = prevItem.itemMeta
            prevMeta.displayName(serializer.deserialize("&e上一页"))
            prevItem.itemMeta = prevMeta
            inventory.setItem(45, prevItem)
        }

        // 创建按钮（仅管理角色可见）
        if (canManage) {
            val createItem = ItemStack(if (canCreate) Material.LIME_DYE else Material.GRAY_DYE)
            val createMeta = createItem.itemMeta
            createMeta.displayName(serializer.deserialize(if (canCreate) "&a创建 PHome" else "&c已达上限"))
            val loreList = mutableListOf(
                serializer.deserialize("&7在当前位置创建 PHome"),
                serializer.deserialize("&7当前: &f$currentCount/$limit")
            )
            if (isOpMode) {
                loreList.add(serializer.deserialize("&6OP 模式: 无限制"))
            }
            createMeta.lore(loreList)
            createItem.itemMeta = createMeta
            inventory.setItem(47, createItem)
        }

        // 返回/关闭按钮
        if (isOpMode) {
            // OP 模式显示返回按钮
            val backItem = ItemStack(Material.ARROW)
            val backMeta = backItem.itemMeta
            backMeta.displayName(serializer.deserialize("&c返回小镇列表"))
            backItem.itemMeta = backMeta
            inventory.setItem(49, backItem)
        } else {
            // 普通模式显示关闭按钮
            val closeItem = ItemStack(Material.BARRIER)
            val closeMeta = closeItem.itemMeta
            closeMeta.displayName(serializer.deserialize("&c关闭"))
            closeItem.itemMeta = closeMeta
            inventory.setItem(49, closeItem)
        }

        // 下一页
        if (currentPage < totalPages - 1) {
            val nextItem = ItemStack(Material.ARROW)
            val nextMeta = nextItem.itemMeta
            nextMeta.displayName(serializer.deserialize("&e下一页"))
            nextItem.itemMeta = nextMeta
            inventory.setItem(53, nextItem)
        }
    }

    /**
     * 处理物品栏点击
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return

        val holder = event.inventory.holder as? TownPHomeInventoryHolder ?: return

        event.isCancelled = true

        if (event.click.isShiftClick || event.click.isKeyboardClick) return

        val slot = event.rawSlot
        if (slot < 0) return

        if (event.clickedInventory != event.view.topInventory) return

        when (holder.menuType) {
            MenuType.TOWN_SELECTOR -> handleTownSelectorClick(player, slot, holder.page)
            MenuType.PHOME_LIST -> handlePHomeListClick(player, slot, holder, event.isRightClick)
        }
    }

    /**
     * 处理小镇选择界面点击
     */
    private fun handleTownSelectorClick(player: Player, slot: Int, currentPage: Int) {
        val towns = manager.storage.getAllTownNames()
        val totalPages = maxOf(1, (towns.size + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE)

        when (slot) {
            45 -> {
                if (currentPage > 0) {
                    openTownSelector(player, currentPage - 1)
                }
            }
            49 -> player.closeInventory()
            53 -> {
                if (currentPage < totalPages - 1) {
                    openTownSelector(player, currentPage + 1)
                }
            }
            in 0 until ITEMS_PER_PAGE -> {
                val index = currentPage * ITEMS_PER_PAGE + slot
                if (index < towns.size) {
                    val townName = towns[index]
                    openPHomeList(player, townName, 0, true)
                }
            }
        }
    }

    /**
     * 处理 PHome 列表界面点击
     */
    private fun handlePHomeListClick(player: Player, slot: Int, holder: TownPHomeInventoryHolder, isRightClick: Boolean) {
        val townName = holder.townName ?: return
        val currentPage = holder.page
        val isOpMode = holder.isOpMode
        val homes = manager.getHomes(townName).values.toList()
        
        val townLevel = if (isOpMode) 1 else manager.getPlayerTownLevel(player)
        val limit = manager.getLimit(townLevel)
        val totalPages = maxOf(1, (homes.size + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE)
        val canManage = isOpMode || manager.canManage(player)
        // OP 模式下不受数量限制
        val canCreate = isOpMode || homes.size < limit

        when (slot) {
            45 -> {
                if (currentPage > 0) {
                    openPHomeList(player, townName, currentPage - 1, isOpMode)
                }
            }
            47 -> {
                // 创建 PHome
                if (canManage && canCreate) {
                    player.closeInventory()
                    player.sendMessage(serializer.deserialize(manager.getMessage("gui_enter_name")))
                    awaitingInput[player.uniqueId] = AwaitingInputData(townName, isOpMode)
                }
            }
            49 -> {
                if (isOpMode) {
                    // 返回小镇列表
                    openTownSelector(player)
                } else {
                    player.closeInventory()
                }
            }
            53 -> {
                if (currentPage < totalPages - 1) {
                    openPHomeList(player, townName, currentPage + 1, isOpMode)
                }
            }
            in 0 until ITEMS_PER_PAGE -> {
                val index = currentPage * ITEMS_PER_PAGE + slot
                if (index < homes.size) {
                    val home = homes[index]
                    if (isRightClick && canManage) {
                        // 右键删除
                        if (manager.removeHome(townName, home.name)) {
                            player.sendMessage(serializer.deserialize(
                                manager.getMessage("del_success", "name" to home.name)
                            ))
                            openPHomeList(player, townName, currentPage, isOpMode)
                        }
                    } else {
                        // 左键传送
                        player.closeInventory()
                        val result = manager.teleportToHome(player, townName, home.name)
                        when (result) {
                            TeleportResult.SUCCESS -> {
                                player.sendMessage(serializer.deserialize(
                                    manager.getMessage("teleport_success", "name" to home.name)
                                ))
                            }
                            TeleportResult.NOT_FOUND -> {
                                player.sendMessage(serializer.deserialize(
                                    manager.getMessage("not_found", "name" to home.name)
                                ))
                            }
                            TeleportResult.WORLD_NOT_FOUND -> {
                                player.sendMessage(serializer.deserialize(
                                    manager.getMessage("world_not_found")
                                ))
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 监听聊天消息用于创建 PHome
     */
    @EventHandler(priority = EventPriority.LOWEST)
    fun onAsyncChat(event: AsyncChatEvent) {
        val player = event.player
        if (!awaitingInput.containsKey(player.uniqueId)) return

        val message = PlainTextComponentSerializer.plainText().serialize(event.message())
        event.isCancelled = true
        handleChatInput(player, message)
    }

    /**
     * 处理聊天输入（创建 PHome）
     */
    private fun handleChatInput(player: Player, message: String): Boolean {
        val inputData = awaitingInput.remove(player.uniqueId) ?: return false
        val townName = inputData.townName
        val isOpMode = inputData.isOpMode

        val homeName = message.trim()
        if (homeName.isEmpty()) {
            player.sendMessage(serializer.deserialize(manager.getMessage("gui_name_empty")))
            return true
        }

        // OP 模式下绕过等级限制检查
        if (!isOpMode) {
            val townLevel = manager.getPlayerTownLevel(player)
            if (!manager.canCreateHome(townName, homeName, townLevel)) {
                val limit = manager.getLimit(townLevel)
                player.sendMessage(serializer.deserialize(
                    manager.getMessage("limit_reached", "limit" to limit.toString())
                ))
                return true
            }
        }

        val isOverwrite = manager.getHome(townName, homeName) != null

        if (manager.setHome(townName, homeName, player)) {
            if (isOverwrite) {
                player.sendMessage(serializer.deserialize(
                    manager.getMessage("set_overwrite", "name" to homeName)
                ))
            } else {
                player.sendMessage(serializer.deserialize(
                    manager.getMessage("set_success", "name" to homeName)
                ))
            }
            // 重新打开 GUI（保持原来的模式）
            openPHomeList(player, townName, 0, isOpMode)
        } else {
            player.sendMessage(serializer.deserialize(manager.getMessage("set_failed")))
        }

        return true
    }

    /**
     * 检查玩家是否在等待输入
     */
    fun isAwaitingInput(player: Player): Boolean {
        return awaitingInput.containsKey(player.uniqueId)
    }

    /**
     * 取消等待输入
     */
    fun cancelInput(player: Player) {
        awaitingInput.remove(player.uniqueId)
    }
}
