package org.tsl.tSLplugins.modules.near

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.tsl.tSLplugins.SubCommandHandler
import org.tsl.tSLplugins.core.AbstractModule
import kotlin.math.roundToInt

/**
 * Near 模块 - 附近玩家查询
 * 
 * 查找并显示附近的玩家及距离
 * 
 * ## 功能
 * - 查找指定范围内的玩家
 * - 按距离排序显示
 * 
 * ## 命令
 * - `/tsl near [范围]` - 查找附近的玩家
 * 
 * ## 权限
 * - `tsl.near.use` - 使用附近玩家查询
 * - `tsl.near.bypass` - 无视范围限制
 */
class NearModule : AbstractModule() {

    override val id = "near"
    override val configPath = "near"

    // 配置项
    private var defaultRadius: Int = 1000
    private var maxRadius: Int = 1000

    override fun doEnable() {
        loadNearConfig()
    }

    override fun doDisable() {
        // 无需清理
    }

    override fun doReload() {
        loadNearConfig()
    }

    override fun getCommandHandler(): SubCommandHandler = NearModuleCommand(this)
    
    override fun getDescription(): String = "附近玩家查询"

    /**
     * 加载配置
     */
    private fun loadNearConfig() {
        defaultRadius = getConfigInt("defaultRadius", 1000)
        maxRadius = getConfigInt("maxRadius", 1000)
    }

    // ============== 公开 API ==============

    /**
     * 获取默认搜索半径
     */
    fun getDefaultRadius(): Int = defaultRadius

    /**
     * 获取最大搜索半径
     */
    fun getMaxRadius(): Int = maxRadius

    /**
     * 查找附近的玩家
     */
    fun findNearbyPlayers(player: Player, radius: Int): List<Pair<Player, Double>> {
        val playerLocation = player.location
        val radiusSquared = radius * radius.toDouble()

        return player.world.players
            .filter { other ->
                if (other.uniqueId == player.uniqueId) return@filter false
                val distanceSquared = playerLocation.distanceSquared(other.location)
                distanceSquared <= radiusSquared
            }
            .map { other ->
                val distance = playerLocation.distance(other.location)
                Pair(other, distance)
            }
            .sortedBy { it.second }
    }

    /**
     * 格式化距离显示
     */
    fun formatDistance(distance: Double): String {
        return "${distance.roundToInt()} 米"
    }

    /**
     * 获取插件实例
     */
    fun getPlugin() = context.plugin
}

/**
 * Near 命令处理器
 */
class NearModuleCommand(private val module: NearModule) : SubCommandHandler {

    override fun handle(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (!module.isEnabled()) {
            sender.sendMessage(Component.text("附近玩家功能未启用！", NamedTextColor.RED))
            return true
        }

        if (sender !is Player) {
            sender.sendMessage(Component.text("此命令只能由玩家使用！", NamedTextColor.RED))
            return true
        }

        if (!sender.hasPermission("tsl.near.use")) {
            sender.sendMessage(Component.text("你没有权限使用此命令！", NamedTextColor.RED))
            return true
        }

        // 解析半径参数
        val radius = if (args.isEmpty()) {
            module.getDefaultRadius()
        } else {
            try {
                args[0].toInt()
            } catch (e: NumberFormatException) {
                sender.sendMessage(
                    Component.text("无效的范围！", NamedTextColor.RED)
                        .append(Component.text(" 请输入一个数字。", NamedTextColor.GRAY))
                )
                return true
            }
        }

        // 检查半径限制
        val actualRadius = if (sender.hasPermission("tsl.near.bypass")) {
            radius
        } else {
            val max = module.getMaxRadius()
            if (radius > max) {
                sender.sendMessage(
                    Component.text("范围过大！", NamedTextColor.RED)
                        .append(Component.text(" 最大范围: $max 米", NamedTextColor.GRAY))
                )
                return true
            }
            radius
        }

        if (actualRadius <= 0) {
            sender.sendMessage(Component.text("范围必须大于 0！", NamedTextColor.RED))
            return true
        }

        // 使用 Folia 玩家调度器执行查询
        sender.scheduler.run(module.getPlugin(), { _ ->
            try {
                val nearbyPlayers = module.findNearbyPlayers(sender, actualRadius)

                if (nearbyPlayers.isEmpty()) {
                    sender.sendMessage(
                        Component.text("附近 $actualRadius 米内没有其他玩家", NamedTextColor.YELLOW)
                    )
                } else {
                    sender.sendMessage(
                        Component.text("========== ", NamedTextColor.GRAY)
                            .append(Component.text("附近玩家", NamedTextColor.GOLD, TextDecoration.BOLD))
                            .append(Component.text(" ==========", NamedTextColor.GRAY))
                    )
                    sender.sendMessage(
                        Component.text("搜索范围: ", NamedTextColor.YELLOW)
                            .append(Component.text("$actualRadius 米", NamedTextColor.GREEN))
                    )
                    sender.sendMessage(Component.empty())

                    nearbyPlayers.forEachIndexed { index, (player, distance) ->
                        val distanceStr = module.formatDistance(distance)
                        sender.sendMessage(
                            Component.text("${index + 1}. ", NamedTextColor.GRAY)
                                .append(Component.text(player.name, NamedTextColor.WHITE))
                                .append(Component.text(" ~ ", NamedTextColor.DARK_GRAY))
                                .append(Component.text(distanceStr, NamedTextColor.AQUA))
                        )
                    }

                    sender.sendMessage(Component.empty())
                    sender.sendMessage(Component.text("共 ${nearbyPlayers.size} 名玩家", NamedTextColor.GRAY))
                    sender.sendMessage(Component.text("====================================", NamedTextColor.GRAY))
                }
            } catch (e: Exception) {
                sender.sendMessage(Component.text("查询失败: ${e.message}", NamedTextColor.RED))
            }
        }, null)

        return true
    }

    override fun tabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): List<String> {
        if (sender !is Player) return emptyList()
        if (!sender.hasPermission("tsl.near.use")) return emptyList()

        return when (args.size) {
            1 -> listOf("50", "100", "500", "1000").filter { it.startsWith(args[0], ignoreCase = true) }
            else -> emptyList()
        }
    }

    override fun getDescription(): String = "附近玩家查询"
}
