package org.tsl.tSLplugins.modules.title

import net.luckperms.api.LuckPerms
import net.luckperms.api.LuckPermsProvider
import net.luckperms.api.model.user.User
import net.luckperms.api.node.types.PrefixNode
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 称号管理器
 * 
 * 负责：
 * - 通过 LuckPerms API 设置/清除玩家称号
 * - 缓存玩家称号数据
 * - 处理称号相关的 WebSocket 事件
 */
class TitleManager(private val plugin: Plugin) {

    private var enabled = false
    private var luckPerms: LuckPerms? = null
    
    // 称号优先级（LP 权重）
    private var titlePriority = 100
    
    // 玩家上线延迟请求称号（tick）
    private var joinDelay = 20L
    
    // 称号缓存 (UUID -> TitleData)
    private val titleCache = ConcurrentHashMap<UUID, TitleData>()
    
    // 待处理的兑换请求回调 (requestId -> Player)
    private val pendingRedeemRequests = ConcurrentHashMap<String, Player>()

    /**
     * 初始化称号管理器
     */
    fun initialize() {
        val config = plugin.config.getConfigurationSection("title")
        
        if (config == null) {
            plugin.logger.info("[Title] 配置未找到，模块未启用")
            return
        }
        
        enabled = config.getBoolean("enabled", false)
        
        if (!enabled) {
            plugin.logger.info("[Title] 模块未启用")
            return
        }
        
        // 检查 LuckPerms 是否可用
        if (Bukkit.getPluginManager().getPlugin("LuckPerms") == null) {
            plugin.logger.warning("[Title] LuckPerms 未安装，称号功能无法使用")
            enabled = false
            return
        }
        
        try {
            luckPerms = LuckPermsProvider.get()
        } catch (e: Exception) {
            plugin.logger.warning("[Title] 无法获取 LuckPerms API: ${e.message}")
            enabled = false
            return
        }
        
        // 读取配置
        titlePriority = config.getInt("luckperms-priority", 100)
        joinDelay = config.getLong("join-delay", 20L)
        
        plugin.logger.info("[Title] 模块已启用 (LP 权重: $titlePriority)")
    }

    /**
     * 关闭称号管理器
     */
    fun shutdown() {
        titleCache.clear()
        pendingRedeemRequests.clear()
        if (enabled) {
            plugin.logger.info("[Title] 模块已关闭")
        }
        enabled = false
        luckPerms = null
    }

    /**
     * 重新加载配置
     * 支持运行时动态启用/禁用
     */
    fun reload() {
        val wasEnabled = enabled
        
        // 先关闭
        shutdown()
        
        // 重新初始化
        initialize()
        
        // 状态变化日志
        if (!wasEnabled && enabled) {
            plugin.logger.info("[Title] 模块从禁用变为启用")
        } else if (wasEnabled && !enabled) {
            plugin.logger.info("[Title] 模块从启用变为禁用")
        }
    }

    /**
     * 检查模块是否启用
     */
    fun isEnabled(): Boolean = enabled

    /**
     * 获取玩家上线延迟
     */
    fun getJoinDelay(): Long = joinDelay

    /**
     * 设置玩家称号
     * 
     * @param uuid 玩家 UUID
     * @param title 称号内容（null 则清除）
     * @param tier 付费等级
     */
    fun setPlayerTitle(uuid: UUID, title: String?, tier: Int = 0) {
        val lp = luckPerms ?: return
        
        // 异步加载用户（玩家可能不在线）
        lp.userManager.loadUser(uuid).thenAcceptAsync { user ->
            if (user == null) {
                plugin.logger.warning("[Title] 无法加载用户: $uuid")
                return@thenAcceptAsync
            }
            
            // 1. 清除旧的 TSL 称号
            clearTslTitle(user)
            
            // 2. 设置新称号（统一使用 prefix）
            if (!title.isNullOrEmpty()) {
                val node = PrefixNode.builder(title, titlePriority).build()
                user.data().add(node)
            }
            
            // 3. 保存
            lp.userManager.saveUser(user)
            
            // 4. 更新缓存
            if (!title.isNullOrEmpty()) {
                titleCache[uuid] = TitleData(title, tier)
            } else {
                titleCache.remove(uuid)
            }
            
            val playerName = Bukkit.getOfflinePlayer(uuid).name ?: uuid.toString()
            plugin.logger.info("[Title] 已更新玩家 $playerName 的称号: ${title ?: "(清除)"}")
        }
    }

    /**
     * 清除玩家的 TSL 称号
     */
    private fun clearTslTitle(user: User) {
        // 清除权重等于 titlePriority 的前缀（我们设置的）
        user.data().clear { node ->
            node is PrefixNode && node.priority == titlePriority
        }
    }

    /**
     * 处理称号更新事件（来自 WebSocket）
     * 注意：position 参数已废弃，统一使用 prefix
     */
    fun handleTitleUpdate(playerUuid: String, playerName: String?, title: String?, position: String, tier: Int) {
        try {
            val uuid = UUID.fromString(playerUuid)
            setPlayerTitle(uuid, title, tier)
            
            // 如果玩家在线，发送通知
            val player = Bukkit.getPlayer(uuid)
            if (player != null && player.isOnline) {
                if (!title.isNullOrEmpty()) {
                    player.sendMessage("§a[称号] §7你的称号已更新为: $title")
                } else {
                    player.sendMessage("§a[称号] §7你的称号已被清除")
                }
            }
        } catch (e: Exception) {
            plugin.logger.warning("[Title] 处理称号更新失败: ${e.message}")
        }
    }

    /**
     * 处理获取称号响应（来自 WebSocket）
     * 注意：position 参数已废弃，统一使用 prefix
     */
    fun handleGetTitleResponse(playerUuid: String, title: String?, position: String?, tier: Int, found: Boolean) {
        if (!found || title == null) {
            return
        }
        
        try {
            val uuid = UUID.fromString(playerUuid)
            setPlayerTitle(uuid, title, tier)
        } catch (e: Exception) {
            plugin.logger.warning("[Title] 处理称号响应失败: ${e.message}")
        }
    }

    /**
     * 处理兑换码响应（来自 WebSocket）
     */
    fun handleRedeemResponse(requestId: String, success: Boolean, message: String, grantedTier: Int?) {
        val player = pendingRedeemRequests.remove(requestId)
        if (player == null || !player.isOnline) {
            return
        }
        
        if (success) {
            player.sendMessage("§a[称号] §a$message")
            if (grantedTier != null && grantedTier >= 1) {
                player.sendMessage("§a[称号] §7你现在可以使用渐变称号功能了！")
            }
        } else {
            player.sendMessage("§a[称号] §c$message")
        }
    }

    /**
     * 注册兑换请求
     */
    fun registerRedeemRequest(requestId: String, player: Player) {
        pendingRedeemRequests[requestId] = player
        
        // 30 秒后自动清理
        Bukkit.getGlobalRegionScheduler().runDelayed(plugin, { _ ->
            val removed = pendingRedeemRequests.remove(requestId)
            if (removed != null && removed.isOnline) {
                removed.sendMessage("§a[称号] §c兑换请求超时，请重试")
            }
        }, 600L) // 30 秒
    }

    /**
     * 获取缓存的称号数据
     */
    fun getCachedTitle(uuid: UUID): TitleData? = titleCache[uuid]

    /**
     * 清除缓存
     */
    fun clearCache(uuid: UUID) {
        titleCache.remove(uuid)
    }
}

/**
 * 称号数据
 */
data class TitleData(
    val title: String,
    val tier: Int
)
