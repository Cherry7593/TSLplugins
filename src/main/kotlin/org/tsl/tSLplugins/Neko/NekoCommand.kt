package org.tsl.tSLplugins.Neko

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.tsl.tSLplugins.SubCommandHandler

/**
 * 猫娘模式命令处理器
 * 处理 /tsl neko 相关命令
 */
class NekoCommand(
    private val manager: NekoManager
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

        if (args.isEmpty()) {
            showUsage(sender)
            return true
        }

        when (args[0].lowercase()) {
            "set" -> handleSet(sender, args.drop(1).toTypedArray())
            "reset", "remove" -> handleReset(sender, args.drop(1).toTypedArray())
            "list" -> handleList(sender)
            "check" -> handleCheck(sender, args.drop(1).toTypedArray())
            else -> showUsage(sender)
        }

        return true
    }

    /**
     * 处理设置猫娘命令
     * /tsl neko set <player> <duration>
     */
    private fun handleSet(sender: CommandSender, args: Array<out String>) {
        // 权限检查
        if (!sender.hasPermission("tsl.neko.set")) {
            sender.sendMessage(serializer.deserialize(manager.getMessage("no_permission")))
            return
        }

        if (args.size < 2) {
            sender.sendMessage(serializer.deserialize(
                "&c用法: /tsl neko set <玩家> <时间>"
            ))
            sender.sendMessage(serializer.deserialize(
                "&7示例: /tsl neko set Steve 30m"
            ))
            sender.sendMessage(serializer.deserialize(
                "&7时间格式: 10s, 5m, 2h, 1d 或纯数字(秒)"
            ))
            return
        }

        val playerName = args[0]
        val durationStr = args[1]

        // 查找玩家
        val target = Bukkit.getPlayer(playerName)
        if (target == null || !target.isOnline) {
            sender.sendMessage(serializer.deserialize(
                manager.getMessage("player_not_found", "player" to playerName)
            ))
            return
        }

        // 解析持续时间
        val durationMs = manager.parseDuration(durationStr)
        if (durationMs == null || durationMs <= 0) {
            sender.sendMessage(serializer.deserialize(
                manager.getMessage("invalid_duration", "duration" to durationStr)
            ))
            return
        }

        // 设置猫娘状态
        val success = manager.setNeko(target, durationMs, "command:${sender.name}")

        if (success) {
            val durationText = formatDuration(durationMs)
            sender.sendMessage(serializer.deserialize(
                manager.getMessage("set_success",
                    "player" to target.name,
                    "duration" to durationText
                )
            ))
            // 通知目标玩家
            target.sendMessage(serializer.deserialize(
                manager.getMessage("become_neko", "duration" to durationText)
            ))
        } else {
            sender.sendMessage(serializer.deserialize(
                manager.getMessage("set_failed")
            ))
        }
    }

    /**
     * 处理取消猫娘命令
     * /tsl neko reset <player>
     */
    private fun handleReset(sender: CommandSender, args: Array<out String>) {
        // 权限检查
        if (!sender.hasPermission("tsl.neko.reset")) {
            sender.sendMessage(serializer.deserialize(manager.getMessage("no_permission")))
            return
        }

        if (args.isEmpty()) {
            sender.sendMessage(serializer.deserialize(
                "&c用法: /tsl neko reset <玩家>"
            ))
            return
        }

        val playerName = args[0]

        // 查找玩家
        val target = Bukkit.getPlayer(playerName)
        if (target == null || !target.isOnline) {
            sender.sendMessage(serializer.deserialize(
                manager.getMessage("player_not_found", "player" to playerName)
            ))
            return
        }

        // 取消猫娘状态
        val success = manager.resetNeko(target)

        if (success) {
            sender.sendMessage(serializer.deserialize(
                manager.getMessage("reset_success", "player" to target.name)
            ))
            // 通知目标玩家
            target.sendMessage(serializer.deserialize(
                manager.getMessage("no_longer_neko")
            ))
        } else {
            sender.sendMessage(serializer.deserialize(
                manager.getMessage("not_neko", "player" to target.name)
            ))
        }
    }

    /**
     * 处理列出所有猫娘命令
     * /tsl neko list
     */
    private fun handleList(sender: CommandSender) {
        // 权限检查
        if (!sender.hasPermission("tsl.neko.list")) {
            sender.sendMessage(serializer.deserialize(manager.getMessage("no_permission")))
            return
        }

        val nekos = manager.getAllNekos()

        if (nekos.isEmpty()) {
            sender.sendMessage(serializer.deserialize(
                manager.getMessage("list_empty")
            ))
            return
        }

        sender.sendMessage(serializer.deserialize(
            "&d&l===== 猫娘列表 (${nekos.size}) ====="
        ))

        nekos.forEach { effect ->
            sender.sendMessage(serializer.deserialize(
                "&7- &f${effect.playerName} &7| 剩余: &b${effect.formatRemainingTime()}"
            ))
        }
    }

    /**
     * 处理检查玩家是否是猫娘
     * /tsl neko check <player>
     */
    private fun handleCheck(sender: CommandSender, args: Array<out String>) {
        if (args.isEmpty()) {
            sender.sendMessage(serializer.deserialize(
                "&c用法: /tsl neko check <玩家>"
            ))
            return
        }

        val playerName = args[0]
        val target = Bukkit.getPlayer(playerName)
        if (target == null || !target.isOnline) {
            sender.sendMessage(serializer.deserialize(
                manager.getMessage("player_not_found", "player" to playerName)
            ))
            return
        }

        val effect = manager.getNekoEffect(target.uniqueId)
        if (effect != null) {
            sender.sendMessage(serializer.deserialize(
                "&a${target.name} &7是猫娘，剩余时间: &b${effect.formatRemainingTime()}"
            ))
        } else {
            sender.sendMessage(serializer.deserialize(
                "&e${target.name} &7不是猫娘"
            ))
        }
    }

    /**
     * 显示用法
     */
    private fun showUsage(sender: CommandSender) {
        sender.sendMessage(serializer.deserialize("&d&l===== 猫娘模式命令 ====="))
        sender.sendMessage(serializer.deserialize("&e/tsl neko set <玩家> <时间> &7- 设置玩家为猫娘"))
        sender.sendMessage(serializer.deserialize("&e/tsl neko reset <玩家> &7- 取消猫娘状态"))
        sender.sendMessage(serializer.deserialize("&e/tsl neko list &7- 列出所有猫娘"))
        sender.sendMessage(serializer.deserialize("&e/tsl neko check <玩家> &7- 检查玩家状态"))
        sender.sendMessage(serializer.deserialize(""))
        sender.sendMessage(serializer.deserialize("&7时间格式: &f10s&7(秒), &f5m&7(分), &f2h&7(时), &f1d&7(天)"))
    }

    /**
     * 格式化持续时间
     */
    private fun formatDuration(ms: Long): String {
        val seconds = (ms / 1000).toInt()
        return when {
            seconds < 60 -> "${seconds}秒"
            seconds < 3600 -> "${seconds / 60}分${seconds % 60}秒"
            seconds < 86400 -> "${seconds / 3600}小时${(seconds % 3600) / 60}分"
            else -> "${seconds / 86400}天${(seconds % 86400) / 3600}小时"
        }
    }

    override fun tabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): List<String> {
        if (!manager.isEnabled()) return emptyList()

        return when (args.size) {
            1 -> {
                // 子命令
                listOf("set", "reset", "list", "check")
                    .filter { it.startsWith(args[0], ignoreCase = true) }
            }
            2 -> {
                when (args[0].lowercase()) {
                    "set", "reset", "check" -> {
                        // 玩家名
                        Bukkit.getOnlinePlayers()
                            .map { it.name }
                            .filter { it.startsWith(args[1], ignoreCase = true) }
                    }
                    else -> emptyList()
                }
            }
            3 -> {
                when (args[0].lowercase()) {
                    "set" -> {
                        // 时间建议
                        listOf("30s", "1m", "5m", "10m", "30m", "1h", "1d")
                            .filter { it.startsWith(args[2], ignoreCase = true) }
                    }
                    else -> emptyList()
                }
            }
            else -> emptyList()
        }
    }

    override fun getDescription(): String {
        return "猫娘模式管理"
    }
}

