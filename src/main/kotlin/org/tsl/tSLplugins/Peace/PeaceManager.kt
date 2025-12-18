package org.tsl.tSLplugins.Peace

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.tsl.tSLplugins.DatabaseManager
import java.sql.Connection
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

/**
 * 伪和平模式管理器
 * 玩家在指定时间内不会被怪物发现/锁定
 */
class PeaceManager(private val plugin: JavaPlugin) {

    private var enabled: Boolean = true
    private var scanIntervalTicks: Long = 20L

    // 在线玩家的和平效果缓存：playerUuid -> expireAt
    private val peacePlayers: ConcurrentHashMap<UUID, Long> = ConcurrentHashMap()

    // 消息配置
    private val messages: MutableMap<String, String> = mutableMapOf()

    init {
        loadConfig()
        initDatabase()
        loadAllFromDatabase()
    }

    /**
     * 加载配置
     */
    fun loadConfig() {
        val config = plugin.config

        enabled = config.getBoolean("peace.enabled", true)
        scanIntervalTicks = config.getLong("peace.scan-interval-ticks", 20L)

        // 读取消息配置
        val prefix = config.getString("peace.messages.prefix", "&a[和平]&r ") ?: "&a[和平]&r "
        messages.clear()
        val messagesSection = config.getConfigurationSection("peace.messages")
        if (messagesSection != null) {
            for (key in messagesSection.getKeys(false)) {
                if (key == "prefix") continue
                val rawMessage = messagesSection.getString(key) ?: ""
                messages[key] = rawMessage.replace("%prefix%", prefix)
            }
        }

        if (!enabled) {
            plugin.logger.info("[Peace] 模块已禁用")
            return
        }

        plugin.logger.info("[Peace] 已加载配置")
    }

