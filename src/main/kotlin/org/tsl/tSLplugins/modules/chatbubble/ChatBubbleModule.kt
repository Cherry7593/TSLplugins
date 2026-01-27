package org.tsl.tSLplugins.modules.chatbubble

import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Color
import org.bukkit.GameMode
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Display
import org.bukkit.entity.Player
import org.bukkit.entity.TextDisplay
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.potion.PotionEffectType
import org.tsl.tSLplugins.SubCommandHandler
import org.tsl.tSLplugins.core.AbstractModule
import java.util.concurrent.ConcurrentHashMap

/**
 * ChatBubble 模块 - 聊天气泡
 * 
 * 在玩家头顶显示聊天消息气泡
 * 
 * ## 命令
 * - `/tsl chatbubble` - 切换自我显示
 * - `/tsl chatbubble status` - 查看状态
 * 
 * ## 权限
 * 无特殊权限要求
 */
class ChatBubbleModule : AbstractModule() {

    override val id = "chatbubble"
    override val configPath = "chatbubble"

    // 配置项
    private var yOffset: Double = 0.75
    private var timeSpan: Int = 100
    private var billboard: Display.Billboard = Display.Billboard.VERTICAL
    private var shadow: Boolean = false
    private var viewRange: Float = 16.0f
    private var defaultOpacity: Byte = (-1).toByte()
    private var useDefaultBackground: Boolean = true
    private var backgroundColor: Color = Color.fromARGB(0, 0, 0, 0)

    // 运行时数据
    private val bubbles: MutableMap<Player, TextDisplay> = ConcurrentHashMap()
    private val selfDisplayEnabled: MutableSet<Player> = ConcurrentHashMap.newKeySet()

    private lateinit var listener: ChatBubbleModuleListener

    override fun doEnable() {
        loadChatBubbleConfig()
        listener = ChatBubbleModuleListener(this)
        registerListener(listener)
    }

    override fun doDisable() {
        cleanupAll()
    }

    override fun doReload() {
        loadChatBubbleConfig()
    }

    override fun getCommandHandler(): SubCommandHandler = ChatBubbleModuleCommand(this)
    
    override fun getDescription(): String = "聊天气泡"

    private fun loadChatBubbleConfig() {
        yOffset = getConfigDouble("yOffset", 0.75)
        timeSpan = getConfigInt("timeSpan", 100)
        shadow = getConfigBoolean("shadow", false)
        viewRange = getConfigDouble("viewRange", 16.0).toFloat()

        defaultOpacity = (getConfigDouble("opacity.default", 1.0).coerceIn(0.0, 1.0) * 255).toInt().toByte()

        billboard = try {
            Display.Billboard.valueOf(getConfigString("billboard", "VERTICAL").uppercase())
        } catch (_: IllegalArgumentException) {
            Display.Billboard.VERTICAL
        }

        val bgRed = getConfigInt("background.red", -1)
        useDefaultBackground = bgRed < 0
        if (!useDefaultBackground) {
            val alpha = getConfigInt("background.alpha", 0).coerceIn(0, 255)
            val red = bgRed.coerceIn(0, 255)
            val green = getConfigInt("background.green", 0).coerceIn(0, 255)
            val blue = getConfigInt("background.blue", 0).coerceIn(0, 255)
            backgroundColor = Color.fromARGB(alpha, red, green, blue)
        }
    }

    // ============== 公开 API ==============

    fun createOrUpdateBubble(player: Player, message: Component) {
        if (!isEnabled()) return
        if (player.isInvisibleForBubble()) return

        cleanupPlayer(player)

        val location = player.location.add(0.0, player.boundingBox.height + yOffset, 0.0).apply {
            yaw = 0f
            pitch = 0f
        }

        val display = player.world.spawn(location, TextDisplay::class.java) { textDisplay ->
            textDisplay.isPersistent = false
            textDisplay.text(message)
            textDisplay.isSeeThrough = false
            textDisplay.isShadowed = shadow
            textDisplay.viewRange = viewRange
            textDisplay.billboard = billboard
            textDisplay.textOpacity = defaultOpacity
            textDisplay.isDefaultBackground = useDefaultBackground
            if (!useDefaultBackground) {
                textDisplay.backgroundColor = backgroundColor
            }
        }

        player.addPassenger(display)

        if (!selfDisplayEnabled.contains(player)) {
            try { player.hideEntity(context.plugin, display) } catch (_: Exception) {}
        }

        bubbles[player] = display

        display.scheduler.runDelayed(context.plugin, { _ ->
            try { if (display.isValid) display.remove() } catch (_: Exception) {}
            player.scheduler.run(context.plugin, { _ -> bubbles.remove(player) }, null)
        }, null, timeSpan.toLong())
    }

