package org.tsl.tSLplugins.modules.scale

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.tsl.tSLplugins.SubCommandHandler

/**
 * Scale 命令处理器（新架构版本）
 * 
 * 处理 /tsl scale 相关命令
 */
class ScaleModuleCommand(
    private val module: ScaleModule
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
        
        // 需要是玩家
        if (sender !is Player) {
            sender.sendMessage(serializer.deserialize(module.getModuleMessage("console-only")))
            return true
        }
        
        // 权限检查
        if (!sender.hasPermission("tsl.scale.use")) {
            sender.sendMessage(serializer.deserialize(module.getModuleMessage("no-permission")))
            return true
        }
        
        if (args.isEmpty()) {
            sender.sendMessage(serializer.deserialize(module.getModuleMessage("usage")))
            return true
        }
        
        val arg = args[0].lowercase()
        
        return when {
            arg == "reset" -> handleReset(sender)
            arg.toDoubleOrNull() != null -> handleSetScale(sender, arg.toDouble())
            else -> {
                sender.sendMessage(serializer.deserialize(module.getModuleMessage("usage")))
                true
            }
        }
    }
    
    private fun handleReset(player: Player): Boolean {
        module.resetPlayerScale(player)
        player.sendMessage(serializer.deserialize(module.getModuleMessage("reset")))
        return true
    }
    
    private fun handleSetScale(player: Player, scale: Double): Boolean {
        // 检查是否有 bypass 权限
        val hasBypass = player.hasPermission("tsl.scale.bypass")
        
        if (!hasBypass && !module.isValidScale(scale)) {
            val message = module.getModuleMessage("usage")
                .replace("%min%", module.getScaleMin().toString())
                .replace("%max%", module.getScaleMax().toString())
            player.sendMessage(serializer.deserialize(message))
            return true
        }
        
        module.setPlayerScale(player, scale)
        val message = module.getModuleMessage("set")
            .replace("%scale%", String.format("%.1f", scale))
        player.sendMessage(serializer.deserialize(message))
        return true
    }
    
    override fun tabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): List<String> {
        if (!module.isEnabled()) return emptyList()
        if (args.size != 1) return emptyList()
        
        val token = args[0].lowercase()
        val options = mutableListOf<String>()
        
        // 添加 reset 选项
        options.add("reset")
        
        // 如果有 bypass 权限，提供更大范围的选项
        if (sender.hasPermission("tsl.scale.bypass")) {
            var v = 0.1
            while (v <= 2.0 + 0.0001) {
                options.add(String.format("%.1f", v))
                v += 0.1
            }
        } else {
            options.addAll(module.getValidScales())
        }
        
        return options.filter { it.lowercase().startsWith(token) }
    }
    
    override fun getDescription(): String = "调整玩家体型"
}
