package org.tsl.tSLplugins.modules.townphome

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.tsl.tSLplugins.SubCommandHandler

/**
 * 小镇PHome命令处理器
 * 处理 /tsl phome 命令
 */
class TownPHomeCommand(
    private val manager: TownPHomeManager,
    private val gui: TownPHomeGUI
) : SubCommandHandler {

    private val serializer = LegacyComponentSerializer.legacyAmpersand()
    
    // 保留字列表 - 这些名称不能用作 phome 名称
    companion object {
        val RESERVED_WORDS = setOf("set", "del", "delete", "remove", "gui", "help", "list", "tp")
    }

    override fun handle(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!manager.isEnabled()) {
            sender.sendMessage(serializer.deserialize(manager.getMessage("disabled")))
            return true
        }

        if (sender !is Player) {
            sender.sendMessage(serializer.deserialize(manager.getMessage("player_only")))
            return true
        }

        if (!manager.isPapiAvailable()) {
            sender.sendMessage(serializer.deserialize(manager.getMessage("papi_not_available")))
            return true
        }

        // 无参数时直接打开 GUI
        if (args.isEmpty()) {
            gui.open(sender)
            return true
        }

        val townName = manager.getPlayerTownName(sender)
        
        // 处理命令
        when (args[0].lowercase()) {
            "gui" -> {
                gui.open(sender)
                return true
            }
            "help" -> {
                showHelp(sender)
                return true
            }
            "set" -> {
                if (townName == null) {
                    sender.sendMessage(serializer.deserialize(manager.getMessage("not_in_town")))
                    return true
                }
                handleSet(sender, townName, args)
                return true
            }
            "del", "delete", "remove" -> {
                if (townName == null) {
                    sender.sendMessage(serializer.deserialize(manager.getMessage("not_in_town")))
                    return true
                }
                handleDelete(sender, townName, args)
                return true
            }
            else -> {
                // 直接传送：/phome <名称>
                if (townName == null) {
                    sender.sendMessage(serializer.deserialize(manager.getMessage("not_in_town")))
                    return true
                }
                handleTeleport(sender, townName, args[0])
                return true
            }
        }
    }

    private fun handleList(player: Player, townName: String) {
        val homes = manager.getHomes(townName)
        val townLevel = manager.getPlayerTownLevel(player)
        val limit = manager.getLimit(townLevel)
        val count = homes.size

        player.sendMessage(serializer.deserialize(
            manager.getMessage("list_header", 
                "town" to townName,
                "count" to count.toString(),
                "limit" to limit.toString()
            )
        ))

        if (homes.isEmpty()) {
            player.sendMessage(serializer.deserialize(manager.getMessage("list_empty")))
        } else {
            homes.values.forEach { home ->
                player.sendMessage(serializer.deserialize(
                    manager.getMessage("list_entry",
                        "name" to home.name,
                        "world" to home.world
                    )
                ))
            }
        }
    }

    private fun handleSet(player: Player, townName: String, args: Array<out String>) {
        if (!manager.canManage(player)) {
            player.sendMessage(serializer.deserialize(manager.getMessage("no_permission")))
            return
        }

        if (args.size < 2) {
            player.sendMessage(serializer.deserialize(manager.getMessage("usage_set")))
            return
        }

        val homeName = args[1]
        
        // 检查保留字
        if (homeName.lowercase() in RESERVED_WORDS) {
            player.sendMessage(serializer.deserialize("&c名称 '$homeName' 是保留字，不能用作PHome名称！"))
            return
        }
        
        val townLevel = manager.getPlayerTownLevel(player)

        if (!manager.canCreateHome(townName, homeName, townLevel)) {
            val limit = manager.getLimit(townLevel)
            player.sendMessage(serializer.deserialize(
                manager.getMessage("limit_reached", "limit" to limit.toString())
            ))
            return
        }

        val isOverwrite = manager.getHome(townName, homeName) != null

        if (manager.setHome(townName, homeName, player)) {
            if (isOverwrite) {
                player.sendMessage(serializer.deserialize(
                    manager.getMessage("set_overwrite", "name" to homeName)
                ))
            } else {
                player.sendMessage(serializer.deserialize(
                    manager.getMessage("set_success", "name" to homeName)
                ))
            }
            // 通知小镇成员
            manager.notifyTownMembers(townName, player, homeName, true)
        } else {
            player.sendMessage(serializer.deserialize(manager.getMessage("set_failed")))
        }
    }

    private fun handleDelete(player: Player, townName: String, args: Array<out String>) {
        if (!manager.canManage(player)) {
            player.sendMessage(serializer.deserialize(manager.getMessage("no_permission")))
            return
        }

        if (args.size < 2) {
            player.sendMessage(serializer.deserialize(manager.getMessage("usage_del")))
            return
        }

        val homeName = args[1]

        if (manager.getHome(townName, homeName) == null) {
            player.sendMessage(serializer.deserialize(
                manager.getMessage("not_found", "name" to homeName)
            ))
            return
        }

        if (manager.removeHome(townName, homeName)) {
            player.sendMessage(serializer.deserialize(
                manager.getMessage("del_success", "name" to homeName)
            ))
            // 通知小镇成员
            manager.notifyTownMembers(townName, player, homeName, false)
        } else {
            player.sendMessage(serializer.deserialize(manager.getMessage("del_failed")))
        }
    }

    private fun handleTeleport(player: Player, townName: String, homeName: String) {
        val result = manager.teleportToHome(player, townName, homeName)
        when (result) {
            TeleportResult.SUCCESS -> {
                player.sendMessage(serializer.deserialize(
                    manager.getMessage("teleport_success", "name" to homeName)
                ))
            }
            TeleportResult.NOT_FOUND -> {
                player.sendMessage(serializer.deserialize(
                    manager.getMessage("not_found", "name" to homeName)
                ))
            }
            TeleportResult.WORLD_NOT_FOUND -> {
                player.sendMessage(serializer.deserialize(manager.getMessage("world_not_found")))
            }
        }
    }

    private fun showHelp(player: Player) {
        val canManage = manager.canManage(player)
        player.sendMessage(serializer.deserialize("&6=== 小镇PHome帮助 ==="))
        player.sendMessage(serializer.deserialize("&e/tsl phome &7- 打开GUI界面"))
        player.sendMessage(serializer.deserialize("&e/tsl phome <名称> &7- 传送到PHome"))
        if (canManage) {
            player.sendMessage(serializer.deserialize("&c--- 管理命令 ---"))
            player.sendMessage(serializer.deserialize("&e/tsl phome set <名称> &7- 创建/覆盖PHome"))
            player.sendMessage(serializer.deserialize("&e/tsl phome del <名称> &7- 删除PHome"))
        }
    }

    override fun getDescription(): String {
        return "小镇共享PHome"
    }

    override fun tabComplete(sender: CommandSender, command: Command, label: String, args: Array<out String>): List<String> {
        if (!manager.isEnabled() || sender !is Player) return emptyList()

        val townName = manager.getPlayerTownName(sender)
        val isOp = sender.isOp

        // OP 没有小镇时仍可使用 gui 和 help
        if (townName == null) {
            return if (isOp && args.size == 1) {
                listOf("gui", "help").filter { it.startsWith(args[0], ignoreCase = true) }
            } else {
                emptyList()
            }
        }

        val canManage = manager.canManage(sender)

        return when (args.size) {
            1 -> {
                val suggestions = mutableListOf("gui", "help")
                if (canManage) {
                    suggestions.addAll(listOf("set", "del"))
                }
                // 添加已有的 phome 名称用于直接传送
                val homes = manager.getHomes(townName).keys.toList()
                suggestions.addAll(homes)
                suggestions.filter { it.startsWith(args[0], ignoreCase = true) }
            }
            2 -> {
                when (args[0].lowercase()) {
                    "del", "delete", "remove" -> {
                        manager.getHomes(townName).keys
                            .filter { it.startsWith(args[1], ignoreCase = true) }
                            .toList()
                    }
                    else -> emptyList()
                }
            }
            else -> emptyList()
        }
    }
}
