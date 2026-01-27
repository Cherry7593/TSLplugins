package org.tsl.tSLplugins.modules.hat

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.tsl.tSLplugins.SubCommandHandler

/**
 * Hat 命令处理器（新架构版本）
 * 
 * 处理 /tsl hat 命令
 */
class HatModuleCommand(
    private val module: HatModule
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
            sender.sendMessage(serializer.deserialize(module.getModuleMessage("console-only")))
            return true
        }
        
        // 检查权限
        if (!sender.hasPermission("tsl.hat.use")) {
            sender.sendMessage(serializer.deserialize(module.getModuleMessage("no-permission")))
            return true
        }
        
        // 获取手持物品
        val itemInHand = sender.inventory.itemInMainHand
        
        // 检查是否为空气
        if (itemInHand.type == Material.AIR) {
            sender.sendMessage(serializer.deserialize(module.getModuleMessage("no-item")))
            return true
        }
        
        // 检查物品是否在黑名单中
        if (module.isBlacklisted(itemInHand.type)) {
            sender.sendMessage(serializer.deserialize(module.getModuleMessage("blacklisted")))
            return true
        }
        
        // 检查冷却时间
        val remaining = module.checkCooldown(sender.uniqueId)
        if (remaining != null) {
            val formatted = String.format("%.1f", remaining)
            sender.sendMessage(serializer.deserialize(
                module.getModuleMessage("cooldown", "cooldown" to formatted)
            ))
            return true
        }
        
        // 使用 Folia 实体调度器执行物品交换
        sender.scheduler.run(module.getPlugin(), { _ ->
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
        module.setCooldown(sender.uniqueId)
        
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
    
    override fun getDescription(): String = "将手中物品戴在头上"
}
