package org.tsl.tSLplugins.WebBridge

import org.bukkit.plugin.Plugin

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
    private var isEnabled = false

    // 消息格式配置
    private var webToGameFormat = "&7[&b{source}&7] &f<{playerName}> &7{message}"

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

        val url = wsConfig.getString("url")

        if (url.isNullOrBlank()) {
            plugin.logger.warning("[WebBridge] WebSocket URL 未配置")
            return
        }

        // 读取消息格式配置
        val msgConfig = config.getConfigurationSection("messages")
        webToGameFormat = msgConfig?.getString("web-to-game", webToGameFormat) ?: webToGameFormat

        // 初始化 WebSocket 客户端（不自动连接）
        client = WebBridgeClient(plugin, url, this)

        // 注册聊天监听器
        chatListener = WebBridgeChatListener(plugin, this)
        plugin.server.pluginManager.registerEvents(chatListener!!, plugin)

        plugin.logger.info("[WebBridge] 模块已启用，URL: $url")
        plugin.logger.info("[WebBridge] 提示: 使用 /tsl webbridge connect 命令连接到 WebSocket 服务器")
    }


    /**
     * 关闭 WebBridge 模块
     */
    fun shutdown() {
        if (!isEnabled) {
            return
        }

        plugin.logger.info("[WebBridge] 正在关闭模块...")

        // 停止 WebSocket 客户端
        client?.stop()
        client = null

        chatListener = null

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
        return client?.connect() ?: false
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
     * 获取 Web 到游戏的消息格式
     */
    fun getWebToGameFormat(): String = webToGameFormat

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
            client?.stop()
            client = null

            // 注销聊天监听器
            if (chatListener != null) {
                org.bukkit.event.HandlerList.unregisterAll(chatListener!!)
                chatListener = null
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

        val url = wsConfig.getString("url")

        if (url.isNullOrBlank()) {
            plugin.logger.warning("[WebBridge] WebSocket URL 未配置")
            isEnabled = false
            return
        }

        // 读取消息格式配置
        val msgConfig = config.getConfigurationSection("messages")
        webToGameFormat = msgConfig?.getString("web-to-game", webToGameFormat) ?: webToGameFormat

        // 重新初始化 WebSocket 客户端（不自动连接）
        client = WebBridgeClient(plugin, url, this)

        // 重新注册聊天监听器
        chatListener = WebBridgeChatListener(plugin, this)
        plugin.server.pluginManager.registerEvents(chatListener!!, plugin)

        plugin.logger.info("[WebBridge] 模块重载完成，URL: $url")
        plugin.logger.info("[WebBridge] 提示: 使用 /tsl webbridge connect 命令连接到 WebSocket 服务器")
    }
}

