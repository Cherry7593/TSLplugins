package org.tsl.tSLplugins.PapiAlias

import me.clip.placeholderapi.PlaceholderAPI
import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.OfflinePlayer
import org.bukkit.plugin.java.JavaPlugin

/**
 * PapiAlias PlaceholderAPI 扩展
 * 
 * 拦截已有的 PAPI 变量值，并根据配置将其转换为预设的简写。
 * 
 * 使用方式：
 * %tsl_alias_<变量名>% 
 * 
 * 例如：
 * - %tsl_alias_guild% -> 解析 %guild% 的值，然后映射为简写
 * - %tsl_alias_faction% -> 解析 %faction% 的值，然后映射为简写
 * 
 * 如果原始值在配置列表内，返回对应的简写值；
 * 如果原始值不在配置内，返回原始字符串。
 */
class PapiAliasExpansion(
    private val plugin: JavaPlugin,
    private val manager: PapiAliasManager
) : PlaceholderExpansion() {
    
    override fun getIdentifier(): String = "tsl"
    
    override fun getAuthor(): String = "TSL"
    
    override fun getVersion(): String = "1.0"
    
    override fun persist(): Boolean = true
    
    /**
     * 处理占位符请求
     * 
     * 支持格式：
     * - %tsl_alias_<变量名>% - 解析并映射变量值
     * 
     * @param player 请求的玩家（可能为 null）
     * @param params 参数（去除 tsl_ 前缀后的部分）
     * @return 处理后的字符串，如果不匹配返回 null
     */
    override fun onRequest(player: OfflinePlayer?, params: String): String? {
        // 只处理 alias_ 开头的参数
        if (!params.startsWith("alias_", ignoreCase = true)) {
            return null
        }
        
        // 检查功能是否启用
        if (!manager.isEnabled()) {
            return null
        }
        
        // 提取变量名（移除 "alias_" 前缀）
        val variableName = params.substring(6) // "alias_".length = 6
        
        if (variableName.isEmpty()) {
            return null
        }
        
        // 使用 PlaceholderAPI 解析原始变量值
        // 构建完整的占位符格式：%变量名%
        val originalPlaceholder = "%$variableName%"
        
        // 解析原始值（需要在线玩家）
        val originalValue = if (player != null) {
            PlaceholderAPI.setPlaceholders(player, originalPlaceholder)
        } else {
            // 没有玩家时尝试解析（某些变量不需要玩家）
            PlaceholderAPI.setPlaceholders(null, originalPlaceholder)
        }
        
        // 如果解析结果仍然是占位符本身，说明变量不存在
        if (originalValue == originalPlaceholder) {
            return null
        }
        
        // 查找映射并返回
        return manager.getAliasValue(variableName, originalValue)
    }
}
