package org.tsl.tSLplugins.PlayTime

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.tsl.tSLplugins.DatabaseManager
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 玩家在线时长管理器
 * 统计玩家当天的在线时长，支持跨日重置
 * 
 * 设计思路：
 * - 内存缓存会话开始时间和累计时长
 * - 定期自动保存到数据库
 * - 玩家退出时保存
 * - 每日自动重置（可配置时区）
 */
class PlayTimeManager(private val plugin: JavaPlugin) {

    private var enabled: Boolean = true
    private var saveIntervalTicks: Long = 6000L // 默认5分钟保存一次
    private var timezone: ZoneId = ZoneId.systemDefault()

    // 玩家会话开始时间（毫秒）
    private val sessionStartTime: ConcurrentHashMap<UUID, Long> = ConcurrentHashMap()
    
    // 玩家当天已累计的时长（秒）- 不包括当前会话
    private val dailyPlayTime: ConcurrentHashMap<UUID, Long> = ConcurrentHashMap()
    
    // 玩家上次记录的日期（用于检测跨日）
    private val lastRecordDate: ConcurrentHashMap<UUID, LocalDate> = ConcurrentHashMap()

    // 消息配置
    private val messages: MutableMap<String, String> = mutableMapOf()

    private var saveTaskId: Int = -1

    init {
        loadConfig()
        initDatabase()
    }

    /**
     * 加载配置
     */
    fun loadConfig() {
        val config = plugin.config

        enabled = config.getBoolean("playtime.enabled", true)
        saveIntervalTicks = config.getLong("playtime.save-interval-ticks", 6000L)
        
        val timezoneStr = config.getString("playtime.timezone", "Asia/Shanghai") ?: "Asia/Shanghai"
        timezone = try {
            ZoneId.of(timezoneStr)
        } catch (e: Exception) {
            plugin.logger.warning("[PlayTime] 无效的时区配置: $timezoneStr，使用系统默认时区")
            ZoneId.systemDefault()
        }

        // 读取消息配置
        val prefix = config.getString("playtime.messages.prefix", "&e[时长]&r ") ?: "&e[时长]&r "
        messages.clear()
        val messagesSection = config.getConfigurationSection("playtime.messages")
        if (messagesSection != null) {
            for (key in messagesSection.getKeys(false)) {
                if (key == "prefix") continue
                val rawMessage = messagesSection.getString(key) ?: ""
                messages[key] = rawMessage.replace("%prefix%", prefix)
            }
        }

        if (!enabled) {
            plugin.logger.info("[PlayTime] 模块已禁用")
            return
        }

        plugin.logger.info("[PlayTime] 已加载配置 - 保存间隔: ${saveIntervalTicks / 20}秒, 时区: $timezone")
    }

