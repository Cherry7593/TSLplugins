package org.tsl.tSLplugins.modules.landmark

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
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

/**
 * 地标系统 GUI 菜单
 */
class LandmarkGUI(
    private val plugin: JavaPlugin,
    private val manager: LandmarkManager,
    private val listener: LandmarkListener
) : Listener {

    private val serializer = LegacyComponentSerializer.legacyAmpersand()

    private val ITEMS_PER_PAGE = 45
    private val GUI_SIZE = 54

    enum class MenuType {
        MAIN,
        EDIT
    }

    /**
     * 自定义 InventoryHolder，存储 GUI 状态信息
     * 这样点击处理可以直接从 holder 获取状态，不依赖外部 Map
     */
    private inner class LandmarkInventoryHolder(
        val menuType: MenuType,
        val page: Int = 0,
        val editingLandmarkId: String? = null
    ) : InventoryHolder {
        override fun getInventory(): Inventory {
            throw UnsupportedOperationException("This is a virtual holder")
        }
    }

    /**
     * 打开主菜单
     */
    fun openMainMenu(player: Player, page: Int = 0) {
        val landmarks = manager.getAllLandmarks().toList()
        val totalPages = maxOf(1, (landmarks.size + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE)
        val currentPage = page.coerceIn(0, totalPages - 1)

        val holder = LandmarkInventoryHolder(MenuType.MAIN, page = currentPage)
        val title = serializer.deserialize("&6地标列表 &7(${currentPage + 1}/$totalPages)")
        val inventory = Bukkit.createInventory(holder, GUI_SIZE, title)

        // 填充地标物品
        val startIndex = currentPage * ITEMS_PER_PAGE
        val endIndex = minOf(startIndex + ITEMS_PER_PAGE, landmarks.size)

        for (i in startIndex until endIndex) {
            val landmark = landmarks[i]
            val slot = i - startIndex
            inventory.setItem(slot, createLandmarkItem(player, landmark))
        }

        // 底部导航栏
        fillNavigationBar(inventory, currentPage, totalPages)

        player.openInventory(inventory)
    }

    /**
     * 打开编辑菜单
     */
    fun openEditMenu(player: Player, landmark: Landmark) {
        val holder = LandmarkInventoryHolder(MenuType.EDIT, editingLandmarkId = landmark.id)
        val title = serializer.deserialize("&6编辑地标: ${landmark.name}")
        val inventory = Bukkit.createInventory(holder, 27, title)

        // 图标设置
        val iconItem = ItemStack(Material.ITEM_FRAME)
        val iconMeta = iconItem.itemMeta
        iconMeta.displayName(serializer.deserialize("&e设置图标"))
        iconMeta.lore(listOf(
            serializer.deserialize("&7当前: &f${landmark.icon}"),
            serializer.deserialize("&7点击手持物品设置图标")
        ))
        iconItem.itemMeta = iconMeta
        inventory.setItem(11, iconItem)

        // 传送点设置
        val warpItem = ItemStack(Material.ENDER_PEARL)
        val warpMeta = warpItem.itemMeta
        warpMeta.displayName(serializer.deserialize("&e设置传送点"))
        val warpLore = if (landmark.warpPoint != null) {
            listOf(
                serializer.deserialize("&7当前: &f${landmark.warpPoint!!.x.toInt()}, ${landmark.warpPoint!!.y.toInt()}, ${landmark.warpPoint!!.z.toInt()}"),
                serializer.deserialize("&7点击设置为当前位置")
            )
        } else {
            listOf(
                serializer.deserialize("&7当前: &c未设置"),
                serializer.deserialize("&7点击设置为当前位置")
            )
        }
        warpMeta.lore(warpLore)
        warpItem.itemMeta = warpMeta
        inventory.setItem(13, warpItem)

        // 默认解锁切换（仅管理员）
        if (player.hasPermission("tsl.landmark.admin")) {
            val unlockItem = ItemStack(if (landmark.defaultUnlocked) Material.LIME_DYE else Material.GRAY_DYE)
            val unlockMeta = unlockItem.itemMeta
            unlockMeta.displayName(serializer.deserialize("&e默认解锁"))
            unlockMeta.lore(listOf(
                serializer.deserialize("&7当前: ${if (landmark.defaultUnlocked) "&a是" else "&c否"}"),
                serializer.deserialize("&7点击切换")
            ))
            unlockItem.itemMeta = unlockMeta
            inventory.setItem(15, unlockItem)
        }

        // 返回按钮
        val backItem = ItemStack(Material.ARROW)
        val backMeta = backItem.itemMeta
        backMeta.displayName(serializer.deserialize("&c返回"))
        backItem.itemMeta = backMeta
        inventory.setItem(22, backItem)

        player.openInventory(inventory)
    }

    /**
     * 创建地标物品
     */
    private fun createLandmarkItem(player: Player, landmark: Landmark): ItemStack {
        val unlocked = manager.isUnlocked(player.uniqueId, landmark)
        val material = if (unlocked) {
            Material.matchMaterial(landmark.icon) ?: Material.ENDER_PEARL
        } else {
            Material.BARRIER
        }

        val item = ItemStack(material)
        val meta = item.itemMeta

        val nameColor = if (unlocked) "&a" else "&c"
        meta.displayName(serializer.deserialize("$nameColor${landmark.name}"))

        val lore = mutableListOf<Component>()

        // 状态
        if (unlocked) {
            lore.add(serializer.deserialize("&a✓ 已解锁"))
        } else {
            lore.add(serializer.deserialize("&c✗ 未解锁"))
        }

        if (landmark.defaultUnlocked) {
            lore.add(serializer.deserialize("&7(默认解锁)"))
        }

        // 描述
        if (landmark.lore.isNotEmpty()) {
            lore.add(serializer.deserialize(""))
            landmark.lore.forEach { line ->
                lore.add(serializer.deserialize("&7$line"))
            }
        }

        // 坐标（如果配置启用）
        if (manager.showCoordsInGUI && unlocked) {
            lore.add(serializer.deserialize(""))
            lore.add(serializer.deserialize("&7世界: &f${landmark.world}"))
            val center = landmark.region.center()
            lore.add(serializer.deserialize("&7坐标: &f${center.first.toInt()}, ${center.second.toInt()}, ${center.third.toInt()}"))
        }

        // 操作提示
        lore.add(serializer.deserialize(""))
        if (unlocked) {
            lore.add(serializer.deserialize("&e左键传送"))
            // 检查是否有编辑权限
            val canEdit = player.hasPermission("tsl.landmark.admin") || 
                          manager.isMaintainer(player.uniqueId, landmark)
            if (canEdit) {
                lore.add(serializer.deserialize("&b右键编辑"))
            }
        } else {
            lore.add(serializer.deserialize("&c需要先解锁"))
        }

        meta.lore(lore)
        item.itemMeta = meta

        return item
    }

    /**
     * 填充导航栏
     */
    private fun fillNavigationBar(inventory: Inventory, currentPage: Int, totalPages: Int) {
        // 填充玻璃板
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

        // 下一页
        if (currentPage < totalPages - 1) {
            val nextItem = ItemStack(Material.ARROW)
            val nextMeta = nextItem.itemMeta
            nextMeta.displayName(serializer.deserialize("&e下一页"))
            nextItem.itemMeta = nextMeta
            inventory.setItem(53, nextItem)
        }

        // 关闭按钮
        val closeItem = ItemStack(Material.BARRIER)
        val closeMeta = closeItem.itemMeta
        closeMeta.displayName(serializer.deserialize("&c关闭"))
        closeItem.itemMeta = closeMeta
        inventory.setItem(49, closeItem)
    }

    /**
     * 处理物品栏点击
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        
        // 通过 InventoryHolder 判断是否为地标 GUI，并获取状态
        val holder = event.inventory.holder as? LandmarkInventoryHolder ?: return
        
        // 始终取消事件，防止物品被取出
        event.isCancelled = true

        // 防止 shift 点击和数字键移动物品
        if (event.click.isShiftClick || event.click.isKeyboardClick) {
            return
        }

        val slot = event.rawSlot
        if (slot < 0) return

        // 只处理上方物品栏的点击
        if (event.clickedInventory != event.view.topInventory) return

        // 直接从 holder 获取状态信息，不依赖外部 Map
        when (holder.menuType) {
            MenuType.MAIN -> handleMainMenuClick(player, slot, event, holder.page)
            MenuType.EDIT -> handleEditMenuClick(player, slot, event, holder.editingLandmarkId)
        }
    }

    /**
     * 处理主菜单点击
     */
    private fun handleMainMenuClick(player: Player, slot: Int, event: InventoryClickEvent, currentPage: Int) {
        val landmarks = manager.getAllLandmarks().toList()

        when (slot) {
            45 -> {
                // 上一页
                if (currentPage > 0) {
                    openMainMenu(player, currentPage - 1)
                }
            }
            49 -> {
                // 关闭
                player.closeInventory()
            }
            53 -> {
                // 下一页
                val totalPages = maxOf(1, (landmarks.size + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE)
                if (currentPage < totalPages - 1) {
                    openMainMenu(player, currentPage + 1)
                }
            }
            in 0 until ITEMS_PER_PAGE -> {
                // 点击地标
                val index = currentPage * ITEMS_PER_PAGE + slot
                if (index < landmarks.size) {
                    val landmark = landmarks[index]
                    val isRightClick = event.isRightClick
                    handleLandmarkClick(player, landmark, isRightClick)
                }
            }
        }
    }

    /**
     * 处理地标点击（左键传送，右键编辑）
     */
    private fun handleLandmarkClick(player: Player, landmark: Landmark, isRightClick: Boolean) {
        if (isRightClick) {
            // 右键：打开编辑菜单（需要权限）
            val canEdit = player.hasPermission("tsl.landmark.admin") || 
                          manager.isMaintainer(player.uniqueId, landmark)
            if (canEdit) {
                openEditMenu(player, landmark)
            }
            // 无权限则静默忽略
            return
        }

        // 左键：传送
        if (!manager.isUnlocked(player.uniqueId, landmark)) {
            player.sendMessage(serializer.deserialize(
                manager.getMessage("not_unlocked", "landmark" to landmark.name)
            ))
            return
        }

        player.closeInventory()

        val result = manager.canTeleport(player, landmark)
        when (result) {
            TeleportResult.NOT_IN_LANDMARK -> {
                player.sendMessage(serializer.deserialize(manager.getMessage("not_in_landmark")))
            }
            TeleportResult.ON_COOLDOWN -> {
                player.sendMessage(serializer.deserialize(manager.getMessage("on_cooldown")))
            }
            TeleportResult.SUCCESS -> {
                if (manager.teleport(player, landmark)) {
                    if (manager.castTimeSeconds <= 0) {
                        player.sendMessage(serializer.deserialize(
                            manager.getMessage("teleport_success", "landmark" to landmark.name)
                        ))
                    } else {
                        // 启动吟唱任务
                        listener.startCastTimeTask(player)
                        player.sendMessage(serializer.deserialize(
                            manager.getMessage("teleport_started", "seconds" to manager.castTimeSeconds.toString())
                        ))
                    }
                } else {
                    player.sendMessage(serializer.deserialize(manager.getMessage("teleport_failed")))
                }
            }
            else -> {
                player.sendMessage(serializer.deserialize(manager.getMessage("teleport_failed")))
            }
        }
    }

    /**
     * 处理编辑菜单点击
     */
    private fun handleEditMenuClick(player: Player, slot: Int, event: InventoryClickEvent, editingLandmarkId: String?) {
        val landmarkId = editingLandmarkId ?: return
        val landmark = manager.storage.getLandmark(landmarkId) ?: return

        when (slot) {
            11 -> {
                // 设置图标
                val heldItem = player.inventory.itemInMainHand
                if (heldItem.type != Material.AIR) {
                    landmark.icon = heldItem.type.name
                    manager.storage.updateLandmark(landmark)
                    player.sendMessage(serializer.deserialize(
                        manager.getMessage("edit_success", "field" to "icon", "value" to heldItem.type.name)
                    ))
                    openEditMenu(player, landmark)
                }
            }
            13 -> {
                // 设置传送点
                val loc = player.location
                // 验证玩家位置是否在目标地标的区域范围内
                if (loc.world?.name != landmark.world || !landmark.region.contains(loc.x, loc.y, loc.z)) {
                    player.sendMessage(serializer.deserialize(
                        manager.getMessage("warp_not_in_region", "landmark" to landmark.name)
                    ))
                    openEditMenu(player, landmark)
                    return
                }
                landmark.warpPoint = WarpPoint(loc.x, loc.y, loc.z, loc.yaw, loc.pitch)
                manager.storage.updateLandmark(landmark)
                player.sendMessage(serializer.deserialize(
                    manager.getMessage("warp_set", "landmark" to landmark.name)
                ))
                openEditMenu(player, landmark)
            }
            15 -> {
                // 切换默认解锁
                if (player.hasPermission("tsl.landmark.admin")) {
                    landmark.defaultUnlocked = !landmark.defaultUnlocked
                    manager.storage.updateLandmark(landmark)
                    player.sendMessage(serializer.deserialize(
                        manager.getMessage("edit_success", "field" to "defaultUnlocked", "value" to landmark.defaultUnlocked.toString())
                    ))
                    openEditMenu(player, landmark)
                }
            }
            22 -> {
                // 返回
                openMainMenu(player)
            }
        }
    }

    // 状态已完全存储在 LandmarkInventoryHolder 中，无需 onInventoryClose 清理
}
