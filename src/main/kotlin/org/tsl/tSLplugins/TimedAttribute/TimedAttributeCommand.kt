package org.tsl.tSLplugins.TimedAttribute

import me.clip.placeholderapi.PlaceholderAPI
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.tsl.tSLplugins.SubCommandHandler
/**
 * 计时属性效果命令处理器（堆栈版）
 * 处理 /tsl attr 相关命令
 */
class TimedAttributeCommand(
    private val manager: TimedAttributeManager
) : SubCommandHandler {

    private val serializer = LegacyComponentSerializer.legacyAmpersand()

    override fun handle(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        // 检查功能是否启用
        if (!manager.isEnabled()) {
            sender.sendMessage(serializer.deserialize(manager.getMessage("disabled")))
            return true
        }

        if (args.isEmpty()) {
            showUsage(sender)
            return true
        }

        when (args[0].lowercase()) {
            "set" -> handleSet(sender, args.drop(1).toTypedArray())
            "cancel" -> handleRemove(sender, args.drop(1).toTypedArray())
            "list" -> handleList(sender, args.drop(1).toTypedArray())
            "stack" -> handleStack(sender, args.drop(1).toTypedArray())
            "clear" -> handleClear(sender, args.drop(1).toTypedArray())
            "reset" -> handleReset(sender, args.drop(1).toTypedArray())
            "help" -> handleHelp(sender, args.drop(1).toTypedArray())
            else -> showUsage(sender)
        }

        return true
    }

    /**
     * 处理设置属性命令
     * /tsl attr set <player> <attribute> <value> <duration>
     */
    private fun handleSet(sender: CommandSender, args: Array<out String>) {
        // 权限检查
        if (!sender.hasPermission("tsl.attribute.set")) {
            sender.sendMessage(serializer.deserialize(manager.getMessage("no_permission")))
            return
        }

        if (args.size < 4) {
            sender.sendMessage(serializer.deserialize(
                "&c用法: /tsl attr set <玩家> <属性> <数值> <时间>"
            ))
            sender.sendMessage(serializer.deserialize(
                "&7示例: /tsl attr set Steve SCALE 2 30s"
            ))
            sender.sendMessage(serializer.deserialize(
                "&7示例: /tsl attr set Steve HP 40 5m"
            ))
            sender.sendMessage(serializer.deserialize(
                "&7使用 &e/tsl attr help &7查看可用属性"
            ))
            return
        }

        val playerName = args[0]
        val attributeInput = args[1]
        var valueStr = args[2]
        var durationStr = args[3]

        // 查找玩家
        val target = Bukkit.getPlayer(playerName)
        if (target == null || !target.isOnline) {
            sender.sendMessage(serializer.deserialize(
                manager.getMessage("player_not_found", "player" to playerName)
            ))
            return
        }

        // 解析 PlaceholderAPI 变量（如果可用）
        if (isPlaceholderAPIAvailable()) {
            valueStr = PlaceholderAPI.setPlaceholders(target, valueStr)
            durationStr = PlaceholderAPI.setPlaceholders(target, durationStr)
        }

        // 解析属性
        val resolvedAttribute = manager.resolveAttributeKey(attributeInput)
        if (resolvedAttribute == null) {
            sender.sendMessage(serializer.deserialize(
                manager.getMessage("invalid_attribute", "attribute" to attributeInput)
            ))
            sender.sendMessage(serializer.deserialize(
                "&7使用 &e/tsl attr help &7查看可用属性"
            ))
            return
        }

        // 解析数值
        val value = valueStr.toDoubleOrNull()
        if (value == null) {
            sender.sendMessage(serializer.deserialize(
                manager.getMessage("invalid_amount", "amount" to valueStr)
            ))
            return
        }

        // 解析持续时间
        val durationMs = manager.parseDuration(durationStr)
        if (durationMs == null || durationMs <= 0) {
            sender.sendMessage(serializer.deserialize(
                manager.getMessage("invalid_duration", "duration" to durationStr)
            ))
            return
        }

        // 应用 SET 效果
        val modifierUuid = manager.applySetEffect(
            player = target,
            attributeKey = resolvedAttribute,
            targetValue = value,
            durationMs = durationMs,
            source = "command:${sender.name}"
        )

        if (modifierUuid != null) {
            val durationText = formatDuration(durationMs)
            sender.sendMessage(serializer.deserialize(
                "&a已为 &e${target.name} &a设置 &f$resolvedAttribute &a为 &b$value &7(${durationText})"
            ))
        } else {
            sender.sendMessage(serializer.deserialize(
                "&c设置属性失败，请检查属性名称是否正确"
            ))
        }
    }

    /**
     * 处理取消效果命令
     * /tsl attr cancel <player> <attribute>
     */
    private fun handleRemove(sender: CommandSender, args: Array<out String>) {
        // 权限检查
        if (!sender.hasPermission("tsl.attribute.remove")) {
            sender.sendMessage(serializer.deserialize(manager.getMessage("no_permission")))
            return
        }

        if (args.size < 2) {
            sender.sendMessage(serializer.deserialize(
                "&c用法: /tsl attr cancel <玩家> <属性> "
            ))
            sender.sendMessage(serializer.deserialize(
                "&7示例: /tsl attr cancel Steve SCALE"
            ))
            sender.sendMessage(serializer.deserialize(
                "&7取消该属性的所有堆叠效果，恢复到原始值"
            ))
            return
        }

        val playerName = args[0]
        val attributeInput = args[1]

        // 查找玩家
        val target = Bukkit.getPlayer(playerName)
        if (target == null || !target.isOnline) {
            sender.sendMessage(serializer.deserialize(
                manager.getMessage("player_not_found", "player" to playerName)
            ))
            return
        }

        // 解析属性
        val resolvedAttribute = manager.resolveAttributeKey(attributeInput)
        if (resolvedAttribute == null) {
            sender.sendMessage(serializer.deserialize(
                manager.getMessage("invalid_attribute", "attribute" to attributeInput)
            ))
            return
        }

        // 取消效果
        val count = manager.cancelEffects(target, resolvedAttribute)
        if (count > 0) {
            sender.sendMessage(serializer.deserialize(
                "&a已取消 &e${target.name} &a的 &f$resolvedAttribute &a效果 &7($count 层)"
            ))
        } else {
            sender.sendMessage(serializer.deserialize(
                manager.getMessage("effect_not_found")
            ))
        }
    }

    /**
     * 处理列出效果命令
     * /tsl attr list [player]
     */
    private fun handleList(sender: CommandSender, args: Array<out String>) {
        // 权限检查
        if (!sender.hasPermission("tsl.attribute.list")) {
            sender.sendMessage(serializer.deserialize(manager.getMessage("no_permission")))
            return
        }

        // 确定目标玩家
        val target: Player = if (args.isEmpty()) {
            if (sender is Player) {
                sender
            } else {
                sender.sendMessage(serializer.deserialize("&c用法: /tsl attr list <玩家>"))
                return
            }
        } else {
            val player = Bukkit.getPlayer(args[0])
            if (player == null || !player.isOnline) {
                sender.sendMessage(serializer.deserialize(
                    manager.getMessage("player_not_found", "player" to args[0])
                ))
                return
            }
            player
        }

        val effects = manager.listActiveEffects(target)

        if (effects.isEmpty()) {
            sender.sendMessage(serializer.deserialize(
                manager.getMessage("no_effects", "player" to target.name)
            ))
            return
        }

        sender.sendMessage(serializer.deserialize(
            manager.getMessage("list_header", "player" to target.name, "count" to effects.size.toString())
        ))

        effects.forEach { effect ->
            // 获取该属性的堆栈深度
            val stackDepth = manager.getEffectStack(target, effect.attributeKey).size
            val stackInfo = if (stackDepth > 1) " &8[堆栈: $stackDepth 层]" else ""
            val deltaStr = if (effect.delta >= 0) "+${formatValue(effect.delta)}" else formatValue(effect.delta)
            sender.sendMessage(serializer.deserialize(
                "&7- &f${effect.attributeKey} &b${formatValue(effect.targetValue)} &7(delta: $deltaStr) &7剩余: &e${effect.formatRemainingTime()}$stackInfo"
            ))
        }
    }

    /**
     * 处理查看堆栈命令
     * /tsl attr stack <player> <attribute>
     */
    private fun handleStack(sender: CommandSender, args: Array<out String>) {
        if (!sender.hasPermission("tsl.attribute.list")) {
            sender.sendMessage(serializer.deserialize(manager.getMessage("no_permission")))
            return
        }

        if (args.size < 2) {
            sender.sendMessage(serializer.deserialize(
                "&c用法: /tsl attr stack <玩家> <属性>"
            ))
            sender.sendMessage(serializer.deserialize(
                "&7查看指定属性的效果堆栈详情"
            ))
            return
        }

        val playerName = args[0]
        val attributeInput = args[1]

        val target = Bukkit.getPlayer(playerName)
        if (target == null || !target.isOnline) {
            sender.sendMessage(serializer.deserialize(
                manager.getMessage("player_not_found", "player" to playerName)
            ))
            return
        }

        val resolvedAttribute = manager.resolveAttributeKey(attributeInput)
        if (resolvedAttribute == null) {
            sender.sendMessage(serializer.deserialize(
                manager.getMessage("invalid_attribute", "attribute" to attributeInput)
            ))
            return
        }

        val stack = manager.getEffectStack(target, resolvedAttribute)
        if (stack.isEmpty()) {
            sender.sendMessage(serializer.deserialize(
                "&e${target.name} &7的 &f$resolvedAttribute &7没有活跃效果"
            ))
            return
        }

        sender.sendMessage(serializer.deserialize(
            "&6===== ${target.name} 的 $resolvedAttribute 效果堆栈 ====="
        ))
        
        // 计算总 delta
        val totalDelta = stack.sumOf { it.delta }
        val totalDeltaStr = if (totalDelta >= 0) "+${formatValue(totalDelta)}" else formatValue(totalDelta)
        sender.sendMessage(serializer.deserialize(
            "&7总变化量: &f$totalDeltaStr &7(取消后将撤销此变化)"
        ))
        sender.sendMessage(serializer.deserialize(""))

        // 从栈底到栈顶显示
        stack.forEachIndexed { index, effect ->
            val status = if (effect.isPaused) "&c[暂停]" else "&a[活跃]"
            val isTop = if (index == stack.size - 1) " &e← 当前" else ""
            val deltaStr = if (effect.delta >= 0) "+${formatValue(effect.delta)}" else formatValue(effect.delta)
            sender.sendMessage(serializer.deserialize(
                "&7层 ${index + 1}: &b${formatValue(effect.targetValue)} &7(delta: $deltaStr) $status &7剩余: ${effect.formatRemainingTime()}$isTop"
            ))
        }
    }

    /**
     * 处理清除效果命令
     * /tsl attr clear [player]
     */
    private fun handleClear(sender: CommandSender, args: Array<out String>) {
        // 权限检查
        if (!sender.hasPermission("tsl.attribute.clear")) {
            sender.sendMessage(serializer.deserialize(manager.getMessage("no_permission")))
            return
        }

        // 确定目标玩家
        val target: Player = if (args.isEmpty()) {
            if (sender is Player) {
                sender
            } else {
                sender.sendMessage(serializer.deserialize("&c用法: /tsl attr clear <玩家>"))
                return
            }
        } else {
            val player = Bukkit.getPlayer(args[0])
            if (player == null || !player.isOnline) {
                sender.sendMessage(serializer.deserialize(
                    manager.getMessage("player_not_found", "player" to args[0])
                ))
                return
            }
            player
        }

        val count = manager.clearAllEffects(target)

        if (count > 0) {
            sender.sendMessage(serializer.deserialize(
                manager.getMessage("clear_success",
                    "player" to target.name,
                    "count" to count.toString()
                )
            ))
        } else {
            sender.sendMessage(serializer.deserialize(
                manager.getMessage("no_effects", "player" to target.name)
            ))
        }
    }

    /**
     * 处理重置命令（恢复所有属性到游戏默认值）
     * /tsl attr reset <player>
     */
    private fun handleReset(sender: CommandSender, args: Array<out String>) {
        // 权限检查
        if (!sender.hasPermission("tsl.attribute.reset")) {
            sender.sendMessage(serializer.deserialize(manager.getMessage("no_permission")))
            return
        }

        if (args.isEmpty()) {
            sender.sendMessage(serializer.deserialize(
                "&c用法: /tsl attr reset <玩家>"
            ))
            sender.sendMessage(serializer.deserialize(
                "&7将玩家的所有属性恢复到游戏默认值"
            ))
            sender.sendMessage(serializer.deserialize(
                "&e警告: 这会清除所有效果并重置属性，用于修复旧版本 bug"
            ))
            return
        }

        val playerName = args[0]

        // 查找玩家
        val target = Bukkit.getPlayer(playerName)
        if (target == null || !target.isOnline) {
            sender.sendMessage(serializer.deserialize(
                manager.getMessage("player_not_found", "player" to playerName)
            ))
            return
        }

        // 执行重置
        val effectCount = manager.resetToDefault(target)
        
        sender.sendMessage(serializer.deserialize(
            "&a已重置 &e${target.name} &a的所有属性到默认值"
        ))
        if (effectCount > 0) {
            sender.sendMessage(serializer.deserialize(
                "&7清除了 $effectCount 层效果"
            ))
        }
    }

    /**
     * 处理帮助命令
     * /tsl attr help [属性名]
     */
    private fun handleHelp(sender: CommandSender, args: Array<out String>) {
        if (args.isEmpty()) {
            // 显示所有属性列表
            showAttributeList(sender)
        } else {
            // 显示指定属性的详细信息
            showAttributeDetail(sender, args[0])
        }
    }

    /**
     * 显示属性列表
     */
    private fun showAttributeList(sender: CommandSender) {
        sender.sendMessage(serializer.deserialize("&6&l======== 可用属性列表 (1.21.4) ========"))
        sender.sendMessage(serializer.deserialize("&7使用 &e/tsl attr help <属性名> &7查看详情"))
        sender.sendMessage(serializer.deserialize(""))

        // 生命与防御
        sender.sendMessage(serializer.deserialize("&c&l[生命与防御]"))
        sender.sendMessage(serializer.deserialize("  &fHP&7/HEALTH &8- &a最大生命值 &7(默认20)"))
        sender.sendMessage(serializer.deserialize("  &fABS&7/ABSORPTION &8- &a最大吸收值 &7(金心)"))
        sender.sendMessage(serializer.deserialize("  &fARMOR&7/DEF &8- &a护甲值 &7| &fTOUGHNESS &8- &a护甲韧性"))
        sender.sendMessage(serializer.deserialize("  &fKB_RES &8- &a击退抗性 &7| &fEXP_KB_RES &8- &a爆炸击退抗性"))

        // 攻击
        sender.sendMessage(serializer.deserialize("&c&l[攻击]"))
        sender.sendMessage(serializer.deserialize("  &fDMG&7/ATK &8- &a攻击伤害 &7| &fATK_SPEED &8- &a攻击速度"))
        sender.sendMessage(serializer.deserialize("  &fATK_KB &8- &a攻击击退 &7| &fSWEEP &8- &a横扫伤害比例"))

        // 移动
        sender.sendMessage(serializer.deserialize("&a&l[移动]"))
        sender.sendMessage(serializer.deserialize("  &fSPEED&7/SPD &8- &a移动速度 &7(默认0.1)"))
        sender.sendMessage(serializer.deserialize("  &fFLY &8- &a飞行速度 &7| &fSNEAK &8- &a潜行速度"))
        sender.sendMessage(serializer.deserialize("  &fSWIM &8- &a水中移动效率 &7| &fMOVE_EFF &8- &a移动效率"))

        // 体型与跳跃
        sender.sendMessage(serializer.deserialize("&b&l[体型与跳跃]"))
        sender.sendMessage(serializer.deserialize("  &fSCALE&7/SIZE &8- &a体型大小 &7(0.0625~16)"))
        sender.sendMessage(serializer.deserialize("  &fSTEP &8- &a跨步高度 &7| &fJUMP &8- &a跳跃力量"))
        sender.sendMessage(serializer.deserialize("  &fGRAV &8- &a重力 &7(负值=上浮)"))

        // 摔落
        sender.sendMessage(serializer.deserialize("&e&l[摔落]"))
        sender.sendMessage(serializer.deserialize("  &fFALL_DMG &8- &a摔落伤害倍率 &7| &fSAFE_FALL &8- &a安全摔落距离"))

        // 挖掘
        sender.sendMessage(serializer.deserialize("&d&l[挖掘]"))
        sender.sendMessage(serializer.deserialize("  &fDIG&7/BREAK_SPEED &8- &a方块破坏速度"))
        sender.sendMessage(serializer.deserialize("  &fMINE &8- &a挖掘效率 &7| &fWATER_MINE &8- &a水下挖掘速度"))

        // 交互范围
        sender.sendMessage(serializer.deserialize("&9&l[交互范围]"))
        sender.sendMessage(serializer.deserialize("  &fBLOCK_RANGE &8- &a方块交互范围 &7(默认4.5)"))
        sender.sendMessage(serializer.deserialize("  &fREACH&7/ENTITY_RANGE &8- &a实体交互范围 &7(默认3)"))

        // 其他
        sender.sendMessage(serializer.deserialize("&7&l[其他]"))
        sender.sendMessage(serializer.deserialize("  &fLUCK &8- &a幸运值 &7| &fOXYGEN&7/O2 &8- &a水下呼吸加成"))
        sender.sendMessage(serializer.deserialize("  &fBURN &8- &a燃烧时间倍率 &7| &fCAMERA &8- &a相机距离"))
        sender.sendMessage(serializer.deserialize("  &fFOLLOW &8- &a跟随范围 &7| &fTEMPT &8- &a吸引范围"))

        sender.sendMessage(serializer.deserialize(""))
        sender.sendMessage(serializer.deserialize("&6&l========= 使用示例 ========="))
        sender.sendMessage(serializer.deserialize("&e/tsl attr set Steve HP 40 5m &7- 生命设为40,5分钟"))
        sender.sendMessage(serializer.deserialize("&e/tsl attr set Steve SCALE 2 30s &7- 体型设为2,30秒"))
        sender.sendMessage(serializer.deserialize("&e/tsl attr set Steve SPEED 0.2 1h &7- 速度设为0.2,1小时"))
    }

    /**
     * 显示指定属性的详细信息
     */
    private fun showAttributeDetail(sender: CommandSender, attributeName: String) {
        val info = manager.getAttributeInfo(attributeName)

        if (info == null) {
            sender.sendMessage(serializer.deserialize("&c未找到属性: $attributeName"))
            sender.sendMessage(serializer.deserialize("&7使用 &e/tsl attr help &7查看所有可用属性"))
            return
        }

        sender.sendMessage(serializer.deserialize("&6&l===== 属性详情 ====="))
        sender.sendMessage(serializer.deserialize("&7属性键: &f${info.key}"))
        sender.sendMessage(serializer.deserialize("&7简称: &e${info.alias}"))
        sender.sendMessage(serializer.deserialize("&7描述: &a${info.description}"))
        sender.sendMessage(serializer.deserialize("&7默认值: &b${info.defaultValue}"))
        sender.sendMessage(serializer.deserialize("&7范围: &c${info.minValue} &7~ &a${info.maxValue}"))
        sender.sendMessage(serializer.deserialize(""))
        sender.sendMessage(serializer.deserialize("&7使用示例:"))
        sender.sendMessage(serializer.deserialize("&e/tsl attr set <玩家> ${info.alias} <数值> <时间>"))

        // 根据属性给出建议示例
        val exampleValue = when (info.key) {
            "max_health" -> "40"
            "movement_speed" -> "0.2"
            "attack_damage" -> "10"
            "scale" -> "2"
            "gravity" -> "0.04"
            else -> "5"
        }
        sender.sendMessage(serializer.deserialize("&7示例: &f/tsl attr set Steve ${info.alias} $exampleValue 5m"))
    }

    /**
     * 显示用法
     */
    private fun showUsage(sender: CommandSender) {
        sender.sendMessage(serializer.deserialize("&6&l===== 计时属性效果命令 (堆栈版) ====="))
        sender.sendMessage(serializer.deserialize("&e/tsl attr set <玩家> <属性> <数值> <时间>"))
        sender.sendMessage(serializer.deserialize("  &7设置属性值，新效果会暂停旧效果"))
        sender.sendMessage(serializer.deserialize("&e/tsl attr cancel <玩家> <属性> &7- 取消指定属性效果"))
        sender.sendMessage(serializer.deserialize("&e/tsl attr list [玩家] &7- 列出当前效果"))
        sender.sendMessage(serializer.deserialize("&e/tsl attr stack <玩家> <属性> &7- 查看堆栈详情"))
        sender.sendMessage(serializer.deserialize("&e/tsl attr clear [玩家] &7- 清除所有效果"))
        sender.sendMessage(serializer.deserialize("&e/tsl attr reset <玩家> &7- 重置所有属性到默认"))
        sender.sendMessage(serializer.deserialize("&e/tsl attr help [属性] &7- 查看可用属性"))
        sender.sendMessage(serializer.deserialize(""))
        sender.sendMessage(serializer.deserialize("&7时间格式: &f30s&7(秒) &f5m&7(分) &f2h&7(时) &f1d&7(天)"))
    }

    /**
     * 格式化持续时间
     */
    private fun formatDuration(ms: Long): String {
        val seconds = (ms / 1000).toInt()
        return when {
            seconds < 60 -> "${seconds}秒"
            seconds < 3600 -> "${seconds / 60}分${seconds % 60}秒"
            seconds < 86400 -> "${seconds / 3600}小时${(seconds % 3600) / 60}分"
            else -> "${seconds / 86400}天${(seconds % 86400) / 3600}小时"
        }
    }

    /**
     * 格式化数值
     */
    private fun formatValue(value: Double): String {
        return if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            String.format("%.2f", value)
        }
    }

    override fun tabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): List<String> {
        if (!manager.isEnabled()) return emptyList()

        return when (args.size) {
            1 -> {
                // 子命令
                listOf("set", "cancel", "list", "stack", "clear", "reset", "help")
                    .filter { it.startsWith(args[0], ignoreCase = true) }
            }
            2 -> {
                when (args[0].lowercase()) {
                    "set", "cancel", "list", "stack", "clear", "reset" -> {
                        // 玩家名
                        Bukkit.getOnlinePlayers()
                            .map { it.name }
                            .filter { it.startsWith(args[1], ignoreCase = true) }
                    }
                    "help" -> {
                        // 属性名
                        manager.getAttributeInfoList()
                            .flatMap { listOf(it.key, it.alias) }
                            .filter { it.startsWith(args[1], ignoreCase = true) }
                    }
                    else -> emptyList()
                }
            }
            3 -> {
                when (args[0].lowercase()) {
                    "set" -> {
                        // 属性名（常用简写优先）
                        val commonShorts = listOf(
                            "HP", "SCALE", "SPEED", "DMG", "ARMOR", "JUMP",
                            "ATK", "DEF", "FLY", "SNEAK", "GRAV", "LUCK",
                            "KB_RES", "REACH", "DIG", "FALL", "ABS"
                        )
                        val allSuggestions = commonShorts + manager.getAttributeInfoList().map { it.key }
                        allSuggestions.filter { it.startsWith(args[2], ignoreCase = true) }.distinct().take(20)
                    }
                    "cancel", "stack" -> {
                        // 列出该玩家的活跃效果的属性名
                        val player = Bukkit.getPlayer(args[1])
                        if (player != null) {
                            manager.listActiveEffects(player)
                                .map { it.attributeKey.uppercase() }
                                .filter { it.startsWith(args[2], ignoreCase = true) }
                        } else {
                            emptyList()
                        }
                    }
                    else -> emptyList()
                }
            }
            4 -> {
                when (args[0].lowercase()) {
                    "set" -> {
                        // set 命令的数值提示
                        val info = manager.getAttributeInfo(args[2])
                        if (info != null) {
                            // 根据属性给出合理的基础值建议
                            val suggestions = when (info.key) {
                                "max_health" -> listOf("20", "30", "40", "60", "100")
                                "movement_speed" -> listOf("0.1", "0.15", "0.2", "0.3")
                                "attack_damage" -> listOf("1", "5", "10", "15", "20")
                                "scale" -> listOf("0.5", "1", "1.5", "2", "3", "5")
                                "armor" -> listOf("0", "10", "20", "30")
                                else -> listOf("1", "5", "10", "20")
                            }
                            suggestions.filter { it.startsWith(args[3]) }
                        } else {
                            listOf("1", "5", "10", "20")
                                .filter { it.startsWith(args[3]) }
                        }
                    }
                    else -> emptyList()
                }
            }
            5 -> {
                when (args[0].lowercase()) {
                    "set" -> {
                        // 持续时间
                        listOf("30s", "1m", "5m", "10m", "30m", "1h", "1d")
                            .filter { it.startsWith(args[4], ignoreCase = true) }
                    }
                    else -> emptyList()
                }
            }
            else -> emptyList()
        }
    }

    override fun getDescription(): String {
        return "计时属性效果管理"
    }

    /**
     * 检查 PlaceholderAPI 是否可用
     */
    private fun isPlaceholderAPIAvailable(): Boolean {
        return Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null
    }
}