    private fun Player.isInvisibleForBubble(): Boolean {
        return this.gameMode == GameMode.SPECTATOR || this.hasPotionEffect(PotionEffectType.INVISIBILITY)
    }

    fun toggleSelfDisplay(player: Player): Boolean {
        return if (selfDisplayEnabled.contains(player)) {
            selfDisplayEnabled.remove(player)
            bubbles[player]?.let { try { player.hideEntity(context.plugin, it) } catch (_: Exception) {} }
            false
        } else {
            selfDisplayEnabled.add(player)
            bubbles[player]?.let { try { player.showEntity(context.plugin, it) } catch (_: Exception) {} }
            true
        }
    }

    fun getBubble(player: Player): TextDisplay? = bubbles[player]
    fun getSelfDisplayEnabled(player: Player): Boolean = selfDisplayEnabled.contains(player)

    fun cleanupPlayer(player: Player) {
        bubbles.remove(player)?.let { display ->
            try {
                display.scheduler.run(context.plugin, { _ ->
                    try { if (display.isValid) display.remove() } catch (_: Exception) {}
                }, null)
            } catch (_: Exception) {}
        }
        selfDisplayEnabled.remove(player)
    }

    private fun cleanupAll() {
        bubbles.values.forEach { try { if (it.isValid) it.remove() } catch (_: Exception) {} }
        bubbles.clear()
        selfDisplayEnabled.clear()
    }

    fun getPlugin() = context.plugin
}

class ChatBubbleModuleListener(private val module: ChatBubbleModule) : Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onAsyncChat(event: AsyncChatEvent) {
        if (!module.isEnabled()) return
        val player = event.player
        val message = event.message()
        player.scheduler.run(module.getPlugin(), { _ ->
            module.createOrUpdateBubble(player, message)
        }, null)
    }

    @EventHandler
    fun onPlayerTeleport(event: PlayerTeleportEvent) {
        if (!module.isEnabled()) return
        module.cleanupPlayer(event.player)
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        module.cleanupPlayer(event.player)
    }
}

class ChatBubbleModuleCommand(private val module: ChatBubbleModule) : SubCommandHandler {
    private val serializer = LegacyComponentSerializer.legacyAmpersand()

    override fun handle(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!module.isEnabled()) {
            sender.sendMessage(serializer.deserialize("&c[ChatBubble] &7功能已禁用"))
            return true
        }
        if (sender !is Player) {
            sender.sendMessage(serializer.deserialize("&c[ChatBubble] &7此命令只能由玩家执行"))
            return true
        }

        when {
            args.isEmpty() || args[0].equals("self", ignoreCase = true) -> {
                val newState = module.toggleSelfDisplay(sender)
                val stateText = if (newState) "&a启用" else "&c禁用"
                sender.sendMessage(serializer.deserialize("&6[ChatBubble] &7自我显示已$stateText"))
            }
            args[0].equals("status", ignoreCase = true) -> {
                val enabled = module.getSelfDisplayEnabled(sender)
                val bubble = module.getBubble(sender)
                val stateText = if (enabled) "&a启用" else "&c禁用"
                val bubbleText = if (bubble != null && bubble.isValid) "&a存在" else "&7无"
                sender.sendMessage(serializer.deserialize("&6[ChatBubble] &7状态:"))
                sender.sendMessage(serializer.deserialize("&7- 自我显示: $stateText"))
                sender.sendMessage(serializer.deserialize("&7- 当前气泡: $bubbleText"))
            }
            else -> {
                sender.sendMessage(serializer.deserialize("&6[ChatBubble] &e使用方法:"))
                sender.sendMessage(serializer.deserialize("&7/tsl chatbubble &f- 切换自我显示"))
                sender.sendMessage(serializer.deserialize("&7/tsl chatbubble status &f- 查看状态"))
            }
        }
        return true
    }

    override fun tabComplete(sender: CommandSender, command: Command, label: String, args: Array<out String>): List<String> {
        if (!module.isEnabled() || sender !is Player) return emptyList()
        return when (args.size) {
            1 -> listOf("self", "status").filter { it.startsWith(args[0], ignoreCase = true) }
            else -> emptyList()
        }
    }

    override fun getDescription(): String = "聊天气泡功能"
}
