package org.tsl.tSLplugins.modules.redstonefreeze

import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.*
import org.bukkit.event.entity.EntitySpawnEvent
import org.bukkit.event.entity.ExplosionPrimeEvent
import org.tsl.tSLplugins.SubCommandHandler
import org.tsl.tSLplugins.core.AbstractModule
import java.util.concurrent.ConcurrentHashMap

/**
 * RedstoneFreeze 模块 - 红石冻结
 */
class RedstoneFreezeModule : AbstractModule() {

    override val id = "redstone-freeze"
    override val configPath = "redstone-freeze"

    private var maxRadius: Int = 32
    private var bossBarTitle: String = "§c❄ 当前区域物理已冻结 ❄"
    private var affectRedstoneSignal = true
    private var affectPistonExtend = true
    private var affectPistonRetract = true
    private var affectBlockPhysics = true
    private var affectTntPrime = true
    private var affectExplosion = true
    private var affectTntSpawn = true

    @Volatile private var freezeActive = false
    private val frozenChunks: MutableSet<Long> = ConcurrentHashMap.newKeySet()
    @Volatile private var frozenWorld: World? = null
    @Volatile private var freezeCenterX = 0
    @Volatile private var freezeCenterZ = 0
    @Volatile private var freezeRadius = 0
    @Volatile private var cachedChunkCount = 0

    private var freezeBossBar: BossBar? = null
    private var bossBarTask: ScheduledTask? = null
    private val bossBarPlayers: MutableSet<Player> = ConcurrentHashMap.newKeySet()

    private lateinit var listener: RedstoneFreezeModuleListener

    override fun doEnable() {
        loadFreezeConfig()
        listener = RedstoneFreezeModuleListener(this)
        registerListener(listener)
    }

    override fun doDisable() {
        cancelFreezeInternal()
    }

    override fun doReload() {
        loadFreezeConfig()
    }

    override fun getCommandHandler(): SubCommandHandler = RedstoneFreezeModuleCommand(this)
    override fun getDescription(): String = "红石冻结"

    private fun loadFreezeConfig() {
        maxRadius = getConfigInt("max-radius", 32)
        bossBarTitle = getConfigString("bossbar-title", "§c❄ 当前区域物理已冻结 ❄")
        affectRedstoneSignal = context.plugin.config.getBoolean("redstone-freeze.affected-components.redstone-signal", true)
        affectPistonExtend = context.plugin.config.getBoolean("redstone-freeze.affected-components.piston-extend", true)
        affectPistonRetract = context.plugin.config.getBoolean("redstone-freeze.affected-components.piston-retract", true)
        affectBlockPhysics = context.plugin.config.getBoolean("redstone-freeze.affected-components.block-physics", true)
        affectTntPrime = context.plugin.config.getBoolean("redstone-freeze.affected-components.tnt-prime", true)
        affectExplosion = context.plugin.config.getBoolean("redstone-freeze.affected-components.explosion", true)
        affectTntSpawn = context.plugin.config.getBoolean("redstone-freeze.affected-components.tnt-spawn", true)
        logInfo("配置已加载 - 最大半径: $maxRadius")
    }

    fun isFreezeActive() = freezeActive
    fun isChunkFrozen(chunkKey: Long) = frozenChunks.contains(chunkKey)
    fun isChunkFrozen(chunk: Chunk) = frozenChunks.contains(chunk.chunkKey)
    fun getMaxRadius() = maxRadius
    fun isRedstoneSignalAffected() = affectRedstoneSignal
    fun isPistonExtendAffected() = affectPistonExtend
    fun isPistonRetractAffected() = affectPistonRetract
    fun isBlockPhysicsAffected() = affectBlockPhysics
    fun isTntPrimeAffected() = affectTntPrime
    fun isExplosionAffected() = affectExplosion
    fun isTntSpawnAffected() = affectTntSpawn
    fun getFreezeInfo(): String? = if (!freezeActive) null else "世界: ${frozenWorld?.name}, 中心: ($freezeCenterX, $freezeCenterZ), 半径: $freezeRadius, 区块数: $cachedChunkCount"

