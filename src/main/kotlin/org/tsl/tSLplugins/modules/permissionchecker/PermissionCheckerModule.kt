package org.tsl.tSLplugins.modules.permissionchecker

import org.tsl.tSLplugins.core.AbstractModule

/**
 * 权限检测模块
 * 根据 PlaceholderAPI 变量值自动修改玩家 LuckPerms 权限组
 */
class PermissionCheckerModule : AbstractModule() {
    override val id = "permission-checker"
    override val configPath = "permission-checker"
    override fun getDescription() = "权限自动检测"

    lateinit var checker: PermissionChecker
        private set

    override fun doEnable() {
        checker = PermissionChecker(context.plugin)
        registerListener(checker)
    }

    override fun doReload() {
        checker.reload()
    }

    /**
     * 外部调用：检查玩家权限
     */
    fun checkPlayer(player: org.bukkit.entity.Player) {
        if (enabled) {
            checker.checkPlayer(player)
        }
    }
}
