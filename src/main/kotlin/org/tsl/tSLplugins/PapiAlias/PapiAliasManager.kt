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
     * @return 映射后的别名值，如果不存在则根据配置返回原值或 null
     */
    fun getAliasValue(variableName: String, originalValue: String): String {
        if (!enabled) return originalValue
        
        val variableMap = aliasMappings[variableName.lowercase()]
        if (variableMap == null) {
            return if (returnOriginalIfNotFound) originalValue else originalValue
        }
        
        // 先尝试精确匹配
        val aliasValue = variableMap[originalValue]
        if (aliasValue != null) {
            return aliasValue
        }
        
        // 如果没有精确匹配，返回原值
        return if (returnOriginalIfNotFound) originalValue else originalValue
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
}
