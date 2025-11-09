package org.tsl.tSLplugins.Maintenance

import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.util.UUID

/**
 * 维护模式管理器
 * 负责管理维护模式的状态持久化和白名单
 */
class MaintenanceManager(private val plugin: JavaPlugin) {

    private val maintenanceFile = File(plugin.dataFolder, "maintenance.dat")
    private val whitelistFile = File(plugin.dataFolder, "maintenance-whitelist.txt")
    private var maintenanceEnabled = false
    private val whitelist = mutableMapOf<UUID, String>() // UUID -> 玩家名

    init {
        loadMaintenanceState()
        loadWhitelist()
    }

    /**
     * 获取当前维护模式状态
     */
    fun isMaintenanceEnabled(): Boolean = maintenanceEnabled

    /**
     * 设置维护模式状态
     */
    fun setMaintenanceEnabled(enabled: Boolean) {
        maintenanceEnabled = enabled
        saveMaintenanceState()
    }

    /**
     * 切换维护模式状态
     */
    fun toggleMaintenance(): Boolean {
        maintenanceEnabled = !maintenanceEnabled
        saveMaintenanceState()
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
        saveWhitelist()
    }

    /**
     * 从白名单移除玩家
     */
    fun removeFromWhitelist(uuid: UUID): Boolean {
        val removed = whitelist.remove(uuid) != null
        if (removed) {
            saveWhitelist()
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
     * 从文件加载维护模式状态
     */
    private fun loadMaintenanceState() {
        if (maintenanceFile.exists()) {
            try {
                maintenanceEnabled = maintenanceFile.readText().trim().toBoolean()
                plugin.logger.info("维护模式状态已加载: $maintenanceEnabled")
            } catch (e: Exception) {
                plugin.logger.warning("无法读取维护模式状态文件: ${e.message}")
                maintenanceEnabled = false
            }
        }
    }

    /**
     * 保存维护模式状态到文件
     */
    private fun saveMaintenanceState() {
        try {
            if (!plugin.dataFolder.exists()) {
                plugin.dataFolder.mkdirs()
            }
            maintenanceFile.writeText(maintenanceEnabled.toString())
        } catch (e: Exception) {
            plugin.logger.severe("无法保存维护模式状态: ${e.message}")
        }
    }

    /**
     * 从文件加载白名单
     * 格式: UUID:玩家名
     */
    private fun loadWhitelist() {
        whitelist.clear()
        if (whitelistFile.exists()) {
            try {
                whitelistFile.readLines().forEach { line ->
                    val trimmed = line.trim()
                    if (trimmed.isNotEmpty()) {
                        val parts = trimmed.split(":", limit = 2)
                        if (parts.size == 2) {
                            try {
                                val uuid = UUID.fromString(parts[0])
                                val name = parts[1]
                                whitelist[uuid] = name
                            } catch (e: IllegalArgumentException) {
                                plugin.logger.warning("无效的白名单条目: $trimmed")
                            }
                        }
                    }
                }
                plugin.logger.info("维护模式白名单已加载: ${whitelist.size} 个玩家")
            } catch (e: Exception) {
                plugin.logger.warning("无法读取白名单文件: ${e.message}")
            }
        }
    }

    /**
     * 保存白名单到文件
     * 格式: UUID:玩家名
     */
    private fun saveWhitelist() {
        try {
            if (!plugin.dataFolder.exists()) {
                plugin.dataFolder.mkdirs()
            }
            val content = whitelist.entries.joinToString("\n") { "${it.key}:${it.value}" }
            whitelistFile.writeText(content)
        } catch (e: Exception) {
            plugin.logger.severe("无法保存白名单文件: ${e.message}")
        }
    }

    /**
     * 获取配置文件
     */
    fun getConfig(): FileConfiguration = plugin.config
}

