package org.tsl.tSLplugins.modules.visitor

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.title.Title
import net.luckperms.api.LuckPerms
import net.luckperms.api.event.user.UserDataRecalculateEvent
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityTargetEvent
import org.bukkit.event.player.*
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.tsl.tSLplugins.SubCommandHandler
import org.tsl.tSLplugins.core.AbstractModule
import java.time.Duration
import java.util.UUID

/**
 * Visitor 模块 - 访客模式
 * 基于 LuckPerms 权限组检测访客身份
 */
class VisitorModule : AbstractModule() {

    override val id = "visitor"
    override val configPath = "visitor"

    private var luckPerms: LuckPerms? = null
    private val visitorPlayers = mutableSetOf<UUID>()
    private val manualVisitors = mutableSetOf<UUID>()
    private val messageCooldowns = mutableMapOf<String, Long>()
    private val cooldownTime = 2000L

    private var visitorGroups: List<String> = emptyList()
    private var restrictBlockBreak = true
    private var restrictBlockPlace = true
    private var restrictItemUse = true
    private var restrictContainerOpen = true
    private var restrictPressurePlate = true
    private var restrictEntityDamage = true

    private lateinit var listener: VisitorModuleListener

    override fun doEnable() {
        loadVisitorConfig()
        setupLuckPerms()
        listener = VisitorModuleListener(this)
        registerListener(listener)
    }

    override fun doDisable() {
        visitorPlayers.forEach { uuid ->
            Bukkit.getPlayer(uuid)?.removePotionEffect(PotionEffectType.GLOWING)
        }
        visitorPlayers.clear()
        manualVisitors.clear()
        messageCooldowns.clear()
    }

    override fun doReload() {
        loadVisitorConfig()
    }

    override fun getCommandHandler(): SubCommandHandler = VisitorModuleCommand(this)
    override fun getDescription(): String = "访客模式"

    private fun loadVisitorConfig() {
        visitorGroups = getConfigStringList("groups")
        restrictBlockBreak = getConfigBoolean("restrictions.block-break", true)
        restrictBlockPlace = getConfigBoolean("restrictions.block-place", true)
        restrictItemUse = getConfigBoolean("restrictions.item-use", true)
        restrictContainerOpen = getConfigBoolean("restrictions.container-open", true)
        restrictPressurePlate = getConfigBoolean("restrictions.pressure-plate", true)
        restrictEntityDamage = getConfigBoolean("restrictions.entity-damage", true)
        logInfo("访客权限组: $visitorGroups")
    }

    private fun setupLuckPerms() {
        val provider = Bukkit.getServicesManager().getRegistration(LuckPerms::class.java)
        if (provider != null) {
            luckPerms = provider.provider
            luckPerms?.eventBus?.subscribe(context.plugin, UserDataRecalculateEvent::class.java, ::onPermissionChange)
            logInfo("LuckPerms 集成成功")
        } else {
            logWarning("未找到 LuckPerms！访客模式权限组检测不可用")
        }
    }

    fun isVisitor(uuid: UUID): Boolean = visitorPlayers.contains(uuid)
    fun isVisitor(player: Player): Boolean = visitorPlayers.contains(player.uniqueId)

    fun setVisitor(player: Player, isVisitor: Boolean, silent: Boolean = false) {
        val uuid = player.uniqueId
        val wasVisitor = visitorPlayers.contains(uuid)
        if (isVisitor) {
            manualVisitors.add(uuid)
            if (!wasVisitor) {
                applyVisitorEffect(player)
                if (!silent) sendGainedMessage(player)
            }
        } else {
            manualVisitors.remove(uuid)
            if (wasVisitor && !checkVisitorByGroup(player)) {
                removeVisitorEffect(player)
                if (!silent) sendLostMessage(player)
            }
        }
    }

    fun checkVisitorByGroup(player: Player): Boolean {
        if (manualVisitors.contains(player.uniqueId)) return true
        if (visitorGroups.isEmpty()) return false
        val lp = luckPerms ?: return false
        val user = lp.userManager.getUser(player.uniqueId) ?: return false
        return visitorGroups.any { it.equals(user.primaryGroup, ignoreCase = true) }
    }

    fun applyVisitorEffect(player: Player) {
        player.addPotionEffect(PotionEffect(PotionEffectType.GLOWING, PotionEffect.INFINITE_DURATION, 0, false, false, false))
        visitorPlayers.add(player.uniqueId)
    }

    fun removeVisitorEffect(player: Player) {
        player.removePotionEffect(PotionEffectType.GLOWING)
        visitorPlayers.remove(player.uniqueId)
    }

