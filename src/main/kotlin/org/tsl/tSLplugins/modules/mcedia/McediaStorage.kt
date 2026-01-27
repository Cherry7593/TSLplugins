package org.tsl.tSLplugins.modules.mcedia

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.plugin.java.JavaPlugin
import org.tsl.tSLplugins.service.DatabaseManager
import java.util.UUID
import java.util.concurrent.CompletableFuture

/**
 * 待处理操作类型
 */
enum class PendingOperationType {
    DELETE,     // 删除盔甲架
    UPDATE      // 更新盔甲架配置
}

/**
 * 待处理操作数据
 */
data class PendingOperation(
    val uuid: UUID,             // 播放器 UUID
    val operationType: PendingOperationType,
    val worldName: String,      // 世界名称
    val x: Double,              // 位置坐标
    val y: Double,
    val z: Double,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Mcedia 配置模板
 * 存储玩家保存的播放器配置模板
 */
data class McediaTemplate(
    val id: Int,                    // 模板 ID (1-7)
    val playerUUID: UUID,           // 玩家 UUID
    val name: String,               // 模板名称（来自盔甲架名称）
    val scale: Double,
    val volume: Double,
    val maxVolumeRange: Double,
    val hearingRange: Double,
    val offsetX: Double,
    val offsetY: Double,
    val offsetZ: Double,
    val looping: Boolean,
    val noDanmaku: Boolean,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Mcedia 播放器存储接口
 */
interface McediaStorage {
    /**
     * 加载所有播放器
     */
    fun loadAll(): CompletableFuture<List<McediaPlayer>>

    /**
     * 根据 UUID 加载播放器
     */
    fun loadByUuid(uuid: UUID): CompletableFuture<McediaPlayer?>

    /**
     * 根据名称加载播放器
     */
    fun loadByName(name: String): CompletableFuture<McediaPlayer?>

    /**
     * 插入或更新播放器
     */
    fun save(player: McediaPlayer): CompletableFuture<Boolean>

    /**
     * 删除播放器
     */
    fun delete(uuid: UUID): CompletableFuture<Boolean>

    /**
     * 添加待处理操作
     */
    fun addPendingOperation(operation: PendingOperation): CompletableFuture<Boolean>

    /**
     * 获取指定区块的待处理操作
     */
    fun getPendingOperations(worldName: String, chunkX: Int, chunkZ: Int): CompletableFuture<List<PendingOperation>>

    /**
     * 删除待处理操作
     */
    fun removePendingOperation(uuid: UUID): CompletableFuture<Boolean>

    /**
     * 获取玩家的所有模板
     */
    fun getTemplates(playerUUID: UUID): CompletableFuture<List<McediaTemplate>>

    /**
     * 保存模板
     */
    fun saveTemplate(template: McediaTemplate): CompletableFuture<Boolean>

    /**
     * 删除模板
     */
    fun deleteTemplate(playerUUID: UUID, templateId: Int): CompletableFuture<Boolean>

    /**
     * 获取玩家下一个可用的模板 ID (1-7)，如果满了返回 null
     */
    fun getNextTemplateId(playerUUID: UUID): CompletableFuture<Int?>

    /**
     * 关闭存储连接
     */
    fun close()
}

/**
 * SQLite 实现的 Mcedia 播放器存储
 * 使用全局 DatabaseManager
 */
class SQLiteMcediaStorage(
    private val plugin: JavaPlugin
) : McediaStorage {

    private val tableName = "${DatabaseManager.getTablePrefix()}mcedia_players"
    private val pendingTableName = "${DatabaseManager.getTablePrefix()}mcedia_pending"
    private val templateTableName = "${DatabaseManager.getTablePrefix()}mcedia_templates"

    init {
        // 创建播放器表
        DatabaseManager.createTable("""
            CREATE TABLE IF NOT EXISTS $tableName (
                uuid CHAR(36) PRIMARY KEY,
                name VARCHAR(64) NOT NULL,
                world VARCHAR(64) NOT NULL,
                x DOUBLE NOT NULL,
                y DOUBLE NOT NULL,
                z DOUBLE NOT NULL,
                yaw FLOAT NOT NULL DEFAULT 0,
                pitch FLOAT NOT NULL DEFAULT 0,
                video_url TEXT,
                start_time VARCHAR(16),
                scale DOUBLE NOT NULL DEFAULT 1.0,
                volume DOUBLE NOT NULL DEFAULT 1.0,
                max_volume_range DOUBLE NOT NULL DEFAULT 5.0,
                hearing_range DOUBLE NOT NULL DEFAULT 500.0,
                offset_x DOUBLE NOT NULL DEFAULT 0.0,
                offset_y DOUBLE NOT NULL DEFAULT 0.0,
                offset_z DOUBLE NOT NULL DEFAULT 0.0,
                looping BOOLEAN NOT NULL DEFAULT 0,
                no_danmaku BOOLEAN NOT NULL DEFAULT 0,
                created_by CHAR(36),
                created_at BIGINT NOT NULL
            )
        """.trimIndent())

        // 创建待处理操作表
        DatabaseManager.createTable("""
            CREATE TABLE IF NOT EXISTS $pendingTableName (
                uuid CHAR(36) PRIMARY KEY,
                operation_type VARCHAR(16) NOT NULL,
                world VARCHAR(64) NOT NULL,
                x DOUBLE NOT NULL,
                y DOUBLE NOT NULL,
                z DOUBLE NOT NULL,
                chunk_x INT NOT NULL,
                chunk_z INT NOT NULL,
                created_at BIGINT NOT NULL
            )
        """.trimIndent())

        // 创建模板表
        DatabaseManager.createTable("""
            CREATE TABLE IF NOT EXISTS $templateTableName (
                player_uuid CHAR(36) NOT NULL,
                template_id INT NOT NULL,
                name VARCHAR(64) NOT NULL,
                scale DOUBLE NOT NULL DEFAULT 1.0,
                volume DOUBLE NOT NULL DEFAULT 1.0,
                max_volume_range DOUBLE NOT NULL DEFAULT 5.0,
                hearing_range DOUBLE NOT NULL DEFAULT 500.0,
                offset_x DOUBLE NOT NULL DEFAULT 0.0,
                offset_y DOUBLE NOT NULL DEFAULT 0.0,
                offset_z DOUBLE NOT NULL DEFAULT 0.0,
                looping BOOLEAN NOT NULL DEFAULT 0,
                no_danmaku BOOLEAN NOT NULL DEFAULT 0,
                created_at BIGINT NOT NULL,
                PRIMARY KEY (player_uuid, template_id)
            )
        """.trimIndent())

        // 创建索引
        DatabaseManager.createIndex("CREATE INDEX IF NOT EXISTS idx_mcedia_name ON $tableName(name)")
        DatabaseManager.createIndex("CREATE INDEX IF NOT EXISTS idx_mcedia_world ON $tableName(world)")
        DatabaseManager.createIndex("CREATE INDEX IF NOT EXISTS idx_mcedia_pending_chunk ON $pendingTableName(world, chunk_x, chunk_z)")
        DatabaseManager.createIndex("CREATE INDEX IF NOT EXISTS idx_mcedia_template_player ON $templateTableName(player_uuid)")

        plugin.logger.info("[Mcedia] 存储表已初始化")
    }

    override fun loadAll(): CompletableFuture<List<McediaPlayer>> {
        return CompletableFuture.supplyAsync({
            val players = mutableListOf<McediaPlayer>()
            try {
                DatabaseManager.getConnection().prepareStatement("SELECT * FROM $tableName").use { stmt ->
                    stmt.executeQuery().use { rs ->
                        while (rs.next()) {
                            val player = resultSetToPlayer(rs)
                            if (player != null) {
                                players.add(player)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                plugin.logger.warning("[Mcedia] 加载所有播放器失败: ${e.message}")
            }
            players
        }, DatabaseManager.getExecutor())
    }

    override fun loadByUuid(uuid: UUID): CompletableFuture<McediaPlayer?> {
        return CompletableFuture.supplyAsync({
            try {
                DatabaseManager.getConnection().prepareStatement("SELECT * FROM $tableName WHERE uuid = ?").use { stmt ->
                    stmt.setString(1, uuid.toString())
                    stmt.executeQuery().use { rs ->
                        if (rs.next()) {
                            resultSetToPlayer(rs)
                        } else {
                            null
                        }
                    }
                }
            } catch (e: Exception) {
                plugin.logger.warning("[Mcedia] 加载播放器失败: ${e.message}")
                null
            }
        }, DatabaseManager.getExecutor())
    }

    override fun loadByName(name: String): CompletableFuture<McediaPlayer?> {
        return CompletableFuture.supplyAsync({
            try {
                DatabaseManager.getConnection().prepareStatement("SELECT * FROM $tableName WHERE name = ? COLLATE NOCASE").use { stmt ->
                    stmt.setString(1, name)
                    stmt.executeQuery().use { rs ->
                        if (rs.next()) {
                            resultSetToPlayer(rs)
                        } else {
                            null
                        }
                    }
                }
            } catch (e: Exception) {
                plugin.logger.warning("[Mcedia] 加载播放器失败: ${e.message}")
                null
            }
        }, DatabaseManager.getExecutor())
    }

    override fun save(player: McediaPlayer): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync({
            try {
                DatabaseManager.getConnection().prepareStatement("""
                    INSERT OR REPLACE INTO $tableName 
                    (uuid, name, world, x, y, z, yaw, pitch, video_url, start_time, 
                     scale, volume, max_volume_range, hearing_range, 
                     offset_x, offset_y, offset_z, looping, no_danmaku, created_by, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()).use { stmt ->
                    stmt.setString(1, player.uuid.toString())
                    stmt.setString(2, player.name)
                    stmt.setString(3, player.location.world?.name ?: "world")
                    stmt.setDouble(4, player.location.x)
                    stmt.setDouble(5, player.location.y)
                    stmt.setDouble(6, player.location.z)
                    stmt.setFloat(7, player.location.yaw)
                    stmt.setFloat(8, player.location.pitch)
                    stmt.setString(9, player.videoUrl)
                    stmt.setString(10, player.startTime)
                    stmt.setDouble(11, player.scale)
                    stmt.setDouble(12, player.volume)
                    stmt.setDouble(13, player.maxVolumeRange)
                    stmt.setDouble(14, player.hearingRange)
                    stmt.setDouble(15, player.offsetX)
                    stmt.setDouble(16, player.offsetY)
                    stmt.setDouble(17, player.offsetZ)
                    stmt.setBoolean(18, player.looping)
                    stmt.setBoolean(19, player.noDanmaku)
                    stmt.setString(20, player.createdBy.toString())
                    stmt.setLong(21, player.createdAt)
                    stmt.executeUpdate() > 0
                }
            } catch (e: Exception) {
                plugin.logger.warning("[Mcedia] 保存播放器失败: ${e.message}")
                false
            }
        }, DatabaseManager.getExecutor())
    }

    override fun delete(uuid: UUID): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync({
            try {
                DatabaseManager.getConnection().prepareStatement("DELETE FROM $tableName WHERE uuid = ?").use { stmt ->
                    stmt.setString(1, uuid.toString())
                    stmt.executeUpdate() > 0
                }
            } catch (e: Exception) {
                plugin.logger.warning("[Mcedia] 删除播放器失败: ${e.message}")
                false
            }
        }, DatabaseManager.getExecutor())
    }

    override fun addPendingOperation(operation: PendingOperation): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync({
            try {
                val chunkX = (operation.x / 16).toInt()
                val chunkZ = (operation.z / 16).toInt()
                DatabaseManager.getConnection().prepareStatement("""
                    INSERT OR REPLACE INTO $pendingTableName 
                    (uuid, operation_type, world, x, y, z, chunk_x, chunk_z, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()).use { stmt ->
                    stmt.setString(1, operation.uuid.toString())
                    stmt.setString(2, operation.operationType.name)
                    stmt.setString(3, operation.worldName)
                    stmt.setDouble(4, operation.x)
                    stmt.setDouble(5, operation.y)
                    stmt.setDouble(6, operation.z)
                    stmt.setInt(7, chunkX)
                    stmt.setInt(8, chunkZ)
                    stmt.setLong(9, operation.createdAt)
                    stmt.executeUpdate() > 0
                }
            } catch (e: Exception) {
                plugin.logger.warning("[Mcedia] 添加待处理操作失败: ${e.message}")
                false
            }
        }, DatabaseManager.getExecutor())
    }

    override fun getPendingOperations(worldName: String, chunkX: Int, chunkZ: Int): CompletableFuture<List<PendingOperation>> {
        return CompletableFuture.supplyAsync({
            val operations = mutableListOf<PendingOperation>()
            try {
                DatabaseManager.getConnection().prepareStatement(
                    "SELECT * FROM $pendingTableName WHERE world = ? AND chunk_x = ? AND chunk_z = ?"
                ).use { stmt ->
                    stmt.setString(1, worldName)
                    stmt.setInt(2, chunkX)
                    stmt.setInt(3, chunkZ)
                    stmt.executeQuery().use { rs ->
                        while (rs.next()) {
                            operations.add(PendingOperation(
                                uuid = UUID.fromString(rs.getString("uuid")),
                                operationType = PendingOperationType.valueOf(rs.getString("operation_type")),
                                worldName = rs.getString("world"),
                                x = rs.getDouble("x"),
                                y = rs.getDouble("y"),
                                z = rs.getDouble("z"),
                                createdAt = rs.getLong("created_at")
                            ))
                        }
                    }
                }
            } catch (e: Exception) {
                plugin.logger.warning("[Mcedia] 获取待处理操作失败: ${e.message}")
            }
            operations
        }, DatabaseManager.getExecutor())
    }

    override fun removePendingOperation(uuid: UUID): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync({
            try {
                DatabaseManager.getConnection().prepareStatement(
                    "DELETE FROM $pendingTableName WHERE uuid = ?"
                ).use { stmt ->
                    stmt.setString(1, uuid.toString())
                    stmt.executeUpdate() > 0
                }
            } catch (e: Exception) {
                plugin.logger.warning("[Mcedia] 删除待处理操作失败: ${e.message}")
                false
            }
        }, DatabaseManager.getExecutor())
    }

    override fun getTemplates(playerUUID: UUID): CompletableFuture<List<McediaTemplate>> {
        return CompletableFuture.supplyAsync({
            val templates = mutableListOf<McediaTemplate>()
            try {
                DatabaseManager.getConnection().prepareStatement(
                    "SELECT * FROM $templateTableName WHERE player_uuid = ? ORDER BY template_id"
                ).use { stmt ->
                    stmt.setString(1, playerUUID.toString())
                    stmt.executeQuery().use { rs ->
                        while (rs.next()) {
                            templates.add(McediaTemplate(
                                id = rs.getInt("template_id"),
                                playerUUID = UUID.fromString(rs.getString("player_uuid")),
                                name = rs.getString("name"),
                                scale = rs.getDouble("scale"),
                                volume = rs.getDouble("volume"),
                                maxVolumeRange = rs.getDouble("max_volume_range"),
                                hearingRange = rs.getDouble("hearing_range"),
                                offsetX = rs.getDouble("offset_x"),
                                offsetY = rs.getDouble("offset_y"),
                                offsetZ = rs.getDouble("offset_z"),
                                looping = rs.getBoolean("looping"),
                                noDanmaku = rs.getBoolean("no_danmaku"),
                                createdAt = rs.getLong("created_at")
                            ))
                        }
                    }
                }
            } catch (e: Exception) {
                plugin.logger.warning("[Mcedia] 获取模板失败: ${e.message}")
            }
            templates
        }, DatabaseManager.getExecutor())
    }

    override fun saveTemplate(template: McediaTemplate): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync({
            try {
                DatabaseManager.getConnection().prepareStatement("""
                    INSERT OR REPLACE INTO $templateTableName 
                    (player_uuid, template_id, name, scale, volume, max_volume_range, hearing_range,
                     offset_x, offset_y, offset_z, looping, no_danmaku, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()).use { stmt ->
                    stmt.setString(1, template.playerUUID.toString())
                    stmt.setInt(2, template.id)
                    stmt.setString(3, template.name)
                    stmt.setDouble(4, template.scale)
                    stmt.setDouble(5, template.volume)
                    stmt.setDouble(6, template.maxVolumeRange)
                    stmt.setDouble(7, template.hearingRange)
                    stmt.setDouble(8, template.offsetX)
                    stmt.setDouble(9, template.offsetY)
                    stmt.setDouble(10, template.offsetZ)
                    stmt.setBoolean(11, template.looping)
                    stmt.setBoolean(12, template.noDanmaku)
                    stmt.setLong(13, template.createdAt)
                    stmt.executeUpdate() > 0
                }
            } catch (e: Exception) {
                plugin.logger.warning("[Mcedia] 保存模板失败: ${e.message}")
                false
            }
        }, DatabaseManager.getExecutor())
    }

    override fun deleteTemplate(playerUUID: UUID, templateId: Int): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync({
            try {
                DatabaseManager.getConnection().prepareStatement(
                    "DELETE FROM $templateTableName WHERE player_uuid = ? AND template_id = ?"
                ).use { stmt ->
                    stmt.setString(1, playerUUID.toString())
                    stmt.setInt(2, templateId)
                    stmt.executeUpdate() > 0
                }
            } catch (e: Exception) {
                plugin.logger.warning("[Mcedia] 删除模板失败: ${e.message}")
                false
            }
        }, DatabaseManager.getExecutor())
    }

    override fun getNextTemplateId(playerUUID: UUID): CompletableFuture<Int?> {
        return CompletableFuture.supplyAsync({
            try {
                val usedIds = mutableSetOf<Int>()
                DatabaseManager.getConnection().prepareStatement(
                    "SELECT template_id FROM $templateTableName WHERE player_uuid = ?"
                ).use { stmt ->
                    stmt.setString(1, playerUUID.toString())
                    stmt.executeQuery().use { rs ->
                        while (rs.next()) {
                            usedIds.add(rs.getInt("template_id"))
                        }
                    }
                }
                // 找到第一个未使用的 ID (1-7)
                (1..7).firstOrNull { it !in usedIds }
            } catch (e: Exception) {
                plugin.logger.warning("[Mcedia] 获取下一个模板ID失败: ${e.message}")
                null
            }
        }, DatabaseManager.getExecutor())
    }

    override fun close() {
        // 不再单独关闭连接，由 DatabaseManager 统一管理
        plugin.logger.info("[Mcedia] 存储已关闭")
    }

    /**
     * 从 ResultSet 转换为 McediaPlayer
     */
    private fun resultSetToPlayer(rs: java.sql.ResultSet): McediaPlayer? {
        return try {
            val worldName = rs.getString("world")
            val world = Bukkit.getWorld(worldName)

            // 即使世界未加载，也创建 Location（世界可能稍后加载）
            val location = if (world != null) {
                Location(
                    world,
                    rs.getDouble("x"),
                    rs.getDouble("y"),
                    rs.getDouble("z"),
                    rs.getFloat("yaw"),
                    rs.getFloat("pitch")
                )
            } else {
                // 创建一个临时的 Location，世界名存在 McediaPlayer 中
                Location(
                    Bukkit.getWorlds().firstOrNull(),
                    rs.getDouble("x"),
                    rs.getDouble("y"),
                    rs.getDouble("z"),
                    rs.getFloat("yaw"),
                    rs.getFloat("pitch")
                )
            }

            McediaPlayer(
                uuid = UUID.fromString(rs.getString("uuid")),
                name = rs.getString("name"),
                location = location,
                videoUrl = rs.getString("video_url") ?: "",
                startTime = rs.getString("start_time") ?: "",
                scale = rs.getDouble("scale"),
                volume = rs.getDouble("volume"),
                maxVolumeRange = rs.getDouble("max_volume_range"),
                hearingRange = rs.getDouble("hearing_range"),
                offsetX = rs.getDouble("offset_x"),
                offsetY = rs.getDouble("offset_y"),
                offsetZ = rs.getDouble("offset_z"),
                looping = rs.getBoolean("looping"),
                noDanmaku = rs.getBoolean("no_danmaku"),
                createdBy = UUID.fromString(rs.getString("created_by")),
                createdAt = rs.getLong("created_at")
            )
        } catch (e: Exception) {
            plugin.logger.warning("[Mcedia] 解析播放器数据失败: ${e.message}")
            null
        }
    }
}

