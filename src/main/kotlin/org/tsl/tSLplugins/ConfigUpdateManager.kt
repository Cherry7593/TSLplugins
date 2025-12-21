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
        /**
         * 用户专属配置路径列表
         * 这些配置的子节点完全由用户自定义，更新时会保留用户的全部内容
         * 不会被默认配置覆盖
         */
        private val USER_OWNED_KEYS = setOf(
            "permission-checker.rules"  // 权限检测规则，用户自定义
        )
    }

    // 从默认配置动态读取的目标版本（懒加载）
    private val targetVersion: Int by lazy { readDefaultConfigVersion() }

    /**
     * 从 resources/config.yml 读取默认配置版本
     * 这样就不需要手动维护版本常量了
     */
    private fun readDefaultConfigVersion(): Int {
        return try {
            plugin.getResource("config.yml")?.use { stream ->
                val defaultConfig = YamlConfiguration()
                defaultConfig.load(InputStreamReader(stream, StandardCharsets.UTF_8))
                defaultConfig.getInt("config-version", 0)
            } ?: 0
        } catch (e: Exception) {
            plugin.logger.warning("无法读取默认配置版本: ${e.message}")
            0
        }
    }

    /**
     * 检查并更新配置文件
     * @return 是否进行了更新
     */
    fun checkAndUpdate(): Boolean {
        val configFile = File(plugin.dataFolder, "config.yml")

        // 先获取目标版本
        val latestVersion = targetVersion
        if (latestVersion == 0) {
            plugin.logger.warning("无法确定默认配置版本，跳过配置更新检查")
            if (!configFile.exists()) {
                plugin.saveDefaultConfig()
            }
            return false
        }

        // 如果配置文件不存在，直接保存默认配置
        if (!configFile.exists()) {
            plugin.saveDefaultConfig()
            plugin.logger.info("配置文件不存在，已创建默认配置文件（版本 $latestVersion）")
            return true
        }

        // 尝试读取现有配置，如果解析失败则自动修复
        val currentConfig: YamlConfiguration
        val currentVersion: Int
        
        try {
            currentConfig = YamlConfiguration()
            currentConfig.load(configFile)
            currentVersion = currentConfig.getInt("config-version", 0)
        } catch (e: Exception) {
            plugin.logger.severe("配置文件解析失败: ${e.message}")
            plugin.logger.info("正在尝试自动修复配置文件...")
            
            // 备份损坏的配置文件
            val corruptedBackup = File(plugin.dataFolder, "config-corrupted-${System.currentTimeMillis()}.yml.bak")
            try {
                configFile.copyTo(corruptedBackup, overwrite = true)
                plugin.logger.info("已备份损坏的配置文件到: ${corruptedBackup.name}")
            } catch (ex: Exception) {
                plugin.logger.warning("备份损坏文件失败: ${ex.message}")
            }
            
            // 尝试修复常见的 YAML 语法错误
            val repaired = tryRepairYaml(configFile)
            if (repaired) {
                plugin.logger.info("配置文件已自动修复，重新加载...")
                return checkAndUpdate() // 递归重试
            }
            
            // 无法修复，使用默认配置
            plugin.logger.warning("无法自动修复配置文件，将使用默认配置")
            plugin.saveDefaultConfig()
            plugin.logger.info("已重置为默认配置文件（版本 $latestVersion）")
            plugin.logger.info("旧配置已备份到: ${corruptedBackup.name}")
            return true
        }

        // 版本一致，无需更新
        if (currentVersion == latestVersion) {
            plugin.logger.info("配置文件版本正确（v$currentVersion），无需更新")
            return false
        }

        // 需要更新
        plugin.logger.info("检测到配置文件版本不同（当前: v$currentVersion, 最新: v$latestVersion）")
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
            plugin.logger.info("  - 配置文件已更新到版本 $latestVersion")
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
                // 先检查是否是用户专属配置
                val userOwnedResult = checkUserOwnedKey(line, lines, i, userConfig, keyStack)
                if (userOwnedResult != null) {
                    result.addAll(userOwnedResult.lines)
                    i += userOwnedResult.skipLines
                    i++
                    continue
                }

                val processedInfo = processConfigLine(line, userConfig, keyStack, lines, i)
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
     * 用户专属配置处理结果
     */
    private data class UserOwnedResult(
        val lines: List<String>,
        val skipLines: Int
    )

    /**
     * 检查并处理用户专属配置
     * 如果当前行是用户专属配置的键，返回用户的完整配置内容
     *
     * @return 如果是用户专属配置，返回处理结果；否则返回 null
     */
    private fun checkUserOwnedKey(
        currentLine: String,
        allLines: List<String>,
        currentIndex: Int,
        userConfig: YamlConfiguration,
        keyStack: MutableList<Pair<String, Int>>
    ): UserOwnedResult? {
        val colonIndex = currentLine.indexOf(":")
        if (colonIndex == -1) return null

        val beforeColon = currentLine.substring(0, colonIndex)
        val indent = beforeColon.takeWhile { it.isWhitespace() }
        val indentLevel = indent.length
        val key = beforeColon.trim()

        // 临时计算完整键路径（不修改 keyStack）
        val tempStack = keyStack.toMutableList()
        while (tempStack.isNotEmpty() && tempStack.last().second >= indentLevel) {
            tempStack.removeAt(tempStack.size - 1)
        }

        val fullKey = if (tempStack.isEmpty()) key else tempStack.joinToString(".") { it.first } + ".$key"

        // 检查是否是用户专属配置
        if (fullKey !in USER_OWNED_KEYS) return null

        // 这是用户专属配置，需要特殊处理
        plugin.logger.info("检测到用户专属配置: $fullKey，将保留用户的完整配置")

        // 更新真正的 keyStack
        while (keyStack.isNotEmpty() && keyStack.last().second >= indentLevel) {
            keyStack.removeAt(keyStack.size - 1)
        }
        keyStack.add(Pair(key, indentLevel))

        // 计算需要跳过的默认配置行数
        val skipLines = countChildLines(allLines, currentIndex, indentLevel)

        // 生成用户的配置内容
        val userSection = userConfig.getConfigurationSection(fullKey)
        val resultLines = mutableListOf<String>()

        // 添加节点标题行
        resultLines.add(currentLine)

        if (userSection != null && userSection.getKeys(false).isNotEmpty()) {
            // 用户有配置，序列化用户的完整配置
            val userContent = serializeConfigSection(userSection, indentLevel + 2)
            resultLines.addAll(userContent)
        } else {
            // 用户没有配置，保留默认配置的示例
            // 这种情况下不跳过默认行，让它们被正常处理
            return null
        }

        return UserOwnedResult(resultLines, skipLines)
    }

    /**
     * 计算 List 项的行数（以 "- " 开头的连续行）
     */
    private fun countListItems(lines: List<String>, startIndex: Int, parentIndent: Int): Int {
        var count = 0
        var i = startIndex + 1
        val listItemIndent = parentIndent + 2  // List 项的缩进比父节点多 2

        while (i < lines.size) {
            val line = lines[i]
            val trimmed = line.trim()

            // 空行或注释行，继续检查
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                count++
                i++
                continue
            }

            // 检查是否是 List 项（以 "- " 开头）
            val currentIndent = line.takeWhile { it.isWhitespace() }.length
            if (currentIndent == listItemIndent && trimmed.startsWith("- ")) {
                count++
                i++
                continue
            }

            // 不是 List 项，停止计数
            break
        }

        return count
    }

    /**
     * 计算某个节点下的所有子行数（用于跳过默认配置）
     */
    private fun countChildLines(lines: List<String>, startIndex: Int, parentIndent: Int): Int {
        var count = 0
        var i = startIndex + 1

        while (i < lines.size) {
            val line = lines[i]
            val trimmed = line.trim()

            // 空行和注释行也计入
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                // 检查下一个非空行的缩进
                var nextNonEmpty = i + 1
                while (nextNonEmpty < lines.size) {
                    val nextLine = lines[nextNonEmpty].trim()
                    if (nextLine.isNotEmpty() && !nextLine.startsWith("#")) break
                    nextNonEmpty++
                }

                if (nextNonEmpty < lines.size) {
                    val nextIndent = lines[nextNonEmpty].takeWhile { it.isWhitespace() }.length
                    if (nextIndent <= parentIndent) {
                        // 下一个有效行不是子节点，停止计数
                        break
                    }
                }
                count++
                i++
                continue
            }

            // 检查缩进级别
            val currentIndent = line.takeWhile { it.isWhitespace() }.length
            if (currentIndent <= parentIndent) {
                // 不再是子节点，停止计数
                break
            }

            count++
            i++
        }

        return count
    }

    /**
     * 序列化 ConfigurationSection 为 YAML 行列表
     */
    private fun serializeConfigSection(section: org.bukkit.configuration.ConfigurationSection, baseIndent: Int): List<String> {
        val result = mutableListOf<String>()
        val indentStr = " ".repeat(baseIndent)

        for (key in section.getKeys(false)) {
            val value = section.get(key)

            when (value) {
                is org.bukkit.configuration.ConfigurationSection -> {
                    // 嵌套的 section
                    result.add("$indentStr$key:")
                    result.addAll(serializeConfigSection(value, baseIndent + 2))
                }
                is List<*> -> {
                    // 列表
                    result.add("$indentStr$key:")
                    for (item in value) {
                        result.add("$indentStr  - ${formatValueForList(item)}")
                    }
                }
                else -> {
                    // 简单值
                    result.add("$indentStr$key: ${formatValue(value)}")
                }
            }
        }

        return result
    }

    /**
     * 处理单行配置，保留注释并替换值
     * @param allLines 所有配置行（用于计算需要跳过的行数）
     * @param currentIndex 当前行索引
     */
    private fun processConfigLine(
        line: String,
        userConfig: YamlConfiguration,
        keyStack: MutableList<Pair<String, Int>>,
        allLines: List<String> = emptyList(),
        currentIndex: Int = 0
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

        // config-version 强制使用默认配置的版本，不允许用户值覆盖
        if (fullKey == "config-version") {
            // 直接返回默认配置中的版本行，不做任何修改
            return ProcessResult(line)
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
                    // 计算需要跳过的默认配置 List 项行数
                    val skipLines = countListItems(allLines, currentIndex, indentLevel)
                    return ProcessResult(listBlock, skipLines)
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

    /**
     * 尝试修复常见的 YAML 语法错误
     * @return 是否成功修复
     */
    private fun tryRepairYaml(configFile: File): Boolean {
        try {
            var content = configFile.readText(StandardCharsets.UTF_8)
            var modified = false

            // 修复 1: 将 {key=value} 格式修复为 {key: value}
            // 常见错误: - {min=0.1, max=1.0} 应该是 - {min: 0.1, max: 1.0}
            val inlineMapPattern = Regex("""(\{[^}]*?)=([^},]*?)([,}])""")
            while (inlineMapPattern.containsMatchIn(content)) {
                content = inlineMapPattern.replace(content) { match ->
                    "${match.groupValues[1]}: ${match.groupValues[2]}${match.groupValues[3]}"
                }
                modified = true
            }

            // 修复 2: 将内联 map 转换为块格式（解决混合格式问题）
            // 例如: - {min: 0.1, max: 1.0, weight: 1.0}
            // 转换为:
            // - min: 0.1
            //   max: 1.0
            //   weight: 1.0
            val inlineMapLinePattern = Regex("""^(\s*)-\s*\{([^}]+)\}\s*$""", RegexOption.MULTILINE)
            if (inlineMapLinePattern.containsMatchIn(content)) {
                content = inlineMapLinePattern.replace(content) { match ->
                    val indent = match.groupValues[1]
                    val mapContent = match.groupValues[2]
                    val entries = mapContent.split(",").map { it.trim() }
                    val blockFormat = StringBuilder()
                    entries.forEachIndexed { index, entry ->
                        if (index == 0) {
                            blockFormat.append("$indent- $entry")
                        } else {
                            blockFormat.append("\n$indent  $entry")
                        }
                    }
                    blockFormat.toString()
                }
                modified = true
            }

            // 修复 3: 移除行尾多余的空格
            val trailingSpacePattern = Regex("""[ \t]+$""", RegexOption.MULTILINE)
            if (trailingSpacePattern.containsMatchIn(content)) {
                content = trailingSpacePattern.replace(content, "")
                modified = true
            }

            // 修复 4: 确保文件末尾有换行符
            if (!content.endsWith("\n")) {
                content += "\n"
                modified = true
            }

            // 修复 5: 修复缩进不一致问题（将 tab 转换为空格）
            if (content.contains("\t")) {
                content = content.replace("\t", "  ")
                modified = true
            }

            if (modified) {
                // 验证修复后的内容是否有效
                try {
                    val testConfig = YamlConfiguration()
                    testConfig.loadFromString(content)
                    // 验证成功，保存修复后的文件
                    configFile.writeText(content, StandardCharsets.UTF_8)
                    plugin.logger.info("配置文件语法错误已自动修复")
                    return true
                } catch (e: Exception) {
                    plugin.logger.warning("修复后的配置仍然无效: ${e.message}")
                    return false
                }
            }

            return false
        } catch (e: Exception) {
            plugin.logger.warning("尝试修复配置文件时出错: ${e.message}")
            return false
        }
    }
}
