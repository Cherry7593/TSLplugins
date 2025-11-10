package org.tsl.tSLplugins.Farmprotect

import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityInteractEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.plugin.java.JavaPlugin

class FarmProtect(private val plugin: JavaPlugin) : Listener {

    private var enabled: Boolean = true

    init {
        loadConfig()
    }

    /**
     * 加载配置
     */
    fun loadConfig() {
        enabled = plugin.config.getBoolean("farmprotect.enabled", true)
    }

    // 防止生物踩踏农田
    @EventHandler
    fun onEntityInteract(event: EntityInteractEvent) {
        if (!enabled) return

        if (event.block.type == Material.FARMLAND) {
            event.isCancelled = true
        }
    }

    // 防止玩家踩踏农田
    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (!enabled) return

        if (event.action == Action.PHYSICAL &&
            event.clickedBlock?.type == Material.FARMLAND) {
            event.isCancelled = true
        }
    }
}

