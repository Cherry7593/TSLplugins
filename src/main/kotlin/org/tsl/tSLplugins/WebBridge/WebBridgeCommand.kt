package org.tsl.tSLplugins.WebBridge

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.tsl.tSLplugins.SubCommandHandler

/**
 * WebBridge 命令处理器
 *
 * 提供以下命令：
 * - /tsl webbridge status - 查看 WebBridge 状态
 * - /tsl webbridge connect - 连接到 WebSocket 服务器
 * - /tsl webbridge disconnect - 断开 WebSocket 连接
 */
class WebBridgeCommand(private val manager: WebBridgeManager) : SubCommandHandler {

    override fun handle(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (args.isEmpty()) {
            sender.sendMessage("§e用法: /tsl webbridge <status|connect|disconnect>")
            return true
        }

        when (args[0].lowercase()) {
            "status" -> handleStatus(sender)
            "connect" -> handleConnect(sender)
            "disconnect" -> handleDisconnect(sender)
            else -> {
                sender.sendMessage("§c未知的子命令: ${args[0]}")
                sender.sendMessage("§e可用命令: status, connect, disconnect")
            }
        }

        return true
    }

    override fun tabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): List<String> {
        if (args.size == 1) {
            return listOf("status", "connect", "disconnect").filter { it.startsWith(args[0].lowercase()) }
        }
        return emptyList()
    }

    override fun getDescription(): String {
        return "管理 WebBridge 连接"
    }

    /**
     * 处理 status 子命令
     */
    private fun handleStatus(sender: CommandSender) {
        if (!sender.hasPermission("tsl.webbridge.status")) {
            sender.sendMessage("§c你没有权限执行此命令")
            return
        }

        sender.sendMessage("§6========== WebBridge 状态 ==========")
        sender.sendMessage("§e模块状态: ${if (manager.isEnabled()) "§a已启用" else "§c未启用"}")

        if (manager.isEnabled()) {
            sender.sendMessage("§e连接状态: ${if (manager.isConnected()) "§a已连接" else "§c未连接"}")
            sender.sendMessage("§e队列长度: §f${manager.getQueueSize()} 条消息")
        }

        sender.sendMessage("§6====================================")
    }

    /**
     * 处理 connect 子命令
     */
    private fun handleConnect(sender: CommandSender) {
        if (!sender.hasPermission("tsl.webbridge.manage")) {
            sender.sendMessage("§c你没有权限执行此命令")
            return
        }

        if (!manager.isEnabled()) {
            sender.sendMessage("§c[WebBridge] 模块未启用")
            return
        }

        if (manager.isConnected()) {
            sender.sendMessage("§e[WebBridge] 已经连接，无需重复连接")
            return
        }

        sender.sendMessage("§e[WebBridge] 正在连接...")
        val success = manager.connect()

        if (success) {
            sender.sendMessage("§a[WebBridge] 连接成功")
        } else {
            sender.sendMessage("§c[WebBridge] 连接失败，请查看控制台日志")
        }
    }

    /**
     * 处理 disconnect 子命令
     */
    private fun handleDisconnect(sender: CommandSender) {
        if (!sender.hasPermission("tsl.webbridge.manage")) {
            sender.sendMessage("§c你没有权限执行此命令")
            return
        }

        if (!manager.isEnabled()) {
            sender.sendMessage("§c[WebBridge] 模块未启用")
            return
        }

        if (!manager.isConnected()) {
            sender.sendMessage("§e[WebBridge] 当前未连接")
            return
        }

        manager.disconnect()
        sender.sendMessage("§e[WebBridge] 已断开连接")
    }
}

