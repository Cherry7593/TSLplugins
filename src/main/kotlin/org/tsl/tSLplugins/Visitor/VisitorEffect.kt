package org.tsl.tSLplugins.Visitor

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.title.Title
import net.luckperms.api.LuckPerms
import net.luckperms.api.event.user.UserDataRecalculateEvent
import net.luckperms.api.model.user.User
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityTargetEvent
import org.bukkit.event.player.*
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.time.Duration
import java.util.UUID

/**
 * 访客模式管理器
 * - 基于权限组检测访客身份
 * - 性能优化的事件处理
 * - 可配置的访客限制
 * - 管理命令支持
 */
class VisitorEffect(private val plugin: JavaPlugin) : Listener {

    private var luckPerms: LuckPerms? = null

    // 访客玩家集合（用于快速查询，避免重复权限检查）
    private val visitorPlayers = mutableSetOf<UUID>()

    // 手动设置的访客（优先级高于权限组检测）
    private val manualVisitors = mutableSetOf<UUID>()

    // 消息冷却机制（防止刷屏）
    // Key: "玩家UUID-消息类型", Value: 最后发送时间（毫秒）
    private val messageCooldowns = mutableMapOf<String, Long>()
    private val cooldownTime = 2000L // 冷却时间：2秒

    // 配置项缓存
    private var enabled: Boolean = true
    private var visitorGroups: List<String> = emptyList()

    // 限制开关
    private var restrictBlockBreak: Boolean = true
    private var restrictBlockPlace: Boolean = true
    private var restrictItemUse: Boolean = true
    private var restrictContainerOpen: Boolean = true
    private var restrictPressurePlate: Boolean = true
    private var restrictEntityDamage: Boolean = true

    init {
        loadConfig()
        setupLuckPerms()
    }

    /**
     * 加载配置
     */
    fun loadConfig() {
        enabled = plugin.config.getBoolean("visitor.enabled", true)
        visitorGroups = plugin.config.getStringList("visitor.groups")

        // 加载限制配置
        restrictBlockBreak = plugin.config.getBoolean("visitor.restrictions.block-break", true)
        restrictBlockPlace = plugin.config.getBoolean("visitor.restrictions.block-place", true)
        restrictItemUse = plugin.config.getBoolean("visitor.restrictions.item-use", true)
        restrictContainerOpen = plugin.config.getBoolean("visitor.restrictions.container-open", true)
        restrictPressurePlate = plugin.config.getBoolean("visitor.restrictions.pressure-plate", true)
        restrictEntityDamage = plugin.config.getBoolean("visitor.restrictions.entity-damage", true)

        plugin.logger.info("访客模式已加载配置，访客权限组: $visitorGroups")
    }

    /**
     * 检查 Visitor 功能是否启用
     */
    fun isEnabled(): Boolean = enabled

    /**
     * 快速检查玩家是否是访客（使用缓存，性能优化）
     */
    fun isVisitor(uuid: UUID): Boolean {
        return visitorPlayers.contains(uuid)
    }

    /**
     * 快速检查玩家是否是访客（Player 版本）
     */
    fun isVisitor(player: Player): Boolean {
        return visitorPlayers.contains(player.uniqueId)
    }

    /**
     * 发送带冷却的限制消息（防止刷屏）
     * @param player 玩家
     * @param messageType 消息类型（用于区分不同的限制）
     * @param message 消息内容
     */
    private fun sendCooldownMessage(player: Player, messageType: String, message: String) {
        val now = System.currentTimeMillis()
        val cooldownKey = "${player.uniqueId}-$messageType"
        val lastSent = messageCooldowns[cooldownKey] ?: 0L

        // 如果距离上次发送超过冷却时间，才发送消息
        if (now - lastSent >= cooldownTime) {
            player.sendMessage(colorize(message))
            messageCooldowns[cooldownKey] = now
        }
    }

