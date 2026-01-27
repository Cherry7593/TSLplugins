package org.tsl.tSLplugins.modules.snowantimelt

import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockFadeEvent
import org.tsl.tSLplugins.core.AbstractModule

/**
 * 雪防融化模块
 * 阻止雪层和雪块因光照融化
 */
class SnowAntiMeltModule : AbstractModule() {
    override val id = "snow-anti-melt"
    override val configPath = "snow-anti-melt"
    override fun getDescription() = "雪防融化"

    override fun doEnable() {
        registerListener(SnowAntiMeltListener())
    }

    /**
     * 雪防融化监听器
     */
    private inner class SnowAntiMeltListener : Listener {

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        fun onBlockFade(event: BlockFadeEvent) {
            val blockType = event.block.type
            val newType = event.newState.type

            // 如果是雪层或雪块，且要变成空气，则阻止
            if ((blockType == Material.SNOW || blockType == Material.SNOW_BLOCK) && newType == Material.AIR) {
                event.isCancelled = true
            }
        }
    }
}
