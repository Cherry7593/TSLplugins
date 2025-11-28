# 🔴 Visitor 模块权限变更失效问题分析

## 问题描述

**现象**：
- 玩家在线时，通过 Permission 模块修改权限组后，Visitor 效果（发光、怪物保护）不能实时更新
- 只有玩家重新登录才能正确应用/移除效果

**触发场景**：
```
玩家登录（默认权限组，有 tsl.visitor 权限）
  ↓
应用访客效果（发光 + 怪物保护）✅
  ↓
白名单系统检测到玩家在白名单
  ↓
Permission 模块执行：移除所有权限组 → 设置为 normal 组（无 tsl.visitor 权限）
  ↓
【问题】Visitor 模块没有响应，玩家仍保持发光效果 ❌
```

---

## 🔍 根本原因分析

### 问题 1：Permission 模块修改权限的方式不触发 LuckPerms 事件

**PermissionChecker.kt 的 setGroup 方法**：
```kotlin
private fun setGroup(user: User, groupName: String, lp: LuckPerms) {
    // 移除所有现有的权限组节点
    val groupNodes = user.nodes.stream()
        .filter { it.key.startsWith("group.") }
        .toList()

    for (node in groupNodes) {
        user.data().remove(node)  // ⚠️ 直接操作 User 对象
    }

    // 添加新权限组
    val newGroupNode = Node.builder("group.$groupName").build()
    user.data().add(newGroupNode)  // ⚠️ 直接操作 User 对象

    // 保存
    lp.userManager.saveUser(user)  // ⚠️ 保存到存储，但不触发事件
}
```

**关键问题**：
1. **直接操作 `user.data()`**：这是底层 API，不会触发 LuckPerms 的事件总线
2. **`saveUser()` 只是持久化**：将数据保存到数据库/文件，不会触发 `UserDataRecalculateEvent`
3. **权限更新不同步**：服务器内存中的权限缓存没有刷新

### 问题 2：Visitor 模块依赖 UserDataRecalculateEvent

**VisitorEffect.kt 的事件订阅**：
```kotlin
private fun setupLuckPerms() {
    // ...
    luckPerms?.eventBus?.subscribe(
        plugin, 
        UserDataRecalculateEvent::class.java, 
        ::onPermissionChange  // ⚠️ 只监听这个事件
    )
}
```

**事件触发条件**：
- `UserDataRecalculateEvent` 只在以下情况触发：
  1. 使用 LuckPerms 命令修改权限（如 `/lp user ... permission set`）
  2. 通过 LuckPerms Web Editor 修改
  3. 调用 `lp.userManager.loadUser()` 重新加载用户数据

**Permission 模块的操作不会触发此事件！**

---

## 🎯 解决方案

### 方案 1：修改 Permission 模块，手动触发权限重算（推荐）⭐

在 `setGroup()` 方法保存后，手动刷新玩家权限：

```kotlin
private fun setGroup(user: User, groupName: String, lp: LuckPerms) {
    // ...existing code...
    
    // 保存用户数据
    lp.userManager.saveUser(user)
    
    // 【关键修复】刷新玩家的权限缓存，触发权限重算
    lp.userManager.loadUser(user.uniqueId).thenAccept { updatedUser ->
        if (updatedUser != null) {
            // 这会触发 UserDataRecalculateEvent
            plugin.logger.info("已刷新玩家 ${user.username ?: "Unknown"} 的权限缓存")
        }
    }
    
    plugin.logger.info("已将玩家 ${user.username ?: "Unknown"} 的权限组设置为 '$groupName'。")
}
```

**优点**：
- ✅ 符合 LuckPerms 设计模式
- ✅ 会触发所有订阅的事件监听器
- ✅ 不需要修改 Visitor 模块
- ✅ 其他依赖权限变更的模块也能受益

**缺点**：
- ⚠️ 涉及异步操作，需要处理回调

---

