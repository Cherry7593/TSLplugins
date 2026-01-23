package org.tsl.tSLplugins.DeathPenalty

import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.tsl.tSLplugins.XconomyTrigger.XconomyApi

/**
 * 死亡金币惩罚管理器
 * 管理死亡时扣除金币的配置和逻辑
 */
class DeathPenaltyManager(private val plugin: JavaPlugin) {

    private var enabled = false
    private var penaltyAmount = 1.0

    private val xconomyApi = XconomyApi(plugin)

    init {
        loadConfig()
    }

    /**
     * 加载配置
     */
    fun loadConfig() {
        val config = plugin.config
        val section = config.getConfigurationSection("death-penalty")

        if (section == null) {
            enabled = false
            plugin.logger.info("[DeathPenalty] 配置节不存在，模块已禁用")
            return
        }

        enabled = section.getBoolean("enabled", false)
        penaltyAmount = section.getDouble("amount", 1.0)

        if (enabled) {
            if (!xconomyApi.isAvailable()) {
                enabled = false
                plugin.logger.warning("[DeathPenalty] XConomy 未安装或不可用，模块已禁用")
                return
            }

            if (!xconomyApi.canWithdraw()) {
                enabled = false
                plugin.logger.warning("[DeathPenalty] XConomy 扣除方法不可用，模块已禁用")
                return
            }

            plugin.logger.info("[DeathPenalty] 模块已启用，死亡惩罚金额: $penaltyAmount")
        } else {
            plugin.logger.info("[DeathPenalty] 模块未启用")
        }
    }

    /**
     * 检查模块是否启用
     */
    fun isEnabled(): Boolean = enabled

    /**
     * 获取惩罚金额
     */
    fun getPenaltyAmount(): Double = penaltyAmount

    /**
     * 获取玩家余额
     */
    fun getBalance(player: Player): Double? = xconomyApi.getBalance(player)

    /**
     * 从玩家账户扣除金币
     * @return true 表示扣除成功
     */
    fun withdraw(player: Player, amount: Double): Boolean = xconomyApi.withdraw(player, amount)

    /**
     * 重新加载配置
     */
    fun reload() {
        xconomyApi.reload()
        loadConfig()
    }
}
