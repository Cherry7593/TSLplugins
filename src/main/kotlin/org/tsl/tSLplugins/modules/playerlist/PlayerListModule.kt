package org.tsl.tSLplugins.modules.playerlist

import org.tsl.tSLplugins.SubCommandHandler
import org.tsl.tSLplugins.core.AbstractModule

/**
 * 玩家列表模块
 * 提供按世界分类的在线玩家列表命令
 */
class PlayerListModule : AbstractModule() {
    override val id = "playerlist"
    override val configPath = "playerlist"
    override fun getDescription() = "玩家列表命令"

    override fun doEnable() {
        // 无需额外初始化，仅提供命令
    }

    override fun getCommandHandler(): SubCommandHandler = PlayerListCommand()
}
