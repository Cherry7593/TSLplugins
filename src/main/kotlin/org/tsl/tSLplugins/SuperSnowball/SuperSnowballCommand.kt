package org.tsl.tSLplugins.SuperSnowball

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.tsl.tSLplugins.SubCommandHandler

/**
 * 超级大雪球命令处理器
 * 处理 /tsl ss give [player] 命令
 */
class SuperSnowballCommand(
    private val manager: SuperSnowballManager
) : SubCommandHandler {

    private val serializer = LegacyComponentSerializer.legacyAmpersand()

    override fun handle(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (!manager.isEnabled()) {
            sender.sendMessage(serializer.deserialize("&c超级大雪球功能已禁用"))
            return true
        }

        if (args.isEmpty()) {
            showHelp(sender)
            return true
        }

        when (args[0].lowercase()) {
            "give" -> handleGive(sender, args)
            else -> showHelp(sender)
        }

        return true
    }

    private fun handleGive(sender: CommandSender, args: Array<out String>) {
        // 权限检查
        if (!sender.hasPermission("tsl.ss.give")) {
            sender.sendMessage(serializer.deserialize("&c没有权限执行此命令"))
            return
        }

        val targetPlayer: Player = when {
            args.size >= 2 -> {
                // 指定了玩家
                Bukkit.getPlayer(args[1]) ?: run {
                    sender.sendMessage(serializer.deserialize("&c玩家 ${args[1]} 不在线"))
                    return
                }
            }
            sender is Player -> sender
            else -> {
                sender.sendMessage(serializer.deserialize("&c控制台必须指定玩家"))
                return
            }
        }

        // 给予超级大雪球
        val snowball = manager.createSuperSnowball()
        val remaining = targetPlayer.inventory.addItem(snowball)

        if (remaining.isEmpty()) {
            if (sender == targetPlayer) {
                sender.sendMessage(serializer.deserialize("&a你获得了 &b超级大雪球&a！"))
            } else {
                sender.sendMessage(serializer.deserialize("&a已给予 &f${targetPlayer.name} &a一个 &b超级大雪球"))
                targetPlayer.sendMessage(serializer.deserialize("&a你获得了 &b超级大雪球&a！"))
            }
        } else {
            sender.sendMessage(serializer.deserialize("&c${targetPlayer.name} 的背包已满"))
        }
    }

    private fun showHelp(sender: CommandSender) {
        sender.sendMessage(serializer.deserialize("&6===== 超级大雪球 ====="))
        sender.sendMessage(serializer.deserialize("&e/tsl ss give [玩家] &7- 给予超级大雪球"))
    }

    override fun tabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): List<String> {
        if (!manager.isEnabled()) return emptyList()

        return when (args.size) {
            1 -> listOf("give").filter { it.startsWith(args[0], true) }
            2 -> {
                if (args[0].equals("give", true) && sender.hasPermission("tsl.ss.give")) {
                    Bukkit.getOnlinePlayers().map { it.name }.filter { it.startsWith(args[1], true) }
                } else {
                    emptyList()
                }
            }
            else -> emptyList()
        }
    }

    override fun getDescription(): String = "超级大雪球功能"
}
