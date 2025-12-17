package org.tsl.tSLplugins.Mcedia

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.tsl.tSLplugins.SubCommandHandler

/**
 * Mcedia 命令处理器
 * 处理 /tsl mcedia 相关命令
 */
class McediaCommand(
    private val manager: McediaManager,
    private val gui: McediaGUI
) : SubCommandHandler {

    private val serializer = LegacyComponentSerializer.legacyAmpersand()

    override fun handle(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        // 检查功能是否启用
        if (!manager.isEnabled()) {
            sender.sendMessage(serializer.deserialize(manager.getMessage("disabled")))
            return true
        }

        if (args.isEmpty()) {
            // 如果是玩家，打开 GUI
            if (sender is Player) {
                if (!sender.hasPermission("tsl.mcedia.use")) {
                    sender.sendMessage(serializer.deserialize("&c你没有权限使用此命令！"))
                    return true
                }
                gui.openMainMenu(sender)
            } else {
                showUsage(sender)
            }
            return true
        }

        when (args[0].lowercase()) {
            "create" -> handleCreate(sender, args.drop(1).toTypedArray())
            "delete", "remove" -> handleDelete(sender, args.drop(1).toTypedArray())
            "list" -> handleList(sender)
            "scan" -> handleScan(sender)
            "set" -> handleSet(sender, args.drop(1).toTypedArray())
            "tp", "teleport" -> handleTeleport(sender, args.drop(1).toTypedArray())
            "gui", "menu" -> handleGUI(sender)
            "help" -> showUsage(sender)
            else -> showUsage(sender)
        }

        return true
    }

    /**
     * 创建播放器
     * /tsl mcedia create <name>
     */
    private fun handleCreate(sender: CommandSender, args: Array<out String>) {
        if (!sender.hasPermission("tsl.mcedia.create")) {
            sender.sendMessage(serializer.deserialize("&c你没有权限使用此命令！"))
            return
        }

        if (sender !is Player) {
            sender.sendMessage(serializer.deserialize("&c此命令只能由玩家使用！"))
            return
        }

        if (args.isEmpty()) {
            sender.sendMessage(serializer.deserialize("&c用法: /tsl mcedia create <名称>"))
            return
        }

        val name = args.joinToString(" ")

        sender.scheduler.run(manager.getPlugin(), { _ ->
            val player = manager.createPlayerSync(sender.location, name, sender.uniqueId)
            if (player != null) {
                sender.sendMessage(serializer.deserialize("&a播放器创建成功！名称: &f${player.name}"))
            } else {
                sender.sendMessage(serializer.deserialize("&c创建失败！可能已达到最大数量限制。"))
            }
        }, null)
    }

    /**
     * 删除播放器
     * /tsl mcedia delete <name>
     */
    private fun handleDelete(sender: CommandSender, args: Array<out String>) {
        if (!sender.hasPermission("tsl.mcedia.delete")) {
            sender.sendMessage(serializer.deserialize("&c你没有权限使用此命令！"))
            return
        }

        if (args.isEmpty()) {
            sender.sendMessage(serializer.deserialize("&c用法: /tsl mcedia delete <名称>"))
            return
        }

        val name = args.joinToString(" ")
        val player = manager.findPlayerByName(name)

        if (player == null) {
            sender.sendMessage(serializer.deserialize("&c未找到播放器: &e$name"))
            return
        }

        if (manager.deletePlayer(player.uuid)) {
            sender.sendMessage(serializer.deserialize("&a播放器已删除: &f${player.name}"))
        } else {
            sender.sendMessage(serializer.deserialize("&c删除失败！"))
        }
    }

    /**
     * 列出所有播放器
     * /tsl mcedia list
     */
    private fun handleList(sender: CommandSender) {
        if (!sender.hasPermission("tsl.mcedia.list")) {
            sender.sendMessage(serializer.deserialize("&c你没有权限使用此命令！"))
            return
        }

        val players = manager.getAllPlayers()

        if (players.isEmpty()) {
            sender.sendMessage(serializer.deserialize("&7当前没有任何播放器"))
            return
        }

        sender.sendMessage(serializer.deserialize("&6&l===== Mcedia 播放器列表 (${players.size}) ====="))

        players.forEach { player ->
            val platform = VideoPlatform.detect(player.videoUrl)
            val videoStatus = if (player.videoUrl.isEmpty()) "&c未设置" else "&a${platform.displayName}"
            sender.sendMessage(serializer.deserialize(
                "&7- &f${player.name} &7| $videoStatus &7| &8${player.location.blockX}, ${player.location.blockY}, ${player.location.blockZ}"
            ))
        }
    }

    /**
     * 扫描现有播放器
     * /tsl mcedia scan
     */
    private fun handleScan(sender: CommandSender) {
        if (!sender.hasPermission("tsl.mcedia.scan")) {
            sender.sendMessage(serializer.deserialize("&c你没有权限使用此命令！"))
            return
        }

        sender.sendMessage(serializer.deserialize("&7正在扫描..."))
        manager.scanExistingPlayers()
        sender.sendMessage(serializer.deserialize("&a扫描完成！当前共 &f${manager.getAllPlayers().size} &a个播放器"))
    }

    /**
     * 设置视频
     * /tsl mcedia set <name> <url>
     */
    private fun handleSet(sender: CommandSender, args: Array<out String>) {
        if (!sender.hasPermission("tsl.mcedia.set")) {
            sender.sendMessage(serializer.deserialize("&c你没有权限使用此命令！"))
            return
        }

        if (args.size < 2) {
            sender.sendMessage(serializer.deserialize("&c用法: /tsl mcedia set <名称> <视频链接>"))
            return
        }

        val name = args[0]
        val url = args.drop(1).joinToString(" ")

        val player = manager.findPlayerByName(name)
        if (player == null) {
            sender.sendMessage(serializer.deserialize("&c未找到播放器: &e$name"))
            return
        }

        if (manager.setVideo(player.uuid, url)) {
            val platform = VideoPlatform.detect(url)
            sender.sendMessage(serializer.deserialize("&a视频已设置！平台: &f${platform.displayName}"))
        } else {
            sender.sendMessage(serializer.deserialize("&c设置失败！"))
        }
    }

    /**
     * 传送到播放器
     * /tsl mcedia tp <name>
     */
    private fun handleTeleport(sender: CommandSender, args: Array<out String>) {
        if (sender !is Player) {
            sender.sendMessage(serializer.deserialize("&c此命令只能由玩家使用！"))
            return
        }

        if (!sender.hasPermission("tsl.mcedia.teleport")) {
            sender.sendMessage(serializer.deserialize("&c你没有权限使用此命令！"))
            return
        }

        if (args.isEmpty()) {
            sender.sendMessage(serializer.deserialize("&c用法: /tsl mcedia tp <名称>"))
            return
        }

        val name = args.joinToString(" ")
        val player = manager.findPlayerByName(name)

        if (player == null) {
            sender.sendMessage(serializer.deserialize("&c未找到播放器: &e$name"))
            return
        }

        // 使用 teleportAsync 进行 Folia 兼容的异步传送
        sender.teleportAsync(player.location).thenAccept { success ->
            if (success) {
                sender.sendMessage(serializer.deserialize("&a已传送到播放器: &f${player.name}"))
            } else {
                sender.sendMessage(serializer.deserialize("&c传送失败！"))
            }
        }
    }

    /**
     * 打开 GUI
     */
    private fun handleGUI(sender: CommandSender) {
        if (sender !is Player) {
            sender.sendMessage(serializer.deserialize("&c此命令只能由玩家使用！"))
            return
        }

        if (!sender.hasPermission("tsl.mcedia.use")) {
            sender.sendMessage(serializer.deserialize("&c你没有权限使用此命令！"))
            return
        }

        gui.openMainMenu(sender)
    }

    /**
     * 显示用法
     */
    private fun showUsage(sender: CommandSender) {
        sender.sendMessage(serializer.deserialize("&6&l===== Mcedia 视频播放器命令 ====="))
        sender.sendMessage(serializer.deserialize("&e/tsl mcedia &7- 打开 GUI 菜单"))
        sender.sendMessage(serializer.deserialize("&e/tsl mcedia create <名称> &7- 创建播放器"))
        sender.sendMessage(serializer.deserialize("&e/tsl mcedia delete <名称> &7- 删除播放器"))
        sender.sendMessage(serializer.deserialize("&e/tsl mcedia list &7- 列出所有播放器"))
        sender.sendMessage(serializer.deserialize("&e/tsl mcedia set <名称> <链接> &7- 设置视频"))
        sender.sendMessage(serializer.deserialize("&e/tsl mcedia tp <名称> &7- 传送到播放器"))
        sender.sendMessage(serializer.deserialize("&e/tsl mcedia scan &7- 扫描现有播放器"))
        sender.sendMessage(serializer.deserialize(""))
        sender.sendMessage(serializer.deserialize("&7提示: 潜行右键点击播放器可直接打开编辑界面"))
    }

    override fun tabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): List<String> {
        if (!manager.isEnabled()) return emptyList()

        return when (args.size) {
            1 -> {
                listOf("create", "delete", "list", "set", "tp", "scan", "gui", "help")
                    .filter { it.startsWith(args[0], ignoreCase = true) }
            }
            2 -> {
                when (args[0].lowercase()) {
                    "delete", "set", "tp" -> {
                        manager.getAllPlayers()
                            .map { it.name }
                            .filter { it.startsWith(args[1], ignoreCase = true) }
                    }
                    else -> emptyList()
                }
            }
            3 -> {
                when (args[0].lowercase()) {
                    "set" -> {
                        // 视频链接提示
                        listOf(
                            "https://www.bilibili.com/video/",
                            "https://live.bilibili.com/",
                            "https://b23.tv/"
                        ).filter { it.startsWith(args[2], ignoreCase = true) }
                    }
                    else -> emptyList()
                }
            }
            else -> emptyList()
        }
    }

    override fun getDescription(): String {
        return "Mcedia 视频播放器管理"
    }
}