    /**
     * 手动设置玩家为访客
     */
    fun setVisitor(player: Player, isVisitor: Boolean, silent: Boolean = false) {
        val uuid = player.uniqueId
        val wasVisitor = visitorPlayers.contains(uuid)

        if (isVisitor) {
            manualVisitors.add(uuid)
            if (!wasVisitor) {
                applyVisitorEffect(player)
                if (!silent) {
                    sendGainedMessage(player)
                }
                plugin.logger.info("管理员手动设置玩家 ${player.name} 为访客")
            }
        } else {
            manualVisitors.remove(uuid)
            if (wasVisitor) {
                // 重新检查权限组，如果权限组也不是访客才移除效果
                if (!checkVisitorByGroup(player)) {
                    removeVisitorEffect(player)
                    if (!silent) {
                        sendLostMessage(player)
                    }
                    plugin.logger.info("管理员手动取消玩家 ${player.name} 的访客身份")
                }
            }
        }
    }

    /**
     * 设置 LuckPerms 集成，监听权限变更事件
     */
    private fun setupLuckPerms() {
        val provider = Bukkit.getServicesManager().getRegistration(LuckPerms::class.java)
        if (provider != null) {
            luckPerms = provider.provider
            luckPerms?.eventBus?.subscribe(plugin, UserDataRecalculateEvent::class.java, ::onPermissionChange)
            plugin.logger.info("LuckPerms 集成成功！访客模式将通过权限组实时检测。")
        } else {
            plugin.logger.warning("未找到 LuckPerms！访客模式权限组检测将不可用。")
        }
    }

    /**
     * 检查玩家是否属于访客权限组
     * 优先级：手动设置 > 权限组检测
     */
    private fun checkVisitorByGroup(player: Player): Boolean {
        // 优先检查手动设置
        if (manualVisitors.contains(player.uniqueId)) {
            return true
        }

        // 如果没有配置访客组，返回 false
        if (visitorGroups.isEmpty()) {
            return false
        }

        val lp = luckPerms ?: return false
        val user = lp.userManager.getUser(player.uniqueId) ?: return false

        // 获取玩家的主权限组（优先级最高的组）
        val primaryGroup = user.primaryGroup

        // 检查主权限组是否在访客组列表中
        if (visitorGroups.any { it.equals(primaryGroup, ignoreCase = true) }) {
            return true
        }

        // 检查玩家的所有继承组
        val inheritedGroups = user.nodes.stream()
            .filter { it.key.startsWith("group.") }
            .map { it.key.substring(6) }
            .toList()

        // 如果玩家同时拥有访客组和非访客组，以 LuckPerms 优先级为准（主权限组）
        // 所以这里只检查主权限组已经足够
        return false
    }

    // ==================== 事件监听器 ====================

