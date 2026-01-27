package org.tsl.tSLplugins.modules.fixghost

import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.tsl.tSLplugins.SubCommandHandler
import org.tsl.tSLplugins.core.AbstractModule
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * FixGhost 模块 - 幽灵方块清理
 * 
 * 清理附近的幽灵方块（moving_piston）
 * 
 * ## 命令
 * - `/tsl fixghost [半径]` - 清理附近的幽灵方块
 * 
 * ## 权限
 * - `tsl.fixghost.use` - 使用幽灵方块清理
 */
class FixGhostModule : AbstractModule() {

    override val id = "fixghost"
    override val configPath = "fixghost"

    // 配置项
    private var defaultRadius: Int = 5
    private var maxRadius: Int = 5
    private var cooldown: Long = 5000L

    // 冷却记录
    private val cooldowns: MutableMap<UUID, Long> = ConcurrentHashMap()

    override fun doEnable() {
        loadFixGhostConfig()
    }

    override fun doDisable() {
        cooldowns.clear()
    }

    override fun doReload() {
        loadFixGhostConfig()
    }

    override fun getCommandHandler(): SubCommandHandler = FixGhostModuleCommand(this)
    
    override fun getDescription(): String = "幽灵方块清理"

    private fun loadFixGhostConfig() {
        defaultRadius = getConfigInt("default-radius", 5).coerceIn(1, 10)
        maxRadius = getConfigInt("max-radius", 5).coerceIn(1, 10)
        cooldown = (getConfigDouble("cooldown", 5.0) * 1000).toLong()
    }

    // ============== 公开 API ==============

    fun getDefaultRadius(): Int = defaultRadius
    fun getMaxRadius(): Int = maxRadius
    fun getCooldown(): Long = cooldown

    fun checkCooldown(uuid: UUID): Long {
        val currentTime = System.currentTimeMillis()
        val lastUsed = cooldowns.getOrDefault(uuid, 0L)
        val remaining = cooldown - (currentTime - lastUsed)
        return if (remaining > 0) remaining else 0
    }

    fun updateCooldown(uuid: UUID) {
        if (cooldown > 0) {
            cooldowns[uuid] = System.currentTimeMillis()
        }
    }

    fun getPlugin() = context.plugin

    fun getModuleMessage(key: String, vararg replacements: Pair<String, String>): String {
        return getMessage(key, *replacements)
    }
}

class FixGhostModuleCommand(private val module: FixGhostModule) : SubCommandHandler {

    override fun handle(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!module.isEnabled()) {
            sender.sendMessage(module.getModuleMessage("disabled"))
            return true
        }

        if (sender !is Player) {
            sender.sendMessage(module.getModuleMessage("console-only"))
            return true
        }

        if (!sender.hasPermission("tsl.fixghost.use")) {
            sender.sendMessage(module.getModuleMessage("no-permission"))
            return true
        }

        // 检查冷却
        val remaining = module.checkCooldown(sender.uniqueId)
        if (remaining > 0) {
            val formatted = String.format("%.1f", remaining / 1000.0)
            sender.sendMessage(module.getModuleMessage("cooldown", "cooldown" to formatted))
            return true
        }

        val radius = if (args.isNotEmpty()) {
            args[0].toIntOrNull()?.coerceIn(1, module.getMaxRadius()) ?: module.getDefaultRadius()
        } else {
            module.getDefaultRadius()
        }

        // 使用 Folia 玩家调度器执行
        sender.scheduler.run(module.getPlugin(), { _ ->
            val playerLoc = sender.location
            val world = sender.world

            var count = 0

            val startX = playerLoc.blockX - radius
            val startY = playerLoc.blockY - radius
            val startZ = playerLoc.blockZ - radius
            val endX = playerLoc.blockX + radius
            val endY = playerLoc.blockY + radius
            val endZ = playerLoc.blockZ + radius

            val minY = world.minHeight
            val maxY = world.maxHeight - 1

            for (x in startX..endX) {
                for (y in startY.coerceIn(minY, maxY)..endY.coerceIn(minY, maxY)) {
                    for (z in startZ..endZ) {
                        val block = world.getBlockAt(x, y, z)
                        if (!block.chunk.isLoaded) continue

                        if (block.type == Material.MOVING_PISTON) {
                            block.type = Material.AIR
                            count++
                        }
                    }
                }
            }

            sender.sendMessage(
                module.getModuleMessage(
                    "success",
                    "count" to count.toString(),
                    "radius" to radius.toString()
                )
            )
        }, null)

        module.updateCooldown(sender.uniqueId)
        return true
    }

    override fun tabComplete(sender: CommandSender, command: Command, label: String, args: Array<out String>): List<String> {
        if (args.size == 1) {
            return (1..module.getMaxRadius()).map { it.toString() }.filter { it.startsWith(args[0]) }
        }
        return emptyList()
    }

    override fun getDescription(): String = "清理附近的幽灵方块"
}
