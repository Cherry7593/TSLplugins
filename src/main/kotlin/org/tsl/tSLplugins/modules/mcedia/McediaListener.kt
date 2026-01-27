package org.tsl.tSLplugins.modules.mcedia

import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.player.PlayerArmorStandManipulateEvent
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.entity.ArmorStand
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.meta.BookMeta
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID

/**
 * Mcedia 事件监听器
 * 监听盔甲架操作、聊天输入等
 *
 * 包含原 paperplugin 的同步功能：
 * - 当玩家放置书到盔甲架时，更新书的 displayName 为 "玩家名:时间戳"
 * - 这样客户端 mod 可以检测到变化并更新播放内容
 */
class McediaListener(
    private val plugin: JavaPlugin,
    private val manager: McediaManager,
    private val gui: McediaGUI
) : Listener {

    /**
     * 监听聊天消息用于 GUI 输入
     */
    @EventHandler(priority = EventPriority.LOWEST)
    fun onAsyncChat(event: AsyncChatEvent) {
        val player = event.player
        val state = gui.getPlayerState(player) ?: return

        // 检查是否在等待输入
        if (state.type == McediaGUI.GUIType.CREATE_PLAYER ||
            state.tempData["awaiting_input"] != null) {

            val message = PlainTextComponentSerializer.plainText().serialize(event.message())

            // 取消聊天消息
            event.isCancelled = true

            // 处理输入
            gui.handleChatInput(player, message)
        }
    }

    /**
     * 监听实体交互事件（用于不可见盔甲架）
     * 这个事件在玩家右键任何实体（包括不可见的）时触发
     */
    @EventHandler(priority = EventPriority.HIGH)
    fun onPlayerInteractAtEntity(event: PlayerInteractAtEntityEvent) {
        if (!manager.isEnabled()) return
        if (event.rightClicked !is ArmorStand) return
        if (event.hand != EquipmentSlot.HAND) return

        val armorStand = event.rightClicked as ArmorStand
        val customName = armorStand.customName()?.let {
            PlainTextComponentSerializer.plainText().serialize(it)
        } ?: ""

        // 检查是否是 mcedia 播放器
        val prefix = manager.getPlayerNamePrefix()
        if (!customName.contains(prefix, ignoreCase = true)) return

        // 如果玩家有权限且潜行，检查触发物品后打开 GUI 编辑界面
        if (event.player.isSneaking && event.player.hasPermission("tsl.mcedia.use")) {
            val itemInHand = event.player.inventory.itemInMainHand
            val triggerItem = manager.getTriggerItem()

            // 检查手持物品是否匹配触发物品
            val canTrigger = if (triggerItem == null) {
                // 配置为空，需要空手
                itemInHand.type.isAir
            } else {
                // 需要指定物品
                itemInHand.type == triggerItem
            }

            if (!canTrigger) return

            event.isCancelled = true

            var mcediaPlayer = manager.getPlayer(armorStand.uniqueId)
            
            // 如果不在缓存中，尝试自动注册（支持手动放置书本召唤的盔甲架）
            if (mcediaPlayer == null) {
                val playerName = customName.removePrefix("$prefix:").ifEmpty { customName }
                mcediaPlayer = McediaPlayer(
                    uuid = armorStand.uniqueId,
                    name = playerName,
                    location = armorStand.location,
                    createdBy = event.player.uniqueId
                )
                manager.addPlayer(mcediaPlayer)
                event.player.sendMessage("§6[Mcedia] §a已自动记录播放器: §f$playerName")
            }
            
            event.player.scheduler.run(plugin, { _ ->
                gui.openPlayerEdit(event.player, mcediaPlayer)
            }, null)
        }
    }

    /**
     * 监听盔甲架操作
     *
     * 功能：如果玩家正常右键放置书，进行同步处理（原 paperplugin 功能）
     * 注意：潜行打开 GUI 的功能已移至 onPlayerInteractAtEntity，因为它可以处理不可见盔甲架
     */
    @EventHandler
    fun onArmorStandManipulate(event: PlayerArmorStandManipulateEvent) {
        if (!manager.isEnabled()) return

        val armorStand = event.rightClicked
        val customName = armorStand.customName()?.let {
            PlainTextComponentSerializer.plainText().serialize(it)
        } ?: ""

        // 检查是否是 mcedia 播放器（名称包含 mcedia 前缀）
        val prefix = manager.getPlayerNamePrefix()
        if (!customName.contains(prefix, ignoreCase = true)) return

        // 如果玩家潜行且满足触发条件，已经在 onPlayerInteractAtEntity 中处理了
        if (event.player.isSneaking && event.player.hasPermission("tsl.mcedia.use")) {
            val itemInHand = event.player.inventory.itemInMainHand
            val triggerItem = manager.getTriggerItem()
            val canTrigger = if (triggerItem == null) {
                itemInHand.type.isAir
            } else {
                itemInHand.type == triggerItem
            }
            if (canTrigger) {
                event.isCancelled = true
                return
            }
        }

        // 同步功能 - 处理书的放置（原 paperplugin 功能）
        // 只处理对主手的操作
        if (event.slot != EquipmentSlot.HAND) return

        // 阻止玩家直接拿走/放置，我们自己处理
        event.isCancelled = true

        val playerItem = event.playerItem // 玩家手上的物品

        // 如果玩家手上没有物品，清空盔甲架主手
        if (playerItem.type.isAir) {
            armorStand.equipment.setItemInMainHand(null)
            plugin.logger.info("[Mcedia] 播放器 $customName 已清空")
            return
        }

        // 确保玩家手上拿的是书
        if (!isBook(playerItem.type)) {
            return
        }

        // 克隆玩家手上的书，以防修改影响到玩家背包里的原件
        val bookToPlace = playerItem.clone()
        bookToPlace.amount = 1 // 确保只放置一本

        val bookMeta = bookToPlace.itemMeta as? BookMeta
        if (bookMeta == null || !bookMeta.hasPages()) {
            return
        }

        // 修改物品的显示名称，格式为 "玩家名:时间戳"
        // 这是客户端 mod 用于检测变化的关键
        bookMeta.displayName(Component.text("${event.player.name}:${System.currentTimeMillis()}"))
        bookToPlace.itemMeta = bookMeta

        // 将处理过的新书放置到盔甲架上
        armorStand.equipment.setItemInMainHand(bookToPlace)
        plugin.logger.info("[Mcedia] 播放器 $customName 的内容已更新")
    }

    /**
     * 检查是否是书类型物品
     */
    private fun isBook(material: Material): Boolean {
        return material == Material.WRITABLE_BOOK ||
               material == Material.WRITTEN_BOOK ||
               material.name.contains("BOOK")
    }

    /**
     * 监听盔甲架死亡，从缓存和数据库中移除
     */
    @EventHandler
    fun onEntityDeath(event: EntityDeathEvent) {
        if (event.entity !is ArmorStand) return

        val armorStand = event.entity as ArmorStand
        val customName = armorStand.customName()?.let {
            PlainTextComponentSerializer.plainText().serialize(it)
        } ?: return

        if (customName.contains(manager.getPlayerNamePrefix(), ignoreCase = true)) {
            // 从管理器中删除（包括数据库）
            val mcediaPlayer = manager.getPlayer(armorStand.uniqueId)
            if (mcediaPlayer != null) {
                manager.deletePlayer(armorStand.uniqueId)
                plugin.logger.info("[Mcedia] 播放器被移除: ${mcediaPlayer.name}")
            }
        }
    }

    /**
     * 区块加载时扫描播放器并处理待处理操作
     */
    @EventHandler
    fun onChunkLoad(event: ChunkLoadEvent) {
        if (!manager.isEnabled()) return

        val chunk = event.chunk
        val worldName = chunk.world.name

        // 处理待处理操作（删除、更新等）
        manager.processPendingOperations(worldName, chunk.x, chunk.z)

        // 检查区块中的盔甲架
        chunk.entities.filterIsInstance<ArmorStand>().forEach { armorStand ->
            val customName = armorStand.customName()?.let {
                PlainTextComponentSerializer.plainText().serialize(it)
            } ?: return@forEach

            if (customName.contains(manager.getPlayerNamePrefix(), ignoreCase = true)) {
                // 如果不在缓存中，添加到缓存
                if (manager.getPlayer(armorStand.uniqueId) == null) {
                    val name = customName.removePrefix("${manager.getPlayerNamePrefix()}:").ifEmpty { customName }

                    val mcediaPlayer = McediaPlayer(
                        uuid = armorStand.uniqueId,
                        name = name,
                        location = armorStand.location,
                        createdBy = UUID.randomUUID()
                    )

                    manager.addPlayer(mcediaPlayer)
                }
            }
        }
    }

    /**
     * 监听玩家使用命名牌命名盔甲架
     * 当玩家用命名牌将盔甲架命名为包含 mcedia 前缀时，自动记录为播放器
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerNameTagEntity(event: PlayerInteractEntityEvent) {
        if (!manager.isEnabled()) return
        if (event.rightClicked !is ArmorStand) return
        if (event.hand != EquipmentSlot.HAND) return

        val player = event.player
        val itemInHand = player.inventory.itemInMainHand

        // 检查是否是命名牌
        if (itemInHand.type != Material.NAME_TAG) return

        // 获取命名牌上的名称
        val itemMeta = itemInHand.itemMeta ?: return
        if (!itemMeta.hasDisplayName()) return

        val newName = itemMeta.displayName()?.let {
            PlainTextComponentSerializer.plainText().serialize(it)
        } ?: return

        val prefix = manager.getPlayerNamePrefix()

        // 检查新名称是否包含 mcedia 前缀
        if (!newName.contains(prefix, ignoreCase = true)) return

        val armorStand = event.rightClicked as ArmorStand

        // 延迟一个 tick 执行，等待命名牌生效
        armorStand.scheduler.runDelayed(plugin, { _ ->
            // 再次检查盔甲架的名称
            val finalName = armorStand.customName()?.let {
                PlainTextComponentSerializer.plainText().serialize(it)
            } ?: return@runDelayed

            if (!finalName.contains(prefix, ignoreCase = true)) return@runDelayed

            // 检查是否已经在缓存中
            if (manager.getPlayer(armorStand.uniqueId) != null) {
                plugin.logger.info("[Mcedia] 播放器已存在: $finalName")
                return@runDelayed
            }

            // 提取播放器名称
            val playerName = finalName.removePrefix("$prefix:").ifEmpty { finalName }

            // 创建并添加播放器
            val mcediaPlayer = McediaPlayer(
                uuid = armorStand.uniqueId,
                name = playerName,
                location = armorStand.location,
                createdBy = player.uniqueId
            )

            manager.addPlayer(mcediaPlayer)
            player.sendMessage("§6[Mcedia] §a已自动记录播放器: §f$playerName")
            plugin.logger.info("[Mcedia] 玩家 ${player.name} 使用命名牌创建了播放器: $playerName")
        }, null, 1L)
    }
}

