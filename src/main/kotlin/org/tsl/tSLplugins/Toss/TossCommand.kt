package org.tsl.tSLplugins.Toss

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.tsl.tSLplugins.SubCommandHandler

/**
 * Toss 命令处理器
 * 处理 /tsl toss 相关命令
 */
class TossCommand(
    private val manager: TossManager
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
            args.isEmpty() -> {
                // /tsl toss - 显示当前状态
                showStatus(sender)
            }
            args[0].equals("toggle", ignoreCase = true) -> {
                // /tsl toss toggle - 切换开关
                handleToggle(sender)
            }
            args[0].equals("velocity", ignoreCase = true) && args.size >= 2 -> {
                // /tsl toss velocity <数值> - 设置投掷速度
                handleVelocity(sender, args[1])
            }
            args[0].equals("velocity", ignoreCase = true) -> {
                // /tsl toss velocity - 显示当前速度
                showVelocity(sender)
            }
            else -> {
                // 显示用法
                showUsage(sender)
            }
        }

        return true
    }

    /**
     * 显示当前状态
     */
    private fun showStatus(player: Player) {
        val enabled = manager.isPlayerEnabled(player)
        val velocity = manager.getPlayerThrowVelocity(player)

        val statusMessage = manager.getMessage(
            "status",
            "status" to if (enabled) "已启用" else "已禁用",
            "velocity" to String.format("%.1f", velocity)
        )
        player.sendMessage(serializer.deserialize(statusMessage))
    }

    /**
     * 处理切换开关
     */
    private fun handleToggle(player: Player) {
        val newStatus = manager.togglePlayer(player)

        val message = if (newStatus) {
            manager.getMessage("toggle_enabled")
        } else {
            manager.getMessage("toggle_disabled")
        }

        player.sendMessage(serializer.deserialize(message))
    }

    /**
     * 处理设置投掷速度
     */
    private fun handleVelocity(player: Player, velocityStr: String) {
        // 检查权限
        if (!player.hasPermission("tsl.toss.velocity")) {
            player.sendMessage(serializer.deserialize(manager.getMessage("no_permission")))
            return
        }

        // 解析速度值
        val velocity = velocityStr.toDoubleOrNull()
        if (velocity == null) {
            player.sendMessage(serializer.deserialize(manager.getMessage("invalid_velocity")))
            return
        }

        // 速度值基本验证（防止负数或过大值导致问题）
        if (velocity < 0.0) {
            player.sendMessage(serializer.deserialize(manager.getMessage("invalid_velocity")))
            return
        }

        // OP 或有 bypass 权限可以无视配置限制
        val hasBypass = player.isOp || player.hasPermission("tsl.toss.velocity.bypass")

        if (!hasBypass) {
            // 普通玩家需要遵守配置文件的范围限制
            val min = manager.getThrowVelocityMin()
            val max = manager.getThrowVelocityMax()

            if (velocity < min || velocity > max) {
                val message = manager.getMessage(
                    "velocity_out_of_range",
                    "min" to String.format("%.1f", min),
                    "max" to String.format("%.1f", max)
                )
                player.sendMessage(serializer.deserialize(message))
                return
            }
        } else {
            // OP/管理员最大限制为 10.0（Minecraft 服务器默认上限）
            if (velocity > 10.0) {
                player.sendMessage(serializer.deserialize(
                    "&c速度过大！建议范围: 0.1-10.0 (当前: ${String.format("%.1f", velocity)})"
                ))
                return
            }
        }

        // 设置速度（不再受配置文件限制约束）
        manager.setPlayerThrowVelocityUnrestricted(player, velocity)

        val message = manager.getMessage(
            "velocity_set",
            "velocity" to String.format("%.1f", velocity)
        )
        player.sendMessage(serializer.deserialize(message))
    }

    /**
     * 显示当前投掷速度
     */
    private fun showVelocity(player: Player) {
        val velocity = manager.getPlayerThrowVelocity(player)
        val message = manager.getMessage(
            "velocity_current",
            "velocity" to String.format("%.1f", velocity),
            "min" to String.format("%.1f", manager.getThrowVelocityMin()),
            "max" to String.format("%.1f", manager.getThrowVelocityMax())
        )
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
                val completions = mutableListOf("toggle", "velocity")
                completions.filter { it.startsWith(args[0], ignoreCase = true) }
            }
            2 -> {
                if (args[0].equals("velocity", ignoreCase = true) && sender.hasPermission("tsl.toss.velocity")) {
                    // 根据玩家权限提供不同的速度建议
                    val suggestions = if (sender.isOp || sender.hasPermission("tsl.toss.velocity.bypass")) {
                        // OP/管理员：提供更多选项
                        listOf("0.5", "1.0", "1.5", "2.0", "3.0", "5.0", "8.0", "10.0")
                    } else {
                        // 普通玩家：只提供配置范围内的值
                        listOf("0.5", "1.0", "1.5", "2.0", "2.5", "3.0", "4.0", "5.0")
                    }
                    suggestions.filter { it.startsWith(args[1]) }
                } else {
                    emptyList()
                }
            }
            else -> emptyList()
        }
    }

    override fun getDescription(): String {
        return "生物举起功能管理"
    }
}

