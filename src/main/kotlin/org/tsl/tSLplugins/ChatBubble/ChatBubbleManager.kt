package org.tsl.tSLplugins.ChatBubble

import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.entity.Player
import org.bukkit.entity.TextDisplay
import org.bukkit.entity.Display
import org.bukkit.Location
import org.bukkit.Color
import org.bukkit.GameMode
import org.bukkit.potion.PotionEffectType
import net.kyori.adventure.text.Component
import java.util.concurrent.ConcurrentHashMap

/**
 * 聊天气泡管理器
 * 负责创建、更新和管理玩家头顶的聊天气泡
 */
class ChatBubbleManager(private val plugin: JavaPlugin) {

    // ===== 配置缓存 =====
    private var enabled: Boolean = true
    private var yOffset: Double = 0.75
    private var timeSpan: Int = 100
    private var billboard: Display.Billboard = Display.Billboard.VERTICAL
    private var shadow: Boolean = false
    private var viewRange: Float = 16.0f
    private var updateTicks: Long = 2L
    private var movementTicks: Int = 4
    private var defaultOpacity: Byte = (-1).toByte() // 255
    private var sneakingOpacity: Byte = 64 // 0.25 * 255
    private var useDefaultBackground: Boolean = true
    private var backgroundColor: Color = Color.fromARGB(0, 0, 0, 0)

    // ===== 运行时数据 =====
    // 玩家 -> 气泡实体
    private val bubbles: MutableMap<Player, TextDisplay> = ConcurrentHashMap()
    // 玩家是否可以看到自己的气泡
    private val selfDisplayEnabled: MutableSet<Player> = ConcurrentHashMap.newKeySet()

    init {
        loadConfig()
    }

    /**
     * 加载配置（启动时和重载时调用）
     */
    fun loadConfig() {
        val config = plugin.config

        // 读取基础配置
        enabled = config.getBoolean("chatbubble.enabled", true)
        yOffset = config.getDouble("chatbubble.yOffset", 0.75)
        timeSpan = config.getInt("chatbubble.timeSpan", 100)
        shadow = config.getBoolean("chatbubble.shadow", false)
        viewRange = config.getDouble("chatbubble.viewRange", 16.0).toFloat()
        updateTicks = config.getLong("chatbubble.updateTicks", 2L)
        movementTicks = config.getInt("chatbubble.movementTicks", 4)

        // 读取不透明度配置
        defaultOpacity = (config.getDouble("chatbubble.opacity.default", 1.0)
            .coerceIn(0.0, 1.0) * 255).toInt().toByte()
        sneakingOpacity = (config.getDouble("chatbubble.opacity.sneaking", 0.25)
            .coerceIn(0.0, 1.0) * 255).toInt().toByte()

        // 读取 Billboard 类型
        billboard = try {
            Display.Billboard.valueOf(
                config.getString("chatbubble.billboard", "VERTICAL")?.uppercase() ?: "VERTICAL"
            )
        } catch (_: IllegalArgumentException) {
            plugin.logger.warning("[ChatBubble] 无效的 billboard 类型，使用默认值 VERTICAL")
            Display.Billboard.VERTICAL
        }

        // 读取背景颜色配置
        val bgRed = config.getInt("chatbubble.background.red", -1)
        useDefaultBackground = bgRed < 0
        if (!useDefaultBackground) {
            val alpha = config.getInt("chatbubble.background.alpha", 0).coerceIn(0, 255)
            val red = bgRed.coerceIn(0, 255)
            val green = config.getInt("chatbubble.background.green", 0).coerceIn(0, 255)
            val blue = config.getInt("chatbubble.background.blue", 0).coerceIn(0, 255)
            backgroundColor = Color.fromARGB(alpha, red, green, blue)
        }

        plugin.logger.info("[ChatBubble] 配置已加载 - 启用: $enabled")
    }

    /**
     * 功能是否启用
     */
    fun isEnabled(): Boolean = enabled

