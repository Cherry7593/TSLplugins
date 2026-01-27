package org.tsl.tSLplugins.modules.peace

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.entity.Monster
import org.bukkit.entity.Slime
import org.bukkit.entity.MagmaCube
import org.bukkit.entity.Phantom
import org.bukkit.entity.Ghast
import org.bukkit.entity.Shulker
import org.bukkit.entity.Hoglin
import org.bukkit.entity.Piglin
import org.bukkit.entity.PiglinBrute
import org.bukkit.entity.Zoglin
import org.bukkit.entity.Warden
import org.bukkit.entity.Breeze
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.event.entity.EntityTargetEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.tsl.tSLplugins.service.DatabaseManager
import org.tsl.tSLplugins.SubCommandHandler
import org.tsl.tSLplugins.core.AbstractModule
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

/**
 * Peace 模块 - 伪和平模式
 * 
 * - peace: 玩家不会被怪物发现/锁定
 * - nospawn: 玩家附近禁止自然生成敌对生物
 */
class PeaceModule : AbstractModule() {

    override val id = "peace"
    override val configPath = "peace"

    private var scanIntervalTicks: Long = 20L
    private var noSpawnRadius: Int = 48

    private val peacePlayers: ConcurrentHashMap<UUID, Long> = ConcurrentHashMap()
    private val noSpawnPlayers: ConcurrentHashMap<UUID, Long> = ConcurrentHashMap()

    private lateinit var listener: PeaceModuleListener
    
    // 使用全局表前缀
    private val peaceTable: String get() = "${DatabaseManager.getTablePrefix()}peace_players"
    private val noSpawnTable: String get() = "${DatabaseManager.getTablePrefix()}peace_nospawn_players"

    override fun doEnable() {
        loadPeaceConfig()
        initDatabase()
        loadAllFromDatabase()
        startExpirationTask()
        listener = PeaceModuleListener(this)
        registerListener(listener)
    }

    override fun doDisable() {
        peacePlayers.clear()
        noSpawnPlayers.clear()
    }

    override fun doReload() {
        loadPeaceConfig()
    }

    override fun getCommandHandler(): SubCommandHandler = PeaceModuleCommand(this)
    override fun getDescription(): String = "伪和平模式"

    private fun loadPeaceConfig() {
        scanIntervalTicks = getConfigLong("scan-interval-ticks", 20L)
        noSpawnRadius = getConfigInt("nospawn-radius", 48)
    }

    private fun initDatabase() {
        // 创建和平模式表
        DatabaseManager.createTable("""
            CREATE TABLE IF NOT EXISTS $peaceTable (
                player_uuid TEXT PRIMARY KEY, player_name TEXT NOT NULL,
                expire_at INTEGER NOT NULL, created_at INTEGER NOT NULL, source TEXT DEFAULT 'command')
        """.trimIndent())
        
        // 创建禁怪模式表
        DatabaseManager.createTable("""
            CREATE TABLE IF NOT EXISTS $noSpawnTable (
                player_uuid TEXT PRIMARY KEY, player_name TEXT NOT NULL,
                expire_at INTEGER NOT NULL, created_at INTEGER NOT NULL, source TEXT DEFAULT 'command')
        """.trimIndent())
        
        // 添加索引优化过期查询
        DatabaseManager.createIndex("CREATE INDEX IF NOT EXISTS idx_peace_expire ON $peaceTable(expire_at)")
        DatabaseManager.createIndex("CREATE INDEX IF NOT EXISTS idx_nospawn_expire ON $noSpawnTable(expire_at)")
    }

    private fun loadAllFromDatabase() {
        CompletableFuture.runAsync({
            try {
                val now = System.currentTimeMillis()
                val conn = DatabaseManager.getConnection()
                
                // 加载和平模式玩家
                conn.prepareStatement("SELECT player_uuid, expire_at FROM $peaceTable").use { stmt ->
                    val rs = stmt.executeQuery()
                    while (rs.next()) {
                        val uuid = UUID.fromString(rs.getString("player_uuid"))
                        val expireAt = rs.getLong("expire_at")
                        if (expireAt > now) peacePlayers[uuid] = expireAt
                    }
                }
                
                // 加载禁怪模式玩家
                conn.prepareStatement("SELECT player_uuid, expire_at FROM $noSpawnTable").use { stmt ->
                    val rs = stmt.executeQuery()
                    while (rs.next()) {
                        val uuid = UUID.fromString(rs.getString("player_uuid"))
                        val expireAt = rs.getLong("expire_at")
                        if (expireAt > now) noSpawnPlayers[uuid] = expireAt
                    }
                }
            } catch (e: Exception) { logWarning("加载数据失败: ${e.message}") }
        }, DatabaseManager.getExecutor())
    }

