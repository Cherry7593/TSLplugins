package org.tsl.tSLplugins

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

/**
 * 配置文件更新管理器
 * 负责检测和更新配置文件版本，保留注释并优化格式
 *
 * 特性：
 * - 自动检测版本并更新配置文件
 * - 完整保留用户配置的值（包括 List 类型）
 * - 保留默认配置的注释和格式
 * - 自动备份旧配置文件
 */
class ConfigUpdateManager(private val plugin: JavaPlugin) {

    companion object {
        // 当前配置文件版本
        const val CURRENT_CONFIG_VERSION = 13
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

        // 备份旧配置文件（带版本号）
        val backupFile = File(plugin.dataFolder, "config-v$currentVersion.yml.bak")
        try {
            configFile.copyTo(backupFile, overwrite = true)
            plugin.logger.info("已备份旧配置文件到: ${backupFile.name}")
        } catch (e: Exception) {
            plugin.logger.warning("备份配置文件失败: ${e.message}")
        }

        // 读取默认配置的原始文本（保留注释）
        val defaultConfigText = plugin.getResource("config.yml")?.use {
            InputStreamReader(it, StandardCharsets.UTF_8).readText()
        } ?: run {
            plugin.logger.severe("无法读取默认配置文件！")
            return false
        }

        // 合并配置：使用默认配置的格式和注释，但替换为用户的值
        val updatedConfigText = mergeConfigWithComments(defaultConfigText, currentConfig)

        // 保存更新后的配置
        try {
            configFile.writeText(updatedConfigText, StandardCharsets.UTF_8)
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
     * 合并配置：使用默认配置的格式，但替换为用户的值
     */
    private fun mergeConfigWithComments(
        defaultText: String,
        userConfig: YamlConfiguration
    ): String {
        val lines = defaultText.lines().toMutableList()
        val result = mutableListOf<String>()
        val keyStack = mutableListOf<Pair<String, Int>>() // (key, indentLevel)

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
                val processedInfo = processConfigLine(line, userConfig, keyStack)
                result.add(processedInfo.line)

                // 如果处理了 List，跳过默认配置中的 List 项
                if (processedInfo.skipLines > 0) {
                    i += processedInfo.skipLines
                }
            } else {
                result.add(line)
            }

            i++
        }

        return result.joinToString("\n")
    }

    /**
     * 配置行处理结果
     */
    private data class ProcessResult(
        val line: String,
        val skipLines: Int = 0
    )

