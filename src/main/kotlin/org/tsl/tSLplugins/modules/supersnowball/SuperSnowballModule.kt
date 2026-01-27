package org.tsl.tSLplugins.modules.supersnowball

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.*
import org.bukkit.block.data.type.Snow
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.*
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.entity.ProjectileLaunchEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.util.Vector
import org.tsl.tSLplugins.SubCommandHandler
import org.tsl.tSLplugins.core.AbstractModule
import java.util.*
import kotlin.random.Random

/**
 * SuperSnowball 模块 - 超级大雪球
 */
class SuperSnowballModule : AbstractModule() {

    override val id = "super-snowball"
    override val configPath = "super-snowball"

    lateinit var SUPER_SNOWBALL_KEY: NamespacedKey private set

    // 配置项
    private var snowRadius: Int = 5
    private var knockbackRadius: Double = 7.0
    private var knockbackStrength: Double = 1.5
    private var gravity: Double = 0.06
    private var velocityMultiplier: Double = 0.6
    private var trailParticleCount: Int = 3
    private var trailParticleInterval: Long = 2L
    private var impactParticleCount: Int = 50
    private var snowLayerChance: Double = 0.7
    private var maxSnowLayers: Int = 3
    private var freezeTicks: Int = 100
    private var useCustomModel: Boolean = true
    private var customModelKey: String = "cris_tsl:big_snow_ball"
    private var impactSounds: List<SoundConfig> = listOf()

    private val trackedSnowballs: MutableSet<UUID> = Collections.synchronizedSet(mutableSetOf())
    private lateinit var listener: SuperSnowballModuleListener

    data class SoundConfig(val sound: Sound, val volume: Float, val pitch: Float)

    override fun doEnable() {
        SUPER_SNOWBALL_KEY = NamespacedKey(context.plugin, "is_super")
        loadSnowballConfig()
        listener = SuperSnowballModuleListener(this)
        registerListener(listener)
    }

    override fun doReload() {
        loadSnowballConfig()
    }

    override fun getCommandHandler(): SubCommandHandler = SuperSnowballModuleCommand(this)
    override fun getDescription(): String = "超级大雪球"

    private fun loadSnowballConfig() {
        snowRadius = getConfigInt("snow-radius", 5)
        knockbackRadius = getConfigDouble("knockback-radius", 7.0)
        knockbackStrength = getConfigDouble("knockback-strength", 1.5)
        gravity = getConfigDouble("gravity", 0.06)
        velocityMultiplier = getConfigDouble("velocity-multiplier", 0.6)
        trailParticleCount = getConfigInt("trail-particle-count", 3)
        trailParticleInterval = getConfigLong("trail-particle-interval", 2L)
        impactParticleCount = getConfigInt("impact-particle-count", 50)
        snowLayerChance = getConfigDouble("snow-layer-chance", 0.7)
        maxSnowLayers = getConfigInt("max-snow-layers", 3)
        freezeTicks = getConfigInt("freeze-ticks", 100)
        useCustomModel = getConfigBoolean("use-custom-model", true)
        customModelKey = getConfigString("custom-model-key", "cris_tsl:big_snow_ball")
        impactSounds = loadSounds(getConfigStringList("impact-sounds"))
        logInfo("配置已加载 - 覆雪半径: $snowRadius, 击飞半径: $knockbackRadius")
    }

    private fun loadSounds(soundList: List<String>): List<SoundConfig> {
        if (soundList.isEmpty()) return listOf(
            SoundConfig(Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.5f, 1.0f),
            SoundConfig(Sound.ENTITY_WIND_CHARGE_WIND_BURST, 1.0f, 1.2f)
        )
        return soundList.mapNotNull { line ->
            try {
                val parts = line.split(":")
                SoundConfig(Sound.valueOf(parts[0].uppercase()), parts.getOrNull(1)?.toFloatOrNull() ?: 1.0f, parts.getOrNull(2)?.toFloatOrNull() ?: 1.0f)
            } catch (e: Exception) { null }
        }
    }

    fun isSuperSnowball(item: ItemStack?): Boolean {
        if (item == null || item.type != Material.SNOWBALL) return false
        return item.itemMeta?.persistentDataContainer?.has(SUPER_SNOWBALL_KEY, PersistentDataType.BYTE) ?: false
    }

