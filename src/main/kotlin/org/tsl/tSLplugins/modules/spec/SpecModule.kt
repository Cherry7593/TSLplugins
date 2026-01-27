package org.tsl.tSLplugins.modules.spec

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import org.tsl.tSLplugins.SubCommandHandler
import org.tsl.tSLplugins.core.AbstractModule
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Spec 模块 - 观众模式
 * 
 * 允许管理员以旁观者模式循环浏览玩家视角
 */
class SpecModule : AbstractModule() {

    override val id = "spec"
    override val configPath = "spec"

    private val whitelist = ConcurrentHashMap.newKeySet<UUID>()
    private val spectatingPlayers = ConcurrentHashMap<UUID, SpectatorState>()

    private var defaultDelay: Int = 5
    private var minDelay: Int = 1
    private var maxDelay: Int = 60

    private lateinit var listener: SpecModuleListener

    override fun doEnable() {
        loadSpecConfig()
        listener = SpecModuleListener(this)
        registerListener(listener)
    }

    override fun doDisable() {
        cleanup()
    }

    override fun doReload() {
        loadSpecConfig()
    }

    override fun getCommandHandler(): SubCommandHandler = SpecModuleCommand(this)
    override fun getDescription(): String = "观众模式"

    private fun loadSpecConfig() {
        defaultDelay = getConfigInt("defaultDelay", 5)
        minDelay = getConfigInt("minDelay", 1)
        maxDelay = getConfigInt("maxDelay", 60)

        whitelist.clear()
        val whitelistNames = getConfigStringList("whitelist")
        whitelistNames.forEach { name ->
            try {
                val uuid = UUID.fromString(name)
                whitelist.add(uuid)
            } catch (e: IllegalArgumentException) {
                @Suppress("DEPRECATION")
                val offlinePlayer = Bukkit.getOfflinePlayer(name)
                if (offlinePlayer.hasPlayedBefore()) {
                    whitelist.add(offlinePlayer.uniqueId)
                }
            }
        }
    }

    fun getDefaultDelay(): Int = defaultDelay
    fun validateDelay(delay: Int): Int = delay.coerceIn(minDelay, maxDelay)

    fun startSpectating(player: Player, delay: Int): Boolean {
        if (isSpectating(player)) return false

        val originalGameMode = player.gameMode
        val originalLocation = player.location.clone()

        player.scheduler.run(context.plugin, { _ ->
            try { player.gameMode = GameMode.SPECTATOR } catch (_: Exception) {}
        }, null)

        val state = SpectatorState(player, originalGameMode, originalLocation, delay, 0)
        spectatingPlayers[player.uniqueId] = state
        startCycleTask(state)
        return true
    }

    fun stopSpectating(player: Player): Boolean {
        val state = spectatingPlayers.remove(player.uniqueId) ?: return false
        state.cancelTask()

        player.scheduler.run(context.plugin, { _ ->
            try {
                player.gameMode = state.originalGameMode
                player.teleportAsync(state.originalLocation)
            } catch (_: Exception) {}
        }, null)
        return true
    }

    fun isSpectating(player: Player): Boolean = spectatingPlayers.containsKey(player.uniqueId)

    private fun startCycleTask(state: SpectatorState) {
        val delayTicks = state.delay * 20L
        Bukkit.getGlobalRegionScheduler().runAtFixedRate(context.plugin, { _ ->
            try {
                val spectator = Bukkit.getPlayer(state.spectator.uniqueId)
                if (spectator == null || !spectator.isOnline || !spectatingPlayers.containsKey(spectator.uniqueId)) {
                    return@runAtFixedRate
                }
                switchToNextPlayer(spectator, state)
            } catch (_: Exception) {}
        }, delayTicks, delayTicks)
    }

    private fun switchToNextPlayer(spectator: Player, state: SpectatorState) {
        val allViewablePlayers = getViewablePlayers(spectator)
        if (allViewablePlayers.isEmpty()) {
            spectator.scheduler.run(context.plugin, { _ ->
                spectator.sendMessage("§e[Spec] 没有可观看的玩家")
            }, null)
            return
        }

        var availablePlayers = allViewablePlayers.filter { !state.viewedPlayers.contains(it.uniqueId) }
        if (availablePlayers.isEmpty()) {
            state.viewedPlayers.clear()
            availablePlayers = allViewablePlayers
        }

        val targetPlayer = availablePlayers.random()
        state.viewedPlayers.add(targetPlayer.uniqueId)

        spectator.scheduler.run(context.plugin, { _ ->
            try {
                spectator.spectatorTarget = targetPlayer
                spectator.sendMessage("§a[Spec] 正在观看: §f${targetPlayer.name}")
            } catch (_: Exception) {}
        }, null)
    }