    private fun startExpirationTask() {
        Bukkit.getGlobalRegionScheduler().runAtFixedRate(context.plugin, { _ ->
            val now = System.currentTimeMillis()
            peacePlayers.entries.removeIf { (uuid, expireAt) ->
                if (now >= expireAt) {
                    Bukkit.getPlayer(uuid)?.let { p ->
                        p.scheduler.run(context.plugin, { _ -> p.sendMessage(colorize(getModuleMessage("expired"))) }, null)
                    }
                    true
                } else false
            }
            noSpawnPlayers.entries.removeIf { (uuid, expireAt) ->
                if (now >= expireAt) {
                    Bukkit.getPlayer(uuid)?.let { p ->
                        p.scheduler.run(context.plugin, { _ -> p.sendMessage(colorize(getModuleMessage("nospawn_expired"))) }, null)
                    }
                    true
                } else false
            }
        }, scanIntervalTicks, scanIntervalTicks)
    }

    fun setPeace(player: Player, durationMs: Long, source: String = "command"): Boolean {
        val now = System.currentTimeMillis()
        val expireAt = now + durationMs
        peacePlayers[player.uniqueId] = expireAt
        CompletableFuture.runAsync({
            DatabaseManager.update("INSERT OR REPLACE INTO $peaceTable VALUES (?, ?, ?, ?, ?)") { stmt ->
                stmt.setString(1, player.uniqueId.toString())
                stmt.setString(2, player.name)
                stmt.setLong(3, expireAt)
                stmt.setLong(4, now)
                stmt.setString(5, source)
            }
        }, DatabaseManager.getExecutor())
        return true
    }

    fun setNoSpawn(player: Player, durationMs: Long, source: String = "command"): Boolean {
        val now = System.currentTimeMillis()
        val expireAt = now + durationMs
        noSpawnPlayers[player.uniqueId] = expireAt
        CompletableFuture.runAsync({
            DatabaseManager.update("INSERT OR REPLACE INTO $noSpawnTable VALUES (?, ?, ?, ?, ?)") { stmt ->
                stmt.setString(1, player.uniqueId.toString())
                stmt.setString(2, player.name)
                stmt.setLong(3, expireAt)
                stmt.setLong(4, now)
                stmt.setString(5, source)
            }
        }, DatabaseManager.getExecutor())
        return true
    }

    fun isPeaceful(uuid: UUID): Boolean = peacePlayers[uuid]?.let { System.currentTimeMillis() < it } ?: false
    fun hasNoSpawn(uuid: UUID): Boolean = noSpawnPlayers[uuid]?.let { System.currentTimeMillis() < it } ?: false
    fun getNoSpawnRadius(): Int = noSpawnRadius
    fun getNoSpawnPlayerUuids(): Set<UUID> = noSpawnPlayers.filterValues { it > System.currentTimeMillis() }.keys

    fun clearPeace(uuid: UUID): Boolean {
        if (!peacePlayers.containsKey(uuid)) return false
        peacePlayers.remove(uuid)
        CompletableFuture.runAsync({
            DatabaseManager.update("DELETE FROM $peaceTable WHERE player_uuid = ?") { stmt ->
                stmt.setString(1, uuid.toString())
            }
        }, DatabaseManager.getExecutor())
        return true
    }

    fun clearNoSpawn(uuid: UUID): Boolean {
        if (!noSpawnPlayers.containsKey(uuid)) return false
        noSpawnPlayers.remove(uuid)
        CompletableFuture.runAsync({
            DatabaseManager.update("DELETE FROM $noSpawnTable WHERE player_uuid = ?") { stmt ->
                stmt.setString(1, uuid.toString())
            }
        }, DatabaseManager.getExecutor())
        return true
    }

    fun listPeacePlayers(): List<PeaceEntry> {
        val now = System.currentTimeMillis()
        return peacePlayers.mapNotNull { (uuid, expireAt) ->
            if (expireAt > now) PeaceEntry(uuid, Bukkit.getOfflinePlayer(uuid).name ?: uuid.toString(), expireAt - now) else null
        }.sortedByDescending { it.remainingMs }
    }

