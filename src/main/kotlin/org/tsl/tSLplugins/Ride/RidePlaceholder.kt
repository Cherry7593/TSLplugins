package org.tsl.tSLplugins.Ride

import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.plugin.java.JavaPlugin

/**
 * Ride PlaceholderAPI 扩展
 * 提供变量：
 * - %tsl_ride_toggle% - 玩家Ride功能开关状态
 */
class RidePlaceholder(
    private val plugin: JavaPlugin,
    private val manager: RideManager
) : PlaceholderExpansion() {

    override fun getIdentifier(): String = "tsl"

    override fun getAuthor(): String = "TSL"

    override fun getVersion(): String = plugin.description.version

    override fun persist(): Boolean = true

    override fun onRequest(player: OfflinePlayer?, params: String): String? {
        if (player == null) return null

        return when (params) {
            "ride_toggle" -> {
                // 只有在线玩家才能查询 PDC
                val onlinePlayer = Bukkit.getPlayer(player.uniqueId)
                if (onlinePlayer != null) {
                    if (manager.isPlayerEnabled(onlinePlayer)) "启用" else "禁用"
                } else {
                    "离线"
                }
            }
            else -> null
        }
    }
}

