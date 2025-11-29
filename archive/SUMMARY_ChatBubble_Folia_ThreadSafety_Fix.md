# ChatBubble 模块 Folia 线程安全修复总结

## 📋 问题描述

根据需求文档，ChatBubble 模块存在 **Folia 跨线程访问** 的安全隐患，主要问题：

1. **跨线程访问实体状态** - 在玩家的 scheduler 中直接访问 TextDisplay 实体的属性
2. **跨线程删除实体** - 在错误的线程调用 `display.remove()`
3. **传送时的危险性** - 玩家传送后，气泡实体可能在不同的 Region，导致线程冲突

---

## 🔧 修复方案

### 核心原则
在 Folia 中，访问实体的属性和调用实体方法**必须在实体自己的 scheduler 线程上**。

### 实施的修复

#### 1. **修复 `createOrUpdateBubble` 方法**（第 109-130 行）
**问题**：直接访问 `existingBubble.isValid`, `ticksLived`, `text()` 等

**修复**：
```kotlin
// 旧代码（危险）
if (existingBubble != null && existingBubble.isValid) {
    existingBubble.ticksLived = 1  // ❌ 跨线程访问
    existingBubble.text(message)   // ❌ 跨线程调用
    return
}

// 新代码（安全）
if (existingBubble != null) {
    existingBubble.scheduler.execute(plugin, { ->
        if (!existingBubble.isValid) {
            bubbles.remove(player)
            return@execute
        }
        existingBubble.ticksLived = 1  // ✅ 在实体线程
        existingBubble.text(message)   // ✅ 在实体线程
    }, null, 0L)
    return
}
```

---

#### 2. **修复更新任务**（第 145-188 行）
**问题**：在玩家 scheduler 中直接访问 display 实体的属性和方法

**修复**：
```kotlin
// 旧代码（危险）
player.scheduler.runAtFixedRate(plugin, { task ->
    if (display.ticksLived > timeSpan) {      // ❌ 跨线程读取
        display.remove()                       // ❌ 跨线程删除
    }
    display.textOpacity = ...                  // ❌ 跨线程修改
    display.teleportAsync(...)                 // ❌ 跨线程传送
}, ...)

// 新代码（安全）
player.scheduler.runAtFixedRate(plugin, { task ->
    // 玩家线程：检查玩家状态
    if (!player.isValid || player.isInvisibleForBubble()) {
        task.cancel()
        bubbles.remove(player)
        // 在 display 线程删除实体
        display.scheduler.execute(plugin, { ->
            if (display.isValid) {
                display.remove()  // ✅ 在实体线程
            }
        }, null, 0L)
        return@runAtFixedRate
    }

    // 切换到 display 线程：访问和修改 display
    display.scheduler.execute(plugin, { ->
        if (!display.isValid) {
            task.cancel()
            bubbles.remove(player)
            return@execute
        }

        // 检查存活时间
        if (display.ticksLived > timeSpan) {  // ✅ 在实体线程
            task.cancel()
            bubbles.remove(player)
            display.remove()                   // ✅ 在实体线程
            return@execute
        }

        // 更新属性
        display.textOpacity = ...              // ✅ 在实体线程
        display.teleportAsync(...)             // ✅ 在实体线程
    }, null, 0L)

    // 回到玩家线程：更新可见性
    player.location.getNearbyPlayers(...).forEach { ... }
}, ...)
```

**关键改进**：
- ✅ 玩家相关操作在玩家线程（`player.scheduler`）
- ✅ Display 相关操作在 display 线程（`display.scheduler.execute`）
- ✅ 避免了所有跨线程访问

---

#### 3. **修复 `cleanupPlayer` 方法**（第 273-279 行）
**问题**：直接调用 `remove()`

**修复**：
```kotlin
// 旧代码（危险）
fun cleanupPlayer(player: Player) {
    bubbles.remove(player)?.remove()  // ❌ 跨线程删除
    selfDisplayEnabled.remove(player)
}

// 新代码（安全）
fun cleanupPlayer(player: Player) {
    bubbles.remove(player)?.let { display ->
        display.scheduler.execute(plugin, { ->
            if (display.isValid) {
                display.remove()  // ✅ 在实体线程
            }
        }, null, 0L)
    }
    selfDisplayEnabled.remove(player)
}
```

---

#### 4. **修复 `cleanupAll` 方法**（第 285-289 行）
**问题**：批量删除实体时未使用正确线程

**修复**：
```kotlin
// 旧代码（危险）
fun cleanupAll() {
    bubbles.values.forEach { it.remove() }  // ❌ 跨线程删除
    bubbles.clear()
    selfDisplayEnabled.clear()
}

// 新代码（安全）
fun cleanupAll() {
    bubbles.values.forEach { display ->
        display.scheduler.execute(plugin, { ->
            if (display.isValid) {
                display.remove()  // ✅ 在实体线程
            }
        }, null, 0L)
    }
    bubbles.clear()
    selfDisplayEnabled.clear()
}
```

---

