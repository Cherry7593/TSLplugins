package org.tsl.tSLplugins.Advancement

import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

class TSLPlaceholderExpansion(
    private val plugin: JavaPlugin,
    private val countHandler: AdvancementCount
) : PlaceholderExpansion() {

    override fun getIdentifier(): String = "tsl"

    override fun getAuthor(): String = plugin.description.authors.toString()

    override fun getVersion(): String = plugin.description.version

    override fun persist(): Boolean = true // 插件重载时不注销

    override fun onPlaceholderRequest(player: Player?, params: String): String? {
        if (player == null) {
            return null
        }

        // %tsl_adv_count%
        if (params.equals("adv_count", ignoreCase = true)) {
            return countHandler.getAdvancementCount(player).toString()
        }

        return null
    }
}

