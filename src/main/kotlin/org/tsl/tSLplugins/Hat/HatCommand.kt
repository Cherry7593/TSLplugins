package org.tsl.tSLplugins.Hat

import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.tsl.tSLplugins.SubCommandHandler
import org.tsl.tSLplugins.TSLplugins
import java.util.*

/**
 * Hat 命令处理器
 * 处理 /tsl hat 命令
 */
class HatCommand(
    private val plugin: TSLplugins,
    private val manager: HatManager
) : SubCommandHandler {

    private val cooldowns: MutableMap<UUID, Long> = mutableMapOf()

    override fun handle(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        // 检查功能是否启用
        if (!manager.isEnabled()) {
            sender.sendMessage(manager.getMessage("disabled"))
            return true
        }

        // 必须是玩家
        if (sender !is Player) {
            sender.sendMessage(manager.getMessage("console-only"))
            return true
        }

        // 检查权限
        if (!sender.hasPermission("tsl.hat.use")) {
            sender.sendMessage(manager.getMessage("no-permission"))
            return true
        }

        // 获取手持物品
        val itemInHand = sender.inventory.itemInMainHand

        // 检查是否为空气
        if (itemInHand.type == Material.AIR) {
            sender.sendMessage(manager.getMessage("no-item"))
            return true
        }

        // 检查物品是否在黑名单中
        if (manager.isBlacklisted(itemInHand.type)) {
            sender.sendMessage(manager.getMessage("blacklisted"))
            return true
        }

        // 检查冷却时间
        val cooldownTime = manager.getCooldown()
        if (cooldownTime > 0) {
            val currentTime = System.currentTimeMillis()
            val lastUsed = cooldowns.getOrDefault(sender.uniqueId, 0L)

            if (currentTime - lastUsed < cooldownTime) {
                val remaining = ((cooldownTime - (currentTime - lastUsed)) / 1000.0).coerceAtLeast(0.1)
                val formatted = String.format("%.1f", remaining)
                sender.sendMessage(manager.getMessage("cooldown", mapOf("%cooldown%" to formatted)))
                return true
            }
        }

        // 使用 Folia 实体调度器执行物品交换
        sender.scheduler.run(plugin, { _ ->
            val helmet = sender.inventory.helmet
            val itemInHandCurrent = sender.inventory.itemInMainHand

            // 如果手持物品数量大于1，只戴1个
            val itemToEquip = itemInHandCurrent.clone()
            itemToEquip.amount = 1

            // 将1个物品戴到头上
            sender.inventory.setHelmet(itemToEquip)

            // 处理手中剩余的物品
            if (itemInHandCurrent.amount > 1) {
                // 手中还有剩余
                val remainingItem = itemInHandCurrent.clone()
                remainingItem.amount = itemInHandCurrent.amount - 1
                sender.inventory.setItemInMainHand(remainingItem)

                // 如果原来有头盔，尝试放回背包
                if (helmet != null && helmet.type != Material.AIR) {
                    val leftover = sender.inventory.addItem(helmet)
                    if (leftover.isNotEmpty()) {
                        // 背包满了，掉落到地上
                        leftover.values.forEach { item ->
                            sender.world.dropItemNaturally(sender.location, item)
                        }
                    }
                }
            } else {
                // 手中只有1个
                if (helmet != null && helmet.type != Material.AIR) {
                    // 有旧头盔，尝试放入主手
                    sender.inventory.setItemInMainHand(helmet)
                } else {
                    // 没有旧头盔，清空主手
                    sender.inventory.setItemInMainHand(null)
                }
            }
        }, null)

        // 更新冷却时间
        if (cooldownTime > 0) {
            cooldowns[sender.uniqueId] = System.currentTimeMillis()
        }

        return true
    }

    override fun tabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): List<String> {
        // Hat 命令没有参数
        return emptyList()
    }

    override fun getDescription(): String {
        return "将手中物品戴在头上"
    }
}