    /**
     * 创建或更新聊天气泡
     * @param player 玩家
     * @param message 消息内容
     */
    fun createOrUpdateBubble(player: Player, message: Component) {
        if (!enabled) return
        if (player.isInvisibleForBubble()) return

        // 如果已存在气泡，更新内容并重置生命周期
        val existingBubble = bubbles[player]
        if (existingBubble != null && existingBubble.isValid) {
            existingBubble.ticksLived = 1
            existingBubble.text(message)
            return
        }

        // 创建新气泡
        val location = calculateBubbleLocation(player)
        val display = player.world.spawn(location, TextDisplay::class.java) { textDisplay ->
            // 基础属性
            textDisplay.isPersistent = false
            textDisplay.text(message)
            textDisplay.isSeeThrough = false
            textDisplay.isShadowed = shadow
            textDisplay.viewRange = viewRange
            textDisplay.teleportDuration = movementTicks
            textDisplay.textOpacity = defaultOpacity
            textDisplay.billboard = billboard

            // 背景颜色
            textDisplay.isDefaultBackground = useDefaultBackground
            if (!useDefaultBackground) {
                textDisplay.backgroundColor = backgroundColor
            }
        }

        // 如果玩家未启用自我显示，隐藏气泡
        if (!selfDisplayEnabled.contains(player)) {
            player.hideEntity(plugin, display)
        }

        // 保存气泡引用
        bubbles[player] = display

        // 启动更新任务（Folia 实体调度器）
        player.scheduler.runAtFixedRate(plugin, { task ->
            // 检查有效性
            if (!player.isValid || !display.isValid || player.isInvisibleForBubble() ||
                display.ticksLived > timeSpan) {
                task.cancel()
                display.remove()
                bubbles.remove(player)
                return@runAtFixedRate
            }

            // 更新不透明度（潜行时降低）
            display.textOpacity = if (player.isSneaking) sneakingOpacity else defaultOpacity

            // 更新位置
            display.teleportAsync(calculateBubbleLocation(player))

            // 更新附近玩家的可见性
            player.location.getNearbyPlayers(viewRange.toDouble()).forEach { nearbyPlayer ->
                if (nearbyPlayer == player) return@forEach

                if (!nearbyPlayer.canSee(player)) {
                    nearbyPlayer.hideEntity(plugin, display)
                } else {
                    nearbyPlayer.showEntity(plugin, display)
                }
            }
        }, null, 1L, updateTicks)
    }

    /**
     * 计算气泡位置（玩家头顶上方）
     */
    private fun calculateBubbleLocation(player: Player): Location {
        return player.location.add(0.0, player.boundingBox.height + yOffset, 0.0).apply {
            yaw = 0f
            pitch = 0f
        }
    }

    /**
     * 检查玩家是否隐身（用于气泡显示）
     */
    private fun Player.isInvisibleForBubble(): Boolean {
        return this.gameMode == GameMode.SPECTATOR ||
               this.hasPotionEffect(PotionEffectType.INVISIBILITY)
    }

    /**
     * 切换玩家的自我显示设置
     * @param player 玩家
     * @return 切换后的状态（true=启用，false=禁用）
     */
    fun toggleSelfDisplay(player: Player): Boolean {
        val newState = if (selfDisplayEnabled.contains(player)) {
            selfDisplayEnabled.remove(player)
            // 隐藏气泡
            bubbles[player]?.let { bubble ->
                player.hideEntity(plugin, bubble)
            }
            false
        } else {
            selfDisplayEnabled.add(player)
            // 显示气泡
            bubbles[player]?.let { bubble ->
                player.showEntity(plugin, bubble)
            }
            true
        }
        return newState
    }

    /**
     * 获取玩家的气泡实体
     */
    fun getBubble(player: Player): TextDisplay? {
        return bubbles[player]
    }

    /**
     * 获取玩家的所有气泡实体（兼容性方法）
     */
    fun getBubbles(player: Player): List<TextDisplay> {
        return bubbles[player]?.let { listOf(it) } ?: emptyList()
    }

    /**
     * 获取玩家的自我显示状态
     */
    fun getSelfDisplayEnabled(player: Player): Boolean {
        return selfDisplayEnabled.contains(player)
    }

    /**
     * 清理玩家数据（玩家退出时调用）
     */
    fun cleanupPlayer(player: Player) {
        // 移除并删除气泡
        bubbles.remove(player)?.remove()

        // 清理自我显示设置
        selfDisplayEnabled.remove(player)
    }

    /**
     * 清理所有气泡（插件卸载时调用）
     */
    fun cleanupAll() {
        bubbles.values.forEach { it.remove() }
        bubbles.clear()
        selfDisplayEnabled.clear()
    }
}