    fun createSuperSnowball(): ItemStack {
        val item = ItemStack(Material.SNOWBALL, 1)
        val meta = item.itemMeta ?: return item
        meta.displayName(Component.text("超级大雪球").color(NamedTextColor.AQUA).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false))
        meta.lore(listOf(Component.text("❄ 投掷后产生大范围覆雪效果").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                         Component.text("❄ 将附近实体击飞").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)))
        meta.persistentDataContainer.set(SUPER_SNOWBALL_KEY, PersistentDataType.BYTE, 1)
        if (useCustomModel) NamespacedKey.fromString(customModelKey)?.let { meta.setItemModel(it) }
        meta.setMaxStackSize(16)
        item.itemMeta = meta
        return item
    }

    // Getters for listener
    fun getSnowRadius() = snowRadius
    fun getKnockbackRadius() = knockbackRadius
    fun getKnockbackStrength() = knockbackStrength
    fun getGravity() = gravity
    fun getVelocityMultiplier() = velocityMultiplier
    fun getTrailParticleCount() = trailParticleCount
    fun getTrailParticleInterval() = trailParticleInterval
    fun getImpactParticleCount() = impactParticleCount
    fun getSnowLayerChance() = snowLayerChance
    fun getMaxSnowLayers() = maxSnowLayers
    fun getFreezeTicks() = freezeTicks
    fun getImpactSounds() = impactSounds
    fun getTrackedSnowballs() = trackedSnowballs
    fun getPlugin() = context.plugin
}

class SuperSnowballModuleListener(private val module: SuperSnowballModule) : Listener {
    @EventHandler(priority = EventPriority.HIGH)
    fun onProjectileLaunch(event: ProjectileLaunchEvent) {
        if (!module.isEnabled()) return
        val projectile = event.entity
        if (projectile !is Snowball) return
        val shooter = projectile.shooter as? Player ?: return
        if (!module.isSuperSnowball(shooter.inventory.itemInMainHand)) return
        projectile.persistentDataContainer.set(module.SUPER_SNOWBALL_KEY, PersistentDataType.BYTE, 1)
        projectile.velocity = projectile.velocity.multiply(module.getVelocityMultiplier())
        startTrailParticles(projectile)
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onProjectileHit(event: ProjectileHitEvent) {
        if (!module.isEnabled()) return
        val projectile = event.entity
        if (projectile !is Snowball) return
        if (!projectile.persistentDataContainer.has(module.SUPER_SNOWBALL_KEY, PersistentDataType.BYTE)) return
        module.getTrackedSnowballs().remove(projectile.uniqueId)
        val location = projectile.location
        Bukkit.getRegionScheduler().run(module.getPlugin(), location) { _ ->
            createSnowCover(location)
            knockbackEntities(location, projectile.shooter as? Entity)
            spawnImpactParticles(location)
            playImpactSound(location)
        }
    }

    private fun startTrailParticles(snowball: Snowball) {
        module.getTrackedSnowballs().add(snowball.uniqueId)
        Bukkit.getGlobalRegionScheduler().runAtFixedRate(module.getPlugin(), { task ->
            if (!snowball.isValid || snowball.isDead || !module.getTrackedSnowballs().contains(snowball.uniqueId)) {
                task.cancel(); module.getTrackedSnowballs().remove(snowball.uniqueId); return@runAtFixedRate
            }
            val loc = snowball.location
            val world = loc.world ?: return@runAtFixedRate
            repeat(module.getTrailParticleCount()) {
                world.spawnParticle(Particle.SNOWFLAKE, loc.x + (Random.nextDouble() - 0.5) * 0.5, loc.y + (Random.nextDouble() - 0.5) * 0.5, loc.z + (Random.nextDouble() - 0.5) * 0.5, 1, 0.0, 0.0, 0.0, 0.0)
            }
            snowball.velocity = Vector(snowball.velocity.x, snowball.velocity.y - module.getGravity(), snowball.velocity.z)
        }, module.getTrailParticleInterval(), module.getTrailParticleInterval())
    }

    private fun createSnowCover(center: Location) {
        val world = center.world ?: return
        val radius = module.getSnowRadius()
        for (x in -radius..radius) {
            for (z in -radius..radius) {
                if (x * x + z * z > radius * radius) continue
                if (Random.nextDouble() > module.getSnowLayerChance()) continue
                val checkX = center.blockX + x
                val checkZ = center.blockZ + z
                for (y in center.blockY downTo (center.blockY - radius - 3).coerceAtLeast(world.minHeight)) {
                    val block = world.getBlockAt(checkX, y, checkZ)
                    val blockAbove = world.getBlockAt(checkX, y + 1, checkZ)
                    if (block.type.isSolid && (blockAbove.type == Material.AIR || blockAbove.type == Material.SNOW)) {
                        when (blockAbove.type) {
                            Material.AIR, Material.CAVE_AIR -> {
                                blockAbove.type = Material.SNOW
                                (blockAbove.blockData as? Snow)?.let { it.layers = 1; blockAbove.blockData = it }
                            }
                            Material.SNOW -> (blockAbove.blockData as? Snow)?.let { if (it.layers < module.getMaxSnowLayers()) { it.layers = (it.layers + 1).coerceAtMost(it.maximumLayers); blockAbove.blockData = it } }
                            else -> {}
                        }
                        break
                    }
                }
            }
        }
    }

    private fun knockbackEntities(center: Location, shooter: Entity?) {
        val world = center.world ?: return
        val radius = module.getKnockbackRadius()
        world.getNearbyEntities(center, radius, radius, radius).forEach { entity ->
            if (entity !is LivingEntity) return@forEach
            val entityLoc = entity.location.add(0.0, entity.height / 2, 0.0)
            val direction = entityLoc.toVector().subtract(center.toVector())
            val distance = direction.length()
            if (distance > radius) return@forEach
            val distanceFactor = (1.0 - distance / radius).coerceIn(0.3, 1.0)
            val knockbackPower = module.getKnockbackStrength() * distanceFactor
            direction.normalize().multiply(knockbackPower)
            direction.y = (direction.y + 0.4 * distanceFactor).coerceIn(0.2, 1.2)
            if (entity is Player) entity.scheduler.run(module.getPlugin(), { _ -> entity.velocity = direction; entity.freezeTicks = (entity.freezeTicks + module.getFreezeTicks()).coerceAtMost(entity.maxFreezeTicks + 40) }, null)
            else { entity.velocity = direction; entity.freezeTicks = (entity.freezeTicks + module.getFreezeTicks()).coerceAtMost(entity.maxFreezeTicks + 40) }
        }
    }

    private fun spawnImpactParticles(location: Location) {
        val world = location.world ?: return
        world.spawnParticle(Particle.SNOWFLAKE, location, module.getImpactParticleCount(), 2.5, 1.5, 2.5, 0.15)
        world.spawnParticle(Particle.CLOUD, location, module.getImpactParticleCount() / 2, 2.0, 0.8, 2.0, 0.08)
    }

    private fun playImpactSound(location: Location) {
        val world = location.world ?: return
        module.getImpactSounds().forEach { world.playSound(location, it.sound, SoundCategory.BLOCKS, it.volume, it.pitch) }
    }
}

class SuperSnowballModuleCommand(private val module: SuperSnowballModule) : SubCommandHandler {
    private val serializer = LegacyComponentSerializer.legacyAmpersand()
    override fun handle(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!module.isEnabled()) { sender.sendMessage(serializer.deserialize("&c超级大雪球功能已禁用")); return true }
        if (args.isEmpty() || args[0].lowercase() != "give") { showHelp(sender); return true }
        handleGive(sender, args)
        return true
    }

