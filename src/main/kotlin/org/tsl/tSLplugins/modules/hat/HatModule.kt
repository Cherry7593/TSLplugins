package org.tsl.tSLplugins.modules.hat

import org.bukkit.Material
import org.tsl.tSLplugins.SubCommandHandler
import org.tsl.tSLplugins.core.AbstractModule
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 头戴物品模块
 * 
 * 允许玩家将手中的物品戴在头上。
 * 
 * ## 功能
 * - 将手持物品戴在头上
 * - 支持物品黑名单
 * - 支持冷却时间
 * - 自动处理头盔交换
 * 
 * ## 命令
 * - `/tsl hat` - 将手中物品戴在头上
 * 
 * ## 权限
 * - `tsl.hat.use` - 使用 hat 命令
 * 
 * ## 配置
 * ```yaml
 * hat:
 *   enabled: true
 *   cooldown: 0.0
 *   blacklist:
 *     - COMMAND_BLOCK
 * ```
 */
class HatModule : AbstractModule() {
    
    override val id = "hat"
    override val configPath = "hat"
    
    // 配置项
    private var blacklistedMaterials: Set<Material> = emptySet()
    private var cooldown: Long = 0L
    
    // 冷却记录
    private val cooldowns: MutableMap<UUID, Long> = ConcurrentHashMap()
    
    override fun loadConfig() {
        super.loadConfig()
        cooldown = (getConfigDouble("cooldown", 0.0) * 1000).toLong()
        
        val blacklistStrings = getConfigStringList("blacklist")
        blacklistedMaterials = blacklistStrings.mapNotNull { materialName ->
            try {
                Material.valueOf(materialName.uppercase())
            } catch (e: IllegalArgumentException) {
                logWarning("无效的物品类型: $materialName")
                null
            }
        }.toSet()
    }
    
    override fun doEnable() {
        // Hat 模块不需要监听器，只有命令
        if (blacklistedMaterials.isNotEmpty()) {
            logInfo("黑名单物品数量: ${blacklistedMaterials.size}")
        }
    }
    
    override fun doDisable() {
        cooldowns.clear()
    }
    
    override fun getCommandHandler(): SubCommandHandler {
        return HatModuleCommand(this)
    }
    
    override fun getDescription(): String = "将手中物品戴在头上"
    
    // ==================== 功能方法 ====================
    
    /**
     * 检查物品是否被禁止
     */
    fun isBlacklisted(material: Material): Boolean = blacklistedMaterials.contains(material)
    
    /**
     * 获取冷却时间（毫秒）
     */
    fun getCooldown(): Long = cooldown
    
    /**
     * 检查玩家是否在冷却中
     * 
     * @param uuid 玩家 UUID
     * @return 如果在冷却中返回剩余时间（秒），否则返回 null
     */
    fun checkCooldown(uuid: UUID): Double? {
        if (cooldown <= 0) return null
        
        val currentTime = System.currentTimeMillis()
        val lastUsed = cooldowns[uuid] ?: return null
        
        val elapsed = currentTime - lastUsed
        if (elapsed < cooldown) {
            return ((cooldown - elapsed) / 1000.0).coerceAtLeast(0.1)
        }
        
        return null
    }
    
    /**
     * 设置玩家冷却
     */
    fun setCooldown(uuid: UUID) {
        if (cooldown > 0) {
            cooldowns[uuid] = System.currentTimeMillis()
        }
    }
    
    /**
     * 清理玩家冷却数据
     */
    fun cleanupPlayer(uuid: UUID) {
        cooldowns.remove(uuid)
    }
    
    /**
     * 获取模块消息
     */
    fun getModuleMessage(key: String, vararg replacements: Pair<String, String>): String {
        return getMessage(key, *replacements)
    }
    
    /**
     * 获取插件实例（用于 Folia 调度器）
     */
    fun getPlugin() = context.plugin
}
