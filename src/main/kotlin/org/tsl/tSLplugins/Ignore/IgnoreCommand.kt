package org.tsl.tSLplugins.Ignore

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.tsl.tSLplugins.PlayerDataManager
import org.tsl.tSLplugins.SubCommandHandler

/**
 * 聊天屏蔽命令处理器
 *
 * 命令：
 * - /tsl ignore <player> - 屏蔽/取消屏蔽玩家
 * - /tsl ignore list - 查看屏蔽列表
 */
class IgnoreCommand(
    private val manager: IgnoreManager,
    private val playerDataManager: PlayerDataManager
) : SubCommandHandler {

    override fun handle(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§c该命令只能由玩家执行")
            return true
        }

        if (!manager.isEnabled()) {
            sender.sendMessage("§c聊天屏蔽功能未启用")
            return true
        }

        if (args.isEmpty()) {
            showUsage(sender)
            return true
        }

        when (args[0].lowercase()) {
            "list" -> handleList(sender)
            else -> handleToggle(sender, args[0])
        }

        return true
    }

    override fun tabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): List<String> {
        if (args.size == 1) {
            val input = args[0].lowercase()
            val suggestions = mutableListOf("list")

            // 添加在线玩家名（排除自己）
            Bukkit.getOnlinePlayers()
                .filter { it.name != (sender as? Player)?.name }
                .map { it.name }
                .filter { it.lowercase().startsWith(input) }
                .forEach { suggestions.add(it) }

            return suggestions.filter { it.lowercase().startsWith(input) }
        }
        return emptyList()
    }

    override fun getDescription(): String {
        return "管理聊天屏蔽列表"
    }

    /**
     * 显示用法
     */
    private fun showUsage(player: Player) {
        player.sendMessage("§e用法:")
        player.sendMessage("§7  /tsl ignore <玩家名> §8- §f屏蔽/取消屏蔽玩家")
        player.sendMessage("§7  /tsl ignore list §8- §f查看屏蔽列表")
    }

    /**
     * 处理屏蔽切换
     */
    private fun handleToggle(player: Player, targetName: String) {
        // 查找目标玩家（在线）
        val target = Bukkit.getPlayer(targetName)

        if (target == null) {
            player.sendMessage("§c玩家 $targetName 不在线")
            return
        }

        if (target.uniqueId == player.uniqueId) {
            player.sendMessage("§c你不能屏蔽自己")
            return
        }

        val (success, isNowIgnoring) = manager.toggleIgnore(player.uniqueId, target.uniqueId)

        if (!success) {
            if (isNowIgnoring) {
                // 添加失败，可能是已达上限
                player.sendMessage("§c屏蔽失败，已达最大屏蔽数量 (${manager.getMaxIgnoreCount()})")
            } else {
                player.sendMessage("§c操作失败")
            }
            return
        }

        if (isNowIgnoring) {
            player.sendMessage("§a已屏蔽玩家 §f${target.name}§a，你将不再收到他的聊天消息")
        } else {
            player.sendMessage("§a已取消屏蔽玩家 §f${target.name}")
        }

        // 更新玩家数据（会在退出时自动保存）
        playerDataManager.setIgnoreList(player, manager.getIgnoreList(player.uniqueId))
    }

    /**
     * 显示屏蔽列表
     */
    private fun handleList(player: Player) {
        val ignoreList = manager.getIgnoreList(player.uniqueId)

        if (ignoreList.isEmpty()) {
            player.sendMessage("§7你的屏蔽列表为空")
            return
        }

        player.sendMessage("§6========== 屏蔽列表 (${ignoreList.size}/${manager.getMaxIgnoreCount()}) ==========")

        ignoreList.forEach { uuid ->
            val targetPlayer = Bukkit.getPlayer(uuid)
            val name = targetPlayer?.name ?: Bukkit.getOfflinePlayer(uuid).name ?: uuid.toString()
            val status = if (targetPlayer != null) "§a在线" else "§7离线"
            player.sendMessage("§7  - §f$name $status")
        }

        player.sendMessage("§6=======================================")
    }
}

