package org.tsl.tSLplugins.core

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin
import org.tsl.tSLplugins.service.DatabaseManager
import org.tsl.tSLplugins.service.MessageManager
import org.tsl.tSLplugins.service.PlayerDataManager

/**
 * 模块上下文
 * 
 * 为模块提供对全局服务和资源的访问，实现依赖注入。
 * 模块不应直接访问 TSLplugins 实例，而是通过此上下文获取所需服务。
 * 
 * ## 提供的服务
 * - [plugin] - 插件实例（用于调度器等）
 * - [messageManager] - 消息管理器
 * - [playerDataManager] - 玩家数据管理器
 * - [config] - 主配置文件
 * 
 * ## 便捷方法
 * - [getModuleConfig] - 获取模块专属配置节
 * - [registerListener] - 注册事件监听器
 * - [getMessage] - 获取格式化消息
 * 
 * ## 使用示例
 * ```kotlin
 * class KissModule : AbstractModule() {
 *     private lateinit var manager: KissManager
 *     
 *     override fun doEnable() {
 *         // 获取模块配置
 *         val cooldown = context.getModuleConfig(configPath)
 *             ?.getDouble("cooldown", 1.0) ?: 1.0
 *         
 *         // 初始化管理器
 *         manager = KissManager(context.plugin, context.playerDataManager)
 *         
 *         // 注册监听器
 *         context.registerListener(KissListener(manager))
 *         
 *         // 获取消息
 *         val msg = context.getMessage(id, "enabled")
 *     }
 * }
 * ```
 */
class ModuleContext(
    /**
     * 插件主实例
     * 
     * 用于：
     * - 获取调度器（Bukkit.getGlobalRegionScheduler()）
     * - 获取日志记录器（plugin.logger）
     * - 获取数据目录（plugin.dataFolder）
     */
    val plugin: JavaPlugin,
    
    /**
     * 消息管理器
     * 
     * 用于获取 messages.yml 中的国际化消息。
     * 推荐使用 [getMessage] 便捷方法。
     */
    val messageManager: MessageManager,
    
    /**
     * 玩家数据管理器
     * 
     * 用于存储和读取玩家个人配置（开关状态、个人设置等）。
     * 数据存储在 YAML 文件中，支持 PDC 迁移。
     */
    val playerDataManager: PlayerDataManager,
    
    /**
     * 主配置文件
     * 
     * 即 config.yml 的内容。
     * 推荐使用 [getModuleConfig] 获取模块专属配置节。
     */
    val config: FileConfiguration
) {
    
    /**
     * 获取模块专属配置节
     * 
     * @param path 配置路径，通常是模块的 configPath
     * @return 配置节，如果路径不存在返回 null
     * 
     * ## 示例
     * ```kotlin
     * // config.yml:
     * // kiss:
     * //   enabled: true
     * //   cooldown: 1.0
     * 
     * val section = context.getModuleConfig("kiss")
     * val cooldown = section?.getDouble("cooldown", 1.0) ?: 1.0
     * ```
     */
    fun getModuleConfig(path: String): ConfigurationSection? {
        return config.getConfigurationSection(path)
    }
    
    /**
     * 注册事件监听器
     * 
     * 监听器会在插件禁用时自动注销，无需手动处理。
     * 
     * @param listener 要注册的监听器实例
     * 
     * ## 示例
     * ```kotlin
     * context.registerListener(KissListener(manager))
     * ```
     */
    fun registerListener(listener: Listener) {
        plugin.server.pluginManager.registerEvents(listener, plugin)
    }
    
    /**
     * 获取格式化消息
     * 
     * 从 messages.yml 获取消息并替换占位符。
     * 
     * @param module 模块名（消息文件中的一级键）
     * @param key 消息键
     * @param replacements 占位符替换，格式为 "key" to "value"
     * @return 格式化后的消息（已处理颜色代码）
     * 
     * ## 示例
     * ```kotlin
     * // messages.yml:
     * // kiss:
     * //   success: "&a你亲了 {target}！"
     * 
     * val msg = context.getMessage("kiss", "success", "target" to player.name)
     * // 结果: "§a你亲了 Steve！"
     * ```
     */
    fun getMessage(module: String, key: String, vararg replacements: Pair<String, String>): String {
        return messageManager.getModule(module, key, *replacements)
    }
    
    /**
     * 获取通用消息
     * 
     * 用于获取 common 节下的通用消息（如权限不足、玩家不存在等）。
     * 
     * @param key 消息键
     * @param replacements 占位符替换
     * @return 格式化后的消息
     */
    fun getCommonMessage(key: String, vararg replacements: Pair<String, String>): String {
        return messageManager.getCommon(key, *replacements)
    }
    
    /**
     * 检查数据库是否可用
     * 
     * 在使用 DatabaseManager 之前应先检查。
     */
    fun isDatabaseAvailable(): Boolean {
        return DatabaseManager.isInitialized()
    }
    
    /**
     * 记录信息日志
     */
    fun logInfo(message: String) {
        plugin.logger.info(message)
    }
    
    /**
     * 记录警告日志
     */
    fun logWarning(message: String) {
        plugin.logger.warning(message)
    }
    
    /**
     * 记录错误日志
     */
    fun logSevere(message: String) {
        plugin.logger.severe(message)
    }
}
