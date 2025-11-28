# Visitor（访客模式）功能逻辑详解

## 📋 功能概述

Visitor 模式是一个基于 LuckPerms 权限系统的动态玩家状态管理功能。当玩家拥有 `tsl.visitor` 权限时，会自动应用以下效果：

1. **视觉效果**：永久发光效果（GLOWING）
2. **保护机制**：怪物不会主动攻击访客玩家
3. **实时通知**：权限变更时的聊天消息、Title 和音效提示

---

## 🏗️ 核心架构

### 1. 数据结构

```kotlin
private var luckPerms: LuckPerms?              // LuckPerms API 实例
private val visitorPlayers: MutableSet<UUID>   // 当前是访客的玩家 UUID 集合
private var enabled: Boolean                   // 功能开关
```

**visitorPlayers 的作用**：
- 内存缓存，用于快速判断玩家是否处于访客状态
- 避免频繁调用权限检查 API
- 用于比对权限变更（检测"获得"或"失去"权限）

---

## 🔄 核心逻辑流程

### 初始化流程

```
插件启动
  ↓
加载配置 (loadConfig)
  ↓
设置 LuckPerms 集成 (setupLuckPerms)
  ↓
注册 LuckPerms 事件监听器 (UserDataRecalculateEvent)
```

**关键点**：
- 如果找不到 LuckPerms，仍可正常运行，但权限变更检测不可用
- 使用 `eventBus.subscribe()` 订阅权限变更事件

---

### 玩家加入流程（onPlayerJoin）

```
玩家登录
  ↓
延迟 1 秒（20 ticks）等待权限加载
  ↓
检查玩家是否在线
  ↓
获取当前权限状态：hasPermission = player.hasPermission("tsl.visitor")
获取历史状态：wasVisitor = visitorPlayers.contains(uuid)
  ↓
┌─────────────────────────────────────────────────┐
│  判断状态组合（4 种情况）                        │
├─────────────────────────────────────────────────┤
│  1. hasPermission=true, wasVisitor=false        │
│     → 获得权限（可能离线期间获得）               │
│     → 应用效果 + 发送通知                       │
├─────────────────────────────────────────────────┤
│  2. hasPermission=true, wasVisitor=true         │
│     → 仍有权限（重新登录）                      │
│     → 应用效果 + 静默恢复（不通知）             │
├─────────────────────────────────────────────────┤
│  3. hasPermission=false, wasVisitor=true        │
│     → 失去权限（可能离线期间失去）               │
│     → 移除效果 + 发送通知                       │
├─────────────────────────────────────────────────┤
│  4. hasPermission=false, wasVisitor=false       │
│     → 从未有过权限                               │
│     → 不做任何操作                               │
└─────────────────────────────────────────────────┘
```

**设计意图**：
- **延迟 1 秒**：确保 LuckPerms 权限数据完全加载
- **wasVisitor 检查**：区分"重新登录"和"首次获得权限"
- **静默恢复**：避免玩家重新登录时收到烦人的通知

---

### 权限变更流程（onPermissionChange）

```
管理员执行命令：/lp user <玩家> permission set tsl.visitor true
  ↓
LuckPerms 触发 UserDataRecalculateEvent
  ↓
onPermissionChange 方法被调用
  ↓
延迟 0.5 秒（10 ticks）等待权限完全更新
  ↓
检查玩家是否在线
  ↓
获取当前权限状态：hasPermission
获取历史状态：wasVisitor
  ↓
┌─────────────────────────────────────────────────┐
│  判断状态组合（4 种情况）                        │
├─────────────────────────────────────────────────┤
│  1. hasPermission=true, wasVisitor=false        │
│     → 实时获得权限                               │
│     → 应用效果 + 发送通知 + 日志记录             │
├─────────────────────────────────────────────────┤
│  2. hasPermission=false, wasVisitor=true        │
│     → 实时失去权限                               │
│     → 移除效果 + 发送通知 + 日志记录             │
├─────────────────────────────────────────────────┤
│  3. hasPermission=true, wasVisitor=true         │
│     → 已经是访客（无变化）                       │
│     → 不做任何操作                               │
├─────────────────────────────────────────────────┤
│  4. hasPermission=false, wasVisitor=false       │
│     → 从未是访客（无变化）                       │
│     → 不做任何操作                               │
└─────────────────────────────────────────────────┘
```

**关键点**：
- **延迟 0.5 秒**：确保 LuckPerms 内部权限更新完成
- **日志记录**：只在此处记录，方便管理员追踪权限变更

---

### 玩家退出流程（onPlayerQuit）

```
玩家下线
  ↓
从 visitorPlayers 集合中移除
```

