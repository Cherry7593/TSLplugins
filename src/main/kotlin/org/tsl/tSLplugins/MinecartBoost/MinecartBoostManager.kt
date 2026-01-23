package org.tsl.tSLplugins.MinecartBoost

import org.bukkit.Material
import org.bukkit.plugin.java.JavaPlugin

/**
 * 矿车加速管理器
 * 管理方块-maxSpeed映射配置
 */
class MinecartBoostManager(private val plugin: JavaPlugin) {

    // ===== 配置缓存 =====
    private var enabled: Boolean = false
    private val blockMaxSpeedMap: MutableMap<Material, Double> = mutableMapOf()

    init {
        loadConfig()
    }

    /**
     * 加载配置
     */
    fun loadConfig() {
        val config = plugin.config

        enabled = config.getBoolean("minecart-boost.enabled", false)
        
        // 清空旧映射
        blockMaxSpeedMap.clear()
        
        // 加载方块-maxSpeed映射
        val blocksSection = config.getConfigurationSection("minecart-boost.blocks")
        if (blocksSection != null) {
            for (key in blocksSection.getKeys(false)) {
                val materialName = key.uppercase()
                val maxSpeed = blocksSection.getDouble(key)
                
                try {
                    val material = Material.valueOf(materialName)
                    blockMaxSpeedMap[material] = maxSpeed
                } catch (e: IllegalArgumentException) {
                    plugin.logger.warning("[MinecartBoost] 无效的方块类型: $key")
                }
            }
        }

        plugin.logger.info("[MinecartBoost] 配置已加载 - 启用: $enabled, 方块映射: ${blockMaxSpeedMap.size} 种")
    }

    /**
     * 功能是否启用
     */
    fun isEnabled(): Boolean = enabled

    /**
     * 获取指定方块的 maxSpeed 值
     * @param material 方块类型
     * @return maxSpeed 值（原版为0.4），如果该方块不在映射中则返回 null
     */
    fun getMaxSpeedForBlock(material: Material): Double? = blockMaxSpeedMap[material]

    /**
     * 检查方块是否为加速方块
     */
    fun isBoostBlock(material: Material): Boolean = blockMaxSpeedMap.containsKey(material)
}
