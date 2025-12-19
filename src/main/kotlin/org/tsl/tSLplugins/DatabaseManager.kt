package org.tsl.tSLplugins

import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.util.concurrent.Executors
import java.util.concurrent.ExecutorService

/**
 * 全局数据库管理器
 * 统一管理所有模块的 SQLite 数据库连接
 */
object DatabaseManager {

    private var plugin: JavaPlugin? = null
    private var connection: Connection? = null
    private val connectionLock = Any()

    private var dbFile: File? = null
    private var tablePrefix: String = "tsl_"

    // 单线程执行器，确保数据库操作顺序执行
    private var executor: ExecutorService? = null

    /**
     * 初始化数据库管理器
     */
    fun init(plugin: JavaPlugin) {
        this.plugin = plugin

        val config = plugin.config
        tablePrefix = config.getString("database.table-prefix", "tsl_") ?: "tsl_"
        val dbPath = config.getString("database.sqlite.file", "data/tslplugins.db") ?: "data/tslplugins.db"

        dbFile = File(plugin.dataFolder, dbPath)
        dbFile?.parentFile?.mkdirs()

        // 创建执行器
        executor = Executors.newSingleThreadExecutor { r ->
            Thread(r, "TSL-Database").apply { isDaemon = true }
        }

        // 初始化连接并启用 WAL 模式
        getConnection().use { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute("PRAGMA journal_mode=WAL")
            }
        }

        plugin.logger.info("[Database] 全局数据库已初始化: ${dbFile?.absolutePath}")
    }

    /**
     * 获取数据库连接
     */
    fun getConnection(): Connection {
        synchronized(connectionLock) {
            val conn = connection
            if (conn != null && !conn.isClosed) {
                return conn
            }
            val file = dbFile ?: throw IllegalStateException("DatabaseManager not initialized")
            val newConn = DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}")
            connection = newConn
            return newConn
        }
    }

    /**
     * 获取表前缀
     */
    fun getTablePrefix(): String = tablePrefix

    /**
     * 检查是否已初始化
     */
    fun isInitialized(): Boolean {
        return executor != null && dbFile != null
    }

    /**
     * 获取异步执行器
     */
    fun getExecutor(): ExecutorService {
        return executor ?: throw IllegalStateException("DatabaseManager not initialized")
    }

    /**
     * 获取插件实例
     */
    fun getPlugin(): JavaPlugin {
        return plugin ?: throw IllegalStateException("DatabaseManager not initialized")
    }

    /**
     * 关闭数据库连接
     */
    fun shutdown() {
        try {
            executor?.shutdown()
            executor = null

            synchronized(connectionLock) {
                connection?.close()
                connection = null
            }

            plugin?.logger?.info("[Database] 全局数据库已关闭")
        } catch (e: Exception) {
            plugin?.logger?.warning("[Database] 关闭数据库失败: ${e.message}")
        }
    }

    /**
     * 执行建表语句
     */
    fun createTable(sql: String) {
        try {
            getConnection().createStatement().use { stmt ->
                stmt.execute(sql)
            }
        } catch (e: Exception) {
            plugin?.logger?.warning("[Database] 创建表失败: ${e.message}")
        }
    }

    /**
     * 执行创建索引语句
     */
    fun createIndex(sql: String) {
        try {
            getConnection().createStatement().use { stmt ->
                stmt.execute(sql)
            }
        } catch (e: Exception) {
            plugin?.logger?.warning("[Database] 创建索引失败: ${e.message}")
        }
    }
}

