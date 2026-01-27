package org.tsl.tSLplugins.modules.randomvariable

import org.tsl.tSLplugins.core.AbstractModule
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ThreadLocalRandom

/**
 * 混合分布随机数区间
 */
data class RandomRange(
    val min: Double,
    val max: Double,
    val weight: Double
) {
    fun generate(): Double = ThreadLocalRandom.current().nextDouble(min, max)
}

/**
 * 混合分布随机变量定义
 */
data class RandomVariable(
    val name: String,
    val ranges: List<RandomRange>,
    val precision: Int = -1
) {
    private val totalWeight: Double = ranges.sumOf { it.weight }

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
        return formatValue(ranges.last().generate())
    }

    private fun formatValue(value: Double): Double {
        return if (precision >= 0) {
            val factor = Math.pow(10.0, precision.toDouble())
            Math.round(value * factor) / factor
        } else {
            value
        }
    }

    fun generateString(): String {
        val value = generate()
        return if (precision >= 0) String.format("%.${precision}f", value) else value.toString()
    }
}

/**
 * RandomVariable 模块 - 混合分布随机数
 * 
 * 支持配置多个自定义随机变量，每个变量由多个区间+权重组成。
 * 通过 PAPI 变量 %tsl_random_变量名% 获取随机值。
 */
class RandomVariableModule : AbstractModule() {

    override val id = "randomvariable"
    override val configPath = "random-variable"
    override fun getDescription() = "混合分布随机数"

    private val variables = ConcurrentHashMap<String, RandomVariable>()

    override fun loadConfig() {
        super.loadConfig()
        loadVariables()
    }

    override fun doEnable() {
        logInfo("已加载 ${variables.size} 个随机变量")
    }

    override fun doReload() {
        loadVariables()
        logInfo("已重载 ${variables.size} 个随机变量")
    }

    private fun loadVariables() {
        variables.clear()

        val variablesSection = context.config.getConfigurationSection("$configPath.variables") ?: return

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
            }
        }
    }

    /**
     * 获取随机变量值（字符串格式）
     */
    fun getRandomValue(name: String): String? {
        return variables[name.lowercase()]?.generateString()
    }

    /**
     * 获取随机变量值（Double 格式）
     */
    fun getRandomValueDouble(name: String): Double? {
        return variables[name.lowercase()]?.generate()
    }

    /**
     * 检查变量是否存在
     */
    fun hasVariable(name: String): Boolean = variables.containsKey(name.lowercase())

    /**
     * 获取所有变量名称
     */
    fun getVariableNames(): Set<String> = variables.keys.toSet()
}
