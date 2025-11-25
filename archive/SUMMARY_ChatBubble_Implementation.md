# ChatBubble 聊天气泡功能实现总结

**日期**: 2025-11-26  
**功能类型**: 玩家互动 - 聊天增强

---

## 功能概述

ChatBubble 是一个聊天气泡功能，在玩家聊天时在头顶显示 TextDisplay 实体作为气泡，提供视觉化的聊天体验。本实现参考了 ChatDisplay 项目，并针对 Folia 和 TSLplugins 架构进行了优化。

---

## 核心特性

### 1. 基础功能
- ✅ **聊天气泡显示** - 玩家聊天时头顶显示气泡
- ✅ **自动消失** - 配置存在时间后自动消失
- ✅ **自我显示切换** - 玩家可选择是否看到自己的气泡
- ✅ **潜行半透明** - 潜行时气泡变为半透明
- ✅ **隐身自动隐藏** - 隐身或观察者模式自动隐藏气泡

### 2. 性能优化
- ✅ **Folia 调度器** - 使用实体调度器确保线程安全
- ✅ **配置缓存** - 启动/重载时读取，运行时零开销
- ✅ **及时清理** - 玩家退出/气泡过期时立即清理
- ✅ **ConcurrentHashMap** - 线程安全的数据结构
- ✅ **可配置更新频率** - 平衡性能和流畅度

### 3. 视觉效果
- ✅ **可配置朝向** - FIXED/VERTICAL/HORIZONTAL/CENTER
- ✅ **可配置背景色** - 支持 RGBA 自定义
- ✅ **可配置不透明度** - 正常/潜行状态独立配置
- ✅ **可配置可视范围** - 控制显示距离
- ✅ **平滑移动** - 配置移动过渡时间

---

## 文件结构

```
ChatBubble/
├── ChatBubbleManager.kt   # 管理器：配置、状态、气泡管理
├── ChatBubbleCommand.kt   # 命令处理器：self/status
└── ChatBubbleListener.kt  # 监听器：聊天、世界切换、退出
```

---

## 代码实现

### ChatBubbleManager.kt

**职责**：
- 加载和缓存配置
- 创建和更新气泡实体
- 管理玩家自我显示设置
- 清理过期气泡和数据

**核心方法**：
```kotlin
fun createOrUpdateBubble(player: Player, message: Component)  // 创建/更新气泡
fun toggleSelfDisplay(player: Player): Boolean                // 切换自我显示
fun cleanupPlayer(player: Player)                             // 清理玩家数据
fun cleanupAll()                                              // 清理所有气泡
```

**关键优化**：
```kotlin
// 1. 配置缓存
private var enabled: Boolean = true
private var yOffset: Double = 0.75
private var timeSpan: Int = 100
// ...所有配置项都缓存在内存中

// 2. 线程安全数据结构
private val bubbles: MutableMap<Player, TextDisplay> = ConcurrentHashMap()
private val selfDisplayEnabled: MutableSet<Player> = ConcurrentHashMap.newKeySet()

// 3. Folia 实体调度器
player.scheduler.runAtFixedRate(plugin, { task ->
    // 更新气泡位置和状态
}, null, 1L, updateTicks)
```

---

### ChatBubbleCommand.kt

**职责**：处理 `/tsl chatbubble` 命令

**命令格式**：
- `/tsl chatbubble` - 切换自我显示
- `/tsl chatbubble self` - 切换自我显示
- `/tsl chatbubble status` - 查看状态

**实现**：
```kotlin
class ChatBubbleCommand(
    private val manager: ChatBubbleManager
) : SubCommandHandler {
    
    override fun handle(...): Boolean {
        // 1. 功能检查
        if (!manager.isEnabled()) return true
        
        // 2. 玩家检查
        if (sender !is Player) return true
        
        // 3. 命令分发
        when (args[0]) {
            "self" -> handleSelfToggle(player)
            "status" -> handleStatus(player)
        }
    }
}
```

---

### ChatBubbleListener.kt

