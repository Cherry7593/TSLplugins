package org.tsl.tSLplugins.modules.playtime

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.tsl.tSLplugins.service.DatabaseManager
import org.tsl.tSLplugins.SubCommandHandler
import org.tsl.tSLplugins.core.AbstractModule
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * PlayTime 模块 - 玩家在线时长统计
 */
class PlayTimeModule : AbstractModule() {

    override val id = "playtime"
    override val configPath = "playtime"

    private var saveIntervalTicks: Long = 6000L
    private var timezone: ZoneId = ZoneId.systemDefault()

    private val sessionStartTime: ConcurrentHashMap<UUID, Long> = ConcurrentHashMap()
    private val dailyPlayTime: ConcurrentHashMap<UUID, Long> = ConcurrentHashMap()
    private val lastRecordDate: ConcurrentHashMap<UUID, LocalDate> = ConcurrentHashMap()

    private lateinit var listener: PlayTimeModuleListener
    
    // 使用全局表前缀
    private val tableName: String get() = "${DatabaseManager.getTablePrefix()}playtime_daily"

    override fun doEnable() {
        loadPlayTimeConfig()
        initDatabase()
        startSaveTask()
        listener = PlayTimeModuleListener(this)
        registerListener(listener)
    }

    override fun doDisable() {
        shutdown()
    }

    override fun doReload() {
        loadPlayTimeConfig()
    }

    override fun getCommandHandler(): SubCommandHandler = PlayTimeModuleCommand(this)
    override fun getDescription(): String = "在线时长统计"

    private fun loadPlayTimeConfig() {
        saveIntervalTicks = getConfigLong("save-interval-ticks", 6000L)
        val timezoneStr = getConfigString("timezone", "Asia/Shanghai")
        timezone = try { ZoneId.of(timezoneStr) } catch (e: Exception) { ZoneId.systemDefault() }
    }

    private fun initDatabase() {
        DatabaseManager.createTable("""
            CREATE TABLE IF NOT EXISTS $tableName (
                player_uuid TEXT NOT NULL, record_date TEXT NOT NULL, play_seconds INTEGER NOT NULL DEFAULT 0,
                last_update INTEGER NOT NULL, PRIMARY KEY (player_uuid, record_date))
        """.trimIndent())
        DatabaseManager.createIndex("CREATE INDEX IF NOT EXISTS idx_playtime_uuid ON $tableName(player_uuid)")
        DatabaseManager.createIndex("CREATE INDEX IF NOT EXISTS idx_playtime_date ON $tableName(record_date)")
    }

    private fun startSaveTask() {
        Bukkit.getAsyncScheduler().runAtFixedRate(context.plugin, { _ -> saveAllOnlinePlayers() },
            saveIntervalTicks * 50, saveIntervalTicks * 50, TimeUnit.MILLISECONDS)
    }

    fun onPlayerJoin(player: Player) {
        val uuid = player.uniqueId
        sessionStartTime[uuid] = System.currentTimeMillis()
        loadPlayerData(uuid, LocalDate.now(timezone))
    }

    fun onPlayerQuit(player: Player) {
        val uuid = player.uniqueId
        savePlayerData(uuid)
        sessionStartTime.remove(uuid)
        dailyPlayTime.remove(uuid)
        lastRecordDate.remove(uuid)
    }

    fun getTodayPlayTime(uuid: UUID): Long {
        val today = LocalDate.now(timezone)
        val recordDate = lastRecordDate[uuid]
        if (recordDate != null && recordDate != today) {
            dailyPlayTime[uuid] = 0L
            lastRecordDate[uuid] = today
            sessionStartTime[uuid] = System.currentTimeMillis()
        }
        val accumulated = dailyPlayTime[uuid] ?: 0L
        val sessionStart = sessionStartTime[uuid]
        return if (sessionStart != null) accumulated + (System.currentTimeMillis() - sessionStart) / 1000 else accumulated
    }

