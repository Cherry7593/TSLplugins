package org.tsl.tSLplugins.Maintenance

import com.destroystokyo.paper.event.server.PaperServerListPingEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import java.util.UUID

/**
 * 维护模式 MOTD 监听器
 * 在维护模式下自定义服务器列表信息
 */
class MaintenanceMotdListener(private val manager: MaintenanceManager) : Listener {

    private val serializer = LegacyComponentSerializer.legacyAmpersand()

    @EventHandler(priority = EventPriority.HIGH)
    fun onServerListPing(event: PaperServerListPingEvent) {
        // 如果维护模式未启用，不做任何修改
        if (!manager.isMaintenanceEnabled()) {
            return
        }

        val config = manager.getConfig()

        // 修改 MOTD
        val motdLines = config.getStringList("maintenance.motd")
        if (motdLines.isNotEmpty()) {
            val builder = Component.text()
            motdLines.forEachIndexed { index, line ->
                if (index > 0) builder.append(Component.newline())
                builder.append(serializer.deserialize(line))
            }
            event.motd(builder.build())
        }

        // 修改版本信息
        val versionText = config.getString("maintenance.version-text")
        if (!versionText.isNullOrEmpty()) {
            // 转换颜色代码后设置为字符串
            event.version = serializer.serialize(serializer.deserialize(versionText))
        }

        // 修改协议版本（可选，设置为 -1 会显示为不兼容）
        if (config.getBoolean("maintenance.show-incompatible-version", false)) {
            event.protocolVersion = -1
        }

        // 修改玩家人数显示
        val showFakePlayers = config.getBoolean("maintenance.show-fake-players", true)
        if (showFakePlayers) {
            val fakeOnline = config.getInt("maintenance.fake-online", 0)
            val fakeMax = config.getInt("maintenance.fake-max", 0)
            event.numPlayers = fakeOnline
            event.maxPlayers = fakeMax
        }

        // 修改悬浮玩家列表（使用新的 API）
        val hoverLines = config.getStringList("maintenance.hover-message")
        if (hoverLines.isNotEmpty()) {
            // 创建新的玩家样本列表
            val newSamples = mutableListOf<com.destroystokyo.paper.profile.PlayerProfile>()

            // 添加自定义的悬浮信息（伪装成玩家名）
            hoverLines.forEach { line ->
                val translatedLine = serializer.serialize(serializer.deserialize(line))
                // 创建假玩家用于显示悬浮信息
                try {
                    val profile = Bukkit.createProfile(UUID.randomUUID(), translatedLine)
                    newSamples.add(profile)
                } catch (_: Exception) {
                    // 如果创建失败，忽略
                }
            }

            // 使用反射或者其他方法设置玩家样本
            // 由于新版本 API 已弃用 playerSample，我们需要构建完整的列表
            try {
                val listedPlayersMethod = event.javaClass.getMethod("getListedPlayers")
                @Suppress("UNCHECKED_CAST")
                val listedPlayers = listedPlayersMethod.invoke(event) as MutableCollection<com.destroystokyo.paper.profile.PlayerProfile>
                listedPlayers.clear()
                listedPlayers.addAll(newSamples)
            } catch (_: Exception) {
                // 如果新 API 不可用，尝试使用旧的方式
                try {
                    @Suppress("DEPRECATION")
                    event.playerSample.clear()
                    @Suppress("DEPRECATION")
                    event.playerSample.addAll(newSamples)
                } catch (_: Exception) {
                    // 忽略错误
                }
            }
        }
    }
}