    private fun getViewablePlayers(spectator: Player): List<Player> {
        return Bukkit.getOnlinePlayers()
            .filter { player ->
                player.uniqueId != spectator.uniqueId &&
                !whitelist.contains(player.uniqueId) &&
                !spectatingPlayers.containsKey(player.uniqueId)
            }
            .sortedBy { it.name }
    }

    fun addToWhitelist(uuid: UUID): Boolean {
        val added = whitelist.add(uuid)
        if (added) saveWhitelist()
        return added
    }

    fun removeFromWhitelist(uuid: UUID): Boolean {
        val removed = whitelist.remove(uuid)
        if (removed) saveWhitelist()
        return removed
    }

    fun isInWhitelist(uuid: UUID): Boolean = whitelist.contains(uuid)
    fun getWhitelist(): Set<UUID> = whitelist.toSet()

    private fun saveWhitelist() {
        val config = context.plugin.config
        config.set("spec.whitelist", whitelist.map { it.toString() })
        context.plugin.saveConfig()
    }

    fun onPlayerQuit(player: Player) {
        if (isSpectating(player)) {
            spectatingPlayers.remove(player.uniqueId)?.cancelTask()
        }
    }

    private fun cleanup() {
        spectatingPlayers.values.forEach { state ->
            try {
                state.cancelTask()
                val player = Bukkit.getPlayer(state.spectator.uniqueId)
                player?.scheduler?.run(context.plugin, { _ ->
                    try {
                        player.gameMode = state.originalGameMode
                        player.teleportAsync(state.originalLocation)
                    } catch (_: Exception) {}
                }, null)
            } catch (_: Exception) {}
        }
        spectatingPlayers.clear()
    }
}

data class SpectatorState(
    val spectator: Player,
    val originalGameMode: GameMode,
    val originalLocation: Location,
    val delay: Int,
    var currentIndex: Int,
    val viewedPlayers: MutableSet<UUID> = mutableSetOf()
) {
    private var taskCancelled = false
    fun cancelTask() { taskCancelled = true }
}

class SpecModuleListener(private val module: SpecModule) : Listener {
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        module.onPlayerQuit(event.player)
    }
}

class SpecModuleCommand(private val module: SpecModule) : SubCommandHandler {
    override fun handle(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!module.isEnabled()) {
            sender.sendMessage(Component.text("观众模式功能未启用！", NamedTextColor.RED))
            return true
        }
        if (!sender.hasPermission("tsl.spec.use")) {
            sender.sendMessage(Component.text("你没有权限使用此命令！", NamedTextColor.RED))
            return true
        }
        if (args.isEmpty()) { showHelp(sender); return true }

