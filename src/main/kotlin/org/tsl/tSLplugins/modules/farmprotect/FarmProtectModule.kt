package org.tsl.tSLplugins.modules.farmprotect

import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityInteractEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.tsl.tSLplugins.core.AbstractModule

/**
 * 农田保护模块
 * 防止玩家和生物踩踏农田
 */
class FarmProtectModule : AbstractModule() {
    override val id = "farmprotect"
    override val configPath = "farmprotect"
    override fun getDescription() = "农田保护（防踩踏）"

    override fun doEnable() {
        registerListener(FarmProtectListener())
    }

    /**
     * 农田保护监听器
     */
    private inner class FarmProtectListener : Listener {

        @EventHandler
        fun onEntityInteract(event: EntityInteractEvent) {
            if (event.block.type == Material.FARMLAND) {
                event.isCancelled = true
            }
        }

        @EventHandler
        fun onPlayerInteract(event: PlayerInteractEvent) {
            if (event.action == Action.PHYSICAL &&
                event.clickedBlock?.type == Material.FARMLAND) {
                event.isCancelled = true
            }
        }
    }
}
