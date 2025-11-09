package org.tsl.tSLplugins.Visitor

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.title.Title
import net.luckperms.api.LuckPerms
import net.luckperms.api.event.user.UserDataRecalculateEvent
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityTargetEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.time.Duration
import java.util.UUID

class VisitorEffect(private val plugin: JavaPlugin) : Listener {

    private var luckPerms: LuckPerms? = null
    private val visitorPlayers = mutableSetOf<UUID>()

    init {
        setupLuckPerms()
    }

    private fun setupLuckPerms() {
        val provider = Bukkit.getServicesManager().getRegistration(LuckPerms::class.java)
        if (provider != null) {
            luckPerms = provider.provider
            // 注册 LuckPerms 事件监听器
            luckPerms?.eventBus?.subscribe(plugin, UserDataRecalculateEvent::class.java, ::onPermissionChange)
            plugin.logger.info("LuckPerms 集成成功！")
        } else {
            plugin.logger.warning("未找到 LuckPerms！访客模式权限变更检测将不可用。")
        }
    }

    @EventHandler
    fun onEntityTarget(event: EntityTargetEvent) {
        val player = event.target as? Player ?: return
        if (player.hasPermission("tsl.visitor")) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        val uuid = player.uniqueId

        // 检查玩家是否有访客权限 - 使用实体调度器以兼容 Folia
        player.scheduler.runDelayed(plugin, { _ ->
            if (!player.isOnline) return@runDelayed

            val hasPermission = player.hasPermission("tsl.visitor")
            val wasVisitor = visitorPlayers.contains(uuid)

            when {
                hasPermission && !wasVisitor -> {
                    // 玩家现在有权限，应用效果并通知（可能是离线期间获得的）
                    applyVisitorEffect(player)
                    sendGainedMessage(player)
                }
                hasPermission && wasVisitor -> {
                    // 玩家仍有权限，只应用效果，不通知（静默恢复）
                    applyVisitorEffect(player)
                }
                !hasPermission && wasVisitor -> {
                    // 玩家失去了权限（可能是离线期间失去的），移除效果并通知
                    removeVisitorEffect(player)
                    sendLostMessage(player)
                }
                // 如果 !hasPermission && !wasVisitor，说明玩家本来就没有权限，不做任何操作
            }
        }, null, 20L) // 延迟1秒以确保权限加载完成
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        visitorPlayers.remove(event.player.uniqueId)
    }

    private fun onPermissionChange(event: UserDataRecalculateEvent) {
        val uuid = event.user.uniqueId
        val player = Bukkit.getPlayer(uuid) ?: return

        if (player.isOnline) {
            // 延迟检查，确保权限已更新 - 使用实体调度器以兼容 Folia
            player.scheduler.runDelayed(plugin, { _ ->
                if (player.isOnline) {
                    val hasPermission = player.hasPermission("tsl.visitor")
                    val wasVisitor = visitorPlayers.contains(uuid)

                    when {
                        hasPermission && !wasVisitor -> {
                            // 获得访客权限
                            applyVisitorEffect(player)
                            sendGainedMessage(player)
                        }
                        !hasPermission && wasVisitor -> {
                            // 失去访客权限
                            removeVisitorEffect(player)
                            sendLostMessage(player)
                        }
                    }
                }
            }, null, 5L)
        }
    }

    private fun applyVisitorEffect(player: Player) {
        // 添加发光效果（永久，直到移除）
        player.addPotionEffect(
            PotionEffect(
                PotionEffectType.GLOWING,
                PotionEffect.INFINITE_DURATION,
                0,
                false,
                false,
                false
            )
        )
        visitorPlayers.add(player.uniqueId)
    }

    private fun removeVisitorEffect(player: Player) {
        // 移除发光效果
        player.removePotionEffect(PotionEffectType.GLOWING)
        visitorPlayers.remove(player.uniqueId)
    }

    private fun sendGainedMessage(player: Player) {
        val chatMsg = plugin.config.getString("visitor.gained.chat", "&a[访客模式] &7你已进入访客模式！") ?: ""
        val titleText = plugin.config.getString("visitor.gained.title", "&a访客模式") ?: ""
        val subtitleText = plugin.config.getString("visitor.gained.subtitle", "&7已启用") ?: ""
        val soundName = plugin.config.getString("visitor.gained.sound", "entity.player.levelup") ?: ""

        // 发送聊天消息
        player.sendMessage(colorize(chatMsg))

        // 发送 Title
        val title = colorize(titleText)
        val subtitle = colorize(subtitleText)
        player.showTitle(
            Title.title(
                title,
                subtitle,
                Title.Times.times(
                    Duration.ofMillis(500),
                    Duration.ofMillis(3000),
                    Duration.ofMillis(1000)
                )
            )
        )

        // 播放音效
        playSound(player, soundName)
    }

    private fun sendLostMessage(player: Player) {
        val chatMsg = plugin.config.getString("visitor.lost.chat", "&c[访客模式] &7你已退出访客模式！") ?: ""
        val titleText = plugin.config.getString("visitor.lost.title", "&c访客模式") ?: ""
        val subtitleText = plugin.config.getString("visitor.lost.subtitle", "&7已禁用") ?: ""
        val soundName = plugin.config.getString("visitor.lost.sound", "block.note_block.bass") ?: ""

        // 发送聊天消息
        player.sendMessage(colorize(chatMsg))

        // 发送 Title
        val title = colorize(titleText)
        val subtitle = colorize(subtitleText)
        player.showTitle(
            Title.title(
                title,
                subtitle,
                Title.Times.times(
                    Duration.ofMillis(500),
                    Duration.ofMillis(3000),
                    Duration.ofMillis(1000)
                )
            )
        )

        // 播放音效
        playSound(player, soundName)
    }

    private fun playSound(player: Player, soundName: String) {
        try {
            val key = NamespacedKey.minecraft(soundName.lowercase())
            val sound = Registry.SOUNDS.get(key)
            if (sound != null) {
                player.playSound(player.location, sound, 1.0f, 1.0f)
            } else {
                plugin.logger.warning("无效的音效名称: $soundName")
            }
        } catch (e: Exception) {
            plugin.logger.warning("无效的音效名称: $soundName - ${e.message}")
        }
    }

    private fun colorize(text: String): Component {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text)
    }
}