    /**
     * 初始化数据库表
     */
    private fun initDatabase() {
        if (!enabled) return

        try {
            DatabaseManager.getConnection().use { conn ->
                conn.createStatement().use { stmt ->
                    stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS playtime_daily (
                            player_uuid TEXT NOT NULL,
                            record_date TEXT NOT NULL,
                            play_seconds INTEGER NOT NULL DEFAULT 0,
                            last_update INTEGER NOT NULL,
                            PRIMARY KEY (player_uuid, record_date)
                        )
                    """.trimIndent())
                    
                    // 创建索引
                    stmt.executeUpdate("""
                        CREATE INDEX IF NOT EXISTS idx_playtime_uuid ON playtime_daily(player_uuid)
                    """.trimIndent())
                    stmt.executeUpdate("""
                        CREATE INDEX IF NOT EXISTS idx_playtime_date ON playtime_daily(record_date)
                    """.trimIndent())
                }
            }
            plugin.logger.info("[PlayTime] 数据库表初始化完成")
        } catch (e: Exception) {
            plugin.logger.warning("[PlayTime] 数据库初始化失败: ${e.message}")
        }
    }

    /**
     * 启动定时保存任务
     */
    fun startSaveTask() {
        if (!enabled) return
        
        stopSaveTask()
        
        // 使用 Folia 兼容的调度方式
        try {
            val scheduler = Bukkit.getAsyncScheduler()
            scheduler.runAtFixedRate(plugin, { _ ->
                saveAllOnlinePlayers()
            }, saveIntervalTicks * 50, saveIntervalTicks * 50, java.util.concurrent.TimeUnit.MILLISECONDS)
            plugin.logger.info("[PlayTime] 定时保存任务已启动 (Folia模式)")
        } catch (e: NoSuchMethodError) {
            // 回退到传统调度器
            saveTaskId = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, Runnable {
                saveAllOnlinePlayers()
            }, saveIntervalTicks, saveIntervalTicks).taskId
            plugin.logger.info("[PlayTime] 定时保存任务已启动 (传统模式)")
        }
    }

    /**
     * 停止定时保存任务
     */
    fun stopSaveTask() {
        if (saveTaskId != -1) {
            try {
                Bukkit.getScheduler().cancelTask(saveTaskId)
            } catch (_: Exception) {}
            saveTaskId = -1
        }
    }

    /**
     * 玩家加入时调用
     */
    fun onPlayerJoin(player: Player) {
        if (!enabled) return
        
        val uuid = player.uniqueId
        val now = System.currentTimeMillis()
        val today = LocalDate.now(timezone)
        
        // 设置会话开始时间
        sessionStartTime[uuid] = now
        
        // 从数据库加载今日数据
        loadPlayerData(uuid, today)
    }

    /**
     * 玩家退出时调用
     */
    fun onPlayerQuit(player: Player) {
        if (!enabled) return
        
        val uuid = player.uniqueId
        
        // 保存当前会话时长
        savePlayerData(uuid)
        
        // 清理内存缓存
        sessionStartTime.remove(uuid)
        dailyPlayTime.remove(uuid)
        lastRecordDate.remove(uuid)
    }

    /**
     * 获取玩家今日在线时长（秒）
     * 包括当前会话的实时时长
     */
    fun getTodayPlayTime(uuid: UUID): Long {
        if (!enabled) return 0L
        
        val today = LocalDate.now(timezone)
        val recordDate = lastRecordDate[uuid]
        
        // 检查是否跨日
        if (recordDate != null && recordDate != today) {
            // 跨日了，重置累计时长
            dailyPlayTime[uuid] = 0L
            lastRecordDate[uuid] = today
            // 重置会话开始时间为今天零点或当前时间
            sessionStartTime[uuid] = System.currentTimeMillis()
        }
        
        val accumulated = dailyPlayTime[uuid] ?: 0L
        val sessionStart = sessionStartTime[uuid]
        
        return if (sessionStart != null) {
            val currentSession = (System.currentTimeMillis() - sessionStart) / 1000
            accumulated + currentSession
        } else {
            accumulated
        }
    }

    /**
     * 获取玩家今日在线时长（格式化字符串）
     */
    fun getTodayPlayTimeFormatted(uuid: UUID): String {
        val seconds = getTodayPlayTime(uuid)
        return formatDuration(seconds)
    }

    /**
     * 检查玩家今日在线时长是否达到指定秒数
     */
    fun hasPlayedToday(uuid: UUID, requiredSeconds: Long): Boolean {
        return getTodayPlayTime(uuid) >= requiredSeconds
    }

    /**
     * 检查玩家今日在线时长是否达到指定时间字符串（如 "1h", "30m"）
     */
    fun hasPlayedToday(uuid: UUID, durationString: String): Boolean {
        val requiredSeconds = parseDuration(durationString)
        return hasPlayedToday(uuid, requiredSeconds)
    }

    /**
     * 从数据库加载玩家数据
     */
    private fun loadPlayerData(uuid: UUID, date: LocalDate) {
        DatabaseManager.getExecutor().execute {
            try {
                DatabaseManager.getConnection().use { conn ->
                    conn.prepareStatement(
                        "SELECT play_seconds FROM playtime_daily WHERE player_uuid = ? AND record_date = ?"
                    ).use { stmt ->
                        stmt.setString(1, uuid.toString())
                        stmt.setString(2, date.toString())
                        val rs = stmt.executeQuery()
                        
                        val seconds = if (rs.next()) rs.getLong("play_seconds") else 0L
                        
                        // 更新内存缓存
                        dailyPlayTime[uuid] = seconds
                        lastRecordDate[uuid] = date
                    }
                }
            } catch (e: Exception) {
                plugin.logger.warning("[PlayTime] 加载玩家数据失败 ${uuid}: ${e.message}")
            }
        }
    }

    /**
     * 保存玩家数据到数据库
     */
    private fun savePlayerData(uuid: UUID) {
        val today = LocalDate.now(timezone)
        val totalSeconds = getTodayPlayTime(uuid)
        val now = System.currentTimeMillis()
        
        // 更新内存中的累计时长（将当前会话计入）
        dailyPlayTime[uuid] = totalSeconds
        sessionStartTime[uuid] = now // 重置会话开始时间
        
        DatabaseManager.getExecutor().execute {
            try {
                DatabaseManager.getConnection().use { conn ->
                    conn.prepareStatement("""
                        INSERT INTO playtime_daily (player_uuid, record_date, play_seconds, last_update)
                        VALUES (?, ?, ?, ?)
                        ON CONFLICT(player_uuid, record_date) DO UPDATE SET
                            play_seconds = excluded.play_seconds,
                            last_update = excluded.last_update
                    """.trimIndent()).use { stmt ->
                        stmt.setString(1, uuid.toString())
                        stmt.setString(2, today.toString())
                        stmt.setLong(3, totalSeconds)
                        stmt.setLong(4, now)
                        stmt.executeUpdate()
                    }
                }
            } catch (e: Exception) {
                plugin.logger.warning("[PlayTime] 保存玩家数据失败 ${uuid}: ${e.message}")
            }
        }
    }

    /**
     * 保存所有在线玩家数据
     */
    private fun saveAllOnlinePlayers() {
        for (player in Bukkit.getOnlinePlayers()) {
            savePlayerData(player.uniqueId)
        }
    }

    /**
     * 获取排行榜数据（今日）
     */
    fun getTodayLeaderboard(limit: Int = 10): List<Pair<UUID, Long>> {
        val today = LocalDate.now(timezone)
        val result = mutableListOf<Pair<UUID, Long>>()
        
        try {
            DatabaseManager.getConnection().use { conn ->
                conn.prepareStatement(
                    "SELECT player_uuid, play_seconds FROM playtime_daily WHERE record_date = ? ORDER BY play_seconds DESC LIMIT ?"
                ).use { stmt ->
                    stmt.setString(1, today.toString())
                    stmt.setInt(2, limit)
                    val rs = stmt.executeQuery()
                    
                    while (rs.next()) {
                        val uuid = UUID.fromString(rs.getString("player_uuid"))
                        val seconds = rs.getLong("play_seconds")
                        
                        // 如果玩家在线，使用实时数据
                        val actualSeconds = if (sessionStartTime.containsKey(uuid)) {
                            getTodayPlayTime(uuid)
                        } else {
                            seconds
                        }
                        result.add(uuid to actualSeconds)
                    }
                }
            }
        } catch (e: Exception) {
            plugin.logger.warning("[PlayTime] 获取排行榜失败: ${e.message}")
        }
        
        // 按实时时长重新排序
        return result.sortedByDescending { it.second }.take(limit)
    }

    /**
     * 清理过期数据（保留指定天数）
     */
    fun cleanupOldData(keepDays: Int = 30) {
        val cutoffDate = LocalDate.now(timezone).minusDays(keepDays.toLong())
        
        DatabaseManager.getExecutor().execute {
            try {
                DatabaseManager.getConnection().use { conn ->
                    conn.prepareStatement(
                        "DELETE FROM playtime_daily WHERE record_date < ?"
                    ).use { stmt ->
                        stmt.setString(1, cutoffDate.toString())
                        val deleted = stmt.executeUpdate()
                        if (deleted > 0) {
                            plugin.logger.info("[PlayTime] 已清理 $deleted 条过期记录")
                        }
                    }
                }
            } catch (e: Exception) {
                plugin.logger.warning("[PlayTime] 清理过期数据失败: ${e.message}")
            }
        }
    }

    /**
     * 格式化时长（秒转为可读字符串）
     */
    fun formatDuration(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        
        return when {
            hours > 0 -> "${hours}时${minutes}分${secs}秒"
            minutes > 0 -> "${minutes}分${secs}秒"
            else -> "${secs}秒"
        }
    }

    /**
     * 解析时长字符串（如 "1h30m", "90m", "30s"）
     */
    fun parseDuration(duration: String): Long {
        var totalSeconds = 0L
        var currentNumber = StringBuilder()
        
        for (char in duration.lowercase()) {
            when {
                char.isDigit() || char == '.' -> currentNumber.append(char)
                char == 'h' -> {
                    totalSeconds += (currentNumber.toString().toDoubleOrNull() ?: 0.0 * 3600).toLong()
                    currentNumber = StringBuilder()
                }
                char == 'm' -> {
                    totalSeconds += (currentNumber.toString().toDoubleOrNull() ?: 0.0 * 60).toLong()
                    currentNumber = StringBuilder()
                }
                char == 's' -> {
                    totalSeconds += (currentNumber.toString().toDoubleOrNull() ?: 0.0).toLong()
                    currentNumber = StringBuilder()
                }
                char == 'd' -> {
                    totalSeconds += (currentNumber.toString().toDoubleOrNull() ?: 0.0 * 86400).toLong()
                    currentNumber = StringBuilder()
                }
            }
        }
        
        // 如果没有单位，默认为秒
        if (currentNumber.isNotEmpty()) {
            totalSeconds += currentNumber.toString().toLongOrNull() ?: 0L
        }
        
        return totalSeconds
    }

    /**
     * 获取消息
     */
    fun getMessage(key: String, vararg replacements: Pair<String, String>): String {
        var message = messages[key] ?: return ""
        for ((placeholder, value) in replacements) {
            message = message.replace(placeholder, value)
        }
        return message.replace("&", "§")
    }

    fun isEnabled(): Boolean = enabled

    /**
     * 关闭时调用
     */
    fun shutdown() {
        stopSaveTask()
        
        // 保存所有在线玩家数据
        for (player in Bukkit.getOnlinePlayers()) {
            val uuid = player.uniqueId
            val today = LocalDate.now(timezone)
            val totalSeconds = getTodayPlayTime(uuid)
            val now = System.currentTimeMillis()
            
            // 同步保存（关闭时）
            try {
                DatabaseManager.getConnection().use { conn ->
                    conn.prepareStatement("""
                        INSERT INTO playtime_daily (player_uuid, record_date, play_seconds, last_update)
                        VALUES (?, ?, ?, ?)
                        ON CONFLICT(player_uuid, record_date) DO UPDATE SET
                            play_seconds = excluded.play_seconds,
                            last_update = excluded.last_update
                    """.trimIndent()).use { stmt ->
                        stmt.setString(1, uuid.toString())
                        stmt.setString(2, today.toString())
                        stmt.setLong(3, totalSeconds)
                        stmt.setLong(4, now)
                        stmt.executeUpdate()
                    }
                }
            } catch (e: Exception) {
                plugin.logger.warning("[PlayTime] 关闭时保存数据失败 ${uuid}: ${e.message}")
            }
        }
        
        // 清理缓存
        sessionStartTime.clear()
        dailyPlayTime.clear()
        lastRecordDate.clear()
        
        plugin.logger.info("[PlayTime] 模块已关闭")
    }
}
