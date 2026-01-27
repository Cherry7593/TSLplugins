package org.tsl.tSLplugins.modules.maintenance

import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.util.UUID

/**
 * 维护模式管理器
 * 负责管理维护模式的状态持久化和白名单
 */
class MaintenanceManager(private val plugin: JavaPlugin) {

    private val dataFile = File(plugin.dataFolder, "maintenance.yml")
    private lateinit var dataConfig: YamlConfiguration
    private var maintenanceEnabled = false
    private var featureEnabled = true
    private val whitelist = mutableMapOf<UUID, String>() // UUID -> 玩家名

    init {
        loadConfig()
        loadData()
    }

    /**
     * 加载配置
     */
    fun loadConfig() {
        featureEnabled = plugin.config.getBoolean("maintenance.enabled", true)
    }

    /**
     * 获取当前维护模式状态
     */
    fun isMaintenanceEnabled(): Boolean = maintenanceEnabled

    /**
     * 检查维护模式功能是否启用
     */
    fun isFeatureEnabled(): Boolean = featureEnabled

    /**
     * 设置维护模式状态
     */
    fun setMaintenanceEnabled(enabled: Boolean) {
        maintenanceEnabled = enabled
        saveData()
    }

    /**
     * 切换维护模式状态
     */
    fun toggleMaintenance(): Boolean {
        maintenanceEnabled = !maintenanceEnabled
        saveData()
        return maintenanceEnabled
    }

    /**
     * 检查玩家是否在白名单中
     */
    fun isWhitelisted(uuid: UUID): Boolean = whitelist.containsKey(uuid)

    /**
     * 添加玩家到白名单
     */
    fun addToWhitelist(uuid: UUID, name: String) {
        whitelist[uuid] = name
        saveData()
    }

    /**
     * 从白名单移除玩家
     */
    fun removeFromWhitelist(uuid: UUID): Boolean {
        val removed = whitelist.remove(uuid) != null
        if (removed) {
            saveData()
        }
        return removed
    }

    /**
     * 获取白名单玩家名称列表
     */
    fun getWhitelistNames(): List<String> {
        return whitelist.values.toList()
    }

    /**
     * 从文件加载维护模式数据
     */
    private fun loadData() {
        // 迁移旧格式文件
        migrateOldFiles()

        if (!dataFile.exists()) {
            dataConfig = YamlConfiguration()
            saveData()
            return
        }

        try {
            dataConfig = YamlConfiguration.loadConfiguration(dataFile)

            // 加载维护模式状态
            maintenanceEnabled = dataConfig.getBoolean("enabled", false)

            // 加载白名单
            whitelist.clear()
            val whitelistSection = dataConfig.getConfigurationSection("whitelist")
            if (whitelistSection != null) {
                whitelistSection.getKeys(false).forEach { uuidString ->
                    try {
                        val uuid = UUID.fromString(uuidString)
                        val name = whitelistSection.getString(uuidString) ?: "Unknown"
                        whitelist[uuid] = name
                    } catch (e: IllegalArgumentException) {
                        plugin.logger.warning("无效的白名单 UUID: $uuidString")
                    }
                }
            }

            plugin.logger.info("维护模式数据已加载 - 状态: $maintenanceEnabled, 白名单: ${whitelist.size} 个玩家")
        } catch (e: Exception) {
            plugin.logger.severe("无法加载维护模式数据: ${e.message}")
            dataConfig = YamlConfiguration()
        }
    }

    /**
     * 保存维护模式数据到文件
     */
    private fun saveData() {
        try {
            if (!plugin.dataFolder.exists()) {
                plugin.dataFolder.mkdirs()
            }

            dataConfig.set("enabled", maintenanceEnabled)

            // 清空旧的白名单
            dataConfig.set("whitelist", null)

            // 保存白名单
            whitelist.forEach { (uuid, name) ->
                dataConfig.set("whitelist.$uuid", name)
            }

            dataConfig.save(dataFile)
        } catch (e: Exception) {
            plugin.logger.severe("无法保存维护模式数据: ${e.message}")
        }
    }

    /**
     * 迁移旧格式文件到新格式
     */
    private fun migrateOldFiles() {
        val oldMaintenanceFile = File(plugin.dataFolder, "maintenance.dat")
        val oldWhitelistFile = File(plugin.dataFolder, "maintenance-whitelist.txt")

        var migrated = false

        // 迁移维护模式状态
        if (oldMaintenanceFile.exists()) {
            try {
                val enabled = oldMaintenanceFile.readText().trim().toBoolean()
                if (!dataFile.exists()) {
                    maintenanceEnabled = enabled
                    migrated = true
                }
                oldMaintenanceFile.delete()
                plugin.logger.info("已迁移旧的维护模式状态文件")
            } catch (e: Exception) {
                plugin.logger.warning("迁移旧维护模式状态文件失败: ${e.message}")
            }
        }

        // 迁移白名单
        if (oldWhitelistFile.exists()) {
            try {
                oldWhitelistFile.readLines().forEach { line ->
                    val trimmed = line.trim()
                    if (trimmed.isNotEmpty()) {
                        val parts = trimmed.split(":", limit = 2)
                        if (parts.size == 2) {
                            try {
                                val uuid = UUID.fromString(parts[0])
                                val name = parts[1]
                                whitelist[uuid] = name
                                migrated = true
                            } catch (e: IllegalArgumentException) {
                                plugin.logger.warning("跳过无效的白名单条目: $trimmed")
                            }
                        }
                    }
                }
                oldWhitelistFile.delete()
                plugin.logger.info("已迁移旧的白名单文件")
            } catch (e: Exception) {
                plugin.logger.warning("迁移旧白名单文件失败: ${e.message}")
            }
        }

        if (migrated) {
            saveData()
        }
    }

    /**
     * 获取配置文件
     */
    fun getConfig(): FileConfiguration = plugin.config
}
