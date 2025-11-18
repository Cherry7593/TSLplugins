package org.tsl.tSLplugins

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.io.InputStreamReader

/**
 * 配置文件更新管理器
 * 负责检测和更新配置文件版本，只添加新键值，不修改用户的现有配置
 */
class ConfigUpdateManager(private val plugin: JavaPlugin) {

    companion object {
        // 当前配置文件版本
        const val CURRENT_CONFIG_VERSION = 6
    }

    /**
     * 检查并更新配置文件
     * @return 是否进行了更新
     */
    fun checkAndUpdate(): Boolean {
        val configFile = File(plugin.dataFolder, "config.yml")

        // 如果配置文件不存在，直接保存默认配置
        if (!configFile.exists()) {
            plugin.saveDefaultConfig()
            plugin.logger.info("配置文件不存在，已创建默认配置文件（版本 $CURRENT_CONFIG_VERSION）")
            return true
        }

        // 读取现有配置
        val currentConfig = YamlConfiguration.loadConfiguration(configFile)
        val currentVersion = currentConfig.getInt("config-version", 0)

        // 版本一致，无需更新
        if (currentVersion == CURRENT_CONFIG_VERSION) {
            plugin.logger.info("配置文件版本正确（v$currentVersion），无需更新")
            return false
        }

        // 需要更新
        plugin.logger.info("检测到配置文件版本不同（当前: v$currentVersion, 最新: v$CURRENT_CONFIG_VERSION）")
        plugin.logger.info("开始更新配置文件，保持配置顺序并保留现有配置值...")

        // 读取默认配置（插件 JAR 内的配置）
        val defaultConfig = YamlConfiguration.loadConfiguration(
            InputStreamReader(plugin.getResource("config.yml")!!)
        )

        // 创建新的配置对象，按照默认配置的顺序构建
        val newConfig = YamlConfiguration()

        // 首先设置版本号（确保版本号始终在最上方）
        newConfig.set("config-version", CURRENT_CONFIG_VERSION)

        // 按照默认配置的顺序，合并配置值
        var addedCount = 0
        var updatedCount = 0

        // 获取默认配置的所有键（保持顺序）
        for (key in defaultConfig.getKeys(true)) {
            // 跳过版本号键（已经设置）
            if (key == "config-version") continue

            // 如果是配置节点（有子节点），跳过
            if (defaultConfig.isConfigurationSection(key)) continue

            if (currentConfig.contains(key)) {
                // 保留用户的旧配置值
                newConfig.set(key, currentConfig.get(key))
                updatedCount++
            } else {
                // 添加新的配置项
                newConfig.set(key, defaultConfig.get(key))
                addedCount++
                plugin.logger.info("  + 添加新配置项: $key")
            }
        }

        // 备份旧配置文件
        val backupFile = File(plugin.dataFolder, "config.yml.backup")
        try {
            configFile.copyTo(backupFile, overwrite = true)
            plugin.logger.info("已备份旧配置文件到: config.yml.backup")
        } catch (e: Exception) {
            plugin.logger.warning("备份配置文件失败: ${e.message}")
        }

        // 保存更新后的配置
        try {
            newConfig.save(configFile)
            plugin.logger.info("配置文件更新完成！")
            plugin.logger.info("  - 保留了 $updatedCount 个现有配置项")
            plugin.logger.info("  - 添加了 $addedCount 个新配置项")
            plugin.logger.info("  - 配置文件已更新到版本 $CURRENT_CONFIG_VERSION")
            return true
        } catch (e: Exception) {
            plugin.logger.severe("保存配置文件时出错: ${e.message}")
            // 尝试恢复备份
            if (backupFile.exists()) {
                try {
                    backupFile.copyTo(configFile, overwrite = true)
                    plugin.logger.info("已从备份恢复配置文件")
                } catch (ex: Exception) {
                    plugin.logger.severe("恢复备份失败: ${ex.message}")
                }
            }
            return false
        }
    }

    /**
     * 获取配置文件版本
     */
    fun getCurrentVersion(): Int {
        return plugin.config.getInt("config-version", 0)
    }
}

