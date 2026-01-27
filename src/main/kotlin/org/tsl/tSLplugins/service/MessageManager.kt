package org.tsl.tSLplugins.service

import org.bukkit.ChatColor
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

/**
 * 统一消息管理器
 * 负责加载和管理 messages.yml 中的所有消息配置
 */
class MessageManager(private val plugin: JavaPlugin) {
    
    private lateinit var messagesConfig: YamlConfiguration
    private var prefix: String = "&6[TSL]&r "
    
    init {
        loadMessages()
    }
    
    /**
     * 加载消息配置
     */
    fun loadMessages() {
        val messagesFile = File(plugin.dataFolder, "messages.yml")
        
        // 如果文件不存在，从资源中复制
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false)
        }
        
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile)
        
        // 加载默认配置作为后备
        plugin.getResource("messages.yml")?.let { resource ->
            val defaultConfig = YamlConfiguration.loadConfiguration(
                InputStreamReader(resource, StandardCharsets.UTF_8)
            )
            messagesConfig.setDefaults(defaultConfig)
        }
        
        // 加载全局前缀
        prefix = messagesConfig.getString("prefix", "&6[TSL]&r ") ?: "&6[TSL]&r "
        
        plugin.logger.info("[MessageManager] 消息配置已加载")
    }
    
    /**
     * 重载消息配置
     */
    fun reload() {
        loadMessages()
    }
    
    /**
     * 获取全局前缀
     */
    fun getPrefix(): String {
        return colorize(prefix)
    }
    
    /**
     * 获取原始消息（不处理颜色代码）
     */
    fun getRaw(path: String): String {
        return messagesConfig.getString(path) ?: path
    }
    
    /**
     * 获取消息并处理颜色代码
     * @param path 消息路径，如 "common.no_permission"
     * @param replacements 替换参数，格式为 "key" to "value"
     */
    fun get(path: String, vararg replacements: Pair<String, String>): String {
        var message = messagesConfig.getString(path) ?: return colorize("§c[Missing: $path]")
        
        // 替换 %prefix% 为全局前缀
        message = message.replace("%prefix%", prefix)
        
        // 替换自定义占位符
        for ((key, value) in replacements) {
            message = message.replace("{$key}", value)
            message = message.replace("%$key%", value)
        }
        
        return colorize(message)
    }
    
    /**
     * 获取通用消息
     */
    fun getCommon(key: String, vararg replacements: Pair<String, String>): String {
        return get("common.$key", *replacements)
    }
    
    /**
     * 获取模块消息
     * @param module 模块名，如 "timed-attribute", "peace"
     * @param key 消息键，如 "add_success"
     */
    fun getModule(module: String, key: String, vararg replacements: Pair<String, String>): String {
        return get("$module.$key", *replacements)
    }
    
    /**
     * 检查消息是否存在
     */
    fun has(path: String): Boolean {
        return messagesConfig.contains(path)
    }
    
    /**
     * 获取消息列表
     */
    fun getList(path: String): List<String> {
        return messagesConfig.getStringList(path).map { 
            colorize(it.replace("%prefix%", prefix))
        }
    }
    
    /**
     * 处理颜色代码
     * 支持 & 和 § 颜色代码
     */
    private fun colorize(text: String): String {
        return ChatColor.translateAlternateColorCodes('&', text)
    }
    
    companion object {
        /**
         * 格式化时间显示
         */
        fun formatDuration(seconds: Long): String {
            return when {
                seconds < 60 -> "${seconds}秒"
                seconds < 3600 -> "${seconds / 60}分${seconds % 60}秒"
                seconds < 86400 -> "${seconds / 3600}时${(seconds % 3600) / 60}分"
                else -> "${seconds / 86400}天${(seconds % 86400) / 3600}时"
            }
        }
        
        /**
         * 解析时间字符串为秒数
         * 支持格式: 60, 5m, 2h, 1d
         */
        fun parseDuration(input: String): Long? {
            val trimmed = input.trim().lowercase()
            
            // 纯数字（秒）
            trimmed.toLongOrNull()?.let { return it }
            
            // 带单位
            val regex = Regex("^(\\d+)([smhd])$")
            val match = regex.matchEntire(trimmed) ?: return null
            
            val value = match.groupValues[1].toLongOrNull() ?: return null
            val unit = match.groupValues[2]
            
            return when (unit) {
                "s" -> value
                "m" -> value * 60
                "h" -> value * 3600
                "d" -> value * 86400
                else -> null
            }
        }
    }
}
