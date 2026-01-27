package org.tsl.tSLplugins.modules.papialias

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.tsl.tSLplugins.SubCommandHandler
import org.tsl.tSLplugins.core.AbstractModule
import java.util.concurrent.ConcurrentHashMap

/**
 * PapiAlias 模块 - 变量映射
 * 
 * 将 PAPI 变量值映射为简写形式。
 * 使用 ConcurrentHashMap 确保 Folia 多线程环境下的线程安全。
 */
class PapiAliasModule : AbstractModule() {

    override val id = "papialias"
    override val configPath = "papi-alias"
    override fun getDescription() = "变量映射"

    // 映射存储：外层 key 是变量名，内层是 原始值 -> 简写值
    @Volatile
    private var aliasMappings: ConcurrentHashMap<String, ConcurrentHashMap<String, String>> = ConcurrentHashMap()
    
    private var returnOriginalIfNotFound: Boolean = true

    override fun loadConfig() {
        super.loadConfig()
        returnOriginalIfNotFound = getConfigBoolean("return-original-if-not-found", true)
        loadMappings()
    }

    override fun doEnable() {
        logInfo("已加载 ${getMappingCount()} 个变量映射")
    }

    override fun doReload() {
        loadMappings()
        logInfo("已重载 ${getMappingCount()} 个变量映射")
    }

    override fun getCommandHandler(): SubCommandHandler = PapiAliasModuleCommand(this)

    private fun loadMappings() {
        val tempMappings = ConcurrentHashMap<String, ConcurrentHashMap<String, String>>()

        val mappingsSection = context.config.getConfigurationSection("$configPath.mappings")
        if (mappingsSection != null) {
            for (variableName in mappingsSection.getKeys(false)) {
                val variableSection = mappingsSection.getConfigurationSection(variableName)
                if (variableSection != null) {
                    val valueMap = ConcurrentHashMap<String, String>()
                    for (originalValue in variableSection.getKeys(false)) {
                        val aliasValue = variableSection.getString(originalValue)
                        if (aliasValue != null) {
                            valueMap[originalValue] = aliasValue
                        }
                    }
                    if (valueMap.isNotEmpty()) {
                        tempMappings[variableName.lowercase()] = valueMap
                    }
                }
            }
        }

        aliasMappings = tempMappings
    }

    /**
     * 获取变量的别名值
     */
    fun getAliasValue(variableName: String, originalValue: String): String {
        if (!isEnabled()) return originalValue

        val variableMap = aliasMappings[variableName.lowercase()]
        if (variableMap == null) {
            return if (returnOriginalIfNotFound) originalValue else ""
        }

        // 1. 精确匹配
        variableMap[originalValue]?.let { return it }

        // 2. 中文匹配
        val chineseOriginal = extractChinese(originalValue)
        if (chineseOriginal.isNotEmpty()) {
            for ((key, value) in variableMap) {
                val chineseKey = extractChinese(key)
                if (chineseKey == chineseOriginal || 
                    (chineseOriginal.contains(chineseKey) && chineseKey.isNotEmpty())) {
                    return value
                }
            }
        }

        // 3. 去色后匹配
        val strippedOriginal = stripColorCodes(originalValue)
        for ((key, value) in variableMap) {
            val strippedKey = stripColorCodes(key)
            if (strippedKey == strippedOriginal || 
                (strippedOriginal.contains(strippedKey) && strippedKey.isNotEmpty())) {
                return value
            }
        }

        return if (returnOriginalIfNotFound) originalValue else ""
    }

    private fun extractChinese(text: String): String {
        return text.filter { it.code in 0x4E00..0x9FFF || it.code in 0x3400..0x4DBF }
    }

    private fun stripColorCodes(text: String): String {
        var result = text
        result = result.replace(Regex("<[^>]+>"), "")
        result = result.replace(Regex("&#[0-9a-fA-F]{6}"), "")
        result = result.replace(Regex("&x(&[0-9a-fA-F]){6}"), "")
        result = result.replace(Regex("§x(§[0-9a-fA-F]){6}"), "")
        result = result.replace(Regex("[&§][0-9a-fA-Fk-oK-OrR]"), "")
        return result
    }

    fun hasMapping(variableName: String): Boolean = aliasMappings.containsKey(variableName.lowercase())
    fun getMappedVariables(): Set<String> = aliasMappings.keys.toSet()
    fun getVariableMappings(variableName: String): Map<String, String> = aliasMappings[variableName.lowercase()]?.toMap() ?: emptyMap()
    fun getMappingCount(): Int = aliasMappings.values.sumOf { it.size }
    fun getVariableCount(): Int = aliasMappings.size

    fun debugPrintMappings(): List<String> {
        val lines = mutableListOf("PapiAlias 映射状态: enabled=${isEnabled()}, returnOriginal=$returnOriginalIfNotFound")
        for ((varName, mappings) in aliasMappings) {
            lines.add("变量 [$varName]:")
            for ((original, alias) in mappings) {
                lines.add("  '$original' -> '$alias'")
            }
        }
        return lines
    }
}

/**
 * PapiAlias 命令处理器
 */
class PapiAliasModuleCommand(private val module: PapiAliasModule) : SubCommandHandler {

    override fun handle(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) { showUsage(sender); return true }

