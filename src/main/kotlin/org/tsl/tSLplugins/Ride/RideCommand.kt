package org.tsl.tSLplugins.Ride

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.tsl.tSLplugins.SubCommandHandler

/**
 * Ride 命令处理器
 * 处理 /tsl ride 相关命令
 */
class RideCommand(
    private val manager: RideManager
) : SubCommandHandler {

    private val serializer = LegacyComponentSerializer.legacyAmpersand()

    override fun handle(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        // 检查功能是否启用
        if (!manager.isEnabled()) {
            sender.sendMessage(serializer.deserialize(manager.getMessage("disabled")))
            return true
        }

        // 必须是玩家
        if (sender !is Player) {
            sender.sendMessage(serializer.deserialize(manager.getMessage("console_only")))
            return true
        }

        when {
            args.isEmpty() || args[0].equals("toggle", ignoreCase = true) -> {
                // /tsl ride toggle - 切换开关
                handleToggle(sender)
            }
            else -> {
                // 显示用法
                showUsage(sender)
            }
        }

        return true
    }

    /**
     * 处理切换开关
     */
    private fun handleToggle(player: Player) {
        val newStatus = manager.togglePlayer(player.uniqueId)

        val message = if (newStatus) {
            manager.getMessage("toggle_enabled")
        } else {
            manager.getMessage("toggle_disabled")
        }

        player.sendMessage(serializer.deserialize(message))
    }

    /**
     * 显示用法
     */
    private fun showUsage(sender: CommandSender) {
        sender.sendMessage(serializer.deserialize(manager.getMessage("usage")))
    }

    override fun tabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): List<String> {
        if (!manager.isEnabled() || sender !is Player) {
            return emptyList()
        }

        return when (args.size) {
            1 -> {
                listOf("toggle").filter { it.startsWith(args[0], ignoreCase = true) }
            }
            else -> emptyList()
        }
    }

    override fun getDescription(): String {
        return "右键骑乘生物功能管理"
    }
}

