package org.tsl.tSLplugins.BabyLock

import org.bukkit.entity.Ageable
import org.bukkit.entity.EntityType
import org.bukkit.plugin.java.JavaPlugin
import org.tsl.tSLplugins.TSLplugins

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

    private val msg get() = (plugin as TSLplugins).messageManager

    init {
        loadConfig()
    }

    /**
     * 加载配置
     */
    fun loadConfig() {
        val config = plugin.config
        enabled = config.getBoolean("babylock.enabled", true)
        preventDespawn = config.getBoolean("babylock.prevent_despawn", true)
        caseSensitive = config.getBoolean("babylock.case_sensitive", false)

        val prefixList = config.getStringList("babylock.prefixes")
        prefixes.clear()
        if (prefixList.isEmpty()) {
            prefixes.add("[幼]")
        } else {
            prefixes.addAll(prefixList)
        }

        val typeStrings = config.getStringList("babylock.enabled_types")
        enabledTypes.clear()
        typeStrings.forEach { typeName ->
            try {
                enabledTypes.add(EntityType.valueOf(typeName.uppercase()))
            } catch (e: IllegalArgumentException) {
                plugin.logger.warning("[BabyLock] 无效的实体类型: $typeName")
            }
        }
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
        // 必须是幼年 - 检查 age 属性而不是 isAdult
        // 在 Minecraft 中，幼年生物的 age < 0
        // 成年生物的 age >= 0
        val isBaby = entity.age < 0

        if (!isBaby) return false

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
        return msg.getModule("babylock", key, *replacements)
    }

    /**
     * 是否防止消失
     */
    fun isPreventDespawn(): Boolean = preventDespawn
}