### 方案 2：在 Permission 模块中直接调用 Visitor 模块

在 `setGroup()` 后，直接通知 Visitor 模块：

```kotlin
// PermissionChecker.kt
private var visitorEffect: VisitorEffect? = null

fun setVisitorEffect(effect: VisitorEffect) {
    this.visitorEffect = effect
}

private fun setGroup(user: User, groupName: String, lp: LuckPerms) {
    // ...existing code...
    lp.userManager.saveUser(user)
    
    // 直接通知 Visitor 模块检查权限
    val player = Bukkit.getPlayer(user.uniqueId)
    if (player != null && player.isOnline) {
        visitorEffect?.checkAndUpdatePlayerStatus(player)  // 需要在 Visitor 中添加此方法
    }
}
```

**优点**：
- ✅ 简单直接
- ✅ 不依赖 LuckPerms 事件机制

**缺点**：
- ❌ 模块间耦合度高
- ❌ 如果还有其他模块依赖权限变更，需要逐个通知
- ❌ 违反模块化设计原则

---

### 方案 3：Visitor 模块增加周期性检查（不推荐）

定时检查所有在线玩家的权限：

```kotlin
// VisitorEffect.kt
init {
    // 每 5 秒检查一次所有在线玩家
    Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, { _ ->
        for (player in Bukkit.getOnlinePlayers()) {
            checkAndUpdatePlayerStatus(player)
        }
    }, 100L, 100L)
}
```

**优点**：
- ✅ 完全独立，不依赖其他模块

**缺点**：
- ❌ 浪费性能
- ❌ 延迟响应（最多 5 秒）
- ❌ 不优雅

---

### 方案 4：使用 Bukkit 原生权限事件（无效）❌

尝试监听 `PlayerPermissionChangedEvent`：

**问题**：
- Bukkit/Spigot 没有这个事件
- 权限变更是插件内部行为，不会触发任何 Bukkit 事件

---

## ✅ 推荐实现：方案 1（手动触发权限重算）

### 修改步骤

#### 1. 修改 PermissionChecker.kt

```kotlin
private fun setGroup(user: User, groupName: String, lp: LuckPerms) {
    // 移除所有现有的权限组节点
    val groupNodes = user.nodes.stream()
        .filter { it.key.startsWith("group.") }
        .toList()

    for (node in groupNodes) {
        user.data().remove(node)
    }

    plugin.logger.info("已清除玩家 ${user.username ?: "Unknown"} 的所有权限组。")

    // 创建新的权限组节点
    val newGroupNode = Node.builder("group.$groupName").build()
    user.data().add(newGroupNode)

    // 保存用户数据
    lp.userManager.saveUser(user)

    plugin.logger.info("已将玩家 ${user.username ?: "Unknown"} 的权限组设置为 '$groupName'。")

    // 【关键修复】刷新玩家权限缓存，触发 UserDataRecalculateEvent
    val player = Bukkit.getPlayer(user.uniqueId)
    if (player != null && player.isOnline) {
        // 延迟一点点，确保 saveUser 完成
        player.scheduler.runDelayed(plugin, { _ ->
            if (player.isOnline) {
                // 重新加载用户数据会触发 UserDataRecalculateEvent
                lp.userManager.loadUser(user.uniqueId)
                plugin.logger.info("已触发玩家 ${player.name} 的权限重算事件")
            }
        }, null, 5L) // 延迟 0.25 秒
    }
}
```

#### 2. 验证 Visitor 模块（无需修改）

确保 `onPermissionChange` 逻辑正确：

