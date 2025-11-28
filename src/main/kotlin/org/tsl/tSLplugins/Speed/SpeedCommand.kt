package org.tsl.tSLplugins.Speed

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.tsl.tSLplugins.SubCommandHandler

/**
 * 速度控制命令处理器
 * /tsl speed <walk|fly> <value>
 */
class SpeedCommand(
    private val speedManager: SpeedManager
) : SubCommandHandler {

    override fun handle(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        // 必须是玩家
        if (sender !is Player) {
            sender.sendMessage("§c该命令只能由玩家执行！")
            return true
        }

        // 检查参数
        if (args.isEmpty()) {
            sendHelp(sender)
            return true
        }

        // 【新功能】智能检测：如果第一个参数是数字，根据玩家当前状态自动选择类型
        val firstArg = args[0]
        val valueFromFirst = firstArg.toDoubleOrNull()

        if (valueFromFirst != null) {
            // 第一个参数是数字，智能检测玩家状态
            return handleSmartSpeed(sender, valueFromFirst)
        }

        val type = args[0].lowercase()

        // 如果只有类型，显示当前速度
        if (args.size == 1) {
            when (type) {
                "walk" -> {
                    if (!sender.hasPermission("tsl.speed.walk")) {
                        sender.sendMessage("§c你没有权限查看行走速度！")
                        return true
                    }
                    val current = speedManager.getWalkSpeedMultiplier(sender)
                    sender.sendMessage("§a当前行走速度倍率: §e${String.format("%.1f", current)}")
                }
                "fly" -> {
                    if (!sender.hasPermission("tsl.speed.fly")) {
                        sender.sendMessage("§c你没有权限查看飞行速度！")
                        return true
                    }
                    val current = speedManager.getFlySpeedMultiplier(sender)
                    sender.sendMessage("§a当前飞行速度倍率: §e${String.format("%.1f", current)}")
                }
                else -> {
                    sendHelp(sender)
                }
            }
            return true
        }

        // 解析速度值
        val value = args[1].toDoubleOrNull()
        if (value == null) {
            sender.sendMessage("§c无效的速度值！请输入 0.1 到 10.0 之间的数字。")
            return true
        }

        // 检查范围
        if (value < SpeedManager.MIN_MULTIPLIER || value > SpeedManager.MAX_MULTIPLIER) {
            sender.sendMessage("§c速度值超出范围！请输入 0.1 到 10.0 之间的数字。")
            return true
        }

        // 设置速度
        when (type) {
            "walk" -> {
                // 检查权限
                if (!sender.hasPermission("tsl.speed.walk")) {
                    sender.sendMessage("§c你没有权限设置行走速度！")
                    return true
                }

                if (speedManager.setWalkSpeed(sender, value)) {
                    sender.sendMessage("§a已将你的行走速度设置为 §e${String.format("%.1f", value)} §a倍")
                } else {
                    sender.sendMessage("§c设置行走速度失败！")
                }
            }

            "fly" -> {
                // 检查权限
                if (!sender.hasPermission("tsl.speed.fly")) {
                    sender.sendMessage("§c你没有权限设置飞行速度！")
                    return true
                }

                if (speedManager.setFlySpeed(sender, value)) {
                    sender.sendMessage("§a已将你的飞行速度设置为 §e${String.format("%.1f", value)} §a倍")
                } else {
                    sender.sendMessage("§c设置飞行速度失败！")
                }
            }

            else -> {
                sendHelp(sender)
            }
        }

        return true
    }

    override fun getDescription(): String {
        return "设置移动速度或飞行速度"
    }

    /**
     * 智能速度设置：根据玩家当前状态自动选择类型
     */
    private fun handleSmartSpeed(player: Player, value: Double): Boolean {
        // 检查范围
        if (value < SpeedManager.MIN_MULTIPLIER || value > SpeedManager.MAX_MULTIPLIER) {
            player.sendMessage("§c速度值超出范围！请输入 0.1 到 10.0 之间的数字。")
            return true
        }

        // 根据玩家当前飞行状态选择类型
        if (player.isFlying) {
            // 玩家正在飞行，设置飞行速度
            if (!player.hasPermission("tsl.speed.fly")) {
                player.sendMessage("§c你没有权限设置飞行速度！")
                return true
            }

            if (speedManager.setFlySpeed(player, value)) {
                player.sendMessage("§a已将你的飞行速度设置为 §e${String.format("%.1f", value)} §a倍 §7(智能检测)")
            } else {
                player.sendMessage("§c设置飞行速度失败！")
            }
        } else {
            // 玩家在地面，设置行走速度
            if (!player.hasPermission("tsl.speed.walk")) {
                player.sendMessage("§c你没有权限设置行走速度！")
                return true
            }

            if (speedManager.setWalkSpeed(player, value)) {
                player.sendMessage("§a已将你的行走速度设置为 §e${String.format("%.1f", value)} §a倍 §7(智能检测)")
            } else {
                player.sendMessage("§c设置行走速度失败！")
            }
        }

        return true
    }

    override fun tabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): List<String> {
        return when (args.size) {
            1 -> {
                // 第一级：类型选项 + 常用速度值
                val options = mutableListOf<String>()

                // 添加类型选项
                if ("walk".startsWith(args[0].lowercase())) {
                    options.add("walk")
                }
                if ("fly".startsWith(args[0].lowercase())) {
                    options.add("fly")
                }

                // 添加常用速度值（智能检测功能）
                val speedValues = listOf("0.5", "1.0", "1.5", "2.0", "2.5", "3.0", "5.0", "10.0")
                options.addAll(speedValues.filter { it.startsWith(args[0]) })

                options.sorted()
            }
            2 -> {
                // 第二级：速度值补全
                if (args[0].lowercase() in listOf("walk", "fly")) {
                    val speedValues = listOf("0.5", "1.0", "1.5", "2.0", "2.5", "3.0", "5.0", "10.0")
                    speedValues.filter { it.startsWith(args[1]) }
                } else {
                    emptyList()
                }
            }
            else -> emptyList()
        }
    }

    private fun sendHelp(sender: CommandSender) {
        sender.sendMessage("§6§l=== 速度控制 ===")
        sender.sendMessage("§e/tsl speed <0.1-10.0> §7- 智能设置速度（根据飞行状态自动选择）")
        sender.sendMessage("§e/tsl speed walk <0.1-10.0> §7- 设置行走速度")
        sender.sendMessage("§e/tsl speed fly <0.1-10.0> §7- 设置飞行速度")
        sender.sendMessage("§e/tsl speed walk §7- 查看当前行走速度")
        sender.sendMessage("§e/tsl speed fly §7- 查看当前飞行速度")
        sender.sendMessage("§7默认速度倍率为 1.0")
    }
}

