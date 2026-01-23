package org.tsl.tSLplugins.WebBridge

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.tsl.tSLplugins.PlayerDataManager
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * QQ 群绑定管理器
 * 处理 MC 账号与 QQ 号的绑定
 */
class QQBindManager(
    private val plugin: Plugin,
    private val webBridgeManager: WebBridgeManager
) {
    // 待处理的绑定请求 (requestId -> Player)
    private val pendingRequests = ConcurrentHashMap<String, Player>()

    // 待处理的绑定状态查询 (requestId -> playerUuid)
    private val pendingBindStatusQueries = ConcurrentHashMap<String, String>()

    // 玩家数据管理器（用于更新绑定状态缓存）
    private var playerDataManager: PlayerDataManager? = null

    /**
     * 设置玩家数据管理器
     */
    fun setPlayerDataManager(manager: PlayerDataManager) {
        playerDataManager = manager
    }

    // JSON 序列化器
    private val json = Json {
        prettyPrint = false
        encodeDefaults = true
    }

    /**
     * 请求 QQ 绑定（新版本：由 Web 端生成验证码）
     */
    fun requestQQBind(player: Player) {
        if (!webBridgeManager.isConnected()) {
            player.sendMessage(getMessage("not_connected"))
            return
        }

        val requestId = UUID.randomUUID().toString()

        val request = QQBindRequest(
            requestId = requestId,
            data = QQBindRequestData(
                playerUuid = player.uniqueId.toString(),
                playerName = player.name
            )
        )

        // 注册回调
        pendingRequests[requestId] = player

        // 30秒超时自动清理
        Bukkit.getGlobalRegionScheduler().runDelayed(plugin, { _ ->
            val removed = pendingRequests.remove(requestId)
            if (removed != null && removed.isOnline) {
                removed.sendMessage(getMessage("request_timeout"))
            }
        }, 600L) // 30秒

        val jsonString = json.encodeToString(request)
        webBridgeManager.sendMessage(jsonString)

        player.sendMessage(getMessage("request_sent"))
    }

    /**
     * 处理绑定请求响应
     */
    fun handleBindRequestResponse(
        requestId: String,
        success: Boolean,
        code: String?,
        remainingSeconds: Int?,
        isNew: Boolean?,
        bindCommand: String?,
        message: String?,
        error: String?
    ) {
        val player = pendingRequests.remove(requestId)
        if (player == null || !player.isOnline) {
            return
        }

        if (success) {
            // 显示绑定指令（优先使用 bindCommand，否则使用 code）
            val displayCommand = bindCommand ?: ("我要绑定 " + (code ?: ""))
            sendClickableBindCommand(player, displayCommand)
            if (remainingSeconds != null) {
                val minutes = remainingSeconds / 60
                player.sendMessage(getMessage("expire_hint").replace("%seconds%", remainingSeconds.toString()).replace("%minutes%", minutes.toString()))
            }
        } else {
            // 已绑定时显示带解绑按钮的提示
            if (error == "already_bound") {
                val unbindCmd = plugin.config.getString("webbridge.qq-bind.unbind-click-command", "/tsl bind unbind") ?: "/tsl bind unbind"
                BindCommand.sendAlreadyBoundMessage(player, unbindCmd)
                return
            }
            
            val errorMsg = when (error) {
                "invalid_code" -> getMessage("invalid_code")
                "server_error" -> getMessage("server_error")
                else -> message ?: getMessage("unknown_error")
            }
            player.sendMessage(errorMsg)
        }
    }

    /**
     * 处理绑定结果通知（广播）- 旧版 QQ_BIND_RESULT
     * 
     * 注意：新版本 Web 端同时会发送 BIND_STATUS_UPDATE 事件，
     * 为避免重复消息，此处只播放音效，消息和缓存更新由 handleBindStatusUpdateEvent 处理
     */
    fun handleBindResult(
        success: Boolean,
        mcUuid: String,
        mcName: String,
        qqNumber: String?,
        message: String?
    ) {
        // 查找在线玩家
        val uuid = try {
            UUID.fromString(mcUuid)
        } catch (e: Exception) {
            plugin.logger.warning("[QQBind] 无效的 UUID: $mcUuid")
            return
        }

        val player = Bukkit.getPlayer(uuid)
        if (player == null || !player.isOnline) {
            return
        }

        if (success) {
            // 只播放音效，消息和缓存更新由 BIND_STATUS_UPDATE 事件处理
            try {
                player.playSound(player.location, org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f)
            } catch (e: Exception) {
                // 忽略音效错误
            }
            plugin.logger.fine("[QQBind] QQ_BIND_RESULT 收到，音效已播放 (消息由 BIND_STATUS_UPDATE 处理)")
        } else {
            // 失败消息仍然显示（BIND_STATUS_UPDATE 不会发送失败事件）
            player.sendMessage(getMessage("bind_failed").replace("%reason%", message ?: "未知错误"))
        }
    }

    /**
     * 获取消息配置
     */
    private fun getMessage(key: String): String {
        val prefix = plugin.config.getString("messages.prefix", "&6[TSL]&r ") ?: "&6[TSL]&r "
        val defaultMessages = mapOf(
            "not_connected" to "%prefix%&cWebSocket 未连接，无法使用此功能",
            "request_sent" to "%prefix%&7正在请求绑定码...",
            "request_timeout" to "%prefix%&c请求超时，请重试",
            "bind_instruction" to "%prefix%&a请在 QQ 群内发送：&e&l%command%",
            "expire_hint" to "%prefix%&7验证码有效期：&f%minutes% 分钟",
            "already_bound" to "%prefix%&c该账号已绑定其他 QQ",
            "invalid_code" to "%prefix%&c验证码格式错误，请重试",
            "server_error" to "%prefix%&c服务器繁忙，请稍后重试",
            "unknown_error" to "%prefix%&c未知错误，请重试",
            "bind_success" to "%prefix%&a✓ QQ 绑定成功！",
            "bind_success_hint" to "%prefix%&7你的 MC 账号已与 QQ 关联",
            "bind_failed" to "%prefix%&c✗ 绑定失败：%reason%",
            "unbind_processing" to "%prefix%&7正在处理解绑请求...",
            "unbind_success" to "%prefix%&a✓ 解绑成功！",
            "unbind_qq_hint" to "%prefix%&7已解除与 QQ &e%qq% &7的绑定",
            "not_bound" to "%prefix%&c该账号未绑定任何 QQ",
            "bind_status_updated" to "%prefix%&a绑定状态已同步更新",
            "unbind_status_updated" to "%prefix%&e账号已解绑，状态已同步"
        )

        val msgKey = "messages.qq-bind.$key"
        val msg = plugin.config.getString(msgKey, defaultMessages[key]) ?: defaultMessages[key] ?: ""
        return colorize(msg.replace("%prefix%", prefix))
    }

    /**
     * 颜色代码转换
     */
    private fun colorize(text: String): String {
        return text.replace("&", "§")
    }

    /**
     * 发送可点击复制的绑定口令消息
     */
    private fun sendClickableBindCommand(player: Player, bindCommand: String) {
        val prefix = Component.text("[绑定] ", NamedTextColor.GOLD)
        val instruction = Component.text("请在 QQ 群内发送：", NamedTextColor.GREEN)
        
        val clickableCommand = Component.text(bindCommand, NamedTextColor.YELLOW)
            .decorate(TextDecoration.BOLD)
            .clickEvent(ClickEvent.copyToClipboard(bindCommand))
            .hoverEvent(HoverEvent.showText(Component.text("点击复制绑定口令", NamedTextColor.GRAY)))
        
        player.sendMessage(prefix.append(instruction).append(clickableCommand))
    }

    /**
     * 请求解绑账号
     */
    fun requestUnbind(player: Player) {
        if (!webBridgeManager.isConnected()) {
            player.sendMessage(getMessage("not_connected"))
            return
        }

        val requestId = UUID.randomUUID().toString()

        val request = UnbindRequest(
            requestId = requestId,
            data = UnbindRequestData(
                playerUuid = player.uniqueId.toString(),
                playerName = player.name
            )
        )

        // 注册回调
        pendingRequests[requestId] = player

        // 30秒超时自动清理
        Bukkit.getGlobalRegionScheduler().runDelayed(plugin, { _ ->
            val removed = pendingRequests.remove(requestId)
            if (removed != null && removed.isOnline) {
                removed.sendMessage(getMessage("request_timeout"))
            }
        }, 600L)

        val jsonString = json.encodeToString(request)
        webBridgeManager.sendMessage(jsonString)

        player.sendMessage(getMessage("unbind_processing"))
    }

    /**
     * 处理解绑响应
     */
    fun handleUnbindResponse(
        requestId: String,
        success: Boolean,
        mcName: String?,
        qqNumber: String?,
        message: String?,
        error: String?
    ) {
        val player = pendingRequests.remove(requestId)
        if (player == null || !player.isOnline) {
            return
        }

        if (success) {
            player.sendMessage(getMessage("unbind_success"))
            if (qqNumber != null) {
                player.sendMessage(getMessage("unbind_qq_hint").replace("%qq%", qqNumber))
            }
            // 更新玩家绑定状态缓存
            playerDataManager?.let { manager ->
                val profile = manager.getProfileStore().getOrCreate(player.uniqueId, player.name)
                profile.bindStatus = false
                profile.bindQQ = ""
                manager.getProfileStore().save(profile)
                plugin.logger.info("[QQBind] 已清除玩家 ${player.name} 的绑定缓存")
            }
            plugin.logger.info("[QQBind] 玩家 ${mcName ?: player.name} 解绑成功")
        } else {
            val errorMsg = when (error) {
                "not_bound" -> getMessage("not_bound")
                "server_error" -> getMessage("server_error")
                else -> message ?: getMessage("unknown_error")
            }
            player.sendMessage(errorMsg)
        }
    }

    /**
     * 处理绑定状态更新事件（实时推送）
     * 当玩家在网页、QQ 群等渠道完成绑定/解绑时，Web 端广播此事件
     * 
     * 这是绑定状态更新的主要处理函数，负责：
     * 1. 更新玩家绑定状态缓存
     * 2. 发送提示消息给玩家
     * 3. 执行配置的绑定/解绑命令
     */
    fun handleBindStatusUpdateEvent(
        mcUuid: String,
        mcName: String,
        qqNumber: String?,
        action: String,
        source: String
    ) {
        val uuid = try {
            UUID.fromString(mcUuid)
        } catch (e: Exception) {
            plugin.logger.warning("[QQBind] 无效的 UUID: $mcUuid")
            return
        }

        // 查找在线玩家
        val player = Bukkit.getPlayer(uuid)
        if (player == null || !player.isOnline) {
            plugin.logger.fine("[QQBind] 收到绑定更新但玩家不在线: $mcName")
            return
        }

        // 更新玩家绑定状态缓存
        playerDataManager?.let { manager ->
            val profile = manager.getProfileStore().getOrCreate(uuid, mcName)
            
            when (action) {
                "bind" -> {
                    profile.bindStatus = true
                    profile.bindQQ = qqNumber ?: ""
                    // 根据来源显示不同消息
                    player.sendMessage(getMessage("bind_success"))
                    player.sendMessage(getMessage("bind_success_hint"))
                    plugin.logger.info("[QQBind] 玩家 $mcName 绑定成功 (来源: $source, QQ: $qqNumber)")
                }
                "unbind" -> {
                    profile.bindStatus = false
                    profile.bindQQ = ""
                    player.sendMessage(getMessage("unbind_status_updated"))
                    plugin.logger.info("[QQBind] 玩家 $mcName 解绑成功 (来源: $source)")
                }
            }
            
            manager.getProfileStore().save(profile)
        }

        // 执行绑定/解绑命令
        executeBindCommands(player, action, qqNumber)
    }

    /**
     * 执行绑定/解绑时的命令
     */
    private fun executeBindCommands(player: Player, action: String, qqNumber: String?) {
        val configKey = when (action) {
            "bind" -> "webbridge.qq-bind.on-bind-commands"
            "unbind" -> "webbridge.qq-bind.on-unbind-commands"
            else -> return
        }
        
        val commands = plugin.config.getStringList(configKey)
        if (commands.isEmpty()) return
        
        Bukkit.getGlobalRegionScheduler().run(plugin) { _ ->
            for (command in commands) {
                val finalCommand = command
                    .replace("%player%", player.name)
                    .replace("%uuid%", player.uniqueId.toString())
                    .replace("%qq%", qqNumber ?: "")
                try {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand)
                    plugin.logger.info("[QQBind] 执行命令: $finalCommand")
                } catch (e: Exception) {
                    plugin.logger.warning("[QQBind] 执行命令失败: $finalCommand - ${e.message}")
                }
            }
        }
    }

    /**
     * 清理所有待处理请求
     */
    fun shutdown() {
        pendingRequests.clear()
        pendingBindStatusQueries.clear()
    }

    /**
     * 注册绑定状态查询请求
     */
    fun registerBindStatusQuery(requestId: String, playerUuid: String) {
        pendingBindStatusQueries[requestId] = playerUuid
    }

    /**
     * 处理绑定状态查询响应
     * 当玩家加入服务器时查询绑定状态的响应
     */
    fun handleBindStatusResponse(
        requestId: String,
        success: Boolean,
        bound: Boolean,
        qqNumber: String?,
        source: String?,
        message: String?
    ) {
        // 通过 requestId 获取 playerUuid
        val playerUuid = pendingBindStatusQueries.remove(requestId)
        if (playerUuid == null) {
            plugin.logger.fine("[QQBind] 未找到绑定状态查询请求: $requestId")
            return
        }

        val uuid = try {
            UUID.fromString(playerUuid)
        } catch (e: Exception) {
            plugin.logger.warning("[QQBind] 无效的 UUID: $playerUuid")
            return
        }

        if (!success) {
            plugin.logger.fine("[QQBind] 绑定状态查询失败: $message")
            return
        }

        // 更新玩家绑定状态缓存
        playerDataManager?.let { manager ->
            val player = Bukkit.getPlayer(uuid)
            val playerName = player?.name ?: "Unknown"
            val profile = manager.getProfileStore().getOrCreate(uuid, playerName)
            
            val changed = profile.bindStatus != bound || profile.bindQQ != (qqNumber ?: "")
            
            profile.bindStatus = bound
            profile.bindQQ = qqNumber ?: ""
            manager.getProfileStore().save(profile)
            
            if (changed) {
                plugin.logger.info("[QQBind] 已更新玩家 $playerName 的绑定缓存: bound=$bound, qq=$qqNumber, source=$source")
            }
        }
    }
}

// ========== 数据类 ==========

@Serializable
data class QQBindRequest(
    val type: String = "request",
    val action: String = "QQ_BIND_REQUEST",
    val requestId: String,
    val data: QQBindRequestData
)

@Serializable
data class QQBindRequestData(
    val playerUuid: String,
    val playerName: String
)

@Serializable
data class UnbindRequest(
    val type: String = "request",
    val action: String = "UNBIND_ACCOUNT",
    val requestId: String,
    val data: UnbindRequestData
)

@Serializable
data class UnbindRequestData(
    val playerUuid: String,
    val playerName: String
)

@Serializable
data class QueryBindStatusRequest(
    val type: String = "request",
    val action: String = "QUERY_BIND_STATUS",
    val requestId: String,
    val data: QueryBindStatusRequestData
)

@Serializable
data class QueryBindStatusRequestData(
    val playerUuid: String,
    val playerName: String
)