    /**
     * 初始化数据库表（同步执行，确保表存在后才能进行其他操作）
     */
    private fun initDatabase() {
        if (!enabled) return

        try {
            DatabaseManager.getConnection()?.use { conn ->
                conn.createStatement().use { stmt ->
                    stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS peace_players (
                            player_uuid TEXT PRIMARY KEY,
                            player_name TEXT NOT NULL,
                            expire_at INTEGER NOT NULL,
                            created_at INTEGER NOT NULL,
                            source TEXT DEFAULT 'command'
                        )
                    """.trimIndent())
                }
            }
            plugin.logger.info("[Peace] 数据库表初始化完成")
        } catch (e: Exception) {
            plugin.logger.severe("[Peace] 数据库初始化失败: ${e.message}")
        }
    }

    /**
     * 从数据库加载所有记录
     */
    private fun loadAllFromDatabase() {
        if (!enabled) return

        CompletableFuture.runAsync {
            try {
                DatabaseManager.getConnection()?.use { conn ->
                    conn.prepareStatement("SELECT player_uuid, expire_at FROM peace_players").use { stmt ->
                        val rs = stmt.executeQuery()
                        val now = System.currentTimeMillis()
                        while (rs.next()) {
                            val uuid = UUID.fromString(rs.getString("player_uuid"))
                            val expireAt = rs.getLong("expire_at")
                            if (expireAt > now) {
                                peacePlayers[uuid] = expireAt
                            }
                        }
                    }
                }
                plugin.logger.info("[Peace] 从数据库加载了 ${peacePlayers.size} 个和平玩家")
            } catch (e: Exception) {
                plugin.logger.severe("[Peace] 加载数据失败: ${e.message}")
            }
        }
    }

    /**
     * 启动过期扫描任务
     */
    fun startExpirationTask() {
        if (!enabled) return

        Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, { _ ->
            scanAndRemoveExpired()
        }, scanIntervalTicks, scanIntervalTicks)

        plugin.logger.info("[Peace] 过期扫描任务已启动, 间隔: ${scanIntervalTicks} ticks")
    }

    /**
     * 扫描并移除过期效果
     */
    private fun scanAndRemoveExpired() {
        val now = System.currentTimeMillis()
        val expiredUuids = mutableListOf<UUID>()

        peacePlayers.forEach { (uuid, expireAt) ->
            if (now >= expireAt) {
                expiredUuids.add(uuid)
            }
        }

        if (expiredUuids.isNotEmpty()) {
            expiredUuids.forEach { uuid ->
                peacePlayers.remove(uuid)

                // 通知玩家
                val player = Bukkit.getPlayer(uuid)
                if (player != null && player.isOnline) {
                    player.scheduler.run(plugin, { _ ->
                        player.sendMessage(colorize(getMessage("expired")))
                    }, null)
                }
            }

            // 异步删除数据库记录
            CompletableFuture.runAsync {
                try {
                    DatabaseManager.getConnection()?.use { conn ->
                        conn.prepareStatement("DELETE FROM peace_players WHERE expire_at <= ?").use { stmt ->
                            stmt.setLong(1, now)
                            stmt.executeUpdate()
                        }
                    }
                } catch (e: Exception) {
                    plugin.logger.warning("[Peace] 删除过期记录失败: ${e.message}")
                }
            }
        }
    }

    /**
     * 设置玩家的和平模式持续时间
     *
     * @param player 目标玩家
     * @param durationMs 持续时间（毫秒）
     * @param source 来源标识
     * @return 是否成功
     */
    fun setPeace(player: Player, durationMs: Long, source: String = "command"): Boolean {
        if (!enabled) return false

        val now = System.currentTimeMillis()
        val expireAt = now + durationMs

        // 更新缓存
        peacePlayers[player.uniqueId] = expireAt

        // 异步写入数据库
        CompletableFuture.runAsync {
            try {
                DatabaseManager.getConnection()?.use { conn ->
                    conn.prepareStatement("""
                        INSERT OR REPLACE INTO peace_players (player_uuid, player_name, expire_at, created_at, source)
                        VALUES (?, ?, ?, ?, ?)
                    """.trimIndent()).use { stmt ->
                        stmt.setString(1, player.uniqueId.toString())
                        stmt.setString(2, player.name)
                        stmt.setLong(3, expireAt)
                        stmt.setLong(4, now)
                        stmt.setString(5, source)
                        stmt.executeUpdate()
                    }
                }
            } catch (e: Exception) {
                plugin.logger.warning("[Peace] 保存数据失败: ${e.message}")
            }
        }

        plugin.logger.info("[Peace] ${player.name} 获得和平模式, 持续 ${durationMs / 1000} 秒")
        return true
    }

    /**
     * 检查玩家是否处于和平模式
     */
    fun isPeaceful(uuid: UUID): Boolean {
        val expireAt = peacePlayers[uuid] ?: return false
        return System.currentTimeMillis() < expireAt
    }

    /**
     * 检查玩家是否处于和平模式（Player 版本）
     */
    fun isPeaceful(player: Player): Boolean {
        return isPeaceful(player.uniqueId)
    }

    /**
     * 获取玩家的和平模式剩余时间（毫秒）
     */
    fun getRemainingTime(uuid: UUID): Long {
        val expireAt = peacePlayers[uuid] ?: return 0
        val remaining = expireAt - System.currentTimeMillis()
        return if (remaining > 0) remaining else 0
    }

    /**
     * 清除玩家的和平模式
     */
    fun clearPeace(uuid: UUID): Boolean {
        if (!peacePlayers.containsKey(uuid)) return false

        peacePlayers.remove(uuid)

        // 异步删除数据库记录
        CompletableFuture.runAsync {
            try {
                DatabaseManager.getConnection()?.use { conn ->
                    conn.prepareStatement("DELETE FROM peace_players WHERE player_uuid = ?").use { stmt ->
                        stmt.setString(1, uuid.toString())
                        stmt.executeUpdate()
                    }
                }
            } catch (e: Exception) {
                plugin.logger.warning("[Peace] 删除记录失败: ${e.message}")
            }
        }

        return true
    }

    /**
     * 获取所有和平模式玩家列表
     */
    fun listPeacePlayers(): List<PeaceEntry> {
        val now = System.currentTimeMillis()
        return peacePlayers.mapNotNull { (uuid, expireAt) ->
            if (expireAt > now) {
                val playerName = Bukkit.getOfflinePlayer(uuid).name ?: uuid.toString()
                PeaceEntry(uuid, playerName, expireAt - now)
            } else {
                null
            }
        }.sortedByDescending { it.remainingMs }
    }

    /**
     * 清除所有和平模式
     */
    fun clearAll(): Int {
        val count = peacePlayers.size
        peacePlayers.clear()

        // 异步清空数据库
        CompletableFuture.runAsync {
            try {
                DatabaseManager.getConnection()?.use { conn ->
                    conn.createStatement().use { stmt ->
                        stmt.executeUpdate("DELETE FROM peace_players")
                    }
                }
            } catch (e: Exception) {
                plugin.logger.warning("[Peace] 清空数据失败: ${e.message}")
            }
        }

        return count
    }

    /**
     * 玩家上线时加载效果
     */
    fun onPlayerJoin(player: Player) {
        if (!enabled) return

        // 检查是否有未过期的和平状态
        val expireAt = peacePlayers[player.uniqueId]
        if (expireAt != null && expireAt > System.currentTimeMillis()) {
            val remaining = (expireAt - System.currentTimeMillis()) / 1000
            player.scheduler.runDelayed(plugin, { _ ->
                player.sendMessage(colorize(getMessage("rejoin", "time" to formatDuration(remaining * 1000))))
            }, null, 20L)
        }
    }

    /**
     * 玩家下线时（不清理缓存，保持持久化）
     */
    fun onPlayerQuit(player: Player) {
        // 不清理缓存，让和平状态持续
    }

    /**
     * 关闭管理器
     */
    fun shutdown() {
        peacePlayers.clear()
        plugin.logger.info("[Peace] 管理器已关闭")
    }

    /**
     * 检查功能是否启用
     */
    fun isEnabled(): Boolean = enabled

    /**
     * 获取消息文本
     */
    fun getMessage(key: String, vararg replacements: Pair<String, String>): String {
        var message = messages[key] ?: key
        for ((placeholder, value) in replacements) {
            message = message.replace("{$placeholder}", value)
        }
        return message
    }

    /**
     * 解析持续时间字符串
     * 支持格式：10s, 5m, 2h, 1d, 纯数字（默认秒）
     */
    fun parseDuration(input: String): Long? {
        val trimmed = input.trim().lowercase()

        // 纯数字，默认为秒
        trimmed.toLongOrNull()?.let { return it * 1000 }

        // 带单位的格式
        val regex = Regex("""^(\d+)([smhd])$""")
        val match = regex.matchEntire(trimmed) ?: return null

        val value = match.groupValues[1].toLongOrNull() ?: return null
        val unit = match.groupValues[2]

        return when (unit) {
            "s" -> value * 1000
            "m" -> value * 60 * 1000
            "h" -> value * 60 * 60 * 1000
            "d" -> value * 24 * 60 * 60 * 1000
            else -> null
        }
    }

    /**
     * 格式化持续时间
     */
    fun formatDuration(ms: Long): String {
        val seconds = ms / 1000
        return when {
            seconds < 60 -> "${seconds}秒"
            seconds < 3600 -> "${seconds / 60}分${seconds % 60}秒"
            seconds < 86400 -> "${seconds / 3600}时${(seconds % 3600) / 60}分"
            else -> "${seconds / 86400}天${(seconds % 86400) / 3600}时"
        }
    }

    private fun colorize(text: String): String {
        return text.replace("&", "§")
    }

    /**
     * 和平玩家条目
     */
    data class PeaceEntry(
        val uuid: UUID,
        val playerName: String,
        val remainingMs: Long
    )
}
