package org.tsl.tSLplugins.TownPHome

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 小镇PHome数据持久化存储
 * 使用JSON文件存储，脏标记 + 定期保存机制
 */
class TownPHomeStorage(private val plugin: JavaPlugin) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val dataFile: File
        get() = File(plugin.dataFolder, "town-phomes.json")

    private val townHomes: MutableMap<String, TownPHome> = ConcurrentHashMap()

    // 脏标记
    private val dirty = AtomicBoolean(false)

    // 自动保存间隔（秒）
    private val autoSaveIntervalSeconds = 60L

    /**
     * 加载所有数据并启动自动保存
     */
    fun loadAll() {
        townHomes.clear()
        if (!dataFile.exists()) {
            plugin.logger.info("[TownPHome] 数据文件不存在，将创建新文件")
            startAutoSave()
            return
        }

        try {
            val content = dataFile.readText()
            if (content.isBlank()) {
                startAutoSave()
                return
            }

            val list: List<TownPHome> = json.decodeFromString(content)
            list.forEach { townPHome ->
                townHomes[townPHome.townName.lowercase()] = townPHome
            }
            plugin.logger.info("[TownPHome] 已加载 ${townHomes.size} 个小镇的PHome数据")
        } catch (e: Exception) {
            plugin.logger.severe("[TownPHome] 加载数据失败: ${e.message}")
            e.printStackTrace()
        }
        startAutoSave()
    }

    /**
     * 启动自动保存任务
     */
    private fun startAutoSave() {
        Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, { _ ->
            saveAll()
        }, autoSaveIntervalSeconds * 20L, autoSaveIntervalSeconds * 20L)
        plugin.logger.info("[TownPHome] 自动保存任务已启动 (间隔: ${autoSaveIntervalSeconds}秒)")
    }

    /**
     * 保存所有数据（仅在有变更时保存）
     */
    fun saveAll() {
        if (!dirty.compareAndSet(true, false)) return
        doSave()
    }

    /**
     * 强制保存所有数据（不检查脏标记）
     */
    fun forceSaveAll() {
        dirty.set(false)
        doSave()
    }

    /**
     * 实际执行保存
     */
    private fun doSave() {
        try {
            if (!plugin.dataFolder.exists()) {
                plugin.dataFolder.mkdirs()
            }
            val content = json.encodeToString(townHomes.values.toList())
            dataFile.writeText(content)
        } catch (e: Exception) {
            plugin.logger.severe("[TownPHome] 保存数据失败: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * 标记数据需要保存
     */
    private fun markDirty() {
        dirty.set(true)
    }

    /**
     * 获取所有有 PHome 的小镇名称
     */
    fun getAllTownNames(): List<String> {
        return townHomes.values
            .filter { it.homes.isNotEmpty() }
            .map { it.townName }
            .sortedBy { it.lowercase() }
    }

    /**
     * 获取小镇的PHome数据（不存在则创建）
     */
    fun getTownPHome(townName: String): TownPHome {
        val key = townName.lowercase()
        return townHomes.getOrPut(key) {
            TownPHome(townName = townName)
        }
    }

    /**
     * 获取小镇的PHome列表
     */
    fun getHomes(townName: String): Map<String, TownPHomeLocation> {
        return getTownPHome(townName).homes.toMap()
    }

    /**
     * 获取指定PHome
     */
    fun getHome(townName: String, homeName: String): TownPHomeLocation? {
        return getTownPHome(townName).homes[homeName.lowercase()]
    }

    /**
     * 设置PHome（创建或覆盖）
     */
    fun setHome(townName: String, location: TownPHomeLocation): Boolean {
        val townPHome = getTownPHome(townName)
        townPHome.homes[location.name.lowercase()] = location
        markDirty()
        return true
    }

    /**
     * 删除PHome
     */
    fun removeHome(townName: String, homeName: String): Boolean {
        val townPHome = getTownPHome(townName)
        val removed = townPHome.homes.remove(homeName.lowercase()) != null
        if (removed) {
            markDirty()
        }
        return removed
    }

    /**
     * 检查PHome是否存在
     */
    fun homeExists(townName: String, homeName: String): Boolean {
        return getTownPHome(townName).homes.containsKey(homeName.lowercase())
    }

    /**
     * 获取小镇PHome数量
     */
    fun getHomeCount(townName: String): Int {
        return getTownPHome(townName).homes.size
    }
}
