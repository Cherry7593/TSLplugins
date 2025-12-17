package org.tsl.tSLplugins.TimedAttribute

import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 计时属性效果管理器
 * 负责效果的添加、移除、过期扫描和玩家上下线处理
 */
class TimedAttributeManager(private val plugin: JavaPlugin) {

    private var enabled: Boolean = true
    private var scanIntervalTicks: Long = 20L
    private var storage: TimedEffectStorage? = null

    // 在线玩家的活跃效果缓存：playerUuid -> attributeKey -> List<effect>
    private val activeEffects: ConcurrentHashMap<UUID, ConcurrentHashMap<String, MutableList<TimedAttributeEffect>>> =
        ConcurrentHashMap()

    // 玩家属性的原始默认值缓存：playerUuid -> attributeKey -> baseValue
    private val playerBaseValues: ConcurrentHashMap<UUID, ConcurrentHashMap<String, Double>> =
        ConcurrentHashMap()

    // Attribute 别名映射（简写 -> 完整名称）
    private val attributeAliases: Map<String, String> = buildAttributeAliases()

    private val messages: MutableMap<String, String> = mutableMapOf()

    init {
        loadConfig()
    }

    /**
     * 加载配置
     */
    fun loadConfig() {
        val config = plugin.config

        enabled = config.getBoolean("timed-attribute.enabled", true)
        scanIntervalTicks = config.getLong("timed-attribute.scan-interval-ticks", 20L)

        // 读取消息配置
        val prefix = config.getString("timed-attribute.messages.prefix", "&6[属性]&r ") ?: "&6[属性]&r "
        messages.clear()
        val messagesSection = config.getConfigurationSection("timed-attribute.messages")
        if (messagesSection != null) {
            for (key in messagesSection.getKeys(false)) {
                if (key == "prefix") continue
                val rawMessage = messagesSection.getString(key) ?: ""
                messages[key] = rawMessage.replace("%prefix%", prefix)
            }
        }

        // 如果已有存储，关闭它
        storage?.close()

        if (!enabled) {
            plugin.logger.info("[TimedAttribute] 模块已禁用")
            return
        }

        // 初始化存储 - 使用全局 DatabaseManager
        storage = SQLiteTimedEffectStorage(plugin)

        plugin.logger.info("[TimedAttribute] 已加载配置")
    }

    /**
     * 启动过期扫描任务
     */
    fun startExpirationTask() {
        if (!enabled) return

        Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, { _ ->
            scanAndRemoveExpired()
        }, scanIntervalTicks, scanIntervalTicks)

