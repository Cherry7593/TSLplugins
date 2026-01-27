package org.tsl.tSLplugins.modules.landmark

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 地标数据持久化存储
 * 使用脏标记 + 定期保存机制优化性能
 */
class LandmarkStorage(private val plugin: JavaPlugin) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val landmarksFile: File
        get() = File(plugin.dataFolder, "landmarks.json")

    private val unlocksFile: File
        get() = File(plugin.dataFolder, "landmark-unlocks.json")

    // 内存缓存
    private val landmarks: MutableMap<String, Landmark> = ConcurrentHashMap()
    private val playerUnlocks: MutableMap<String, PlayerUnlocks> = ConcurrentHashMap()

    // 世界索引缓存（性能优化）
    private val worldIndex: MutableMap<String, MutableList<Landmark>> = ConcurrentHashMap()

    // 脏标记（标记数据是否需要保存）
    private val landmarksDirty = AtomicBoolean(false)
    private val unlocksDirty = AtomicBoolean(false)

    // 自动保存间隔（秒）
    private val autoSaveIntervalSeconds = 60L

    /**
     * 加载所有数据并启动自动保存
     */
    fun loadAll() {
        loadLandmarks()
        loadUnlocks()
        startAutoSave()
    }

    /**
     * 保存所有数据（仅保存有变更的数据）
     */
    fun saveAll() {
        if (landmarksDirty.compareAndSet(true, false)) {
            doSaveLandmarks()
        }
        if (unlocksDirty.compareAndSet(true, false)) {
            doSaveUnlocks()
        }
    }

    /**
     * 强制保存所有数据（不检查脏标记）
     */
    fun forceSaveAll() {
        landmarksDirty.set(false)
        unlocksDirty.set(false)
        doSaveLandmarks()
        doSaveUnlocks()
    }

    /**
     * 启动自动保存任务
     */
    private fun startAutoSave() {
        Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, { _ ->
            saveAll()
        }, autoSaveIntervalSeconds * 20L, autoSaveIntervalSeconds * 20L)
        plugin.logger.info("[Landmark] 自动保存任务已启动 (间隔: ${autoSaveIntervalSeconds}秒)")
    }

    /**
     * 加载地标数据
     */
    private fun loadLandmarks() {
        landmarks.clear()
        if (!landmarksFile.exists()) {
            plugin.logger.info("[Landmark] 地标数据文件不存在，将创建新文件")
            return
        }

        try {
            val content = landmarksFile.readText()
            if (content.isBlank()) return

            val list: List<Landmark> = json.decodeFromString(content)
            list.forEach { landmark ->
                landmarks[landmark.id] = landmark
            }
            rebuildWorldIndex()
            plugin.logger.info("[Landmark] 已加载 ${landmarks.size} 个地标")
        } catch (e: Exception) {
            plugin.logger.severe("[Landmark] 加载地标数据失败: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * 标记地标数据需要保存
     */
    fun saveLandmarks() {
        landmarksDirty.set(true)
    }

    /**
     * 实际执行地标数据保存
     */
    private fun doSaveLandmarks() {
        try {
            if (!plugin.dataFolder.exists()) {
                plugin.dataFolder.mkdirs()
            }
            val content = json.encodeToString(landmarks.values.toList())
            landmarksFile.writeText(content)
        } catch (e: Exception) {
            plugin.logger.severe("[Landmark] 保存地标数据失败: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * 加载解锁数据
     */
    private fun loadUnlocks() {
        playerUnlocks.clear()
        if (!unlocksFile.exists()) {
            plugin.logger.info("[Landmark] 解锁数据文件不存在，将创建新文件")
            return
        }

        try {
            val content = unlocksFile.readText()
            if (content.isBlank()) return

            val list: List<PlayerUnlocks> = json.decodeFromString(content)
            list.forEach { unlock ->
                playerUnlocks[unlock.playerUuid] = unlock
            }
            plugin.logger.info("[Landmark] 已加载 ${playerUnlocks.size} 个玩家的解锁数据")
        } catch (e: Exception) {
            plugin.logger.severe("[Landmark] 加载解锁数据失败: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * 标记解锁数据需要保存
     */
    fun saveUnlocks() {
        unlocksDirty.set(true)
    }

    /**
     * 实际执行解锁数据保存
     */
    private fun doSaveUnlocks() {
        try {
            if (!plugin.dataFolder.exists()) {
                plugin.dataFolder.mkdirs()
            }
            val content = json.encodeToString(playerUnlocks.values.toList())
            unlocksFile.writeText(content)
        } catch (e: Exception) {
            plugin.logger.severe("[Landmark] 保存解锁数据失败: ${e.message}")
            e.printStackTrace()
        }
    }

    // ========== 地标操作 ==========

    fun getLandmark(id: String): Landmark? = landmarks[id]

    fun getLandmarkByName(name: String): Landmark? {
        return landmarks.values.find { it.name.equals(name, ignoreCase = true) }
    }

    fun getAllLandmarks(): Collection<Landmark> = landmarks.values.toList()

    fun getLandmarksByWorld(world: String): List<Landmark> {
        return worldIndex[world] ?: emptyList()
    }

    private fun rebuildWorldIndex() {
        worldIndex.clear()
        landmarks.values.forEach { landmark ->
            worldIndex.getOrPut(landmark.world) { mutableListOf() }.add(landmark)
        }
    }

    private fun addToWorldIndex(landmark: Landmark) {
        worldIndex.getOrPut(landmark.world) { mutableListOf() }.add(landmark)
    }

    private fun removeFromWorldIndex(landmark: Landmark) {
        worldIndex[landmark.world]?.remove(landmark)
    }

    fun addLandmark(landmark: Landmark): Boolean {
        if (landmarks.containsKey(landmark.id)) return false
        if (getLandmarkByName(landmark.name) != null) return false
        landmarks[landmark.id] = landmark
        addToWorldIndex(landmark)
        saveLandmarks()
        return true
    }

    fun updateLandmark(landmark: Landmark): Boolean {
        if (!landmarks.containsKey(landmark.id)) return false
        landmark.updatedAt = System.currentTimeMillis()
        landmarks[landmark.id] = landmark
        saveLandmarks()
        return true
    }

    fun removeLandmark(id: String): Boolean {
        val landmark = landmarks.remove(id)
        if (landmark != null) {
            removeFromWorldIndex(landmark)
            saveLandmarks()
            return true
        }
        return false
    }

    fun landmarkExists(name: String): Boolean {
        return getLandmarkByName(name) != null
    }

    // ========== 解锁操作 ==========

    fun getPlayerUnlocks(playerUuid: String): PlayerUnlocks {
        return playerUnlocks.getOrPut(playerUuid) {
            PlayerUnlocks(playerUuid)
        }
    }

    fun isUnlocked(playerUuid: String, landmarkId: String): Boolean {
        val landmark = getLandmark(landmarkId)
        if (landmark?.defaultUnlocked == true) return true
        return getPlayerUnlocks(playerUuid).unlockedLandmarks.contains(landmarkId)
    }

    fun unlockLandmark(playerUuid: String, landmarkId: String): Boolean {
        val unlocks = getPlayerUnlocks(playerUuid)
        if (unlocks.unlockedLandmarks.contains(landmarkId)) return false
        unlocks.unlockedLandmarks.add(landmarkId)
        unlocks.lastUnlockTime = System.currentTimeMillis()
        saveUnlocks()
        return true
    }

    /**
     * 锁定地标（撤销解锁）
     */
    fun lockLandmark(playerUuid: String, landmarkId: String): Boolean {
        val unlocks = getPlayerUnlocks(playerUuid)
        if (!unlocks.unlockedLandmarks.contains(landmarkId)) return false
        unlocks.unlockedLandmarks.remove(landmarkId)
        saveUnlocks()
        return true
    }

    /**
     * 解锁地标给所有已记录的玩家
     */
    fun unlockLandmarkForAll(landmarkId: String): Int {
        var count = 0
        playerUnlocks.values.forEach { unlocks ->
            if (!unlocks.unlockedLandmarks.contains(landmarkId)) {
                unlocks.unlockedLandmarks.add(landmarkId)
                count++
            }
        }
        if (count > 0) saveUnlocks()
        return count
    }

    /**
     * 锁定地标给所有玩家
     */
    fun lockLandmarkForAll(landmarkId: String): Int {
        var count = 0
        playerUnlocks.values.forEach { unlocks ->
            if (unlocks.unlockedLandmarks.remove(landmarkId)) {
                count++
            }
        }
        if (count > 0) saveUnlocks()
        return count
    }

    /**
     * 获取所有已记录的玩家 UUID
     */
    fun getAllPlayerUuids(): Set<String> {
        return playerUnlocks.keys.toSet()
    }

    fun getUnlockedLandmarks(playerUuid: String): Set<String> {
        val playerSet = getPlayerUnlocks(playerUuid).unlockedLandmarks.toMutableSet()
        // 添加默认解锁的地标
        landmarks.values.filter { it.defaultUnlocked }.forEach {
            playerSet.add(it.id)
        }
        return playerSet
    }
}