        return when (args[0].lowercase()) {
            "start" -> handleStart(sender, args)
            "stop" -> handleStop(sender)
            "add" -> handleAdd(sender, args)
            "remove" -> handleRemove(sender, args)
            "list" -> handleList(sender)
            else -> { showHelp(sender); true }
        }
    }

    private fun handleStart(sender: CommandSender, args: Array<out String>): Boolean {
        if (sender !is Player) { sender.sendMessage(Component.text("此命令只能由玩家使用！", NamedTextColor.RED)); return true }
        val delay = if (args.size >= 2) args[1].toIntOrNull() ?: module.getDefaultDelay() else module.getDefaultDelay()
        val validDelay = module.validateDelay(delay)
        val success = module.startSpectating(sender, validDelay)
        if (success) {
            sender.sendMessage(Component.text("✓ 已开始循环观看模式，每 $validDelay 秒切换", NamedTextColor.GREEN))
        } else {
            sender.sendMessage(Component.text("你已经在观看模式中！", NamedTextColor.RED))
        }
        return true
    }

    private fun handleStop(sender: CommandSender): Boolean {
        if (sender !is Player) { sender.sendMessage(Component.text("此命令只能由玩家使用！", NamedTextColor.RED)); return true }
        val success = module.stopSpectating(sender)
        if (success) {
            sender.sendMessage(Component.text("✓ 已停止观看模式", NamedTextColor.GREEN))
        } else {
            sender.sendMessage(Component.text("你没有在观看模式中！", NamedTextColor.RED))
        }
        return true
    }

    private fun handleAdd(sender: CommandSender, args: Array<out String>): Boolean {
        if (args.size < 2) { sender.sendMessage(Component.text("用法: /tsl spec add <玩家名>", NamedTextColor.RED)); return true }
        @Suppress("DEPRECATION")
        val target = Bukkit.getOfflinePlayer(args[1])
        if (!target.hasPlayedBefore() && !target.isOnline) { sender.sendMessage(Component.text("找不到玩家: ${args[1]}", NamedTextColor.RED)); return true }
        val added = module.addToWhitelist(target.uniqueId)
        if (added) sender.sendMessage(Component.text("✓ 已将 ${target.name ?: args[1]} 添加到白名单", NamedTextColor.GREEN))
        else sender.sendMessage(Component.text("该玩家已在白名单中！", NamedTextColor.RED))
        return true
    }

    private fun handleRemove(sender: CommandSender, args: Array<out String>): Boolean {
        if (args.size < 2) { sender.sendMessage(Component.text("用法: /tsl spec remove <玩家名>", NamedTextColor.RED)); return true }
        @Suppress("DEPRECATION")
        val target = Bukkit.getOfflinePlayer(args[1])
        if (!target.hasPlayedBefore() && !target.isOnline) { sender.sendMessage(Component.text("找不到玩家: ${args[1]}", NamedTextColor.RED)); return true }
        val removed = module.removeFromWhitelist(target.uniqueId)
        if (removed) sender.sendMessage(Component.text("✓ 已将 ${target.name ?: args[1]} 从白名单移除", NamedTextColor.GREEN))
        else sender.sendMessage(Component.text("该玩家不在白名单中！", NamedTextColor.RED))
        return true
    }

    private fun handleList(sender: CommandSender): Boolean {
        val whitelist = module.getWhitelist()
        if (whitelist.isEmpty()) { sender.sendMessage(Component.text("白名单为空", NamedTextColor.YELLOW)); return true }
        sender.sendMessage(Component.text("===== 观看白名单 (${whitelist.size}) =====", NamedTextColor.GOLD))
        whitelist.forEachIndexed { index, uuid ->
            @Suppress("DEPRECATION")
            val playerName = Bukkit.getOfflinePlayer(uuid).name ?: uuid.toString()
            sender.sendMessage(Component.text("${index + 1}. $playerName", NamedTextColor.GRAY))
        }
        return true
    }

    private fun showHelp(sender: CommandSender) {
        sender.sendMessage(Component.text("===== 观众模式命令 =====", NamedTextColor.GOLD))
        sender.sendMessage(Component.text("/tsl spec start [延迟] - 开始循环观看", NamedTextColor.AQUA))
        sender.sendMessage(Component.text("/tsl spec stop - 停止观看", NamedTextColor.AQUA))
        sender.sendMessage(Component.text("/tsl spec add <玩家> - 添加到白名单", NamedTextColor.AQUA))
        sender.sendMessage(Component.text("/tsl spec remove <玩家> - 从白名单移除", NamedTextColor.AQUA))
        sender.sendMessage(Component.text("/tsl spec list - 查看白名单", NamedTextColor.AQUA))
    }

    override fun tabComplete(sender: CommandSender, command: Command, label: String, args: Array<out String>): List<String> {
        if (!sender.hasPermission("tsl.spec.use")) return emptyList()
        return when (args.size) {
            1 -> listOf("start", "stop", "add", "remove", "list").filter { it.startsWith(args[0], ignoreCase = true) }
            2 -> when (args[0].lowercase()) {
                "start" -> listOf("3", "5", "10", "15", "30").filter { it.startsWith(args[1]) }
                "add", "remove" -> Bukkit.getOnlinePlayers().map { it.name }.filter { it.startsWith(args[1], ignoreCase = true) }
                else -> emptyList()
            }
            else -> emptyList()
        }
    }

    override fun getDescription(): String = "观众模式命令"
}
