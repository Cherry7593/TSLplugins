package org.tsl.tSLplugins.TimedAttribute

import org.bukkit.plugin.java.JavaPlugin
import org.tsl.tSLplugins.DatabaseManager
import java.util.UUID
import java.util.concurrent.CompletableFuture

/**
 * 计时效果存储接口
 */
interface TimedEffectStorage {
    /**
     * 加载指定玩家的所有效果
     */
    fun loadByPlayer(playerUuid: UUID): CompletableFuture<List<TimedAttributeEffect>>

    /**
     * 插入新效果
     */
    fun insert(effect: TimedAttributeEffect): CompletableFuture<Boolean>

    /**
     * 根据 modifierUuid 删除效果
     */
    fun deleteByModifier(modifierUuid: UUID): CompletableFuture<Boolean>

    /**
     * 删除指定玩家的所有效果
     */
    fun deleteByPlayer(playerUuid: UUID): CompletableFuture<Int>

    /**
     * 删除所有已过期的效果
     */
    fun deleteExpired(now: Long): CompletableFuture<Int>

    /**
     * 关闭存储连接
     */
    fun close()
}

/**
 * SQLite 实现的计时效果存储
 * 使用全局 DatabaseManager
 */
class SQLiteTimedEffectStorage(
    private val plugin: JavaPlugin
) : TimedEffectStorage {

    private val tableName = "${DatabaseManager.getTablePrefix()}timed_attribute_effects"

    init {
        // 创建表（新版本增加 effect_type, created_at, base_value 字段）
        DatabaseManager.createTable("""
            CREATE TABLE IF NOT EXISTS $tableName (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                player_uuid CHAR(36) NOT NULL,
                attribute VARCHAR(64) NOT NULL,
                modifier_uuid CHAR(36) NOT NULL UNIQUE,
                amount DOUBLE NOT NULL,
                effect_type VARCHAR(16) NOT NULL DEFAULT 'ADD',
                created_at BIGINT NOT NULL,
                expire_at BIGINT NOT NULL,
                base_value DOUBLE,
                source VARCHAR(64)
            )
        """.trimIndent())

        // 尝试添加新列（兼容旧数据库）
        tryAddColumn("effect_type", "VARCHAR(16) NOT NULL DEFAULT 'ADD'")
        tryAddColumn("created_at", "BIGINT NOT NULL DEFAULT 0")
        tryAddColumn("base_value", "DOUBLE")

        // 创建索引
        DatabaseManager.createIndex("CREATE INDEX IF NOT EXISTS idx_player_uuid ON $tableName(player_uuid)")
        DatabaseManager.createIndex("CREATE INDEX IF NOT EXISTS idx_expire_at ON $tableName(expire_at)")
        DatabaseManager.createIndex("CREATE INDEX IF NOT EXISTS idx_created_at ON $tableName(created_at)")

        plugin.logger.info("[TimedAttribute] 存储表已初始化")
    }

    private fun tryAddColumn(columnName: String, columnDef: String) {
        try {
            DatabaseManager.getConnection().prepareStatement(
                "ALTER TABLE $tableName ADD COLUMN $columnName $columnDef"
            ).use { it.executeUpdate() }
        } catch (e: Exception) {
            // 列已存在，忽略
        }
    }

    override fun loadByPlayer(playerUuid: UUID): CompletableFuture<List<TimedAttributeEffect>> {
        return CompletableFuture.supplyAsync({
            val effects = mutableListOf<TimedAttributeEffect>()
            try {
                DatabaseManager.getConnection().prepareStatement(
                    "SELECT * FROM $tableName WHERE player_uuid = ?"
                ).use { stmt ->
                    stmt.setString(1, playerUuid.toString())
                    stmt.executeQuery().use { rs ->
                        while (rs.next()) {
                            val effectTypeStr = try { rs.getString("effect_type") } catch (e: Exception) { "ADD" }
                            val createdAt = try { rs.getLong("created_at") } catch (e: Exception) { 0L }
                            val baseValue = try { rs.getDouble("base_value").takeIf { !rs.wasNull() } } catch (e: Exception) { null }
                            
                            effects.add(
                                TimedAttributeEffect(
                                    playerUuid = UUID.fromString(rs.getString("player_uuid")),
                                    attributeKey = rs.getString("attribute"),
                                    modifierUuid = UUID.fromString(rs.getString("modifier_uuid")),
                                    amount = rs.getDouble("amount"),
                                    effectType = try { EffectType.valueOf(effectTypeStr ?: "ADD") } catch (e: Exception) { EffectType.ADD },
                                    createdAt = createdAt,
                                    expireAt = rs.getLong("expire_at"),
                                    baseValue = baseValue,
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

    override fun insert(effect: TimedAttributeEffect): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync({
            try {
                DatabaseManager.getConnection().prepareStatement("""
                    INSERT INTO $tableName 
                    (player_uuid, attribute, modifier_uuid, amount, effect_type, created_at, expire_at, base_value, source)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()).use { stmt ->
                    stmt.setString(1, effect.playerUuid.toString())
                    stmt.setString(2, effect.attributeKey)
                    stmt.setString(3, effect.modifierUuid.toString())
                    stmt.setDouble(4, effect.amount)
                    stmt.setString(5, effect.effectType.name)
                    stmt.setLong(6, effect.createdAt)
                    stmt.setLong(7, effect.expireAt)
                    if (effect.baseValue != null) {
                        stmt.setDouble(8, effect.baseValue)
                    } else {
                        stmt.setNull(8, java.sql.Types.DOUBLE)
                    }
                    stmt.setString(9, effect.source)
                    stmt.executeUpdate() > 0
                }
            } catch (e: Exception) {
                plugin.logger.warning("[TimedAttribute] 插入效果失败: ${e.message}")
                false
            }
        }, DatabaseManager.getExecutor())
    }

    override fun deleteByModifier(modifierUuid: UUID): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync({
            try {
                DatabaseManager.getConnection().prepareStatement(
                    "DELETE FROM $tableName WHERE modifier_uuid = ?"
                ).use { stmt ->
                    stmt.setString(1, modifierUuid.toString())
                    stmt.executeUpdate() > 0
                }
            } catch (e: Exception) {
                plugin.logger.warning("[TimedAttribute] 删除效果失败: ${e.message}")
                false
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

    override fun deleteExpired(now: Long): CompletableFuture<Int> {
        return CompletableFuture.supplyAsync({
            try {
                DatabaseManager.getConnection().prepareStatement(
                    "DELETE FROM $tableName WHERE expire_at <= ?"
                ).use { stmt ->
                    stmt.setLong(1, now)
                    stmt.executeUpdate()
                }
            } catch (e: Exception) {
                plugin.logger.warning("[TimedAttribute] 删除过期效果失败: ${e.message}")
                0
            }
        }, DatabaseManager.getExecutor())
    }

    override fun close() {
        // 不再单独关闭连接，由 DatabaseManager 统一管理
        plugin.logger.info("[TimedAttribute] 存储已关闭")
    }
}

