package org.tsl.tSLplugins.modules.freeze

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.tsl.tSLplugins.SubCommandHandler

/**
 * Freeze 命令处理器（新架构版本）
 * 
 * 处理 /tsl freeze 相关命令
 */
class FreezeModuleCommand(
    private val module: FreezeModule
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
        
        // 权限检查
        if (!sender.hasPermission("tsl.freeze.use")) {
            sender.sendMessage(serializer.deserialize(module.getModuleMessage("no_permission")))
            return true
        }
        
        when {
            args.isEmpty() -> {
                showUsage(sender)
            }
            args[0].equals("list", ignoreCase = true) -> {
                handleList(sender)
            }
            else -> {
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
        val target = Bukkit.getPlayer(targetName)
        if (target == null || !target.isOnline) {
            sender.sendMessage(serializer.deserialize(
                module.getModuleMessage("player_not_found", "player" to targetName)
            ))
            return
        }
        
        // 检查目标是否有 bypass 权限
        if (target.hasPermission("tsl.freeze.bypass")) {
            sender.sendMessage(serializer.deserialize(
                module.getModuleMessage("target_bypass", "player" to target.name)
            ))
            return
        }
        
        // 检查当前是否已被冻结
        if (module.isFrozen(target.uniqueId)) {
            // 已冻结，执行解冻
            module.unfreezePlayer(target.uniqueId)
            
            sender.sendMessage(serializer.deserialize(
                module.getModuleMessage("unfreeze_success", "player" to target.name)
            ))
            target.sendMessage(serializer.deserialize(module.getModuleMessage("unfrozen")))
        } else {
            // 未冻结，执行冻结
            module.freezePlayer(target.uniqueId, duration)
            
            val durationText = if (duration > 0) formatTime(duration) else "永久"
            sender.sendMessage(serializer.deserialize(
                module.getModuleMessage("freeze_success",
                    "player" to target.name,
                    "duration" to durationText)
            ))
            target.sendMessage(serializer.deserialize(
                module.getModuleMessage("frozen", "duration" to durationText)
            ))
        }
    }
    
    /**
     * 处理列出被冻结的玩家
     */
    private fun handleList(sender: CommandSender) {
        val frozenPlayers = module.getFrozenPlayers()
        
        if (frozenPlayers.isEmpty()) {
            sender.sendMessage(serializer.deserialize(module.getModuleMessage("no_frozen_players")))
            return
        }
        
        sender.sendMessage(serializer.deserialize(module.getModuleMessage("list_header")))
        
        frozenPlayers.forEach { (uuid, _) ->
            val player = Bukkit.getOfflinePlayer(uuid)
            val name = player.name ?: uuid.toString()
            val remaining = module.getRemainingTime(uuid)
            val timeText = if (remaining < 0) "永久" else formatTime(remaining)
            val status = if (player.isOnline) "在线" else "离线"
            
            sender.sendMessage(serializer.deserialize(
                module.getModuleMessage("list_entry",
                    "player" to name,
                    "time" to timeText,
                    "status" to status)
            ))
        }
    }
    
    private fun showUsage(sender: CommandSender) {
        sender.sendMessage(serializer.deserialize(module.getModuleMessage("usage")))
    }
    
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
        if (!module.isEnabled() || !sender.hasPermission("tsl.freeze.use")) {
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
