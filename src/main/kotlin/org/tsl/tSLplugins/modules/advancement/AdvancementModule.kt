package org.tsl.tSLplugins.modules.advancement

import org.bukkit.plugin.java.JavaPlugin
import org.tsl.tSLplugins.SubCommandHandler
import org.tsl.tSLplugins.core.AbstractModule

/**
 * 成就统计模块
 * 统计玩家成就完成数量
 */
class AdvancementModule : AbstractModule() {
    override val id = "advcount"
    override val configPath = "advancement"
    override fun getDescription() = "成就统计功能"

    lateinit var countHandler: AdvancementCount
        private set

    override fun doEnable() {
        countHandler = AdvancementCount(context.plugin)
        registerListener(countHandler)
    }

    override fun getCommandHandler(): SubCommandHandler = 
        AdvancementCommand(context.plugin, countHandler)
    
    /**
     * 清理过期缓存
     */
    fun cleanExpiredCache() {
        if (::countHandler.isInitialized) {
            countHandler.cleanExpiredCache()
        }
    }
}
