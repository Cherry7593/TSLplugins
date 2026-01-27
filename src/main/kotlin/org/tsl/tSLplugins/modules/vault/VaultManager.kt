package org.tsl.tSLplugins.modules.vault

import org.bukkit.NamespacedKey
import org.bukkit.plugin.java.JavaPlugin
import java.util.concurrent.ConcurrentHashMap

class VaultManager(private val plugin: JavaPlugin) {
    private var enabled = true
    private var currentEpoch = 1
    private var resetIntervalDays = 15
    private var lastResetTimestamp = 0L
    private var debugMode = false
    private var abnormalThreshold = 100_000L

    val epochKey = NamespacedKey(plugin, "vault_sync_epoch")
    private val processingLocks = ConcurrentHashMap<String, Long>()
    private val lockTimeoutMs = 1000L

    init { loadConfig() }

    fun loadConfig() {
        val config = plugin.config
        enabled = config.getBoolean("vault.enabled", true)
        currentEpoch = config.getInt("vault.current-epoch", 1)
        resetIntervalDays = config.getInt("vault.reset-interval-days", 15)
        lastResetTimestamp = config.getLong("vault.last-reset-timestamp", 0L)
        debugMode = config.getBoolean("vault.debug", false)
        abnormalThreshold = config.getLong("vault.abnormal-threshold", 100_000L)
        if (lastResetTimestamp == 0L) { lastResetTimestamp = System.currentTimeMillis(); saveEpochConfig() }
        if (enabled) plugin.logger.info("[Vault] 已启用 - 周期: $currentEpoch, 间隔: ${resetIntervalDays}天")
    }

    fun checkAndUpdateEpoch(): Boolean {
        if (!enabled) return false
        val intervalMs = resetIntervalDays.toLong() * 24 * 60 * 60 * 1000
        if (System.currentTimeMillis() >= lastResetTimestamp + intervalMs) {
            currentEpoch++; lastResetTimestamp = System.currentTimeMillis(); saveEpochConfig()
            plugin.logger.info("[Vault] 周期更新: $currentEpoch"); return true
        }
        return false
    }

    private fun saveEpochConfig() {
        plugin.config.set("vault.current-epoch", currentEpoch)
        plugin.config.set("vault.last-reset-timestamp", lastResetTimestamp)
        plugin.saveConfig()
    }

    fun tryAcquireLock(blockKey: String): Boolean {
        val now = System.currentTimeMillis()
        var acquired = false
        processingLocks.compute(blockKey) { _, existing ->
            if (existing == null || now - existing >= lockTimeoutMs) { acquired = true; now } else existing
        }
        return acquired
    }

    fun releaseLock(blockKey: String) { processingLocks.remove(blockKey) }
    fun isEnabled() = enabled
    fun getCurrentEpoch() = currentEpoch
    fun isDebugMode() = debugMode
    fun getAbnormalThreshold() = abnormalThreshold
    fun debug(message: String) { if (debugMode) plugin.logger.info("[Vault/Debug] $message") }
    fun reload() { loadConfig(); plugin.logger.info("[Vault] 配置已重载") }
}
