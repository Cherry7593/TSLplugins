package org.tsl.tSLplugins.modules.freeze

import org.bukkit.Bukkit
import org.tsl.tSLplugins.SubCommandHandler
import org.tsl.tSLplugins.core.AbstractModule
import org.tsl.tSLplugins.core.ModuleContext
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 玩家冻结模块
 * 
 * 提供冻结玩家的功能，被冻结的玩家无法移动、交互、使用命令等。
 * 
 * ## 功能
 * - 冻结/解冻玩家
 * - 支持计时冻结（自动过期）
 * - 永久冻结
 * - 被冻结玩家无法执行任何操作
 * 
 * ## 命令
 * - `/tsl freeze <玩家> [时间]` - 冻结/解冻玩家
 * - `/tsl freeze list` - 列出被冻结的玩家
 * 
 * ## 权限
 * - `tsl.freeze.use` - 使用冻结命令
 * - `tsl.freeze.bypass` - 绕过冻结（不能被冻结）
 * 
 * ## 配置
 * ```yaml
 * freeze:
 *   enabled: true
 * ```
 */
class FreezeModule : AbstractModule() {
    
    override val id = "freeze"
    override val configPath = "freeze"
    
    // 冻结的玩家（UUID -> 过期时间戳，-1表示永久）
    private val frozenPlayers: MutableMap<UUID, Long> = ConcurrentHashMap()
    
    // 定时任务 ID
    private var expirationTask: io.papermc.paper.threadedregions.scheduler.ScheduledTask? = null
    
    private lateinit var listener: FreezeModuleListener
    
    override fun doEnable() {
        // 注册监听器
        listener = FreezeModuleListener(context.plugin, this)
        registerListener(listener)
        
        // 启动过期检查任务
        startExpirationCheck()
    }
    
    override fun doDisable() {
        // 取消定时任务
        expirationTask?.cancel()
        expirationTask = null
        
        // 清空冻结列表（可选：保留到下次启动）
        // frozenPlayers.clear()
    }
    
    override fun doReload() {
        // 重启过期检查任务（如果有配置变化）
        expirationTask?.cancel()
        startExpirationCheck()
    }
    
    override fun getCommandHandler(): SubCommandHandler {
        return FreezeModuleCommand(this)
    }
    
    override fun getDescription(): String = "冻结玩家功能"
    
    // ==================== 冻结管理 ====================
    
    /**
     * 冻结玩家
     * 
     * @param uuid 玩家 UUID
     * @param duration 持续时间（秒），-1 表示永久
     */
    fun freezePlayer(uuid: UUID, duration: Int = -1) {
        val expireTime = if (duration > 0) {
            System.currentTimeMillis() + (duration * 1000L)
        } else {
            -1L
        }
        frozenPlayers[uuid] = expireTime
    }
    
    /**
     * 解冻玩家
     * 
     * @param uuid 玩家 UUID
     * @return 是否成功解冻（true = 之前被冻结，false = 之前未被冻结）
     */
    fun unfreezePlayer(uuid: UUID): Boolean {
        return frozenPlayers.remove(uuid) != null
    }
    
    /**
     * 检查玩家是否被冻结
     * 
     * @param uuid 玩家 UUID
     * @return 是否被冻结
     */
    fun isFrozen(uuid: UUID): Boolean {
        val expireTime = frozenPlayers[uuid] ?: return false
        
        // 检查是否过期
        if (expireTime > 0 && System.currentTimeMillis() > expireTime) {
            frozenPlayers.remove(uuid)
            return false
        }
        
        return true
    }
    
    /**
     * 获取被冻结的玩家列表
     * 
     * @return UUID 到过期时间的映射（-1 表示永久）
     */
    fun getFrozenPlayers(): Map<UUID, Long> {
        return frozenPlayers.toMap()
    }
    
    /**
     * 获取剩余冻结时间
     * 
     * @param uuid 玩家 UUID
     * @return 剩余时间（秒），-1 表示永久，0 表示已过期或未被冻结
     */
    fun getRemainingTime(uuid: UUID): Int {
        val expireTime = frozenPlayers[uuid] ?: return 0
        
        if (expireTime < 0) {
            return -1 // 永久冻结
        }
        
        val remaining = (expireTime - System.currentTimeMillis()) / 1000
        return if (remaining > 0) remaining.toInt() else 0
    }
    
    /**
     * 获取模块消息
     */
    fun getModuleMessage(key: String, vararg replacements: Pair<String, String>): String {
        return getMessage(key, *replacements)
    }
    
    // ==================== 私有方法 ====================
    
    /**
     * 启动过期检查任务
     */
    private fun startExpirationCheck() {
        expirationTask = Bukkit.getGlobalRegionScheduler().runAtFixedRate(context.plugin, { _ ->
            val currentTime = System.currentTimeMillis()
            val expired = mutableListOf<UUID>()
            
            frozenPlayers.forEach { (uuid, expireTime) ->
                if (expireTime > 0 && currentTime > expireTime) {
                    expired.add(uuid)
                }
            }
            
            // 移除过期的冻结并通知玩家
            expired.forEach { uuid ->
                frozenPlayers.remove(uuid)
                val player = Bukkit.getPlayer(uuid)
                if (player != null && player.isOnline) {
                    player.sendMessage(getMessage("expired"))
                }
            }
        }, 20L, 20L) // 延迟1秒，每秒执行一次
    }
}