**职责**：监听游戏事件

**监听的事件**：
```kotlin
@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
fun onAsyncChat(event: AsyncChatEvent) {
    // 聊天时创建气泡
    player.scheduler.run(plugin, { _ ->
        manager.createOrUpdateBubble(player, message)
    }, null)
}

@EventHandler
fun onWorldChange(event: PlayerChangedWorldEvent) {
    // 世界切换后重新隐藏气泡（如果需要）
}

@EventHandler
fun onPlayerQuit(event: PlayerQuitEvent) {
    // 玩家退出时清理数据
    manager.cleanupPlayer(event.player)
}
```

**设计要点**：
- AsyncChatEvent 在异步线程触发，使用玩家调度器回到主线程
- 世界切换后需要重新设置可见性
- 玩家退出时及时清理避免内存泄漏

---

## 配置文件

### config.yml

```yaml
chatbubble:
  # 是否启用功能
  enabled: true
  
  # 气泡位置偏移（玩家头顶上方的距离）
  yOffset: 0.75
  
  # 气泡存在时间（单位：tick，20 tick = 1 秒）
  timeSpan: 100
  
  # 气泡朝向模式
  # 可选值: FIXED(固定), VERTICAL(垂直), HORIZONTAL(水平), CENTER(居中)
  billboard: "VERTICAL"
  
  # 是否显示阴影
  shadow: false
  
  # 可视范围（单位：方块）
  viewRange: 16.0
  
  # 更新频率（单位：tick，数值越小更新越频繁，性能消耗越大）
  updateTicks: 2
  
  # 位置移动过渡时间（单位：tick）
  movementTicks: 4
  
  # 不透明度配置（取值范围：0.0-1.0）
  opacity:
    default: 1.0      # 正常状态
    sneaking: 0.25    # 潜行状态
  
  # 背景颜色配置（RGBA，取值范围：0-255）
  # 如果 red 设置为 -1，则使用默认背景
  background:
    red: -1
    green: 0
    blue: 0
    alpha: 0
```

**配置说明**：
- `timeSpan: 100` = 5秒后消失
- `updateTicks: 2` = 每 0.1 秒更新一次（平衡性能和流畅度）
- `billboard: "VERTICAL"` = 垂直朝向玩家（推荐）
- `viewRange: 16.0` = 16方块内可见

---

## 使用说明

### 玩家命令

```bash
/tsl chatbubble         # 切换自我显示
/tsl chatbubble self    # 切换自我显示
/tsl chatbubble status  # 查看状态
```

### 管理员命令

```bash
/tsl reload  # 重载配置（包括 ChatBubble）
```

---

## 权限节点

| 权限 | 说明 | 默认 |
|------|------|------|
| 无需权限 | 所有玩家默认可用 | 所有人 |

---

## 技术细节

### Folia 调度器适配

**实体调度器**：
```kotlin
player.scheduler.runAtFixedRate(plugin, { task ->
    // 气泡更新逻辑
    if (!player.isValid || !display.isValid) {
        task.cancel()
        return@runAtFixedRate
    }
    
    // 更新位置、不透明度、可见性
}, null, 1L, updateTicks)
```

**AsyncChatEvent 处理**：
```kotlin
@EventHandler
fun onAsyncChat(event: AsyncChatEvent) {
    // 异步事件 -> 使用玩家调度器回到主线程
    player.scheduler.run(plugin, { _ ->
        manager.createOrUpdateBubble(player, message)
    }, null)
}
```

---

### 气泡位置计算

```kotlin
private fun calculateBubbleLocation(player: Player): Location {
    return player.location.add(
        0.0, 
        player.boundingBox.height + yOffset,  // 玩家高度 + 偏移
        0.0
    ).apply {
        yaw = 0f    // 重置旋转
        pitch = 0f
    }
}
```

**说明**：
- 使用 `boundingBox.height` 自动适应玩家体型
- 支持 Scale 功能的体型变化
- 配合 `billboard` 确保气泡朝向正确

