package org.tsl.tSLplugins.Ping

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
class PingCommand(
    private val manager: PingManager
) : SubCommandHandler {

    private val serializer = LegacyComponentSerializer.legacyAmpersand()
    private val paginator = PingPaginator(manager)

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

        // 检查权限
        if (!sender.hasPermission("tsl.ping.use")) {
            sender.sendMessage(serializer.deserialize(manager.getMessage("no_permission")))
            return true
        }

        when {
            args.isEmpty() -> {
                // /tsl ping - 显示自己的延迟（仅玩家）
                if (sender is Player) {
                    handleSelfPing(sender)
                } else {
                    sender.sendMessage(serializer.deserialize(manager.getMessage("console_only")))
                }
            }
            args[0].equals("all", ignoreCase = true) -> {
                // /tsl ping all [page] - 显示所有玩家延迟
                handleAllPing(sender, args)
            }
            else -> {
                // /tsl ping <player> - 显示指定玩家延迟
                handlePlayerPing(sender, args[0])
            }
        }

        return true
    }

    /**
     * 处理查看自己的延迟
     */
    private fun handleSelfPing(player: Player) {
        val ping = manager.getPlayerPing(player)
        val colorCode = manager.getPingColorCode(ping)
        val message = manager.getMessage(
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
            val message = manager.getMessage("player_not_found", "player" to playerName)
            sender.sendMessage(serializer.deserialize(message))
            return
        }

        val ping = manager.getPlayerPing(targetPlayer)
        val colorCode = manager.getPingColorCode(ping)
        val message = manager.getMessage(
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
        // 检查权限
        if (!sender.hasPermission("tsl.ping.all")) {
            sender.sendMessage(serializer.deserialize(manager.getMessage("no_permission")))
            return
        }

        // 必须是玩家才能使用（需要可点击的分页按钮）
        if (sender !is Player) {
            sender.sendMessage(serializer.deserialize(manager.getMessage("console_only")))
            return
        }

        // 解析页码
        val page = paginator.parsePage(args, 1, 1)

        // 显示延迟列表
        paginator.showPingList(sender, page)
    }

    override fun tabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): List<String> {
        if (!manager.isEnabled()) {
            return emptyList()
        }

        return when (args.size) {
            1 -> {
                val completions = mutableListOf<String>()

                // 添加在线玩家名称
                completions.addAll(
                    Bukkit.getOnlinePlayers()
                        .map { it.name }
                        .filter { it.startsWith(args[0], ignoreCase = true) }
                )

                // 添加 "all" 子命令（如果有权限）
                if (sender.hasPermission("tsl.ping.all") && "all".startsWith(args[0], ignoreCase = true)) {
                    completions.add("all")
                }

                completions.sorted()
            }
            2 -> {
                // /tsl ping all [page] - 提供页码补全
                if (args[0].equals("all", ignoreCase = true)) {
                    val totalPlayers = Bukkit.getOnlinePlayers().size
                    val totalPages = (totalPlayers + manager.getEntriesPerPage() - 1) / manager.getEntriesPerPage()
                    (1..totalPages).map { it.toString() }
                } else {
                    emptyList()
                }
            }
            else -> emptyList()
        }
    }

    override fun getDescription(): String {
        return "查询玩家延迟信息"
    }
}

