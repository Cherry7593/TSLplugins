package org.tsl.tSLplugins.TimedAttribute

import org.bukkit.plugin.java.JavaPlugin
import org.tsl.tSLplugins.DatabaseManager
import java.util.UUID
import java.util.concurrent.CompletableFuture

/**
 * 计时效果存储接口（堆栈版）
 */
interface TimedEffectStorage {
    /**
     * 加载指定玩家的所有效果
     */
    fun loadByPlayer(playerUuid: UUID): CompletableFuture<List<TimedAttributeEffect>>

    /**
     * 保存效果（插入或更新）
     */
    fun save(effect: TimedAttributeEffect): CompletableFuture<Boolean>

    /**
     * 批量保存效果
     */
    fun saveAll(effects: List<TimedAttributeEffect>): CompletableFuture<Boolean>

    /**
     * 根据 effectId 删除效果
     */
    fun deleteByEffectId(effectId: UUID): CompletableFuture<Boolean>

    /**
     * 删除指定玩家指定属性的所有效果
     */
    fun deleteByPlayerAttribute(playerUuid: UUID, attributeKey: String): CompletableFuture<Int>

    /**
     * 删除指定玩家的所有效果
     */
    fun deleteByPlayer(playerUuid: UUID): CompletableFuture<Int>

    /**
     * 关闭存储连接
     */
    fun close()
}

/**
 * SQLite 实现的计时效果存储（堆栈版）
 * 使用全局 DatabaseManager
 * 
 * 数据模型：每个玩家每个属性可以有多个效果（堆栈）
 */
