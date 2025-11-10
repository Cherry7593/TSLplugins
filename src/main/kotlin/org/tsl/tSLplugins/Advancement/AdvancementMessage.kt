package org.tsl.tSLplugins.Advancement

import io.papermc.paper.advancement.AdvancementDisplay
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerAdvancementDoneEvent
import org.bukkit.plugin.java.JavaPlugin

class AdvancementMessage(private val plugin: JavaPlugin) : Listener {

    private var enabled: Boolean = true

    init {
        loadConfig()
    }

    /**
     * 加载配置
     */
    fun loadConfig() {
        enabled = plugin.config.getBoolean("advancement.enabled", true)
    }

    @EventHandler
    fun onAdvancement(event: PlayerAdvancementDoneEvent) {
        // 检查功能是否启用
        if (!enabled) return

        // 仅当该进度有可显示信息时才处理
        val display = event.advancement.display ?: return

        // 只屏蔽普通（绿色）Task 进度在公屏的消息，其它（Goal/Challenge）保留
        if (display.frame() == AdvancementDisplay.Frame.TASK) {
            // 将将要广播到公屏的消息置为空，达到不在公屏显示的效果
            event.message(null)
        }
    }
}

