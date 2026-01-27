package org.tsl.tSLplugins.modules.toss

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.tsl.tSLplugins.SubCommandHandler

/**
 * Toss 命令处理器
 * 处理 /tsl toss 相关命令
 */
class TossModuleCommand(
    private val module: TossModule
) : SubCommandHandler {

    private val serializer = LegacyComponentSerializer.legacyAmpersand()

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

        // 必须是玩家
        if (sender !is Player) {
            sender.sendMessage(serializer.deserialize(module.getModuleMessage("console_only")))
            return true
        }

        when {
            args.isEmpty() -> showStatus(sender)
            args[0].equals("toggle", ignoreCase = true) -> handleToggle(sender)
            args[0].equals("velocity", ignoreCase = true) && args.size >= 2 -> handleVelocity(sender, args[1])
            args[0].equals("velocity", ignoreCase = true) -> showVelocity(sender)
            else -> showUsage(sender)
        }

        return true
    }

    /**
     * 显示当前状态
     */
    private fun showStatus(player: Player) {
        val enabled = module.isPlayerEnabled(player)
        val velocity = module.getPlayerThrowVelocity(player)

        val statusMessage = module.getModuleMessage(
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
        val newStatus = module.togglePlayer(player)

        val message = if (newStatus) {
            module.getModuleMessage("toggle_enabled")
        } else {
            module.getModuleMessage("toggle_disabled")
        }

        player.sendMessage(serializer.deserialize(message))
    }

    /**
     * 处理设置投掷速度
     */
    private fun handleVelocity(player: Player, velocityStr: String) {
        // 检查权限
        if (!player.hasPermission("tsl.toss.velocity")) {
            player.sendMessage(serializer.deserialize(module.getModuleMessage("no_permission")))
            return
        }

        // 解析速度值
        val velocity = velocityStr.toDoubleOrNull()
        if (velocity == null || velocity < 0.0) {
            player.sendMessage(serializer.deserialize(module.getModuleMessage("invalid_velocity")))
            return
        }

        // OP 或有 bypass 权限可以无视配置限制
        val hasBypass = player.isOp || player.hasPermission("tsl.toss.velocity.bypass")

        if (!hasBypass) {
            val min = module.getThrowVelocityMin()
            val max = module.getThrowVelocityMax()

            if (velocity < min || velocity > max) {
                val message = module.getModuleMessage(
                    "velocity_out_of_range",
                    "min" to String.format("%.1f", min),
                    "max" to String.format("%.1f", max)
                )
                player.sendMessage(serializer.deserialize(message))
                return
            }
        } else {
            if (velocity > 10.0) {
                player.sendMessage(serializer.deserialize(
                    "&c速度过大！建议范围: 0.1-10.0 (当前: ${String.format("%.1f", velocity)})"
                ))
                return
            }
        }

        module.setPlayerThrowVelocityUnrestricted(player, velocity)

        val message = module.getModuleMessage(
            "velocity_set",
            "velocity" to String.format("%.1f", velocity)
        )
        player.sendMessage(serializer.deserialize(message))
    }

    /**
     * 显示当前投掷速度
     */
    private fun showVelocity(player: Player) {
        val velocity = module.getPlayerThrowVelocity(player)
        val message = module.getModuleMessage(
            "velocity_current",
            "velocity" to String.format("%.1f", velocity),
            "min" to String.format("%.1f", module.getThrowVelocityMin()),
            "max" to String.format("%.1f", module.getThrowVelocityMax())
        )
        player.sendMessage(serializer.deserialize(message))
    }

    /**
     * 显示用法
     */
    private fun showUsage(sender: CommandSender) {
        sender.sendMessage(serializer.deserialize(module.getModuleMessage("usage")))
    }

    override fun tabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): List<String> {
        if (!module.isEnabled() || sender !is Player) {
            return emptyList()
        }

        return when (args.size) {
            1 -> {
                val completions = mutableListOf("toggle", "velocity")
                completions.filter { it.startsWith(args[0], ignoreCase = true) }
            }
            2 -> {
                if (args[0].equals("velocity", ignoreCase = true) && sender.hasPermission("tsl.toss.velocity")) {
                    val suggestions = if (sender.isOp || sender.hasPermission("tsl.toss.velocity.bypass")) {
                        listOf("0.5", "1.0", "1.5", "2.0", "3.0", "5.0", "8.0", "10.0")
                    } else {
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

    override fun getDescription(): String = "生物举起功能管理"
}
