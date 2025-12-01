package org.tsl.tSLplugins

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * TSL 玩家配置存储管理器
 * 负责加载、保存和缓存玩家配置文件
 */
class TSLPlayerProfileStore(private val plugin: JavaPlugin) {

    /** 玩家数据目录 */
    private val playerDataDir: File = File(plugin.dataFolder, "playerdata").apply {
        if (!exists()) {
            mkdirs()
            plugin.logger.info("[ProfileStore] 创建玩家数据目录: $absolutePath")
        }
    }

    /** 内存缓存：UUID -> Profile */
    private val profileCache = ConcurrentHashMap<UUID, TSLPlayerProfile>()

    // ==================== 获取配置 ====================

    /**
     * 获取玩家配置（从缓存）
     * @param uuid 玩家 UUID
     * @return 玩家配置，如果不存在则返回 null
     */
    fun get(uuid: UUID): TSLPlayerProfile? {
        return profileCache[uuid]
    }

    /**
     * 获取玩家配置，如果不存在则创建新的
     * @param uuid 玩家 UUID
     * @param playerName 玩家名称
     * @return 玩家配置
     */
    fun getOrCreate(uuid: UUID, playerName: String): TSLPlayerProfile {
        return profileCache.getOrPut(uuid) {
            TSLPlayerProfile(uuid = uuid, playerName = playerName)
        }
    }

    // ==================== 加载配置 ====================

    /**
     * 从文件加载玩家配置
     * @param uuid 玩家 UUID
     * @param playerName 玩家名称（用于新建配置）
     * @return 加载的配置，如果文件不存在则返回新配置
     */
    fun load(uuid: UUID, playerName: String): TSLPlayerProfile {
        val file = getPlayerFile(uuid)

        // 如果文件不存在，返回新配置
        if (!file.exists()) {
            val newProfile = TSLPlayerProfile(uuid = uuid, playerName = playerName)
            profileCache[uuid] = newProfile
            return newProfile
        }

        // 从文件加载
        return try {
            val config = YamlConfiguration.loadConfiguration(file)

            val profile = TSLPlayerProfile(
                uuid = uuid,
                playerName = config.getString("playerName", playerName) ?: playerName,
                kissEnabled = config.getBoolean("kissEnabled", true),
                rideEnabled = config.getBoolean("rideEnabled", true),
                tossEnabled = config.getBoolean("tossEnabled", true),
                allowPhantom = config.getBoolean("allowPhantom", false),
                tossVelocity = config.getDouble("tossVelocity", 1.5),
                migratedFromPdc = config.getBoolean("migratedFromPdc", false),
                lastSaved = config.getLong("lastSaved", System.currentTimeMillis())
            )

            // 放入缓存
            profileCache[uuid] = profile

            plugin.logger.info("[ProfileStore] 加载玩家配置: $playerName ($uuid)")
            profile
        } catch (e: Exception) {
            plugin.logger.severe("[ProfileStore] 加载玩家配置失败: $uuid - ${e.message}")
            e.printStackTrace()

            // 返回新配置
            val newProfile = TSLPlayerProfile(uuid = uuid, playerName = playerName)
            profileCache[uuid] = newProfile
            newProfile
        }
    }

    // ==================== 保存配置 ====================

    /**
     * 保存玩家配置到文件
     * @param profile 玩家配置
     */
    fun save(profile: TSLPlayerProfile) {
        try {
            val file = getPlayerFile(profile.uuid)
            val config = YamlConfiguration()

            // 更新保存时间
            profile.updateSaveTime()

            // 写入数据
            config.set("playerName", profile.playerName)
            config.set("kissEnabled", profile.kissEnabled)
            config.set("rideEnabled", profile.rideEnabled)
            config.set("tossEnabled", profile.tossEnabled)
            config.set("allowPhantom", profile.allowPhantom)
            config.set("tossVelocity", profile.tossVelocity)
            config.set("migratedFromPdc", profile.migratedFromPdc)
            config.set("lastSaved", profile.lastSaved)

            // 保存到文件
            config.save(file)

            plugin.logger.info("[ProfileStore] 保存玩家配置: ${profile.playerName} (${profile.uuid})")
        } catch (e: Exception) {
            plugin.logger.severe("[ProfileStore] 保存玩家配置失败: ${profile.uuid} - ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * 保存玩家配置（通过 UUID）
     */
    fun save(uuid: UUID) {
        profileCache[uuid]?.let { save(it) }
    }

    /**
     * 保存所有缓存的玩家配置
     */
    fun saveAll() {
        val count = profileCache.size
        plugin.logger.info("[ProfileStore] 开始保存 $count 个玩家配置...")

        var successCount = 0
        profileCache.values.forEach { profile ->
            try {
                save(profile)
                successCount++
            } catch (e: Exception) {
                plugin.logger.severe("[ProfileStore] 保存失败: ${profile.uuid}")
            }
        }

        plugin.logger.info("[ProfileStore] 保存完成: $successCount/$count 成功")
    }

    // ==================== 移除配置 ====================

    /**
     * 从缓存中移除玩家配置（玩家退出时调用）
     * @param uuid 玩家 UUID
     */
    fun remove(uuid: UUID) {
        profileCache.remove(uuid)
    }

    /**
     * 清空所有缓存
     */
    fun clearCache() {
        profileCache.clear()
    }

    // ==================== 工具方法 ====================

    /**
     * 获取玩家配置文件
     */
    private fun getPlayerFile(uuid: UUID): File {
        return File(playerDataDir, "$uuid.yml")
    }

    /**
     * 检查玩家配置文件是否存在
     */
    fun exists(uuid: UUID): Boolean {
        return getPlayerFile(uuid).exists()
    }

    /**
     * 获取缓存的玩家数量
     */
    fun getCacheSize(): Int {
        return profileCache.size
    }

    /**
     * 获取所有缓存的 UUID
     */
    fun getCachedUUIDs(): Set<UUID> {
        return profileCache.keys.toSet()
    }
}

