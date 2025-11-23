package org.tsl.tSLplugins

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.io.InputStreamReader

/**
 * 配置文件更新管理器
 * 负责检测和更新配置文件版本，保留注释并优化格式
 */
class ConfigUpdateManager(private val plugin: JavaPlugin) {

    companion object {
        // 当前配置文件版本
        const val CURRENT_CONFIG_VERSION = 10
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
        plugin.logger.info("开始更新配置文件，保留注释并优化格式...")

        // 备份旧配置文件
        val backupFile = File(plugin.dataFolder, "config.yml.backup")
        try {
            configFile.copyTo(backupFile, overwrite = true)
            plugin.logger.info("已备份旧配置文件到: config.yml.backup")
        } catch (e: Exception) {
            plugin.logger.warning("备份配置文件失败: ${e.message}")
        }

        // 读取默认配置的原始文本（保留注释）
        val defaultConfigText = plugin.getResource("config.yml")?.let {
            InputStreamReader(it).readText()
        } ?: run {
            plugin.logger.severe("无法读取默认配置文件！")
            return false
        }

        // 读取用户配置的值
        val userValues = mutableMapOf<String, Any?>()
        extractAllValues(currentConfig, "", userValues)

        // 合并配置：使用默认配置的格式和注释，但替换为用户的值
        val updatedConfigText = mergeConfigWithComments(defaultConfigText, userValues, currentConfig)

        // 保存更新后的配置
        try {
            configFile.writeText(updatedConfigText)
            plugin.logger.info("配置文件更新完成！")
            plugin.logger.info("  - 已保留所有注释和格式")
            plugin.logger.info("  - 已保留用户的配置值")
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
     * 递归提取所有配置值
     */
    private fun extractAllValues(config: YamlConfiguration, prefix: String, values: MutableMap<String, Any?>) {
        for (key in config.getKeys(false)) {
            val fullKey = if (prefix.isEmpty()) key else "$prefix.$key"
            val value = config.get(key)

            if (value is org.bukkit.configuration.ConfigurationSection) {
                // 递归处理子节点
                extractAllValues(
                    YamlConfiguration().apply {
                        value.getKeys(false).forEach { subKey ->
                            this.set(subKey, value.get(subKey))
                        }
                    },
                    fullKey,
                    values
                )
            } else {
                values[fullKey] = value
            }
        }
    }

    /**
     * 合并配置：使用默认配置的格式，但替换为用户的值
     */
    private fun mergeConfigWithComments(
        defaultText: String,
        userValues: Map<String, Any?>,
        userConfig: YamlConfiguration
    ): String {
        val lines = defaultText.lines().toMutableList()
        val result = mutableListOf<String>()

        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            val trimmed = line.trim()

            // 保留空行和纯注释行
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                result.add(line)
                i++
                continue
            }

            // 解析键值对
            if (line.contains(":")) {
                val (processedLine, keyPath) = processConfigLine(line, userValues, userConfig)
                result.add(processedLine)
            } else {
                result.add(line)
            }

            i++
        }

        return result.joinToString("\n")
    }

    /**
     * 处理单行配置，保留注释并替换值
     */
    private fun processConfigLine(
        line: String,
        userValues: Map<String, Any?>,
        userConfig: YamlConfiguration
    ): Pair<String, String?> {
        val colonIndex = line.indexOf(":")
        if (colonIndex == -1) return Pair(line, null)

        val beforeColon = line.substring(0, colonIndex)
        val afterColon = line.substring(colonIndex + 1)

        // 提取缩进
        val indent = beforeColon.takeWhile { it.isWhitespace() }
        val key = beforeColon.trim()

        // 计算完整的键路径（基于缩进级别）
        val keyPath = key

        // 检查是否有行尾注释
        val commentMatch = """#.*$""".toRegex().find(afterColon)
        val comment = commentMatch?.value ?: ""
        val valuePartWithoutComment = if (comment.isNotEmpty()) {
            afterColon.substring(0, afterColon.indexOf('#')).trim()
        } else {
            afterColon.trim()
        }

        // 如果用户配置中有这个键，使用用户的值
        val userValue = if (userConfig.contains(keyPath)) {
            userConfig.get(keyPath)
        } else {
            null
        }

        // 如果有用户值且值部分不为空（不是节点标题）
        if (userValue != null && valuePartWithoutComment.isNotEmpty()) {
            val formattedValue = formatValue(userValue)
            // 保留行尾注释，将注释放在值后面
            val newLine = if (comment.isNotEmpty()) {
                "$indent$key: $formattedValue  $comment"
            } else {
                "$indent$key: $formattedValue"
            }
            return Pair(newLine, keyPath)
        }

        return Pair(line, keyPath)
    }

    /**
     * 格式化值为 YAML 格式
     */
    private fun formatValue(value: Any?): String {
        return when (value) {
            null -> "null"
            is String -> {
                // 如果字符串包含特殊字符或颜色代码，使用引号
                if (value.contains("&") || value.contains("#") || value.contains(":") ||
                    value.contains("[") || value.contains("]") || value.contains("\"")) {
                    "\"$value\""
                } else {
                    value
                }
            }
            is Boolean -> value.toString()
            is Number -> value.toString()
            is List<*> -> {
                if (value.isEmpty()) "[]"
                else "\n" + value.joinToString("\n") { "  - $it" }
            }
            else -> value.toString()
        }
    }

    /**
     * 获取配置文件版本
     */
    fun getCurrentVersion(): Int {
        return plugin.config.getInt("config-version", 0)
    }
}