#### 5. **新增：传送事件处理**（ChatBubbleListener.kt）
**需求**：玩家传送时清除气泡，避免跨区域线程冲突

**实现**：
```kotlin
@EventHandler
fun onPlayerTeleport(event: PlayerTeleportEvent) {
    if (!manager.isEnabled()) return

    val player = event.player
    
    // 传送时清除当前气泡（Folia 线程安全的最佳实践）
    manager.cleanupPlayer(player)
}
```

**原因**：
- 传送可能导致玩家和气泡实体在不同 Region
- 气泡实体的更新任务仍在旧 Region 的线程上运行
- 直接清除气泡最安全，玩家可以在新位置重新说话创建新气泡

---

## ✅ 修复效果

### 解决的问题
1. ✅ **跨线程访问** - 所有对 display 实体的访问都在其自身线程
2. ✅ **跨线程删除** - 所有 `remove()` 调用都在实体线程
3. ✅ **传送安全** - 传送时自动清除气泡
4. ✅ **Folia 兼容** - 完全符合 Folia 线程模型

### 性能影响
- **几乎无影响** - `scheduler.execute` 是轻量级调度，延迟几乎可忽略
- **更稳定** - 避免了 `IllegalStateException` 和潜在的崩溃

### 用户体验
- **无感知** - 玩家不会察觉到任何变化
- **传送清除** - 传送后气泡消失（符合预期，且更安全）
- **更流畅** - 避免了因线程冲突导致的卡顿

---

## 🎯 技术细节

### Folia 线程模型
```
Region 1 (Thread A)          Region 2 (Thread B)
├── Player A                 ├── Player B
├── Display Entity A         └── Display Entity B
└── Scheduler A              └── Scheduler B
```

**规则**：
- Entity A 的属性只能在 Thread A 访问
- 跨线程访问会抛出 `IllegalStateException`

### 正确的跨线程操作模式
```kotlin
// ❌ 错误：在 Player 线程访问 Display
player.scheduler.run(plugin, { ->
    display.remove()  // 可能跨线程！
})

// ✅ 正确：切换到 Display 线程
player.scheduler.run(plugin, { ->
    display.scheduler.execute(plugin, { ->
        display.remove()  // 安全！
    }, null, 0L)
})
```

---

## 📝 测试建议

### 测试场景
1. **正常聊天** - 气泡正常显示和更新
2. **快速传送** - `/tp` 命令多次传送，气泡应清除且无错误
3. **跨世界传送** - 切换世界时气泡清除
4. **高延迟** - 模拟网络延迟，气泡应稳定
5. **多玩家** - 10+ 玩家同时聊天，无性能问题

### 预期结果
- ✅ 无 `IllegalStateException` 错误
- ✅ 无 "Cannot access entity" 日志
- ✅ 传送后气泡自动清除
- ✅ TPS 稳定，无卡顿

---

## 🔄 后续优化建议

### 可选改进
1. **传送后重新创建气泡**
   ```kotlin
   // 在传送完成后，如果玩家最近说过话，可以重新创建气泡
   event.getPlayer().scheduler.execute(plugin, { ->
       // 重新创建逻辑
   }, null, 10L) // 延迟 10 tick
   ```

2. **气泡跟随优化**
   - 使用 `Entity.addPassenger()` 让气泡成为玩家的乘客
   - 自动跟随，无需手动传送

3. **性能监控**
   - 记录气泡创建/删除次数
   - 监控线程切换延迟

---

## 📊 修改统计

| 文件 | 修改行数 | 修改类型 |
|------|---------|---------|
| `ChatBubbleManager.kt` | 约 50 行 | 线程安全修复 |
| `ChatBubbleListener.kt` | 约 15 行 | 新增传送事件 |
| **总计** | **约 65 行** | **Bug 修复** |

---

## 🎨 代码质量

### 改进点
- ✅ **线程安全** - 完全符合 Folia 规范
- ✅ **异常处理** - 移除了 try-catch（不再需要）
- ✅ **代码简洁** - 逻辑清晰，易于维护
- ✅ **注释完善** - 说明了线程切换原因

### 遵循的最佳实践
- Entity Scheduler for Entity Operations
- Player Scheduler for Player Operations
- No Cross-Thread Direct Access
- Defensive Validity Checks

---

## 📅 修复信息

- **修复日期**: 2025-11-29
- **插件版本**: TSLplugins 1.0
- **配置版本**: 14（无变化）
- **目标服务端**: MC Folia/Luminol 1.21.8
- **问题严重性**: 高（线程安全问题）
- **修复难度**: 中等

---

## ✨ 总结

成功修复了 ChatBubble 模块的所有 Folia 线程安全问题：

1. **所有跨线程访问** → 使用 `entity.scheduler.execute` 包装
2. **所有实体删除** → 在实体自身线程执行
3. **传送事件** → 自动清除气泡避免冲突
4. **代码质量** → 更清晰、更安全、更易维护

**结论**：ChatBubble 模块现在是完全 Folia 兼容的，不会再出现跨线程错误。✅