    private fun handleGive(sender: CommandSender, args: Array<out String>) {
        if (!sender.hasPermission("tsl.ss.give")) { sender.sendMessage(serializer.deserialize("&c没有权限")); return }
        var targetPlayer: Player? = null
        var amount = 1
        when {
            args.size >= 3 -> { targetPlayer = Bukkit.getPlayer(args[1]) ?: run { sender.sendMessage(serializer.deserialize("&c玩家不在线")); return }; amount = args[2].toIntOrNull() ?: 1 }
            args.size == 2 -> { val asAmount = args[1].toIntOrNull(); if (asAmount != null) { if (sender !is Player) { sender.sendMessage(serializer.deserialize("&c控制台必须指定玩家")); return }; targetPlayer = sender; amount = asAmount } else { targetPlayer = Bukkit.getPlayer(args[1]) ?: run { sender.sendMessage(serializer.deserialize("&c玩家不在线")); return } } }
            else -> { if (sender !is Player) { sender.sendMessage(serializer.deserialize("&c控制台必须指定玩家")); return }; targetPlayer = sender }
        }
        amount = amount.coerceIn(1, 64)
        val snowball = module.createSuperSnowball()
        snowball.amount = amount
        val remaining = targetPlayer.inventory.addItem(snowball)
        val given = amount - (remaining.values.firstOrNull()?.amount ?: 0)
        if (given > 0) sender.sendMessage(serializer.deserialize("&a已给予 &f${targetPlayer.name} &b$given &a个超级大雪球"))
        else sender.sendMessage(serializer.deserialize("&c背包已满"))
    }

    private fun showHelp(sender: CommandSender) {
        sender.sendMessage(serializer.deserialize("&6===== 超级大雪球 ====="))
        sender.sendMessage(serializer.deserialize("&e/tsl ss give [玩家] [数量] &7- 给予超级大雪球"))
    }

    override fun tabComplete(sender: CommandSender, command: Command, label: String, args: Array<out String>): List<String> {
        return when (args.size) {
            1 -> listOf("give").filter { it.startsWith(args[0], true) }
            2 -> if (args[0].equals("give", true)) (Bukkit.getOnlinePlayers().map { it.name } + listOf("1", "16", "64")).filter { it.startsWith(args[1], true) } else emptyList()
            3 -> if (args[0].equals("give", true)) listOf("1", "16", "64").filter { it.startsWith(args[2], true) } else emptyList()
            else -> emptyList()
        }
    }

    override fun getDescription(): String = "超级大雪球功能"
}