    fun sendCooldownMessage(player: Player, messageType: String, message: String) {
        val now = System.currentTimeMillis()
        val cooldownKey = "${player.uniqueId}-$messageType"
        if (now - (messageCooldowns[cooldownKey] ?: 0L) >= cooldownTime) {
            player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(message))
            messageCooldowns[cooldownKey] = now
        }
    }

    fun sendGainedMessage(player: Player) {
        val chatMsg = getConfigString("gained.chat", "&a[访客模式] &7你已进入访客模式！")
        val titleText = getConfigString("gained.title", "&a访客模式")
        val subtitleText = getConfigString("gained.subtitle", "&7已启用")
        val soundName = getConfigString("gained.sound", "entity.player.levelup")
        player.sendMessage(colorize(chatMsg))
        player.showTitle(Title.title(colorize(titleText), colorize(subtitleText), Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3000), Duration.ofMillis(1000))))
        playSound(player, soundName)
    }

    fun sendLostMessage(player: Player) {
        val chatMsg = getConfigString("lost.chat", "&c[访客模式] &7你已退出访客模式！")
        val titleText = getConfigString("lost.title", "&c访客模式")
        val subtitleText = getConfigString("lost.subtitle", "&7已禁用")
        val soundName = getConfigString("lost.sound", "block.note_block.bass")
        player.sendMessage(colorize(chatMsg))
        player.showTitle(Title.title(colorize(titleText), colorize(subtitleText), Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3000), Duration.ofMillis(1000))))
        playSound(player, soundName)
    }

    private fun playSound(player: Player, soundName: String) {
        try {
            val key = NamespacedKey.minecraft(soundName.lowercase())
            Registry.SOUNDS.get(key)?.let { player.playSound(player.location, it, 1.0f, 1.0f) }
        } catch (_: Exception) {}
    }

    private fun onPermissionChange(event: UserDataRecalculateEvent) {
        val uuid = event.user.uniqueId
        val player = Bukkit.getPlayer(uuid) ?: return
        if (!player.isOnline) return
        player.scheduler.runDelayed(context.plugin, { _ ->
            if (!player.isOnline) return@runDelayed
            val isVisitorNow = checkVisitorByGroup(player)
            val wasVisitor = visitorPlayers.contains(uuid)
            when {
                isVisitorNow && !wasVisitor -> { applyVisitorEffect(player); sendGainedMessage(player) }
                !isVisitorNow && wasVisitor -> { removeVisitorEffect(player); sendLostMessage(player) }
            }
        }, null, 10L)
    }

    fun cleanupPlayer(uuid: UUID) {
        visitorPlayers.remove(uuid)
        messageCooldowns.keys.removeIf { it.startsWith("$uuid-") }
    }

    private fun colorize(text: String): Component = LegacyComponentSerializer.legacyAmpersand().deserialize(text)

    // Getters for restrictions
    fun isRestrictBlockBreak() = restrictBlockBreak
    fun isRestrictBlockPlace() = restrictBlockPlace
    fun isRestrictContainerOpen() = restrictContainerOpen
    fun isRestrictPressurePlate() = restrictPressurePlate
    fun isRestrictEntityDamage() = restrictEntityDamage
    fun isRestrictItemUse() = restrictItemUse
    fun getPlugin() = context.plugin
    fun reloadModule() = loadVisitorConfig()
}

class VisitorModuleListener(private val module: VisitorModule) : Listener {
    @EventHandler(priority = EventPriority.HIGH) fun onEntityTarget(event: EntityTargetEvent) {
        if (!module.isEnabled()) return
        val player = event.target as? Player ?: return
        if (module.isVisitor(player)) event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.NORMAL) fun onPlayerJoin(event: PlayerJoinEvent) {
        if (!module.isEnabled()) return
        val player = event.player
        player.scheduler.runDelayed(module.getPlugin(), { _ ->
            if (!player.isOnline) return@runDelayed
            val isVisitorNow = module.checkVisitorByGroup(player)
            val wasVisitor = module.isVisitor(player)
            when {
                isVisitorNow && !wasVisitor -> { module.applyVisitorEffect(player); module.sendGainedMessage(player) }
                isVisitorNow && wasVisitor -> module.applyVisitorEffect(player)
                !isVisitorNow && wasVisitor -> { module.removeVisitorEffect(player); module.sendLostMessage(player) }
            }
        }, null, 20L)
    }

