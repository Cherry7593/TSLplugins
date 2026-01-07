package org.tsl.tSLplugins.WebBridge

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.tsl.tSLplugins.Title.TitleManager
import org.tsl.tSLplugins.Title.GetTitleRequest
import org.tsl.tSLplugins.Title.GetTitleRequestData
import org.tsl.tSLplugins.Title.RedeemCodeRequest
import org.tsl.tSLplugins.Title.RedeemCodeRequestData
import java.util.UUID

/**
 * WebBridge 管理器
 *
 * 负责：
 * - 读取配置
 * - 初始化 WebSocket 客户端
 * - 注册聊天监听器
 * - 提供统一的消息发送接口
 */
class WebBridgeManager(private val plugin: Plugin) {

    private var client: WebBridgeClient? = null
    private var chatListener: WebBridgeChatListener? = null
    private var playerEventListener: WebBridgePlayerListener? = null
    private var titleManager: TitleManager? = null
    private var bindManager: BindManager? = null
    private var isEnabled = false

    // 服务器标识（多服务器支持）
    private var serverId: String = ""

    // 消息格式配置
    private var webToGameFormat = "&7[&b{source}&7] &f<{playerName}> &7{message}"

    // 玩家列表推送配置
    private var playerListIntervalSeconds = 30L
    private var heartbeatIntervalSeconds = 30L
    private var playerListTask: io.papermc.paper.threadedregions.scheduler.ScheduledTask? = null
    private var heartbeatTask: io.papermc.paper.threadedregions.scheduler.ScheduledTask? = null

    // JSON 序列化器
    private val json = Json {
        prettyPrint = false
        encodeDefaults = true
    }

    // 自动重连配置
    private var autoReconnect = true
    private var reconnectIntervalSeconds = 30L
    private var maxReconnectAttempts = 5
    private var reconnectAttempts = 0
    private var reconnectTask: io.papermc.paper.threadedregions.scheduler.ScheduledTask? = null

    /**
     * 初始化 WebBridge 模块
     */
    fun initialize() {
        // 读取配置
        val config = plugin.config.getConfigurationSection("webbridge")

        if (config == null) {
            plugin.logger.warning("[WebBridge] 配置文件中未找到 webbridge 配置块")
            return
        }

        isEnabled = config.getBoolean("enabled", false)

        if (!isEnabled) {
            plugin.logger.info("[WebBridge] 模块未启用")
            return
        }

        // 读取 WebSocket 配置
        val wsConfig = config.getConfigurationSection("websocket")
        if (wsConfig == null) {
            plugin.logger.warning("[WebBridge] 配置文件中未找到 websocket 配置块")
            return
        }

        val baseUrl = wsConfig.getString("url")
        val token = wsConfig.getString("token", "")

        if (baseUrl.isNullOrBlank()) {
            plugin.logger.warning("[WebBridge] WebSocket URL 未配置")
            return
        }

        // 读取服务器标识配置
        loadOrGenerateServerId(config)

        // 构建完整的 WebSocket URL
        val url = buildWebSocketUrl(baseUrl, token)

        // 读取消息格式配置
        val msgConfig = config.getConfigurationSection("messages")
        webToGameFormat = msgConfig?.getString("web-to-game", webToGameFormat) ?: webToGameFormat

        // 读取自动重连配置
        autoReconnect = config.getBoolean("auto-reconnect", true)
        reconnectIntervalSeconds = config.getLong("reconnect-interval", 30L)
        maxReconnectAttempts = config.getInt("max-reconnect-attempts", 5)

        // 读取玩家列表和心跳配置
        playerListIntervalSeconds = config.getLong("player-list-interval", 30L)
        heartbeatIntervalSeconds = config.getLong("heartbeat-interval", 30L)

        // 初始化 WebSocket 客户端（不自动连接）
        client = WebBridgeClient(plugin, url, this)

        // 注册聊天监听器
        chatListener = WebBridgeChatListener(plugin, this)
        plugin.server.pluginManager.registerEvents(chatListener!!, plugin)

        // 注册玩家进出事件监听器
        playerEventListener = WebBridgePlayerListener(this)
        plugin.server.pluginManager.registerEvents(playerEventListener!!, plugin)

        // 初始化称号管理器
        titleManager = TitleManager(plugin)
        titleManager?.initialize()

        // 初始化绑定管理器
        bindManager = BindManager(plugin)

        plugin.logger.info("[WebBridge] 模块已启用，URL: $url")

        // 启动自动重连任务
        if (autoReconnect) {
            startAutoReconnect()
        } else {
            plugin.logger.info("[WebBridge] 自动重连已禁用，使用 /tsl webbridge connect 手动连接")
        }
    }


