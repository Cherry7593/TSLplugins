package org.tsl.tSLplugins.modules.timedattribute

import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.tsl.tSLplugins.TSLplugins
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.ArrayDeque

/**
 * 计时属性效果管理器（堆栈版）
 * 
 * 设计原则：
 * - 每个玩家可以同时拥有多种不同属性的效果（如 scale、hp 等）
 * - 同一属性可以堆叠多个效果，新效果暂停旧效果
 * - 效果过期后恢复到下一层效果，直到恢复原始值
 * - 上下线、死亡不影响计时
 */
class TimedAttributeManager(private val plugin: JavaPlugin) {

    private var enabled: Boolean = true
    private var scanIntervalTicks: Long = 20L
    private var storage: TimedEffectStorage? = null

    private val msg get() = (plugin as TSLplugins).messageManager

    // 在线玩家的效果堆栈缓存：playerUuid -> attributeKey -> Stack<effect>
    // 栈顶是活跃效果，下面是暂停的效果
    private val effectStacks: ConcurrentHashMap<UUID, ConcurrentHashMap<String, ArrayDeque<TimedAttributeEffect>>> =
        ConcurrentHashMap()

    // Attribute 别名映射（简写 -> 完整名称）
    private val attributeAliases: Map<String, String> = buildAttributeAliases()

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

        storage?.close()
        if (!enabled) return
        storage = SQLiteTimedEffectStorage(plugin)
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
     * 扫描并处理过期效果
     */
    private fun scanAndRemoveExpired() {
        // 遍历所有在线玩家的缓存
        effectStacks.forEach { (playerUuid, attributeStacks) ->
            val player = Bukkit.getPlayer(playerUuid)
            if (player == null || !player.isOnline) {
                return@forEach
            }

            // 检查每个属性的栈顶效果
            val attributesToProcess = mutableListOf<String>()
            
            attributeStacks.forEach { (attributeKey, stack) ->
                val topEffect = stack.peekLast()
                if (topEffect != null && !topEffect.isPaused && topEffect.tick()) {
                    // 栈顶效果过期
                    attributesToProcess.add(attributeKey)
                }
            }

            if (attributesToProcess.isNotEmpty()) {
                player.scheduler.run(plugin, { _ ->
                    attributesToProcess.forEach { attributeKey ->
                        processExpiredEffect(player, attributeKey)
                    }
                }, null)
            }
        }
    }

    /**
     * 处理过期效果（弹出栈顶，相对恢复）
     * 
     * 相对恢复逻辑：
     * - 计算 delta = targetValue - capturedValue
     * - 当前值 -= delta（撤销这个效果的变化量）
     * - 这样其他模块/插件的修改会被保留
     */
    private fun processExpiredEffect(player: Player, attributeKey: String) {
        val playerStacks = effectStacks[player.uniqueId] ?: return
        val stack = playerStacks[attributeKey] ?: return
        
        val expiredEffect = stack.pollLast() ?: return
        
        // 从数据库删除过期效果
        storage?.deleteByEffectId(expiredEffect.effectId)
        
        // 获取当前属性值
        val currentValue = getAttributeValue(player, attributeKey) ?: return
        
        // 计算恢复后的值（撤销这个效果的变化量）
        val restoredValue = currentValue - expiredEffect.delta
        
        // 检查是否还有下一层效果
        val nextEffect = stack.peekLast()
        
        if (nextEffect != null) {
            // 恢复下一层效果
            nextEffect.resume()
            // 下一层效果需要重新应用其 delta
            val nextRestoredValue = restoredValue + nextEffect.delta
            applyAttributeValue(player, attributeKey, nextRestoredValue)
            storage?.save(nextEffect)
            
            plugin.logger.info("[TimedAttribute] ${player.name} 的 $attributeKey 效果过期，恢复到下一层: $nextRestoredValue (剩余 ${nextEffect.getRemainingSeconds()}秒)")
        } else {
            // 栈空了，撤销变化量
            applyAttributeValue(player, attributeKey, restoredValue)
            playerStacks.remove(attributeKey)
            
            if (playerStacks.isEmpty()) {
                effectStacks.remove(player.uniqueId)
            }
            
            plugin.logger.info("[TimedAttribute] ${player.name} 的 $attributeKey 所有效果结束，撤销变化量 (delta=${expiredEffect.delta})，当前值: $restoredValue")
        }
    }
    