    fun listNoSpawnPlayers(): List<PeaceEntry> {
        val now = System.currentTimeMillis()
        return noSpawnPlayers.mapNotNull { (uuid, expireAt) ->
            if (expireAt > now) PeaceEntry(uuid, Bukkit.getOfflinePlayer(uuid).name ?: uuid.toString(), expireAt - now) else null
        }.sortedByDescending { it.remainingMs }
    }

    fun parseDuration(input: String): Long? {
        val trimmed = input.trim().lowercase()
        trimmed.toLongOrNull()?.let { return it * 1000 }
        val regex = Regex("""^(\d+)([smhd])$""")
        val match = regex.matchEntire(trimmed) ?: return null
        val value = match.groupValues[1].toLongOrNull() ?: return null
        return when (match.groupValues[2]) {
            "s" -> value * 1000; "m" -> value * 60 * 1000
            "h" -> value * 3600 * 1000; "d" -> value * 86400 * 1000; else -> null
        }
    }

    fun formatDuration(ms: Long): String {
        val seconds = ms / 1000
        return when {
            seconds < 60 -> "${seconds}秒"
            seconds < 3600 -> "${seconds / 60}分${seconds % 60}秒"
            seconds < 86400 -> "${seconds / 3600}时${(seconds % 3600) / 60}分"
            else -> "${seconds / 86400}天${(seconds % 86400) / 3600}时"
        }
    }

    fun getModuleMessage(key: String, vararg replacements: Pair<String, String>): String = getMessage(key, *replacements)
    private fun colorize(text: String): String = text.replace("&", "§")
    data class PeaceEntry(val uuid: UUID, val playerName: String, val remainingMs: Long)
}

class PeaceModuleListener(private val module: PeaceModule) : Listener {
    @EventHandler(priority = EventPriority.HIGH)
    fun onEntityTarget(event: EntityTargetEvent) {
        if (!module.isEnabled()) return
        val target = event.target as? Player ?: return
        if (module.isPeaceful(target.uniqueId)) event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onCreatureSpawn(event: CreatureSpawnEvent) {
        if (!module.isEnabled()) return
        val reason = event.spawnReason
        if (reason != CreatureSpawnEvent.SpawnReason.NATURAL && reason != CreatureSpawnEvent.SpawnReason.SLIME_SPLIT) return

        val entity = event.entity
        val isHostile = entity is Monster || entity is Slime || entity is MagmaCube || entity is Phantom ||
            entity is Ghast || entity is Shulker || entity is Hoglin || entity is Piglin ||
            entity is PiglinBrute || entity is Zoglin || entity is Warden || entity is Breeze
        if (!isHostile) return

        val spawnLocation = event.location
        val radiusSquared = module.getNoSpawnRadius() * module.getNoSpawnRadius()
        for (uuid in module.getNoSpawnPlayerUuids()) {
            val player = Bukkit.getPlayer(uuid) ?: continue
            if (player.world != spawnLocation.world) continue
            if (player.location.distanceSquared(spawnLocation) <= radiusSquared) {
                event.isCancelled = true
                return
            }
        }
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        // 可选：提示玩家效果状态
    }
}

class PeaceModuleCommand(private val module: PeaceModule) : SubCommandHandler {
    override fun handle(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!module.isEnabled()) { sender.sendMessage(colorize(module.getModuleMessage("disabled"))); return true }
        if (args.isEmpty()) return showHelp(sender)
        return when (args[0].lowercase()) {
            "set" -> handleSet(sender, args)
            "list" -> handleList(sender, args)
            "clear" -> handleClear(sender, args)
            else -> showHelp(sender)
        }
    }

