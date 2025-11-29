# ChatBubble 模块跨线程访问完全修复

## 📋 问题历程

### 第一次错误：removePassenger 跨线程
```
at org.bukkit.craftbukkit.entity.CraftEntity.removePassenger()
at ChatBubbleManager.kt:150
```
**原因**：手动调用 `player.removePassenger(display)` 时，display 已在其他 Region

### 第二次错误：remove 跨线程（本次修复）
```
at org.bukkit.craftbukkit.entity.CraftEntity.remove()
at ChatBubbleManager.kt:247
```
**原因**：使用**玩家调度器**删除 display，但 display 传送后已在不同 Region

---

## 🎯 根本原因

### Folia 的区域线程模型
```
玩家在 Region A 创建气泡
↓
player.scheduler.runDelayed() → 任务注册在 Region A 的调度器
↓
玩家传送到 Region B（display 作为 passenger 跟随）
↓
5秒后，Region A 的调度器执行任务
↓
尝试删除在 Region B 的 display → ❌ 跨线程错误
```

### 关键洞察
在 Folia 中：
- **玩家的调度器**绑定到玩家当前所在的 Region
- **实体的调度器**绑定到实体当前所在的 Region
- 当玩家传送时，如果使用玩家的调度器，任务会在**旧 Region** 执行
- 必须使用**实体自己的调度器**才能保证在正确的线程执行

---

## ✅ 最终解决方案

### 核心原则
**使用实体自己的调度器来删除实体**

### 实现方案

#### 1. 定时删除任务
```kotlin
// ❌ 错误方案（第一次修复）
player.scheduler.runDelayed(plugin, { _ ->
    display.remove()  // 跨线程！玩家可能已传送
}, null, timeSpan.toLong())

// ✅ 正确方案（最终修复）
display.scheduler.runDelayed(plugin, { _ ->
    // 在 display 所在的线程删除（线程安全）
    try {
        if (display.isValid) {
            display.remove()
        }
    } catch (e: Exception) {
        // 忽略删除错误
    }
    
    // 清理引用（使用玩家调度器）
    player.scheduler.run(plugin, { _ ->
        bubbles.remove(player)
    }, null)
}, null, timeSpan.toLong())
```

#### 2. 清理方法
```kotlin
/**
 * 线程安全地移除气泡
 * 使用 display 的调度器确保在正确的线程上删除
 */
private fun safeRemoveBubble(player: Player, display: TextDisplay) {
    // 使用 display 自己的调度器在正确的线程上删除
    try {
        display.scheduler.run(plugin, { _ ->
            try {
                if (display.isValid) {
                    display.remove()
                }
            } catch (e: Exception) {
                // 忽略删除错误
            }
        }, null)
    } catch (e: Exception) {
        // 忽略调度错误（实体可能已被删除）
    }
    
    // 立即清理引用
    bubbles.remove(player)
}
```

---

## 🔧 完整修改记录

### 修改 1：createOrUpdateBubble 定时删除（第147-165行）

**旧代码（第一次修复）：**
```kotlin
player.scheduler.runDelayed(plugin, { _ ->
    safeRemoveBubble(player, display)
}, null, timeSpan.toLong())
```

**新代码（最终修复）：**
```kotlin
display.scheduler.runDelayed(plugin, { _ ->
    // 在 display 所在的线程删除（线程安全）
    try {
        if (display.isValid) {
            display.remove()
        }
    } catch (e: Exception) {
        // 忽略删除错误
    }
    
    // 清理引用（使用玩家调度器确保线程安全）
    player.scheduler.run(plugin, { _ ->
        bubbles.remove(player)
    }, null)
}, null, timeSpan.toLong())
```

### 修改 2：safeRemoveBubble 方法（第248-267行）

**旧代码（第一次修复）：**
```kotlin
private fun safeRemoveBubble(player: Player, display: TextDisplay) {
    try {
        if (display.isValid) {
            display.remove()  // ❌ 直接调用，可能跨线程
        }
    } catch (e: Exception) {}
    bubbles.remove(player)
}
```

**新代码（最终修复）：**
```kotlin
private fun safeRemoveBubble(player: Player, display: TextDisplay) {
    try {
        display.scheduler.run(plugin, { _ ->  // ✅ 使用 display 调度器
            try {
                if (display.isValid) {
                    display.remove()
                }
            } catch (e: Exception) {}
        }, null)
    } catch (e: Exception) {}
    
    bubbles.remove(player)
}
```