---

### 可见性管理

**自我显示逻辑**：
```kotlin
// 创建气泡时
if (!selfDisplayEnabled.contains(player)) {
    player.hideEntity(plugin, display)
}

// 切换自我显示时
fun toggleSelfDisplay(player: Player): Boolean {
    val newState = if (selfDisplayEnabled.contains(player)) {
        selfDisplayEnabled.remove(player)
        bubbles[player]?.let { player.hideEntity(plugin, it) }
        false
    } else {
        selfDisplayEnabled.add(player)
        bubbles[player]?.let { player.showEntity(plugin, it) }
        true
    }
    return newState
}
```

**附近玩家可见性**：
```kotlin
player.location.getNearbyPlayers(viewRange.toDouble()).forEach { nearbyPlayer ->
    if (nearbyPlayer == player) return@forEach
    
    if (!nearbyPlayer.canSee(player)) {
        nearbyPlayer.hideEntity(plugin, display)
    } else {
        nearbyPlayer.showEntity(plugin, display)
    }
}
```

**说明**：
- 玩家可以选择是否看到自己的气泡
- 附近玩家根据 `canSee()` 动态显示/隐藏
- 世界切换后需要重新设置可见性

---

### 内存管理

**数据清理时机**：
1. **气泡过期** - 定时任务中检测 `ticksLived > timeSpan`
2. **玩家退出** - `PlayerQuitEvent` 触发清理
3. **插件卸载** - `onDisable()` 清理所有气泡

**清理代码**：
```kotlin
fun cleanupPlayer(player: Player) {
    // 移除并删除气泡实体
    bubbles.remove(player)?.remove()
    
    // 清理自我显示设置
    selfDisplayEnabled.remove(player)
}

fun cleanupAll() {
    bubbles.values.forEach { it.remove() }
    bubbles.clear()
    selfDisplayEnabled.clear()
}
```

---

## 集成到主插件

### TSLplugins.kt 修改

1. ✅ 导入 ChatBubble 相关类
2. ✅ 添加 `chatBubbleManager` 成员变量
3. ✅ 在 `onEnable()` 中初始化系统
4. ✅ 注册 ChatBubble 命令处理器
5. ✅ 在 `onDisable()` 中清理资源
6. ✅ 添加 `reloadChatBubbleManager()` 方法

### ReloadCommand.kt 修改

- ✅ 在重载命令中添加 `plugin.reloadChatBubbleManager()`

### ConfigUpdateManager.kt 修改

- ✅ 更新配置版本号到 12

---

## 性能优化总结

### 1. 配置缓存机制
```kotlin
// 启动时读取配置到内存
private var enabled: Boolean = true
private var yOffset: Double = 0.75
// ...

// 运行时直接读取缓存，零开销
fun isEnabled(): Boolean = enabled
```

### 2. 线程安全
```kotlin
// 使用 ConcurrentHashMap
private val bubbles: MutableMap<Player, TextDisplay> = ConcurrentHashMap()
private val selfDisplayEnabled: MutableSet<Player> = ConcurrentHashMap.newKeySet()
```

### 3. Folia 调度器
```kotlin
// 实体操作使用实体调度器
player.scheduler.runAtFixedRate(plugin, { task ->
    // ...
}, null, 1L, updateTicks)
```

### 4. 及时清理
```kotlin
// 气泡过期时立即清理
if (display.ticksLived > timeSpan) {
    task.cancel()
    display.remove()
    bubbles.remove(player)
}
```

### 5. 可配置更新频率
```yaml
# 平衡性能和流畅度
updateTicks: 2  # 默认每 0.1 秒更新一次
```

**性能对比**：
- `updateTicks: 1` - 最流畅，高性能消耗
- `updateTicks: 2` - 平衡（推荐）
- `updateTicks: 5` - 低性能消耗，略有卡顿

---

## 对比 ChatDisplay 的改进

