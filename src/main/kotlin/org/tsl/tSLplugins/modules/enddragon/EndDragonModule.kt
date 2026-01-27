package org.tsl.tSLplugins.modules.enddragon

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.EnderDragon
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityExplodeEvent
import org.tsl.tSLplugins.SubCommandHandler
import org.tsl.tSLplugins.core.AbstractModule

/**
 * EndDragon 模块 - 末影龙控制
 * 管理末影龙的破坏行为
 */
class EndDragonModule : AbstractModule() {

    override val id = "enddragon"
    override val configPath = "enddragon"

    private var disableDamage: Boolean = true
    private lateinit var listener: EndDragonModuleListener

    override fun doEnable() {
        loadDragonConfig()
        listener = EndDragonModuleListener(this)
        registerListener(listener)
    }

    override fun doReload() {
        loadDragonConfig()
    }

    override fun getCommandHandler(): SubCommandHandler = EndDragonModuleCommand(this)
    override fun getDescription(): String = "末影龙控制"

    private fun loadDragonConfig() {
        disableDamage = getConfigBoolean("disable-damage", true)
        logInfo("配置已加载 - 禁止破坏: $disableDamage")
    }

    fun isDisableDamage(): Boolean = disableDamage && isEnabled()
}

class EndDragonModuleListener(private val module: EndDragonModule) : Listener {
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onEntityExplode(event: EntityExplodeEvent) {
        if (!module.isEnabled()) return
        if (!module.isDisableDamage()) return

        // 检查是否是末影龙造成的爆炸
        val entity = event.entity
        if (entity is EnderDragon) {
            // 阻止末影龙破坏方块
            event.blockList().clear()
        }
    }
}

class EndDragonModuleCommand(private val module: EndDragonModule) : SubCommandHandler {
    override fun handle(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) { sender.sendMessage(Component.text("此命令只能由玩家使用！", NamedTextColor.RED)); return true }
        if (!sender.hasPermission("tsl.enddragon")) { sender.sendMessage(Component.text("你没有权限使用此命令！", NamedTextColor.RED)); return true }
        if (!module.isEnabled()) { sender.sendMessage(Component.text("末影龙控制功能未启用！", NamedTextColor.RED)); return true }
        if (args.isEmpty()) { showHelp(sender); return true }

        when (args[0].lowercase()) {
            "on" -> sender.sendMessage(Component.text("✓ ", NamedTextColor.GREEN).append(Component.text("末影龙控制已启用", NamedTextColor.YELLOW)))
            "off" -> sender.sendMessage(Component.text("✓ ", NamedTextColor.GREEN).append(Component.text("末影龙控制已禁用", NamedTextColor.YELLOW)))
            "status" -> {
                sender.sendMessage(Component.text("═══ 末影龙控制状态 ═══", NamedTextColor.AQUA))
                val damageStat = if (module.isDisableDamage()) "✓ 已禁止" else "✗ 已启用"
                sender.sendMessage(Component.text("禁止破坏方块: ", NamedTextColor.YELLOW).append(Component.text(damageStat, if (module.isDisableDamage()) NamedTextColor.GREEN else NamedTextColor.RED)))
            }
            else -> showHelp(sender)
        }
        return true
    }

    private fun showHelp(sender: Player) {
        sender.sendMessage(Component.text("═══ 末影龙控制命令 ═══", NamedTextColor.AQUA))
        sender.sendMessage(Component.text("  /tsl enddragon on", NamedTextColor.YELLOW).append(Component.text(" - 启用末影龙控制", NamedTextColor.GRAY)))
        sender.sendMessage(Component.text("  /tsl enddragon off", NamedTextColor.YELLOW).append(Component.text(" - 禁用末影龙控制", NamedTextColor.GRAY)))
        sender.sendMessage(Component.text("  /tsl enddragon status", NamedTextColor.YELLOW).append(Component.text(" - 查看当前状态", NamedTextColor.GRAY)))
    }

    override fun tabComplete(sender: CommandSender, command: Command, label: String, args: Array<out String>): List<String> {
        return when {
            args.isEmpty() -> listOf("on", "off", "status")
            args.size == 1 -> listOf("on", "off", "status").filter { it.startsWith(args[0].lowercase()) }
            else -> emptyList()
        }
    }

    override fun getDescription(): String = "末影龙控制"
}