class SQLiteTimedEffectStorage(
    private val plugin: JavaPlugin
) : TimedEffectStorage {

    private val tableName = "${DatabaseManager.getTablePrefix()}timed_attr_v3"

    init {
        // 创建新表（堆栈结构）
        DatabaseManager.createTable("""
            CREATE TABLE IF NOT EXISTS $tableName (
                effect_id CHAR(36) PRIMARY KEY,
                player_uuid CHAR(36) NOT NULL,
                attribute VARCHAR(64) NOT NULL,
                target_value DOUBLE NOT NULL,
                original_value DOUBLE NOT NULL,
                remaining_ms BIGINT NOT NULL,
                stack_index INT NOT NULL,
                is_paused INT NOT NULL DEFAULT 0,
                last_tick_at BIGINT NOT NULL,
                created_at BIGINT NOT NULL,
                source VARCHAR(64)
            )
        """.trimIndent())

        // 创建索引
        DatabaseManager.createIndex("CREATE INDEX IF NOT EXISTS idx_v3_player ON $tableName(player_uuid)")
        DatabaseManager.createIndex("CREATE INDEX IF NOT EXISTS idx_v3_player_attr ON $tableName(player_uuid, attribute)")

        plugin.logger.info("[TimedAttribute] 存储表已初始化 (v3 堆栈版)")
    }

    override fun loadByPlayer(playerUuid: UUID): CompletableFuture<List<TimedAttributeEffect>> {
        return CompletableFuture.supplyAsync({
            val effects = mutableListOf<TimedAttributeEffect>()
            try {
                DatabaseManager.getConnection().prepareStatement(
                    "SELECT * FROM $tableName WHERE player_uuid = ? ORDER BY attribute, stack_index"
                ).use { stmt ->
                    stmt.setString(1, playerUuid.toString())
                    stmt.executeQuery().use { rs ->
                        while (rs.next()) {
                            effects.add(
                                TimedAttributeEffect(
                                    playerUuid = UUID.fromString(rs.getString("player_uuid")),
                                    attributeKey = rs.getString("attribute"),
                                    effectId = UUID.fromString(rs.getString("effect_id")),
                                    targetValue = rs.getDouble("target_value"),
                                    capturedValue = rs.getDouble("original_value"),
                                    remainingMs = rs.getLong("remaining_ms"),
                                    stackIndex = rs.getInt("stack_index"),
                                    isPaused = rs.getInt("is_paused") == 1,
                                    lastTickAt = rs.getLong("last_tick_at"),
                                    createdAt = rs.getLong("created_at"),
                                    source = rs.getString("source")
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                plugin.logger.warning("[TimedAttribute] 加载玩家效果失败: ${e.message}")
            }
            effects
        }, DatabaseManager.getExecutor())
    }

    override fun save(effect: TimedAttributeEffect): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync({
            try {
                DatabaseManager.getConnection().prepareStatement("""
                    INSERT OR REPLACE INTO $tableName 
                    (effect_id, player_uuid, attribute, target_value, original_value, remaining_ms, stack_index, is_paused, last_tick_at, created_at, source)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()).use { stmt ->
                    stmt.setString(1, effect.effectId.toString())
                    stmt.setString(2, effect.playerUuid.toString())
                    stmt.setString(3, effect.attributeKey)
                    stmt.setDouble(4, effect.targetValue)
                    stmt.setDouble(5, effect.capturedValue)
                    stmt.setLong(6, effect.remainingMs)
                    stmt.setInt(7, effect.stackIndex)
                    stmt.setInt(8, if (effect.isPaused) 1 else 0)
                    stmt.setLong(9, effect.lastTickAt)
                    stmt.setLong(10, effect.createdAt)
                    stmt.setString(11, effect.source)
                    stmt.executeUpdate() > 0
                }
            } catch (e: Exception) {
                plugin.logger.warning("[TimedAttribute] 保存效果失败: ${e.message}")
                false
            }
        }, DatabaseManager.getExecutor())
    }

    override fun saveAll(effects: List<TimedAttributeEffect>): CompletableFuture<Boolean> {
        if (effects.isEmpty()) return CompletableFuture.completedFuture(true)
        
        return CompletableFuture.supplyAsync({
            try {
                val conn = DatabaseManager.getConnection()
                conn.autoCommit = false
                try {
                    conn.prepareStatement("""
                        INSERT OR REPLACE INTO $tableName 
                        (effect_id, player_uuid, attribute, target_value, original_value, remaining_ms, stack_index, is_paused, last_tick_at, created_at, source)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent()).use { stmt ->
                        for (effect in effects) {
                            stmt.setString(1, effect.effectId.toString())
                            stmt.setString(2, effect.playerUuid.toString())
                            stmt.setString(3, effect.attributeKey)
                            stmt.setDouble(4, effect.targetValue)
                            stmt.setDouble(5, effect.capturedValue)
                            stmt.setLong(6, effect.remainingMs)
                            stmt.setInt(7, effect.stackIndex)
                            stmt.setInt(8, if (effect.isPaused) 1 else 0)
                            stmt.setLong(9, effect.lastTickAt)
                            stmt.setLong(10, effect.createdAt)
                            stmt.setString(11, effect.source)
                            stmt.addBatch()
                        }
                        stmt.executeBatch()
                    }
                    conn.commit()
                    true
                } catch (e: Exception) {
                    conn.rollback()
                    throw e
                } finally {
                    conn.autoCommit = true
                }
            } catch (e: Exception) {
                plugin.logger.warning("[TimedAttribute] 批量保存效果失败: ${e.message}")
                false
            }
        }, DatabaseManager.getExecutor())
    }

    override fun deleteByEffectId(effectId: UUID): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync({
            try {
                DatabaseManager.getConnection().prepareStatement(
                    "DELETE FROM $tableName WHERE effect_id = ?"
                ).use { stmt ->
                    stmt.setString(1, effectId.toString())
                    stmt.executeUpdate() > 0
                }
            } catch (e: Exception) {
                plugin.logger.warning("[TimedAttribute] 删除效果失败: ${e.message}")
                false
            }
        }, DatabaseManager.getExecutor())
    }

    override fun deleteByPlayerAttribute(playerUuid: UUID, attributeKey: String): CompletableFuture<Int> {
        return CompletableFuture.supplyAsync({
            try {
                DatabaseManager.getConnection().prepareStatement(
                    "DELETE FROM $tableName WHERE player_uuid = ? AND attribute = ?"
                ).use { stmt ->
                    stmt.setString(1, playerUuid.toString())
                    stmt.setString(2, attributeKey)
                    stmt.executeUpdate()
                }
            } catch (e: Exception) {
                plugin.logger.warning("[TimedAttribute] 删除效果失败: ${e.message}")
                0
            }
        }, DatabaseManager.getExecutor())
    }

    override fun deleteByPlayer(playerUuid: UUID): CompletableFuture<Int> {
        return CompletableFuture.supplyAsync({
            try {
                DatabaseManager.getConnection().prepareStatement(
                    "DELETE FROM $tableName WHERE player_uuid = ?"
                ).use { stmt ->
                    stmt.setString(1, playerUuid.toString())
                    stmt.executeUpdate()
                }
            } catch (e: Exception) {
                plugin.logger.warning("[TimedAttribute] 删除玩家效果失败: ${e.message}")
                0
            }
        }, DatabaseManager.getExecutor())
    }

    override fun close() {
        plugin.logger.info("[TimedAttribute] 存储已关闭")
    }
}