    /**
     * 获取玩家属性的当前值
     */
    private fun getAttributeValue(player: Player, attributeKey: String): Double? {
        val attribute = getAttributeByKey(attributeKey) ?: return null
        val attrInstance = player.getAttribute(attribute) ?: return null
        return attrInstance.baseValue
    }

    /**
     * 设置计时属性效果（堆栈模式）
     *
     * @param player 目标玩家
     * @param attributeKey 属性名称（支持别名）
     * @param targetValue 目标值
     * @param durationMs 持续时间（毫秒）
     * @param source 来源标识
     * @return 效果 ID，失败返回 null
     */
    fun applySetEffect(
        player: Player,
        attributeKey: String,
        targetValue: Double,
        durationMs: Long,
        source: String = "command"
    ): UUID? {
        if (!enabled) return null

        // 解析属性名称（支持别名）
        val resolvedAttributeKey = resolveAttributeKey(attributeKey) ?: return null
        val attribute = getAttributeByKey(resolvedAttributeKey)
        if (attribute == null) {
            plugin.logger.warning("[TimedAttribute] 无效的属性: $resolvedAttributeKey")
            return null
        }

        val effectId = UUID.randomUUID()
        val now = System.currentTimeMillis()

        // 在 EntityScheduler 中执行
        player.scheduler.run(plugin, { _ ->
            val attrInstance = player.getAttribute(attribute) ?: return@run

            // 获取或创建该属性的效果堆栈
            val playerStacks = effectStacks.computeIfAbsent(player.uniqueId) { ConcurrentHashMap() }
            val stack = playerStacks.computeIfAbsent(resolvedAttributeKey) { ArrayDeque() }

            // 暂停当前栈顶效果（如果有）
            val currentTop = stack.peekLast()
            if (currentTop != null && !currentTop.isPaused) {
                currentTop.pause()
                storage?.save(currentTop)
            }

            // 捕获当前属性值（用于计算 delta）
            val capturedValue = attrInstance.baseValue

            // 限制目标值在有效范围内
            val attrInfo = getAttributeInfo(resolvedAttributeKey)
            val clampedValue = if (attrInfo != null) {
                targetValue.coerceIn(attrInfo.minValue, attrInfo.maxValue)
            } else {
                targetValue
            }

            // 创建新效果
            val effect = TimedAttributeEffect(
                playerUuid = player.uniqueId,
                attributeKey = resolvedAttributeKey,
                effectId = effectId,
                targetValue = clampedValue,
                capturedValue = capturedValue,
                remainingMs = durationMs,
                stackIndex = stack.size,
                isPaused = false,
                lastTickAt = now,
                createdAt = now,
                source = source
            )

            // 入栈
            stack.addLast(effect)

            // 应用属性值
            applyAttributeValue(player, resolvedAttributeKey, clampedValue)

            // 保存到数据库
            storage?.save(effect)

            val stackSize = stack.size
            plugin.logger.info("[TimedAttribute] ${player.name} 的 $resolvedAttributeKey 设置为 $clampedValue, 持续 ${durationMs/1000}秒 (堆栈层数: $stackSize)")
        }, null)

        return effectId
    }

    /**
     * 应用属性值到玩家
     */
    private fun applyAttributeValue(player: Player, attributeKey: String, value: Double) {
        val attribute = getAttributeByKey(attributeKey) ?: return
        val attrInstance = player.getAttribute(attribute) ?: return

        try {
            attrInstance.baseValue = value
        } catch (e: Exception) {
            plugin.logger.warning("[TimedAttribute] 设置 ${player.name} 的 $attributeKey 失败: ${e.message}")
        }
    }