**设计意图**：
- 清理内存缓存
- 下次登录时重新检查权限（防止离线期间权限变更）

---

### 怪物攻击拦截（onEntityTarget）

```
怪物尝试攻击玩家
  ↓
检查功能是否启用
  ↓
检查目标是否是玩家
  ↓
检查玩家是否有 tsl.visitor 权限
  ↓
如果有权限 → 取消攻击事件
```

**实现细节**：
- 使用 `event.isCancelled = true` 取消攻击
- 直接检查权限，不依赖 `visitorPlayers` 缓存（更可靠）

---

## 🎨 视觉效果

### 应用效果（applyVisitorEffect）

```kotlin
player.addPotionEffect(
    PotionEffect(
        PotionEffectType.GLOWING,      // 发光效果
        PotionEffect.INFINITE_DURATION, // 永久持续
        0,                              // 等级 0（无等级）
        false,                          // 不显示粒子
        false,                          // 不显示图标
        false                           // 不是环境效果
    )[需求.md](../%E9%9C%80%E6%B1%82.md)
)
visitorPlayers.add(player.uniqueId)
```

### 移除效果（removeVisitorEffect）

```kotlin
player.removePotionEffect(PotionEffectType.GLOWING)
visitorPlayers.remove(player.uniqueId)
```

---

## 💬 通知系统

### 获得权限通知（sendGainedMessage）

**包含 3 种通知方式**：

1. **聊天消息**：
   - 配置项：`visitor.gained.chat`
   - 默认：`&a[访客模式] &7你已进入访客模式，怪物将不会攻击你，并且你会发光！`

2. **Title 标题**：
   - 配置项：`visitor.gained.title` + `visitor.gained.subtitle`
   - 显示时间：淡入 0.5s，停留 3s，淡出 1s

3. **音效**：
   - 配置项：`visitor.gained.sound`
   - 默认：`entity.player.levelup`

### 失去权限通知（sendLostMessage）

**同样包含 3 种通知方式**：

1. **聊天消息**：
   - 配置项：`visitor.lost.chat`
   - 默认：`&c[访客模式] &7你已退出访客模式，怪物现在可以攻击你了！`

2. **Title 标题**：
   - 配置项：`visitor.lost.title` + `visitor.lost.subtitle`

3. **音效**：
   - 配置项：`visitor.lost.sound`
   - 默认：`block.note_block.bass`

**颜色代码处理**：
- 使用 `LegacyComponentSerializer.legacyAmpersand()` 解析 `&` 颜色代码
- 转换为 Adventure Component

---

## ⚙️ 配置项详解

```yaml
visitor:
  enabled: true                    # 功能总开关
  gained:                          # 获得权限时的通知
    chat: "..."                    # 聊天消息（支持 & 颜色代码）
    title: "..."                   # Title 主标题
    subtitle: "..."                # Title 副标题
    sound: "entity.player.levelup" # 音效名称（Minecraft 命名空间）
  lost:                            # 失去权限时的通知
    chat: "..."
    title: "..."
    subtitle: "..."
    sound: "block.note_block.bass"
```

---

## 🔧 技术细节

### Folia 兼容性

代码中使用 **实体调度器（Entity Scheduler）** 来确保 Folia 兼容：

```kotlin
player.scheduler.runDelayed(plugin, { _ ->
    // 延迟任务
}, null, 20L)
```

**为什么不用 Bukkit.getScheduler()？**
- Folia 是区域化多线程服务端
- 全局调度器不适用，必须使用实体/区域调度器
- `player.scheduler` 确保任务在玩家所在区域执行

### 音效播放安全机制

```kotlin
val key = NamespacedKey.minecraft(soundName.lowercase())
val sound = Registry.SOUNDS.get(key)
if (sound != null) {
    player.playSound(player.location, sound, 1.0f, 1.0f)
} else {
    plugin.logger.warning("无效的音效名称: $soundName")
}
```

**安全措施**：
- 使用 `Registry.SOUNDS` 验证音效是否存在
- 捕获异常避免服务器崩溃
- 记录警告日志方便调试

---

## 🐛 潜在问题与改进建议

### 1. visitorPlayers 的生命周期问题

**问题**：
- `onPlayerQuit` 会清空缓存
- 如果玩家离线期间权限变更，`wasVisitor` 会返回 `false`
- 导致 `onPlayerJoin` 可能误判

**当前处理**：
- 已通过状态组合判断正确处理
- 离线期间获得权限 → 发送通知 ✅
- 离线期间失去权限 → 发送通知 ✅

### 2. 权限检查的延迟时间

**当前设置**：
- `onPlayerJoin`：延迟 1 秒（20 ticks）
- `onPermissionChange`：延迟 0.5 秒（10 ticks）

