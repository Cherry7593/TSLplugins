package org.tsl.tSLplugins.Advancement

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerAdvancementDoneEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class AdvancementCount(private val plugin: JavaPlugin) : Listener {

    // 缓存结构：UUID -> CacheEntry
    private val cache = ConcurrentHashMap<UUID, CacheEntry>()

    // 缓存过期时间（毫秒）
    companion object {
        private const val CACHE_EXPIRY_MS = 60_000L // 60秒
    }

    /**
     * 缓存条目
     */
    private data class CacheEntry(
        var count: Int,
        var timestamp: Long
    ) {
        fun isExpired(): Boolean {
            return System.currentTimeMillis() - timestamp > CACHE_EXPIRY_MS
        }
    }

    /**
     * 获取玩家完成的成就数量（使用缓存）
     */
    fun getAdvancementCount(player: Player): Int {
        val uuid = player.uniqueId
        val entry = cache[uuid]

        // 如果缓存存在且未过期，直接返回
        if (entry != null && !entry.isExpired()) {
            return entry.count
        }

        // 否则重新计算
        val count = calculateAdvancementCount(player)
        cache[uuid] = CacheEntry(count, System.currentTimeMillis())
        return count
    }

    /**
     * 计算玩家完成的成就数量（全量统计）
     * 过滤掉配方进度，只统计真正的成就
     */
    private fun calculateAdvancementCount(player: Player): Int {
        var count = 0
        val iterator = Bukkit.getServer().advancementIterator()

        while (iterator.hasNext()) {
            val advancement = iterator.next()

            // 过滤掉配方进度（recipes 开头的进度）
            val key = advancement.key.toString()
            if (key.contains("recipes/")) {
                continue
            }

            // 只统计有显示信息的成就（配方进度通常没有显示信息）
            if (advancement.display == null) {
                continue
            }

            if (player.getAdvancementProgress(advancement).isDone) {
                count++
            }
        }

        return count
    }

    /**
     * 强制刷新玩家的成就统计（重新全量计算）
     */
    fun refreshCount(player: Player) {
        val count = calculateAdvancementCount(player)
        cache[player.uniqueId] = CacheEntry(count, System.currentTimeMillis())
        plugin.logger.info("已刷新玩家 ${player.name} 的成就统计: $count")
    }

    /**
     * 刷新所有在线玩家的成就统计
     */
    fun refreshAllCounts() {
        for (player in Bukkit.getOnlinePlayers()) {
            // 使用实体调度器以兼容 Folia
            player.scheduler.run(plugin, { _ ->
                if (player.isOnline) {
                    refreshCount(player)
                }
            }, null)
        }
    }

    /**
     * 监听玩家完成成就事件，自动 +1
     * 过滤掉配方进度，只统计真正的成就
     */
    @EventHandler
    fun onAdvancementDone(event: PlayerAdvancementDoneEvent) {
        val player = event.player
        val uuid = player.uniqueId

        // 过滤掉配方进度
        val key = event.advancement.key.toString()
        if (key.contains("recipes/")) {
            return
        }

        // 只处理有显示信息的成就
        if (event.advancement.display == null) {
            return
        }

        // 使用实体调度器以兼容 Folia
        player.scheduler.run(plugin, { _ ->
            val entry = cache[uuid]

            if (entry != null && !entry.isExpired()) {
                // 缓存存在且未过期，直接 +1
                entry.count++
                entry.timestamp = System.currentTimeMillis() // 更新时间戳
            } else {
                // 缓存不存在或已过期，重新计算
                val count = calculateAdvancementCount(player)
                cache[uuid] = CacheEntry(count, System.currentTimeMillis())
            }
        }, null)
    }

    /**
     * 玩家退出时清理缓存
     */
    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        cache.remove(event.player.uniqueId)
    }

    /**
     * 清理所有过期缓存（可定期调用）
     */
    fun cleanExpiredCache() {
        cache.entries.removeIf { it.value.isExpired() }
    }
}

