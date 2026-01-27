package org.tsl.tSLplugins.modules.townphome

import me.clip.placeholderapi.PlaceholderAPI
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.tsl.tSLplugins.TSLplugins

/**
 * 小镇PHome管理器
 * 处理配置加载、PAPI变量解析、权限检查等核心逻辑
 */
class TownPHomeManager(private val plugin: JavaPlugin) {

    private var enabled: Boolean = false
    private val msg get() = (plugin as TSLplugins).messageManager

    val storage = TownPHomeStorage(plugin)

    private var papiTownName: String = "playerGuild_name"
    private var papiTownLevel: String = "playerGuild_guild_level"
    private var papiPlayerRole: String = "playerGuild_role"

    private var managementRoles: Set<String> = setOf("镇长", "副镇长", "Mayor", "Deputy")
    private var levelLimits: Map<Int, Int> = mapOf(1 to 3, 2 to 5, 3 to 8)
    private var defaultLimit: Int = 10

    private var papiAvailable: Boolean = false

    init {
        loadConfig()
        storage.loadAll()
        papiAvailable = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null
    }

    /**
     * 加载配置
     */
    fun loadConfig() {
        val config = plugin.config
        enabled = config.getBoolean("town-phome.enabled", false)

        papiTownName = config.getString("town-phome.papi-variables.town-name", "playerGuild_name") ?: "playerGuild_name"
        papiTownLevel = config.getString("town-phome.papi-variables.town-level", "playerGuild_guild_level") ?: "playerGuild_guild_level"
        papiPlayerRole = config.getString("town-phome.papi-variables.player-role", "playerGuild_role") ?: "playerGuild_role"

        managementRoles = config.getStringList("town-phome.management-roles").toSet()
        if (managementRoles.isEmpty()) {
            managementRoles = setOf("镇长", "副镇长", "Mayor", "Deputy")
        }

        levelLimits = mutableMapOf()
        val limitsSection = config.getConfigurationSection("town-phome.level-limits")
        if (limitsSection != null) {
            for (key in limitsSection.getKeys(false)) {
                if (key == "default") {
                    defaultLimit = limitsSection.getInt(key, 10)
                } else {
                    key.toIntOrNull()?.let { level ->
                        (levelLimits as MutableMap)[level] = limitsSection.getInt(key)
                    }
                }
            }
        }
    }

    fun isEnabled(): Boolean = enabled

    fun isPapiAvailable(): Boolean = papiAvailable

    fun getMessage(key: String, vararg replacements: Pair<String, String>): String {
        return msg.getModule("town-phome", key, *replacements)
    }

    /**
     * 获取玩家的小镇名称
     */
    fun getPlayerTownName(player: Player): String? {
        if (!papiAvailable) return null
        val result = PlaceholderAPI.setPlaceholders(player, "%$papiTownName%")
        if (result.isBlank() || result == "%$papiTownName%") return null
        return result
    }

    /**
     * 获取玩家的小镇等级
     */
    fun getPlayerTownLevel(player: Player): Int {
        if (!papiAvailable) return 1
        val result = PlaceholderAPI.setPlaceholders(player, "%$papiTownLevel%")
        return result.toIntOrNull() ?: 1
    }

    /**
     * 获取玩家在小镇中的职位
     */
    fun getPlayerRole(player: Player): String? {
        if (!papiAvailable) return null
        val result = PlaceholderAPI.setPlaceholders(player, "%$papiPlayerRole%")
        if (result.isBlank() || result == "%$papiPlayerRole%") return null
        return result
    }

    /**
     * 检查玩家是否有管理权限（镇长/副镇长等）
     */
    fun canManage(player: Player): Boolean {
        val role = getPlayerRole(player) ?: return false
        return managementRoles.any { it.equals(role, ignoreCase = true) }
    }

    /**
     * 获取小镇的PHome数量上限
     */
    fun getLimit(townLevel: Int): Int {
        return levelLimits[townLevel] ?: defaultLimit
    }

    /**
     * 检查是否可以创建新PHome（未达上限或覆盖已有）
     */
    fun canCreateHome(townName: String, homeName: String, townLevel: Int): Boolean {
        val currentCount = storage.getHomeCount(townName)
        val limit = getLimit(townLevel)
        val exists = storage.homeExists(townName, homeName)
        return exists || currentCount < limit
    }

    /**
     * 创建或覆盖PHome
     */
    fun setHome(townName: String, homeName: String, player: Player): Boolean {
        val loc = player.location
        val location = TownPHomeLocation(
            name = homeName,
            world = loc.world?.name ?: return false,
            x = loc.x,
            y = loc.y,
            z = loc.z,
            yaw = loc.yaw,
            pitch = loc.pitch,
            createdBy = player.uniqueId.toString()
        )
        return storage.setHome(townName, location)
    }

    /**
     * 删除PHome
     */
    fun removeHome(townName: String, homeName: String): Boolean {
        return storage.removeHome(townName, homeName)
    }

    /**
     * 获取PHome列表
     */
    fun getHomes(townName: String): Map<String, TownPHomeLocation> {
        return storage.getHomes(townName)
    }

    /**
     * 获取指定PHome
     */
    fun getHome(townName: String, homeName: String): TownPHomeLocation? {
        return storage.getHome(townName, homeName)
    }

    /**
     * 传送玩家到PHome
     */
    fun teleportToHome(player: Player, townName: String, homeName: String): TeleportResult {
        val home = storage.getHome(townName, homeName)
            ?: return TeleportResult.NOT_FOUND

        val world = Bukkit.getWorld(home.world)
            ?: return TeleportResult.WORLD_NOT_FOUND

        val location = Location(world, home.x, home.y, home.z, home.yaw, home.pitch)
        player.teleportAsync(location).thenAccept { success ->
            if (!success) {
                player.sendMessage(getMessage("teleport_failed"))
            }
        }
        return TeleportResult.SUCCESS
    }

    /**
     * 获取PHome数量
     */
    fun getHomeCount(townName: String): Int {
        return storage.getHomeCount(townName)
    }

    /**
     * 关闭时保存数据
     */
    fun shutdown() {
        storage.forceSaveAll()
    }
    
    /**
     * 通知小镇成员 phome 变更
     * @param townName 小镇名称
     * @param operator 操作者
     * @param homeName phome 名称
     * @param isCreate true=创建/更新, false=删除
     */
    fun notifyTownMembers(townName: String, operator: Player, homeName: String, isCreate: Boolean) {
        val serializer = LegacyComponentSerializer.legacyAmpersand()
        val action = if (isCreate) "设置" else "删除"
        val message = "&e[小镇公告] &f${operator.name} &7$action 了PHome: &a$homeName"
        
        // 查找同一小镇的所有在线玩家并通知（排除操作者自己）
        Bukkit.getOnlinePlayers()
            .filter { it.uniqueId != operator.uniqueId }
            .filter { getPlayerTownName(it) == townName }
            .forEach { member ->
                member.sendMessage(serializer.deserialize(message))
            }
    }
}

enum class TeleportResult {
    SUCCESS,
    NOT_FOUND,
    WORLD_NOT_FOUND
}