| 特性 | ChatDisplay | TSLplugins ChatBubble | 改进说明 |
|------|------------|---------------------|---------|
| 调度器 | Paper API | Folia 实体调度器 | ✅ 多线程支持 |
| 配置管理 | 直接读取 | 配置缓存机制 | ✅ 性能优化 |
| 数据结构 | MutableMap | ConcurrentHashMap | ✅ 线程安全 |
| 命令系统 | Brigadier | SubCommandHandler | ✅ 统一架构 |
| 重载系统 | 独立 reload | 统一 /tsl reload | ✅ 管理便捷 |
| 配置版本 | 无 | 自动更新 | ✅ 用户友好 |

---

## 测试场景

### 场景 1：基本聊天气泡
```
1. 玩家发送聊天消息
2. 验证：
   ✅ 头顶出现气泡
   ✅ 显示聊天内容
   ✅ 5秒后自动消失
```

### 场景 2：自我显示切换
```
1. 玩家执行 /tsl chatbubble
2. 验证：
   ✅ 看不到自己的气泡
3. 再次执行命令
4. 验证：
   ✅ 可以看到自己的气泡
```

### 场景 3：潜行半透明
```
1. 玩家发送消息（有气泡）
2. 玩家潜行
3. 验证：
   ✅ 气泡变为半透明
4. 玩家站起
5. 验证：
   ✅ 气泡恢复正常
```

### 场景 4：隐身自动隐藏
```
1. 玩家发送消息
2. 玩家喝隐身药水/切换观察者模式
3. 验证：
   ✅ 气泡不会创建
4. 玩家恢复正常
5. 玩家发送消息
6. 验证：
   ✅ 气泡正常显示
```

### 场景 5：世界切换
```
1. 玩家在世界A发送消息（有气泡）
2. 玩家传送到世界B
3. 验证：
   ✅ 气泡正确清理
   ✅ 自我显示设置保留
```

### 场景 6：玩家退出
```
1. 玩家发送消息（有气泡）
2. 玩家退出服务器
3. 验证：
   ✅ 气泡实体被移除
   ✅ 内存数据被清理
```

### 场景 7：配置重载
```
1. 修改配置（如 timeSpan: 200）
2. 执行 /tsl reload
3. 玩家发送消息
4. 验证：
   ✅ 气泡存在 10 秒后消失
```

---

## 未来扩展

### 可能的功能增强

1. **更多视觉效果**
   - 支持更多 billboard 模式
   - 支持渐变背景色
   - 支持边框样式

2. **高级功能**
   - 表情符号支持
   - 颜色代码支持
   - 多行文本支持

3. **性能优化**
   - 距离分级更新（远处降低更新频率）
   - 气泡池复用机制
   - 批量更新优化

4. **个性化设置**
   - 玩家自定义气泡颜色
   - 玩家自定义气泡大小
   - 玩家自定义显示时长

---

## 常见问题

### Q: 为什么使用 TextDisplay 而不是 ArmorStand？

A: TextDisplay 是 1.19.4+ 新增的专用文本显示实体，相比 ArmorStand：
- ✅ 性能更好
- ✅ 功能更强大（billboard、背景色等）
- ✅ 更简洁（无需隐藏装备、名字等）

### Q: 气泡会影响服务器性能吗？

A: 经过优化后影响很小：
- 使用配置缓存，运行时零配置读取开销
- 使用 Folia 实体调度器，线程安全
- 气泡及时清理，无内存泄漏
- 可配置更新频率，平衡性能和流畅度

### Q: 气泡会阻挡玩家视线吗？

A: 可通过配置优化：
- 设置 `viewRange: 0.5` 减小可视范围
- 潜行时自动半透明
- 玩家可关闭自我显示

### Q: 支持多行文本吗？

A: 当前版本显示聊天消息的单行内容。多行支持可作为未来扩展。

---

**开发信息**：
- 参考项目：ChatDisplay by lukiiy
- 适配目标：Folia 1.21.8
- 配置版本：12
- 实现日期：2025-11-26
- 状态：✅ 完成并集成

