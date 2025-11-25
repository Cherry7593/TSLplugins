package org.tsl.tSLplugins.ChatBubble

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.tsl.tSLplugins.SubCommandHandler
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer

/**
 * ChatBubble 命令处理器
 * 处理 /tsl chatbubble 相关命令
 */
class ChatBubbleCommand(
    private val manager: ChatBubbleManager
) : SubCommandHandler {

    private val serializer = LegacyComponentSerializer.legacyAmpersand()

    override fun handle(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        // 功能检查
        if (!manager.isEnabled()) {
            sender.sendMessage(serializer.deserialize("&c[ChatBubble] &7功能已禁用"))
            return true
        }

        // 玩家检查
        if (sender !is Player) {
            sender.sendMessage(serializer.deserialize("&c[ChatBubble] &7此命令只能由玩家执行"))
            return true
        }

        // 参数解析
        when {
            args.isEmpty() || args[0].equals("self", ignoreCase = true) -> {
                handleSelfToggle(sender)
            }
            args[0].equals("status", ignoreCase = true) -> {
                handleStatus(sender)
            }
            else -> {
                showUsage(sender)
            }
        }

        return true
    }

    /**
     * 处理自我显示切换
     */
    private fun handleSelfToggle(player: Player) {
        val newState = manager.toggleSelfDisplay(player)

        val stateText = if (newState) "&a启用" else "&c禁用"
        player.sendMessage(serializer.deserialize("&6[ChatBubble] &7自我显示已$stateText"))
    }

    /**
     * 显示当前状态
     */
    private fun handleStatus(player: Player) {
        val enabled = manager.getSelfDisplayEnabled(player)
        val bubble = manager.getBubble(player)

        val stateText = if (enabled) "&a启用" else "&c禁用"
        val bubbleText = if (bubble != null && bubble.isValid) "&a存在" else "&7无"

        player.sendMessage(serializer.deserialize("&6[ChatBubble] &7状态:"))
        player.sendMessage(serializer.deserialize("&7- 自我显示: $stateText"))
        player.sendMessage(serializer.deserialize("&7- 当前气泡: $bubbleText"))
    }

    /**
     * 显示使用说明
     */
    private fun showUsage(sender: CommandSender) {
        sender.sendMessage(serializer.deserialize("&6[ChatBubble] &e使用方法:"))
        sender.sendMessage(serializer.deserialize("&7/tsl chatbubble &f- 切换自我显示"))
        sender.sendMessage(serializer.deserialize("&7/tsl chatbubble self &f- 切换自我显示"))
        sender.sendMessage(serializer.deserialize("&7/tsl chatbubble status &f- 查看状态"))
    }

    override fun tabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): List<String> {
        if (!manager.isEnabled()) return emptyList()
        if (sender !is Player) return emptyList()

        return when (args.size) {
            1 -> listOf("self", "status").filter {
                it.startsWith(args[0], ignoreCase = true)
            }
            else -> emptyList()
        }
    }

    override fun getDescription(): String = "聊天气泡功能"
}

