package org.tsl.tSLplugins.modules.kiss

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.tsl.tSLplugins.SubCommandHandler

/**
 * Kiss 命令处理器（新架构版本）
 * 
 * 处理 /tsl kiss 相关命令
 */
class KissModuleCommand(
    private val module: KissModule
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
            args.isEmpty() -> showUsage(sender)
            args[0].equals("toggle", ignoreCase = true) -> handleToggle(sender)
            else -> handleKiss(sender, args[0])
        }
        
        return true
    }
    
    private fun handleToggle(player: Player) {
        val newStatus = module.togglePlayer(player)
        
        val message = if (newStatus) {
            module.getModuleMessage("toggle_enabled")
        } else {
            module.getModuleMessage("toggle_disabled")
        }
        
        player.sendMessage(serializer.deserialize(message))
    }
    
    private fun handleKiss(sender: Player, targetName: String) {
        // 检查发起者是否启用了功能（静默）
        if (!module.isPlayerEnabled(sender)) return
        
        // 冷却检查（静默）
        if (module.isInCooldown(sender.uniqueId) && !sender.hasPermission("tsl.kiss.bypass")) {
            return
        }
        
        // 查找目标玩家
        val target = Bukkit.getPlayer(targetName)
        if (target == null || !target.isOnline) {
            sender.sendMessage(serializer.deserialize(
                module.getModuleMessage("player_not_found", "player" to targetName)
            ))
            return
        }
        
        // 不能亲自己
        if (sender.uniqueId == target.uniqueId) {
            sender.sendMessage(serializer.deserialize(module.getModuleMessage("cannot_kiss_self")))
            return
        }
        
        // 执行亲吻
        module.executeKiss(sender, target)
        
        // 设置冷却
        if (!sender.hasPermission("tsl.kiss.bypass")) {
            module.setCooldown(sender.uniqueId)
        }
    }
    
    private fun showUsage(sender: CommandSender) {
        sender.sendMessage(serializer.deserialize(module.getModuleMessage("usage")))
    }
    
    override fun tabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): List<String> {
        if (!module.isEnabled() || sender !is Player) return emptyList()
        
        return when (args.size) {
            1 -> {
                val suggestions = mutableListOf("toggle")
                suggestions.addAll(
                    Bukkit.getOnlinePlayers()
                        .filter { it.uniqueId != sender.uniqueId }
                        .map { it.name }
                )
                suggestions.filter { it.startsWith(args[0], ignoreCase = true) }
            }
            else -> emptyList()
        }
    }
    
    override fun getDescription(): String = "亲吻玩家功能"
}
