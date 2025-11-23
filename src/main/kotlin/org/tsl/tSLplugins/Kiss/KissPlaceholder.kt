package org.tsl.tSLplugins.Kiss

import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.plugin.java.JavaPlugin

/**
 * Kiss PlaceholderAPI 扩展
 * 提供变量：
 * - %tsl_kiss_count% - 玩家亲吻次数
 * - %tsl_kissed_count% - 玩家被亲吻次数
 * - %tsl_kiss_toggle% - 玩家Kiss功能开关状态
 */
class KissPlaceholder(
    private val plugin: JavaPlugin,
    private val manager: KissManager
) : PlaceholderExpansion() {

    override fun getIdentifier(): String = "tsl"

    override fun getAuthor(): String = "TSL"

    override fun getVersion(): String = plugin.description.version

    override fun persist(): Boolean = true

    override fun onRequest(player: OfflinePlayer?, params: String): String? {
        if (player == null) return null

        return when (params) {
            "kiss_count" -> manager.getKissCount(player.uniqueId).toString()
            "kissed_count" -> manager.getKissedCount(player.uniqueId).toString()
            "kiss_toggle" -> {
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