    fun getTodayPlayTimeFormatted(uuid: UUID): String = formatDuration(getTodayPlayTime(uuid))

    fun getTodayLeaderboard(limit: Int = 10): List<Pair<UUID, Long>> {
        val today = LocalDate.now(timezone)
        val result = DatabaseManager.query(
            "SELECT player_uuid, play_seconds FROM $tableName WHERE record_date = ? ORDER BY play_seconds DESC LIMIT ?",
            { stmt ->
                stmt.setString(1, today.toString())
                stmt.setInt(2, limit)
            }
        ) { rs ->
            val list = mutableListOf<Pair<UUID, Long>>()
            while (rs.next()) {
                val uuid = UUID.fromString(rs.getString("player_uuid"))
                val seconds = if (sessionStartTime.containsKey(uuid)) getTodayPlayTime(uuid) else rs.getLong("play_seconds")
                list.add(uuid to seconds)
            }
            list
        } ?: emptyList()
        return result.sortedByDescending { it.second }.take(limit)
    }

    private fun loadPlayerData(uuid: UUID, date: LocalDate) {
        DatabaseManager.getExecutor().execute {
            val seconds = DatabaseManager.query(
                "SELECT play_seconds FROM $tableName WHERE player_uuid = ? AND record_date = ?",
                { stmt ->
                    stmt.setString(1, uuid.toString())
                    stmt.setString(2, date.toString())
                }
            ) { rs ->
                if (rs.next()) rs.getLong("play_seconds") else 0L
            } ?: 0L
            dailyPlayTime[uuid] = seconds
            lastRecordDate[uuid] = date
        }
    }

    private fun savePlayerData(uuid: UUID) {
        val today = LocalDate.now(timezone)
        val totalSeconds = getTodayPlayTime(uuid)
        val now = System.currentTimeMillis()
        dailyPlayTime[uuid] = totalSeconds
        sessionStartTime[uuid] = now
        DatabaseManager.getExecutor().execute {
            DatabaseManager.update(
                "INSERT INTO $tableName (player_uuid, record_date, play_seconds, last_update) VALUES (?, ?, ?, ?) ON CONFLICT(player_uuid, record_date) DO UPDATE SET play_seconds = excluded.play_seconds, last_update = excluded.last_update"
            ) { stmt ->
                stmt.setString(1, uuid.toString())
                stmt.setString(2, today.toString())
                stmt.setLong(3, totalSeconds)
                stmt.setLong(4, now)
            }
        }
    }

    private fun saveAllOnlinePlayers() {
        Bukkit.getOnlinePlayers().forEach { savePlayerData(it.uniqueId) }
    }

    fun formatDuration(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return when { hours > 0 -> "${hours}时${minutes}分${secs}秒"; minutes > 0 -> "${minutes}分${secs}秒"; else -> "${secs}秒" }
    }

    fun getModuleMessage(key: String, vararg replacements: Pair<String, String>): String = getMessage(key, *replacements)

    private fun shutdown() {
        val today = LocalDate.now(timezone)
        val now = System.currentTimeMillis()
        for (player in Bukkit.getOnlinePlayers()) {
            val uuid = player.uniqueId
            val totalSeconds = getTodayPlayTime(uuid)
            DatabaseManager.update(
                "INSERT INTO $tableName (player_uuid, record_date, play_seconds, last_update) VALUES (?, ?, ?, ?) ON CONFLICT(player_uuid, record_date) DO UPDATE SET play_seconds = excluded.play_seconds, last_update = excluded.last_update"
            ) { stmt ->
                stmt.setString(1, uuid.toString())
                stmt.setString(2, today.toString())
                stmt.setLong(3, totalSeconds)
                stmt.setLong(4, now)
            }
        }
        sessionStartTime.clear()
        dailyPlayTime.clear()
        lastRecordDate.clear()
    }
}

