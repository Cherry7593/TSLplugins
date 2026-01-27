package org.tsl.tSLplugins.modules.xconomytrigger

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.lang.reflect.Method

/**
 * XConomy API 封装
 * 提供对 XConomy 插件余额查询的封装，支持反射调用以避免硬依赖
 */
class XconomyApi(private val plugin: JavaPlugin) {

    private var xconomyAvailable = false
    private var getBalanceMethod: Method? = null
    private var changeBalanceMethod: Method? = null
    private var dataPlayerClass: Class<*>? = null

    init {
        detectXConomy()
    }

    /**
     * 检测 XConomy 插件是否可用
     */
    private fun detectXConomy() {
        try {
            // 检查 XConomy 插件是否加载
            val xconomyPlugin = Bukkit.getPluginManager().getPlugin("XConomy")
            if (xconomyPlugin == null || !xconomyPlugin.isEnabled) {
                plugin.logger.info("[XconomyApi] XConomy 插件未加载")
                return
            }

            // 尝试加载 XConomy API 类
            // XConomy 的 API 通常是 me.yic.xconomy.api.XConomyAPI
            val apiClass = Class.forName("me.yic.xconomy.api.XConomyAPI")
            
            // 获取 getPlayerData 或 getBalance 方法
            // XConomy API: XConomyAPI.getBalance(UUID uuid) 或 XConomyAPI.getPlayerBalance(UUID uuid)
            getBalanceMethod = try {
                apiClass.getMethod("getPlayerBalance", java.util.UUID::class.java)
            } catch (e: NoSuchMethodException) {
                try {
                    apiClass.getMethod("getBalance", java.util.UUID::class.java)
                } catch (e2: NoSuchMethodException) {
                    null
                }
            }

            // 获取 changePlayerBalance 方法用于扣除余额
            // XConomy API: XConomyAPI.changePlayerBalance(UUID uuid, BigDecimal amount, Boolean isAdd)
            changeBalanceMethod = try {
                apiClass.getMethod("changePlayerBalance", 
                    java.util.UUID::class.java, 
                    java.math.BigDecimal::class.java, 
                    Boolean::class.java)
            } catch (e: NoSuchMethodException) {
                try {
                    // 尝试其他可能的方法签名
                    apiClass.getMethod("takeBalance", 
                        java.util.UUID::class.java, 
                        java.math.BigDecimal::class.java)
                } catch (e2: NoSuchMethodException) {
                    null
                }
            }

            if (getBalanceMethod != null) {
                xconomyAvailable = true
                plugin.logger.info("[XconomyApi] XConomy API 已加载 (查询: ${getBalanceMethod?.name}, 扣除: ${changeBalanceMethod?.name ?: "不可用"})")
            } else {
                plugin.logger.warning("[XconomyApi] 无法找到 XConomy 余额查询方法")
            }

        } catch (e: ClassNotFoundException) {
            plugin.logger.info("[XconomyApi] XConomy API 类未找到")
        } catch (e: Exception) {
            plugin.logger.warning("[XconomyApi] 检测 XConomy 时出错: ${e.message}")
        }
    }

    /**
     * 检查 XConomy 是否可用
     */
    fun isAvailable(): Boolean = xconomyAvailable

    /**
     * 获取玩家余额
     * @param player 玩家
     * @return 余额，如果获取失败返回 null
     */
    fun getBalance(player: Player): Double? {
        if (!xconomyAvailable || getBalanceMethod == null) {
            return null
        }

        return try {
            val result = getBalanceMethod!!.invoke(null, player.uniqueId)
            when (result) {
                is Double -> result
                is Number -> result.toDouble()
                is java.math.BigDecimal -> result.toDouble()
                else -> {
                    plugin.logger.warning("[XconomyApi] 未知的余额返回类型: ${result?.javaClass}")
                    null
                }
            }
        } catch (e: Exception) {
            plugin.logger.warning("[XconomyApi] 获取玩家 ${player.name} 余额失败: ${e.message}")
            null
        }
    }

    /**
     * 检查扣除功能是否可用
     */
    fun canWithdraw(): Boolean = xconomyAvailable && changeBalanceMethod != null

    /**
     * 从玩家账户扣除金币
     * @param player 玩家
     * @param amount 扣除金额
     * @return true 表示扣除成功，false 表示失败
     */
    fun withdraw(player: Player, amount: Double): Boolean {
        if (!xconomyAvailable || changeBalanceMethod == null) {
            return false
        }

        return try {
            val bigDecimalAmount = java.math.BigDecimal.valueOf(amount)
            
            // changePlayerBalance(UUID, BigDecimal, Boolean isAdd)
            // isAdd = false 表示扣除
            if (changeBalanceMethod!!.parameterCount == 3) {
                changeBalanceMethod!!.invoke(null, player.uniqueId, bigDecimalAmount, false)
            } else {
                // takeBalance(UUID, BigDecimal)
                changeBalanceMethod!!.invoke(null, player.uniqueId, bigDecimalAmount)
            }
            true
        } catch (e: Exception) {
            plugin.logger.warning("[XconomyApi] 扣除玩家 ${player.name} 金币失败: ${e.message}")
            false
        }
    }

    /**
     * 重新检测 XConomy
     */
    fun reload() {
        xconomyAvailable = false
        getBalanceMethod = null
        changeBalanceMethod = null
        detectXConomy()
    }
}
