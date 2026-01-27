package org.tsl.tSLplugins.core

import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.plugin.java.JavaPlugin
import org.tsl.tSLplugins.service.DatabaseManager
import org.tsl.tSLplugins.service.MessageManager
import org.tsl.tSLplugins.service.PlayerDataManager
import org.tsl.tSLplugins.SubCommandHandler
import org.tsl.tSLplugins.TSLCommand

/**
 * 模块注册器
 * 
 * 负责管理所有 TSLModule 的注册、启用、禁用和重载。
 * 
 * ## 核心职责
 * - 模块注册和生命周期管理
 * - 依赖解析和加载顺序
 * - 命令自动注册
 * - 统一的 reload 入口
 * 
 * ## 使用示例
 * ```kotlin
 * // 在 TSLplugins.onEnable() 中
 * val registry = ModuleRegistry(this, messageManager, playerDataManager)
 * 
 * // 注册模块
 * registry.register(KissModule())
 * registry.register(FreezeModule())
 * registry.register(LandmarkModule())
 * 
 * // 启用所有模块
 * registry.enableAll()
 * 
 * // 注册命令到 TSLCommand
 * registry.registerCommands(tslCommand)
 * ```
 * 
 * ## 加载顺序
 * 模块按以下规则排序加载：
 * 1. 依赖关系（被依赖的模块先加载）
 * 2. 优先级（数值小的先加载）
 * 3. 注册顺序（同优先级按注册顺序）
 */
