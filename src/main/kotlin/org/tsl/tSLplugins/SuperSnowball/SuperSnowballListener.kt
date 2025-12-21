package org.tsl.tSLplugins.SuperSnowball

import org.bukkit.*
import org.bukkit.block.data.type.Snow
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.Snowball
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.entity.ProjectileLaunchEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.util.Vector
import java.util.Collections
import java.util.UUID
import kotlin.random.Random

/**
 * 超级大雪球监听器
 * 处理投掷轨迹粒子、落地效果（覆雪、击飞、音效）
 */
class SuperSnowballListener(
    private val plugin: JavaPlugin,
    private val manager: SuperSnowballManager
) : Listener {

    // 追踪超级雪球的实体UUID（使用同步Set避免ConcurrentHashMap不允许null值的问题）
    private val trackedSnowballs: MutableSet<UUID> = Collections.synchronizedSet(mutableSetOf())

    @EventHandler(priority = EventPriority.HIGH)
    fun onProjectileLaunch(event: ProjectileLaunchEvent) {
        if (!manager.isEnabled()) return

        val projectile = event.entity
        if (projectile !is Snowball) return

        val shooter = projectile.shooter as? Player ?: return

        // 检查是否是超级大雪球（通过检查玩家手持物品的 PDC）
        val item = shooter.inventory.itemInMainHand
        if (!manager.isSuperSnowball(item)) return

        // 标记这个雪球为超级雪球
        projectile.persistentDataContainer.set(
            SuperSnowballManager.SUPER_SNOWBALL_KEY,
            PersistentDataType.BYTE,
            1
        )

        // 调整速度（更慢、下落更大）
        val velocity = projectile.velocity
        projectile.velocity = velocity.multiply(manager.getVelocityMultiplier())

        // 启动轨迹粒子任务
        startTrailParticles(projectile)
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onProjectileHit(event: ProjectileHitEvent) {
        if (!manager.isEnabled()) return

        val projectile = event.entity
        if (projectile !is Snowball) return

        // 检查是否是超级大雪球
        if (!projectile.persistentDataContainer.has(SuperSnowballManager.SUPER_SNOWBALL_KEY, PersistentDataType.BYTE)) {
            return
        }

        // 停止轨迹粒子
        stopTrailParticles(projectile.uniqueId)

        val location = projectile.location
        val world = location.world ?: return

        // 使用 Folia 调度器在正确的区域执行
        Bukkit.getRegionScheduler().run(plugin, location) { _ ->
            // 产生覆雪效果
            createSnowCover(location)

            // 产生击飞效果
            knockbackEntities(location, projectile.shooter as? Entity)

            // 产生粒子特效
            spawnImpactParticles(location)

            // 播放音效
            playImpactSound(location)
        }
    }


    /**
     * 启动轨迹粒子效果
     */
    private fun startTrailParticles(snowball: Snowball) {
        // 记录这个雪球正在被追踪
        val snowballId = snowball.uniqueId
        trackedSnowballs.add(snowballId)

        Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, { scheduledTask ->
            // 检查雪球是否还有效
            if (!snowball.isValid || snowball.isDead || !trackedSnowballs.contains(snowballId)) {
                scheduledTask.cancel()
                trackedSnowballs.remove(snowballId)
                return@runAtFixedRate
            }

            val loc = snowball.location
            val world = loc.world ?: return@runAtFixedRate

            // 生成冬日氛围粒子（控制数量以保证性能）
            repeat(manager.getTrailParticleCount()) {
                val offsetX = (Random.nextDouble() - 0.5) * 0.5
                val offsetY = (Random.nextDouble() - 0.5) * 0.5
                val offsetZ = (Random.nextDouble() - 0.5) * 0.5

                world.spawnParticle(
                    Particle.SNOWFLAKE,
                    loc.x + offsetX,
                    loc.y + offsetY,
                    loc.z + offsetZ,
                    1, 0.0, 0.0, 0.0, 0.0
                )
            }

            // 少量白色粒子增加效果
            world.spawnParticle(
                Particle.WHITE_ASH,
                loc.x, loc.y, loc.z,
                2, 0.2, 0.2, 0.2, 0.0
            )

            // 手动应用额外重力（模拟大雪球下落更快）
            val velocity = snowball.velocity
            snowball.velocity = Vector(
                velocity.x,
                velocity.y - manager.getGravity(),
                velocity.z
            )

        }, manager.getTrailParticleInterval(), manager.getTrailParticleInterval())
    }

    /**
     * 停止轨迹粒子
     */
    private fun stopTrailParticles(uuid: UUID) {
        trackedSnowballs.remove(uuid)
    }

    /**
     * 创建覆雪效果（从雪球位置向下搜索，支持洞穴）
     */
    private fun createSnowCover(center: Location) {
        val world = center.world ?: return
        val radius = manager.getSnowRadius()
        val chance = manager.getSnowLayerChance()
        val maxLayers = manager.getMaxSnowLayers()
        val centerY = center.blockY

        for (x in -radius..radius) {
            for (z in -radius..radius) {
                // 圆形范围检查
                if (x * x + z * z > radius * radius) continue

                // 概率检查
                if (Random.nextDouble() > chance) continue

                val checkX = center.blockX + x
                val checkZ = center.blockZ + z

                // 从雪球位置向上和向下搜索第一个可放置雪的地面
                var groundY = -1
                
                // 先向上搜索（处理比落点高的地形）
                for (y in centerY..(centerY + radius + 3).coerceAtMost(world.maxHeight - 1)) {
                    val block = world.getBlockAt(checkX, y, checkZ)
                    val blockAbove = world.getBlockAt(checkX, y + 1, checkZ)
                    // 找到固体方块且上方是空气或雪
                    if (block.type.isSolid && (blockAbove.type == Material.AIR || blockAbove.type == Material.CAVE_AIR || blockAbove.type == Material.SNOW)) {
                        groundY = y
                        break
                    }
                }
                
                // 如果向上没找到，再向下搜索
                if (groundY < 0) {
                    for (y in (centerY - 1) downTo (centerY - radius - 3).coerceAtLeast(world.minHeight)) {
                        val block = world.getBlockAt(checkX, y, checkZ)
                        val blockAbove = world.getBlockAt(checkX, y + 1, checkZ)
                        // 找到固体方块且上方是空气或雪
                        if (block.type.isSolid && (blockAbove.type == Material.AIR || blockAbove.type == Material.CAVE_AIR || blockAbove.type == Material.SNOW)) {
                            groundY = y
                            break
                        }
                    }
                }

                // 未找到固体方块，跳过
                if (groundY < 0) continue

                val groundBlock = world.getBlockAt(checkX, groundY, checkZ)
                val targetBlock = world.getBlockAt(checkX, groundY + 1, checkZ)

                // 检查地面是否可以放置雪
                if (!canPlaceSnowOn(groundBlock.type)) continue

                // 检查目标位置
                when (targetBlock.type) {
                    Material.AIR, Material.CAVE_AIR -> {
                        // 空气，放置雪（限制在配置的最大层数内）
                        targetBlock.type = Material.SNOW
                        val snowData = targetBlock.blockData as? Snow
                        if (snowData != null) {
                            snowData.layers = 1.coerceAtMost(maxLayers).coerceAtMost(snowData.maximumLayers)
                            targetBlock.blockData = snowData
                        }
                    }
                    Material.SNOW -> {
                        // 已有雪，增加层数（但不超过配置的最大层数）
                        val snowData = targetBlock.blockData as? Snow
                        if (snowData != null && snowData.layers < maxLayers) {
                            val newLayers = (snowData.layers + 1)
                                .coerceAtMost(maxLayers)
                                .coerceAtMost(snowData.maximumLayers)
                            snowData.layers = newLayers
                            targetBlock.blockData = snowData
                        }
                    }
                    else -> {
                        // 其他方块，不处理
                    }
                }
            }
        }
    }

    /**
     * 检查方块是否可以放置雪（符合原版规则）
     */
    private fun canPlaceSnowOn(material: Material): Boolean {
        // 明确不能放置雪的方块（原版规则）
        val blacklist = setOf(
            // 栅栏类
            Material.OAK_FENCE, Material.SPRUCE_FENCE, Material.BIRCH_FENCE,
            Material.JUNGLE_FENCE, Material.ACACIA_FENCE, Material.DARK_OAK_FENCE,
            Material.MANGROVE_FENCE, Material.CHERRY_FENCE, Material.BAMBOO_FENCE,
            Material.CRIMSON_FENCE, Material.WARPED_FENCE, Material.NETHER_BRICK_FENCE,
            // 栅栏门
            Material.OAK_FENCE_GATE, Material.SPRUCE_FENCE_GATE, Material.BIRCH_FENCE_GATE,
            Material.JUNGLE_FENCE_GATE, Material.ACACIA_FENCE_GATE, Material.DARK_OAK_FENCE_GATE,
            Material.MANGROVE_FENCE_GATE, Material.CHERRY_FENCE_GATE, Material.BAMBOO_FENCE_GATE,
            Material.CRIMSON_FENCE_GATE, Material.WARPED_FENCE_GATE,
            // 墙
            Material.COBBLESTONE_WALL, Material.MOSSY_COBBLESTONE_WALL, Material.BRICK_WALL,
            Material.STONE_BRICK_WALL, Material.MOSSY_STONE_BRICK_WALL, Material.NETHER_BRICK_WALL,
            Material.RED_NETHER_BRICK_WALL, Material.SANDSTONE_WALL, Material.RED_SANDSTONE_WALL,
            Material.PRISMARINE_WALL, Material.ANDESITE_WALL, Material.DIORITE_WALL,
            Material.GRANITE_WALL, Material.DEEPSLATE_BRICK_WALL, Material.DEEPSLATE_TILE_WALL,
            Material.POLISHED_DEEPSLATE_WALL, Material.COBBLED_DEEPSLATE_WALL, Material.BLACKSTONE_WALL,
            Material.POLISHED_BLACKSTONE_WALL, Material.POLISHED_BLACKSTONE_BRICK_WALL, Material.END_STONE_BRICK_WALL,
            Material.MUD_BRICK_WALL,
            // 炼药锅、堆肥桶、炼金台等
            Material.CAULDRON, Material.WATER_CAULDRON, Material.LAVA_CAULDRON, Material.POWDER_SNOW_CAULDRON,
            Material.COMPOSTER, Material.BREWING_STAND, Material.HOPPER,
            Material.BELL, Material.GRINDSTONE, Material.STONECUTTER, Material.LECTERN,
            Material.ANVIL, Material.CHIPPED_ANVIL, Material.DAMAGED_ANVIL,
            // 灯类
            Material.TORCH, Material.WALL_TORCH, Material.SOUL_TORCH, Material.SOUL_WALL_TORCH,
            Material.REDSTONE_TORCH, Material.REDSTONE_WALL_TORCH, Material.LANTERN, Material.SOUL_LANTERN,
            // 花、草、作物
            Material.DANDELION, Material.POPPY, Material.BLUE_ORCHID, Material.ALLIUM,
            Material.AZURE_BLUET, Material.RED_TULIP, Material.ORANGE_TULIP, Material.WHITE_TULIP,
            Material.PINK_TULIP, Material.OXEYE_DAISY, Material.CORNFLOWER, Material.LILY_OF_THE_VALLEY,
            Material.WITHER_ROSE, Material.SUNFLOWER, Material.LILAC, Material.ROSE_BUSH, Material.PEONY,
            Material.SHORT_GRASS, Material.TALL_GRASS, Material.FERN, Material.LARGE_FERN,
            Material.DEAD_BUSH, Material.SEAGRASS, Material.TALL_SEAGRASS,
            Material.SWEET_BERRY_BUSH, Material.WHEAT, Material.CARROTS, Material.POTATOES, Material.BEETROOTS,
            Material.MELON_STEM, Material.PUMPKIN_STEM, Material.ATTACHED_MELON_STEM, Material.ATTACHED_PUMPKIN_STEM,
            Material.BAMBOO_SAPLING, Material.BAMBOO,
            // 树苗
            Material.OAK_SAPLING, Material.SPRUCE_SAPLING, Material.BIRCH_SAPLING,
            Material.JUNGLE_SAPLING, Material.ACACIA_SAPLING, Material.DARK_OAK_SAPLING,
            Material.CHERRY_SAPLING, Material.MANGROVE_PROPAGULE,
            // 铁轨、红石元件
            Material.RAIL, Material.POWERED_RAIL, Material.DETECTOR_RAIL, Material.ACTIVATOR_RAIL,
            Material.REDSTONE_WIRE, Material.REPEATER, Material.COMPARATOR,
            Material.LEVER, Material.TRIPWIRE, Material.TRIPWIRE_HOOK,
            // 压力板、按钮
            Material.STONE_PRESSURE_PLATE, Material.OAK_PRESSURE_PLATE, Material.SPRUCE_PRESSURE_PLATE,
            Material.BIRCH_PRESSURE_PLATE, Material.JUNGLE_PRESSURE_PLATE, Material.ACACIA_PRESSURE_PLATE,
            Material.DARK_OAK_PRESSURE_PLATE, Material.MANGROVE_PRESSURE_PLATE, Material.CHERRY_PRESSURE_PLATE,
            Material.BAMBOO_PRESSURE_PLATE, Material.CRIMSON_PRESSURE_PLATE, Material.WARPED_PRESSURE_PLATE,
            Material.LIGHT_WEIGHTED_PRESSURE_PLATE, Material.HEAVY_WEIGHTED_PRESSURE_PLATE, Material.POLISHED_BLACKSTONE_PRESSURE_PLATE,
            Material.STONE_BUTTON, Material.OAK_BUTTON, Material.SPRUCE_BUTTON, Material.BIRCH_BUTTON,
            Material.JUNGLE_BUTTON, Material.ACACIA_BUTTON, Material.DARK_OAK_BUTTON,
            Material.MANGROVE_BUTTON, Material.CHERRY_BUTTON, Material.BAMBOO_BUTTON,
            Material.CRIMSON_BUTTON, Material.WARPED_BUTTON, Material.POLISHED_BLACKSTONE_BUTTON,
            // 其他
            Material.FLOWER_POT, Material.SKELETON_SKULL, Material.WITHER_SKELETON_SKULL,
            Material.ZOMBIE_HEAD, Material.CREEPER_HEAD, Material.DRAGON_HEAD, Material.PIGLIN_HEAD,
            Material.PLAYER_HEAD, Material.ARMOR_STAND, Material.END_ROD,
            Material.LIGHTNING_ROD, Material.POINTED_DRIPSTONE, Material.AMETHYST_CLUSTER,
            Material.SMALL_AMETHYST_BUD, Material.MEDIUM_AMETHYST_BUD, Material.LARGE_AMETHYST_BUD,
            Material.CHAIN, Material.IRON_BARS,
            // 告示牌、床、门、活板门
            Material.OAK_SIGN, Material.SPRUCE_SIGN, Material.BIRCH_SIGN, Material.JUNGLE_SIGN,
            Material.ACACIA_SIGN, Material.DARK_OAK_SIGN, Material.MANGROVE_SIGN, Material.CHERRY_SIGN,
            Material.BAMBOO_SIGN, Material.CRIMSON_SIGN, Material.WARPED_SIGN,
            Material.OAK_WALL_SIGN, Material.SPRUCE_WALL_SIGN, Material.BIRCH_WALL_SIGN,
            Material.OAK_HANGING_SIGN, Material.SPRUCE_HANGING_SIGN, Material.BIRCH_HANGING_SIGN,
            // 地毯
            Material.WHITE_CARPET, Material.ORANGE_CARPET, Material.MAGENTA_CARPET, Material.LIGHT_BLUE_CARPET,
            Material.YELLOW_CARPET, Material.LIME_CARPET, Material.PINK_CARPET, Material.GRAY_CARPET,
            Material.LIGHT_GRAY_CARPET, Material.CYAN_CARPET, Material.PURPLE_CARPET, Material.BLUE_CARPET,
            Material.BROWN_CARPET, Material.GREEN_CARPET, Material.RED_CARPET, Material.BLACK_CARPET, Material.MOSS_CARPET,
            // 特殊方块
            Material.WATER, Material.LAVA, Material.FIRE, Material.SOUL_FIRE,
            Material.COBWEB, Material.LADDER, Material.VINE, Material.GLOW_LICHEN,
            Material.SCULK_VEIN, Material.SNOW, Material.POWDER_SNOW
        )

        if (blacklist.contains(material)) return false

        // 可以放置雪的特殊方块
        val whitelist = setOf(
            // 树叶
            Material.OAK_LEAVES, Material.SPRUCE_LEAVES, Material.BIRCH_LEAVES,
            Material.JUNGLE_LEAVES, Material.ACACIA_LEAVES, Material.DARK_OAK_LEAVES,
            Material.MANGROVE_LEAVES, Material.CHERRY_LEAVES, Material.AZALEA_LEAVES,
            Material.FLOWERING_AZALEA_LEAVES,
            // 脚手架
            Material.SCAFFOLDING,
            // 陷阱门
            Material.OAK_TRAPDOOR, Material.SPRUCE_TRAPDOOR, Material.BIRCH_TRAPDOOR,
            Material.JUNGLE_TRAPDOOR, Material.ACACIA_TRAPDOOR, Material.DARK_OAK_TRAPDOOR,
            Material.MANGROVE_TRAPDOOR, Material.CHERRY_TRAPDOOR, Material.BAMBOO_TRAPDOOR,
            Material.CRIMSON_TRAPDOOR, Material.WARPED_TRAPDOOR, Material.IRON_TRAPDOOR,
            // 台阶（顶部）
            Material.STONE_STAIRS, Material.COBBLESTONE_STAIRS, Material.OAK_STAIRS,
            Material.SPRUCE_STAIRS, Material.BIRCH_STAIRS, Material.JUNGLE_STAIRS,
            Material.ACACIA_STAIRS, Material.DARK_OAK_STAIRS, Material.BRICK_STAIRS,
            // 半砖（顶部）
            Material.STONE_SLAB, Material.COBBLESTONE_SLAB, Material.OAK_SLAB,
            Material.SPRUCE_SLAB, Material.BIRCH_SLAB, Material.JUNGLE_SLAB,
            Material.ACACIA_SLAB, Material.DARK_OAK_SLAB, Material.BRICK_SLAB
        )

        if (whitelist.contains(material)) return true

        // 默认：实心方块可以放置雪
        return material.isSolid && material.isOccluding
    }

    /**
     * 击飞附近实体（手动计算范围冲击波，包括投掷者自己）
     */
    private fun knockbackEntities(center: Location, shooter: Entity?) {
        val world = center.world ?: return
        val radius = manager.getKnockbackRadius()
        val strength = manager.getKnockbackStrength()

        // 获取范围内所有实体并施加击飞（包括投掷者自己）
        world.getNearbyEntities(center, radius, radius, radius).forEach { entity ->
            // 只对生物实体生效
            if (entity !is LivingEntity) return@forEach

            // 计算从爆炸中心到实体的方向
            val entityLoc = entity.location.add(0.0, entity.height / 2, 0.0) // 使用实体中心
            val direction = entityLoc.toVector().subtract(center.toVector())
            val distance = direction.length()

            if (distance < 0.1) {
                // 实体在中心点，随机方向击飞
                direction.setX(Random.nextDouble() - 0.5)
                direction.setY(0.5)
                direction.setZ(Random.nextDouble() - 0.5)
            }

            if (distance > radius) return@forEach

            // 距离越近，击飞越强（类似TNT爆炸的衰减）
            val distanceFactor = (1.0 - distance / radius).coerceIn(0.3, 1.0)
            val knockbackPower = strength * distanceFactor

            // 归一化方向并应用力度
            direction.normalize()
            direction.multiply(knockbackPower)

            // 添加向上的分量（模拟爆炸冲击）
            direction.y = (direction.y + 0.4 * distanceFactor).coerceIn(0.2, 1.2)

            // 使用实体调度器应用速度（减少拉回问题）
            if (entity is Player) {
                entity.scheduler.run(plugin, { _ ->
                    entity.velocity = direction
                    // 应用冻伤效果
                    applyFreezeEffect(entity)
                }, null)
            } else {
                entity.velocity = direction
                // 应用冻伤效果
                applyFreezeEffect(entity)
            }
        }
    }

    /**
     * 应用冻伤效果（类似细雪）
     */
    private fun applyFreezeEffect(entity: LivingEntity) {
        val freezeTicks = manager.getFreezeTicks()
        if (freezeTicks <= 0) return

        // 设置冰冻刻度（满140刻开始受伤）
        val newFreezeTicks = (entity.freezeTicks + freezeTicks).coerceAtMost(entity.maxFreezeTicks + 40)
        entity.freezeTicks = newFreezeTicks
    }

    /**
     * 生成落地粒子效果（冬季主题）
     */
    private fun spawnImpactParticles(location: Location) {
        val world = location.world ?: return
        val count = manager.getImpactParticleCount()

        // 大量雪花爆发（核心效果）
        world.spawnParticle(
            Particle.SNOWFLAKE,
            location,
            count,
            2.5, 1.5, 2.5,
            0.15
        )

        // 白色云雾（营造暴风雪感）
        world.spawnParticle(
            Particle.CLOUD,
            location,
            count / 2,
            2.0, 0.8, 2.0,
            0.08
        )

        // 冰晶闪光效果
        world.spawnParticle(
            Particle.END_ROD,
            location,
            15,
            1.5, 1.0, 1.5,
            0.03
        )

        // 白色灰烬（飘散的雪）
        world.spawnParticle(
            Particle.WHITE_ASH,
            location,
            count,
            3.0, 2.0, 3.0,
            0.02
        )

        // 冲击波环形效果（使用音符粒子模拟冰蓝色）
        for (i in 0 until 16) {
            val angle = (i / 16.0) * 2 * Math.PI
            val ringX = location.x + Math.cos(angle) * 2.5
            val ringZ = location.z + Math.sin(angle) * 2.5
            world.spawnParticle(
                Particle.SNOWFLAKE,
                ringX, location.y + 0.5, ringZ,
                3, 0.1, 0.3, 0.1, 0.02
            )
        }

        // 向上喷射的雪花柱
        world.spawnParticle(
            Particle.SNOWFLAKE,
            location.clone().add(0.0, 0.5, 0.0),
            count / 3,
            0.5, 0.1, 0.5,
            0.3
        )
    }

    /**
     * 播放落地音效（用户可配置）
     */
    private fun playImpactSound(location: Location) {
        val world = location.world ?: return

        val sounds = manager.getImpactSounds()
        if (sounds.isEmpty()) {
            // 如果没有配置音效，使用硬编码的默认音效
            world.playSound(location, Sound.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.BLOCKS, 1.5f, 1.0f)
            world.playSound(location, Sound.ENTITY_WIND_CHARGE_WIND_BURST, SoundCategory.BLOCKS, 1.0f, 1.2f)
            return
        }

        // 播放配置的音效
        sounds.forEach { soundConfig ->
            world.playSound(location, soundConfig.sound, SoundCategory.BLOCKS, soundConfig.volume, soundConfig.pitch)
        }
    }
}
