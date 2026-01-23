package org.tsl.tSLplugins.DeathPenalty

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.plugin.java.JavaPlugin
import org.tsl.tSLplugins.MessageManager

/**
 * 死亡金币惩罚事件监听器
 * 监听玩家死亡事件并执行扣费逻辑
 */
class DeathPenaltyListener(
    private val plugin: JavaPlugin,
    private val manager: DeathPenaltyManager,
    private val messageManager: MessageManager
) : Listener {

    /**
     * 监听玩家死亡事件
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerDeath(event: PlayerDeathEvent) {
        if (!manager.isEnabled()) return

        val player = event.entity

        // 异步处理扣费逻辑，避免阻塞主线程
        Bukkit.getAsyncScheduler().runNow(plugin) { _ ->
            processDeathPenalty(player)
        }
    }

    /**
     * 处理死亡惩罚逻辑（在异步线程中执行）
     */
    private fun processDeathPenalty(player: Player) {
        val penaltyAmount = manager.getPenaltyAmount()

        // 获取玩家余额
        val balance = manager.getBalance(player)
        if (balance == null) {
            // 玩家数据未初始化，跳过
            plugin.logger.fine("[DeathPenalty] 玩家 ${player.name} 余额数据未初始化，跳过扣费")
            return
        }

        // 检查余额是否充足
        if (balance < penaltyAmount) {
            // 余额不足，发送提示
            sendMessageToPlayer(player, "death-penalty.insufficient-balance")
            plugin.logger.fine("[DeathPenalty] 玩家 ${player.name} 余额不足 (${balance} < ${penaltyAmount})，免除惩罚")
            return
        }

        // 执行扣费
        val success = manager.withdraw(player, penaltyAmount)
        if (success) {
            // 扣费成功，发送提示
            sendMessageToPlayer(player, "death-penalty.deducted", mapOf("amount" to penaltyAmount.toString()))
            plugin.logger.fine("[DeathPenalty] 玩家 ${player.name} 死亡扣费 $penaltyAmount 成功")
        } else {
            plugin.logger.warning("[DeathPenalty] 玩家 ${player.name} 扣费失败")
        }
    }

    /**
     * 向玩家发送消息（切换回玩家线程）
     */
    private fun sendMessageToPlayer(player: Player, messageKey: String, placeholders: Map<String, String> = emptyMap()) {
        if (!player.isOnline) return

        player.scheduler.run(plugin, { _ ->
            val replacements = placeholders.map { it.key to it.value }.toTypedArray()
            val message = messageManager.get(messageKey, *replacements)
            player.sendMessage(message)
        }, null)
    }
}
