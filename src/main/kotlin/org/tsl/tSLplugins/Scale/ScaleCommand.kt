package org.tsl.tSLplugins.Scale

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.tsl.tSLplugins.SubCommandHandler

/**
 * Scale 命令处理器
 * 处理 /tsl scale 相关的子命令
 */
class ScaleCommand(
    private val plugin: JavaPlugin,
    private val scaleManager: ScaleManager
) : SubCommandHandler {

    override fun handle(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        // 检查功能是否启用
        if (!scaleManager.isEnabled()) {
            sender.sendMessage(translateColorCode("&c[TSL喵]&r 体型调整功能已禁用"))
            return true
        }

        // 需要是玩家
        if (sender !is Player) {
            sender.sendMessage(translateColorCode(scaleManager.getMessage("console-only")))
            return true
        }

        // 权限检查
        if (!sender.hasPermission("tsl.scale.use")) {
            sender.sendMessage(translateColorCode(scaleManager.getMessage("no-permission")))
            return true
        }

        if (args.isEmpty()) {
            sender.sendMessage(translateColorCode(scaleManager.getMessage("usage")))
            return true
        }

        val arg = args[0].lowercase()

        return when {
            arg == "reset" -> handleReset(sender)
            arg.toDoubleOrNull() != null -> handleSetScale(sender, arg.toDouble())
            else -> {
                sender.sendMessage(translateColorCode(scaleManager.getMessage("usage")))
                true
            }
        }
    }

    override fun tabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): List<String> {
        if (args.size != 1) return emptyList()

        val token = args[0].lowercase()
        val options = mutableListOf<String>()

        // 添加 reset 选项
        options.add("reset")


        // 如果有 bypass 权限，提供更大范围的选项
        if (sender.hasPermission("tsl.scale.bypass")) {
            // 提供 0.1 到 2.0 的范围
            var v = 0.1
            while (v <= 2.0 + 0.0001) {
                options.add(String.format("%.1f", v))
                v += 0.1
            }
        } else {
            // 普通玩家只能看到配置的范围
            options.addAll(scaleManager.getValidScales())
        }

        // 过滤匹配的选项
        return options.filter { it.lowercase().startsWith(token) }
    }

    override fun getDescription(): String = "调整玩家体型"

    private fun handleReset(player: Player): Boolean {
        scaleManager.resetPlayerScale(player)
        player.sendMessage(translateColorCode(scaleManager.getMessage("reset")))
        return true
    }

    private fun handleSetScale(player: Player, scale: Double): Boolean {
        // 检查是否有 bypass 权限，如果有则忽略范围限制
        val hasBypass = player.hasPermission("tsl.scale.bypass")

        if (!hasBypass && !scaleManager.isValidScale(scale)) {
            val message = scaleManager.getMessage("usage")
                .replace("%min%", scaleManager.getScaleMin().toString())
                .replace("%max%", scaleManager.getScaleMax().toString())
            player.sendMessage(translateColorCode(message))
            return true
        }

        scaleManager.setPlayerScale(player, scale)
        val message = scaleManager.getMessage("set").replace("%scale%", String.format("%.1f", scale))
        player.sendMessage(translateColorCode(message))
        return true
    }

    /**
     * 将 & 符号转换为 § 符号用于着色
     */
    private fun translateColorCode(text: String): String {
        return text.replace("&", "§")
    }
}