```kotlin
private fun onPermissionChange(event: UserDataRecalculateEvent) {
    val uuid = event.user.uniqueId
    val player = Bukkit.getPlayer(uuid) ?: return

    if (!player.isOnline) return

    // 延迟检查
    player.scheduler.runDelayed(plugin, { _ ->
        if (!player.isOnline) return@runDelayed

        val hasPermission = player.hasPermission("tsl.visitor")
        val wasVisitor = visitorPlayers.contains(uuid)

        when {
            hasPermission && !wasVisitor -> {
                applyVisitorEffect(player)
                sendGainedMessage(player)
                plugin.logger.info("玩家 ${player.name} 获得了访客权限")
            }
            !hasPermission && wasVisitor -> {
                removeVisitorEffect(player)
                sendLostMessage(player)
                plugin.logger.info("玩家 ${player.name} 失去了访客权限")
            }
        }
    }, null, 10L)
}
```

---

## 🔬 深入分析：为什么 loadUser 会触发事件

### LuckPerms 内部机制

```
lp.userManager.loadUser(uuid)
  ↓
从存储（数据库/文件）重新加载用户数据
  ↓
更新内存中的 User 对象
  ↓
重新计算玩家的有效权限（考虑继承、元数据等）
  ↓
【触发】UserDataRecalculateEvent
  ↓
所有订阅此事件的监听器被调用
  ↓
Visitor 模块的 onPermissionChange 被触发 ✅
```

### 关键 API 对比

| API | 作用 | 是否触发事件 |
|-----|------|-------------|
| `user.data().add(node)` | 直接修改内存中的节点 | ❌ 否 |
| `user.data().remove(node)` | 直接删除内存中的节点 | ❌ 否 |
| `lp.userManager.saveUser(user)` | 保存到存储 | ❌ 否 |
| `lp.userManager.loadUser(uuid)` | 重新加载并重算 | ✅ 是 |

---

## 📊 完整流程对比

### 修复前（不工作）

```
Permission 模块
  ↓
user.data().remove(旧组)
user.data().add(新组)
lp.userManager.saveUser(user)
  ↓
【无事件触发】❌
  ↓
Visitor 模块不知道权限已变更
  ↓
玩家仍保持旧状态（发光效果不变）
```

### 修复后（工作）

```
Permission 模块
  ↓
user.data().remove(旧组)
user.data().add(新组)
lp.userManager.saveUser(user)
  ↓
lp.userManager.loadUser(uuid)  ⭐ 新增
  ↓
【触发】UserDataRecalculateEvent ✅
  ↓
Visitor 模块的 onPermissionChange 被调用
  ↓
检测到权限变更（hasPermission 变化）
  ↓
移除/应用 访客效果 ✅
```

---

## 🧪 测试清单

修复后需要测试：

1. **玩家加入白名单**：
   - [ ] Permission 模块修改权限组
   - [ ] Visitor 效果立即移除
   - [ ] 收到"失去访客权限"通知
   - [ ] 怪物可以攻击玩家

2. **玩家移出白名单**：
   - [ ] Permission 模块恢复默认权限组
   - [ ] Visitor 效果立即应用
   - [ ] 收到"获得访客权限"通知
   - [ ] 怪物不攻击玩家

3. **日志输出**：
   - [ ] Permission 模块日志：`已触发玩家 XXX 的权限重算事件`
   - [ ] Visitor 模块日志：`玩家 XXX 失去了访客权限`

4. **边界情况**：
   - [ ] 玩家离线时修改权限（下次登录生效）
   - [ ] 快速切换权限（不会重复通知）

---

## 🎓 经验总结

### 关键教训

1. **理解 API 层级**：
   - 底层 API（`user.data()`）：性能高，但不触发事件
   - 高层 API（`loadUser()`）：会触发完整的更新流程

2. **模块间通信**：
   - 尽量使用事件/消息系统解耦
   - 避免直接调用其他模块的方法

3. **异步操作注意**：
   - LuckPerms 的 `loadUser()` 是异步的（返回 CompletableFuture）
   - 但触发事件是同步的，无需等待返回值

4. **调试技巧**：
   - 添加详细日志追踪权限变更
   - 使用 `/lp user <玩家> permission check tsl.visitor` 实时检查

---

## 🚀 立即行动

我将为你实现方案 1（推荐方案）！

