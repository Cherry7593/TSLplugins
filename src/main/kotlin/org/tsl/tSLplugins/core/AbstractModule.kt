package org.tsl.tSLplugins.core

import org.bukkit.event.HandlerList
import org.bukkit.event.Listener

/**
 * 模块抽象基类
 * 
 * 提供 [TSLModule] 接口的默认实现，简化模块开发。
 * 大多数模块应继承此类而不是直接实现 TSLModule 接口。
 * 
 * ## 核心功能
 * - 自动加载配置中的 enabled 状态
 * - 提供模块上下文访问
 * - 管理监听器注册和注销
 * - 提供便捷的消息获取方法
 * 
 * ## 子类需要实现
 * - [id] - 模块唯一标识
 * - [doEnable] - 模块启用逻辑
 * - [doDisable] - 模块禁用逻辑（可选）
 * 
 * ## 示例
 * ```kotlin
 * class KissModule : AbstractModule() {
 *     override val id = "kiss"
 *     
 *     private lateinit var manager: KissManager
 *     private lateinit var listener: KissListener
 *     
 *     override fun doEnable() {
 *         val cooldown = getConfigDouble("cooldown", 1.0)
 *         manager = KissManager(context.plugin, context.playerDataManager, cooldown)
 *         listener = KissListener(manager)
 *         registerListener(listener)
 *     }
 *     
 *     override fun doDisable() {
 *         manager.cleanup()
 *     }
 *     
 *     override fun getCommandHandler() = KissCommand(manager)
 * }
 * ```
 */
abstract class AbstractModule : TSLModule {
    
    /**
     * 模块上下文
     * 
     * 在 [onEnable] 后可用，提供对全局服务的访问。
     * 在 [doEnable] 中可以安全使用。
     */
    protected lateinit var context: ModuleContext
        private set
    
    /**
     * 模块是否已启用
     * 
     * 从配置文件的 `<configPath>.enabled` 读取。
     */
    protected var enabled: Boolean = false
        private set
    
    /**
     * 已注册的监听器列表
     * 
     * 用于在模块禁用时自动注销。
     */
    private val registeredListeners = mutableListOf<Listener>()
    
    // ==================== 生命周期方法 ====================
    
    /**
     * 模块启用入口
     * 
     * 不要重写此方法，而是重写 [doEnable]。
     */
    final override fun onEnable(context: ModuleContext) {
        this.context = context
        loadConfig()
        
        if (enabled) {
            try {
                doEnable()
                context.logInfo("[$id] 模块已启用")
            } catch (e: Exception) {
                context.logSevere("[$id] 模块启用失败: ${e.message}")
                e.printStackTrace()
                enabled = false
            }
        } else {
            context.logInfo("[$id] 模块未启用（配置中 enabled=false）")
        }
    }
    
    /**
     * 模块禁用入口
     * 
     * 不要重写此方法，而是重写 [doDisable]。
     */
    final override fun onDisable() {
        if (enabled) {
            try {
                doDisable()
                context.logInfo("[$id] 模块已禁用")
            } catch (e: Exception) {
                context.logWarning("[$id] 模块禁用时出错: ${e.message}")
            }
        }
        
        // 注销所有监听器
        unregisterAllListeners()
    }
    
    /**
     * 配置重载入口
     * 
     * 默认行为是重新加载配置。
     * 如需额外逻辑，重写 [doReload]。
     */
    override fun onReload() {
        val wasEnabled = enabled
        loadConfig()
        
        // 状态变化处理
        when {
            !wasEnabled && enabled -> {
                // 从禁用变为启用
                context.logInfo("[$id] 模块已启用（重载后）")
                doEnable()
            }
            wasEnabled && !enabled -> {
                // 从启用变为禁用
                context.logInfo("[$id] 模块已禁用（重载后）")
                doDisable()
                unregisterAllListeners()
            }
            enabled -> {
                // 保持启用，执行重载逻辑
                doReload()
            }
        }
    }
    
    override fun isEnabled(): Boolean = enabled
    
    // ==================== 子类需要实现的方法 ====================
    
    /**
     * 模块启用时的具体逻辑
     * 
     * 在此方法中：
     * - 初始化管理器
     * - 注册监听器（使用 [registerListener]）
     * - 启动定时任务
     * 
     * 此时 [context] 和配置已可用。
     */
    protected abstract fun doEnable()
    
