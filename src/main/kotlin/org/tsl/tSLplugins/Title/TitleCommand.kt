package org.tsl.tSLplugins.Title

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.tsl.tSLplugins.SubCommandHandler
import org.tsl.tSLplugins.WebBridge.WebBridgeManager

/**
 * 称号命令处理器
 * 处理 /tsl title 相关命令
 */
class TitleCommand(
    private val webBridgeManager: WebBridgeManager
) : SubCommandHandler {

    private val serializer = LegacyComponentSerializer.legacyAmpersand()

    override fun handle(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        val titleManager = webBridgeManager.getTitleManager()
        
        // 检查功能是否启用
        if (titleManager == null || !titleManager.isEnabled()) {
            sender.sendMessage(serializer.deserialize("&c[称号] 称号功能未启用"))
            return true
        }

        // 检查 WebSocket 连接
        if (!webBridgeManager.isConnected()) {
            sender.sendMessage(serializer.deserialize("&c[称号] WebSocket 未连接，无法使用称号功能"))
            return true
        }

        if (args.isEmpty()) {
            showUsage(sender)
            return true
        }

        when (args[0].lowercase()) {
            "redeem", "兑换" -> handleRedeem(sender, args.drop(1).toTypedArray())
            "info", "信息" -> handleInfo(sender)
            "help", "帮助" -> showUsage(sender)
            else -> showUsage(sender)
        }

        return true
    }

    /**
     * 处理兑换码命令
     * /tsl title redeem <code>
     */
    private fun handleRedeem(sender: CommandSender, args: Array<out String>) {
        // 必须是玩家
        if (sender !is Player) {
            sender.sendMessage(serializer.deserialize("&c[称号] 该命令只能由玩家执行"))
            return
        }

        // 权限检查
        if (!sender.hasPermission("tsl.title.redeem")) {
            sender.sendMessage(serializer.deserialize("&c[称号] 你没有权限使用兑换码"))
            return
        }

        if (args.isEmpty()) {
            sender.sendMessage(serializer.deserialize("&c[称号] 用法: /tsl title redeem <兑换码>"))
            sender.sendMessage(serializer.deserialize("&7输入你的称号兑换码来解锁称号权限"))
            return
        }

        val code = args[0]
        
        // 发送兑换请求
        webBridgeManager.requestRedeemCode(sender, code)
        sender.sendMessage(serializer.deserialize("&e[称号] 正在验证兑换码..."))
    }

    /**
     * 查看当前称号信息
     * /tsl title info
     */
    private fun handleInfo(sender: CommandSender) {
        // 必须是玩家
        if (sender !is Player) {
            sender.sendMessage(serializer.deserialize("&c[称号] 该命令只能由玩家执行"))
            return
        }

        val titleManager = webBridgeManager.getTitleManager() ?: return
        val titleData = titleManager.getCachedTitle(sender.uniqueId)

        sender.sendMessage(serializer.deserialize("&6&l===== 称号信息 ====="))
        
        if (titleData != null) {
            sender.sendMessage(serializer.deserialize("&7当前称号: ${titleData.title}"))
            sender.sendMessage(serializer.deserialize("&7权限等级: &f${getTierName(titleData.tier)}"))
        } else {
            sender.sendMessage(serializer.deserialize("&7当前没有设置称号"))
        }
        
        sender.sendMessage(serializer.deserialize(""))
        sender.sendMessage(serializer.deserialize("&7提示: 在官网设置你的个性称号"))
    }

    /**
     * 显示用法
     */
    private fun showUsage(sender: CommandSender) {
        sender.sendMessage(serializer.deserialize("&6&l===== 称号命令 ====="))
        sender.sendMessage(serializer.deserialize("&e/tsl title redeem <兑换码> &7- 使用兑换码"))
        sender.sendMessage(serializer.deserialize("&e/tsl title info &7- 查看当前称号"))
        sender.sendMessage(serializer.deserialize("&e/tsl title help &7- 显示帮助"))
        sender.sendMessage(serializer.deserialize(""))
        sender.sendMessage(serializer.deserialize("&7提示: 在官网自定义你的称号内容"))
    }

    /**
     * 获取等级名称
     * v2: 简化为两个等级 (0=无称号, 1=渐变称号)
     */
    private fun getTierName(tier: Int): String {
        return when {
            tier <= 0 -> "无称号"
            tier >= 1 -> "渐变称号"
            else -> "未知"
        }
    }

    override fun tabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): List<String> {
        val titleManager = webBridgeManager.getTitleManager()
        if (titleManager == null || !titleManager.isEnabled()) return emptyList()

        return when (args.size) {
            1 -> {
                listOf("redeem", "info", "help")
                    .filter { it.startsWith(args[0], ignoreCase = true) }
            }
            else -> emptyList()
        }
    }

    override fun getDescription(): String {
        return "称号管理"
    }
}