    /**
     * 取消指定属性的所有效果（撤销所有变化量）
     */
    fun cancelEffects(player: Player, attributeKey: String): Int {
        if (!enabled) return 0

        val resolvedKey = resolveAttributeKey(attributeKey) ?: return 0
        val playerStacks = effectStacks[player.uniqueId] ?: return 0
        val stack = playerStacks[resolvedKey] ?: return 0
        
        val count = stack.size
        if (count == 0) return 0

        // 计算所有效果的总 delta
        val totalDelta = stack.sumOf { it.delta }

        // 在 EntityScheduler 中取消效果
        player.scheduler.run(plugin, { _ ->
            // 获取当前值并撤销总变化量
            val currentValue = getAttributeValue(player, resolvedKey)
            if (currentValue != null) {
                val restoredValue = currentValue - totalDelta
                applyAttributeValue(player, resolvedKey, restoredValue)
            }
            
            // 清空堆栈
            stack.clear()
            playerStacks.remove(resolvedKey)
            
            if (playerStacks.isEmpty()) {
                effectStacks.remove(player.uniqueId)
            }
            
            // 从数据库删除
            storage?.deleteByPlayerAttribute(player.uniqueId, resolvedKey)
            
            plugin.logger.info("[TimedAttribute] 已取消 ${player.name} 的 $resolvedKey 所有效果 ($count 层, 总delta=$totalDelta)")
        }, null)

        return count
    }

    /**
     * 获取玩家的所有活跃效果（栈顶效果）
     */
    fun listActiveEffects(player: Player): List<TimedAttributeEffect> {
        val playerStacks = effectStacks[player.uniqueId] ?: return emptyList()
        return playerStacks.values.mapNotNull { stack -> 
            stack.peekLast()?.takeIf { !it.isExpired() }
        }
    }

    /**
     * 获取玩家指定属性的效果堆栈
     */
    fun getEffectStack(player: Player, attributeKey: String): List<TimedAttributeEffect> {
        val resolvedKey = resolveAttributeKey(attributeKey) ?: return emptyList()
        val stack = effectStacks[player.uniqueId]?.get(resolvedKey) ?: return emptyList()
        return stack.toList()
    }

    /**
     * 清除玩家的所有计时效果（撤销所有变化量）
     */
    fun clearAllEffects(player: Player): Int {
        if (!enabled) return 0

        val playerStacks = effectStacks[player.uniqueId] ?: return 0
        
        // 统计总效果数
        val count = playerStacks.values.sumOf { it.size }
        if (count == 0) return 0

        // 收集每个属性的总 delta
        val totalDeltas = mutableMapOf<String, Double>()
        playerStacks.forEach { (attrKey, stack) ->
            totalDeltas[attrKey] = stack.sumOf { it.delta }
        }

        // 在 EntityScheduler 中撤销所有变化量
        player.scheduler.run(plugin, { _ ->
            totalDeltas.forEach { (attrKey, totalDelta) ->
                val currentValue = getAttributeValue(player, attrKey)
                if (currentValue != null) {
                    val restoredValue = currentValue - totalDelta
                    applyAttributeValue(player, attrKey, restoredValue)
                }
            }

            // 清空效果缓存
            playerStacks.clear()
            effectStacks.remove(player.uniqueId)

            // 异步删除该玩家的所有数据库记录
            storage?.deleteByPlayer(player.uniqueId)
            
            plugin.logger.info("[TimedAttribute] 已清除 ${player.name} 的所有效果 ($count 层)")
        }, null)

        return count
    }

