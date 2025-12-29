package org.tsl.tSLplugins.WebBridge

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.util.concurrent.ConcurrentHashMap

/**
 * 账号绑定管理器
 * 处理 MC 账号与网站账号的绑定验证
 */
class BindManager(private val plugin: Plugin) {

    // 待处理的绑定请求 (requestId -> Player)
    private val pendingRequests = ConcurrentHashMap<String, Player>()

    // 验证码格式：6位大写字母+数字，排除易混淆字符
    private val codePattern = Regex("^[A-HJ-NP-Z2-9]{6}$")

    /**
     * 验证码格式是否有效
     */
    fun isValidCodeFormat(code: String): Boolean {
        return codePattern.matches(code.uppercase())
    }

    /**
     * 注册绑定请求
     */
    fun registerRequest(requestId: String, player: Player) {
        pendingRequests[requestId] = player

        // 10 秒超时自动清理
        Bukkit.getGlobalRegionScheduler().runDelayed(plugin, { _ ->
            val removed = pendingRequests.remove(requestId)
            if (removed != null && removed.isOnline) {
                removed.sendMessage("§c[绑定] 请求超时，请稍后重试")
            }
        }, 200L) // 10 秒
    }

    /**
     * 处理绑定响应
     */
    fun handleBindResponse(requestId: String, success: Boolean, message: String, error: String?, userId: String?, userName: String?) {
        val player = pendingRequests.remove(requestId)
        if (player == null || !player.isOnline) {
            return
        }

        if (success) {
            player.sendMessage("§a[绑定] §a✓ $message")
            player.sendMessage("§a[绑定] §7现在你可以在官网查看个人资料并设置称号了！")
            plugin.logger.info("[Bind] 玩家 ${player.name} 成功绑定到用户: $userName ($userId)")
        } else {
            player.sendMessage("§a[绑定] §c✗ $message")
            
            // 根据错误码给出额外提示
            when (error) {
                "expired_code" -> {
                    player.sendMessage("§a[绑定] §7请在官网重新获取验证码")
                }
                "already_bound" -> {
                    player.sendMessage("§a[绑定] §7如需解绑请联系管理员")
                }
            }
        }
    }

    /**
     * 清理所有待处理请求
     */
    fun shutdown() {
        pendingRequests.clear()
    }
}
