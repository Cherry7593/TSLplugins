package org.tsl.tSLplugins.modules.toss

import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.tsl.tSLplugins.SubCommandHandler
import org.tsl.tSLplugins.core.AbstractModule
import java.util.concurrent.ConcurrentHashMap

/**
 * Toss 模块 - 生物举起功能
 * 
 * 允许玩家举起生物并投掷，支持叠罗汉效果
 * 
 * ## 功能
 * - 按住 Shift + 右键举起生物
 * - 按住 Shift + 左键投掷生物
 * - 支持叠罗汉（多个生物堆叠）
 * - 可配置投掷速度、最大堆叠数量、黑名单
 * 
 * ## 命令
 * - `/tsl toss` - 显示状态
 * - `/tsl toss toggle` - 切换开关
 * - `/tsl toss velocity <数值>` - 设置投掷速度
 * 
 * ## 权限
 * - `tsl.toss.use` - 使用举起功能
 * - `tsl.toss.velocity` - 设置投掷速度
 * - `tsl.toss.velocity.bypass` - 无视速度限制
 * - `tsl.toss.bypass` - 绕过黑名单限制
 */
class TossModule : AbstractModule() {

    override val id = "toss"
    override val configPath = "toss"

    // 配置项
    private var showMessages: Boolean = false
    private var maxLiftCount: Int = 3
    private var defaultEnabled: Boolean = true
    private var throwVelocityMin: Double = 1.0
    private var throwVelocityMax: Double = 3.0
    private val blacklist: MutableSet<EntityType> = ConcurrentHashMap.newKeySet()

    // Listener 实例
    private lateinit var listener: TossModuleListener

    override fun doEnable() {
        loadTossConfig()
        listener = TossModuleListener(this)
        registerListener(listener)
    }

    override fun doDisable() {
        // 清理资源（如有必要）
    }

    override fun doReload() {
        loadTossConfig()
    }

    override fun getCommandHandler(): SubCommandHandler = TossModuleCommand(this)
    
    override fun getDescription(): String = "生物举起功能"

    /**
     * 加载 Toss 配置
     */
    private fun loadTossConfig() {
        showMessages = getConfigBoolean("show_messages", false)
        maxLiftCount = getConfigInt("max_lift_count", 3)
        defaultEnabled = getConfigBoolean("default_enabled", true)
        throwVelocityMin = getConfigDouble("throw_velocity.min", 1.0)
        throwVelocityMax = getConfigDouble("throw_velocity.max", 3.0)

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
     * 是否显示消息
     */
    fun isShowMessages(): Boolean = showMessages

    /**
     * 获取最大举起数量
     */
    fun getMaxLiftCount(): Int = maxLiftCount

    /**
     * 获取默认启用状态
     */
    fun isDefaultEnabled(): Boolean = defaultEnabled

    /**
     * 获取投掷速度最小值
     */
    fun getThrowVelocityMin(): Double = throwVelocityMin

    /**
     * 获取投掷速度最大值
     */
    fun getThrowVelocityMax(): Double = throwVelocityMax

    /**
     * 检查实体是否在黑名单中
     */
    fun isEntityBlacklisted(entityType: EntityType): Boolean = blacklist.contains(entityType)

    /**
     * 检查玩家是否启用了举起功能
     */
    fun isPlayerEnabled(player: Player): Boolean {
        return context.playerDataManager.getTossToggle(player, defaultEnabled)
    }

    /**
     * 切换玩家的开关状态
     */
    fun togglePlayer(player: Player): Boolean {
        val currentStatus = isPlayerEnabled(player)
        val newStatus = !currentStatus
        context.playerDataManager.setTossToggle(player, newStatus)
        return newStatus
    }

    /**
     * 获取玩家的投掷速度
     */
    fun getPlayerThrowVelocity(player: Player): Double {
        return context.playerDataManager.getTossVelocity(player, throwVelocityMin)
    }

    /**
     * 设置玩家的投掷速度（受配置限制）
     */
    fun setPlayerThrowVelocity(player: Player, velocity: Double): Boolean {
        if (velocity < throwVelocityMin || velocity > throwVelocityMax) {
            return false
        }
        context.playerDataManager.setTossVelocity(player, velocity)
        return true
    }

    /**
     * 设置玩家的投掷速度（不受配置限制，用于 OP/管理员）
     */
    fun setPlayerThrowVelocityUnrestricted(player: Player, velocity: Double) {
        val clampedVelocity = velocity.coerceIn(0.0, 10.0)
        context.playerDataManager.setTossVelocity(player, clampedVelocity)
    }

    /**
     * 获取插件实例（供 Listener 使用 Folia 调度器）
     */
    fun getPlugin() = context.plugin
    
    /**
     * 获取模块消息（供 Command 和 Listener 使用）
     */
    fun getModuleMessage(key: String, vararg replacements: Pair<String, String>): String {
        return getMessage(key, *replacements)
    }
}