    /**
     * 模块禁用时的具体逻辑
     * 
     * 在此方法中：
     * - 保存数据
     * - 取消定时任务
     * - 清理资源
     * 
     * 监听器会自动注销，无需手动处理。
     */
    protected open fun doDisable() {}
    
    /**
     * 配置重载时的具体逻辑
     * 
     * 默认实现为空。如果模块需要在重载时执行额外操作（如重启任务），
     * 则重写此方法。
     * 
     * 注意：配置已在此方法调用前重新加载。
     */
    protected open fun doReload() {}
    
    // ==================== 配置加载 ====================
    
    /**
     * 加载模块配置
     * 
     * 默认从 `<configPath>.enabled` 读取启用状态。
     * 子类可重写以加载更多配置项。
     * 
     * 重写示例：
     * ```kotlin
     * override fun loadConfig() {
     *     super.loadConfig() // 先调用父类方法加载 enabled
     *     cooldown = getConfigDouble("cooldown", 1.0)
     *     maxCount = getConfigInt("max-count", 10)
     * }
     * ```
     */
    protected open fun loadConfig() {
        enabled = getConfigBoolean("enabled", false)
    }
    
    // ==================== 配置便捷方法 ====================
    
    /**
     * 获取模块配置中的布尔值
     */
    protected fun getConfigBoolean(key: String, default: Boolean = false): Boolean {
        return context.getModuleConfig(configPath)?.getBoolean(key, default) ?: default
    }
    
    /**
     * 获取模块配置中的整数值
     */
    protected fun getConfigInt(key: String, default: Int = 0): Int {
        return context.getModuleConfig(configPath)?.getInt(key, default) ?: default
    }
    
    /**
     * 获取模块配置中的长整数值
     */
    protected fun getConfigLong(key: String, default: Long = 0L): Long {
        return context.getModuleConfig(configPath)?.getLong(key, default) ?: default
    }
    
    /**
     * 获取模块配置中的浮点数值
     */
    protected fun getConfigDouble(key: String, default: Double = 0.0): Double {
        return context.getModuleConfig(configPath)?.getDouble(key, default) ?: default
    }
    
    /**
     * 获取模块配置中的字符串值
     */
    protected fun getConfigString(key: String, default: String = ""): String {
        return context.getModuleConfig(configPath)?.getString(key, default) ?: default
    }
    
    /**
     * 获取模块配置中的字符串列表
     */
    protected fun getConfigStringList(key: String): List<String> {
        return context.getModuleConfig(configPath)?.getStringList(key) ?: emptyList()
    }
    
    // ==================== 监听器管理 ====================
    
    /**
     * 注册事件监听器
     * 
     * 监听器会在模块禁用时自动注销。
     * 
     * @param listener 要注册的监听器
     */
    protected fun registerListener(listener: Listener) {
        context.registerListener(listener)
        registeredListeners.add(listener)
    }
    
    /**
     * 注销所有已注册的监听器
     */
    private fun unregisterAllListeners() {
        registeredListeners.forEach { listener ->
            HandlerList.unregisterAll(listener)
        }
        registeredListeners.clear()
    }
    
    // ==================== 消息便捷方法 ====================
    
    /**
     * 获取模块消息
     * 
     * 从 messages.yml 的 `<id>.<key>` 路径获取消息。
     * 
     * @param key 消息键
     * @param replacements 占位符替换
     * @return 格式化后的消息
     */
    protected fun getMessage(key: String, vararg replacements: Pair<String, String>): String {
        return context.getMessage(id, key, *replacements)
    }
    
    /**
     * 获取通用消息
     * 
     * 从 messages.yml 的 `common.<key>` 路径获取消息。
     */
    protected fun getCommonMessage(key: String, vararg replacements: Pair<String, String>): String {
        return context.getCommonMessage(key, *replacements)
    }
    
    // ==================== 日志便捷方法 ====================
    
    /**
     * 记录模块信息日志
     */
    protected fun logInfo(message: String) {
        context.logInfo("[$id] $message")
    }
    
    /**
     * 记录模块警告日志
     */
    protected fun logWarning(message: String) {
        context.logWarning("[$id] $message")
    }
    
    /**
     * 记录模块错误日志
     */
    protected fun logSevere(message: String) {
        context.logSevere("[$id] $message")
    }
}
