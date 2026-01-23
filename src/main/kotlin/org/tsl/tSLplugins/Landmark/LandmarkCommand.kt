package org.tsl.tSLplugins.Landmark

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.tsl.tSLplugins.SubCommandHandler

/**
 * 地标系统命令处理器
 * 处理 /tsl landmark 命令
 */
class LandmarkCommand(
    private val plugin: JavaPlugin,
    private val manager: LandmarkManager,
    private val gui: LandmarkGUI,
    private val opkTool: LandmarkOPKTool
) : SubCommandHandler {

    private val serializer = LegacyComponentSerializer.legacyAmpersand()

    override fun handle(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!manager.isEnabled()) {
            sender.sendMessage(serializer.deserialize(manager.getMessage("disabled")))
            return true
        }

        if (sender !is Player) {
            sender.sendMessage(serializer.deserialize(manager.getMessage("player_only")))
            return true
        }

        if (args.isEmpty()) {
            // 默认打开 GUI
            gui.openMainMenu(sender)
            return true
        }

        when (args[0].lowercase()) {
            "gui" -> handleGui(sender)
            "opk" -> handleOPK(sender)
            "create" -> handleCreate(sender, args)
            "delete" -> handleDelete(sender, args)
            "edit" -> handleEdit(sender, args)
            "trust" -> handleTrust(sender, args)
            "untrust" -> handleUntrust(sender, args)
            "tp" -> handleTeleport(sender, args)
            "list" -> handleList(sender)
            "info" -> handleInfo(sender, args)
            "setwarp" -> handleSetWarp(sender, args)
            "help" -> showHelp(sender)
            else -> showHelp(sender)
        }

        return true
    }

    private fun handleGui(player: Player) {
        gui.openMainMenu(player)
    }

    private fun handleOPK(player: Player) {
        if (!player.hasPermission("tsl.landmark.admin")) {
            player.sendMessage(serializer.deserialize(manager.getMessage("no_permission")))
            return
        }
        opkTool.giveOPKTool(player)
    }

    private fun handleCreate(player: Player, args: Array<out String>) {
        if (!player.hasPermission("tsl.landmark.admin")) {
            player.sendMessage(serializer.deserialize(manager.getMessage("no_permission")))
            return
        }

        if (args.size < 2) {
            player.sendMessage(serializer.deserialize(manager.getMessage("usage_create")))
            return
        }

        val name = args[1]
        if (manager.storage.landmarkExists(name)) {
            player.sendMessage(serializer.deserialize(
                manager.getMessage("landmark_exists", "name" to name)
            ))
            return
        }

        // 从 OPK 工具获取已选择的点
        val selection = opkTool.getSelection(player.uniqueId)
        val pos1 = selection.first
        val pos2 = selection.second
        val world = opkTool.getSelectionWorld(player.uniqueId)

        if (pos1 == null || pos2 == null || world == null) {
            player.sendMessage(serializer.deserialize(manager.getMessage("no_selection")))
            return
        }

        // 创建区域
        val region = LandmarkRegion.fromTwoPoints(pos1.first, pos1.second, pos2.first, pos2.second)

        if (region.volume() > manager.maxRegionVolume) {
            player.sendMessage(serializer.deserialize(manager.getMessage("creation_failed_volume")))
            return
        }

        // 创建地标
        val landmark = manager.createLandmark(name, world, region)
        if (landmark != null) {
            // 清除选区
            opkTool.clearSelection(player.uniqueId)
            player.sendMessage(serializer.deserialize(
                manager.getMessage("creation_success",
                    "name" to landmark.name,
                    "volume" to landmark.region.volume().toString())
            ))
        } else {
            player.sendMessage(serializer.deserialize(manager.getMessage("creation_failed")))
        }
    }

    private fun handleDelete(player: Player, args: Array<out String>) {
        if (!player.hasPermission("tsl.landmark.admin")) {
            player.sendMessage(serializer.deserialize(manager.getMessage("no_permission")))
            return
        }

        if (args.size < 2) {
            player.sendMessage(serializer.deserialize(manager.getMessage("usage_delete")))
            return
        }

        val name = args[1]
        val confirm = args.size >= 3 && args[2].equals("confirm", ignoreCase = true)

        val landmark = manager.getLandmark(name)
        if (landmark == null) {
            player.sendMessage(serializer.deserialize(
                manager.getMessage("landmark_not_found", "name" to name)
            ))
            return
        }

        if (!confirm) {
            player.sendMessage(serializer.deserialize(
                manager.getMessage("delete_confirm", "name" to name)
            ))
            return
        }

        if (manager.deleteLandmark(name)) {
            player.sendMessage(serializer.deserialize(
                manager.getMessage("delete_success", "name" to name)
            ))
        } else {
            player.sendMessage(serializer.deserialize(manager.getMessage("delete_failed")))
        }
    }

    private fun handleEdit(player: Player, args: Array<out String>) {
        if (args.size < 2) {
            player.sendMessage(serializer.deserialize(manager.getMessage("usage_edit")))
            return
        }

        val name = args[1]
        val landmark = manager.getLandmark(name)
        if (landmark == null) {
            player.sendMessage(serializer.deserialize(
                manager.getMessage("landmark_not_found", "name" to name)
            ))
            return
        }

        val isAdmin = player.hasPermission("tsl.landmark.admin")
        val isMaintainer = manager.isMaintainer(player.uniqueId, landmark)

        if (!isAdmin && !isMaintainer) {
            player.sendMessage(serializer.deserialize(manager.getMessage("no_permission")))
            return
        }

        if (args.size < 4) {
            gui.openEditMenu(player, landmark)
            return
        }

        val field = args[2].lowercase()
        val value = args.drop(3).joinToString(" ")

        when (field) {
            "icon" -> {
                val material = Material.matchMaterial(value.uppercase())
                if (material == null) {
                    player.sendMessage(serializer.deserialize(manager.getMessage("invalid_material")))
                    return
                }
                landmark.icon = material.name
                manager.storage.updateLandmark(landmark)
                player.sendMessage(serializer.deserialize(
                    manager.getMessage("edit_success", "field" to "icon", "value" to value)
                ))
            }
            "lore" -> {
                landmark.lore = value.split("\\n").map { it.trim() }
                manager.storage.updateLandmark(landmark)
                player.sendMessage(serializer.deserialize(
                    manager.getMessage("edit_success", "field" to "lore", "value" to value)
                ))
            }
            "defaultunlocked" -> {
                if (!isAdmin) {
                    player.sendMessage(serializer.deserialize(manager.getMessage("no_permission")))
                    return
                }
                landmark.defaultUnlocked = value.equals("true", ignoreCase = true)
                manager.storage.updateLandmark(landmark)
                player.sendMessage(serializer.deserialize(
                    manager.getMessage("edit_success", "field" to "defaultUnlocked", "value" to landmark.defaultUnlocked.toString())
                ))
            }
            else -> {
                player.sendMessage(serializer.deserialize(manager.getMessage("invalid_field")))
            }
        }
    }

    private fun handleTrust(player: Player, args: Array<out String>) {
        if (!player.hasPermission("tsl.landmark.admin")) {
            player.sendMessage(serializer.deserialize(manager.getMessage("no_permission")))
            return
        }

        if (args.size < 3) {
            player.sendMessage(serializer.deserialize(manager.getMessage("usage_trust")))
            return
        }

        val landmarkName = args[1]
        val targetName = args[2]

        val landmark = manager.getLandmark(landmarkName)
        if (landmark == null) {
            player.sendMessage(serializer.deserialize(
                manager.getMessage("landmark_not_found", "name" to landmarkName)
            ))
            return
        }

        // 优先使用在线玩家（避免网络请求）
        val target = Bukkit.getPlayerExact(targetName)
            ?: Bukkit.getOfflinePlayerIfCached(targetName)
        if (target == null) {
            player.sendMessage(serializer.deserialize("&c玩家 $targetName 未找到（需曾经登录过服务器）"))
            return
        }
        if (manager.addMaintainer(landmark, target.uniqueId)) {
            player.sendMessage(serializer.deserialize(
                manager.getMessage("trust_success", "player" to targetName, "landmark" to landmarkName)
            ))
        } else {
            player.sendMessage(serializer.deserialize(
                manager.getMessage("trust_already", "player" to targetName)
            ))
        }
    }

    private fun handleUntrust(player: Player, args: Array<out String>) {
        if (!player.hasPermission("tsl.landmark.admin")) {
            player.sendMessage(serializer.deserialize(manager.getMessage("no_permission")))
            return
        }

        if (args.size < 3) {
            player.sendMessage(serializer.deserialize(manager.getMessage("usage_untrust")))
            return
        }

        val landmarkName = args[1]
        val targetName = args[2]

        val landmark = manager.getLandmark(landmarkName)
        if (landmark == null) {
            player.sendMessage(serializer.deserialize(
                manager.getMessage("landmark_not_found", "name" to landmarkName)
            ))
            return
        }

        // 优先使用在线玩家（避免网络请求）
        val target = Bukkit.getPlayerExact(targetName)
            ?: Bukkit.getOfflinePlayerIfCached(targetName)
        if (target == null) {
            player.sendMessage(serializer.deserialize("&c玩家 $targetName 未找到（需曾经登录过服务器）"))
            return
        }
        if (manager.removeMaintainer(landmark, target.uniqueId)) {
            player.sendMessage(serializer.deserialize(
                manager.getMessage("untrust_success", "player" to targetName, "landmark" to landmarkName)
            ))
        } else {
            player.sendMessage(serializer.deserialize(
                manager.getMessage("untrust_not_found", "player" to targetName)
            ))
        }
    }

    private fun handleTeleport(player: Player, args: Array<out String>) {
        if (!player.hasPermission("tsl.landmark.tp")) {
            player.sendMessage(serializer.deserialize(manager.getMessage("no_permission")))
            return
        }

        if (args.size < 2) {
            player.sendMessage(serializer.deserialize(manager.getMessage("usage_tp")))
            return
        }

        val name = args[1]
        val landmark = manager.getLandmark(name)
        if (landmark == null) {
            player.sendMessage(serializer.deserialize(
                manager.getMessage("landmark_not_found", "name" to name)
            ))
            return
        }

        val result = manager.canTeleport(player, landmark)
        when (result) {
            TeleportResult.NOT_IN_LANDMARK -> {
                player.sendMessage(serializer.deserialize(manager.getMessage("not_in_landmark")))
            }
            TeleportResult.NOT_UNLOCKED -> {
                player.sendMessage(serializer.deserialize(
                    manager.getMessage("not_unlocked", "landmark" to name)
                ))
            }
            TeleportResult.ON_COOLDOWN -> {
                player.sendMessage(serializer.deserialize(manager.getMessage("on_cooldown")))
            }
            TeleportResult.SUCCESS -> {
                if (manager.teleport(player, landmark)) {
                    if (manager.castTimeSeconds <= 0) {
                        player.sendMessage(serializer.deserialize(
                            manager.getMessage("teleport_success", "landmark" to name)
                        ))
                    } else {
                        player.sendMessage(serializer.deserialize(
                            manager.getMessage("teleport_started", "seconds" to manager.castTimeSeconds.toString())
                        ))
                    }
                } else {
                    player.sendMessage(serializer.deserialize(manager.getMessage("teleport_failed")))
                }
            }
            else -> {
                player.sendMessage(serializer.deserialize(manager.getMessage("teleport_failed")))
            }
        }
    }

    private fun handleList(player: Player) {
        val landmarks = manager.getAllLandmarks()
        if (landmarks.isEmpty()) {
            player.sendMessage(serializer.deserialize(manager.getMessage("no_landmarks")))
            return
        }

        player.sendMessage(serializer.deserialize(manager.getMessage("list_header")))
        landmarks.forEach { landmark ->
            val unlocked = if (manager.isUnlocked(player.uniqueId, landmark)) "&a✓" else "&c✗"
            val defaultTag = if (landmark.defaultUnlocked) " &7(默认解锁)" else ""
            player.sendMessage(serializer.deserialize(
                "&7- &f${landmark.name} $unlocked$defaultTag &7[${landmark.world}]"
            ))
        }
    }

    private fun handleInfo(player: Player, args: Array<out String>) {
        if (args.size < 2) {
            player.sendMessage(serializer.deserialize(manager.getMessage("usage_info")))
            return
        }

        val name = args[1]
        val landmark = manager.getLandmark(name)
        if (landmark == null) {
            player.sendMessage(serializer.deserialize(
                manager.getMessage("landmark_not_found", "name" to name)
            ))
            return
        }

        val unlocked = manager.isUnlocked(player.uniqueId, landmark)
        player.sendMessage(serializer.deserialize("&6=== 地标信息: ${landmark.name} ==="))
        player.sendMessage(serializer.deserialize("&7世界: &f${landmark.world}"))
        player.sendMessage(serializer.deserialize("&7区域: &f(${landmark.region.minX}, ${landmark.region.minY}, ${landmark.region.minZ}) - (${landmark.region.maxX}, ${landmark.region.maxY}, ${landmark.region.maxZ})"))
        player.sendMessage(serializer.deserialize("&7体积: &f${landmark.region.volume()}"))
        player.sendMessage(serializer.deserialize("&7已解锁: ${if (unlocked) "&a是" else "&c否"}"))
        player.sendMessage(serializer.deserialize("&7默认解锁: ${if (landmark.defaultUnlocked) "&a是" else "&c否"}"))
        if (landmark.lore.isNotEmpty()) {
            player.sendMessage(serializer.deserialize("&7描述: &f${landmark.lore.joinToString(" ")}"))
        }
    }

    private fun handleSetWarp(player: Player, args: Array<out String>) {
        if (args.size < 2) {
            player.sendMessage(serializer.deserialize(manager.getMessage("usage_setwarp")))
            return
        }

        val name = args[1]
        val landmark = manager.getLandmark(name)
        if (landmark == null) {
            player.sendMessage(serializer.deserialize(
                manager.getMessage("landmark_not_found", "name" to name)
            ))
            return
        }

        val isAdmin = player.hasPermission("tsl.landmark.admin")
        val isMaintainer = manager.isMaintainer(player.uniqueId, landmark)

        if (!isAdmin && !isMaintainer) {
            player.sendMessage(serializer.deserialize(manager.getMessage("no_permission")))
            return
        }

        val loc = player.location
        // 验证玩家位置是否在目标地标的区域范围内
        if (loc.world?.name != landmark.world || !landmark.region.contains(loc.x, loc.y, loc.z)) {
            player.sendMessage(serializer.deserialize(
                manager.getMessage("warp_not_in_region", "landmark" to name)
            ))
            return
        }

        landmark.warpPoint = WarpPoint(loc.x, loc.y, loc.z, loc.yaw, loc.pitch)
        manager.storage.updateLandmark(landmark)
        player.sendMessage(serializer.deserialize(
            manager.getMessage("warp_set", "landmark" to name)
        ))
    }

    private fun showHelp(player: Player) {
        player.sendMessage(serializer.deserialize("&6=== 地标系统帮助 ==="))
        player.sendMessage(serializer.deserialize("&e/lm &7- 打开地标菜单"))
        player.sendMessage(serializer.deserialize("&e/lm list &7- 列出所有地标"))
        player.sendMessage(serializer.deserialize("&e/lm tp <名称> &7- 传送到地标"))
        player.sendMessage(serializer.deserialize("&e/lm info <名称> &7- 查看地标信息"))
        if (player.hasPermission("tsl.landmark.admin")) {
            player.sendMessage(serializer.deserialize("&c--- 管理员命令 ---"))
            player.sendMessage(serializer.deserialize("&e/lm opk &7- 获取选区工具"))
            player.sendMessage(serializer.deserialize("&e/lm create <名称> &7- 创建地标 (需先用选区工具选点)"))
            player.sendMessage(serializer.deserialize("&e/lm delete <名称> confirm &7- 删除地标"))
            player.sendMessage(serializer.deserialize("&e/lm edit <名称> <字段> <值> &7- 编辑地标"))
            player.sendMessage(serializer.deserialize("&e/lm setwarp <名称> &7- 设置传送点"))
            player.sendMessage(serializer.deserialize("&e/lm trust <地标> <玩家> &7- 添加维护者"))
            player.sendMessage(serializer.deserialize("&e/lm untrust <地标> <玩家> &7- 移除维护者"))
        }
    }

    override fun getDescription(): String {
        return "地标系统"
    }

    override fun tabComplete(sender: CommandSender, command: Command, label: String, args: Array<out String>): List<String> {
        if (!manager.isEnabled() || sender !is Player) return emptyList()

        return when (args.size) {
            1 -> {
                val suggestions = mutableListOf("gui", "list", "tp", "info", "help")
                if (sender.hasPermission("tsl.landmark.admin")) {
                    suggestions.addAll(listOf("opk", "create", "delete", "edit", "setwarp", "trust", "untrust"))
                }
                suggestions.filter { it.startsWith(args[0], ignoreCase = true) }
            }
            2 -> {
                when (args[0].lowercase()) {
                    "tp", "info", "delete", "edit", "setwarp", "trust", "untrust" -> {
                        manager.getAllLandmarks().map { it.name }
                            .filter { it.startsWith(args[1], ignoreCase = true) }
                    }
                    else -> emptyList()
                }
            }
            3 -> {
                when (args[0].lowercase()) {
                    "delete" -> listOf("confirm").filter { it.startsWith(args[2], ignoreCase = true) }
                    "edit" -> listOf("icon", "lore", "defaultUnlocked").filter { it.startsWith(args[2], ignoreCase = true) }
                    "trust", "untrust" -> {
                        Bukkit.getOnlinePlayers().map { it.name }
                            .filter { it.startsWith(args[2], ignoreCase = true) }
                    }
                    else -> emptyList()
                }
            }
            else -> emptyList()
        }
    }
}
