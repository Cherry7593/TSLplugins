package org.tsl.tSLplugins.Farmprotect

import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityInteractEvent
import org.bukkit.event.player.PlayerInteractEvent

class FarmProtect : Listener {

    // 防止生物踩踏农田
    @EventHandler
    fun onEntityInteract(event: EntityInteractEvent) {
        if (event.block.type == Material.FARMLAND) {
            event.isCancelled = true
        }
    }

    // 防止玩家踩踏农田
    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.action == Action.PHYSICAL &&
            event.clickedBlock?.type == Material.FARMLAND) {
            event.isCancelled = true
        }
    }
}

