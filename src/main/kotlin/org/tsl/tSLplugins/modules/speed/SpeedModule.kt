package org.tsl.tSLplugins.modules.speed

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.tsl.tSLplugins.SubCommandHandler
import org.tsl.tSLplugins.core.AbstractModule

/**
 * Speed 模块 - 速度控制
 * 
 * 管理玩家的行走速度和飞行速度
 * 
 * ## 命令
 * - `/tsl speed <数值>` - 智能设置速度（根据飞行状态）
 * - `/tsl speed walk <数值>` - 设置行走速度
 * - `/tsl speed fly <数值>` - 设置飞行速度
 * 
 * ## 权限
 * - `tsl.speed.walk` - 设置行走速度
 * - `tsl.speed.fly` - 设置飞行速度
 */
class SpeedModule : AbstractModule() {

    override val id = "speed"
    override val configPath = "speed"

    companion object {
        const val DEFAULT_WALK_SPEED = 0.2f
        const val DEFAULT_FLY_SPEED = 0.1f
        const val MIN_MULTIPLIER = 0.1
        const val MAX_MULTIPLIER = 10.0
    }

    override fun doEnable() {
        // 无需特殊初始化
    }

    override fun doDisable() {
        // 无需清理
    }

    override fun getCommandHandler(): SubCommandHandler = SpeedModuleCommand(this)
    
    override fun getDescription(): String = "速度控制"

    // ============== 公开 API ==============

    fun setWalkSpeed(player: Player, multiplier: Double): Boolean {
        if (multiplier < MIN_MULTIPLIER || multiplier > MAX_MULTIPLIER) return false
        val speed = (DEFAULT_WALK_SPEED * multiplier).toFloat().coerceIn(-1.0f, 1.0f)
        player.walkSpeed = speed
        return true
    }

    fun setFlySpeed(player: Player, multiplier: Double): Boolean {
        if (multiplier < MIN_MULTIPLIER || multiplier > MAX_MULTIPLIER) return false
        val speed = (DEFAULT_FLY_SPEED * multiplier).toFloat().coerceIn(-1.0f, 1.0f)
        player.flySpeed = speed
        return true
    }

    fun resetWalkSpeed(player: Player) { player.walkSpeed = DEFAULT_WALK_SPEED }
    fun resetFlySpeed(player: Player) { player.flySpeed = DEFAULT_FLY_SPEED }
    fun getWalkSpeedMultiplier(player: Player): Double = (player.walkSpeed / DEFAULT_WALK_SPEED).toDouble()
    fun getFlySpeedMultiplier(player: Player): Double = (player.flySpeed / DEFAULT_FLY_SPEED).toDouble()
}

class SpeedModuleCommand(private val module: SpeedModule) : SubCommandHandler {

    override fun handle(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§c该命令只能由玩家执行！")
            return true
        }

        if (args.isEmpty()) {
            sendHelp(sender)
            return true
        }

        // 智能检测：如果第一个参数是数字
        val valueFromFirst = args[0].toDoubleOrNull()
        if (valueFromFirst != null) {
            return handleSmartSpeed(sender, valueFromFirst)
        }

        val type = args[0].lowercase()
        if (args.size == 1) {
            when (type) {
                "walk" -> {
                    if (!sender.hasPermission("tsl.speed.walk")) {
                        sender.sendMessage("§c你没有权限查看行走速度！")
                        return true
                    }
                    val current = module.getWalkSpeedMultiplier(sender)
                    sender.sendMessage("§a当前行走速度倍率: §e${String.format("%.1f", current)}")
                }
                "fly" -> {
                    if (!sender.hasPermission("tsl.speed.fly")) {
                        sender.sendMessage("§c你没有权限查看飞行速度！")
                        return true
                    }
                    val current = module.getFlySpeedMultiplier(sender)
                    sender.sendMessage("§a当前飞行速度倍率: §e${String.format("%.1f", current)}")
                }
                else -> sendHelp(sender)
            }
            return true
        }

        val value = args[1].toDoubleOrNull()
        if (value == null || value < SpeedModule.MIN_MULTIPLIER || value > SpeedModule.MAX_MULTIPLIER) {
            sender.sendMessage("§c无效或超出范围！请输入 0.1 到 10.0 之间的数字。")
            return true
        }

        when (type) {
            "walk" -> {
                if (!sender.hasPermission("tsl.speed.walk")) {
                    sender.sendMessage("§c你没有权限设置行走速度！")
                    return true
                }
                if (module.setWalkSpeed(sender, value)) {
                    sender.sendMessage("§a已将你的行走速度设置为 §e${String.format("%.1f", value)} §a倍")
                }
            }
            "fly" -> {
                if (!sender.hasPermission("tsl.speed.fly")) {
                    sender.sendMessage("§c你没有权限设置飞行速度！")
                    return true
                }
                if (module.setFlySpeed(sender, value)) {
                    sender.sendMessage("§a已将你的飞行速度设置为 §e${String.format("%.1f", value)} §a倍")
                }
            }
            else -> sendHelp(sender)
        }
        return true
    }

    private fun handleSmartSpeed(player: Player, value: Double): Boolean {
        if (value < SpeedModule.MIN_MULTIPLIER || value > SpeedModule.MAX_MULTIPLIER) {
            player.sendMessage("§c速度值超出范围！请输入 0.1 到 10.0 之间的数字。")
            return true
        }

        if (player.isFlying) {
            if (!player.hasPermission("tsl.speed.fly")) {
                player.sendMessage("§c你没有权限设置飞行速度！")
                return true
            }
            if (module.setFlySpeed(player, value)) {
                player.sendMessage("§a已将你的飞行速度设置为 §e${String.format("%.1f", value)} §a倍 §7(智能检测)")
            }
        } else {
            if (!player.hasPermission("tsl.speed.walk")) {
                player.sendMessage("§c你没有权限设置行走速度！")
                return true
            }
            if (module.setWalkSpeed(player, value)) {
                player.sendMessage("§a已将你的行走速度设置为 §e${String.format("%.1f", value)} §a倍 §7(智能检测)")
            }
        }
        return true
    }

    override fun tabComplete(sender: CommandSender, command: Command, label: String, args: Array<out String>): List<String> {
        return when (args.size) {
            1 -> {
                val options = mutableListOf<String>()
                if ("walk".startsWith(args[0].lowercase())) options.add("walk")
                if ("fly".startsWith(args[0].lowercase())) options.add("fly")
                val speedValues = listOf("0.5", "1.0", "1.5", "2.0", "2.5", "3.0", "5.0", "10.0")
                options.addAll(speedValues.filter { it.startsWith(args[0]) })
                options.sorted()
            }
            2 -> {
                if (args[0].lowercase() in listOf("walk", "fly")) {
                    listOf("0.5", "1.0", "1.5", "2.0", "2.5", "3.0", "5.0", "10.0").filter { it.startsWith(args[1]) }
                } else emptyList()
            }
            else -> emptyList()
        }
    }

    private fun sendHelp(sender: CommandSender) {
        sender.sendMessage("§6§l=== 速度控制 ===")
        sender.sendMessage("§e/tsl speed <0.1-10.0> §7- 智能设置速度")
        sender.sendMessage("§e/tsl speed walk <0.1-10.0> §7- 设置行走速度")
        sender.sendMessage("§e/tsl speed fly <0.1-10.0> §7- 设置飞行速度")
    }

    override fun getDescription(): String = "设置移动速度或飞行速度"
}
