package org.tsl.tSLplugins.modules.babylock

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.entity.Ageable
import org.bukkit.entity.EntityType
import org.tsl.tSLplugins.core.AbstractModule
import java.util.concurrent.ConcurrentHashMap

/**
 * BabyLock 模块 - 永久幼年生物系统
 * 
 * 通过命名前缀锁定幼年生物，使其永不长大
 * 
 * ## 功能
 * - 使用特定前缀命名的幼年生物将被锁定
 * - 区块加载时自动重新应用锁定
 * - 支持防止自然消失
 * 
 * ## 配置
 * - `babylock.enabled` - 是否启用
 * - `babylock.prefixes` - 锁定前缀列表
 * - `babylock.enabled_types` - 支持的实体类型
 * - `babylock.prevent_despawn` - 是否防止消失
 * - `babylock.case_sensitive` - 前缀是否区分大小写
 * 
 * ## 权限
 * 无特殊权限，任何可以使用命名牌的玩家都可使用
 */
class BabyLockModule : AbstractModule() {

    override val id = "babylock"
    override val configPath = "babylock"

    // 配置项
    private var preventDespawn: Boolean = true
    private var caseSensitive: Boolean = false
    private val prefixes: MutableList<String> = mutableListOf()
    private val enabledTypes: MutableSet<EntityType> = ConcurrentHashMap.newKeySet()

    // Listener 实例
    private lateinit var listener: BabyLockModuleListener

    override fun doEnable() {
        loadBabyLockConfig()
        listener = BabyLockModuleListener(this)
        registerListener(listener)
    }

    override fun doDisable() {
        // 无需清理
    }

    override fun doReload() {
        loadBabyLockConfig()
    }

    override fun getDescription(): String = "永久幼年生物系统"

    /**
     * 加载 BabyLock 配置
     */
    private fun loadBabyLockConfig() {
        preventDespawn = getConfigBoolean("prevent_despawn", true)
        caseSensitive = getConfigBoolean("case_sensitive", false)

        val prefixList = getConfigStringList("prefixes")
        prefixes.clear()
        if (prefixList.isEmpty()) {
            prefixes.add("[幼]")
        } else {
            prefixes.addAll(prefixList)
        }

        val typeStrings = getConfigStringList("enabled_types")
        enabledTypes.clear()
        typeStrings.forEach { typeName ->
            try {
                enabledTypes.add(EntityType.valueOf(typeName.uppercase()))
            } catch (e: IllegalArgumentException) {
                logWarning("无效的实体类型: $typeName")
            }
        }
    }

    // ============== 公开 API ==============

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
        // 必须是幼年 - 检查 age 属性
        val isBaby = entity.age < 0
        if (!isBaby) return false

        // 检查实体类型
        if (!isTypeEnabled(entity.type)) return false

        // 检查名字前缀
        val customName = entity.customName() ?: return false

        val plainName = PlainTextComponentSerializer.plainText().serialize(customName)
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
     * 是否防止消失
     */
    fun isPreventDespawn(): Boolean = preventDespawn

    /**
     * 获取模块消息
     */
    fun getModuleMessage(key: String, vararg replacements: Pair<String, String>): String {
        return getMessage(key, *replacements)
    }

    /**
     * 获取插件实例
     */
    fun getPlugin() = context.plugin
}
