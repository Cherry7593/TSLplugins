package org.tsl.tSLplugins.PlayerList

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.tsl.tSLplugins.SubCommand

/**
 * 玩家列表命令处理器
 * /tsl list - 按世界分类显示在线玩家
 */
class PlayerListCommand : SubCommand {

    override fun handle(sender: CommandSender, args: Array<out String>): Boolean {
        // 获取所有在线玩家并按世界分组
        val playersByWorld = Bukkit.getOnlinePlayers()
            .groupBy { it.world }
            .toSortedMap(compareBy { getWorldDisplayOrder(it) })

        // 总在线人数
        val totalPlayers = Bukkit.getOnlinePlayers().size

        // 发送标题
        sender.sendMessage(
            Component.text("========== ", NamedTextColor.GRAY)
                .append(Component.text("在线玩家列表", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text(" ==========", NamedTextColor.GRAY))
        )
        sender.sendMessage(
            Component.text("总在线: ", NamedTextColor.YELLOW)
                .append(Component.text("$totalPlayers 人", NamedTextColor.GREEN))
        )
        sender.sendMessage(Component.empty())

        // 按世界显示玩家
        if (playersByWorld.isEmpty()) {
            sender.sendMessage(Component.text("当前没有玩家在线", NamedTextColor.RED))
        } else {
            for ((world, players) in playersByWorld) {
                // 世界名称
                val worldDisplayName = getWorldDisplayName(world)
                val playerCount = players.size

                // 发送世界标题
                sender.sendMessage(
                    Component.text("▸ ", NamedTextColor.GRAY)
                        .append(Component.text(worldDisplayName, NamedTextColor.AQUA, TextDecoration.BOLD))
                        .append(Component.text(" ($playerCount)", NamedTextColor.GRAY))
                )

                if (players.isEmpty()) {
                    sender.sendMessage(
                        Component.text("  ", NamedTextColor.GRAY)
                            .append(Component.text("无人在此地", NamedTextColor.DARK_GRAY))
                    )
                } else {
                    // 构建玩家列表
                    val playerNames = players.joinToString(", ") {
                        formatPlayerName(it)
                    }
                    sender.sendMessage(
                        Component.text("  ", NamedTextColor.GRAY)
                            .append(Component.text(playerNames, NamedTextColor.WHITE))
                    )
                }

                // 空行分隔
                sender.sendMessage(Component.empty())
            }
        }

        // 底部分隔线
        sender.sendMessage(
            Component.text("====================================", NamedTextColor.GRAY)
        )

        return true
    }

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        // 无参数补全
        return emptyList()
    }

    /**
     * 获取世界显示名称
     */
    private fun getWorldDisplayName(world: World): String {
        return when (world.environment) {
            World.Environment.NORMAL -> "主世界"
            World.Environment.NETHER -> "下界"
            World.Environment.THE_END -> "末地"
            else -> world.name
        }
    }

    /**
     * 格式化玩家名称（可根据需要添加颜色等）
     */
    private fun formatPlayerName(player: Player): String {
        // 基础显示：玩家名
        // 可以根据需要添加前缀、颜色等
        return player.name
    }

    /**
     * 获取世界排序优先级（主世界 > 下界 > 末地 > 其他）
     */
    private fun getWorldDisplayOrder(world: World): Int {
        return when (world.environment) {
            World.Environment.NORMAL -> 0
            World.Environment.NETHER -> 1
            World.Environment.THE_END -> 2
            else -> 3
        }
    }
}