    /**
     * 处理单行配置，保留注释并替换值
     */
    private fun processConfigLine(
        line: String,
        userConfig: YamlConfiguration,
        keyStack: MutableList<Pair<String, Int>>
    ): ProcessResult {
        val colonIndex = line.indexOf(":")
        if (colonIndex == -1) return ProcessResult(line)

        val beforeColon = line.substring(0, colonIndex)
        val afterColon = line.substring(colonIndex + 1)

        // 提取缩进级别
        val indent = beforeColon.takeWhile { it.isWhitespace() }
        val indentLevel = indent.length
        val key = beforeColon.trim()

        // 更新键栈
        while (keyStack.isNotEmpty() && keyStack.last().second >= indentLevel) {
            keyStack.removeAt(keyStack.size - 1)
        }

        // 构建完整键路径
        val fullKey = if (keyStack.isEmpty()) {
            key
        } else {
            keyStack.joinToString(".") { it.first } + ".$key"
        }

        // 【关键修复1】config-version 强制使用当前版本，不允许用户值覆盖
        if (fullKey == "config-version") {
            val commentMatch = """#.*$""".toRegex().find(afterColon)
            val comment = commentMatch?.value ?: ""
            val commentPart = if (comment.isNotEmpty()) "  $comment" else ""
            return ProcessResult("$indent$key: $CURRENT_CONFIG_VERSION$commentPart")
        }

        // 检查是否有行尾注释
        val commentMatch = """#.*$""".toRegex().find(afterColon)
        val comment = commentMatch?.value ?: ""
        val valuePartWithoutComment = if (comment.isNotEmpty()) {
            afterColon.substring(0, afterColon.indexOf('#')).trim()
        } else {
            afterColon.trim()
        }

        // 如果值部分为空，说明这可能是一个节点标题或 List 的开始
        if (valuePartWithoutComment.isEmpty()) {
            // 【关键修复2】检查用户配置是否包含此 key 的 List
            if (userConfig.contains(fullKey)) {
                val userValue = userConfig.get(fullKey)

                if (userValue is List<*>) {
                    // 用户有 List 值，生成完整的 List 块
                    val listBlock = buildListBlock(indent, key, userValue, comment)
                    return ProcessResult(listBlock, 0)
                }
            }

            // 不是 List，是普通节点标题
            keyStack.add(Pair(key, indentLevel))
            return ProcessResult(line)
        }

        // 有具体值的配置项
        val userValue = if (userConfig.contains(fullKey)) {
            userConfig.get(fullKey)
        } else {
            null
        }

        // 如果有用户值，使用用户的值
        return if (userValue != null) {
            val formattedValue = formatValue(userValue)
            val commentPart = if (comment.isNotEmpty()) "  $comment" else ""
            ProcessResult("$indent$key: $formattedValue$commentPart")
        } else {
            ProcessResult(line) // 保留默认值
        }
    }

    /**
     * 构建 List 配置块
     */
    private fun buildListBlock(indent: String, key: String, list: List<*>, comment: String): String {
        val commentPart = if (comment.isNotEmpty()) "  $comment" else ""
        val sb = StringBuilder()
        sb.append("$indent$key:$commentPart\n")

        for (item in list) {
            val itemValue = formatValueForList(item)
            sb.append("$indent  - $itemValue\n")
        }

        // 移除最后的换行符
        return sb.toString().trimEnd('\n')
    }

    /**
     * 格式化 List 项的值
     */
    private fun formatValueForList(value: Any?): String {
        return when (value) {
            null -> "null"
            is String -> {
                // 使用 YamlConfiguration 来正确序列化字符串
                val temp = YamlConfiguration()
                temp.set("v", value)
                val raw = temp.saveToString()
                raw.lineSequence()
                    .first { it.startsWith("v:") }
                    .substringAfter("v: ")
            }
            else -> value.toString()
        }
    }

    /**
     * 格式化值为 YAML 格式
     * 使用 YamlConfiguration 来确保正确的序列化
     */
    private fun formatValue(value: Any?): String {
        return when (value) {
            null -> "null"
            is String -> {
                // 使用 YamlConfiguration 来正确处理特殊字符、引号、转义等
                val temp = YamlConfiguration()
                temp.set("v", value)
                val raw = temp.saveToString()
                raw.lineSequence()
                    .first { it.startsWith("v:") }
                    .substringAfter("v: ")
            }
            is Boolean -> value.toString()
            is Number -> value.toString()
            is List<*> -> {
                // List 类型应该在 buildListBlock 中处理，这里不应该到达
                // 但为了安全，还是提供一个后备方案
                if (value.isEmpty()) "[]"
                else {
                    val sb = StringBuilder("\n")
                    for (item in value) {
                        sb.append("  - ${formatValueForList(item)}\n")
                    }
                    sb.toString().trimEnd('\n')
                }
            }
            else -> {
                // 其他类型也用 YamlConfiguration 序列化
                val temp = YamlConfiguration()
                temp.set("v", value)
                val raw = temp.saveToString()
                raw.lineSequence()
                    .first { it.startsWith("v:") }
                    .substringAfter("v: ")
            }
        }
    }

    /**
     * 获取配置文件版本
     */
    fun getCurrentVersion(): Int {
        return plugin.config.getInt("config-version", 0)
    }
}

