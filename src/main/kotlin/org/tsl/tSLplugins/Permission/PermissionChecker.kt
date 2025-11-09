package org.tsl.tSLplugins.Permission

import me.clip.placeholderapi.PlaceholderAPI
import net.luckperms.api.LuckPerms
import net.luckperms.api.model.user.User
import net.luckperms.api.node.Node
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.plugin.java.JavaPlugin

class PermissionChecker(private val plugin: JavaPlugin) : Listener {

    private var luckPerms: LuckPerms? = null
    private var placeholderApiAvailable = false

    init {
        setupLuckPerms()
        checkPlaceholderAPI()
    }

    private fun setupLuckPerms() {
        val provider = Bukkit.getServicesManager().getRegistration(LuckPerms::class.java)
        if (provider != null) {
            luckPerms = provider.provider
            plugin.logger.info("LuckPerms 集成成功！")
        } else {
            plugin.logger.warning("未找到 LuckPerms！权限检测功能将不可用。")
        }
    }

    private fun checkPlaceholderAPI() {
        placeholderApiAvailable = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null
        if (placeholderApiAvailable) {
            plugin.logger.info("PlaceholderAPI 已加载，变量解析可用。")
        } else {
            plugin.logger.warning("未找到 PlaceholderAPI！变量解析功能将不可用。")
        }
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player

        // 检查功能是否启用
        if (!plugin.config.getBoolean("permission-checker.enabled", true)) {
            return
        }

        // 检查 LuckPerms 是否可用
        val lp = luckPerms
        if (lp == null) {
            plugin.logger.warning("LuckPerms 不可用，无法执行权限检测。")
            return
        }

        // 延迟检查，确保玩家数据完全加载（使用实体调度器以兼容 Folia）
        player.scheduler.runDelayed(plugin, { _ ->
            if (!player.isOnline) return@runDelayed

            try {
                checkAndUpdatePermission(player, lp)
            } catch (e: Exception) {
                plugin.logger.severe("检查玩家 ${player.name} 的权限时发生错误: ${e.message}")
                e.printStackTrace()
            }
        }, null, 20L) // 延迟 1 秒
    }

    private fun checkAndUpdatePermission(player: Player, lp: LuckPerms) {
        val targetGroup = plugin.config.getString("permission-checker.target-group", "normal") ?: "normal"

        // 获取玩家的 LuckPerms User 对象
        val user = lp.userManager.getUser(player.uniqueId)
        if (user == null) {
            plugin.logger.warning("无法获取玩家 ${player.name} 的 LuckPerms 用户数据。")
            return
        }

        // 检查玩家是否已在目标权限组
        if (isInGroup(user, targetGroup)) {
            plugin.logger.info("玩家 ${player.name} 已在权限组 '$targetGroup' 中，无需操作。")
            return
        }

        plugin.logger.info("玩家 ${player.name} 不在权限组 '$targetGroup' 中，检查变量...")

        // 检查变量
        if (!checkVariable(player)) {
            plugin.logger.info("玩家 ${player.name} 的变量检查未通过，不执行权限组修改。")
            return
        }

        // 变量检查通过，执行权限组修改
        plugin.logger.info("玩家 ${player.name} 的变量检查通过，设置权限组为 '$targetGroup'。")
        setGroup(user, targetGroup, lp)

        // 检查是否需要执行命令
        if (plugin.config.getBoolean("permission-checker.execute-command", false)) {
            executeCommands(player)
        }
    }

    private fun isInGroup(user: User, groupName: String): Boolean {
        // 检查主权限组
        if (user.primaryGroup.equals(groupName, ignoreCase = true)) {
            return true
        }

        // 检查继承的权限组
        val groups = user.nodes.stream()
            .filter { it.key.startsWith("group.") }
            .map { it.key.substring(6) }
            .toList()

        return groups.any { it.equals(groupName, ignoreCase = true) }
    }

    private fun checkVariable(player: Player): Boolean {
        val variableName = plugin.config.getString("permission-checker.variable-name", "%player_gamemode%")
            ?: "%player_gamemode%"
        val expectedValue = plugin.config.getString("permission-checker.variable-value", "SURVIVAL")
            ?: "SURVIVAL"

        // 如果 PlaceholderAPI 不可用，直接返回 false
        if (!placeholderApiAvailable) {
            plugin.logger.warning("PlaceholderAPI 不可用，无法解析变量 '$variableName'。")
            return false
        }

        // 解析变量
        val actualValue = PlaceholderAPI.setPlaceholders(player, variableName)

        plugin.logger.info("玩家 ${player.name} 的变量 '$variableName' = '$actualValue'，期望值 = '$expectedValue'")

        return actualValue.equals(expectedValue, ignoreCase = true)
    }

    private fun setGroup(user: User, groupName: String, lp: LuckPerms) {
        // 移除所有现有的权限组节点
        val groupNodes = user.nodes.stream()
            .filter { it.key.startsWith("group.") }
            .toList()

        for (node in groupNodes) {
            user.data().remove(node)
        }

        plugin.logger.info("已清除玩家 ${user.username ?: "Unknown"} 的所有权限组。")

        // 创建新的权限组节点
        val newGroupNode = Node.builder("group.$groupName").build()

        // 设置新的权限组（添加到用户）
        user.data().add(newGroupNode)

        // 保存用户数据
        lp.userManager.saveUser(user)

        plugin.logger.info("已将玩家 ${user.username ?: "Unknown"} 的权限组设置为 '$groupName'。")
    }

    private fun executeCommands(player: Player) {
        val commands = plugin.config.getStringList("permission-checker.commands")

        if (commands.isEmpty()) {
            plugin.logger.info("未配置执行命令，跳过命令执行。")
            return
        }

        // 使用全局调度器执行命令（以兼容 Folia）
        Bukkit.getGlobalRegionScheduler().run(plugin) { _ ->
            for (command in commands) {
                val finalCommand = command.replace("%player%", player.name)
                try {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand)
                    plugin.logger.info("执行命令: $finalCommand")
                } catch (e: Exception) {
                    plugin.logger.warning("执行命令 '$finalCommand' 时发生错误: ${e.message}")
                }
            }
        }
    }
}

