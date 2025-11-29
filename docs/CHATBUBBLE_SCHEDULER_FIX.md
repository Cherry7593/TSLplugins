# ChatBubble 跨线程修复技术要点

## 🎯 问题核心

在 Folia 中，**调度器绑定到 Region，而非实体**：
```
玩家在 Region A 创建气泡并注册定时删除任务
↓
player.scheduler.runDelayed() → 任务绑定到 Region A
↓
玩家传送到 Region B（气泡作为 passenger 跟随）
↓
5秒后，Region A 执行删除任务
↓
尝试删除在 Region B 的气泡 → ❌ 跨线程错误
```

## ✅ 解决方案

**使用实体自己的调度器，而非玩家的调度器**

```kotlin
// ❌ 错误：任务不跟随玩家传送
player.scheduler.runDelayed { 
    display.remove() 
}

// ✅ 正确：任务跟随实体移动
display.scheduler.runDelayed { 
    display.remove() 
}
```

## 📝 核心代码

### 定时删除任务
```kotlin
// 使用 display 的调度器（任务跟随实体）
display.scheduler.runDelayed(plugin, { _ ->
    try {
        if (display.isValid) {
            display.remove()
        }
    } catch (e: Exception) {}
    
    // 清理引用使用玩家调度器
    player.scheduler.run(plugin, { _ ->
        bubbles.remove(player)
    }, null)
}, null, timeSpan.toLong())
```

### 安全清理方法
```kotlin
private fun safeRemoveBubble(player: Player, display: TextDisplay) {
    try {
        display.scheduler.run(plugin, { _ ->
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

## 💡 关键理解

1. **实体调度器的特性**：任务会自动转移到实体当前所在的 Region
2. **玩家调度器的限制**：任务绑定到玩家创建任务时所在的 Region
3. **双层调度的必要性**：删除实体用实体调度器，清理数据用玩家调度器

## ✅ 修复完成

- [x] 定时删除线程安全
- [x] 玩家退出线程安全
- [x] 传送场景完美支持
- [x] 代码简洁清晰

---
**日期**: 2025-11-29  
**版本**: TSLplugins v1.0