**潜在问题**：
- 如果 LuckPerms 加载非常慢，可能仍不够
- 网络延迟大的服务器可能需要更长延迟

**建议**：
- 可以考虑增加重试机制
- 或者使用 LuckPerms 的异步权限查询 API

### 3. 怪物攻击拦截的性能

**当前实现**：
```kotlin
@EventHandler
fun onEntityTarget(event: EntityTargetEvent) {
    val player = event.target as? Player ?: return
    if (player.hasPermission("tsl.visitor")) {
        event.isCancelled = true
    }
}
```

**性能考虑**：
- 每次怪物选择目标都会触发
- 如果服务器怪物很多，可能频繁调用
- `hasPermission()` 调用相对轻量，应该问题不大

**优化建议**：
- 可以改为检查 `visitorPlayers.contains(player.uniqueId)`
- 但需要确保缓存完全同步

### 4. 多服务器同步问题

**问题**：
- 如果是 BungeeCord/Velocity 多服架构
- `visitorPlayers` 是内存缓存，不会跨服同步
- 玩家切换服务器时，状态可能不一致

**当前处理**：
- `onPlayerJoin` 会重新检查权限
- 因为 LuckPerms 是跨服同步的，可以正确识别

### 5. 效果持久化

**当前实现**：
- 发光效果设置为 `INFINITE_DURATION`
- 但玩家重新登录时会丢失（Minecraft 限制）

**当前处理**：
- `onPlayerJoin` 中会重新应用效果 ✅

---

## 📊 状态转换图

```
┌─────────────────┐
│   玩家下线      │
│ (visitorPlayers │
│   清空缓存)     │
└────────┬────────┘
         │
         ↓
┌─────────────────┐
│   玩家登录      │
│  (延迟检查)     │
└────────┬────────┘
         │
    ┌────┴────┐
    │ 检查权限 │
    └────┬────┘
         │
    ┌────┴──────────────────┐
    │                       │
    ↓                       ↓
┌────────┐            ┌──────────┐
│有权限   │            │无权限     │
└───┬────┘            └──┬───────┘
    │                    │
    ↓                    ↓
┌─────────┐          ┌─────────┐
│应用效果  │          │无操作   │
│发光+保护 │          │         │
└───┬─────┘          └─────────┘
    │
    │   管理员修改权限
    ↓   (LuckPerms 事件)
┌─────────────┐
│onPermission │
│  Change     │
└───┬─────────┘
    │
┌───┴────┐
│实时检测 │
└───┬────┘
    │
┌───┴──────┐
│应用/移除  │
│效果+通知  │
└──────────┘
```

---

## 🎯 使用场景

### 典型应用场景

1. **新玩家保护**：
   - 新玩家加入时自动分配 `tsl.visitor` 权限
   - 获得发光效果便于识别
   - 怪物不会攻击，可以安全探索

2. **VIP 访客**：
   - 给特定玩家临时访客权限
   - 可以参观服务器但不影响游戏

3. **活动模式**：
   - 活动期间给所有参与者访客权限
   - 避免被怪物干扰

### 权限管理命令示例

```bash
# 给玩家添加访客权限
/lp user <玩家名> permission set tsl.visitor true

# 移除访客权限
/lp user <玩家名> permission unset tsl.visitor

# 给权限组添加权限（所有该组玩家都会是访客）
/lp group visitor permission set tsl.visitor true
```

---

## 🔍 调试建议

### 查看日志

当权限变更时，会输出日志：
```
[TSLplugins] 玩家 PlayerName 获得了访客权限
[TSLplugins] 玩家 PlayerName 失去了访客权限
```

### 检查清单

1. **功能未生效**：
   - 检查 `config.yml` 中 `visitor.enabled` 是否为 `true`
   - 检查 LuckPerms 是否正确安装
   - 检查玩家是否有 `tsl.visitor` 权限

2. **通知未显示**：
   - 检查配置文件中的消息是否为空
   - 检查音效名称是否正确

3. **怪物仍攻击访客**：
   - 检查 `onEntityTarget` 事件是否正确注册
   - 使用 `/lp user <玩家> permission check tsl.visitor` 确认权限

---

## 💡 可能的调整方向

根据你的需求，可能需要调整：

1. **延迟时间调整**：修改 20L/10L 的延迟
2. **效果类型调整**：不止发光，可以添加其他药水效果
3. **通知方式调整**：可以添加 ActionBar、BossBar 等
4. **权限检查优化**：缓存机制优化或改用数据库
5. **多效果支持**：根据不同权限应用不同效果
6. **冷却机制**：防止频繁切换权限导致刷屏

---

**告诉我你想做什么调整，我会帮你实现！** 🚀

