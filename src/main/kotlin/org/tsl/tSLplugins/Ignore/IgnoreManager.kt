package org.tsl.tSLplugins.Ignore

import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 聊天屏蔽管理器
 *
 * 功能：
 * - 管理玩家之间的单向屏蔽关系
 * - 内存缓存 + 持久化存储
 * - 支持热重载
 */
class IgnoreManager(private val plugin: JavaPlugin) {

    // 内存中的屏蔽映射：viewer -> Set<被屏蔽的玩家UUID>
    private val ignoreMap = ConcurrentHashMap<UUID, MutableSet<UUID>>()

    // 模块配置
    private var enabled = true
    private var maxIgnoreCount = 100

    /**
     * 加载配置
     */
    fun loadConfig() {
        val config = plugin.config.getConfigurationSection("ignore")
        enabled = config?.getBoolean("enabled", true) ?: true
        maxIgnoreCount = config?.getInt("max-ignore-count", 100) ?: 100

        if (enabled) {
            plugin.logger.info("[Ignore] 聊天屏蔽模块已启用 (最大屏蔽数: $maxIgnoreCount)")
        } else {
            plugin.logger.info("[Ignore] 聊天屏蔽模块未启用")
        }
    }

    /**
     * 检查模块是否启用
     */
    fun isEnabled(): Boolean = enabled

    /**
     * 获取最大屏蔽数
     */
    fun getMaxIgnoreCount(): Int = maxIgnoreCount

    // ==================== 屏蔽操作 ====================

    /**
     * 添加屏蔽
     * @param viewer 执行屏蔽的玩家
     * @param target 被屏蔽的玩家
     * @return 是否成功添加（false 表示已达上限或已屏蔽）
     */
    fun addIgnore(viewer: UUID, target: UUID): Boolean {
        if (viewer == target) return false

        val ignoreSet = ignoreMap.computeIfAbsent(viewer) { ConcurrentHashMap.newKeySet() }

        if (ignoreSet.size >= maxIgnoreCount) {
            return false
        }

        return ignoreSet.add(target)
    }

    /**
     * 移除屏蔽
     */
    fun removeIgnore(viewer: UUID, target: UUID): Boolean {
        val ignoreSet = ignoreMap[viewer] ?: return false
        return ignoreSet.remove(target)
    }

    /**
     * 切换屏蔽状态
     * @return Pair<是否成功, 当前是否为屏蔽状态>
     */
    fun toggleIgnore(viewer: UUID, target: UUID): Pair<Boolean, Boolean> {
        if (isIgnoring(viewer, target)) {
            val success = removeIgnore(viewer, target)
            return Pair(success, false)
        } else {
            val success = addIgnore(viewer, target)
            return Pair(success, true)
        }
    }

    /**
     * 检查是否屏蔽
     */
    fun isIgnoring(viewer: UUID, sender: UUID): Boolean {
        return ignoreMap[viewer]?.contains(sender) ?: false
    }

    /**
     * 获取玩家的屏蔽列表
     */
    fun getIgnoreList(viewer: UUID): Set<UUID> {
        return ignoreMap[viewer]?.toSet() ?: emptySet()
    }

    /**
     * 获取屏蔽列表大小
     */
    fun getIgnoreCount(viewer: UUID): Int {
        return ignoreMap[viewer]?.size ?: 0
    }

    // ==================== 玩家生命周期 ====================

    /**
     * 玩家加入时加载屏蔽数据
     */
    fun loadPlayerData(playerUuid: UUID, ignoreList: Set<UUID>) {
        if (ignoreList.isNotEmpty()) {
            val set = ConcurrentHashMap.newKeySet<UUID>()
            set.addAll(ignoreList)
            ignoreMap[playerUuid] = set
        }
    }

    /**
     * 获取玩家的屏蔽数据（用于保存）
     */
    fun getPlayerData(playerUuid: UUID): Set<UUID> {
        return ignoreMap[playerUuid]?.toSet() ?: emptySet()
    }

    /**
     * 玩家退出时清理内存
     */
    fun unloadPlayerData(playerUuid: UUID) {
        ignoreMap.remove(playerUuid)
    }

    /**
     * 关闭模块
     */
    fun shutdown() {
        ignoreMap.clear()
    }
}