    fun activateFreeze(player: Player, radius: Int): Int {
        if (freezeActive) cancelFreezeInternal()
        val world = player.world
        val playerChunk = player.location.chunk
        val centerChunkX = playerChunk.x
        val centerChunkZ = playerChunk.z
        for (cx in (centerChunkX - radius)..(centerChunkX + radius)) {
            for (cz in (centerChunkZ - radius)..(centerChunkZ + radius)) {
                val dx = cx - centerChunkX; val dz = cz - centerChunkZ
                if (dx * dx + dz * dz <= radius * radius) frozenChunks.add(Chunk.getChunkKey(cx, cz))
            }
        }
        frozenWorld = world; freezeCenterX = centerChunkX; freezeCenterZ = centerChunkZ; freezeRadius = radius; cachedChunkCount = frozenChunks.size
        freezeActive = true
        createBossBar(); startBossBarTask()
        logInfo("已冻结 $cachedChunkCount 个区块")
        return cachedChunkCount
    }

    fun cancelFreeze(): Int { val count = cachedChunkCount; cancelFreezeInternal(); return count }

    private fun cancelFreezeInternal() {
        freezeActive = false; stopBossBarTask(); removeBossBar()
        frozenChunks.clear(); frozenWorld = null; freezeCenterX = 0; freezeCenterZ = 0; freezeRadius = 0; cachedChunkCount = 0
    }

    private fun createBossBar() { freezeBossBar = BossBar.bossBar(Component.text(bossBarTitle).color(NamedTextColor.RED), 1.0f, BossBar.Color.BLUE, BossBar.Overlay.PROGRESS) }
    private fun removeBossBar() { val bar = freezeBossBar ?: return; bossBarPlayers.forEach { try { it.hideBossBar(bar) } catch (_: Exception) {} }; bossBarPlayers.clear(); freezeBossBar = null }
    private fun startBossBarTask() { bossBarTask = Bukkit.getGlobalRegionScheduler().runAtFixedRate(context.plugin, { _ -> updateBossBarVisibility() }, 20L, 20L) }
    private fun stopBossBarTask() { bossBarTask?.cancel(); bossBarTask = null }

    private fun updateBossBarVisibility() {
        val bar = freezeBossBar ?: return; val world = frozenWorld ?: return
        world.players.forEach { player ->
            val loc = player.location; val chunkKey = Chunk.getChunkKey(loc.blockX shr 4, loc.blockZ shr 4)
            val isInFrozenArea = frozenChunks.contains(chunkKey)
            if (isInFrozenArea && !bossBarPlayers.contains(player)) { player.scheduler.run(context.plugin, { _ -> player.showBossBar(bar) }, null); bossBarPlayers.add(player) }
            else if (!isInFrozenArea && bossBarPlayers.contains(player)) { player.scheduler.run(context.plugin, { _ -> player.hideBossBar(bar) }, null); bossBarPlayers.remove(player) }
        }
        bossBarPlayers.removeIf { !it.isOnline }
    }