    /**
     * 玩家上线时加载效果
     */
    fun onPlayerJoin(player: Player) {
        if (!enabled) return

        storage?.loadByPlayer(player.uniqueId)?.thenAccept { effects ->
            if (effects.isEmpty()) return@thenAccept

            // 按属性分组，并按 stackIndex 排序
            val effectsByAttribute = effects.groupBy { it.attributeKey }
                .mapValues { (_, list) -> list.sortedBy { it.stackIndex } }

            // 在 EntityScheduler 中恢复效果
            player.scheduler.run(plugin, { _ ->
                val playerStacks = effectStacks.computeIfAbsent(player.uniqueId) { ConcurrentHashMap() }

                effectsByAttribute.forEach { (attributeKey, attrEffects) ->
                    val stack = ArrayDeque<TimedAttributeEffect>()
                    
                    // 过滤掉已过期的效果
                    val validEffects = attrEffects.filter { !it.isExpired() }
                    
                    if (validEffects.isEmpty()) {
                        // 所有效果都过期了，删除数据库记录
                        attrEffects.forEach { storage?.deleteByEffectId(it.effectId) }
                        return@forEach
                    }

                    // 重建堆栈
                    validEffects.forEachIndexed { index, effect ->
                        effect.stackIndex = index
                        effect.lastTickAt = System.currentTimeMillis()
                        // 只有栈顶效果是活跃的
                        effect.isPaused = (index < validEffects.size - 1)
                        stack.addLast(effect)
                    }

                    playerStacks[attributeKey] = stack

                    // 应用栈顶效果
                    val topEffect = stack.peekLast()
                    if (topEffect != null) {
                        applyAttributeValue(player, attributeKey, topEffect.targetValue)
                    }

                    // 删除过期的效果记录
                    val expiredEffects = attrEffects.filter { it.isExpired() }
                    expiredEffects.forEach { storage?.deleteByEffectId(it.effectId) }
                    
                    // 保存更新后的效果
                    storage?.saveAll(validEffects)
                }

                val totalEffects = playerStacks.values.sumOf { it.size }
                if (totalEffects > 0) {
                    plugin.logger.info("[TimedAttribute] ${player.name}: 恢复 $totalEffects 层效果")
                }
            }, null)
        }
    }

    /**
     * 玩家下线时保存效果状态
     */
    fun onPlayerQuit(player: Player) {
        val playerStacks = effectStacks.remove(player.uniqueId) ?: return
        
        // 收集所有效果并保存到数据库
        val allEffects = mutableListOf<TimedAttributeEffect>()
        playerStacks.forEach { (_, stack) ->
            stack.forEach { effect ->
                // 更新剩余时间（如果是活跃效果）
                if (!effect.isPaused) {
                    val now = System.currentTimeMillis()
                    val elapsed = now - effect.lastTickAt
                    effect.remainingMs -= elapsed
                    effect.lastTickAt = now
                }
                allEffects.add(effect)
            }
        }
        
        if (allEffects.isNotEmpty()) {
            storage?.saveAll(allEffects)
            plugin.logger.info("[TimedAttribute] ${player.name}: 保存 ${allEffects.size} 层效果")
        }
    }

    /**
     * 关闭管理器
     */
    fun shutdown() {
        // 保存所有在线玩家的效果
        Bukkit.getOnlinePlayers().forEach { player ->
            onPlayerQuit(player)
        }
        storage?.close()
        effectStacks.clear()
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
        return msg.getModule("timed-attribute", key, *replacements)
    }

