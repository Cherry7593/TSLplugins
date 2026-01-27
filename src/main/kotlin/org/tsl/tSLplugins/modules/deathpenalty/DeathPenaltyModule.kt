package org.tsl.tSLplugins.modules.deathpenalty

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.tsl.tSLplugins.modules.xconomytrigger.XconomyApi
import org.tsl.tSLplugins.core.AbstractModule

/**
 * DeathPenalty 模块 - 死亡金币惩罚
 */
class DeathPenaltyModule : AbstractModule() {

    override val id = "death-penalty"
    override val configPath = "death-penalty"

    private var penaltyAmount = 1.0
    private lateinit var xconomyApi: XconomyApi
    private lateinit var listener: DeathPenaltyModuleListener

    private var xconomyAvailable = false

    override fun loadConfig() {
        // 调用父类加载基本 enabled 配置
        super.loadConfig()
        
        // 如果启用，检查 XConomy 依赖
        if (isEnabled()) {
            xconomyApi = XconomyApi(context.plugin)
            xconomyAvailable = xconomyApi.isAvailable() && xconomyApi.canWithdraw()
            if (!xconomyAvailable) {
                logWarning("XConomy 未安装或不可用，模块功能受限")
            }
        }
    }

    override fun isEnabled(): Boolean = super.isEnabled() && xconomyAvailable

    override fun doEnable() {
        loadPenaltyConfig()
        listener = DeathPenaltyModuleListener(this)
        registerListener(listener)
    }

    override fun doReload() {
        xconomyApi.reload()
        loadPenaltyConfig()
    }

    override fun getDescription(): String = "死亡金币惩罚"

    private fun loadPenaltyConfig() {
        penaltyAmount = getConfigDouble("amount", 1.0)
        logInfo("死亡惩罚金额: $penaltyAmount")
    }

    fun getPenaltyAmount(): Double = penaltyAmount
    fun getBalance(player: Player): Double? = xconomyApi.getBalance(player)
    fun withdraw(player: Player, amount: Double): Boolean = xconomyApi.withdraw(player, amount)
    fun getPlugin() = context.plugin
    fun getMessageManager() = context.messageManager
}

class DeathPenaltyModuleListener(private val module: DeathPenaltyModule) : Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerDeath(event: PlayerDeathEvent) {
        if (!module.isEnabled()) return
        val player = event.entity
        Bukkit.getAsyncScheduler().runNow(module.getPlugin()) { _ ->
            processDeathPenalty(player)
        }
    }

    private fun processDeathPenalty(player: Player) {
        val penaltyAmount = module.getPenaltyAmount()
        val balance = module.getBalance(player)
        if (balance == null || balance < penaltyAmount) {
            if (balance != null) sendMessage(player, "death-penalty.insufficient-balance")
            return
        }
        if (module.withdraw(player, penaltyAmount)) {
            sendMessage(player, "death-penalty.deducted", mapOf("amount" to penaltyAmount.toString()))
        }
    }

    private fun sendMessage(player: Player, messageKey: String, placeholders: Map<String, String> = emptyMap()) {
        if (!player.isOnline) return
        player.scheduler.run(module.getPlugin(), { _ ->
            val replacements = placeholders.map { it.key to it.value }.toTypedArray()
            val message = module.getMessageManager().get(messageKey, *replacements)
            player.sendMessage(message)
        }, null)
    }
}
