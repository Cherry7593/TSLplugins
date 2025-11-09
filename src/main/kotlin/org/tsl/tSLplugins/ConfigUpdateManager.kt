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
        const val CURRENT_CONFIG_VERSION = 2
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
        plugin.logger.info("开始更新配置文件，只添加新配置项，不修改现有配置...")

        // 读取默认配置（插件 JAR 内的配置）
        val defaultConfig = YamlConfiguration.loadConfiguration(
            InputStreamReader(plugin.getResource("config.yml")!!)
        )

        // 更新版本号
        currentConfig.set("config-version", CURRENT_CONFIG_VERSION)

        // 合并配置：只添加新键，不修改已存在的键
        var addedCount = 0
        for (key in defaultConfig.getKeys(true)) {
            // 跳过版本号键（已经设置）
            if (key == "config-version") continue

            // 只添加不存在的键
            if (!currentConfig.contains(key)) {
                currentConfig.set(key, defaultConfig.get(key))
                addedCount++
                plugin.logger.info("  + 添加新配置项: $key")
            }
        }

        // 保存更新后的配置
        try {
            currentConfig.save(configFile)
            plugin.logger.info("配置文件更新完成！添加了 $addedCount 个新配置项")
            plugin.logger.info("配置文件已更新到版本 $CURRENT_CONFIG_VERSION")
            return true
        } catch (e: Exception) {
            plugin.logger.severe("保存配置文件时出错: ${e.message}")
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

