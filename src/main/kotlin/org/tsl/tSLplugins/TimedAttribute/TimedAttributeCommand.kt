package org.tsl.tSLplugins.TimedAttribute

import me.clip.placeholderapi.PlaceholderAPI
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.tsl.tSLplugins.SubCommandHandler
import java.util.UUID

/**
 * 计时属性效果命令处理器
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
            "add" -> handleAdd(sender, args.drop(1).toTypedArray())
            "set" -> handleSet(sender, args.drop(1).toTypedArray())
            "remove" -> handleRemove(sender, args.drop(1).toTypedArray())
            "list" -> handleList(sender, args.drop(1).toTypedArray())
            "clear" -> handleClear(sender, args.drop(1).toTypedArray())
            "help" -> handleHelp(sender, args.drop(1).toTypedArray())
            else -> showUsage(sender)
        }

        return true
    }

    /**
     * 处理添加效果命令（ADD 类型 - 在当前值上增减）
     * /tsl attr add <player> <attribute> <+/-value> <duration>
     */
    private fun handleAdd(sender: CommandSender, args: Array<out String>) {
        // 权限检查
        if (!sender.hasPermission("tsl.attribute.add")) {
            sender.sendMessage(serializer.deserialize(manager.getMessage("no_permission")))
            return
        }

        if (args.size < 4) {
            sender.sendMessage(serializer.deserialize(
                "&c用法: /tsl attr add <玩家> <属性> <+/-数值> <时间>"
            ))
            sender.sendMessage(serializer.deserialize(
                "&7示例: /tsl attr add Steve SCALE +2 5m"
            ))
            sender.sendMessage(serializer.deserialize(
                "&7示例: /tsl attr add Steve HEALTH -5 30s"
            ))
            sender.sendMessage(serializer.deserialize(
                "&7ADD 在当前值上增减，支持 +N 或 -N 格式"
            ))
            return
        }

        val playerName = args[0]
        val attributeInput = args[1]
        var amountStr = args[2]
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
            amountStr = PlaceholderAPI.setPlaceholders(target, amountStr)
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

        // 解析增减量（支持 +N、-N、N 格式）
        val amount = amountStr.replace("+", "").toDoubleOrNull()
        if (amount == null) {
            sender.sendMessage(serializer.deserialize(
                manager.getMessage("invalid_amount", "amount" to amountStr)
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

        // 应用 ADD 效果
        val modifierUuid = manager.applyAddEffect(
            player = target,
            attributeKey = resolvedAttribute,
            amount = amount,
            durationMs = durationMs,
            source = "command:${sender.name}"
        )

        if (modifierUuid != null) {
            val durationText = formatDuration(durationMs)
            val sign = if (amount >= 0) "+" else ""
            sender.sendMessage(serializer.deserialize(
                "&a已为 &e${target.name} &a添加 &f$resolvedAttribute &a效果: &b$sign$amount &7(${durationText})"
            ))
        } else {
            sender.sendMessage(serializer.deserialize(
                manager.getMessage("add_failed")
            ))
        }
    }

    /**
     * 处理设置属性命令（SET 类型 - 直接设置固定值，覆盖之前所有操作）
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
                "&7示例: /tsl attr set Steve SCALE 5 30s"
            ))
            sender.sendMessage(serializer.deserialize(
                "&7SET 直接设置固定值，会覆盖之前所有效果"
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
            value = value,
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
     * 处理移除效果命令
     * /tsl attr remove <player> <modifierUuid>
     */
    private fun handleRemove(sender: CommandSender, args: Array<out String>) {
        // 权限检查
        if (!sender.hasPermission("tsl.attribute.remove")) {
            sender.sendMessage(serializer.deserialize(manager.getMessage("no_permission")))
            return
        }

        if (args.size < 2) {
            sender.sendMessage(serializer.deserialize(
                "&c用法: /tsl attr remove <玩家> <效果ID>"
            ))
            sender.sendMessage(serializer.deserialize(
                "&7效果ID可通过 /tsl attr list 查看"
            ))
            return
        }

        val playerName = args[0]
        val modifierUuidStr = args[1]

        // 查找玩家
        val target = Bukkit.getPlayer(playerName)
        if (target == null || !target.isOnline) {
            sender.sendMessage(serializer.deserialize(
                manager.getMessage("player_not_found", "player" to playerName)
            ))
            return
        }

        // 解析 UUID（支持短格式）
        val modifierUuid = parseModifierUuid(target, modifierUuidStr)
        if (modifierUuid == null) {
            sender.sendMessage(serializer.deserialize(
                manager.getMessage("invalid_modifier_uuid", "uuid" to modifierUuidStr)
            ))
            return
        }

        // 移除效果
        val success = manager.removeEffect(target, modifierUuid)
        if (success) {
            sender.sendMessage(serializer.deserialize(
                manager.getMessage("remove_success",
                    "player" to target.name,
                    "modifier_uuid" to modifierUuid.toString().substring(0, 8)
                )
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
            val typeStr = if (effect.effectType == EffectType.SET) "SET" else "ADD"
            val amountStr = if (effect.effectType == EffectType.ADD) {
                val sign = if (effect.amount >= 0) "+" else ""
                "$sign${formatAmount(effect.amount)}"
            } else {
                formatAmount(effect.amount)
            }
            sender.sendMessage(serializer.deserialize(
                "&7- &f${effect.attributeKey} &8[$typeStr] &b$amountStr &7剩余: &e${effect.formatRemainingTime()} &8(${effect.modifierUuid.toString().substring(0, 8)})"
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
        sender.sendMessage(serializer.deserialize("&e/tsl attr add Steve HP +10 5m &7- 增加10点生命,5分钟"))
        sender.sendMessage(serializer.deserialize("&e/tsl attr add Steve SCALE +1 30s &7- 体型+1,30秒"))
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
        sender.sendMessage(serializer.deserialize("&e/tsl attr add <玩家> ${info.alias} <数值> <时间>"))

        // 根据属性给出建议示例
        val exampleValue = when (info.key) {
            "max_health" -> "+10"
            "movement_speed" -> "+0.05"
            "attack_damage" -> "+5"
            "scale" -> "+0.5"
            "gravity" -> "-0.04"
            else -> "+1"
        }
        sender.sendMessage(serializer.deserialize("&7示例: &f/tsl attr add Steve ${info.alias} $exampleValue 5m"))
    }

    /**
     * 显示用法
     */
    private fun showUsage(sender: CommandSender) {
        sender.sendMessage(serializer.deserialize("&6&l===== 计时属性效果命令 ====="))
        sender.sendMessage(serializer.deserialize("&e/tsl attr add <玩家> <属性> <+/-数值> <时间>"))
        sender.sendMessage(serializer.deserialize("  &7在当前值基础上增减，支持 &f+10&7 或 &f-5&7 格式"))
        sender.sendMessage(serializer.deserialize("&e/tsl attr set <玩家> <属性> <数值> <时间>"))
        sender.sendMessage(serializer.deserialize("  &7设置固定值，会覆盖之前的效果"))
        sender.sendMessage(serializer.deserialize("&e/tsl attr remove <玩家> <ID> &7- 移除指定效果"))
        sender.sendMessage(serializer.deserialize("&e/tsl attr list [玩家] &7- 列出当前效果"))
        sender.sendMessage(serializer.deserialize("&e/tsl attr clear [玩家] &7- 清除所有效果"))
        sender.sendMessage(serializer.deserialize("&e/tsl attr help [属性] &7- 查看可用属性"))
        sender.sendMessage(serializer.deserialize(""))
        sender.sendMessage(serializer.deserialize("&7时间格式: &f30s&7(秒) &f5m&7(分) &f2h&7(时) &f1d&7(天)"))
        sender.sendMessage(serializer.deserialize("&7常用属性: &fHP SCALE SPEED DMG ARMOR JUMP"))
    }

    /**
     * 解析修改器 UUID（支持短格式匹配）
     */
    private fun parseModifierUuid(player: Player, input: String): UUID? {
        // 尝试完整 UUID
        try {
            return UUID.fromString(input)
        } catch (e: Exception) {
            // 忽略，尝试短格式
        }

        // 短格式匹配（前8位）
        val effects = manager.listActiveEffects(player)
        val matches = effects.filter { it.modifierUuid.toString().startsWith(input, ignoreCase = true) }

        return when {
            matches.size == 1 -> matches[0].modifierUuid
            matches.size > 1 -> null // 多个匹配，需要更精确的输入
            else -> null
        }
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
     * 格式化数值（带正负号）
     */
    private fun formatAmount(amount: Double): String {
        val formatted = if (amount == amount.toLong().toDouble()) {
            amount.toLong().toString()
        } else {
            String.format("%.2f", amount)
        }
        return if (amount >= 0) "+$formatted" else formatted
    }

    /**
     * 解析带模式的数值
     *
     * @param player 目标玩家
     * @param attributeKey 已解析的属性键名
     * @param input 输入字符串
     * @return Triple(实际应用的数值, 是否为绝对设置模式, 操作描述)
     *
     * 支持格式：
     * - +N：在当前基础上增加 N
     * - -N：在当前基础上减少 N
     * - N（无符号）：直接将属性设置为 N
     */
    private fun parseAmountWithMode(
        player: Player,
        attributeKey: String,
        input: String
    ): Triple<Double?, Boolean, String> {
        val trimmed = input.trim()

        // 判断模式
        return when {
            // 以 + 开头：增加模式
            trimmed.startsWith("+") -> {
                val value = trimmed.substring(1).toDoubleOrNull()
                Triple(value, false, "增加")
            }
            // 以 - 开头：减少模式
            trimmed.startsWith("-") -> {
                val value = trimmed.toDoubleOrNull() // 包含负号一起解析
                Triple(value, false, "减少")
            }
            // 无符号：直接设置模式
            else -> {
                val targetValue = trimmed.toDoubleOrNull()
                if (targetValue == null) {
                    return Triple(null, true, "设置为")
                }

                // 获取玩家当前属性的基础值
                val currentBaseValue = getPlayerAttributeBaseValue(player, attributeKey)
                if (currentBaseValue == null) {
                    // 如果无法获取当前值，直接使用目标值作为增量
                    return Triple(targetValue, true, "设置为")
                }

                // 计算差值：目标值 - 当前基础值
                val delta = targetValue - currentBaseValue
                Triple(delta, true, "设置为 $targetValue (差值)")
            }
        }
    }

    /**
     * 获取玩家属性的基础值
     */
    private fun getPlayerAttributeBaseValue(player: Player, attributeKey: String): Double? {
        return try {
            val attribute = org.bukkit.Registry.ATTRIBUTE.get(
                org.bukkit.NamespacedKey.minecraft(attributeKey.lowercase())
            ) ?: return null
            val attrInstance = player.getAttribute(attribute) ?: return null
            attrInstance.baseValue
        } catch (e: Exception) {
            null
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
                listOf("add", "set", "remove", "list", "clear", "help")
                    .filter { it.startsWith(args[0], ignoreCase = true) }
            }
            2 -> {
                when (args[0].lowercase()) {
                    "add", "set", "remove", "list", "clear" -> {
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
                    "add", "set" -> {
                        // 属性名（常用简写优先）
                        val commonShorts = listOf(
                            "HP", "SCALE", "SPEED", "DMG", "ARMOR", "JUMP",
                            "ATK", "DEF", "FLY", "SNEAK", "GRAV", "LUCK",
                            "KB_RES", "REACH", "DIG", "FALL", "ABS"
                        )
                        val allSuggestions = commonShorts + manager.getAttributeInfoList().map { it.key }
                        allSuggestions.filter { it.startsWith(args[2], ignoreCase = true) }.distinct().take(20)
                    }
                    "remove" -> {
                        // 修改器 UUID（列出该玩家的活跃效果）
                        val player = Bukkit.getPlayer(args[1])
                        if (player != null) {
                            manager.listActiveEffects(player)
                                .map { it.modifierUuid.toString().substring(0, 8) }
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
                    "add" -> {
                        // 数值提示（支持三种模式：+N增加, -N减少, N直接设置）
                        val info = manager.getAttributeInfo(args[2])
                        if (info != null) {
                            // 根据属性给出合理的建议值
                            val suggestions = when (info.key) {
                                "max_health" -> listOf("+5", "+10", "-5", "-10", "20", "30", "40")
                                "movement_speed" -> listOf("+0.05", "+0.1", "-0.05", "0.1", "0.15")
                                "attack_damage" -> listOf("+2", "+5", "-2", "5", "10", "15")
                                "scale" -> listOf("+0.5", "-0.3", "1", "1.5", "2")
                                "armor" -> listOf("+5", "+10", "-5", "10", "15", "20")
                                else -> listOf("+1", "+5", "-1", "-5", "5", "10")
                            }
                            suggestions.filter { it.startsWith(args[3]) }
                        } else {
                            listOf("+1", "+5", "-1", "-5", "5", "10")
                                .filter { it.startsWith(args[3]) }
                        }
                    }
                    "set" -> {
                        // set 命令的数值提示（直接设置基础值）
                        val info = manager.getAttributeInfo(args[2])
                        if (info != null) {
                            // 根据属性给出合理的基础值建议
                            val suggestions = when (info.key) {
                                "max_health" -> listOf("20", "30", "40", "60", "100")
                                "movement_speed" -> listOf("0.1", "0.15", "0.2", "0.3")
                                "attack_damage" -> listOf("1", "5", "10", "15", "20")
                                "scale" -> listOf("0.5", "1", "1.5", "2", "3")
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
                    "add" -> {
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

