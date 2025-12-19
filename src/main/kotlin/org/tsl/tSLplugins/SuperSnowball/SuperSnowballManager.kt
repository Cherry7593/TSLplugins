package org.tsl.tSLplugins.SuperSnowball

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin

/**
 * 超级大雪球管理器
 * 负责配置管理、雪球物品生成
 */
class SuperSnowballManager(private val plugin: JavaPlugin) {

    companion object {
        lateinit var SUPER_SNOWBALL_KEY: NamespacedKey
            private set
    }

    // 配置项
    private var enabled: Boolean = true
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
    private val messages: MutableMap<String, String> = mutableMapOf()

    // 音效配置数据类
    data class SoundConfig(
        val sound: Sound,
        val volume: Float,
        val pitch: Float
    )

    init {
        SUPER_SNOWBALL_KEY = NamespacedKey(plugin, "is_super")
        loadConfig()
    }

    fun loadConfig() {
        val config = plugin.config
        val section = config.getConfigurationSection("super-snowball") ?: return

        enabled = section.getBoolean("enabled", true)
        snowRadius = section.getInt("snow-radius", 5)
        knockbackRadius = section.getDouble("knockback-radius", 7.0)
        knockbackStrength = section.getDouble("knockback-strength", 1.5)
        gravity = section.getDouble("gravity", 0.06)
        velocityMultiplier = section.getDouble("velocity-multiplier", 0.6)
        trailParticleCount = section.getInt("trail-particle-count", 3)
        trailParticleInterval = section.getLong("trail-particle-interval", 2L)
        impactParticleCount = section.getInt("impact-particle-count", 50)
        snowLayerChance = section.getDouble("snow-layer-chance", 0.7)
        maxSnowLayers = section.getInt("max-snow-layers", 3)
        freezeTicks = section.getInt("freeze-ticks", 100)
        useCustomModel = section.getBoolean("use-custom-model", true)
        customModelKey = section.getString("custom-model-key", "cris_tsl:big_snow_ball") ?: "cris_tsl:big_snow_ball"

        // 加载音效配置
        impactSounds = loadSounds(section.getStringList("impact-sounds"))

        // 加载消息
        val prefix = section.getString("messages.prefix", "&b[雪球]&r ") ?: "&b[雪球]&r "
        messages.clear()
        section.getConfigurationSection("messages")?.getKeys(false)?.forEach { key ->
            if (key != "prefix") {
                messages[key] = (section.getString("messages.$key") ?: "").replace("%prefix%", prefix)
            }
        }

        plugin.logger.info("[SuperSnowball] 配置已加载 - 覆雪半径: $snowRadius, 击飞半径: $knockbackRadius")
    }

    fun isEnabled(): Boolean = enabled
    fun getSnowRadius(): Int = snowRadius
    fun getKnockbackRadius(): Double = knockbackRadius
    fun getKnockbackStrength(): Double = knockbackStrength
    fun getGravity(): Double = gravity
    fun getVelocityMultiplier(): Double = velocityMultiplier
    fun getTrailParticleCount(): Int = trailParticleCount
    fun getTrailParticleInterval(): Long = trailParticleInterval
    fun getImpactParticleCount(): Int = impactParticleCount
    fun getSnowLayerChance(): Double = snowLayerChance
    fun getMaxSnowLayers(): Int = maxSnowLayers
    fun getFreezeTicks(): Int = freezeTicks
    fun getImpactSounds(): List<SoundConfig> = impactSounds

    /**
     * 加载音效配置列表
     * 格式: "SOUND_NAME:volume:pitch"
     */
    private fun loadSounds(soundList: List<String>): List<SoundConfig> {
        if (soundList.isEmpty()) {
            // 默认音效：紫水晶 + 风弹
            return listOf(
                SoundConfig(Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.5f, 1.0f),
                SoundConfig(Sound.ENTITY_WIND_CHARGE_WIND_BURST, 1.0f, 1.2f)
            )
        }

        return soundList.mapNotNull { line ->
            try {
                val parts = line.split(":")
                val soundName = parts[0].uppercase()
                val volume = parts.getOrNull(1)?.toFloatOrNull() ?: 1.0f
                val pitch = parts.getOrNull(2)?.toFloatOrNull() ?: 1.0f
                val sound = Sound.valueOf(soundName)
                SoundConfig(sound, volume, pitch)
            } catch (e: Exception) {
                plugin.logger.warning("[SuperSnowball] 无效的音效配置: $line")
                null
            }
        }
    }

    fun getMessage(key: String, vararg replacements: Pair<String, String>): String {
        var message = messages[key] ?: key
        for ((placeholder, value) in replacements) {
            message = message.replace("{$placeholder}", value)
        }
        return message
    }

    /**
     * 创建超级大雪球物品
     */
    fun createSuperSnowball(): ItemStack {
        val item = ItemStack(Material.SNOWBALL, 1)
        val meta = item.itemMeta ?: return item

        // 设置显示名称
        meta.displayName(
            Component.text("超级大雪球")
                .color(NamedTextColor.AQUA)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false)
        )

        // 设置 Lore
        meta.lore(listOf(
            Component.text("").decoration(TextDecoration.ITALIC, false),
            Component.text("❄ 投掷后产生大范围覆雪效果").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
            Component.text("❄ 将附近实体击飞").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
        ))

        // 设置 PDC 标记
        meta.persistentDataContainer.set(SUPER_SNOWBALL_KEY, PersistentDataType.BYTE, 1)

        // 设置自定义模型（如果启用）
        if (useCustomModel) {
            val modelKey = NamespacedKey.fromString(customModelKey)
            if (modelKey != null) {
                meta.setItemModel(modelKey)
            }
        }

        // 设置可堆叠，一组16个
        meta.setMaxStackSize(16)

        item.itemMeta = meta
        return item
    }

    /**
     * 检查物品是否是超级大雪球
     */
    fun isSuperSnowball(item: ItemStack?): Boolean {
        if (item == null || item.type != Material.SNOWBALL) return false
        val meta = item.itemMeta ?: return false
        return meta.persistentDataContainer.has(SUPER_SNOWBALL_KEY, PersistentDataType.BYTE)
    }

}
