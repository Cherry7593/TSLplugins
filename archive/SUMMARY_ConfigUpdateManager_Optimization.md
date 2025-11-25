# ConfigUpdateManager 优化总结

## 优化时间
2025年11月26日

## 问题概述
根据需求文档，ConfigUpdateManager 存在两个"致命点"和若干细节问题，导致配置更新机制不稳定。

## 主要问题

### 致命问题 1：config-version 无限更新循环
**问题原因**：
- 所有配置项统一使用用户值覆盖默认值
- 当默认 config.yml 版本更新到 12，但用户配置还是 9 时
- 更新逻辑会把默认的 12 覆盖回 9
- 导致每次启动都检测到版本不对，进入无限更新循环

**解决方案**：
在 `processConfigLine` 方法中对 `config-version` 特判：
```kotlin
if (fullKey == "config-version") {
    val commentMatch = """#.*$""".toRegex().find(afterColon)
    val comment = commentMatch?.value ?: ""
    val commentPart = if (comment.isNotEmpty()) "  $comment" else ""
    return ProcessResult("$indent$key: $CURRENT_CONFIG_VERSION$commentPart")
}
```
强制写入当前版本号，不允许被用户旧值覆盖。

### 致命问题 2：List 类型配置未正确合并
**问题原因**：
- 原逻辑按"单行 key: value"处理
- 对于 List 类型配置（如 `allowed-worlds:`），只处理了标题行
- 下面的 `- world` 等项因为不含冒号，被完全忽略
- 导致用户修改的 List 配置完全无效

**解决方案**：
1. 检测到值部分为空时，判断用户配置是否包含 List：
```kotlin
if (valuePartWithoutComment.isEmpty()) {
    if (userConfig.contains(fullKey)) {
        val userValue = userConfig.get(fullKey)
        if (userValue is List<*>) {
            val listBlock = buildListBlock(indent, key, userValue, comment)
            return ProcessResult(listBlock, 0)
        }
    }
    keyStack.add(Pair(key, indentLevel))
    return ProcessResult(line)
}
```

2. 新增 `buildListBlock` 方法生成完整 List 块：
```kotlin
private fun buildListBlock(indent: String, key: String, list: List<*>, comment: String): String {
    val commentPart = if (comment.isNotEmpty()) "  $comment" else ""
    val sb = StringBuilder()
    sb.append("$indent$key:$commentPart\n")
    
    for (item in list) {
        val itemValue = formatValueForList(item)
        sb.append("$indent  - $itemValue\n")
    }
    
    return sb.toString().trimEnd('\n')
}
```

## 细节优化

### 1. 字符串序列化问题
**问题**：
- 手动处理字符串引号和转义容易出错
- 对 `#ffffff` 这种字符串可能误判为注释
- 字符串中包含双引号时未正确转义

**解决方案**：
使用 `YamlConfiguration.saveToString()` 来序列化所有值：
```kotlin
private fun formatValue(value: Any?): String {
    return when (value) {
        is String -> {
            val temp = YamlConfiguration()
            temp.set("v", value)
            val raw = temp.saveToString()
            raw.lineSequence()
                .first { it.startsWith("v:") }
                .substringAfter("v: ")
        }
        // ... 其他类型
    }
}
```
这样确保所有特殊字符、引号、转义都由 SnakeYAML 正确处理。

### 2. 编码问题
**修改前**：
```kotlin
InputStreamReader(it).readText()
```

**修改后**：
```kotlin
InputStreamReader(it, StandardCharsets.UTF_8).readText()
```
并添加导入：`import java.nio.charset.StandardCharsets`

同时使用 `.use {}` 确保流自动关闭。

### 3. 备份文件命名优化
**修改前**：
```kotlin
val backupFile = File(plugin.dataFolder, "config.yml.backup")
```

**修改后**：
```kotlin
val backupFile = File(plugin.dataFolder, "config-v$currentVersion.yml.bak")
plugin.logger.info("已备份旧配置文件到: ${backupFile.name}")
```
带版本号的备份文件，便于追溯和管理。

### 4. 移除未使用的 extractAllValues 方法
原代码中 `extractAllValues` 方法被调用但其结果 `userValues` 从未使用，已完全移除该方法，简化代码逻辑。

### 5. 配置重载优化
**TSLplugins.kt 修改**：
```kotlin
val configUpdated = configUpdateManager.checkAndUpdate()
if (configUpdated) {
    reloadConfig()
}
```
只在配置真正更新时才重新加载，避免不必要的开销。

## 数据结构改进

### ProcessResult 数据类
新增了返回结果封装类：
```kotlin
private data class ProcessResult(
    val line: String,
    val skipLines: Int = 0
)
```
用于在处理 List 时告知主循环跳过默认配置中的列表项（预留功能）。

## 核心优势

1. **版本号强制更新**：彻底解决无限更新循环问题
2. **List 完整保留**：用户修改的列表配置完全生效
3. **字符串安全**：所有特殊字符由 SnakeYAML 正确处理
4. **UTF-8 编码**：中文注释不会乱码
5. **版本化备份**：便于追溯历史配置

## 兼容性说明

- 保持与 Bukkit/Spigot API 完全兼容
- 保留所有现有注释和格式
- 向后兼容旧版本配置文件
- 适用于 MC Folia 1.21.8 及类似版本

## 测试建议

建议测试场景：
1. 配置文件不存在时的初始化
2. 从旧版本（如 v9）更新到 v12
3. List 类型配置的保留（如 allowed-worlds）
4. 包含特殊字符的字符串配置（如颜色代码 `&#ffffff`）
5. 中文注释的保留和编码正确性
6. 多次更新不会进入循环

## 相关文件

- `src/main/kotlin/org/tsl/tSLplugins/ConfigUpdateManager.kt` - 主要修改文件
- `src/main/kotlin/org/tsl/tSLplugins/TSLplugins.kt` - 调用处优化

## 结论

本次优化解决了配置更新机制的核心问题，使系统更加稳定可靠。通过引入版本强制更新、List 完整处理和 YAML 标准序列化，大幅提升了配置管理的健壮性。

