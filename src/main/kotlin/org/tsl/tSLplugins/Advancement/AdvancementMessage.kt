package org.tsl.tSLplugins.Advancement

import io.papermc.paper.advancement.AdvancementDisplay
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerAdvancementDoneEvent

class AdvancementMessage : Listener {

    @EventHandler
    fun onAdvancement(event: PlayerAdvancementDoneEvent) {
        // 仅当该进度有可显示信息时才处理
        val display = event.advancement.display ?: return

        // 只屏蔽普通（绿色）Task 进度在公屏的消息，其它（Goal/Challenge）保留
        if (display.frame() == AdvancementDisplay.Frame.TASK) {
            // 将将要广播到公屏的消息置为空，达到不在公屏显示的效果
            event.message(null)
        }
    }
}

