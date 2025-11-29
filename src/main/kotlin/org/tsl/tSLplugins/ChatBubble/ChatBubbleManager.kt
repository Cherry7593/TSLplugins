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
     * 创建或更新气泡
     * @param player 玩家
     * @param message 消息内容
     *
     * 方案 D：使用 Passenger 机制
     * - TextDisplay 作为玩家的乘客，自动跟随
     * - 创建时设置固定属性，之后不再修改
     * - 完全避免跨线程访问
     */
    fun createOrUpdateBubble(player: Player, message: Component) {
        if (!enabled) return
        if (player.isInvisibleForBubble()) return

        // 清除旧气泡
        cleanupPlayer(player)

        // 创建新气泡（所有属性在创建时固定）
        val location = calculateBubbleLocation(player)
        val display = player.world.spawn(location, TextDisplay::class.java) { textDisplay ->
            // 基础属性
            textDisplay.isPersistent = false
            textDisplay.text(message)
            textDisplay.isSeeThrough = false
            textDisplay.isShadowed = shadow
            textDisplay.viewRange = viewRange
            textDisplay.billboard = billboard

            // 固定不透明度（不再动态更新）
            textDisplay.textOpacity = defaultOpacity

            // 背景颜色
            textDisplay.isDefaultBackground = useDefaultBackground
            if (!useDefaultBackground) {
                textDisplay.backgroundColor = backgroundColor
            }
        }

        // 让气泡成为玩家的乘客（自动跟随，包括传送）
        player.addPassenger(display)

        // 如果玩家未启用自我显示，隐藏气泡
        if (!selfDisplayEnabled.contains(player)) {
            try {
                player.hideEntity(plugin, display)
            } catch (e: Exception) {
                // 忽略隐藏错误
            }
        }

        // 保存气泡引用
        bubbles[player] = display

        // 定时删除气泡（使用 display 的调度器确保线程安全）
        display.scheduler.runDelayed(plugin, { _ ->
            // 在 display 所在的线程删除（线程安全）
            try {
                if (display.isValid) {
                    display.remove()
                }
            } catch (e: Exception) {
                // 忽略删除错误
            }

            // 清理引用（使用玩家调度器确保线程安全）
            player.scheduler.run(plugin, { _ ->
                bubbles.remove(player)
            }, null)
        }, null, timeSpan.toLong())
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
            // 隐藏气泡（安全处理）
            bubbles[player]?.let { bubble ->
                try {
                    player.hideEntity(plugin, bubble)
                } catch (e: Exception) {
                    // 忽略错误
                }
            }
            false
        } else {
            selfDisplayEnabled.add(player)
            // 显示气泡（安全处理）
            bubbles[player]?.let { bubble ->
                try {
                    player.showEntity(plugin, bubble)
                } catch (e: Exception) {
                    // 忽略错误
                }
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
     * 清理玩家数据（玩家退出或传送时调用）
     */
    fun cleanupPlayer(player: Player) {
        // 移除并删除气泡
        bubbles.remove(player)?.let { display ->
            safeRemoveBubble(player, display)
        }

        // 清理自我显示设置
        selfDisplayEnabled.remove(player)
    }

    /**
     * 线程安全地移除气泡
     * 使用 display 的调度器确保在正确的线程上删除
     */
    private fun safeRemoveBubble(player: Player, display: TextDisplay) {
        // 使用 display 自己的调度器在正确的线程上删除
        // 不在外部检查 isValid，避免跨线程访问
        try {
            display.scheduler.run(plugin, { _ ->
                try {
                    if (display.isValid) {
                        display.remove()
                    }
                } catch (e: Exception) {
                    // 忽略删除错误（实体可能已删除或在其他线程）
                }
            }, null)
        } catch (e: Exception) {
            // 忽略调度错误（实体可能已被删除）
        }

        // 立即清理引用（不等待删除完成）
        bubbles.remove(player)
    }

    /**
     * 清理所有气泡（插件卸载时调用）
     */
    fun cleanupAll() {
        bubbles.values.forEach { display ->
            try {
                if (display.isValid) {
                    display.remove()
                }
            } catch (e: Exception) {
                // 忽略删除错误
            }
        }
        bubbles.clear()
        selfDisplayEnabled.clear()
    }
}