    private fun handleSet(sender: CommandSender, args: Array<out String>): Boolean {
        if (!sender.hasPermission("tsl.peace.set")) { sender.sendMessage("§c无权限"); return true }
        if (args.size < 3) { sender.sendMessage("§c用法: /tsl peace set <peace|nospawn> <时间> [玩家]"); return true }
        val mode = args[1].lowercase()
        if (mode != "peace" && mode != "nospawn") { sender.sendMessage("§c无效模式"); return true }
        val durationMs = module.parseDuration(args[2])
        if (durationMs == null || durationMs <= 0) { sender.sendMessage("§c无效时间"); return true }
        val target: Player = if (args.size >= 4) Bukkit.getPlayer(args[3]) ?: run { sender.sendMessage("§c玩家不在线"); return true }
        else if (sender is Player) sender else { sender.sendMessage("§c控制台必须指定玩家"); return true }
        val formattedTime = module.formatDuration(durationMs)
        if (mode == "peace") {
            module.setPeace(target, durationMs, sender.name)
            sender.sendMessage("§a已为 ${target.name} 设置和平模式 $formattedTime")
        } else {
            module.setNoSpawn(target, durationMs, sender.name)
            sender.sendMessage("§a已为 ${target.name} 设置禁怪模式 $formattedTime (半径: ${module.getNoSpawnRadius()})")
        }
        return true
    }

    private fun handleList(sender: CommandSender, args: Array<out String>): Boolean {
        if (!sender.hasPermission("tsl.peace.list")) { sender.sendMessage("§c无权限"); return true }
        val modeFilter = if (args.size >= 2) args[1].lowercase() else null
        if (modeFilter == null || modeFilter == "peace") {
            val entries = module.listPeacePlayers()
            if (entries.isNotEmpty()) {
                sender.sendMessage("§a===== 和平模式 =====")
                entries.forEach { sender.sendMessage("§f${it.playerName} §7- 剩余: §e${module.formatDuration(it.remainingMs)}") }
            }
        }
        if (modeFilter == null || modeFilter == "nospawn") {
            val entries = module.listNoSpawnPlayers()
            if (entries.isNotEmpty()) {
                sender.sendMessage("§b===== 禁怪模式 =====")
                entries.forEach { sender.sendMessage("§f${it.playerName} §7- 剩余: §e${module.formatDuration(it.remainingMs)}") }
            }
        }
        return true
    }

    private fun handleClear(sender: CommandSender, args: Array<out String>): Boolean {
        if (!sender.hasPermission("tsl.peace.clear")) { sender.sendMessage("§c无权限"); return true }
        if (args.size < 2) { sender.sendMessage("§c用法: /tsl peace clear <玩家|all> [peace|nospawn]"); return true }
        val targetName = args[1]
        val modeFilter = if (args.size >= 3) args[2].lowercase() else null
        if (targetName.equals("all", ignoreCase = true)) {
            sender.sendMessage("§a已清除所有模式")
            return true
        }
        @Suppress("DEPRECATION")
        val target = Bukkit.getOfflinePlayers().find { it.name?.equals(targetName, ignoreCase = true) == true }
        if (target == null) { sender.sendMessage("§c找不到玩家"); return true }
        if (modeFilter == null || modeFilter == "peace") module.clearPeace(target.uniqueId)
        if (modeFilter == null || modeFilter == "nospawn") module.clearNoSpawn(target.uniqueId)
        sender.sendMessage("§a已清除 ${target.name} 的模式")
        return true
    }

    private fun showHelp(sender: CommandSender): Boolean {
        sender.sendMessage("§a===== 伪和平模式 =====")
        sender.sendMessage("§e/tsl peace set <peace|nospawn> <时间> [玩家]")
        sender.sendMessage("§e/tsl peace list [peace|nospawn]")
        sender.sendMessage("§e/tsl peace clear <玩家|all> [peace|nospawn]")
        return true
    }

    override fun tabComplete(sender: CommandSender, command: Command, label: String, args: Array<out String>): List<String> {
        return when (args.size) {
            1 -> listOf("set", "list", "clear").filter { it.startsWith(args[0], ignoreCase = true) }
            2 -> when (args[0].lowercase()) {
                "set", "list" -> listOf("peace", "nospawn").filter { it.startsWith(args[1], ignoreCase = true) }
                "clear" -> (listOf("all") + module.listPeacePlayers().map { it.playerName }).filter { it.startsWith(args[1], ignoreCase = true) }
                else -> emptyList()
            }
            3 -> when (args[0].lowercase()) {
                "set" -> listOf("60", "5m", "1h", "1d").filter { it.startsWith(args[2], ignoreCase = true) }
                else -> emptyList()
            }
            4 -> if (args[0].lowercase() == "set") Bukkit.getOnlinePlayers().map { it.name }.filter { it.startsWith(args[3], ignoreCase = true) } else emptyList()
            else -> emptyList()
        }
    }

    override fun getDescription(): String = "伪和平模式管理"
    private fun colorize(text: String): String = text.replace("&", "§")
}
