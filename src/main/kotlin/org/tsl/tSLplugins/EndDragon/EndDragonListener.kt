package org.tsl.tSLplugins.EndDragon

import org.bukkit.entity.EnderDragon
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.plugin.java.JavaPlugin

/**
 * 末影龙监听器
 * 处理末影龙破坏方块相关逻辑
 */
class EndDragonListener(
    private val plugin: JavaPlugin,
    private val manager: EndDragonManager
) : Listener {

    /**
     * 监听实体爆炸事件
     * 末影龙撞击方块时会触发爆炸事件，通过清空方块列表来禁止破坏
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onEntityExplode(event: EntityExplodeEvent) {
        // 检查功能是否启用
        if (!manager.isDisableDamage()) return

        // 检查爆炸源是末影龙
        if (event.entity !is EnderDragon) return

        // 清空要破坏的方块列表
        event.blockList().clear()

        plugin.logger.fine("[EndDragon] 阻止末影龙破坏方块")
    }

}