    /**
     * 解析属性名称（支持别名）
     * 例如：MAX_HEALTH -> max_health
     */
    fun resolveAttributeKey(input: String): String? {
        val lowerInput = input.lowercase()
        val upperInput = input.uppercase()

        // 直接匹配（小写）
        if (isValidAttribute(lowerInput)) {
            return lowerInput
        }

        // 尝试别名
        val aliased = attributeAliases[upperInput]
        if (aliased != null && isValidAttribute(aliased)) {
            return aliased
        }

        // 尝试将下划线分隔的名称转为小写
        val normalized = upperInput.lowercase()
        if (isValidAttribute(normalized)) {
            return normalized
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
     * 构建属性别名映射（Minecraft 1.21.4 完整列表）
     */
    private fun buildAttributeAliases(): Map<String, String> {
        val aliases = mutableMapOf<String, String>()

        // ===== 生命与防御 =====
        aliases["MAX_HEALTH"] = "max_health"
        aliases["HEALTH"] = "max_health"
        aliases["HP"] = "max_health"
        aliases["LIFE"] = "max_health"
        aliases["生命"] = "max_health"
        
        aliases["MAX_ABSORPTION"] = "max_absorption"
        aliases["ABSORPTION"] = "max_absorption"
        aliases["ABS"] = "max_absorption"
        aliases["吸收"] = "max_absorption"
        
        aliases["ARMOR"] = "armor"
        aliases["DEF"] = "armor"
        aliases["护甲"] = "armor"
        
        aliases["ARMOR_TOUGHNESS"] = "armor_toughness"
        aliases["TOUGHNESS"] = "armor_toughness"
        aliases["韧性"] = "armor_toughness"
        
        aliases["KNOCKBACK_RESISTANCE"] = "knockback_resistance"
        aliases["KB_RES"] = "knockback_resistance"
        aliases["KNOCKBACK_RES"] = "knockback_resistance"
        aliases["击退抗性"] = "knockback_resistance"
        
        aliases["EXPLOSION_KNOCKBACK_RESISTANCE"] = "explosion_knockback_resistance"
        aliases["EXP_KB_RES"] = "explosion_knockback_resistance"
        aliases["爆炸击退抗性"] = "explosion_knockback_resistance"

        // ===== 攻击 =====
        aliases["ATTACK_DAMAGE"] = "attack_damage"
        aliases["DAMAGE"] = "attack_damage"
        aliases["DMG"] = "attack_damage"
        aliases["ATK"] = "attack_damage"
        aliases["伤害"] = "attack_damage"
        
        aliases["ATTACK_SPEED"] = "attack_speed"
        aliases["ATK_SPEED"] = "attack_speed"
        aliases["ATKSPD"] = "attack_speed"
        aliases["攻速"] = "attack_speed"
        
        aliases["ATTACK_KNOCKBACK"] = "attack_knockback"
        aliases["ATK_KB"] = "attack_knockback"
        aliases["击退"] = "attack_knockback"
        
        aliases["SWEEPING_DAMAGE_RATIO"] = "sweeping_damage_ratio"
        aliases["SWEEP"] = "sweeping_damage_ratio"
        aliases["SWEEP_RATIO"] = "sweeping_damage_ratio"
        aliases["横扫"] = "sweeping_damage_ratio"

        // ===== 移动 =====
        aliases["MOVEMENT_SPEED"] = "movement_speed"
        aliases["SPEED"] = "movement_speed"
        aliases["SPD"] = "movement_speed"
        aliases["MOVE_SPEED"] = "movement_speed"
        aliases["速度"] = "movement_speed"
        aliases["移速"] = "movement_speed"
        
        aliases["FLYING_SPEED"] = "flying_speed"
        aliases["FLY_SPEED"] = "flying_speed"
        aliases["FLY"] = "flying_speed"
        aliases["飞行速度"] = "flying_speed"
        
        aliases["SNEAKING_SPEED"] = "sneaking_speed"
        aliases["SNEAK_SPEED"] = "sneaking_speed"
        aliases["SNEAK"] = "sneaking_speed"
        aliases["潜行速度"] = "sneaking_speed"
        
        aliases["WATER_MOVEMENT_EFFICIENCY"] = "water_movement_efficiency"
        aliases["WATER_SPEED"] = "water_movement_efficiency"
        aliases["SWIM"] = "water_movement_efficiency"
        aliases["水中移速"] = "water_movement_efficiency"
        
        aliases["MOVEMENT_EFFICIENCY"] = "movement_efficiency"
        aliases["MOVE_EFF"] = "movement_efficiency"
        aliases["移动效率"] = "movement_efficiency"

        // ===== 体型与跳跃 =====
        aliases["SCALE"] = "scale"
        aliases["SIZE"] = "scale"
        aliases["体型"] = "scale"
        aliases["大小"] = "scale"
        
        aliases["STEP_HEIGHT"] = "step_height"
        aliases["STEP"] = "step_height"
        aliases["跨步高度"] = "step_height"
        
        aliases["JUMP_STRENGTH"] = "jump_strength"
        aliases["JUMP"] = "jump_strength"
        aliases["跳跃"] = "jump_strength"
        
        aliases["GRAVITY"] = "gravity"
        aliases["GRAV"] = "gravity"
        aliases["重力"] = "gravity"

        // ===== 摔落 =====
        aliases["FALL_DAMAGE_MULTIPLIER"] = "fall_damage_multiplier"
        aliases["FALL_DMG"] = "fall_damage_multiplier"
        aliases["FALL"] = "fall_damage_multiplier"
        aliases["摔伤"] = "fall_damage_multiplier"
        
        aliases["SAFE_FALL_DISTANCE"] = "safe_fall_distance"
        aliases["SAFE_FALL"] = "safe_fall_distance"
        aliases["安全摔落"] = "safe_fall_distance"

        // ===== 挖掘 =====
        aliases["BLOCK_BREAK_SPEED"] = "block_break_speed"
        aliases["BREAK_SPEED"] = "block_break_speed"
        aliases["DIG"] = "block_break_speed"
        aliases["挖掘速度"] = "block_break_speed"
        
        aliases["MINING_EFFICIENCY"] = "mining_efficiency"
        aliases["MINING_EFF"] = "mining_efficiency"
        aliases["MINE"] = "mining_efficiency"
        aliases["挖掘效率"] = "mining_efficiency"
        
        aliases["SUBMERGED_MINING_SPEED"] = "submerged_mining_speed"
        aliases["SUBMERGED_MINING"] = "submerged_mining_speed"
        aliases["WATER_MINE"] = "submerged_mining_speed"
        aliases["水下挖掘"] = "submerged_mining_speed"

        // ===== 交互范围 =====
        aliases["BLOCK_INTERACTION_RANGE"] = "block_interaction_range"
        aliases["BLOCK_RANGE"] = "block_interaction_range"
        aliases["方块范围"] = "block_interaction_range"
        
        aliases["ENTITY_INTERACTION_RANGE"] = "entity_interaction_range"
        aliases["ENTITY_RANGE"] = "entity_interaction_range"
        aliases["REACH"] = "entity_interaction_range"
        aliases["实体范围"] = "entity_interaction_range"

        // ===== 其他 =====
        aliases["LUCK"] = "luck"
        aliases["LCK"] = "luck"
        aliases["幸运"] = "luck"
        
        aliases["FOLLOW_RANGE"] = "follow_range"
        aliases["FOLLOW"] = "follow_range"
        aliases["跟随范围"] = "follow_range"
        
        aliases["OXYGEN_BONUS"] = "oxygen_bonus"
        aliases["OXYGEN"] = "oxygen_bonus"
        aliases["O2"] = "oxygen_bonus"
        aliases["氧气"] = "oxygen_bonus"
        
        aliases["BURNING_TIME"] = "burning_time"
        aliases["BURN_TIME"] = "burning_time"
        aliases["BURN"] = "burning_time"
        aliases["燃烧时间"] = "burning_time"
        
        aliases["SPAWN_REINFORCEMENTS"] = "spawn_reinforcements"
        aliases["SPAWN_REINF"] = "spawn_reinforcements"
        aliases["召唤援军"] = "spawn_reinforcements"
        
        aliases["TEMPT_RANGE"] = "tempt_range"
        aliases["TEMPT"] = "tempt_range"
        aliases["吸引范围"] = "tempt_range"
        
        aliases["CAMERA_DISTANCE"] = "camera_distance"
        aliases["CAMERA"] = "camera_distance"
        aliases["相机距离"] = "camera_distance"

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
     * 重置玩家所有属性到默认值（用于修复旧版本 bug）
     * 清除所有效果并将属性恢复到游戏默认值
     */
    fun resetToDefault(player: Player): Int {
        if (!enabled) return 0

        // 先清除所有效果（不撤销 delta，因为要完全重置）
        val playerStacks = effectStacks.remove(player.uniqueId)
        val effectCount = playerStacks?.values?.sumOf { it.size } ?: 0

        // 在 EntityScheduler 中重置所有属性到默认值
        player.scheduler.run(plugin, { _ ->
            var resetCount = 0
            
            getAttributeInfoList().forEach { attrInfo ->
                val attribute = getAttributeByKey(attrInfo.key)
                if (attribute != null) {
                    val attrInstance = player.getAttribute(attribute)
                    if (attrInstance != null && attrInstance.baseValue != attrInfo.defaultValue) {
                        try {
                            attrInstance.baseValue = attrInfo.defaultValue
                            resetCount++
                        } catch (e: Exception) {
                            plugin.logger.warning("[TimedAttribute] 重置 ${player.name} 的 ${attrInfo.key} 失败: ${e.message}")
                        }
                    }
                }
            }

            // 删除数据库记录
            storage?.deleteByPlayer(player.uniqueId)
            
            plugin.logger.info("[TimedAttribute] 已重置 ${player.name} 的所有属性到默认值 (清除 $effectCount 层效果, 重置 $resetCount 个属性)")
        }, null)

        return effectCount
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
     * 获取所有属性的详细信息（Minecraft 1.21.4 完整列表）
     */
    fun getAttributeInfoList(): List<AttributeInfo> {
        return listOf(
            // 生命与防御
            AttributeInfo("max_health", "HP", "最大生命值", 20.0, 1.0, 1024.0),
            AttributeInfo("max_absorption", "ABS", "最大吸收值", 0.0, 0.0, 2048.0),
            AttributeInfo("armor", "ARMOR", "护甲值", 0.0, 0.0, 30.0),
            AttributeInfo("armor_toughness", "TOUGHNESS", "护甲韧性", 0.0, 0.0, 20.0),
            AttributeInfo("knockback_resistance", "KB_RES", "击退抗性", 0.0, 0.0, 1.0),
            AttributeInfo("explosion_knockback_resistance", "EXP_KB_RES", "爆炸击退抗性", 0.0, 0.0, 1.0),

            // 攻击
            AttributeInfo("attack_damage", "DMG", "攻击伤害", 2.0, 0.0, 2048.0),
            AttributeInfo("attack_speed", "ATK_SPEED", "攻击速度", 4.0, 0.0, 1024.0),
            AttributeInfo("attack_knockback", "ATK_KB", "攻击击退", 0.0, 0.0, 5.0),
            AttributeInfo("sweeping_damage_ratio", "SWEEP", "横扫伤害比例", 0.0, 0.0, 1.0),

            // 移动
            AttributeInfo("movement_speed", "SPEED", "移动速度", 0.1, 0.0, 1024.0),
            AttributeInfo("flying_speed", "FLY", "飞行速度", 0.4, 0.0, 1024.0),
            AttributeInfo("sneaking_speed", "SNEAK", "潜行速度", 0.3, 0.0, 1.0),
            AttributeInfo("water_movement_efficiency", "SWIM", "水中移动效率", 0.0, 0.0, 1.0),
            AttributeInfo("movement_efficiency", "MOVE_EFF", "移动效率", 0.0, 0.0, 1.0),

            // 体型与跳跃
            AttributeInfo("scale", "SCALE", "体型大小", 1.0, 0.0625, 16.0),
            AttributeInfo("step_height", "STEP", "跨步高度", 0.6, 0.0, 10.0),
            AttributeInfo("jump_strength", "JUMP", "跳跃力量", 0.42, 0.0, 32.0),
            AttributeInfo("gravity", "GRAV", "重力", 0.08, -1.0, 1.0),

            // 摔落
            AttributeInfo("fall_damage_multiplier", "FALL", "摔落伤害倍率", 1.0, 0.0, 100.0),
            AttributeInfo("safe_fall_distance", "SAFE_FALL", "安全摔落距离", 3.0, -1024.0, 1024.0),

            // 挖掘
            AttributeInfo("block_break_speed", "DIG", "方块破坏速度", 1.0, 0.0, 1024.0),
            AttributeInfo("mining_efficiency", "MINE", "挖掘效率", 0.0, 0.0, 1024.0),
            AttributeInfo("submerged_mining_speed", "WATER_MINE", "水下挖掘速度", 0.2, 0.0, 20.0),

            // 交互范围
            AttributeInfo("block_interaction_range", "BLOCK_RANGE", "方块交互范围", 4.5, 0.0, 64.0),
            AttributeInfo("entity_interaction_range", "REACH", "实体交互范围", 3.0, 0.0, 64.0),

            // 其他
            AttributeInfo("luck", "LUCK", "幸运值", 0.0, -1024.0, 1024.0),
            AttributeInfo("follow_range", "FOLLOW", "跟随范围", 32.0, 0.0, 2048.0),
            AttributeInfo("oxygen_bonus", "O2", "水下呼吸加成", 0.0, 0.0, 1024.0),
            AttributeInfo("burning_time", "BURN", "燃烧时间倍率", 1.0, 0.0, 1024.0),
            AttributeInfo("spawn_reinforcements", "SPAWN_REINF", "召唤援军概率", 0.0, 0.0, 1.0),
            AttributeInfo("tempt_range", "TEMPT", "吸引范围", 10.0, 0.0, 2048.0),
            AttributeInfo("camera_distance", "CAMERA", "相机距离", 4.0, 0.0, 32.0)
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
