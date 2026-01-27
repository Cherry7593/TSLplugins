package org.tsl.tSLplugins.modules.webbridge

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.tsl.tSLplugins.SubCommandHandler

/**
 * 账号绑定命令处理器
 * 处理 /tsl bind [验证码|unbind] 命令
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

        // 处理解绑命令
        if (args.isNotEmpty() && args[0].equals("unbind", ignoreCase = true)) {
            val qqBindManager = webBridgeManager.getQQBindManager()
            if (qqBindManager != null) {
                qqBindManager.requestUnbind(sender)
            } else {
                sender.sendMessage(serializer.deserialize("&c[绑定] 解绑功能不可用"))
            }
            return true
        }

        // 无参数时，发起 QQ 群绑定
        if (args.isEmpty()) {
            val qqBindManager = webBridgeManager.getQQBindManager()
            if (qqBindManager != null) {
                qqBindManager.requestQQBind(sender)
            } else {
                sender.sendMessage(serializer.deserialize("&c[绑定] QQ 绑定功能不可用"))
            }
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

        // 发送绑定请求（网站验证码绑定）
        sender.sendMessage(serializer.deserialize("&e[绑定] 正在验证..."))
        webBridgeManager.requestBindAccount(sender, code)

        return true
    }

    override fun tabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): List<String> {
        if (args.size == 1) {
            return listOf("unbind").filter { it.startsWith(args[0], ignoreCase = true) }
        }
        return emptyList()
    }

    override fun getDescription(): String {
        return "绑定/解绑账号"
    }

    companion object {
        /**
         * 发送已绑定提示（带可点击解绑按钮）
         * @param unbindCommand 解绑命令，默认为 /tsl bind unbind
         */
        fun sendAlreadyBoundMessage(player: Player, unbindCommand: String = "/tsl bind unbind") {
            val prefix = Component.text("[绑定] ", NamedTextColor.GOLD)
            val message = Component.text("该账号已绑定，请勿重复绑定。", NamedTextColor.YELLOW)
            
            val unbindButton = Component.text(" [点击解绑]", NamedTextColor.RED)
                .decorate(TextDecoration.BOLD)
                .clickEvent(ClickEvent.runCommand(unbindCommand))
                .hoverEvent(HoverEvent.showText(Component.text("点击解除绑定", NamedTextColor.GRAY)))
            
            player.sendMessage(prefix.append(message).append(unbindButton))
        }
    }
}
