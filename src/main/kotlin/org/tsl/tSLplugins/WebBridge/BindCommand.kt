package org.tsl.tSLplugins.WebBridge

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.tsl.tSLplugins.SubCommandHandler

/**
 * 账号绑定命令处理器
 * 处理 /tsl bind <验证码> 命令
 */
class BindCommand(
    private val webBridgeManager: WebBridgeManager
) : SubCommandHandler {

    private val serializer = LegacyComponentSerializer.legacyAmpersand()

    override fun handle(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        // 必须是玩家
        if (sender !is Player) {
            sender.sendMessage(serializer.deserialize("&c[绑定] 该命令只能由玩家执行"))
            return true
        }

        // 检查 WebBridge 是否启用
        if (!webBridgeManager.isEnabled()) {
            sender.sendMessage(serializer.deserialize("&c[绑定] 绑定功能未启用"))
            return true
        }

        // 检查 WebSocket 连接
        if (!webBridgeManager.isConnected()) {
            sender.sendMessage(serializer.deserialize("&c[绑定] 服务器未连接，请稍后重试"))
            return true
        }

        if (args.isEmpty()) {
            showUsage(sender)
            return true
        }

        val code = args[0].uppercase().trim()

        // 验证码格式检查
        val bindManager = webBridgeManager.getBindManager()
        if (bindManager == null || !bindManager.isValidCodeFormat(code)) {
            sender.sendMessage(serializer.deserialize("&c[绑定] 验证码格式无效，请检查是否输入正确"))
            sender.sendMessage(serializer.deserialize("&7验证码为 6 位字母数字组合"))
            return true
        }

        // 发送绑定请求
        sender.sendMessage(serializer.deserialize("&e[绑定] 正在验证..."))
        webBridgeManager.requestBindAccount(sender, code)

        return true
    }

    private fun showUsage(sender: CommandSender) {
        sender.sendMessage(serializer.deserialize("&6&l===== 账号绑定 ====="))
        sender.sendMessage(serializer.deserialize("&e/tsl bind <验证码> &7- 绑定网站账号"))
        sender.sendMessage(serializer.deserialize(""))
        sender.sendMessage(serializer.deserialize("&7步骤:"))
        sender.sendMessage(serializer.deserialize("&71. 访问官网绑定页面"))
        sender.sendMessage(serializer.deserialize("&72. 点击\"生成验证码\""))
        sender.sendMessage(serializer.deserialize("&73. 在游戏内输入 &f/tsl bind <验证码>"))
    }

    override fun tabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): List<String> {
        // 不提供补全，验证码需要手动输入
        return emptyList()
    }

    override fun getDescription(): String {
        return "绑定网站账号"
    }
}
