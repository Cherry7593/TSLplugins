package org.tsl.tSLplugins.PapiAlias

import org.bukkit.plugin.java.JavaPlugin
import java.util.concurrent.ConcurrentHashMap

/**
 * PapiAlias 变量映射管理器
 * 
 * 负责管理变量别名映射配置，支持将 PAPI 变量值映射为简写形式。
 * 使用 ConcurrentHashMap 确保 Folia 多线程环境下的线程安全。
 * 
 * 配置示例：
 * papi-alias:
 *   enabled: true
 *   mappings:
 *     guild:
 *       "宝可梦": "梦"
 *       "潮汐亭": "汐"
 *     faction:
 *       "红队": "红"
 *       "蓝队": "蓝"
 */
class PapiAliasManager(private val plugin: JavaPlugin) {
    
    private var enabled: Boolean = false
    
    // 映射存储：外层 key 是变量名，内层是 原始值 -> 简写值
    // 使用 volatile 确保原子化更新时的可见性
    @Volatile
    private var aliasMappings: ConcurrentHashMap<String, ConcurrentHashMap<String, String>> = ConcurrentHashMap()
    
    // 缓存默认值配置（当映射不存在时返回原值还是空）
    private var returnOriginalIfNotFound: Boolean = true
    
    init {
        loadConfig()
    }
    
    /**
     * 加载配置（线程安全，原子化更新）
     * 先创建临时 Map 加载数据，完成后再替换主引用
     */
    fun loadConfig() {
        val config = plugin.config
        
        enabled = config.getBoolean("papi-alias.enabled", true)
        returnOriginalIfNotFound = config.getBoolean("papi-alias.return-original-if-not-found", true)
        
        // 创建临时映射表
        val tempMappings = ConcurrentHashMap<String, ConcurrentHashMap<String, String>>()
        
        val mappingsSection = config.getConfigurationSection("papi-alias.mappings")
        if (mappingsSection != null) {
            for (variableName in mappingsSection.getKeys(false)) {
                val variableSection = mappingsSection.getConfigurationSection(variableName)
                if (variableSection != null) {
                    val valueMap = ConcurrentHashMap<String, String>()
                    for (originalValue in variableSection.getKeys(false)) {
                        val aliasValue = variableSection.getString(originalValue)
                        if (aliasValue != null) {
                            valueMap[originalValue] = aliasValue
                        }
                    }
                    if (valueMap.isNotEmpty()) {
                        tempMappings[variableName.lowercase()] = valueMap
                    }
                }
            }
        }
        
        // 原子化替换主引用
        aliasMappings = tempMappings
        
        plugin.logger.info("[PapiAlias] 已加载 ${getMappingCount()} 个变量映射")
    }
    
    /**
     * 重载配置
     */
    fun reloadConfig() {
        plugin.reloadConfig()
        loadConfig()
    }
    
    /**
     * 检查功能是否启用
     */
    fun isEnabled(): Boolean = enabled
    
    /**
     * 获取变量的别名值
     * 
     * @param variableName 变量名（如 "guild"）
     * @param originalValue 原始值（如 "宝可梦"）
     * @return 映射后的别名值，如果不存在则根据配置返回原值或空字符串
     */
    fun getAliasValue(variableName: String, originalValue: String): String {
        if (!enabled) return originalValue
        
        val variableMap = aliasMappings[variableName.lowercase()]
        if (variableMap == null) {
            return if (returnOriginalIfNotFound) originalValue else ""
        }
        
        // 1. 先尝试精确匹配
        val exactMatch = variableMap[originalValue]
        if (exactMatch != null) {
            return exactMatch
        }
        
        // 2. 提取原值中的中文字符进行匹配（核心匹配逻辑）
        val chineseOriginal = extractChinese(originalValue)
        if (chineseOriginal.isNotEmpty()) {
            for ((key, value) in variableMap) {
                val chineseKey = extractChinese(key)
                // 中文完全匹配
                if (chineseKey == chineseOriginal) {
                    return value
                }
                // 中文包含匹配（原值中文包含配置的中文key）
                if (chineseOriginal.contains(chineseKey) && chineseKey.isNotEmpty()) {
                    return value
                }
            }
        }
        
        // 3. 尝试去除颜色代码后匹配
        val strippedOriginal = stripColorCodes(originalValue)
        for ((key, value) in variableMap) {
            val strippedKey = stripColorCodes(key)
            if (strippedKey == strippedOriginal) {
                return value
            }
        }
        
        // 4. 尝试去色后包含匹配
        for ((key, value) in variableMap) {
            val strippedKey = stripColorCodes(key)
            if (strippedOriginal.contains(strippedKey) && strippedKey.isNotEmpty()) {
                return value
            }
        }
        
        // 如果没有匹配，返回原值或空
        return if (returnOriginalIfNotFound) originalValue else ""
    }
    
    /**
     * 提取文本中的中文字符
     * 用于匹配带有颜色代码的变量值
     */
    private fun extractChinese(text: String): String {
        return text.filter { it.code in 0x4E00..0x9FFF || it.code in 0x3400..0x4DBF }
    }
    
    /**
     * 去除颜色代码
     * 支持: §x, &x, &#xxxxxx, <gradient:...>, <color:...> 等格式
     */
    private fun stripColorCodes(text: String): String {
        var result = text
        // 去除 MiniMessage 格式 <gradient:...>内容</gradient> 等
        result = result.replace(Regex("<[^>]+>"), "")
        // 去除 &#xxxxxx 格式 (hex color)
        result = result.replace(Regex("&#[0-9a-fA-F]{6}"), "")
        // 去除 &x&x&x&x&x&x&x 格式 (hex color)
        result = result.replace(Regex("&x(&[0-9a-fA-F]){6}"), "")
        // 去除 §x§x§x§x§x§x§x 格式
        result = result.replace(Regex("§x(§[0-9a-fA-F]){6}"), "")
        // 去除 &x 或 §x 格式的颜色代码
        result = result.replace(Regex("[&§][0-9a-fA-Fk-oK-OrR]"), "")
        return result
    }
    
    /**
     * 检查某个变量是否有映射配置
     */
    fun hasMapping(variableName: String): Boolean {
        return aliasMappings.containsKey(variableName.lowercase())
    }
    
    /**
     * 获取所有已配置的变量名列表
     */
    fun getMappedVariables(): Set<String> {
        return aliasMappings.keys.toSet()
    }
    
    /**
     * 获取某个变量的所有映射
     */
    fun getVariableMappings(variableName: String): Map<String, String> {
        return aliasMappings[variableName.lowercase()]?.toMap() ?: emptyMap()
    }
    
    /**
     * 获取总映射数量
     */
    fun getMappingCount(): Int {
        return aliasMappings.values.sumOf { it.size }
    }
    
    /**
     * 获取变量数量
     */
    fun getVariableCount(): Int {
        return aliasMappings.size
    }
    
    /**
     * 调试：打印当前加载的所有映射
     */
    fun debugPrintMappings(): List<String> {
        val lines = mutableListOf<String>()
        lines.add("PapiAlias 映射状态: enabled=$enabled, returnOriginal=$returnOriginalIfNotFound")
        for ((varName, mappings) in aliasMappings) {
            lines.add("变量 [$varName]:")
            for ((original, alias) in mappings) {
                lines.add("  '$original' -> '$alias'")
                lines.add("  (中文: '${extractChinese(original)}', 去色: '${stripColorCodes(original)}')") 
            }
        }
        return lines
    }
}
