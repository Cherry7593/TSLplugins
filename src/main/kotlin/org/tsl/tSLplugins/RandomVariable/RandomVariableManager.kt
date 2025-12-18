package org.tsl.tSLplugins.RandomVariable

import org.bukkit.plugin.java.JavaPlugin
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ThreadLocalRandom

/**
 * 混合分布随机数区间
 *
 * @param min 区间最小值
 * @param max 区间最大值
 * @param weight 权重
 */
data class RandomRange(
    val min: Double,
    val max: Double,
    val weight: Double
) {
    /**
     * 在此区间内生成随机数
     */
    fun generate(): Double {
        return ThreadLocalRandom.current().nextDouble(min, max)
    }
}

/**
 * 混合分布随机变量定义
 *
 * @param name 变量名称
 * @param ranges 区间列表
 * @param precision 小数精度（-1 表示不限制）
 */
data class RandomVariable(
    val name: String,
    val ranges: List<RandomRange>,
    val precision: Int = -1
) {
    private val totalWeight: Double = ranges.sumOf { it.weight }

    /**
     * 根据权重随机选择区间并生成随机数
     */
    fun generate(): Double {
        if (ranges.isEmpty()) return 0.0
        if (ranges.size == 1) return formatValue(ranges[0].generate())

        val random = ThreadLocalRandom.current().nextDouble() * totalWeight
        var cumulative = 0.0

        for (range in ranges) {
            cumulative += range.weight
            if (random < cumulative) {
                return formatValue(range.generate())
            }
        }

        // 兜底：返回最后一个区间的值
        return formatValue(ranges.last().generate())
    }

    /**
     * 格式化数值精度
     */
    private fun formatValue(value: Double): Double {
        return if (precision >= 0) {
            val factor = Math.pow(10.0, precision.toDouble())
            Math.round(value * factor) / factor
        } else {
            value
        }
    }

    /**
     * 获取格式化的字符串值
     */
    fun generateString(): String {
        val value = generate()
        return if (precision >= 0) {
            String.format("%.${precision}f", value)
        } else {
            value.toString()
        }
    }
}

/**
 * 混合分布随机数变量管理器
 *
 * 支持配置多个自定义随机变量，每个变量由多个区间+权重组成。
 * 通过 PAPI 变量 %tsl_random_变量名% 获取随机值。
 */
class RandomVariableManager(private val plugin: JavaPlugin) {

    private var enabled: Boolean = true
    private val variables: ConcurrentHashMap<String, RandomVariable> = ConcurrentHashMap()

    init {
        loadConfig()
    }

    /**
     * 加载配置
     */
    fun loadConfig() {
        val config = plugin.config

        enabled = config.getBoolean("random-variable.enabled", true)

        if (!enabled) {
            plugin.logger.info("[RandomVariable] 模块已禁用")
            return
        }

        // 清空现有变量
        variables.clear()

        // 读取变量配置
        val variablesSection = config.getConfigurationSection("random-variable.variables")
        if (variablesSection == null) {
            plugin.logger.info("[RandomVariable] 未配置任何随机变量")
            return
        }

        for (varName in variablesSection.getKeys(false)) {
            val varSection = variablesSection.getConfigurationSection(varName) ?: continue

            val precision = varSection.getInt("precision", -1)
            val rangesList = varSection.getList("ranges") ?: continue

            val ranges = mutableListOf<RandomRange>()

            for (rangeObj in rangesList) {
                if (rangeObj is Map<*, *>) {
                    val min = (rangeObj["min"] as? Number)?.toDouble() ?: continue
                    val max = (rangeObj["max"] as? Number)?.toDouble() ?: continue
                    val weight = (rangeObj["weight"] as? Number)?.toDouble() ?: 1.0

                    if (min <= max && weight > 0) {
                        ranges.add(RandomRange(min, max, weight))
                    }
                }
            }

            if (ranges.isNotEmpty()) {
                val variable = RandomVariable(varName, ranges, precision)
                variables[varName.lowercase()] = variable
                plugin.logger.info("[RandomVariable] 加载变量: $varName (${ranges.size} 个区间)")
            }
        }

        plugin.logger.info("[RandomVariable] 已加载 ${variables.size} 个随机变量")
    }

    /**
     * 重载配置
     */
    fun reload() {
        loadConfig()
    }

    /**
     * 检查模块是否启用
     */
    fun isEnabled(): Boolean = enabled

    /**
     * 获取随机变量值（字符串格式）
     *
     * @param name 变量名称（忽略大小写）
     * @return 随机值字符串，变量不存在返回 null
     */
    fun getRandomValue(name: String): String? {
        val variable = variables[name.lowercase()] ?: return null
        return variable.generateString()
    }

    /**
     * 获取随机变量值（Double 格式）
     *
     * @param name 变量名称（忽略大小写）
     * @return 随机值，变量不存在返回 null
     */
    fun getRandomValueDouble(name: String): Double? {
        val variable = variables[name.lowercase()] ?: return null
        return variable.generate()
    }

    /**
     * 检查变量是否存在
     */
    fun hasVariable(name: String): Boolean {
        return variables.containsKey(name.lowercase())
    }

    /**
     * 获取所有变量名称
     */
    fun getVariableNames(): Set<String> {
        return variables.keys.toSet()
    }

    /**
     * 获取变量信息（用于调试）
     */
    fun getVariableInfo(name: String): String? {
        val variable = variables[name.lowercase()] ?: return null
        val rangesInfo = variable.ranges.joinToString(", ") {
            "[${it.min}-${it.max}, 权重${it.weight}]"
        }
        return "变量: ${variable.name}, 精度: ${variable.precision}, 区间: $rangesInfo"
    }
}