class ModuleRegistry(
    private val plugin: JavaPlugin,
    private val messageManager: MessageManager,
    private val playerDataManager: PlayerDataManager
) {
    
    /**
     * 已注册的模块（按 ID 索引）
     */
    private val modules = mutableMapOf<String, TSLModule>()
    
    /**
     * 模块加载顺序（依赖解析后的顺序）
     */
    private val loadOrder = mutableListOf<String>()
    
    /**
     * 是否已完成初始化
     */
    private var initialized = false
    
    /**
     * 注册模块
     * 
     * 模块会在 [enableAll] 调用时统一启用。
     * 重复注册相同 ID 的模块会覆盖之前的注册。
     * 
     * @param module 要注册的模块
     * @throws IllegalStateException 如果在 enableAll 后调用
     */
    fun register(module: TSLModule) {
        if (initialized) {
            throw IllegalStateException("不能在初始化完成后注册新模块")
        }
        
        val id = module.id
        if (modules.containsKey(id)) {
            plugin.logger.warning("[ModuleRegistry] 模块 '$id' 已存在，将被覆盖")
        }
        
        modules[id] = module
        plugin.logger.info("[ModuleRegistry] 注册模块: $id")
    }
    
    /**
     * 批量注册模块
     * 
     * @param moduleList 要注册的模块列表
     */
    fun registerAll(vararg moduleList: TSLModule) {
        moduleList.forEach { register(it) }
    }
    
    /**
     * 启用所有已注册的模块
     * 
     * 按依赖顺序和优先级启用模块。
     * 只应在插件启动时调用一次。
     * 
     * @return 成功启用的模块数量
     */
    fun enableAll(): Int {
        if (initialized) {
            plugin.logger.warning("[ModuleRegistry] enableAll() 已经调用过")
            return 0
        }
        
        // 解析依赖并确定加载顺序
        resolveDependencies()
        
        // 创建模块上下文
        val context = createContext()
        
        // 按顺序启用模块
        var enabledCount = 0
        loadOrder.forEach { moduleId ->
            val module = modules[moduleId]
            if (module != null) {
                try {
                    module.onEnable(context)
                    if (module.isEnabled()) {
                        enabledCount++
                    }
                } catch (e: Exception) {
                    plugin.logger.severe("[ModuleRegistry] 启用模块 '$moduleId' 失败: ${e.message}")
                    e.printStackTrace()
                }
            }
        }
        
        initialized = true
        plugin.logger.info("[ModuleRegistry] 已启用 $enabledCount/${modules.size} 个模块")
        
        return enabledCount
    }
    
    /**
     * 禁用所有模块
     * 
     * 按启用的逆序禁用模块。
     * 只应在插件关闭时调用。
     */
    fun disableAll() {
        if (!initialized) {
            return
        }
        
        // 逆序禁用
        loadOrder.reversed().forEach { moduleId ->
            val module = modules[moduleId]
            if (module != null && module.isEnabled()) {
                try {
                    module.onDisable()
                } catch (e: Exception) {
                    plugin.logger.warning("[ModuleRegistry] 禁用模块 '$moduleId' 时出错: ${e.message}")
                }
            }
        }
        
        plugin.logger.info("[ModuleRegistry] 所有模块已禁用")
    }
    
    /**
     * 重载所有模块
     * 
     * 刷新配置并通知所有模块重新加载。
     * 
     * @return 重载后启用的模块数量
     */
    fun reloadAll(): Int {
        if (!initialized) {
            plugin.logger.warning("[ModuleRegistry] 模块尚未初始化，无法重载")
            return 0
        }
        
        // 重新加载配置文件
        plugin.reloadConfig()
        
        // 重载所有模块
        var enabledCount = 0
        loadOrder.forEach { moduleId ->
            val module = modules[moduleId]
            if (module != null) {
                try {
                    // 对于 AbstractModule，需要更新 context
                    if (module is AbstractModule) {
                        // AbstractModule 的 onReload 会重新加载配置
                        module.onReload()
                    } else {
                        module.onReload()
                    }
                    
                    if (module.isEnabled()) {
                        enabledCount++
                    }
                } catch (e: Exception) {
                    plugin.logger.warning("[ModuleRegistry] 重载模块 '$moduleId' 时出错: ${e.message}")
                }
            }
        }
        
        plugin.logger.info("[ModuleRegistry] 重载完成，$enabledCount/${modules.size} 个模块已启用")
        
        return enabledCount
    }
    
    /**
     * 注册所有已启用模块的命令到 TSLCommand
     * 
     * 注意：只为已启用的模块注册命令，避免访问未初始化的属性
     * 
     * @param dispatcher TSLCommand 命令分发器
     */
    fun registerCommands(dispatcher: TSLCommand) {
        modules.values.forEach { module ->
            // 只为已启用的模块注册命令
            if (!module.isEnabled()) {
                return@forEach
            }
            
            try {
                // 注册主命令
                val handler = module.getCommandHandler()
                if (handler != null) {
                    dispatcher.registerSubCommand(module.id, handler)
                    plugin.logger.info("[ModuleRegistry] 注册命令: /tsl ${module.id}")
                }
                
                // 注册额外命令
                val additionalHandlers = module.getAdditionalCommandHandlers()
                additionalHandlers.forEach { (name, additionalHandler) ->
                    dispatcher.registerSubCommand(name, additionalHandler)
                    plugin.logger.info("[ModuleRegistry] 注册额外命令: /tsl $name (来自模块 ${module.id})")
                }
            } catch (e: Exception) {
                plugin.logger.warning("[ModuleRegistry] 注册命令 '${module.id}' 失败: ${e.message}")
            }
        }
    }
    
    /**
     * 获取指定模块
     * 
     * @param id 模块 ID
     * @return 模块实例，如果不存在返回 null
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : TSLModule> getModule(id: String): T? {
        return modules[id] as? T
    }
    
    /**
     * 检查模块是否已注册
     */
    fun hasModule(id: String): Boolean {
        return modules.containsKey(id)
    }
    
    /**
     * 获取所有已注册的模块 ID
     */
    fun getModuleIds(): Set<String> {
        return modules.keys.toSet()
    }
    
    /**
     * 获取所有已启用的模块
     */
    fun getEnabledModules(): List<TSLModule> {
        return modules.values.filter { it.isEnabled() }
    }
    
    /**
     * 获取模块数量
     */
    fun getModuleCount(): Int = modules.size
    
    /**
     * 获取已启用的模块数量
     */
    fun getEnabledCount(): Int = modules.values.count { it.isEnabled() }
    
    // ==================== 私有方法 ====================
    
    /**
     * 创建模块上下文
     */
    private fun createContext(): ModuleContext {
        return ModuleContext(
            plugin = plugin,
            messageManager = messageManager,
            playerDataManager = playerDataManager,
            config = plugin.config
        )
    }
    
    /**
     * 解析依赖关系并确定加载顺序
     * 
     * 使用拓扑排序确保依赖模块先于被依赖模块加载。
     */
    private fun resolveDependencies() {
        loadOrder.clear()
        
        // 构建依赖图
        val inDegree = mutableMapOf<String, Int>()
        val dependents = mutableMapOf<String, MutableList<String>>()
        
        modules.keys.forEach { id ->
            inDegree[id] = 0
            dependents[id] = mutableListOf()
        }
        
        // 计算入度
        modules.forEach { (id, module) ->
            module.dependencies.forEach { depId ->
                if (modules.containsKey(depId)) {
                    inDegree[id] = (inDegree[id] ?: 0) + 1
                    dependents[depId]?.add(id)
                } else {
                    plugin.logger.warning("[ModuleRegistry] 模块 '$id' 依赖的模块 '$depId' 未注册")
                }
            }
        }
        
        // 按优先级和入度排序的队列
        val queue = modules.keys
            .filter { (inDegree[it] ?: 0) == 0 }
            .sortedWith(compareBy({ modules[it]?.priority ?: 100 }, { it }))
            .toMutableList()
        
        // 拓扑排序
        while (queue.isNotEmpty()) {
            val current = queue.removeAt(0)
            loadOrder.add(current)
            
            // 减少依赖当前模块的模块的入度
            dependents[current]?.forEach { depId ->
                val newInDegree = (inDegree[depId] ?: 1) - 1
                inDegree[depId] = newInDegree
                
                if (newInDegree == 0) {
                    // 按优先级插入队列
                    val priority = modules[depId]?.priority ?: 100
                    val insertIndex = queue.indexOfFirst { 
                        (modules[it]?.priority ?: 100) > priority 
                    }
                    if (insertIndex == -1) {
                        queue.add(depId)
                    } else {
                        queue.add(insertIndex, depId)
                    }
                }
            }
        }
        
        // 检查循环依赖
        if (loadOrder.size != modules.size) {
            val missing = modules.keys - loadOrder.toSet()
            plugin.logger.severe("[ModuleRegistry] 检测到循环依赖，以下模块无法加载: $missing")
        }
        
        plugin.logger.info("[ModuleRegistry] 模块加载顺序: ${loadOrder.joinToString(" -> ")}")
    }
}