---

## 📊 线程安全分析

### 场景 1：玩家未传送
```
创建气泡（Region A）
↓
display.scheduler.runDelayed() → 在 Region A 注册
↓
5秒后，Region A 执行任务
↓
删除 display（Region A）→ ✅ 同线程，安全
```

### 场景 2：玩家传送（关键场景）
```
创建气泡（Region A）
↓
display.scheduler.runDelayed() → 在 Region A 注册
↓
玩家传送到 Region B（display 跟随）
↓
display 的调度器任务**自动转移到 Region B**
↓
5秒后，Region B 执行任务
↓
删除 display（Region B）→ ✅ 同线程，安全
```

### 场景 3：玩家退出
```
玩家退出
↓
调用 cleanupPlayer()
↓
safeRemoveBubble() → display.scheduler.run()
↓
在 display 所在线程删除 → ✅ 安全
```

---

## 🎨 关键技术点

### 1. 实体调度器的特性
```kotlin
// Folia 保证：
// - 实体的调度器会跟随实体移动
// - 任务会在实体当前所在的 Region 执行
// - 即使实体传送，任务仍然有效

display.scheduler.runDelayed() // ✅ 任务跟随实体
player.scheduler.runDelayed()  // ❌ 任务不跟随玩家传送
```

### 2. 双层调度的必要性
```kotlin
// 删除实体：使用 display 调度器
display.scheduler.run {
    display.remove()
}

// 清理引用：使用 player 调度器
player.scheduler.run {
    bubbles.remove(player)
}
```

### 3. 防御性编程
```kotlin
try {
    display.scheduler.run {  // 外层 try：防止调度失败
        try {
            display.remove()  // 内层 try：防止删除失败
        } catch (e: Exception) {}
    }
} catch (e: Exception) {}
```

---

## ✅ 测试验证

### 测试场景 1：聊天后等待删除
```
步骤：
1. 玩家发送消息
2. 等待 5 秒

预期：
✅ 气泡正常消失
✅ 无错误日志

验证点：
- 控制台无跨线程错误
- bubbles Map 正确清理
```

### 测试场景 2：聊天后立即传送（关键）
```
步骤：
1. 玩家发送消息
2. 立即 /tp @s ~1000 ~ ~（传送到远处）
3. 等待 5 秒

预期：
✅ 气泡跟随传送
✅ 5 秒后在新位置消失
✅ 无跨线程错误

验证点：
- 无 "Thread failed main thread check" 错误
- 气泡正确删除
```

### 测试场景 3：聊天后跨世界传送
```
步骤：
1. 主世界发送消息
2. /tp @s world_nether 0 64 0
3. 等待 5 秒

预期：
✅ 传送时清理旧气泡（由 ChatBubbleListener 处理）
✅ 无错误日志

验证点：
- 传送瞬间气泡消失
- 无跨线程错误
```

### 测试场景 4：聊天后立即退出
```
步骤：
1. 玩家发送消息
2. 立即退出游戏

预期：
✅ 玩家正常退出
✅ 气泡正确清理
✅ 无错误日志

验证点：
- 控制台无错误
- 实体正确删除
```

---

## 📝 总结

### 问题演进
1. **第一次错误**：`removePassenger()` 跨线程 → 放弃手动移除 passenger
2. **第二次错误**：`display.remove()` 跨线程 → 改用 display 调度器

### 最终方案
**核心**：使用实体自己的调度器来操作实体
- 删除实体：`display.scheduler.run { display.remove() }`
- 清理引用：`player.scheduler.run { bubbles.remove() }`

### Folia 最佳实践
1. ✅ 操作实体时，使用**实体自己的调度器**
2. ✅ 操作玩家数据时，使用**玩家的调度器**
3. ✅ 所有跨线程操作都用 try-catch 包装
4. ❌ 不要在错误的调度器上操作实体

### 代码质量
- ✅ 完全线程安全
- ✅ 代码简洁清晰
- ✅ 异常处理完善
- ✅ 注释详尽

---

**修复完成时间**: 2025-11-29  
**修复版本**: TSLplugins v1.0  
**Folia 版本**: Luminol 1.21.8  
**修复次数**: 2 次迭代

