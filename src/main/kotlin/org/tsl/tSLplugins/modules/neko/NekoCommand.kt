package org.tsl.tSLplugins.modules.neko

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.tsl.tSLplugins.SubCommandHandler

class NekoCommand(private val manager: NekoManager) : SubCommandHandler {
    private val serializer = LegacyComponentSerializer.legacyAmpersand()

    override fun handle(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!manager.isEnabled()) {
            sender.sendMessage(serializer.deserialize(manager.getMessage("disabled")))
            return true
        }
        if (args.isEmpty()) { showUsage(sender); return true }
        when (args[0].lowercase()) {
            "set" -> handleSet(sender, args.drop(1).toTypedArray())
            "reset", "remove" -> handleReset(sender, args.drop(1).toTypedArray())
            "list" -> handleList(sender)
            "check" -> handleCheck(sender, args.drop(1).toTypedArray())
            else -> showUsage(sender)
        }
        return true
    }

    private fun handleSet(sender: CommandSender, args: Array<out String>) {
        if (!sender.hasPermission("tsl.neko.set")) { sender.sendMessage(serializer.deserialize(manager.getMessage("no_permission"))); return }
        if (args.size < 2) { sender.sendMessage(serializer.deserialize("&c用法: /tsl neko set <玩家> <时间>")); return }
        val target = Bukkit.getPlayer(args[0])
        if (target == null || !target.isOnline) { sender.sendMessage(serializer.deserialize(manager.getMessage("player_not_found", "player" to args[0]))); return }
        val durationMs = manager.parseDuration(args[1])
        if (durationMs == null || durationMs <= 0) { sender.sendMessage(serializer.deserialize(manager.getMessage("invalid_duration", "duration" to args[1]))); return }
        if (manager.setNeko(target, durationMs, "command:${sender.name}")) {
            val durationText = formatDuration(durationMs)
            sender.sendMessage(serializer.deserialize(manager.getMessage("set_success", "player" to target.name, "duration" to durationText)))
            target.sendMessage(serializer.deserialize(manager.getMessage("become_neko", "duration" to durationText)))
        }
    }

    private fun handleReset(sender: CommandSender, args: Array<out String>) {
        if (!sender.hasPermission("tsl.neko.reset")) { sender.sendMessage(serializer.deserialize(manager.getMessage("no_permission"))); return }
        if (args.isEmpty()) { sender.sendMessage(serializer.deserialize("&c用法: /tsl neko reset <玩家>")); return }
        val target = Bukkit.getPlayer(args[0])
        if (target == null || !target.isOnline) { sender.sendMessage(serializer.deserialize(manager.getMessage("player_not_found", "player" to args[0]))); return }
        if (manager.resetNeko(target)) {
            sender.sendMessage(serializer.deserialize(manager.getMessage("reset_success", "player" to target.name)))
            target.sendMessage(serializer.deserialize(manager.getMessage("no_longer_neko")))
        } else {
            sender.sendMessage(serializer.deserialize(manager.getMessage("not_neko", "player" to target.name)))
        }
    }

    private fun handleList(sender: CommandSender) {
        if (!sender.hasPermission("tsl.neko.list")) { sender.sendMessage(serializer.deserialize(manager.getMessage("no_permission"))); return }
        val nekos = manager.getAllNekos()
        if (nekos.isEmpty()) { sender.sendMessage(serializer.deserialize(manager.getMessage("list_empty"))); return }
        sender.sendMessage(serializer.deserialize("&d&l===== 猫娘列表 (${nekos.size}) ====="))
        nekos.forEach { sender.sendMessage(serializer.deserialize("&7- &f${it.playerName} &7| 剩余: &b${it.formatRemainingTime()}")) }
    }

    private fun handleCheck(sender: CommandSender, args: Array<out String>) {
        if (args.isEmpty()) { sender.sendMessage(serializer.deserialize("&c用法: /tsl neko check <玩家>")); return }
        val target = Bukkit.getPlayer(args[0])
        if (target == null || !target.isOnline) { sender.sendMessage(serializer.deserialize(manager.getMessage("player_not_found", "player" to args[0]))); return }
        val effect = manager.getNekoEffect(target.uniqueId)
        if (effect != null) sender.sendMessage(serializer.deserialize("&a${target.name} &7是猫娘，剩余时间: &b${effect.formatRemainingTime()}"))
        else sender.sendMessage(serializer.deserialize("&e${target.name} &7不是猫娘"))
    }

    private fun showUsage(sender: CommandSender) {
        sender.sendMessage(serializer.deserialize("&d&l===== 猫娘模式命令 ====="))
        sender.sendMessage(serializer.deserialize("&e/tsl neko set <玩家> <时间> &7- 设置玩家为猫娘"))
        sender.sendMessage(serializer.deserialize("&e/tsl neko reset <玩家> &7- 取消猫娘状态"))
        sender.sendMessage(serializer.deserialize("&e/tsl neko list &7- 列出所有猫娘"))
        sender.sendMessage(serializer.deserialize("&e/tsl neko check <玩家> &7- 检查玩家状态"))
    }

    private fun formatDuration(ms: Long): String {
        val seconds = (ms / 1000).toInt()
        return when {
            seconds < 60 -> "${seconds}秒"
            seconds < 3600 -> "${seconds / 60}分${seconds % 60}秒"
            seconds < 86400 -> "${seconds / 3600}小时${(seconds % 3600) / 60}分"
            else -> "${seconds / 86400}天${(seconds % 86400) / 3600}小时"
        }
    }

    override fun tabComplete(sender: CommandSender, command: Command, label: String, args: Array<out String>): List<String> {
        if (!manager.isEnabled()) return emptyList()
        return when (args.size) {
            1 -> listOf("set", "reset", "list", "check").filter { it.startsWith(args[0], ignoreCase = true) }
            2 -> if (args[0].lowercase() in listOf("set", "reset", "check")) Bukkit.getOnlinePlayers().map { it.name }.filter { it.startsWith(args[1], ignoreCase = true) } else emptyList()
            3 -> if (args[0].lowercase() == "set") listOf("30s", "1m", "5m", "10m", "30m", "1h", "1d").filter { it.startsWith(args[2], ignoreCase = true) } else emptyList()
            else -> emptyList()
        }
    }

    override fun getDescription(): String = "猫娘模式管理"
}