    fun triggerPistonUpdates(player: Player, radius: Int, callback: (Int) -> Unit) {
        val world = player.world; val centerX = player.location.blockX shr 4; val centerZ = player.location.blockZ shr 4
        val blockRadius = radius * 16; val startX = (centerX shl 4) - blockRadius; val endX = (centerX shl 4) + blockRadius + 15
        val startZ = (centerZ shl 4) - blockRadius; val endZ = (centerZ shl 4) + blockRadius + 15
        Bukkit.getRegionScheduler().run(context.plugin, player.location) { _ ->
            var updatedCount = 0
            for (x in startX..endX) { for (z in startZ..endZ) { for (y in world.minHeight until world.maxHeight) {
                val block = world.getBlockAt(x, y, z)
                if (block.type == Material.PISTON || block.type == Material.STICKY_PISTON) {
                    val neighbors = listOf(block.getRelative(0, 1, 0), block.getRelative(0, -1, 0), block.getRelative(1, 0, 0), block.getRelative(-1, 0, 0), block.getRelative(0, 0, 1), block.getRelative(0, 0, -1))
                    for (neighbor in neighbors) { if (neighbor.type == Material.AIR) { try { neighbor.setType(Material.STONE, false); neighbor.setType(Material.AIR, true); updatedCount++; break } catch (_: Exception) {} } }
                }
            } } }
            callback(updatedCount)
        }
    }

    fun getPlugin() = context.plugin
}

class RedstoneFreezeModuleListener(private val module: RedstoneFreezeModule) : Listener {
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true) fun onBlockRedstone(event: BlockRedstoneEvent) {
        if (!module.isFreezeActive()) return; if (!module.isChunkFrozen(event.block.chunk.chunkKey)) return; if (!module.isRedstoneSignalAffected()) return
        event.newCurrent = event.oldCurrent
    }
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true) fun onPistonExtend(event: BlockPistonExtendEvent) {
        if (!module.isFreezeActive() || !module.isChunkFrozen(event.block.chunk.chunkKey) || !module.isPistonExtendAffected()) return; event.isCancelled = true
    }
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true) fun onPistonRetract(event: BlockPistonRetractEvent) {
        if (!module.isFreezeActive() || !module.isChunkFrozen(event.block.chunk.chunkKey) || !module.isPistonRetractAffected()) return; event.isCancelled = true
    }
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true) fun onBlockPhysics(event: BlockPhysicsEvent) {
        if (!module.isFreezeActive() || !module.isChunkFrozen(event.block.chunk.chunkKey) || !module.isBlockPhysicsAffected()) return; event.isCancelled = true
    }
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true) fun onTNTPrime(event: TNTPrimeEvent) {
        if (!module.isFreezeActive() || !module.isChunkFrozen(event.block.chunk.chunkKey) || !module.isTntPrimeAffected()) return; event.isCancelled = true
    }
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true) fun onExplosionPrime(event: ExplosionPrimeEvent) {
        if (!module.isFreezeActive() || !module.isChunkFrozen(event.entity.location.chunk.chunkKey) || !module.isExplosionAffected()) return; event.isCancelled = true
    }
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true) fun onEntitySpawn(event: EntitySpawnEvent) {
        if (event.entityType != EntityType.TNT) return
        if (!module.isFreezeActive() || !module.isChunkFrozen(event.location.chunk.chunkKey) || !module.isTntSpawnAffected()) return; event.isCancelled = true
    }
}