        when (args[0].lowercase()) {
            "reload" -> handleReload(sender)
            "info" -> handleInfo(sender)
            "list" -> handleList(sender, args.drop(1).toTypedArray())
            "debug" -> handleDebug(sender)
            "test" -> handleTest(sender, args.drop(1).toTypedArray())
            else -> showUsage(sender)
        }
        return true
    }

    private fun handleReload(sender: CommandSender) {
        if (!sender.hasPermission("tsl.papialias.reload")) {
            sender.sendMessage(Component.text("你没有权限执行此命令").color(NamedTextColor.RED))
            return
        }
        module.onReload()
        sender.sendMessage(Component.text("[PapiAlias] ").color(NamedTextColor.GOLD)
            .append(Component.text("配置已重载，共加载 ").color(NamedTextColor.GREEN))
            .append(Component.text("${module.getVariableCount()}").color(NamedTextColor.AQUA))
            .append(Component.text(" 个变量，").color(NamedTextColor.GREEN))
            .append(Component.text("${module.getMappingCount()}").color(NamedTextColor.AQUA))
            .append(Component.text(" 个映射").color(NamedTextColor.GREEN)))
    }

    private fun handleInfo(sender: CommandSender) {
        if (!sender.hasPermission("tsl.papialias.info")) {
            sender.sendMessage(Component.text("你没有权限执行此命令").color(NamedTextColor.RED))
            return
        }
        sender.sendMessage(Component.text("===== PapiAlias 信息 =====").color(NamedTextColor.GOLD))
        sender.sendMessage(Component.text("状态: ").color(NamedTextColor.GRAY)
            .append(if (module.isEnabled()) Component.text("已启用").color(NamedTextColor.GREEN) 
                    else Component.text("已禁用").color(NamedTextColor.RED)))
        sender.sendMessage(Component.text("变量数量: ").color(NamedTextColor.GRAY)
            .append(Component.text("${module.getVariableCount()}").color(NamedTextColor.AQUA)))
        sender.sendMessage(Component.text("映射总数: ").color(NamedTextColor.GRAY)
            .append(Component.text("${module.getMappingCount()}").color(NamedTextColor.AQUA)))
        sender.sendMessage(Component.text("使用方式: ").color(NamedTextColor.GRAY)
            .append(Component.text("%tsl_alias_<变量名>%").color(NamedTextColor.YELLOW)))
    }

    private fun handleList(sender: CommandSender, args: Array<out String>) {
        if (!sender.hasPermission("tsl.papialias.list")) {
            sender.sendMessage(Component.text("你没有权限执行此命令").color(NamedTextColor.RED))
            return
        }
        if (args.isEmpty()) {
            val variables = module.getMappedVariables()
            if (variables.isEmpty()) {
                sender.sendMessage(Component.text("[PapiAlias] 暂无配置任何映射").color(NamedTextColor.YELLOW))
                return
            }
            sender.sendMessage(Component.text("===== 已配置的变量 =====").color(NamedTextColor.GOLD))
            for (variable in variables) {
                val mappings = module.getVariableMappings(variable)
                sender.sendMessage(Component.text("  $variable ").color(NamedTextColor.WHITE)
                    .append(Component.text("(${mappings.size} 个映射)").color(NamedTextColor.GRAY)))
            }
        } else {
            val mappings = module.getVariableMappings(args[0])
            if (mappings.isEmpty()) {
                sender.sendMessage(Component.text("[PapiAlias] 变量 '${args[0]}' 没有配置映射").color(NamedTextColor.YELLOW))
                return
            }
            sender.sendMessage(Component.text("===== ${args[0]} 的映射 =====").color(NamedTextColor.GOLD))
            for ((original, alias) in mappings) {
                sender.sendMessage(Component.text("  \"$original\" → \"$alias\"").color(NamedTextColor.WHITE))
            }
        }
    }

    private fun handleDebug(sender: CommandSender) {
        if (!sender.hasPermission("tsl.papialias.debug")) {
            sender.sendMessage(Component.text("你没有权限执行此命令").color(NamedTextColor.RED))
            return
        }
        for (line in module.debugPrintMappings()) {
            sender.sendMessage(Component.text(line).color(NamedTextColor.WHITE))
        }
    }

    private fun handleTest(sender: CommandSender, args: Array<out String>) {
        if (!sender.hasPermission("tsl.papialias.test")) {
            sender.sendMessage(Component.text("你没有权限执行此命令").color(NamedTextColor.RED))
            return
        }
        if (args.size < 2) {
            sender.sendMessage(Component.text("用法: /tsl papialias test <变量名> <原始值>").color(NamedTextColor.YELLOW))
            return
        }
        val variableName = args[0]
        val originalValue = args.drop(1).joinToString(" ")
        val result = module.getAliasValue(variableName, originalValue)
        sender.sendMessage(Component.text("'$originalValue' → '$result'").color(
            if (result != originalValue) NamedTextColor.GREEN else NamedTextColor.YELLOW))
    }

    private fun showUsage(sender: CommandSender) {
        sender.sendMessage(Component.text("===== PapiAlias 命令 =====").color(NamedTextColor.GOLD))
        sender.sendMessage(Component.text("/tsl papialias reload - 重载配置").color(NamedTextColor.GRAY))
        sender.sendMessage(Component.text("/tsl papialias info - 显示信息").color(NamedTextColor.GRAY))
        sender.sendMessage(Component.text("/tsl papialias list [变量] - 列出映射").color(NamedTextColor.GRAY))
        sender.sendMessage(Component.text("/tsl papialias test <变量> <值> - 测试映射").color(NamedTextColor.GRAY))
    }

    override fun tabComplete(sender: CommandSender, command: Command, label: String, args: Array<out String>): List<String> {
        return when (args.size) {
            1 -> listOf("reload", "info", "list", "debug", "test").filter { it.startsWith(args[0], true) }
            2 -> if (args[0].lowercase() in listOf("list", "test")) 
                     module.getMappedVariables().filter { it.startsWith(args[1], true) }.toList()
                 else emptyList()
            else -> emptyList()
        }
    }

    override fun getDescription() = "变量映射管理"
}
