package org.tsl.tSLplugins.modules.mcedia

import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import java.util.UUID

/**
 * Mcedia GUI 的自定义 InventoryHolder
 * 用于正确识别 Mcedia 的 GUI 界面并存储状态信息
 */
class McediaInventoryHolder(
    val guiType: McediaGUI.GUIType,
    val page: Int = 0,
    val editingPlayerUUID: UUID? = null
) : InventoryHolder {
    override fun getInventory(): Inventory {
        throw UnsupportedOperationException("This is a virtual holder")
    }
}

