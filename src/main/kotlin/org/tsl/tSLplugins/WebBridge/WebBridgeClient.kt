package org.tsl.tSLplugins.WebBridge

import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.util.logging.Level

/**
 * WebSocket 客户端，负责与 Web 后端通信
 *
 * 功能：
 * - 连接管理（自动重连）
 * - 消息发送队列（异步发送）
 * - 接收消息处理（预留扩展点）
 *
 * 注意：所有认证信息（如 token）应直接包含在 URL 的查询参数中
 */
class WebBridgeClient(
    private val plugin: Plugin,
    private val url: String,
    private val manager: WebBridgeManager
) {
    // 发送队列
    private val messageQueue = ConcurrentLinkedQueue<String>()

    // WebSocket 客户端实例
    private val clientRef = AtomicReference<InternalWebSocketClient?>(null)

    // 运行状态
    private val isRunning = AtomicBoolean(false)

    // 发送任务引用
    private var sendTask: ScheduledTask? = null


    /**
     * 启动 WebSocket 客户端
     */
    fun start() {
        if (!isRunning.compareAndSet(false, true)) {
            plugin.logger.warning("[WebBridge] 客户端已经在运行中")
            return
        }

        plugin.logger.info("[WebBridge] 正在启动 WebSocket 客户端...")
        connect()
        startSendTask()
    }

    /**
     * 停止 WebSocket 客户端
     */
    fun stop() {
        if (!isRunning.compareAndSet(true, false)) {
            return
        }

        plugin.logger.info("[WebBridge] 正在停止 WebSocket 客户端...")

        // 停止发送任务
        sendTask?.cancel()
        sendTask = null

        // 关闭连接
        clientRef.getAndSet(null)?.close()

        // 清空队列
        messageQueue.clear()

        plugin.logger.info("[WebBridge] WebSocket 客户端已停止")
    }

    /**
     * 将消息加入发送队列
     *
     * @param json JSON 格式的消息
     */
    fun enqueue(json: String) {
        // 检查真实的连接状态
        val client = clientRef.get()
        if (client == null || !client.isOpen) {
            plugin.logger.warning("[WebBridge] 未连接到服务器，消息未加入队列")
            return
        }

        messageQueue.offer(json)

        // 队列过长警告
        val queueSize = messageQueue.size
        if (queueSize > 100) {
            plugin.logger.warning("[WebBridge] 发送队列过长 (${queueSize} 条消息)，可能存在网络问题")
        }
    }

    /**
     * 获取当前队列长度
     */
    fun getQueueSize(): Int = messageQueue.size

    /**
     * 检查连接状态
     */
    fun isConnected(): Boolean = clientRef.get()?.isOpen ?: false

    /**
     * 连接到 WebSocket 服务器
     */
    fun connect(): Boolean {
        try {
            if (clientRef.get()?.isOpen == true) {
                plugin.logger.warning("[WebBridge] 已经连接，无需重复连接")
                return false
            }

            plugin.logger.info("[WebBridge] 正在连接: $url")

            val uri = URI(url)

            // 检查协议
            if (uri.scheme != "ws" && uri.scheme != "wss") {
                plugin.logger.severe("[WebBridge] ❌ 错误: 无效的协议 '${uri.scheme}'，仅支持 ws:// 或 wss://")
                return false
            }

            // 创建并连接客户端
            val client = InternalWebSocketClient(uri)
            client.connectionLostTimeout = 30
            clientRef.set(client)

            // 阻塞式连接（5秒超时）
            val connectSuccess = try {
                client.connectBlocking(5, java.util.concurrent.TimeUnit.SECONDS)
            } catch (e: InterruptedException) {
                plugin.logger.severe("[WebBridge] ⚠️ 连接被中断")
                false
            } catch (e: Exception) {
                plugin.logger.log(Level.SEVERE, "[WebBridge] ⚠️ 连接过程异常", e)
                false
            }

            if (!connectSuccess) {
                plugin.logger.warning("[WebBridge] ❌ 连接失败")
                clientRef.set(null)
                return false
            }

            // 连接成功，启动发送任务（如果还没启动）
            if (sendTask == null) {
                startSendTask()
                plugin.logger.info("[WebBridge] 发送任务已启动")
            }

            return true

        } catch (e: java.net.URISyntaxException) {
            plugin.logger.log(Level.SEVERE, "[WebBridge] ❌ URL 格式错误", e)
            return false
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "[WebBridge] ❌ 连接初始化失败", e)
            return false
        }
    }

    /**
     * 手动断开连接
     */
    fun disconnect() {
        // 停止发送任务
        sendTask?.cancel()
        sendTask = null

        val client = clientRef.getAndSet(null)
        if (client != null && client.isOpen) {
            client.close()
            plugin.logger.info("[WebBridge] 已断开连接")
        } else {
            plugin.logger.warning("[WebBridge] 当前未连接")
        }

        // 清空队列
        messageQueue.clear()
    }

    /**
     * 启动发送任务
     */
    private fun startSendTask() {
        // 每秒执行一次，处理队列中的消息
        sendTask = Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, { _ ->
            processSendQueue()
        }, 20L, 20L) // 延迟 1 秒，每 1 秒执行一次
    }

    /**
     * 处理发送队列
     */
    private fun processSendQueue() {
        val client = clientRef.get()

        // 未连接，跳过
        if (client == null || !client.isOpen) {
            return
        }

        // 批量发送消息（最多 10 条/次）
        var sent = 0
        while (sent < 10) {
            val message = messageQueue.poll() ?: break

            try {
                client.send(message)
                sent++
            } catch (e: Exception) {
                plugin.logger.log(Level.WARNING, "[WebBridge] 发送消息失败", e)
                // 发送失败，重新加入队列
                messageQueue.offer(message)
                break
            }
        }
    }

    /**
     * 内部 WebSocket 客户端实现
     *
     * 认证方式：所有认证信息（如 token）直接包含在 URI 的查询参数中
     */
    private inner class InternalWebSocketClient(
        serverUri: URI
    ) : WebSocketClient(serverUri) {

        override fun onOpen(handshakedata: ServerHandshake) {
            plugin.logger.info("[WebBridge] ✅ 连接成功")
        }

        override fun onMessage(message: String) {
            try {
                // 解析 JSON 消息
                val json = Json.parseToJsonElement(message).jsonObject
                val type = json["type"]?.jsonPrimitive?.content

                when (type) {
                    "chat" -> handleChatMessage(json)
                    "system" -> handleSystemMessage(json)
                    else -> plugin.logger.warning("[WebBridge] 未知消息类型: $type")
                }
            } catch (e: Exception) {
                plugin.logger.log(Level.WARNING, "[WebBridge] 处理消息时出错", e)
            }
        }

        /**
         * 处理聊天消息
         */
        private fun handleChatMessage(json: kotlinx.serialization.json.JsonObject) {
            try {
                val source = json["source"]?.jsonPrimitive?.content

                // 只处理来自 Web 的消息
                if (source == "web") {
                    val payload = json["payload"]?.jsonObject ?: return
                    val playerName = payload["playerName"]?.jsonPrimitive?.content ?: "Web用户"
                    val messageText = payload["message"]?.jsonPrimitive?.content ?: return

                    // 使用可配置的消息格式
                    val format = manager.getWebToGameFormat()
                    val formattedMessage = format
                        .replace("{source}", "Web")
                        .replace("{playerName}", playerName)
                        .replace("{message}", messageText)

                    // 解析颜色代码（支持 & 符号）
                    val gameMessage = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                        .legacyAmpersand()
                        .deserialize(formattedMessage)

                    // 在游戏中广播消息（使用 Folia 兼容的方式）
                    Bukkit.getGlobalRegionScheduler().run(plugin) { _ ->
                        Bukkit.getServer().sendMessage(gameMessage)
                        plugin.logger.info("[WebBridge] Web消息已广播: $playerName: $messageText")
                    }
                }
            } catch (e: Exception) {
                plugin.logger.log(Level.WARNING, "[WebBridge] 处理聊天消息失败", e)
            }
        }

        /**
         * 处理系统消息
         */
        private fun handleSystemMessage(json: kotlinx.serialization.json.JsonObject) {
            try {
                val systemMessage = json["message"]?.jsonPrimitive?.content ?: return
                plugin.logger.info("[WebBridge] 系统消息: $systemMessage")
            } catch (e: Exception) {
                plugin.logger.log(Level.WARNING, "[WebBridge] 处理系统消息失败", e)
            }
        }

        override fun onClose(code: Int, reason: String, remote: Boolean) {
            val source = if (remote) "服务器" else "客户端"
            plugin.logger.warning("[WebBridge] 连接已关闭 (由${source}发起, code: $code)")
            plugin.logger.info("[WebBridge] 提示: 使用 /tsl webbridge connect 命令重新连接")

            // 清理引用
            clientRef.compareAndSet(this, null)
        }

        override fun onError(ex: Exception) {
            plugin.logger.log(Level.SEVERE, "[WebBridge] WebSocket 错误", ex)
        }
    }
}

