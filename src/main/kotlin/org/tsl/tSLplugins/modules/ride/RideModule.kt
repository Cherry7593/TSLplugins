package org.tsl.tSLplugins.modules.ride

import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.tsl.tSLplugins.SubCommandHandler
import org.tsl.tSLplugins.core.AbstractModule
import java.util.concurrent.ConcurrentHashMap

/**
 * Ride 模块 - 骑乘功能
 * 
 * 允许玩家右键空手骑乘生物
 * 
 * ## 功能
 * - 空手右键骑乘生物
 * - 不按 Shift 时触发（避免与 Toss 冲突）
 * - 可配置黑名单
 * 
 * ## 命令
 * - `/tsl ride` - 切换骑乘功能开关
 * - `/tsl ride toggle` - 切换骑乘功能开关
 * - `/tsl ride toggle <玩家>` - 切换他人开关（需权限）
 * 
 * ## 权限
 * - `tsl.ride.use` - 使用骑乘功能
 * - `tsl.ride.toggle.others` - 切换他人开关
 * - `tsl.ride.bypass` - 绕过黑名单限制
 */
class RideModule : AbstractModule() {

    override val id = "ride"
    override val configPath = "ride"

    // 配置项
    private var defaultEnabled: Boolean = true
    private val blacklist: MutableSet<EntityType> = ConcurrentHashMap.newKeySet()

    // Listener 实例
    private lateinit var listener: RideModuleListener

    override fun doEnable() {
        loadRideConfig()
        listener = RideModuleListener(this)
        registerListener(listener)
    }

    override fun doDisable() {
        // 清理资源（如有必要）
    }

    override fun doReload() {
        loadRideConfig()
    }

    override fun getCommandHandler(): SubCommandHandler = RideModuleCommand(this)
    
    override fun getDescription(): String = "骑乘功能"

    /**
     * 加载 Ride 配置
     */
    private fun loadRideConfig() {
        defaultEnabled = getConfigBoolean("default_enabled", true)

        val blacklistStrings = getConfigStringList("blacklist")
        blacklist.clear()
        blacklistStrings.forEach { entityName ->
            try {
                blacklist.add(EntityType.valueOf(entityName.uppercase()))
            } catch (e: IllegalArgumentException) {
                logWarning("无效的实体类型: $entityName")
            }
        }
    }

    // ============== 公开 API ==============

    /**
     * 获取默认启用状态
     */
    fun isDefaultEnabled(): Boolean = defaultEnabled

    /**
     * 检查实体是否在黑名单中
     */
    fun isEntityBlacklisted(entityType: EntityType): Boolean = blacklist.contains(entityType)

    /**
     * 检查玩家是否启用了骑乘功能
     */
    fun isPlayerEnabled(player: Player): Boolean {
        return context.playerDataManager.getRideToggle(player, defaultEnabled)
    }

    /**
     * 切换玩家的开关状态
     */
    fun togglePlayer(player: Player): Boolean {
        val currentStatus = isPlayerEnabled(player)
        val newStatus = !currentStatus
        context.playerDataManager.setRideToggle(player, newStatus)
        return newStatus
    }

    /**
     * 获取插件实例（供 Listener 使用 Folia 调度器）
     */
    fun getPlugin() = context.plugin
    
    /**
     * 获取模块消息（供 Command 使用）
     */
    fun getModuleMessage(key: String, vararg replacements: Pair<String, String>): String {
        return getMessage(key, *replacements)
    }
}
