package org.tsl.tSLplugins.modules.kiss

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.tsl.tSLplugins.SubCommandHandler
import org.tsl.tSLplugins.core.AbstractModule
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 亲吻功能模块
 * 
 * 允许玩家通过命令或 Shift+右键 亲吻其他玩家。
 * 
 * ## 功能
 * - 亲吻玩家（命令或交互）
 * - 爱心粒子效果
 * - 甜美音效
 * - 亲吻统计（发起/被亲次数）
 * - 个人开关
 * - 冷却时间
 * 
 * ## 命令
 * - `/tsl kiss <玩家>` - 亲吻指定玩家
 * - `/tsl kiss toggle` - 切换个人开关
 * 
 * ## 交互
 * - Shift + 右键玩家 = 亲吻
 * 
 * ## 权限
 * - `tsl.kiss.bypass` - 绕过冷却时间
 * 
 * ## 配置
 * ```yaml
 * kiss:
 *   enabled: true
 *   cooldown: 1.0
 * ```
 */
class KissModule : AbstractModule() {
    
    override val id = "kiss"
    override val configPath = "kiss"
    
    // 配置项
    private var cooldown: Long = 1000L
    
    // 冷却记录
    private val playerCooldowns: MutableMap<UUID, Long> = ConcurrentHashMap()
    
    private val serializer = LegacyComponentSerializer.legacyAmpersand()
    
    override fun loadConfig() {
        super.loadConfig()
        cooldown = (getConfigDouble("cooldown", 1.0) * 1000).toLong()
    }
    
    override fun doEnable() {
        // 注册监听器
        registerListener(KissModuleListener(this))
        logInfo("冷却时间: ${cooldown / 1000.0} 秒")
    }
    
    override fun doDisable() {
        playerCooldowns.clear()
    }
    
    override fun getCommandHandler(): SubCommandHandler {
        return KissModuleCommand(this)
    }
    
    override fun getDescription(): String = "亲吻玩家功能"
    
    // ==================== 玩家开关 ====================
    
    /**
     * 检查玩家是否启用了功能
     */
    fun isPlayerEnabled(player: Player): Boolean {
        return context.playerDataManager.getKissToggle(player, true)
    }
    
    /**
     * 切换玩家的功能开关
     * @return 切换后的状态
     */
    fun togglePlayer(player: Player): Boolean {
        val current = isPlayerEnabled(player)
        val newStatus = !current
        context.playerDataManager.setKissToggle(player, newStatus)
        return newStatus
    }
    
    // ==================== 冷却管理 ====================
    
    /**
     * 检查玩家是否在冷却中
     */
    fun isInCooldown(uuid: UUID): Boolean {
        val lastUsed = playerCooldowns[uuid] ?: return false
        return System.currentTimeMillis() - lastUsed < cooldown
    }
    
    /**
     * 设置玩家冷却
     */
    fun setCooldown(uuid: UUID) {
        playerCooldowns[uuid] = System.currentTimeMillis()
    }
    
    /**
     * 清理玩家数据
     */
    fun cleanupPlayer(uuid: UUID) {
        playerCooldowns.remove(uuid)
    }
    
    // ==================== 统计数据 ====================
    
    /**
     * 增加玩家亲吻次数
     */
    fun incrementKissCount(uuid: UUID) {
        val profile = context.playerDataManager.getProfileStore().getOrCreate(uuid, "Unknown")
        profile.kissCount++
    }
    
    /**
     * 增加玩家被亲吻次数
     */
    fun incrementKissedCount(uuid: UUID) {
        val profile = context.playerDataManager.getProfileStore().getOrCreate(uuid, "Unknown")
        profile.kissedCount++
    }
    
    // ==================== 执行亲吻 ====================
    
    /**
     * 执行亲吻动作
     */
    fun executeKiss(sender: Player, target: Player) {
        // 增加统计数据
        incrementKissCount(sender.uniqueId)
        incrementKissedCount(target.uniqueId)
        
        // 发送消息给发起者
        sender.sendMessage(serializer.deserialize(
            getMessage("kiss_sent", "player" to target.name)
        ))
        
        // 发送消息给目标
        target.sendMessage(serializer.deserialize(
            getMessage("kiss_received", "player" to sender.name)
        ))
        
        // 在目标玩家位置生成粒子效果和音效
        target.scheduler.run(context.plugin, { _ ->
            if (target.isValid && target.isOnline) {
                spawnHeartParticles(target.location)
                playKissSound(target)
            }
        }, null)
    }
    
    /**
     * 生成爱心粒子效果
     */
    private fun spawnHeartParticles(location: Location) {
        val headLocation = location.clone().add(0.0, 2.0, 0.0)
        
        location.world.spawnParticle(
            Particle.HEART,
            headLocation,
            20,              // 粒子数量
            0.5,             // X 偏移
            0.5,             // Y 偏移
            0.5,             // Z 偏移
            0.1              // 速度
        )
    }
    
    /**
     * 播放亲吻音效
     */
    private fun playKissSound(player: Player) {
        player.playSound(
            player.location,
            Sound.ENTITY_PLAYER_LEVELUP,
            1.0f,            // 音量
            1.5f             // 音调
        )
    }
    
    /**
     * 获取模块消息
     */
    fun getModuleMessage(key: String, vararg replacements: Pair<String, String>): String {
        return getMessage(key, *replacements)
    }
}
