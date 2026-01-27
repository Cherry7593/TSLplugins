package org.tsl.tSLplugins.modules.vault

import org.bukkit.plugin.java.JavaPlugin
import org.tsl.tSLplugins.core.AbstractModule

/**
 * Vault 宝库重置模块
 * 基于 epoch 周期系统，周期性重置宝库
 */
class VaultModule : AbstractModule() {
    override val id = "vault"
    override val configPath = "vault"
    override fun getDescription() = "宝库周期重置功能"

    lateinit var manager: VaultManager
        private set
    private lateinit var listener: VaultListener

    override fun doEnable() {
        // 初始化 NMS 助手
        VaultNBTHelper.init(context.plugin)
        
        // 初始化管理器
        manager = VaultManager(context.plugin)
        
        // 注册监听器
        listener = VaultListener(context.plugin, manager)
        registerListener(listener)
    }

    override fun doReload() {
        manager.reload()
    }
}
