package org.tsl.tSLplugins.Patrol

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Patrol 巡逻管理器
 * 随机传送到玩家位置进行巡查
 * 内存中维护巡逻列表和时间戳，不持久化
 */
class PatrolManager(private val plugin: JavaPlugin) {

    /** 巡逻记录：玩家UUID -> 巡逻时间戳 */
    private val patrolRecords = ConcurrentHashMap<UUID, Long>()

    /** 当前循环中已巡逻的玩家 */
    private val currentCyclePatrolled = ConcurrentHashMap.newKeySet<UUID>()

    /** 巡逻冷却时间（毫秒）= 10 分钟 */
    private val cooldownMillis = 10 * 60 * 1000L

    /**
     * 执行巡逻
     * @param patroller 执行巡逻的管理员
     * @return 巡逻结果信息
     */
    fun patrol(patroller: Player): PatrolResult {
        // 获取所有在线玩家（排除自己）
        val onlinePlayers = Bukkit.getOnlinePlayers()
            .filter { it.uniqueId != patroller.uniqueId }

        if (onlinePlayers.isEmpty()) {
            return PatrolResult.NoPlayers
        }

        // 清理过期的巡逻记录
        cleanExpiredRecords()

        // 获取候选玩家（未在当前循环中被巡逻过，且不在冷却期）
        val candidates = getCandidates(onlinePlayers)

        // 如果没有候选玩家，检查是否所有人都在冷却期
        if (candidates.isEmpty()) {
            val allInCooldown = onlinePlayers.all { isInCooldown(it.uniqueId) }

            if (allInCooldown) {
                // 所有玩家都在冷却期，开始新循环
                return startNewCycle(patroller, onlinePlayers)
            } else {
                // 当前循环已完成，开始新循环
                currentCyclePatrolled.clear()
                return patrol(patroller) // 递归调用，重新选择
            }
        }

        // 随机选择一个候选玩家
        val target = candidates.random()

        // 记录巡逻
        val now = System.currentTimeMillis()
        patrolRecords[target.uniqueId] = now
        currentCyclePatrolled.add(target.uniqueId)

        // 执行传送
        patroller.teleportAsync(target.location)

        return PatrolResult.Success(target, null)
    }

    /**
     * 获取候选玩家列表
     */
    private fun getCandidates(onlinePlayers: Collection<Player>): List<Player> {
        return onlinePlayers.filter { player ->
            val uuid = player.uniqueId
            // 未在当前循环中被巡逻过，且不在冷却期
            !currentCyclePatrolled.contains(uuid) && !isInCooldown(uuid)
        }
    }

    /**
     * 检查玩家是否在冷却期
     */
    private fun isInCooldown(uuid: UUID): Boolean {
        val lastPatrolTime = patrolRecords[uuid] ?: return false
        val elapsed = System.currentTimeMillis() - lastPatrolTime
        return elapsed < cooldownMillis
    }

    /**
     * 清理过期的巡逻记录
     */
    private fun cleanExpiredRecords() {
        val now = System.currentTimeMillis()
        val iterator = patrolRecords.entries.iterator()

        while (iterator.hasNext()) {
            val entry = iterator.next()
            val elapsed = now - entry.value
            if (elapsed >= cooldownMillis) {
                iterator.remove()
            }
        }
    }

    /**
     * 开始新的巡逻循环
     */
    private fun startNewCycle(patroller: Player, onlinePlayers: Collection<Player>): PatrolResult {
        currentCyclePatrolled.clear()

        // 从在线玩家中随机选择（排除冷却期外的玩家优先）
        val notInCooldown = onlinePlayers.filter { !isInCooldown(it.uniqueId) }

        if (notInCooldown.isNotEmpty()) {
            // 优先选择不在冷却期的玩家
            val target = notInCooldown.random()
            val now = System.currentTimeMillis()
            patrolRecords[target.uniqueId] = now
            currentCyclePatrolled.add(target.uniqueId)

            patroller.teleportAsync(target.location)
            return PatrolResult.Success(target, null)
        }

        // 所有玩家都在冷却期，选择冷却时间最长的
        val target = onlinePlayers.minByOrNull { player ->
            patrolRecords[player.uniqueId] ?: 0L
        } ?: return PatrolResult.NoPlayers

        val lastPatrolTime = patrolRecords[target.uniqueId]!!
        val elapsed = System.currentTimeMillis() - lastPatrolTime
        val timeSinceLastPatrol = formatElapsedTime(elapsed)

        // 更新记录
        val now = System.currentTimeMillis()
        patrolRecords[target.uniqueId] = now
        currentCyclePatrolled.add(target.uniqueId)

        patroller.teleportAsync(target.location)
        return PatrolResult.Success(target, timeSinceLastPatrol)
    }

    /**
     * 格式化经过的时间
     */
    private fun formatElapsedTime(millis: Long): String {
        val totalSeconds = millis / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "${minutes} 分 ${seconds} 秒"
    }

    /**
     * 获取玩家的最后巡逻时间
     */
    fun getLastPatrolTime(uuid: UUID): Long? {
        return patrolRecords[uuid]
    }

    /**
     * 清理所有数据（用于插件卸载）
     */
    fun cleanup() {
        patrolRecords.clear()
        currentCyclePatrolled.clear()
    }
}

/**
 * 巡逻结果
 */
sealed class PatrolResult {
    /** 成功巡逻 */
    data class Success(
        val target: Player,
        val timeSinceLastPatrol: String? // 如果是冷却期内的二次巡逻，显示上次时间
    ) : PatrolResult()

    /** 没有可巡逻的玩家 */
    object NoPlayers : PatrolResult()
}