class PlayTimeModuleListener(private val module: PlayTimeModule) : Listener {
    @EventHandler fun onPlayerJoin(event: PlayerJoinEvent) { if (module.isEnabled()) module.onPlayerJoin(event.player) }
    @EventHandler fun onPlayerQuit(event: PlayerQuitEvent) { if (module.isEnabled()) module.onPlayerQuit(event.player) }
}

class PlayTimeModuleCommand(private val module: PlayTimeModule) : SubCommandHandler {
    override fun handle(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!module.isEnabled()) { sender.sendMessage("§c在线时长模块未启用"); return true }
        when { args.isEmpty() -> handleSelf(sender); args[0].equals("check", true) -> handleCheck(sender, args)
            args[0].equals("top", true) -> handleTop(sender, args); else -> showHelp(sender) }
        return true
    }

    private fun handleSelf(sender: CommandSender) {
        if (sender !is Player) { sender.sendMessage("§c该命令只能由玩家执行"); return }
        val playTime = module.getTodayPlayTimeFormatted(sender.uniqueId)
        sender.sendMessage(module.getModuleMessage("self", "%player%" to sender.name, "%time%" to playTime).ifEmpty { "§e你今日的在线时长: §a$playTime" })
    }

    private fun handleCheck(sender: CommandSender, args: Array<out String>) {
        if (!sender.hasPermission("tsl.playtime.check")) { sender.sendMessage("§c无权限"); return }
        if (args.size < 2) { sender.sendMessage("§c用法: /tsl playtime check <玩家>"); return }
        val target = Bukkit.getOfflinePlayer(args[1])
        if (!target.hasPlayedBefore() && !target.isOnline) { sender.sendMessage("§c玩家不存在"); return }
        val playTime = module.getTodayPlayTimeFormatted(target.uniqueId)
        sender.sendMessage(module.getModuleMessage("check", "%player%" to (target.name ?: args[1]), "%time%" to playTime).ifEmpty { "§e${target.name} 今日的在线时长: §a$playTime" })
    }

    private fun handleTop(sender: CommandSender, args: Array<out String>) {
        if (!sender.hasPermission("tsl.playtime.top")) { sender.sendMessage("§c无权限"); return }
        val limit = if (args.size > 1) args[1].toIntOrNull()?.coerceIn(1, 20) ?: 10 else 10
        val leaderboard = module.getTodayLeaderboard(limit)
        if (leaderboard.isEmpty()) { sender.sendMessage("§e今日暂无在线时长数据"); return }
        sender.sendMessage("§6========== 今日在线时长排行榜 ==========")
        leaderboard.forEachIndexed { index, (uuid, seconds) ->
            val playerName = Bukkit.getOfflinePlayer(uuid).name ?: "Unknown"
            sender.sendMessage("§e${index + 1}. §a$playerName §7- §f${module.formatDuration(seconds)}")
        }
    }

    private fun showHelp(sender: CommandSender) {
        sender.sendMessage("§6========== 在线时长命令帮助 ==========")
        sender.sendMessage("§e/tsl playtime §7- 查看自己今日在线时长")
        sender.sendMessage("§e/tsl playtime check <玩家> §7- 查看指定玩家在线时长")
        sender.sendMessage("§e/tsl playtime top [数量] §7- 查看今日排行榜")
    }

    override fun tabComplete(sender: CommandSender, command: Command, label: String, args: Array<out String>): List<String> {
        return when (args.size) {
            1 -> listOf("check", "top", "help").filter { it.startsWith(args[0], ignoreCase = true) }
            2 -> when (args[0].lowercase()) { "check" -> Bukkit.getOnlinePlayers().map { it.name }.filter { it.startsWith(args[1], ignoreCase = true) }; "top" -> listOf("5", "10", "20").filter { it.startsWith(args[1]) }; else -> emptyList() }
            else -> emptyList()
        }
    }

    override fun getDescription(): String = "在线时长统计"
}
