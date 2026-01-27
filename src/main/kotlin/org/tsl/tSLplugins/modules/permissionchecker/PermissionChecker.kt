package org.tsl.tSLplugins.modules.permissionchecker

import me.clip.placeholderapi.PlaceholderAPI
import net.luckperms.api.LuckPerms
import net.luckperms.api.model.user.User
import net.luckperms.api.node.Node
import org.bukkit.Bukkit
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.plugin.java.JavaPlugin

/**
 * 权限检测器
 * 支持多规则检测和自定义权限组修改方式
 */
class PermissionChecker(private val plugin: JavaPlugin) : Listener {

    private var luckPerms: LuckPerms? = null
    private var placeholderApiAvailable = false
    private val rules = mutableListOf<PermissionRule>()

    init {
        setupLuckPerms()
        checkPlaceholderAPI()
        loadRules()
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

    /**
     * 加载权限检测规则
     */
    private fun loadRules() {
        rules.clear()

        val rulesSection = plugin.config.getConfigurationSection("permission-checker.rules")
        if (rulesSection == null) {
            plugin.logger.warning("未找到权限检测规则配置 (permission-checker.rules)")
            return
        }

        var count = 0
        for (ruleKey in rulesSection.getKeys(false)) {
            val ruleSection = rulesSection.getConfigurationSection(ruleKey)
            if (ruleSection == null) {
                plugin.logger.warning("规则 '$ruleKey' 配置格式错误")
                continue
            }

            try {
                val rule = PermissionRule(
                    name = ruleKey,
                    variableName = ruleSection.getString("variable") ?: "",
                    expectedValue = ruleSection.getString("value") ?: "",
                    targetGroup = ruleSection.getString("target-group") ?: "",
                    mode = parseMode(ruleSection.getString("mode") ?: "set"),
                    executeCommands = ruleSection.getBoolean("execute-commands", false),
                    commands = ruleSection.getStringList("commands")
                )

                if (rule.variableName.isEmpty() || rule.expectedValue.isEmpty() || rule.targetGroup.isEmpty()) {
                    plugin.logger.warning("规则 '$ruleKey' 配置不完整，已跳过")
                    continue
                }

                rules.add(rule)
                count++
            } catch (e: Exception) {
                plugin.logger.warning("加载规则 '$ruleKey' 时发生错误: ${e.message}")
            }
        }

        plugin.logger.info("权限检测规则加载完成，共 $count 条规则")
    }

    private fun parseMode(modeStr: String): PermissionMode {
        return when (modeStr.lowercase()) {
            "set", "replace", "覆盖" -> PermissionMode.SET
            "add", "append", "添加" -> PermissionMode.ADD
            else -> {
                plugin.logger.warning("未知的权限修改模式: $modeStr，使用默认模式 SET")
                PermissionMode.SET
            }
        }
    }

    /**
     * 重新加载配置
     */
    fun reload() {
        loadRules()
    }

    /**
     * 手动触发权限检查（供外部调用）
     * 用于绑定状态变更后立即检查权限
     */
    fun checkPlayer(player: Player) {
        if (!plugin.config.getBoolean("permission-checker.enabled", true)) {
            return
        }

        val lp = luckPerms ?: return

        plugin.server.asyncScheduler.runNow(plugin) {
            try {
                checkAndUpdatePermissionAsync(player, lp)
            } catch (e: Exception) {
                plugin.logger.severe("检查玩家 ${player.name} 的权限时发生错误: ${e.message}")
            }
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
            return
        }

        // 延迟检查，确保玩家数据完全加载（使用实体调度器以兼容 Folia）
        // 增加延迟到 40 tick (2秒)，避免与其他模块冲突
        player.scheduler.runDelayed(plugin, { _ ->
            if (!player.isOnline) return@runDelayed

            // 使用异步执行权限检查，避免阻塞主线程
            plugin.server.asyncScheduler.runNow(plugin) {
                try {
                    checkAndUpdatePermissionAsync(player, lp)
                } catch (e: Exception) {
                    plugin.logger.severe("检查玩家 ${player.name} 的权限时发生错误: ${e.message}")
                    e.printStackTrace()
                }
            }
        }, null, 40L) // 延迟 2 秒
    }

    private fun checkAndUpdatePermission(player: Player, lp: LuckPerms) {
        // 获取玩家的 LuckPerms User 对象
        val user = lp.userManager.getUser(player.uniqueId)
        if (user == null) {
            plugin.logger.warning("无法获取玩家 ${player.name} 的 LuckPerms 用户数据。")
            return
        }

        // 遍历所有规则，检查是否匹配
        for (rule in rules) {
            if (checkRule(player, user, rule, lp)) {
                // 规则匹配，执行权限组修改
                plugin.logger.info("玩家 ${player.name} 匹配规则 '${rule.name}'，应用权限组修改")
                applyRule(player, user, rule, lp)

                // 只应用第一个匹配的规则
                break
            }
        }
    }

    /**
     * 异步权限检查（避免阻塞主线程）
     */
    private fun checkAndUpdatePermissionAsync(player: Player, lp: LuckPerms) {
        // 异步获取玩家的 LuckPerms User 对象
        lp.userManager.loadUser(player.uniqueId).thenAcceptAsync { user ->
            if (user == null) {
                plugin.logger.warning("无法获取玩家 ${player.name} 的 LuckPerms 用户数据。")
                return@thenAcceptAsync
            }

            // 遍历所有规则，检查是否匹配
            for (rule in rules) {
                if (checkRuleAsync(player, user, rule)) {
                    // 规则匹配，在主线程执行权限组修改
                    plugin.server.globalRegionScheduler.run(plugin) { _ ->
                        if (player.isOnline) {
                            plugin.logger.info("玩家 ${player.name} 匹配规则 '${rule.name}'，应用权限组修改")
                            applyRule(player, user, rule, lp)
                        }
                    }
                    // 只应用第一个匹配的规则
                    break
                }
            }
        }
    }

    /**
     * 异步检查规则是否匹配（不记录过多日志）
     */
    private fun checkRuleAsync(player: Player, user: User, rule: PermissionRule): Boolean {
        // 检查变量值
        if (!checkVariableQuiet(player, rule.variableName, rule.expectedValue)) {
            return false
        }

        // 检查玩家是否已在目标权限组
        if (isInGroup(user, rule.targetGroup)) {
            // 已在组中，跳过（不记录日志，避免刷屏）
            return false
        }

        return true
    }

    /**
     * 静默检查变量（不记录详细日志）
     */
    private fun checkVariableQuiet(player: Player, variableName: String, expectedValue: String): Boolean {
        if (!placeholderApiAvailable) {
            return false
        }

        val actualValue = PlaceholderAPI.setPlaceholders(player, variableName)
        return actualValue.equals(expectedValue, ignoreCase = true)
    }

    /**
     * 检查规则是否匹配
     */
    private fun checkRule(player: Player, user: User, rule: PermissionRule, lp: LuckPerms): Boolean {
        // 检查变量值
        if (!checkVariable(player, rule.variableName, rule.expectedValue)) {
            return false
        }

        // 检查玩家是否已在目标权限组（如果是 SET 模式）
        if (rule.mode == PermissionMode.SET) {
            if (isInGroup(user, rule.targetGroup)) {
                plugin.logger.info("玩家 ${player.name} 已在权限组 '${rule.targetGroup}' 中，跳过规则 '${rule.name}'")
                return false
            }
        } else if (rule.mode == PermissionMode.ADD) {
            // ADD 模式下，如果已经有该组，也跳过
            if (isInGroup(user, rule.targetGroup)) {
                plugin.logger.info("玩家 ${player.name} 已拥有权限组 '${rule.targetGroup}'，跳过规则 '${rule.name}'")
                return false
            }
        }

        return true
    }

    /**
     * 应用规则
     */
    private fun applyRule(player: Player, user: User, rule: PermissionRule, lp: LuckPerms) {
        when (rule.mode) {
            PermissionMode.SET -> setGroup(user, rule.targetGroup, lp)
            PermissionMode.ADD -> addGroup(user, rule.targetGroup, lp)
        }

        // 执行命令（如果启用）
        if (rule.executeCommands && rule.commands.isNotEmpty()) {
            executeCommands(player, rule.commands)
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

    private fun checkVariable(player: Player, variableName: String, expectedValue: String): Boolean {
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

    /**
     * SET 模式：覆盖权限组（删除所有现有组，设置为目标组）
     */
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
        user.data().add(newGroupNode)

        // 保存用户数据
        lp.userManager.saveUser(user)

        plugin.logger.info("已将玩家 ${user.username ?: "Unknown"} 的权限组设置为 '$groupName'（SET模式）。")

        // 触发权限重算
        triggerPermissionRecalculation(user, lp)
    }

    /**
     * ADD 模式：添加权限组（保留现有组，添加新组）
     */
    private fun addGroup(user: User, groupName: String, lp: LuckPerms) {
        // 创建新的权限组节点
        val newGroupNode = Node.builder("group.$groupName").build()
        user.data().add(newGroupNode)

        // 保存用户数据
        lp.userManager.saveUser(user)

        plugin.logger.info("已将权限组 '$groupName' 添加到玩家 ${user.username ?: "Unknown"}（ADD模式）。")

        // 触发权限重算
        triggerPermissionRecalculation(user, lp)
    }

    /**
     * 触发权限重算，使其他模块能实时响应
     */
    private fun triggerPermissionRecalculation(user: User, lp: LuckPerms) {
        val player = Bukkit.getPlayer(user.uniqueId)
        if (player != null && player.isOnline) {
            player.scheduler.runDelayed(plugin, { _ ->
                if (player.isOnline) {
                    lp.userManager.loadUser(user.uniqueId)
                    plugin.logger.info("已触发玩家 ${player.name} 的权限重算事件")
                }
            }, null, 5L)
        }
    }

    private fun executeCommands(player: Player, commands: List<String>) {
        if (commands.isEmpty()) return

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

    /**
     * 权限检测规则数据类
     */
    data class PermissionRule(
        val name: String,
        val variableName: String,
        val expectedValue: String,
        val targetGroup: String,
        val mode: PermissionMode,
        val executeCommands: Boolean,
        val commands: List<String>
    )

    /**
     * 权限修改模式
     */
    enum class PermissionMode {
        SET,  // 覆盖模式：删除所有现有组，设置为目标组
        ADD   // 添加模式：保留现有组，添加新组
    }
}