    /**
     * 关闭 WebBridge 模块
     */
    fun shutdown() {
        if (!isEnabled) {
            return
        }

        plugin.logger.info("[WebBridge] 正在关闭模块...")

        // 停止所有定时任务
        stopAutoReconnect()
        stopScheduledTasks()

        // 停止 WebSocket 客户端
        client?.stop()
        client = null

        chatListener = null
        playerEventListener = null

        // 关闭称号管理器
        titleManager?.shutdown()
        titleManager = null

        // 关闭绑定管理器
        bindManager?.shutdown()
        bindManager = null

        plugin.logger.info("[WebBridge] 模块已关闭")
    }

    /**
     * 发送消息到 Web 后端
     *
     * @param json JSON 格式的消息
     */
    fun sendMessage(json: String) {
        client?.enqueue(json)
    }

    /**
     * 手动连接到 WebSocket 服务器
     */
    fun connect(): Boolean {
        val result = client?.connect() ?: false
        if (result) {
            // 连接成功，重置重试计数
            reconnectAttempts = 0
        }
        return result
    }

    /**
     * 手动断开 WebSocket 连接
     */
    fun disconnect() {
        client?.disconnect()
    }

    /**
     * 获取当前连接状态
     */
    fun isConnected(): Boolean = client?.isConnected() ?: false

    /**
     * 获取发送队列长度
     */
    fun getQueueSize(): Int = client?.getQueueSize() ?: 0

    /**
     * 检查模块是否启用
     */
    fun isEnabled(): Boolean = isEnabled

