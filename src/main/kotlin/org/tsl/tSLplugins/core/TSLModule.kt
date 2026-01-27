package org.tsl.tSLplugins.core

import org.tsl.tSLplugins.SubCommandHandler

/**
 * TSL 模块接口
 * 
 * 所有功能模块都应实现此接口，以便统一管理生命周期。
 * 推荐继承 [AbstractModule] 而不是直接实现此接口。
 * 
 * ## 生命周期
 * 1. [onEnable] - 插件启动时调用，初始化模块
 * 2. [onReload] - 配置重载时调用，重新加载配置
 * 3. [onDisable] - 插件关闭时调用，清理资源
 * 
 * ## 示例
 * ```kotlin
 * class KissModule : AbstractModule() {
 *     override val id = "kiss"
 *     
 *     override fun doEnable() {
 *         // 初始化逻辑
 *     }
 * }
 * ```
 */
interface TSLModule {
    
    /**
     * 模块唯一标识符
     * 
     * 用于：
     * - 命令注册 (/tsl <id>)
     * - 配置路径查找
     * - 模块依赖解析
     * - 日志标识
     * 
     * 命名规范：小写字母，单词用连字符分隔（kebab-case）
     * 示例：kiss, chat-bubble, timed-attribute
     */
    val id: String
    
    /**
     * 配置文件中的路径前缀
     * 
     * 默认与 [id] 相同，但某些模块可能需要自定义。
     * 例如：id="chatbubble" 但 configPath="chat-bubble"
     */
    val configPath: String get() = id
    
    /**
     * 依赖的其他模块 ID 列表
     * 
     * ModuleRegistry 会确保依赖模块先于当前模块启用。
     * 如果依赖模块未启用，当前模块也会被禁用。
     * 
     * 示例：
     * ```kotlin
     * override val dependencies = listOf("player-data", "database")
     * ```
     */
    val dependencies: List<String> get() = emptyList()
    
    /**
     * 模块优先级
     * 
     * 数值越小越先加载。用于控制无依赖关系模块的加载顺序。
     * - 0-99: 核心服务（数据库、消息管理等）
     * - 100-199: 基础功能模块
     * - 200+: 普通功能模块
     * 
     * 默认值 100，大多数模块无需修改。
     */
    val priority: Int get() = 100
    
    /**
     * 模块启用时调用
     * 
     * 在此方法中：
     * - 加载配置
     * - 注册事件监听器
     * - 初始化管理器
     * - 启动定时任务
     * 
     * @param context 模块上下文，提供对全局服务的访问
     */
    fun onEnable(context: ModuleContext)
    
    /**
     * 模块禁用时调用
     * 
     * 在此方法中：
     * - 保存数据
     * - 取消定时任务
     * - 清理资源
     * - 注销监听器（通常自动处理）
     */
    fun onDisable()
    
    /**
     * 配置重载时调用
     * 
     * 在此方法中：
     * - 重新读取配置
     * - 更新缓存的配置值
     * - 重启受配置影响的任务
     * 
     * 注意：不要重新注册监听器，它们会自动保留
     */
    fun onReload()
    
    /**
     * 获取命令处理器
     * 
     * 如果模块有命令，返回 SubCommandHandler 实例。
     * 命令会自动注册到 /tsl <id> 下。
     * 
     * @return 命令处理器，无命令则返回 null
     */
    fun getCommandHandler(): SubCommandHandler? = null
    
    /**
     * 获取额外的命令处理器
     * 
     * 某些模块可能需要注册多个子命令（例如 WebBridge 模块需要 /tsl webbridge 和 /tsl bind）。
     * 返回一个 Map，键为命令名称，值为对应的处理器。
     * 
     * @return 额外命令处理器映射，默认为空
     */
    fun getAdditionalCommandHandlers(): Map<String, SubCommandHandler> = emptyMap()
    
    /**
     * 检查模块是否已启用
     * 
     * 用于运行时检查模块状态。
     * 配置中 enabled=false 的模块会返回 false。
     */
    fun isEnabled(): Boolean
    
    /**
     * 获取模块描述
     * 
     * 用于帮助命令和日志输出。
     */
    fun getDescription(): String = ""
}
