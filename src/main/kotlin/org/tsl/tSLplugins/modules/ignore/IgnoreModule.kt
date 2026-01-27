package org.tsl.tSLplugins.modules.ignore

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.tsl.tSLplugins.SubCommandHandler
import org.tsl.tSLplugins.core.AbstractModule
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Ignore 模块 - 聊天屏蔽
 * 
 * 管理玩家之间的单向屏蔽关系
 */
class IgnoreModule : AbstractModule() {

    override val id = "ignore"
    override val configPath = "ignore"

    private val ignoreMap = ConcurrentHashMap<UUID, MutableSet<UUID>>()
    private var maxIgnoreCount = 100

    private lateinit var listener: IgnoreModuleListener

    override fun doEnable() {
        loadIgnoreConfig()
        listener = IgnoreModuleListener(this)
        registerListener(listener)
    }

    override fun doDisable() {
        ignoreMap.clear()
    }

    override fun doReload() {
        loadIgnoreConfig()
    }

    override fun getCommandHandler(): SubCommandHandler = IgnoreModuleCommand(this)
    override fun getDescription(): String = "聊天屏蔽"

    private fun loadIgnoreConfig() {
        maxIgnoreCount = getConfigInt("max-ignore-count", 100)
    }

    fun getMaxIgnoreCount(): Int = maxIgnoreCount

    fun addIgnore(viewer: UUID, target: UUID): Boolean {
        if (viewer == target) return false
        val ignoreSet = ignoreMap.computeIfAbsent(viewer) { ConcurrentHashMap.newKeySet() }
        if (ignoreSet.size >= maxIgnoreCount) return false
        return ignoreSet.add(target)
    }

    fun removeIgnore(viewer: UUID, target: UUID): Boolean {
        return ignoreMap[viewer]?.remove(target) ?: false
    }

    fun toggleIgnore(viewer: UUID, target: UUID): Pair<Boolean, Boolean> {
        return if (isIgnoring(viewer, target)) {
            Pair(removeIgnore(viewer, target), false)
        } else {
            Pair(addIgnore(viewer, target), true)
        }
    }

    fun isIgnoring(viewer: UUID, sender: UUID): Boolean {
        return ignoreMap[viewer]?.contains(sender) ?: false
    }

    fun getIgnoreList(viewer: UUID): Set<UUID> {
        return ignoreMap[viewer]?.toSet() ?: emptySet()
    }

    fun loadPlayerData(playerUuid: UUID, ignoreList: Set<UUID>) {
        if (ignoreList.isNotEmpty()) {
            val set = ConcurrentHashMap.newKeySet<UUID>()
            set.addAll(ignoreList)
            ignoreMap[playerUuid] = set
        }
    }

    fun getPlayerData(playerUuid: UUID): Set<UUID> {
        return ignoreMap[playerUuid]?.toSet() ?: emptySet()
    }

    fun unloadPlayerData(playerUuid: UUID) {
        ignoreMap.remove(playerUuid)
    }

    fun getPlugin() = context.plugin
    fun getPlayerDataManager() = context.playerDataManager
}

class IgnoreModuleListener(private val module: IgnoreModule) : Listener {
    @EventHandler(priority = EventPriority.LOW)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        if (!module.isEnabled()) return
        val player = event.player
        val ignoreList = module.getPlayerDataManager().getIgnoreList(player)
        module.loadPlayerData(player.uniqueId, ignoreList)
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        if (!module.isEnabled()) return
        val player = event.player
        module.getPlayerDataManager().setIgnoreList(player, module.getPlayerData(player.uniqueId))
        module.unloadPlayerData(player.uniqueId)
    }
}

class IgnoreModuleCommand(private val module: IgnoreModule) : SubCommandHandler {
    override fun handle(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) { sender.sendMessage("§c该命令只能由玩家执行"); return true }
        if (!module.isEnabled()) { sender.sendMessage("§c聊天屏蔽功能未启用"); return true }
        if (args.isEmpty()) { showUsage(sender); return true }

        when (args[0].lowercase()) {
            "list" -> handleList(sender)
            else -> handleToggle(sender, args[0])
        }
        return true
    }

    private fun handleToggle(player: Player, targetName: String) {
        val target = Bukkit.getPlayer(targetName)
        if (target == null) { player.sendMessage("§c玩家 $targetName 不在线"); return }
        if (target.uniqueId == player.uniqueId) { player.sendMessage("§c你不能屏蔽自己"); return }

        val (success, isNowIgnoring) = module.toggleIgnore(player.uniqueId, target.uniqueId)
        if (!success) {
            player.sendMessage(if (isNowIgnoring) "§c屏蔽失败，已达最大屏蔽数量 (${module.getMaxIgnoreCount()})" else "§c操作失败")
            return
        }

        if (isNowIgnoring) {
            player.sendMessage("§a已屏蔽玩家 §f${target.name}§a，你将不再收到他的聊天消息")
        } else {
            player.sendMessage("§a已取消屏蔽玩家 §f${target.name}")
        }
        module.getPlayerDataManager().setIgnoreList(player, module.getIgnoreList(player.uniqueId))
    }

    private fun handleList(player: Player) {
        val ignoreList = module.getIgnoreList(player.uniqueId)
        if (ignoreList.isEmpty()) { player.sendMessage("§7你的屏蔽列表为空"); return }

        player.sendMessage("§6========== 屏蔽列表 (${ignoreList.size}/${module.getMaxIgnoreCount()}) ==========")
        ignoreList.forEach { uuid ->
            val targetPlayer = Bukkit.getPlayer(uuid)
            val name = targetPlayer?.name ?: Bukkit.getOfflinePlayer(uuid).name ?: uuid.toString()
            val status = if (targetPlayer != null) "§a在线" else "§7离线"
            player.sendMessage("§7  - §f$name $status")
        }
    }

    private fun showUsage(player: Player) {
        player.sendMessage("§e用法:")
        player.sendMessage("§7  /tsl ignore <玩家名> §8- §f屏蔽/取消屏蔽玩家")
        player.sendMessage("§7  /tsl ignore list §8- §f查看屏蔽列表")
    }

    override fun tabComplete(sender: CommandSender, command: Command, label: String, args: Array<out String>): List<String> {
        if (args.size == 1) {
            val input = args[0].lowercase()
            val suggestions = mutableListOf("list")
            Bukkit.getOnlinePlayers()
                .filter { it.name != (sender as? Player)?.name }
                .map { it.name }
                .filter { it.lowercase().startsWith(input) }
                .forEach { suggestions.add(it) }
            return suggestions.filter { it.lowercase().startsWith(input) }
        }
        return emptyList()
    }

    override fun getDescription(): String = "管理聊天屏蔽列表"
}
