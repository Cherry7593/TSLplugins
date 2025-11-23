package org.tsl.tSLplugins

import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin

/**
 * 玩家数据管理器
 * 使用 PersistentDataContainer 保存玩家个人配置
 */
class PlayerDataManager(private val plugin: JavaPlugin) {

    // PDC Keys
    private val kissToggleKey = NamespacedKey(plugin, "kiss_toggle")
    private val rideToggleKey = NamespacedKey(plugin, "ride_toggle")
    private val tossToggleKey = NamespacedKey(plugin, "toss_toggle")
    private val tossVelocityKey = NamespacedKey(plugin, "toss_velocity")

    // ==================== Kiss 功能 ====================

    /**
     * 获取玩家的 Kiss 开关状态
     * @param player 玩家
     * @param defaultValue 默认值
     * @return 开关状态
     */
    fun getKissToggle(player: Player, defaultValue: Boolean = true): Boolean {
        val pdc = player.persistentDataContainer
        return if (pdc.has(kissToggleKey, PersistentDataType.BOOLEAN)) {
            pdc.get(kissToggleKey, PersistentDataType.BOOLEAN) ?: defaultValue
        } else {
            defaultValue
        }
    }

    /**
     * 设置玩家的 Kiss 开关状态
     */
    fun setKissToggle(player: Player, enabled: Boolean) {
        player.persistentDataContainer.set(kissToggleKey, PersistentDataType.BOOLEAN, enabled)
    }

    // ==================== Ride 功能 ====================

    /**
     * 获取玩家的 Ride 开关状态
     */
    fun getRideToggle(player: Player, defaultValue: Boolean = true): Boolean {
        val pdc = player.persistentDataContainer
        return if (pdc.has(rideToggleKey, PersistentDataType.BOOLEAN)) {
            pdc.get(rideToggleKey, PersistentDataType.BOOLEAN) ?: defaultValue
        } else {
            defaultValue
        }
    }

    /**
     * 设置玩家的 Ride 开关状态
     */
    fun setRideToggle(player: Player, enabled: Boolean) {
        player.persistentDataContainer.set(rideToggleKey, PersistentDataType.BOOLEAN, enabled)
    }

    // ==================== Toss 功能 ====================

    /**
     * 获取玩家的 Toss 开关状态
     */
    fun getTossToggle(player: Player, defaultValue: Boolean = true): Boolean {
        val pdc = player.persistentDataContainer
        return if (pdc.has(tossToggleKey, PersistentDataType.BOOLEAN)) {
            pdc.get(tossToggleKey, PersistentDataType.BOOLEAN) ?: defaultValue
        } else {
            defaultValue
        }
    }

    /**
     * 设置玩家的 Toss 开关状态
     */
    fun setTossToggle(player: Player, enabled: Boolean) {
        player.persistentDataContainer.set(tossToggleKey, PersistentDataType.BOOLEAN, enabled)
    }

    /**
     * 获取玩家的 Toss 投掷速度
     */
    fun getTossVelocity(player: Player, defaultValue: Double = 1.5): Double {
        val pdc = player.persistentDataContainer
        return if (pdc.has(tossVelocityKey, PersistentDataType.DOUBLE)) {
            pdc.get(tossVelocityKey, PersistentDataType.DOUBLE) ?: defaultValue
        } else {
            defaultValue
        }
    }

    /**
     * 设置玩家的 Toss 投掷速度
     */
    fun setTossVelocity(player: Player, velocity: Double) {
        player.persistentDataContainer.set(tossVelocityKey, PersistentDataType.DOUBLE, velocity)
    }

    // ==================== 工具方法 ====================

    /**
     * 清除玩家的所有 TSL 数据（调试用）
     */
    fun clearPlayerData(player: Player) {
        val pdc = player.persistentDataContainer
        pdc.remove(kissToggleKey)
        pdc.remove(rideToggleKey)
        pdc.remove(tossToggleKey)
        pdc.remove(tossVelocityKey)
    }
}

