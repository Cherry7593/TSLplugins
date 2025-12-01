package org.tsl.tSLplugins.Near

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.tsl.tSLplugins.SubCommand

/**
 * Near 命令处理器
 * /tsl near [范围] - 查找附近的玩家
 */
class NearCommand(private val manager: NearManager) : SubCommand {

    override fun handle(sender: CommandSender, args: Array<out String>): Boolean {
        // 检查功能是否启用
        if (!manager.isEnabled()) {
            sender.sendMessage(Component.text("附近玩家功能未启用！", NamedTextColor.RED))
            return true
        }

        // 只允许玩家使用
        if (sender !is Player) {
            sender.sendMessage(Component.text("此命令只能由玩家使用！", NamedTextColor.RED))
            return true
        }

        // 权限检查
        if (!sender.hasPermission("tsl.near.use")) {
            sender.sendMessage(Component.text("你没有权限使用此命令！", NamedTextColor.RED))
            return true
        }

        // 解析半径参数
        val radius = if (args.isEmpty()) {
            // 没有参数，使用默认值
            manager.getDefaultRadius()
        } else {
            // 解析半径
            try {
                args[0].toInt()
            } catch (e: NumberFormatException) {
                sender.sendMessage(
                    Component.text("无效的范围！", NamedTextColor.RED)
                        .append(Component.text(" 请输入一个数字。", NamedTextColor.GRAY))
                )
                return true
            }
        }

        // 检查半径限制
        val maxRadius = if (sender.hasPermission("tsl.near.bypass")) {
            // OP 或有 bypass 权限，无限制
            radius
        } else {
            // 普通玩家，限制最大范围
            val max = manager.getMaxRadius()
            if (radius > max) {
                sender.sendMessage(
                    Component.text("范围过大！", NamedTextColor.RED)
                        .append(Component.text(" 最大范围: $max 米", NamedTextColor.GRAY))
                )
                return true
            }
            radius
        }

        // 检查半径是否为正数
        if (maxRadius <= 0) {
            sender.sendMessage(Component.text("范围必须大于 0！", NamedTextColor.RED))
            return true
        }

        // 使用玩家的调度器执行查询（Folia 线程安全）
        sender.scheduler.run(manager.plugin, { _ ->
            try {
                // 查找附近玩家
                val nearbyPlayers = manager.findNearbyPlayers(sender, maxRadius)

                // 显示结果
                if (nearbyPlayers.isEmpty()) {
                    sender.sendMessage(
                        Component.text("附近 $maxRadius 米内没有其他玩家", NamedTextColor.YELLOW)
                    )
                } else {
                    // 标题
                    sender.sendMessage(
                        Component.text("========== ", NamedTextColor.GRAY)
                            .append(Component.text("附近玩家", NamedTextColor.GOLD, TextDecoration.BOLD))
                            .append(Component.text(" ==========", NamedTextColor.GRAY))
                    )
                    sender.sendMessage(
                        Component.text("搜索范围: ", NamedTextColor.YELLOW)
                            .append(Component.text("$maxRadius 米", NamedTextColor.GREEN))
                    )
                    sender.sendMessage(Component.empty())

                    // 显示玩家列表
                    nearbyPlayers.forEachIndexed { index, (player, distance) ->
                        val distanceStr = manager.formatDistance(distance)
                        sender.sendMessage(
                            Component.text("${index + 1}. ", NamedTextColor.GRAY)
                                .append(Component.text(player.name, NamedTextColor.WHITE))
                                .append(Component.text(" ~ ", NamedTextColor.DARK_GRAY))
                                .append(Component.text(distanceStr, NamedTextColor.AQUA))
                        )
                    }

                    // 底部
                    sender.sendMessage(Component.empty())
                    sender.sendMessage(
                        Component.text("共 ${nearbyPlayers.size} 名玩家", NamedTextColor.GRAY)
                    )
                    sender.sendMessage(
                        Component.text("====================================", NamedTextColor.GRAY)
                    )
                }
            } catch (e: Exception) {
                sender.sendMessage(Component.text("查询失败: ${e.message}", NamedTextColor.RED))
                manager.plugin.logger.warning("[Near] 查询失败: ${e.message}")
                e.printStackTrace()
            }
        }, null)

        return true
    }

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        // 只有玩家才能使用
        if (sender !is Player) return emptyList()
        if (!sender.hasPermission("tsl.near.use")) return emptyList()

        return when (args.size) {
            1 -> {
                // 提示常用范围
                listOf("50", "100", "500", "1000")
                    .filter { it.startsWith(args[0], ignoreCase = true) }
            }
            else -> emptyList()
        }
    }
}

