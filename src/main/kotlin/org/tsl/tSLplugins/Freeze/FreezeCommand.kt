package org.tsl.tSLplugins.Freeze

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.tsl.tSLplugins.SubCommandHandler

/**
 * Freeze 命令处理器
 * 处理 /tsl freeze 相关命令
 */
class FreezeCommand(
    private val manager: FreezeManager
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

        // 权限检查
        if (!sender.hasPermission("tsl.freeze.use")) {
            sender.sendMessage(serializer.deserialize(manager.getMessage("no_permission")))
            return true
        }

        when {
            args.isEmpty() -> {
                showUsage(sender)
            }
            args[0].equals("list", ignoreCase = true) -> {
                // /tsl freeze list - 列出被冻结的玩家
                handleList(sender)
            }
            else -> {
                // /tsl freeze <玩家> [时间] - 自动切换冻结/解冻
                val duration = if (args.size >= 2) {
                    args[1].toIntOrNull() ?: -1
                } else {
                    -1
                }
                handleToggleFreeze(sender, args[0], duration)
            }
        }

        return true
    }

    /**
     * 处理冻结/解冻切换
     */
    private fun handleToggleFreeze(sender: CommandSender, targetName: String, duration: Int) {
        // 查找目标玩家
        val target = Bukkit.getPlayer(targetName)
        if (target == null || !target.isOnline) {
            sender.sendMessage(serializer.deserialize(
                manager.getMessage("player_not_found", "player" to targetName)
            ))
            return
        }

        // 检查目标是否有 bypass 权限
        if (target.hasPermission("tsl.freeze.bypass")) {
            sender.sendMessage(serializer.deserialize(
                manager.getMessage("target_bypass", "player" to target.name)
            ))
            return
        }

        // 检查当前是否已被冻结
        if (manager.isFrozen(target.uniqueId)) {
            // 已冻结，执行解冻
            manager.unfreezePlayer(target.uniqueId)

            // 发送消息给执行者
            sender.sendMessage(serializer.deserialize(
                manager.getMessage("unfreeze_success", "player" to target.name)
            ))

            // 发送消息给目标玩家
            target.sendMessage(serializer.deserialize(manager.getMessage("unfrozen")))
        } else {
            // 未冻结，执行冻结
            manager.freezePlayer(target.uniqueId, duration)

            // 发送消息给执行者
            val durationText = if (duration > 0) {
                formatTime(duration)
            } else {
                "永久"
            }
            sender.sendMessage(serializer.deserialize(
                manager.getMessage("freeze_success",
                    "player" to target.name,
                    "duration" to durationText)
            ))

            // 发送消息给目标玩家
            target.sendMessage(serializer.deserialize(
                manager.getMessage("frozen", "duration" to durationText)
            ))
        }
    }

    /**
     * 处理列出被冻结的玩家
     */
    private fun handleList(sender: CommandSender) {
        val frozenPlayers = manager.getFrozenPlayers()

        if (frozenPlayers.isEmpty()) {
            sender.sendMessage(serializer.deserialize(manager.getMessage("no_frozen_players")))
            return
        }

        // 发送标题
        sender.sendMessage(serializer.deserialize(manager.getMessage("list_header")))

        // 列出每个被冻结的玩家
        frozenPlayers.forEach { (uuid, _) ->
            val player = Bukkit.getOfflinePlayer(uuid)
            val name = player.name ?: uuid.toString()
            val remaining = manager.getRemainingTime(uuid)
            val timeText = if (remaining < 0) {
                "永久"
            } else {
                formatTime(remaining)
            }
            val status = if (player.isOnline) "在线" else "离线"

            sender.sendMessage(serializer.deserialize(
                manager.getMessage("list_entry",
                    "player" to name,
                    "time" to timeText,
                    "status" to status)
            ))
        }
    }

    /**
     * 显示用法
     */
    private fun showUsage(sender: CommandSender) {
        sender.sendMessage(serializer.deserialize(manager.getMessage("usage")))
    }

    /**
     * 格式化时间
     */
    private fun formatTime(seconds: Int): String {
        return when {
            seconds < 60 -> "${seconds}秒"
            seconds < 3600 -> "${seconds / 60}分${seconds % 60}秒"
            else -> "${seconds / 3600}小时${(seconds % 3600) / 60}分"
        }
    }

    override fun tabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): List<String> {
        if (!manager.isEnabled() || !sender.hasPermission("tsl.freeze.use")) {
            return emptyList()
        }

        return when (args.size) {
            1 -> {
                val suggestions = mutableListOf("list")
                suggestions.addAll(
                    Bukkit.getOnlinePlayers()
                        .filter { !it.hasPermission("tsl.freeze.bypass") }
                        .map { it.name }
                )
                suggestions.filter { it.startsWith(args[0], ignoreCase = true) }
            }
            2 -> {
                // 冻结命令：显示时间建议
                listOf("60", "300", "600", "1800", "3600")
                    .filter { it.startsWith(args[1]) }
            }
            else -> emptyList()
        }
    }

    override fun getDescription(): String {
        return "冻结玩家功能"
    }
}

