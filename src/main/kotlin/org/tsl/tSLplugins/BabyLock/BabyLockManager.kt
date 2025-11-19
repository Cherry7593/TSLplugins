package org.tsl.tSLplugins.BabyLock

import org.bukkit.entity.Ageable
import org.bukkit.entity.EntityType
import org.bukkit.plugin.java.JavaPlugin

/**
 * BabyLock 功能管理器
 * 负责管理永久幼年生物系统的配置和逻辑
 */
class BabyLockManager(private val plugin: JavaPlugin) {

    private var enabled: Boolean = true
    private var preventDespawn: Boolean = true
    private var caseSensitive: Boolean = false
    private val prefixes: MutableList<String> = mutableListOf()
    private val enabledTypes: MutableSet<EntityType> = mutableSetOf()
    private val messages: MutableMap<String, String> = mutableMapOf()

    init {
        loadConfig()
    }

    /**
     * 加载配置
     */
    fun loadConfig() {
        val config = plugin.config

        // 读取是否启用
        enabled = config.getBoolean("babylock.enabled", true)

        // 读取防止消失配置
        preventDespawn = config.getBoolean("babylock.prevent_despawn", true)

        // 读取前缀列表
        val prefixList = config.getStringList("babylock.prefixes")
        prefixes.clear()
        if (prefixList.isEmpty()) {
            prefixes.add("[幼]")  // 默认前缀
        } else {
            prefixes.addAll(prefixList)
        }

        // 读取是否区分大小写
        caseSensitive = config.getBoolean("babylock.case_sensitive", false)

        // 读取启用的实体类型（白名单）
        val typeStrings = config.getStringList("babylock.enabled_types")
        enabledTypes.clear()
        if (typeStrings.isNotEmpty()) {
            typeStrings.forEach { typeName ->
                try {
                    val entityType = EntityType.valueOf(typeName.uppercase())
                    enabledTypes.add(entityType)
                } catch (e: IllegalArgumentException) {
                    plugin.logger.warning("[BabyLock] 无效的实体类型: $typeName")
                }
            }
        }
        // 空列表代表全部 Ageable 生物均启用

        // 读取消息配置
        val prefix = config.getString("babylock.messages.prefix", "&6[TSL喵]&r ")
        messages.clear()
        val messagesSection = config.getConfigurationSection("babylock.messages")
        if (messagesSection != null) {
            for (key in messagesSection.getKeys(false)) {
                if (key == "prefix") continue
                val rawMessage = messagesSection.getString(key) ?: ""
                val processedMessage = rawMessage.replace("%prefix%", prefix ?: "")
                messages[key] = processedMessage
            }
        }

        val typeInfo = if (enabledTypes.isEmpty()) "全部 Ageable 生物" else "${enabledTypes.size} 种生物"
        plugin.logger.info("[BabyLock] 已加载配置 - 前缀数: ${prefixes.size}, 实体类型: $typeInfo")
    }

    /**
     * 检查功能是否启用
     */
    fun isEnabled(): Boolean = enabled

    /**
     * 检查实体类型是否启用
     */
    fun isTypeEnabled(entityType: EntityType): Boolean {
        // 空列表代表全部启用
        if (enabledTypes.isEmpty()) return true
        return enabledTypes.contains(entityType)
    }

    /**
     * 检查名字是否包含锁定前缀
     */
    fun hasLockPrefix(name: String): Boolean {
        if (name.isEmpty()) return false

        return prefixes.any { prefix ->
            if (caseSensitive) {
                name.startsWith(prefix)
            } else {
                name.startsWith(prefix, ignoreCase = true)
            }
        }
    }

    /**
     * 检查实体是否应该被锁定
     */
    fun shouldLock(entity: Ageable): Boolean {
        // 必须是幼年
        if (entity.isAdult) return false

        // 检查实体类型
        if (!isTypeEnabled(entity.type)) return false

        // 检查名字前缀
        val customName = entity.customName()
        if (customName == null) return false

        val plainName = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
            .serialize(customName)

        return hasLockPrefix(plainName)
    }

    /**
     * 锁定生物为永久幼年
     */
    fun lockBaby(entity: Ageable) {
        entity.ageLock = true

        if (preventDespawn) {
            entity.isPersistent = true
        }
    }

    /**
     * 解锁生物，允许继续成长
     */
    fun unlockBaby(entity: Ageable) {
        entity.ageLock = false
    }

    /**
     * 获取消息文本
     */
    fun getMessage(key: String, vararg replacements: Pair<String, String>): String {
        var message = messages[key] ?: key
        for ((placeholder, value) in replacements) {
            message = message.replace("{$placeholder}", value)
        }
        return message
    }

    /**
     * 是否防止消失
     */
    fun isPreventDespawn(): Boolean = preventDespawn
}