        plugin.logger.info("[TimedAttribute] 过期扫描任务已启动, 间隔: ${scanIntervalTicks} ticks")
    }

    /**
     * 扫描并移除过期效果
     */
    private fun scanAndRemoveExpired() {
        val now = System.currentTimeMillis()

        // 遍历所有在线玩家的缓存
        activeEffects.forEach { (playerUuid, attributeEffects) ->
            val player = Bukkit.getPlayer(playerUuid)
            if (player == null || !player.isOnline) {
                return@forEach
            }

            // 检查每个属性的效果
            val attributesToRecalculate = mutableSetOf<String>()

            attributeEffects.forEach { (attributeKey, effects) ->
                val expiredEffects = effects.filter { it.isExpired() }
                if (expiredEffects.isNotEmpty()) {
                    attributesToRecalculate.add(attributeKey)
                    // 从数据库删除过期记录
                    expiredEffects.forEach { effect ->
                        storage?.deleteByModifier(effect.modifierUuid)
                    }
                }
            }

            // 如果有过期效果，重新计算属性值
            if (attributesToRecalculate.isNotEmpty()) {
                player.scheduler.run(plugin, { _ ->
                    attributesToRecalculate.forEach { attributeKey ->
                        // 移除过期效果
                        attributeEffects[attributeKey]?.removeIf { it.isExpired() }
                        // 重新计算并应用属性值
                        recalculateAttribute(player, attributeKey)
                    }
                }, null)
            }
        }

        // 异步清理数据库中的过期记录
        storage?.deleteExpired(now)
    }

    /**
     * 添加计时属性效果（ADD 类型）
     *
     * @param player 目标玩家
     * @param attributeKey 属性名称（支持别名）
     * @param amount 增减数量（正数增加，负数减少）
     * @param durationMs 持续时间（毫秒）
     * @param source 来源标识
     * @return 修改器 UUID，失败返回 null
     */
    fun applyAddEffect(
        player: Player,
        attributeKey: String,
        amount: Double,
        durationMs: Long,
        source: String = "command"
    ): UUID? {
        return applyEffect(player, attributeKey, amount, EffectType.ADD, durationMs, source)
    }

    /**
     * 添加计时属性效果（SET 类型）
     *
     * @param player 目标玩家
     * @param attributeKey 属性名称（支持别名）
     * @param value 目标值
     * @param durationMs 持续时间（毫秒）
     * @param source 来源标识
     * @return 修改器 UUID，失败返回 null
     */
    fun applySetEffect(
        player: Player,
        attributeKey: String,
        value: Double,
        durationMs: Long,
        source: String = "command"
    ): UUID? {
        return applyEffect(player, attributeKey, value, EffectType.SET, durationMs, source)
    }

    /**
     * 添加计时属性效果（内部方法）
     */
    private fun applyEffect(
        player: Player,
        attributeKey: String,
        amount: Double,
        effectType: EffectType,
        durationMs: Long,
        source: String
    ): UUID? {
        if (!enabled) return null

        // 解析属性名称（支持别名）
        val resolvedAttributeKey = resolveAttributeKey(attributeKey) ?: return null
        val attribute = getAttributeByKey(resolvedAttributeKey)
        if (attribute == null) {
            plugin.logger.warning("[TimedAttribute] 无效的属性: $resolvedAttributeKey")
            return null
        }

        val modifierUuid = UUID.randomUUID()
        val now = System.currentTimeMillis()
        val expireAt = now + durationMs

        // 在 EntityScheduler 中执行
        player.scheduler.run(plugin, { _ ->
            val attrInstance = player.getAttribute(attribute) ?: return@run

            // 捕获默认值（如果这是该属性的第一个效果）
            val playerAttrValues = playerBaseValues.computeIfAbsent(player.uniqueId) { ConcurrentHashMap() }
            val baseValue = playerAttrValues.computeIfAbsent(resolvedAttributeKey) {
                attrInstance.baseValue
            }

            val effect = TimedAttributeEffect(
                playerUuid = player.uniqueId,
                attributeKey = resolvedAttributeKey,
                modifierUuid = modifierUuid,
                amount = amount,
                effectType = effectType,
                createdAt = now,
                expireAt = expireAt,
                baseValue = baseValue,
                source = source
            )

            // 添加到缓存
            val playerEffects = activeEffects.computeIfAbsent(player.uniqueId) { ConcurrentHashMap() }
            val attrEffects = playerEffects.computeIfAbsent(resolvedAttributeKey) { mutableListOf() }
            attrEffects.add(effect)

            // 重新计算并应用属性值
            recalculateAttribute(player, resolvedAttributeKey)

            // 异步写入数据库
            storage?.insert(effect)

            plugin.logger.info("[TimedAttribute] ${player.name} 的 $resolvedAttributeKey ${effectType.name} $amount, 持续 ${durationMs/1000}秒")
        }, null)

        return modifierUuid
    }

    /**
     * 根据当前效果列表重新计算属性最终值
     * 实现优先级逻辑：SET 覆盖之前所有操作，ADD 累加
     */
    private fun recalculateAttribute(player: Player, attributeKey: String) {
        val attribute = getAttributeByKey(attributeKey) ?: return
        val attrInstance = player.getAttribute(attribute) ?: return

        val playerEffects = activeEffects[player.uniqueId] ?: return
        val effects = playerEffects[attributeKey]?.filter { !it.isExpired() } ?: emptyList()

        // 获取默认值
        val baseValue = playerBaseValues[player.uniqueId]?.get(attributeKey) ?: attrInstance.baseValue

        if (effects.isEmpty()) {
            // 没有效果，恢复到默认值
            attrInstance.baseValue = baseValue
            // 清理默认值缓存
            playerBaseValues[player.uniqueId]?.remove(attributeKey)
            if (playerBaseValues[player.uniqueId]?.isEmpty() == true) {
                playerBaseValues.remove(player.uniqueId)
            }
            plugin.logger.info("[TimedAttribute] ${player.name} 的 $attributeKey 恢复到默认值 $baseValue")
            return
        }

        // 按创建时间排序
        val sortedEffects = effects.sortedBy { it.createdAt }

        // 找到最新的未过期 SET
        val latestSet = sortedEffects.lastOrNull { it.effectType == EffectType.SET }

        val activeValue: Double
        val activeAdds: List<TimedAttributeEffect>

        if (latestSet != null) {
            // 有未过期的 SET，使用其值作为 activeValue
            activeValue = latestSet.amount
            // 只计算 SET 之后的 ADD
            activeAdds = sortedEffects.filter {
                it.effectType == EffectType.ADD && it.createdAt > latestSet.createdAt
            }
        } else {
            // 没有 SET，使用默认值
            activeValue = baseValue
            // 所有 ADD 都生效
            activeAdds = sortedEffects.filter { it.effectType == EffectType.ADD }
        }

        // 计算最终值
        val addSum = activeAdds.sumOf { it.amount }
        val finalValue = activeValue + addSum

        // 应用到玩家属性
        attrInstance.baseValue = finalValue

        plugin.logger.info("[TimedAttribute] ${player.name} 的 $attributeKey 计算结果: base=$baseValue, activeValue=$activeValue, addSum=$addSum, final=$finalValue")
    }

    /**
     * 直接设置玩家属性的基础值（永久生效）
     * 类似原版 /attribute 命令
     *
     * @param player 目标玩家
     * @param attributeKey 属性名称（支持别名）
     * @param value 要设置的值
     * @return 是否成功设置
     */
    fun setAttributeBaseValue(player: Player, attributeKey: String, value: Double): Boolean {
        val resolvedKey = resolveAttributeKey(attributeKey) ?: return false
        val attribute = getAttributeByKey(resolvedKey) ?: return false

        return try {
            // 在 EntityScheduler 中设置属性（Folia 兼容）
            player.scheduler.run(plugin, { _ ->
                val attrInstance = player.getAttribute(attribute)
                if (attrInstance != null) {
                    attrInstance.baseValue = value
                    plugin.logger.info("[TimedAttribute] 已设置 ${player.name} 的 $resolvedKey 基础值为 $value")
                }
            }, null)
            true
        } catch (e: Exception) {
            plugin.logger.warning("[TimedAttribute] 设置属性基础值失败: ${e.message}")
            false
        }
    }

    /**
     * 移除指定效果
     *
     * @param player 目标玩家
     * @param modifierUuid 修改器 UUID
     * @return 是否成功移除
     */
    fun removeEffect(player: Player, modifierUuid: UUID): Boolean {
        if (!enabled) return false

        val playerEffects = activeEffects[player.uniqueId] ?: return false

        // 查找包含该 modifierUuid 的效果
        var foundAttributeKey: String? = null
        for ((attrKey, effects) in playerEffects) {
            if (effects.any { it.modifierUuid == modifierUuid }) {
                foundAttributeKey = attrKey
                break
            }
        }

        if (foundAttributeKey == null) return false

        // 在 EntityScheduler 中移除效果
        val attrKey = foundAttributeKey
        player.scheduler.run(plugin, { _ ->
            playerEffects[attrKey]?.removeIf { it.modifierUuid == modifierUuid }
            recalculateAttribute(player, attrKey)
            storage?.deleteByModifier(modifierUuid)
        }, null)

        return true
    }

    /**
     * 获取玩家的活跃效果列表
     */
    fun listActiveEffects(player: Player): List<TimedAttributeEffect> {
        val playerEffects = activeEffects[player.uniqueId] ?: return emptyList()
        return playerEffects.values.flatten().filter { !it.isExpired() }
    }

    /**
     * 清除玩家的所有计时效果
     */
    fun clearAllEffects(player: Player): Int {
        if (!enabled) return 0

        val playerEffects = activeEffects[player.uniqueId] ?: return 0
        val count = playerEffects.values.sumOf { it.size }

        if (count == 0) return 0

        // 在 EntityScheduler 中恢复所有属性到默认值
        player.scheduler.run(plugin, { _ ->
            val attributeKeys = playerEffects.keys.toList()

            // 清空效果缓存
            playerEffects.clear()

            // 恢复每个属性到默认值
            attributeKeys.forEach { attributeKey ->
                recalculateAttribute(player, attributeKey)
            }

            // 清理默认值缓存
            playerBaseValues.remove(player.uniqueId)

            // 异步删除该玩家的所有数据库记录
            storage?.deleteByPlayer(player.uniqueId)
        }, null)

        return count
    }

    /**
     * 玩家上线时加载效果
     */
    fun onPlayerJoin(player: Player) {
        if (!enabled) return

        storage?.loadByPlayer(player.uniqueId)?.thenAccept { effects ->
            val validEffects = effects.filter { !it.isExpired() }
            val expiredUuids = effects.filter { it.isExpired() }.map { it.modifierUuid }

            if (validEffects.isEmpty() && expiredUuids.isEmpty()) return@thenAccept

            // 删除过期记录
            expiredUuids.forEach { uuid ->
                storage?.deleteByModifier(uuid)
            }

            // 在 EntityScheduler 中恢复有效效果
            if (validEffects.isNotEmpty()) {
                player.scheduler.run(plugin, { _ ->
                    // 按属性分组
                    val effectsByAttribute = validEffects.groupBy { it.attributeKey }

                    effectsByAttribute.forEach { (attributeKey, attrEffects) ->
                        val attribute = getAttributeByKey(attributeKey) ?: return@forEach
                        val attrInstance = player.getAttribute(attribute) ?: return@forEach

                        // 恢复默认值（如果有记录）
                        val baseValue = attrEffects.firstOrNull { it.baseValue != null }?.baseValue
                            ?: attrInstance.baseValue
                        playerBaseValues.computeIfAbsent(player.uniqueId) { ConcurrentHashMap() }[attributeKey] = baseValue

                        // 添加到缓存
                        val playerEffectsMap = activeEffects.computeIfAbsent(player.uniqueId) { ConcurrentHashMap() }
                        val attrEffectsList = playerEffectsMap.computeIfAbsent(attributeKey) { mutableListOf() }
                        attrEffectsList.addAll(attrEffects)

                        // 重新计算属性值
                        recalculateAttribute(player, attributeKey)
                    }

                    plugin.logger.info("[TimedAttribute] ${player.name}: 恢复 ${validEffects.size} 个效果")
                }, null)
            }
        }
    }

    /**
     * 玩家下线时清理缓存
     */
    fun onPlayerQuit(player: Player) {
        activeEffects.remove(player.uniqueId)
    }

    /**
     * 关闭管理器
     */
    fun shutdown() {
        storage?.close()
        activeEffects.clear()
        plugin.logger.info("[TimedAttribute] 管理器已关闭")
    }

    /**
     * 检查功能是否启用
     */
    fun isEnabled(): Boolean = enabled

    /**
     * 获取消息文本
     */
    fun getMessage(key: String, vararg replacements: Pair<String, String>): String {
        var message = messages[key] ?: key
        for ((placeholder, value) in replacements) {
            message = message.replace("{$placeholder}", value)
        }
        return message
    }

    /**
     * 解析属性名称（支持别名）
     * 例如：MAX_HEALTH -> GENERIC_MAX_HEALTH
     */
    fun resolveAttributeKey(input: String): String? {
        val upperInput = input.uppercase()

        // 直接匹配
        if (isValidAttribute(upperInput)) {
            return upperInput
        }

        // 尝试别名
        val aliased = attributeAliases[upperInput]
        if (aliased != null && isValidAttribute(aliased)) {
            return aliased
        }

        // 尝试添加 GENERIC_ 前缀
        val withPrefix = "GENERIC_$upperInput"
        if (isValidAttribute(withPrefix)) {
            return withPrefix
        }

        return null
    }

    /**
     * 检查属性名称是否有效
     */
    private fun isValidAttribute(name: String): Boolean {
        return getAttributeByKey(name) != null
    }

    /**
     * 根据属性名称获取 Attribute 对象（使用 Registry）
     */
    private fun getAttributeByKey(name: String): Attribute? {
        // 尝试通过 Registry 获取（新 API）
        val key = name.lowercase().replace("_", "_")
        val namespacedKey = NamespacedKey.minecraft(key)
        return Registry.ATTRIBUTE.get(namespacedKey)
    }

    /**
     * 构建属性别名映射
     */
    private fun buildAttributeAliases(): Map<String, String> {
        val aliases = mutableMapOf<String, String>()

        // 常用别名
        aliases["MAX_HEALTH"] = "GENERIC_MAX_HEALTH"
        aliases["HEALTH"] = "GENERIC_MAX_HEALTH"
        aliases["SPEED"] = "GENERIC_MOVEMENT_SPEED"
        aliases["MOVEMENT_SPEED"] = "GENERIC_MOVEMENT_SPEED"
        aliases["ATTACK_DAMAGE"] = "GENERIC_ATTACK_DAMAGE"
        aliases["DAMAGE"] = "GENERIC_ATTACK_DAMAGE"
        aliases["ATTACK_SPEED"] = "GENERIC_ATTACK_SPEED"
        aliases["ARMOR"] = "GENERIC_ARMOR"
        aliases["ARMOR_TOUGHNESS"] = "GENERIC_ARMOR_TOUGHNESS"
        aliases["KNOCKBACK_RESISTANCE"] = "GENERIC_KNOCKBACK_RESISTANCE"
        aliases["LUCK"] = "GENERIC_LUCK"
        aliases["FOLLOW_RANGE"] = "GENERIC_FOLLOW_RANGE"
        aliases["FLYING_SPEED"] = "GENERIC_FLYING_SPEED"
        aliases["ATTACK_KNOCKBACK"] = "GENERIC_ATTACK_KNOCKBACK"
        aliases["SCALE"] = "GENERIC_SCALE"
        aliases["STEP_HEIGHT"] = "GENERIC_STEP_HEIGHT"
        aliases["GRAVITY"] = "GENERIC_GRAVITY"
        aliases["JUMP_STRENGTH"] = "GENERIC_JUMP_STRENGTH"
        aliases["FALL_DAMAGE_MULTIPLIER"] = "GENERIC_FALL_DAMAGE_MULTIPLIER"
        aliases["SAFE_FALL_DISTANCE"] = "GENERIC_SAFE_FALL_DISTANCE"
        aliases["BURNING_TIME"] = "GENERIC_BURNING_TIME"
        aliases["EXPLOSION_KNOCKBACK_RESISTANCE"] = "GENERIC_EXPLOSION_KNOCKBACK_RESISTANCE"
        aliases["WATER_MOVEMENT_EFFICIENCY"] = "GENERIC_WATER_MOVEMENT_EFFICIENCY"
        aliases["OXYGEN_BONUS"] = "GENERIC_OXYGEN_BONUS"
        aliases["BLOCK_INTERACTION_RANGE"] = "PLAYER_BLOCK_INTERACTION_RANGE"
        aliases["ENTITY_INTERACTION_RANGE"] = "PLAYER_ENTITY_INTERACTION_RANGE"
        aliases["BLOCK_BREAK_SPEED"] = "PLAYER_BLOCK_BREAK_SPEED"
        aliases["MINING_EFFICIENCY"] = "PLAYER_MINING_EFFICIENCY"
        aliases["SNEAKING_SPEED"] = "PLAYER_SNEAKING_SPEED"
        aliases["SUBMERGED_MINING_SPEED"] = "PLAYER_SUBMERGED_MINING_SPEED"
        aliases["SWEEPING_DAMAGE_RATIO"] = "PLAYER_SWEEPING_DAMAGE_RATIO"

        return aliases
    }

    /**
     * 获取所有支持的属性名称（包括别名）
     */
    fun getAllAttributeNames(): List<String> {
        val names = mutableListOf<String>()

        // 添加所有有效的 Attribute 名称（从 Registry 获取）
        try {
            org.bukkit.Registry.ATTRIBUTE.forEach { attr ->
                names.add(attr.key.key.uppercase())
            }
        } catch (e: Exception) {
            // 备用：添加常用属性
            names.addAll(listOf(
                "GENERIC_MAX_HEALTH", "GENERIC_MOVEMENT_SPEED", "GENERIC_ATTACK_DAMAGE",
                "GENERIC_ATTACK_SPEED", "GENERIC_ARMOR", "GENERIC_ARMOR_TOUGHNESS",
                "GENERIC_KNOCKBACK_RESISTANCE", "GENERIC_LUCK", "GENERIC_SCALE"
            ))
        }

        // 添加别名
        names.addAll(attributeAliases.keys)

        return names.distinct().sorted()
    }

    /**
     * 解析持续时间字符串
     * 支持格式：10s, 5m, 2h, 1d, 纯数字（默认秒）
     *
     * @return 毫秒数，解析失败返回 null
     */
    fun parseDuration(input: String): Long? {
        val trimmed = input.trim().lowercase()

        // 纯数字，默认为秒
        trimmed.toLongOrNull()?.let { return it * 1000 }

        // 带单位的格式
        val regex = Regex("""^(\d+)([smhd])$""")
        val match = regex.matchEntire(trimmed) ?: return null

        val value = match.groupValues[1].toLongOrNull() ?: return null
        val unit = match.groupValues[2]

        return when (unit) {
            "s" -> value * 1000
            "m" -> value * 60 * 1000
            "h" -> value * 60 * 60 * 1000
            "d" -> value * 24 * 60 * 60 * 1000
            else -> null
        }
    }

    /**
     * 解析操作类型字符串
     */
    fun parseOperation(input: String): AttributeModifier.Operation? {
        return try {
            AttributeModifier.Operation.valueOf(input.uppercase())
        } catch (e: Exception) {
            // 尝试简写
            when (input.uppercase()) {
                "ADD", "ADD_NUMBER" -> AttributeModifier.Operation.ADD_NUMBER
                "ADD_SCALAR", "SCALAR" -> AttributeModifier.Operation.ADD_SCALAR
                "MULTIPLY", "MULTIPLY_SCALAR_1" -> AttributeModifier.Operation.MULTIPLY_SCALAR_1
                else -> null
            }
        }
    }

    /**
     * 属性信息数据类
     */
    data class AttributeInfo(
        val key: String,           // 属性键名
        val alias: String,         // 常用别名
        val description: String,   // 中文描述
        val defaultValue: Double,  // 默认值
        val minValue: Double,      // 最小值
        val maxValue: Double       // 最大值
    )

    /**
     * 获取常用属性的详细信息
     */
    fun getAttributeInfoList(): List<AttributeInfo> {
        return listOf(
            // 生命与防御
            AttributeInfo("max_health", "HEALTH", "最大生命值", 20.0, 1.0, 1024.0),
            AttributeInfo("armor", "ARMOR", "护甲值", 0.0, 0.0, 30.0),
            AttributeInfo("armor_toughness", "ARMOR_TOUGHNESS", "护甲韧性", 0.0, 0.0, 20.0),
            AttributeInfo("knockback_resistance", "KNOCKBACK_RES", "击退抗性", 0.0, 0.0, 1.0),

            // 攻击
            AttributeInfo("attack_damage", "DAMAGE", "攻击伤害", 1.0, 0.0, 2048.0),
            AttributeInfo("attack_speed", "ATK_SPEED", "攻击速度", 4.0, 0.0, 1024.0),
            AttributeInfo("attack_knockback", "ATK_KB", "攻击击退", 0.0, 0.0, 5.0),

            // 移动
            AttributeInfo("movement_speed", "SPEED", "移动速度", 0.1, 0.0, 1024.0),
            AttributeInfo("flying_speed", "FLY_SPEED", "飞行速度", 0.4, 0.0, 1024.0),
            AttributeInfo("sneaking_speed", "SNEAK_SPEED", "潜行速度", 0.3, 0.0, 1.0),

            // 体型与跳跃
            AttributeInfo("scale", "SCALE", "体型大小", 1.0, 0.0625, 16.0),
            AttributeInfo("step_height", "STEP", "跨步高度", 0.6, 0.0, 10.0),
            AttributeInfo("jump_strength", "JUMP", "跳跃力量", 0.42, 0.0, 32.0),
            AttributeInfo("gravity", "GRAVITY", "重力", 0.08, -1.0, 1.0),

            // 摔落
            AttributeInfo("fall_damage_multiplier", "FALL_DMG", "摔落伤害倍率", 1.0, 0.0, 100.0),
            AttributeInfo("safe_fall_distance", "SAFE_FALL", "安全摔落距离", 3.0, -1024.0, 1024.0),

            // 其他
            AttributeInfo("luck", "LUCK", "幸运值", 0.0, -1024.0, 1024.0),
            AttributeInfo("oxygen_bonus", "OXYGEN", "水下呼吸加成", 0.0, 0.0, 1024.0),
            AttributeInfo("block_break_speed", "BREAK_SPEED", "方块破坏速度", 1.0, 0.0, 1024.0),
            AttributeInfo("block_interaction_range", "BLOCK_RANGE", "方块交互范围", 4.5, 0.0, 64.0),
            AttributeInfo("entity_interaction_range", "ENTITY_RANGE", "实体交互范围", 3.0, 0.0, 64.0)
        )
    }

    /**
     * 根据属性键或别名获取属性信息
     */
    fun getAttributeInfo(key: String): AttributeInfo? {
        val upperKey = key.uppercase()
        val lowerKey = key.lowercase().replace("generic_", "").replace("player_", "")

        // 先尝试通过 key 匹配
        val byKey = getAttributeInfoList().find { it.key == lowerKey }
        if (byKey != null) return byKey

        // 再尝试通过 alias（简称）匹配（忽略大小写）
        val byAlias = getAttributeInfoList().find { it.alias.equals(upperKey, ignoreCase = true) }
        if (byAlias != null) return byAlias

        // 尝试解析属性别名后再匹配
        val resolved = resolveAttributeKey(key)
        if (resolved != null) {
            val resolvedLower = resolved.lowercase().replace("generic_", "").replace("player_", "")
            return getAttributeInfoList().find { it.key == resolvedLower }
        }

        return null
    }
}

