package org.tsl.tSLplugins.modules.ping

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.tsl.tSLplugins.SubCommandHandler

/**
 * Ping 命令处理器
 * 处理 /tsl ping 相关命令
 */
class PingModuleCommand(
    private val module: PingModule
) : SubCommandHandler {

    private val serializer = LegacyComponentSerializer.legacyAmpersand()
    private val paginator = PingModulePaginator(module)

    override fun handle(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        // 检查功能是否启用
        if (!module.isEnabled()) {
            sender.sendMessage(serializer.deserialize(module.getModuleMessage("disabled")))
            return true
        }

        // 检查权限
        if (!sender.hasPermission("tsl.ping.use")) {
            sender.sendMessage(serializer.deserialize(module.getModuleMessage("no_permission")))
            return true
        }

        when {
            args.isEmpty() -> {
                if (sender is Player) {
                    handleSelfPing(sender)
                } else {
                    sender.sendMessage(serializer.deserialize(module.getModuleMessage("console_only")))
                }
            }
            args[0].equals("all", ignoreCase = true) -> {
                handleAllPing(sender, args)
            }
            else -> {
                handlePlayerPing(sender, args[0])
            }
        }

        return true
    }

    /**
     * 处理查看自己的延迟
     */
    private fun handleSelfPing(player: Player) {
        val ping = module.getPlayerPing(player)
        val colorCode = module.getPingColorCode(ping)
        val message = module.getModuleMessage(
            "self_ping",
            "ping" to ping.toString()
        ).replace("{color}", colorCode)
        player.sendMessage(serializer.deserialize(message))
    }

    /**
     * 处理查看指定玩家的延迟
     */
    private fun handlePlayerPing(sender: CommandSender, playerName: String) {
        val targetPlayer = Bukkit.getPlayer(playerName)

        if (targetPlayer == null || !targetPlayer.isOnline) {
            val message = module.getModuleMessage("player_not_found", "player" to playerName)
            sender.sendMessage(serializer.deserialize(message))
            return
        }

        val ping = module.getPlayerPing(targetPlayer)
        val colorCode = module.getPingColorCode(ping)
        val message = module.getModuleMessage(
            "player_ping",
            "player" to targetPlayer.name,
            "ping" to ping.toString()
        ).replace("{color}", colorCode)
        sender.sendMessage(serializer.deserialize(message))
    }

    /**
     * 处理查看所有玩家延迟
     */
    private fun handleAllPing(sender: CommandSender, args: Array<out String>) {
        if (!sender.hasPermission("tsl.ping.all")) {
            sender.sendMessage(serializer.deserialize(module.getModuleMessage("no_permission")))
            return
        }

        val page = paginator.parsePage(args, 1, 1)
        paginator.showPingList(sender, page)
    }

    override fun tabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): List<String> {
        if (!module.isEnabled()) {
            return emptyList()
        }

        return when (args.size) {
            1 -> {
                val completions = mutableListOf<String>()

                completions.addAll(
                    Bukkit.getOnlinePlayers()
                        .map { it.name }
                        .filter { it.startsWith(args[0], ignoreCase = true) }
                )

                if (sender.hasPermission("tsl.ping.all") && "all".startsWith(args[0], ignoreCase = true)) {
                    completions.add("all")
                }

                completions.sorted()
            }
            2 -> {
                if (args[0].equals("all", ignoreCase = true)) {
                    val totalPlayers = Bukkit.getOnlinePlayers().size
                    val totalPages = (totalPlayers + module.getEntriesPerPage() - 1) / module.getEntriesPerPage()
                    (1..totalPages).map { it.toString() }
                } else {
                    emptyList()
                }
            }
            else -> emptyList()
        }
    }

    override fun getDescription(): String = "查询玩家延迟信息"
}