class RedstoneFreezeModuleCommand(private val module: RedstoneFreezeModule) : SubCommandHandler {
    override fun handle(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!module.isEnabled()) { sender.sendMessage(Component.text("红石冻结功能已禁用").color(NamedTextColor.RED)); return true }
        if (args.isEmpty()) { sendUsage(sender); return true }
        when (args[0].lowercase()) {
            "cancel" -> handleCancel(sender); "info" -> handleInfo(sender); "update" -> handleUpdate(sender, args); else -> handleFreeze(sender, args[0])
        }
        return true
    }

    private fun handleFreeze(sender: CommandSender, radiusArg: String) {
        if (sender !is Player) { sender.sendMessage(Component.text("该命令只能由玩家执行").color(NamedTextColor.RED)); return }
        if (!sender.hasPermission("tsl.redfreeze.use")) { sender.sendMessage(Component.text("无权限").color(NamedTextColor.RED)); return }
        val radius = radiusArg.toIntOrNull()
        if (radius == null || radius < 1) { sender.sendMessage(Component.text("请输入有效的半径").color(NamedTextColor.RED)); return }
        if (radius > module.getMaxRadius()) { sender.sendMessage(Component.text("半径不能超过 ${module.getMaxRadius()}").color(NamedTextColor.RED)); return }
        sender.scheduler.run(module.getPlugin(), { _ ->
            val frozenCount = module.activateFreeze(sender, radius)
            sender.sendMessage(Component.text("已冻结 ").color(NamedTextColor.GREEN).append(Component.text("$frozenCount").color(NamedTextColor.AQUA)).append(Component.text(" 个区块的红石活动").color(NamedTextColor.GREEN)))
        }, null)
    }

    private fun handleCancel(sender: CommandSender) {
        if (!sender.hasPermission("tsl.redfreeze.use")) { sender.sendMessage(Component.text("无权限").color(NamedTextColor.RED)); return }
        if (!module.isFreezeActive()) { sender.sendMessage(Component.text("当前没有活跃的冻结区域").color(NamedTextColor.YELLOW)); return }
        val releasedCount = module.cancelFreeze()
        sender.sendMessage(Component.text("已取消冻结，释放 ").color(NamedTextColor.GREEN).append(Component.text("$releasedCount").color(NamedTextColor.AQUA)).append(Component.text(" 个区块").color(NamedTextColor.GREEN)))
    }

    private fun handleUpdate(sender: CommandSender, args: Array<out String>) {
        if (sender !is Player) { sender.sendMessage(Component.text("该命令只能由玩家执行").color(NamedTextColor.RED)); return }
        if (!sender.hasPermission("tsl.redfreeze.use")) { sender.sendMessage(Component.text("无权限").color(NamedTextColor.RED)); return }
        val radius = args.getOrNull(1)?.toIntOrNull() ?: 3
        sender.sendMessage(Component.text("正在更新...").color(NamedTextColor.YELLOW))
        module.triggerPistonUpdates(sender, radius) { updatedCount ->
            sender.sendMessage(Component.text("已更新 ").color(NamedTextColor.GREEN).append(Component.text("$updatedCount").color(NamedTextColor.AQUA)).append(Component.text(" 个活塞").color(NamedTextColor.GREEN)))
        }
    }

    private fun handleInfo(sender: CommandSender) {
        if (!sender.hasPermission("tsl.redfreeze.use")) { sender.sendMessage(Component.text("无权限").color(NamedTextColor.RED)); return }
        val info = module.getFreezeInfo()
        if (info == null) sender.sendMessage(Component.text("当前没有活跃的冻结区域").color(NamedTextColor.YELLOW))
        else sender.sendMessage(Component.text("当前冻结信息: ").color(NamedTextColor.GREEN).append(Component.text(info).color(NamedTextColor.AQUA)))
    }

    private fun sendUsage(sender: CommandSender) {
        sender.sendMessage(Component.text("用法:").color(NamedTextColor.YELLOW))
        sender.sendMessage(Component.text("  /tsl redfreeze <半径> - 冻结区块").color(NamedTextColor.GRAY))
        sender.sendMessage(Component.text("  /tsl redfreeze cancel - 取消冻结").color(NamedTextColor.GRAY))
        sender.sendMessage(Component.text("  /tsl redfreeze update [半径] - 触发活塞更新").color(NamedTextColor.GRAY))
        sender.sendMessage(Component.text("  /tsl redfreeze info - 查看冻结信息").color(NamedTextColor.GRAY))
    }

    override fun tabComplete(sender: CommandSender, command: Command, label: String, args: Array<out String>): List<String> {
        if (args.size == 1) return listOf("cancel", "info", "update", "1", "5", "10", "16", "32").filter { it.startsWith(args[0].lowercase()) }
        if (args.size == 2 && args[0].lowercase() == "update") return listOf("1", "3", "5", "8").filter { it.startsWith(args[1]) }
        return emptyList()
    }

    override fun getDescription(): String = "冻结指定区域的红石活动"
}
