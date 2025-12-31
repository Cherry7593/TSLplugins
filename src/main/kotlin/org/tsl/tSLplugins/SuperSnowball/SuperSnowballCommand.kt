package org.tsl.tSLplugins.SuperSnowball

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.tsl.tSLplugins.SubCommandHandler

/**
 * 超级大雪球命令处理器
 * 处理 /tsl ss give [player] [数量] 命令
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

        // 解析参数: /tsl ss give [player] [数量]
        var targetPlayer: Player? = null
        var amount = 1

        when {
            args.size >= 3 -> {
                // 指定了玩家和数量
                targetPlayer = Bukkit.getPlayer(args[1])
                if (targetPlayer == null) {
                    sender.sendMessage(serializer.deserialize("&c玩家 ${args[1]} 不在线"))
                    return
                }
                amount = args[2].toIntOrNull() ?: run {
                    sender.sendMessage(serializer.deserialize("&c无效数量: ${args[2]}"))
                    return
                }
            }
            args.size == 2 -> {
                // 可能是玩家名或数量
                val asAmount = args[1].toIntOrNull()
                if (asAmount != null) {
                    // 是数量，给自己
                    if (sender !is Player) {
                        sender.sendMessage(serializer.deserialize("&c控制台必须指定玩家"))
                        return
                    }
                    targetPlayer = sender
                    amount = asAmount
                } else {
                    // 是玩家名
                    targetPlayer = Bukkit.getPlayer(args[1])
                    if (targetPlayer == null) {
                        sender.sendMessage(serializer.deserialize("&c玩家 ${args[1]} 不在线"))
                        return
                    }
                }
            }
            else -> {
                // 没有参数，给自己
                if (sender !is Player) {
                    sender.sendMessage(serializer.deserialize("&c控制台必须指定玩家"))
                    return
                }
                targetPlayer = sender
            }
        }

        // 限制数量范围
        if (amount < 1) amount = 1
        if (amount > 64) amount = 64

        // 给予超级大雪球
        val snowball = manager.createSuperSnowball()
        snowball.amount = amount
        val remaining = targetPlayer.inventory.addItem(snowball)

        val given = amount - (remaining.values.firstOrNull()?.amount ?: 0)
        if (given > 0) {
            if (sender == targetPlayer) {
                sender.sendMessage(serializer.deserialize("&a你获得了 &b${given} &a个 &b超级大雪球&a！"))
            } else {
                sender.sendMessage(serializer.deserialize("&a已给予 &f${targetPlayer.name} &b${given} &a个 &b超级大雪球"))
                targetPlayer.sendMessage(serializer.deserialize("&a你获得了 &b${given} &a个 &b超级大雪球&a！"))
            }
            if (remaining.isNotEmpty()) {
                sender.sendMessage(serializer.deserialize("&e背包已满，部分物品无法给予"))
            }
        } else {
            sender.sendMessage(serializer.deserialize("&c${targetPlayer.name} 的背包已满"))
        }
    }

    private fun showHelp(sender: CommandSender) {
        sender.sendMessage(serializer.deserialize("&6===== 超级大雪球 ====="))
        sender.sendMessage(serializer.deserialize("&e/tsl ss give [玩家] [数量] &7- 给予超级大雪球"))
        sender.sendMessage(serializer.deserialize("&7示例: /tsl ss give 10 &7- 给自己10个"))
        sender.sendMessage(serializer.deserialize("&7示例: /tsl ss give Steve 5 &7- 给Steve 5个"))
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
                    // 玩家名 + 常用数量
                    val players = Bukkit.getOnlinePlayers().map { it.name }
                    val amounts = listOf("1", "16", "32", "64")
                    (players + amounts).filter { it.startsWith(args[1], true) }
                } else {
                    emptyList()
                }
            }
            3 -> {
                if (args[0].equals("give", true) && sender.hasPermission("tsl.ss.give")) {
                    // 数量补全
                    listOf("1", "16", "32", "64").filter { it.startsWith(args[2], true) }
                } else {
                    emptyList()
                }
            }
            else -> emptyList()
        }
    }

    override fun getDescription(): String = "超级大雪球功能"
}
