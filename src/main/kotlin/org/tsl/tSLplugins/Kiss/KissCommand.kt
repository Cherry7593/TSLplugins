package org.tsl.tSLplugins.Kiss

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.tsl.tSLplugins.SubCommandHandler

/**
 * Kiss 命令处理器
 * 处理 /tsl kiss 相关命令
 */
class KissCommand(
    private val manager: KissManager,
    private val executor: KissExecutor
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
                // 显示用法
                showUsage(sender)
            }
            args[0].equals("toggle", ignoreCase = true) -> {
                // /tsl kiss toggle - 切换开关
                handleToggle(sender)
            }
            else -> {
                // /tsl kiss <玩家> - 亲吻玩家
                handleKiss(sender, args[0])
            }
        }

        return true
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
     * 处理亲吻玩家
     */
    private fun handleKiss(sender: Player, targetName: String) {
        // 检查发起者是否启用了功能（静默检查，不提示）
        if (!manager.isPlayerEnabled(sender)) {
            return
        }

        // 冷却检查（静默检查，不提示）
        if (manager.isInCooldown(sender.uniqueId) && !sender.hasPermission("tsl.kiss.bypass")) {
            return
        }

        // 查找目标玩家
        val target = Bukkit.getPlayer(targetName)
        if (target == null || !target.isOnline) {
            sender.sendMessage(serializer.deserialize(
                manager.getMessage("player_not_found", "player" to targetName)
            ))
            return
        }

        // 不能亲自己
        if (sender.uniqueId == target.uniqueId) {
            sender.sendMessage(serializer.deserialize(manager.getMessage("cannot_kiss_self")))
            return
        }

        // 执行亲吻
        executor.executeKiss(sender, target)

        // 设置冷却
        if (!sender.hasPermission("tsl.kiss.bypass")) {
            manager.setCooldown(sender.uniqueId)
        }
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
                // 第一个参数：toggle 或玩家名
                val suggestions = mutableListOf("toggle")
                suggestions.addAll(
                    Bukkit.getOnlinePlayers()
                        .filter { it.uniqueId != sender.uniqueId } // 排除自己
                        .map { it.name }
                )
                suggestions.filter { it.startsWith(args[0], ignoreCase = true) }
            }
            else -> emptyList()
        }
    }

    override fun getDescription(): String {
        return "亲吻玩家功能"
    }
}

