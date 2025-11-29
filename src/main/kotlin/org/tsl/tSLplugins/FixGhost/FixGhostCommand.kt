package org.tsl.tSLplugins.FixGhost

import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.tsl.tSLplugins.SubCommandHandler
import org.tsl.tSLplugins.TSLplugins
import java.util.*

/**
 * FixGhost 命令处理器
 * 处理 /tsl fixghost [radius] 命令
 * 使用 Folia Region Scheduler 确保线程安全
 */
class FixGhostCommand(
    private val plugin: TSLplugins,
    private val manager: FixGhostManager
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
        if (!sender.hasPermission("tsl.fixghost.use")) {
            sender.sendMessage(manager.getMessage("no-permission"))
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

        // 解析半径参数
        val radius = if (args.isNotEmpty()) {
            args[0].toIntOrNull()?.coerceIn(1, manager.getMaxRadius()) ?: manager.getDefaultRadius()
        } else {
            manager.getDefaultRadius()
        }

        // 使用 Folia 玩家调度器执行方块扫描和替换
        sender.scheduler.run(plugin, { _ ->
            val playerLoc = sender.location
            val world = sender.world

            var count = 0

            // 扫描立方体区域
            val startX = playerLoc.blockX - radius
            val startY = playerLoc.blockY - radius
            val startZ = playerLoc.blockZ - radius
            val endX = playerLoc.blockX + radius
            val endY = playerLoc.blockY + radius
            val endZ = playerLoc.blockZ + radius

            // 限制 Y 轴范围（世界高度限制）
            val minY = world.minHeight
            val maxY = world.maxHeight - 1

            for (x in startX..endX) {
                for (y in startY.coerceIn(minY, maxY)..endY.coerceIn(minY, maxY)) {
                    for (z in startZ..endZ) {
                        val block = world.getBlockAt(x, y, z)

                        // 检查是否为加载的区块（Folia 要求，避免强制加载）
                        if (!block.chunk.isLoaded) {
                            continue
                        }

                        // 检查是否为幽灵方块（moving_piston）
                        if (block.type == Material.MOVING_PISTON) {
                            block.type = Material.AIR
                            count++
                        }
                    }
                }
            }

            // 发送结果消息
            sender.sendMessage(
                manager.getMessage(
                    "success",
                    mapOf(
                        "%count%" to count.toString(),
                        "%radius%" to radius.toString()
                    )
                )
            )
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
        if (args.size == 1) {
            // 提供半径建议（1-5）
            return (1..manager.getMaxRadius()).map { it.toString() }
                .filter { it.startsWith(args[0]) }
        }
        return emptyList()
    }

    override fun getDescription(): String {
        return "清理附近的幽灵方块"
    }
}

