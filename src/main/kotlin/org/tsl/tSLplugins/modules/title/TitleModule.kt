package org.tsl.tSLplugins.modules.title

import org.tsl.tSLplugins.core.AbstractModule

/**
 * 称号系统模块
 * 通过 LuckPerms 管理玩家称号
 * 注意：此模块通常与 WebBridge 配合使用
 * TitleCommand 需要 WebBridgeManager，因此命令由 WebBridge 模块提供
 */
class TitleModule : AbstractModule() {
    override val id = "title"
    override val configPath = "title"
    override fun getDescription() = "称号系统管理"

    lateinit var manager: TitleManager
        private set

    override fun doEnable() {
        manager = TitleManager(context.plugin)
        manager.initialize()
    }

    override fun doDisable() {
        manager.shutdown()
    }

    override fun doReload() {
        manager.reload()
    }
    
    // TitleCommand 需要 WebBridgeManager 因此不在此模块注册
    // 命令由 TSLplugins 的旧架构或 WebBridge 模块处理
}