    @EventHandler(priority = EventPriority.HIGH)
    fun onEntityTarget(event: EntityTargetEvent) {
        if (!isEnabled()) return

        val player = event.target as? Player ?: return

        // 性能优化：直接使用缓存检查
        if (visitorPlayers.contains(player.uniqueId)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        if (!isEnabled()) return

        val player = event.player
        val uuid = player.uniqueId

        // 使用实体调度器以兼容 Folia
        player.scheduler.runDelayed(plugin, { _ ->
            if (!player.isOnline) return@runDelayed

            val isVisitorNow = checkVisitorByGroup(player)
            val wasVisitor = visitorPlayers.contains(uuid)

            when {
                isVisitorNow && !wasVisitor -> {
                    applyVisitorEffect(player)
                    sendGainedMessage(player)
                }
                isVisitorNow && wasVisitor -> {
                    // 静默恢复效果（重新登录）
                    applyVisitorEffect(player)
                }
                !isVisitorNow && wasVisitor -> {
                    removeVisitorEffect(player)
                    sendLostMessage(player)
                }
            }
        }, null, 20L)
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val uuid = event.player.uniqueId
        visitorPlayers.remove(uuid)

        // 清理该玩家的所有冷却记录
        messageCooldowns.keys.removeIf { it.startsWith("$uuid-") }

        // 不清理 manualVisitors，保持持久化
    }

    /**
     * LuckPerms 权限变更事件处理
     */
    private fun onPermissionChange(event: UserDataRecalculateEvent) {
        val uuid = event.user.uniqueId
        val player = Bukkit.getPlayer(uuid) ?: return
        if (!player.isOnline) return

        player.scheduler.runDelayed(plugin, { _ ->
            if (!player.isOnline) return@runDelayed

            val isVisitorNow = checkVisitorByGroup(player)
            val wasVisitor = visitorPlayers.contains(uuid)

            when {
                isVisitorNow && !wasVisitor -> {
                    applyVisitorEffect(player)
                    sendGainedMessage(player)
                    plugin.logger.info("玩家 ${player.name} 获得了访客身份（权限组变更）")
                }
                !isVisitorNow && wasVisitor -> {
                    removeVisitorEffect(player)
                    sendLostMessage(player)
                    plugin.logger.info("玩家 ${player.name} 失去了访客身份（权限组变更）")
                }
            }
        }, null, 10L)
    }

    // ==================== 访客限制事件 ====================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onBlockBreak(event: BlockBreakEvent) {
        if (!isEnabled() || !restrictBlockBreak) return

        // 性能优化：直接使用缓存检查
        if (visitorPlayers.contains(event.player.uniqueId)) {
            event.isCancelled = true
            sendCooldownMessage(event.player, "break", "&c访客不能破坏方块！")
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onBlockPlace(event: BlockPlaceEvent) {
        if (!isEnabled() || !restrictBlockPlace) return

        if (visitorPlayers.contains(event.player.uniqueId)) {
            event.isCancelled = true
            sendCooldownMessage(event.player, "place", "&c访客不能放置方块！")
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (!isEnabled()) return
        if (!visitorPlayers.contains(event.player.uniqueId)) return

        val player = event.player
        val block = event.clickedBlock

        // 检查容器打开
        if (restrictContainerOpen && block != null) {
            val type = block.type
            if (type.name.contains("CHEST") || type.name.contains("BARREL") ||
                type.name.contains("SHULKER_BOX") || type.name.contains("HOPPER") ||
                type.name.contains("FURNACE") || type.name.contains("DISPENSER") ||
                type.name.contains("DROPPER") || type.name.contains("BREWING_STAND")) {
                event.isCancelled = true
                sendCooldownMessage(player, "container", "&c访客不能打开容器！")
                return
            }
        }

        // 检查门和活板门
        if (restrictPressurePlate && block != null) {
            val type = block.type
            if (type.name.contains("DOOR") || type.name.contains("TRAPDOOR") ||
                type.name.contains("FENCE_GATE") || type.name.contains("GATE")) {
                event.isCancelled = true
                sendCooldownMessage(player, "door", "&c访客不能使用门！")
                return
            }
        }

        // 检查压力板和红石设施（使用冷却机制防止刷屏）
        if (restrictPressurePlate && block != null) {
            val type = block.type
            if (type.name.contains("PRESSURE_PLATE") || type.name.contains("BUTTON") ||
                type.name.contains("LEVER") || type.name.contains("TRIPWIRE")) {
                event.isCancelled = true
                // 使用冷却消息，避免踩压力板时频繁提示
                sendCooldownMessage(player, "redstone", "&c访客不能使用红石设施！")
                return
            }
        }

        // 检查物品使用
        if (restrictItemUse && event.hasItem()) {
            val item = event.item
            if (item != null && !item.type.isBlock) {
                // 排除一些基本物品（如食物）
                if (!item.type.isEdible) {
                    event.isCancelled = true
                    sendCooldownMessage(player, "item", "&c访客不能使用该物品！")
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        if (!isEnabled() || !restrictEntityDamage) return

        val damager = event.damager as? Player ?: return

        // 访客不能攻击任何实体
        if (visitorPlayers.contains(damager.uniqueId)) {
            event.isCancelled = true
            sendCooldownMessage(damager, "damage", "&c访客不能攻击实体！")
        }
    }

    // ==================== 效果管理 ====================

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