    @EventHandler fun onPlayerQuit(event: PlayerQuitEvent) { module.cleanupPlayer(event.player.uniqueId) }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true) fun onBlockBreak(event: BlockBreakEvent) {
        if (!module.isEnabled() || !module.isRestrictBlockBreak()) return
        if (module.isVisitor(event.player)) { event.isCancelled = true; module.sendCooldownMessage(event.player, "break", "&c访客不能破坏方块！") }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true) fun onBlockPlace(event: BlockPlaceEvent) {
        if (!module.isEnabled() || !module.isRestrictBlockPlace()) return
        if (module.isVisitor(event.player)) { event.isCancelled = true; module.sendCooldownMessage(event.player, "place", "&c访客不能放置方块！") }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true) fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        if (!module.isEnabled() || !module.isRestrictEntityDamage()) return
        val damager = event.damager as? Player ?: return
        if (module.isVisitor(damager)) { event.isCancelled = true; module.sendCooldownMessage(damager, "damage", "&c访客不能攻击实体！") }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true) fun onPlayerInteract(event: PlayerInteractEvent) {
        if (!module.isEnabled()) return
        if (!module.isVisitor(event.player)) return
        val player = event.player
        val block = event.clickedBlock
        if (module.isRestrictContainerOpen() && block != null) {
            val type = block.type
            if (type.name.contains("CHEST") || type.name.contains("BARREL") || type.name.contains("SHULKER_BOX") || type.name.contains("HOPPER") || type.name.contains("FURNACE")) {
                event.isCancelled = true; module.sendCooldownMessage(player, "container", "&c访客不能打开容器！"); return
            }
        }
        if (module.isRestrictPressurePlate() && block != null) {
            val type = block.type
            if (type.name.contains("DOOR") || type.name.contains("TRAPDOOR") || type.name.contains("PRESSURE_PLATE") || type.name.contains("BUTTON") || type.name.contains("LEVER")) {
                event.isCancelled = true; module.sendCooldownMessage(player, "redstone", "&c访客不能使用红石设施！"); return
            }
        }
    }
}

class VisitorModuleCommand(private val module: VisitorModule) : SubCommandHandler {
    override fun handle(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!module.isEnabled()) { sender.sendMessage("§c访客模式功能已禁用"); return true }
        if (!sender.hasPermission("tsl.visitor.admin")) { sender.sendMessage("§c你没有权限使用此命令！"); return true }
        if (args.isEmpty()) { sendHelp(sender); return true }
        when (args[0].lowercase()) {
            "set" -> { if (args.size < 2) { sender.sendMessage("§c用法: /tsl visitor set <玩家名>"); return true }; val target = Bukkit.getPlayer(args[1]) ?: run { sender.sendMessage("§c玩家不在线"); return true }; module.setVisitor(target, true, false); sender.sendMessage("§a已将玩家 ${target.name} 设置为访客") }
            "remove" -> { if (args.size < 2) { sender.sendMessage("§c用法: /tsl visitor remove <玩家名>"); return true }; val target = Bukkit.getPlayer(args[1]) ?: run { sender.sendMessage("§c玩家不在线"); return true }; module.setVisitor(target, false, false); sender.sendMessage("§a已移除玩家 ${target.name} 的访客身份") }
            "check" -> { if (args.size < 2) { sender.sendMessage("§c用法: /tsl visitor check <玩家名>"); return true }; val target = Bukkit.getPlayer(args[1]) ?: run { sender.sendMessage("§c玩家不在线"); return true }; sender.sendMessage(if (module.isVisitor(target)) "§a玩家 ${target.name} 当前是访客" else "§7玩家 ${target.name} 当前不是访客") }
            "list" -> { val onlineVisitors = Bukkit.getOnlinePlayers().filter { module.isVisitor(it) }; if (onlineVisitors.isEmpty()) sender.sendMessage("§7当前没有在线的访客") else { sender.sendMessage("§a在线访客列表 (${onlineVisitors.size}):"); onlineVisitors.forEach { sender.sendMessage("§7  - ${it.name}") } } }
            "reload" -> { module.reloadModule(); sender.sendMessage("§a访客模式配置已重新加载！") }
            else -> sendHelp(sender)
        }
        return true
    }

    private fun sendHelp(sender: CommandSender) {
        sender.sendMessage("§6§l=== 访客模式管理 ===")
        sender.sendMessage("§e/tsl visitor set <玩家名> §7- 设置玩家为访客")
        sender.sendMessage("§e/tsl visitor remove <玩家名> §7- 移除玩家的访客身份")
        sender.sendMessage("§e/tsl visitor check <玩家名> §7- 检查玩家是否是访客")
        sender.sendMessage("§e/tsl visitor list §7- 列出所有在线访客")
        sender.sendMessage("§e/tsl visitor reload §7- 重新加载配置")
    }

    override fun tabComplete(sender: CommandSender, command: Command, label: String, args: Array<out String>): List<String> {
        if (!module.isEnabled() || !sender.hasPermission("tsl.visitor.admin")) return emptyList()
        return when (args.size) {
            1 -> listOf("set", "remove", "check", "list", "reload").filter { it.startsWith(args[0].lowercase()) }
            2 -> when (args[0].lowercase()) {
                "set", "check" -> Bukkit.getOnlinePlayers().map { it.name }.filter { it.lowercase().startsWith(args[1].lowercase()) }
                "remove" -> Bukkit.getOnlinePlayers().filter { module.isVisitor(it) }.map { it.name }.filter { it.lowercase().startsWith(args[1].lowercase()) }
                else -> emptyList()
            }
            else -> emptyList()
        }
    }

    override fun getDescription(): String = "访客模式管理命令"
}
