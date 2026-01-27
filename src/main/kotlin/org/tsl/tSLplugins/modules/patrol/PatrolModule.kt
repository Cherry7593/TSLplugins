package org.tsl.tSLplugins.modules.patrol

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.tsl.tSLplugins.SubCommandHandler
import org.tsl.tSLplugins.core.AbstractModule
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Patrol 模块 - 巡逻功能
 * 随机传送到玩家位置进行巡查
 */
class PatrolModule : AbstractModule() {

    override val id = "patrol"
    override val configPath = "patrol"

    private val patrolRecords = ConcurrentHashMap<UUID, Long>()
    private val currentCyclePatrolled = ConcurrentHashMap.newKeySet<UUID>()
    private val cooldownMillis = 10 * 60 * 1000L

    override fun doEnable() {
        // 无需特殊初始化
    }

    override fun doDisable() {
        patrolRecords.clear()
        currentCyclePatrolled.clear()
    }

    override fun getCommandHandler(): SubCommandHandler = PatrolModuleCommand(this)
    override fun getDescription(): String = "巡逻功能"

    fun patrol(patroller: Player): PatrolResult {
        val onlinePlayers = Bukkit.getOnlinePlayers().filter { it.uniqueId != patroller.uniqueId }
        if (onlinePlayers.isEmpty()) return PatrolResult.NoPlayers

        cleanExpiredRecords()
        val candidates = getCandidates(onlinePlayers)

        if (candidates.isEmpty()) {
            val allInCooldown = onlinePlayers.all { isInCooldown(it.uniqueId) }
            if (allInCooldown) return startNewCycle(patroller, onlinePlayers)
            else { currentCyclePatrolled.clear(); return patrol(patroller) }
        }

        val target = candidates.random()
        val now = System.currentTimeMillis()
        patrolRecords[target.uniqueId] = now
        currentCyclePatrolled.add(target.uniqueId)
        patroller.teleportAsync(target.location)
        return PatrolResult.Success(target, null)
    }

    private fun getCandidates(onlinePlayers: Collection<Player>): List<Player> {
        return onlinePlayers.filter { !currentCyclePatrolled.contains(it.uniqueId) && !isInCooldown(it.uniqueId) }
    }

    private fun isInCooldown(uuid: UUID): Boolean {
        val lastPatrolTime = patrolRecords[uuid] ?: return false
        return System.currentTimeMillis() - lastPatrolTime < cooldownMillis
    }

    private fun cleanExpiredRecords() {
        val now = System.currentTimeMillis()
        patrolRecords.entries.removeIf { now - it.value >= cooldownMillis }
    }

    private fun startNewCycle(patroller: Player, onlinePlayers: Collection<Player>): PatrolResult {
        currentCyclePatrolled.clear()
        val notInCooldown = onlinePlayers.filter { !isInCooldown(it.uniqueId) }
        if (notInCooldown.isNotEmpty()) {
            val target = notInCooldown.random()
            val now = System.currentTimeMillis()
            patrolRecords[target.uniqueId] = now
            currentCyclePatrolled.add(target.uniqueId)
            patroller.teleportAsync(target.location)
            return PatrolResult.Success(target, null)
        }
        val target = onlinePlayers.minByOrNull { patrolRecords[it.uniqueId] ?: 0L } ?: return PatrolResult.NoPlayers
        val lastPatrolTime = patrolRecords[target.uniqueId]!!
        val elapsed = System.currentTimeMillis() - lastPatrolTime
        val timeSinceLastPatrol = formatElapsedTime(elapsed)
        patrolRecords[target.uniqueId] = System.currentTimeMillis()
        currentCyclePatrolled.add(target.uniqueId)
        patroller.teleportAsync(target.location)
        return PatrolResult.Success(target, timeSinceLastPatrol)
    }

    private fun formatElapsedTime(millis: Long): String {
        val totalSeconds = millis / 1000
        return "${totalSeconds / 60} 分 ${totalSeconds % 60} 秒"
    }

    sealed class PatrolResult {
        data class Success(val target: Player, val timeSinceLastPatrol: String?) : PatrolResult()
        object NoPlayers : PatrolResult()
    }
}

class PatrolModuleCommand(private val module: PatrolModule) : SubCommandHandler {
    override fun handle(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) { sender.sendMessage(Component.text("此命令只能由玩家使用！", NamedTextColor.RED)); return true }
        if (!sender.hasPermission("tsl.patrol.use")) { sender.sendMessage(Component.text("你没有权限使用此命令！", NamedTextColor.RED)); return true }
        
        when (val result = module.patrol(sender)) {
            is PatrolModule.PatrolResult.Success -> {
                sender.sendMessage(Component.text("✓ ", NamedTextColor.GREEN)
                    .append(Component.text("已传送到 ", NamedTextColor.YELLOW))
                    .append(Component.text(result.target.name, NamedTextColor.WHITE))
                    .append(Component.text(" 的位置", NamedTextColor.YELLOW)))
                result.timeSinceLastPatrol?.let { time ->
                    sender.sendMessage(Component.text("  上次巡逻为 ", NamedTextColor.GRAY)
                        .append(Component.text(time, NamedTextColor.AQUA))
                        .append(Component.text(" 前", NamedTextColor.GRAY)))
                }
            }
            is PatrolModule.PatrolResult.NoPlayers -> sender.sendMessage(Component.text("没有可巡逻的玩家！", NamedTextColor.RED))
        }
        return true
    }

    override fun tabComplete(sender: CommandSender, command: Command, label: String, args: Array<out String>): List<String> = emptyList()
    override fun getDescription(): String = "巡逻功能"
}
