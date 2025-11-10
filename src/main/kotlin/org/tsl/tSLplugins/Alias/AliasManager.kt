package org.tsl.tSLplugins.Alias

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandMap
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * 命令别名管理器
 * 负责加载、存储和解析命令别名，并动态注册到 Bukkit 命令系统
 */
class AliasManager(private val plugin: JavaPlugin) {

    private val aliasMap = ConcurrentHashMap<String, String>()
    private val aliasFile: File = File(plugin.dataFolder, "aliases.yml")
    private val registeredCommands = mutableSetOf<String>()
    private val commandMap: CommandMap

    init {
        // 获取 CommandMap
        commandMap = getCommandMap()
        loadAliases()
    }

    /**
     * 获取 CommandMap（通过反射）
     */
    private fun getCommandMap(): CommandMap {
        return try {
            val field = Bukkit.getServer().javaClass.getDeclaredField("commandMap")
            field.isAccessible = true
            field.get(Bukkit.getServer()) as CommandMap
        } catch (e: Exception) {
            plugin.logger.severe("无法获取 CommandMap: ${e.message}")
            throw RuntimeException("无法初始化命令别名系统", e)
        }
    }

    /**
     * 注销所有已注册的别名命令
     */
    private fun unregisterAliases() {
        try {
            val knownCommandsField = commandMap.javaClass.getDeclaredField("knownCommands")
            knownCommandsField.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val knownCommands = knownCommandsField.get(commandMap) as MutableMap<String, Command>

            registeredCommands.forEach { alias ->
                knownCommands.remove(alias)
                knownCommands.remove("tslplugins:$alias")
            }
            registeredCommands.clear()
        } catch (e: Exception) {
            plugin.logger.warning("注销别名命令时出错: ${e.message}")
        }
    }

    /**
     * 动态注册别名命令
     */
    private fun registerAliasCommand(alias: String, targetCommand: String) {
        try {
            // 只注册简单别名，不注册子命令别名
            if (alias.contains(" ")) return

            val dynamicCommand = DynamicAliasCommand(alias, targetCommand, plugin)
            commandMap.register("tslplugins", dynamicCommand)
            registeredCommands.add(alias)

            plugin.logger.fine("已注册别名命令: $alias -> $targetCommand")
        } catch (e: Exception) {
            plugin.logger.warning("注册别名命令 $alias 时出错: ${e.message}")
        }
    }

    /**
     * 加载别名配置文件
     */
    fun loadAliases() {
        // 检查功能是否启用
        if (!plugin.config.getBoolean("alias.enabled", true)) {
            plugin.logger.info("命令别名功能已禁用")
            return
        }

        // 确保配置文件存在
        if (!aliasFile.exists()) {
            plugin.saveResource("aliases.yml", false)
        }

        // 注销旧的别名命令
        unregisterAliases()
        aliasMap.clear()

        val config = YamlConfiguration.loadConfiguration(aliasFile)
        val aliases = config.getStringList("aliases")

        var loadedCount = 0
        for (entry in aliases) {
            val parts = entry.split(":", limit = 2)
            if (parts.size == 2) {
                val alias = parts[0].trim().lowercase()
                val command = parts[1].trim()

                if (alias.isNotEmpty() && command.isNotEmpty()) {
                    aliasMap[alias] = command
                    // 动态注册别名命令
                    registerAliasCommand(alias, command)
                    loadedCount++
                }
            } else {
                plugin.logger.warning("无效的别名格式: $entry")
            }
        }

        plugin.logger.info("成功加载 $loadedCount 个命令别名")
    }

    /**
     * 重载别名配置
     */
    fun reloadAliases() {
        loadAliases()
        // 同步命令到所有在线玩家（让客户端更新命令列表）
        Bukkit.getOnlinePlayers().forEach { player ->
            player.updateCommands()
        }
    }

    /**
     * 获取所有已加载的别名数量
     */
    fun getAliasCount(): Int = aliasMap.size

    /**
     * 获取所有别名（用于调试）
     */
    fun getAllAliases(): Map<String, String> = aliasMap.toMap()
}

