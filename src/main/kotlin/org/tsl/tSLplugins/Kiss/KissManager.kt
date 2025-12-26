package org.tsl.tSLplugins.Kiss

import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.tsl.tSLplugins.PlayerDataManager
import org.tsl.tSLplugins.TSLplugins
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Kiss 功能管理器
 * 负责管理亲吻功能的配置和状态
 */
class KissManager(
    private val plugin: JavaPlugin,
    private val dataManager: PlayerDataManager
) {

    private var enabled: Boolean = true
    private var cooldown: Long = 1000

    private val msg get() = (plugin as TSLplugins).messageManager

    // 玩家冷却时间（UUID -> 最后使用时间戳）
    private val playerCooldowns: MutableMap<UUID, Long> = ConcurrentHashMap()

    init {
        loadConfig()
    }

    /**
     * 加载配置
     */
    fun loadConfig() {
        val config = plugin.config
        enabled = config.getBoolean("kiss.enabled", true)
        cooldown = (config.getDouble("kiss.cooldown", 1.0) * 1000).toLong()
    }

    /**
     * 检查功能是否启用
     */
    fun isEnabled(): Boolean = enabled

    /**
     * 获取冷却时间（毫秒）
     */
    fun getCooldown(): Long = cooldown

    /**
     * 检查玩家是否启用了功能（从 PDC 读取）
     */
    fun isPlayerEnabled(player: Player): Boolean {
        return dataManager.getKissToggle(player, true)
    }

    /**
     * 切换玩家的功能开关状态（写入 PDC）
     * @return 切换后的状态
     */
    fun togglePlayer(player: Player): Boolean {
        val currentStatus = isPlayerEnabled(player)
        val newStatus = !currentStatus
        dataManager.setKissToggle(player, newStatus)
        return newStatus
    }

    /**
     * 检查玩家是否在冷却中
     */
    fun isInCooldown(uuid: UUID): Boolean {
        val lastUsed = playerCooldowns[uuid] ?: return false
        return System.currentTimeMillis() - lastUsed < cooldown
    }

    /**
     * 获取玩家剩余冷却时间（秒）
     */
    fun getRemainingCooldown(uuid: UUID): Double {
        val lastUsed = playerCooldowns[uuid] ?: return 0.0
        val remaining = cooldown - (System.currentTimeMillis() - lastUsed)
        return if (remaining > 0) remaining / 1000.0 else 0.0
    }

    /**
     * 设置玩家冷却时间
     */
    fun setCooldown(uuid: UUID) {
        playerCooldowns[uuid] = System.currentTimeMillis()
    }

    /**
     * 增加玩家亲吻次数（持久化到 Profile）
     */
    fun incrementKissCount(uuid: UUID) {
        val profile = dataManager.getProfileStore().getOrCreate(uuid, "Unknown")
        profile.kissCount++
        // 数据会在玩家退出时自动保存
    }

    /**
     * 增加玩家被亲吻次数（持久化到 Profile）
     */
    fun incrementKissedCount(uuid: UUID) {
        val profile = dataManager.getProfileStore().getOrCreate(uuid, "Unknown")
        profile.kissedCount++
        // 数据会在玩家退出时自动保存
    }

    /**
     * 获取玩家亲吻次数（从 Profile 读取）
     */
    fun getKissCount(uuid: UUID): Int {
        val profile = dataManager.getProfileStore().get(uuid)
        return profile?.kissCount ?: 0
    }

    /**
     * 获取玩家被亲吻次数（从 Profile 读取）
     */
    fun getKissedCount(uuid: UUID): Int {
        val profile = dataManager.getProfileStore().get(uuid)
        return profile?.kissedCount ?: 0
    }

    /**
     * 清理玩家数据（仅清理冷却，Profile 数据会自动保存）
     */
    fun cleanupPlayer(uuid: UUID) {
        playerCooldowns.remove(uuid)
        // Profile 数据会在玩家退出时自动保存，无需手动处理
    }

    /**
     * 获取消息文本
     */
    fun getMessage(key: String, vararg replacements: Pair<String, String>): String {
        return msg.getModule("kiss", key, *replacements)
    }
}

