package org.tsl.tSLplugins.modules.landmark

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType
import org.bukkit.NamespacedKey
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 地标选区工具 (OPK Tool)
 * 提供可视化的地标区域选择功能
 */
class LandmarkOPKTool(
    private val plugin: JavaPlugin,
    private val manager: LandmarkManager
) : Listener {

    private val serializer = LegacyComponentSerializer.legacyAmpersand()
    private val opkKey = NamespacedKey(plugin, "landmark_opk_tool")

    // 玩家选点数据 (UUID -> Pair<pos1, pos2>)
    private val playerSelections: MutableMap<UUID, Pair<Pair<Int, Int>?, Pair<Int, Int>?>> = ConcurrentHashMap()

    // 玩家选点世界
    private val playerWorlds: MutableMap<UUID, String> = ConcurrentHashMap()

    // 可视化任务（存储 ScheduledTask 以便正确取消）
    private val visualizationTasks: MutableMap<UUID, io.papermc.paper.threadedregions.scheduler.ScheduledTask> = ConcurrentHashMap()

    /**
     * 创建 OPK 工具物品
     */
    fun createOPKTool(): ItemStack {
        val item = ItemStack(Material.GOLDEN_HOE)
        val meta = item.itemMeta ?: return item

        meta.displayName(serializer.deserialize("&6&l地标选区工具"))
        meta.lore(listOf(
            serializer.deserialize("&7用于选择地标区域范围"),
            serializer.deserialize(""),
            serializer.deserialize("&e左键 &7- 设置点 1"),
            serializer.deserialize("&e右键 &7- 设置点 2"),
            serializer.deserialize(""),
            serializer.deserialize("&7选择完成后使用"),
            serializer.deserialize("&e/tsl landmark finalize &7完成创建")
        ))

        meta.persistentDataContainer.set(opkKey, PersistentDataType.BYTE, 1)
        item.itemMeta = meta
        return item
    }

    /**
     * 检查物品是否是 OPK 工具
     */
    fun isOPKTool(item: ItemStack?): Boolean {
        if (item == null || item.type != Material.GOLDEN_HOE) return false
        val meta = item.itemMeta ?: return false
        return meta.persistentDataContainer.has(opkKey, PersistentDataType.BYTE)
    }

    /**
     * 给玩家 OPK 工具
     */
    fun giveOPKTool(player: Player) {
        val tool = createOPKTool()
        player.inventory.addItem(tool)
        player.sendMessage(serializer.deserialize(manager.getMessage("opk_tool_given")))
    }

    /**
     * 获取玩家选点数据
     */
    fun getSelection(playerUuid: UUID): Pair<Pair<Int, Int>?, Pair<Int, Int>?> {
        return playerSelections[playerUuid] ?: Pair(null, null)
    }

    /**
     * 获取玩家选点世界
     */
    fun getSelectionWorld(playerUuid: UUID): String? {
        return playerWorlds[playerUuid]
    }

    /**
     * 清除玩家选点数据
     */
    fun clearSelection(playerUuid: UUID) {
        playerSelections.remove(playerUuid)
        playerWorlds.remove(playerUuid)
    }

    /**
     * 处理玩家交互事件
     */
    @EventHandler(priority = EventPriority.HIGH)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val player = event.player
        val item = event.item

        if (!isOPKTool(item)) return
        if (!player.isOp && !player.hasPermission("tsl.landmark.admin")) return

        event.isCancelled = true

        val loc = event.clickedBlock?.location ?: player.location
        val x = loc.blockX
        val z = loc.blockZ
        val world = loc.world?.name ?: return

        when (event.action) {
            Action.LEFT_CLICK_BLOCK, Action.LEFT_CLICK_AIR -> {
                // 设置点 1
                val currentSelection = playerSelections[player.uniqueId] ?: Pair(null, null)
                playerSelections[player.uniqueId] = Pair(Pair(x, z), currentSelection.second)
                playerWorlds[player.uniqueId] = world

                player.sendMessage(serializer.deserialize(
                    manager.getMessage("pos1_set", "x" to x.toString(), "z" to z.toString())
                ))

                // 更新可视化
                updateVisualization(player)
            }
            Action.RIGHT_CLICK_BLOCK, Action.RIGHT_CLICK_AIR -> {
                // 设置点 2
                val currentWorld = playerWorlds[player.uniqueId]
                if (currentWorld != null && currentWorld != world) {
                    player.sendMessage(serializer.deserialize(manager.getMessage("different_world")))
                    return
                }

                val currentSelection = playerSelections[player.uniqueId] ?: Pair(null, null)
                playerSelections[player.uniqueId] = Pair(currentSelection.first, Pair(x, z))
                if (currentWorld == null) playerWorlds[player.uniqueId] = world

                player.sendMessage(serializer.deserialize(
                    manager.getMessage("pos2_set", "x" to x.toString(), "z" to z.toString())
                ))

                // 更新可视化
                updateVisualization(player)
            }
            else -> {}
        }
    }

    /**
     * 处理玩家切换手持物品
     */
    @EventHandler
    fun onItemHeld(event: PlayerItemHeldEvent) {
        val player = event.player
        if (!player.isOp && !player.hasPermission("tsl.landmark.admin")) return

        val newItem = player.inventory.getItem(event.newSlot)

        if (isOPKTool(newItem)) {
            startVisualization(player)
        } else {
            stopVisualization(player)
        }
    }

    /**
     * 处理玩家退出
     */
    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val uuid = event.player.uniqueId
        stopVisualization(event.player)
        playerSelections.remove(uuid)
        playerWorlds.remove(uuid)
    }

    /**
     * 更新可视化
     */
    private fun updateVisualization(player: Player) {
        if (isOPKTool(player.inventory.itemInMainHand)) {
            startVisualization(player)
        }
    }

    /**
     * 启动区域可视化
     */
    private fun startVisualization(player: Player) {
        stopVisualization(player)

        val task = Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, { scheduledTask ->
            if (!player.isOnline) {
                visualizationTasks.remove(player.uniqueId)
                scheduledTask.cancel()
                return@runAtFixedRate
            }

            // 检查玩家是否仍持有 OPK 工具，如果没有则停止可视化
            if (!isOPKTool(player.inventory.itemInMainHand)) {
                visualizationTasks.remove(player.uniqueId)
                scheduledTask.cancel()
                return@runAtFixedRate
            }

            // 显示玩家选择的区域
            val selection = playerSelections[player.uniqueId]
            val world = playerWorlds[player.uniqueId]
            if (selection != null && world != null) {
                val pos1 = selection.first
                val pos2 = selection.second
                if (pos1 != null && pos2 != null) {
                    showRegionBorder(player, pos1, pos2, world, Particle.FLAME)
                } else if (pos1 != null) {
                    showPoint(player, pos1, world, Particle.FLAME)
                }
            }

            // 显示所有地标区域边界
            val playerWorld = player.world.name
            manager.getAllLandmarks()
                .filter { it.world == playerWorld }
                .forEach { landmark ->
                    showLandmarkBorder(player, landmark)
                }
        }, 10L, 10L)

        visualizationTasks[player.uniqueId] = task
    }

    /**
     * 停止区域可视化
     */
    private fun stopVisualization(player: Player) {
        visualizationTasks.remove(player.uniqueId)?.cancel()
    }

    /**
     * 显示区域边界（限制粒子数量以优化性能）
     */
    private fun showRegionBorder(player: Player, pos1: Pair<Int, Int>, pos2: Pair<Int, Int>, worldName: String, particle: Particle) {
        if (player.world.name != worldName) return

        val minX = minOf(pos1.first, pos2.first).toDouble()
        val maxX = maxOf(pos1.first, pos2.first).toDouble() + 1
        val minZ = minOf(pos1.second, pos2.second).toDouble()
        val maxZ = maxOf(pos1.second, pos2.second).toDouble() + 1
        val y = player.location.y

        // 计算边长并动态调整步长（最多每边 50 个粒子）
        val lengthX = maxX - minX
        val lengthZ = maxZ - minZ
        val maxParticlesPerEdge = 50
        val stepX = maxOf(1.0, lengthX / maxParticlesPerEdge)
        val stepZ = maxOf(1.0, lengthZ / maxParticlesPerEdge)

        // 北边 (minZ)
        var x = minX
        while (x <= maxX) {
            player.spawnParticle(particle, x, y, minZ, 1, 0.0, 0.0, 0.0, 0.0)
            x += stepX
        }

        // 南边 (maxZ)
        x = minX
        while (x <= maxX) {
            player.spawnParticle(particle, x, y, maxZ, 1, 0.0, 0.0, 0.0, 0.0)
            x += stepX
        }

        // 西边 (minX)
        var z = minZ
        while (z <= maxZ) {
            player.spawnParticle(particle, minX, y, z, 1, 0.0, 0.0, 0.0, 0.0)
            z += stepZ
        }

        // 东边 (maxX)
        z = minZ
        while (z <= maxZ) {
            player.spawnParticle(particle, maxX, y, z, 1, 0.0, 0.0, 0.0, 0.0)
            z += stepZ
        }
    }

    /**
     * 显示单点
     */
    private fun showPoint(player: Player, pos: Pair<Int, Int>, worldName: String, particle: Particle) {
        if (player.world.name != worldName) return
        val y = player.location.y
        player.spawnParticle(particle, pos.first.toDouble() + 0.5, y, pos.second.toDouble() + 0.5, 5, 0.3, 0.3, 0.3, 0.0)
    }

    /**
     * 显示地标边界
     */
    private fun showLandmarkBorder(player: Player, landmark: Landmark) {
        val region = landmark.region
        showRegionBorder(
            player,
            Pair(region.minX, region.minZ),
            Pair(region.maxX, region.maxZ),
            landmark.world,
            Particle.HAPPY_VILLAGER
        )
    }
}