    /**
     * 启动自动重连任务
     */
    private fun startAutoReconnect() {
        reconnectAttempts = 0
        reconnectTask?.cancel()

        plugin.logger.info("[WebBridge] 启动自动重连 (间隔: ${reconnectIntervalSeconds}秒, 最大重试: ${maxReconnectAttempts}次)")

        reconnectTask = org.bukkit.Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, { _ ->
            // 已连接，无需重连
            if (isConnected()) {
                return@runAtFixedRate
            }

            // 超过最大重试次数
            if (reconnectAttempts >= maxReconnectAttempts) {
                if (reconnectAttempts == maxReconnectAttempts) {
                    plugin.logger.warning("[WebBridge] 已达最大重试次数 ($maxReconnectAttempts)，停止自动重连")
                    plugin.logger.warning("[WebBridge] 使用 /tsl webbridge connect 手动连接，或 /tsl reload 重置重试计数")
                    reconnectAttempts++ // 防止重复打印
                }
                return@runAtFixedRate
            }

            reconnectAttempts++
            plugin.logger.info("[WebBridge] 尝试自动连接 ($reconnectAttempts/$maxReconnectAttempts)...")

            if (client?.connect() == true) {
                plugin.logger.info("[WebBridge] 自动连接成功")
                reconnectAttempts = 0
            }
        }, reconnectIntervalSeconds * 20L, reconnectIntervalSeconds * 20L)
    }

    /**
     * 停止自动重连任务
     */
    private fun stopAutoReconnect() {
        reconnectTask?.cancel()
        reconnectTask = null
    }

    /**
     * 连接断开时的回调（由 Client 调用）
     */
    fun onDisconnected() {
        // 如果启用了自动重连且任务还在运行，重置重试计数以便重新开始重连
        if (autoReconnect && reconnectTask != null && reconnectAttempts > maxReconnectAttempts) {
            reconnectAttempts = 0
        }
    }

    /**
     * 获取 Web 到游戏的消息格式
     */
    fun getWebToGameFormat(): String = webToGameFormat

    /**
     * 获取称号管理器
     */
    fun getTitleManager(): TitleManager? = titleManager

    /**
     * 获取绑定管理器
     */
    fun getBindManager(): BindManager? = bindManager

    // ========== 绑定相关方法 ==========

    /**
     * 请求账号绑定验证
     */
    fun requestBindAccount(player: Player, code: String) {
        if (!isConnected()) {
            player.sendMessage("§c[绑定] 服务器未连接")
            return
        }

        val requestId = "bind-${System.currentTimeMillis()}-${UUID.randomUUID().toString().substring(0, 8)}"

        val request = BindAccountRequest(
            data = BindAccountRequestData(
                id = requestId,
                playerUuid = player.uniqueId.toString(),
                playerName = player.name,
                code = code.uppercase()
            )
        )

        // 注册回调
        bindManager?.registerRequest(requestId, player)

        val jsonString = json.encodeToString(request)
        sendMessage(jsonString)
    }

    // ========== 称号相关方法 ==========

    /**
     * 请求获取玩家称号
     */
    fun requestPlayerTitle(playerUuid: String) {
        if (!isConnected()) return
        if (titleManager?.isEnabled() != true) return

        val request = GetTitleRequest(
            data = GetTitleRequestData(
                id = "gt-${System.currentTimeMillis()}",
                playerUuid = playerUuid
            )
        )
        val jsonString = json.encodeToString(request)
        sendMessage(jsonString)
    }

    /**
     * 请求兑换码验证
     */
    fun requestRedeemCode(player: Player, code: String) {
        if (!isConnected()) return
        if (titleManager?.isEnabled() != true) {
            player.sendMessage("§c[称号] 称号功能未启用")
            return
        }

        val requestId = "rc-${System.currentTimeMillis()}"
        
        val request = RedeemCodeRequest(
            data = RedeemCodeRequestData(
                id = requestId,
                playerUuid = player.uniqueId.toString(),
                playerName = player.name,
                code = code
            )
        )
        
        // 注册回调
        titleManager?.registerRedeemRequest(requestId, player)
        
        val jsonString = json.encodeToString(request)
        sendMessage(jsonString)
    }

    // ========== 玩家列表推送 ==========

    /**
     * 连接成功时的回调（由 Client 调用）
     */
    fun onConnected() {
        // 连接成功后立即推送玩家列表
        sendPlayerList()
        // 启动定时任务
        startScheduledTasks()
    }

    /**
     * 启动定时任务（玩家列表推送 + 心跳）
     */
    private fun startScheduledTasks() {
        stopScheduledTasks() // 先停止旧任务

        // 定时推送玩家列表
        playerListTask = Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, { _ ->
            if (isConnected()) {
                sendPlayerList()
            }
        }, playerListIntervalSeconds * 20L, playerListIntervalSeconds * 20L)

        // 定时发送心跳
        heartbeatTask = Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, { _ ->
            if (isConnected()) {
                sendHeartbeat()
            }
        }, heartbeatIntervalSeconds * 20L, heartbeatIntervalSeconds * 20L)

        plugin.logger.info("[WebBridge] 定时任务已启动 (玩家列表: ${playerListIntervalSeconds}s, 心跳: ${heartbeatIntervalSeconds}s)")
    }

    /**
     * 停止定时任务
     */
    private fun stopScheduledTasks() {
        playerListTask?.cancel()
        playerListTask = null
        heartbeatTask?.cancel()
        heartbeatTask = null
    }

    /**
     * 发送玩家列表事件
     */
    fun sendPlayerList() {
        if (!isConnected()) return

        val players = Bukkit.getOnlinePlayers()
        val playerInfoList = players.map { PlayerInfo(it.uniqueId.toString(), it.name) }

        // 获取 TPS（Paper/Folia 支持）
        val tps = try {
            val tpsArray = Bukkit.getTPS()
            if (tpsArray.isNotEmpty()) {
                (tpsArray[0] * 10.0).toLong() / 10.0 // 保留一位小数
            } else null
        } catch (e: Exception) {
            null
        }

        val eventData = EventData(
            event = "PLAYER_LIST",
            id = "pl-${System.currentTimeMillis()}",
            serverId = serverId,
            online = players.size,
            max = Bukkit.getMaxPlayers(),
            tps = tps,
            players = playerInfoList
        )

        val message = EventMessage(data = eventData)
        val jsonString = json.encodeToString(message)
        sendMessage(jsonString)
    }

    /**
     * 发送心跳消息
     */
    private fun sendHeartbeat() {
        if (!isConnected()) return

        val heartbeat = HeartbeatMessage()
        val jsonString = json.encodeToString(heartbeat)
        sendMessage(jsonString)
    }

    // ========== 服务器标识管理 ==========

    /**
     * 加载或生成服务器标识
     */
    private fun loadOrGenerateServerId(config: org.bukkit.configuration.ConfigurationSection) {
        serverId = config.getString("server-id", "") ?: ""
        
        // 如果 serverId 为空，生成新的 UUID
        if (serverId.isBlank()) {
            serverId = UUID.randomUUID().toString()
            
            // 保存到配置文件
            plugin.config.set("webbridge.server-id", serverId)
            plugin.saveConfig()
            
            plugin.logger.info("[WebBridge] 已生成服务器标识: $serverId")
        } else {
            plugin.logger.info("[WebBridge] 服务器标识: $serverId")
        }
    }

    /**
     * 构建 WebSocket URL（包含 serverId 参数）
     */
    private fun buildWebSocketUrl(baseUrl: String, token: String?): String {
        val sb = StringBuilder(baseUrl)
        
        // 检查是否已有查询参数
        val hasQuery = baseUrl.contains("?")
        sb.append(if (hasQuery) "&" else "?")
        
        // 添加 from 参数
        sb.append("from=mc")
        
        // 添加 serverId
        sb.append("&serverId=").append(serverId)
        
        // 添加 token（如果有）
        if (!token.isNullOrBlank()) {
            sb.append("&token=").append(token)
        }
        
        return sb.toString()
    }

    /**
     * 获取服务器 ID
     */
    fun getServerId(): String = serverId

    /**
     * 重新加载配置并重新初始化模块
     * 支持运行时动态启用/禁用
     */
    fun reload() {
        plugin.logger.info("[WebBridge] 正在重新加载配置...")

        // 记录之前的状态
        val wasEnabled = isEnabled

        // 先关闭现有连接和清理资源
        if (wasEnabled) {
            plugin.logger.info("[WebBridge] 关闭现有连接...")
            stopScheduledTasks()
            client?.stop()
            client = null

            // 注销监听器
            if (chatListener != null) {
                org.bukkit.event.HandlerList.unregisterAll(chatListener!!)
                chatListener = null
            }
            if (playerEventListener != null) {
                org.bukkit.event.HandlerList.unregisterAll(playerEventListener!!)
                playerEventListener = null
            }
        }

        // 重新读取配置
        val config = plugin.config.getConfigurationSection("webbridge")

        if (config == null) {
            plugin.logger.warning("[WebBridge] 配置文件中未找到 webbridge 配置块")
            isEnabled = false
            return
        }

        val newEnabled = config.getBoolean("enabled", false)
        plugin.logger.info("[WebBridge] 配置读取: enabled=$newEnabled (之前: $wasEnabled)")

        // 状态变化日志
        if (!wasEnabled && newEnabled) {
            plugin.logger.info("[WebBridge] 模块从禁用变为启用")
        } else if (wasEnabled && !newEnabled) {
            plugin.logger.info("[WebBridge] 模块从启用变为禁用")
        }

        isEnabled = newEnabled

        if (!isEnabled) {
            plugin.logger.info("[WebBridge] 模块已禁用")
            return
        }

        // 读取 WebSocket 配置
        val wsConfig = config.getConfigurationSection("websocket")
        if (wsConfig == null) {
            plugin.logger.warning("[WebBridge] 配置文件中未找到 websocket 配置块")
            isEnabled = false
            return
        }

        val baseUrl = wsConfig.getString("url")
        val token = wsConfig.getString("token", "")

        if (baseUrl.isNullOrBlank()) {
            plugin.logger.warning("[WebBridge] WebSocket URL 未配置")
            isEnabled = false
            return
        }

        // 读取服务器标识配置
        loadOrGenerateServerId(config)

        // 构建完整的 WebSocket URL
        val url = buildWebSocketUrl(baseUrl, token)

        // 读取消息格式配置
        val msgConfig = config.getConfigurationSection("messages")
        webToGameFormat = msgConfig?.getString("web-to-game", webToGameFormat) ?: webToGameFormat

        // 读取自动重连配置
        autoReconnect = config.getBoolean("auto-reconnect", true)
        reconnectIntervalSeconds = config.getLong("reconnect-interval", 30L)
        maxReconnectAttempts = config.getInt("max-reconnect-attempts", 5)

        // 重新初始化 WebSocket 客户端（不自动连接）
        client = WebBridgeClient(plugin, url, this)

        // 读取玩家列表和心跳配置
        playerListIntervalSeconds = config.getLong("player-list-interval", 30L)
        heartbeatIntervalSeconds = config.getLong("heartbeat-interval", 30L)

        // 重新注册监听器
        chatListener = WebBridgeChatListener(plugin, this)
        plugin.server.pluginManager.registerEvents(chatListener!!, plugin)

        playerEventListener = WebBridgePlayerListener(this)
        plugin.server.pluginManager.registerEvents(playerEventListener!!, plugin)

        // 重新初始化称号管理器
        titleManager?.shutdown()
        titleManager = TitleManager(plugin)
        titleManager?.initialize()

        plugin.logger.info("[WebBridge] 模块重载完成，URL: $url")

        // 启动自动重连任务
        if (autoReconnect) {
            startAutoReconnect()
        } else {
            plugin.logger.info("[WebBridge] 自动重连已禁用，使用 /tsl webbridge connect 手动连接")
        }
    }

    /**
     * 重新加载称号管理器配置
     */
    fun reloadTitleManager() {
        titleManager?.reload()
    }
}

