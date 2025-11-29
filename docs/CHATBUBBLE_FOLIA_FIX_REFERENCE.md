# ChatBubble Folia 线程安全修复 - 快速参考

## 🔧 修复内容

### 1. 实体访问模式
**所有对 TextDisplay 实体的访问都必须在其自身 scheduler 中**

```kotlin
// ❌ 错误
display.ticksLived = 1
display.remove()

// ✅ 正确
display.scheduler.execute(plugin, { ->
    if (display.isValid) {
        display.ticksLived = 1
        display.remove()
    }
}, null, 0L)
```

### 2. 更新任务架构
```
玩家 Scheduler (主循环)
  ├─ 检查玩家状态 ✅
  ├─ display.scheduler.execute
  │   ├─ 检查 display 状态 ✅
  │   ├─ 更新 ticksLived ✅
  │   ├─ 更新 textOpacity ✅
  │   └─ 传送 display ✅
  └─ 更新可见性 ✅
```

### 3. 传送事件
```kotlin
@EventHandler
fun onPlayerTeleport(event: PlayerTeleportEvent) {
    // 传送时清除气泡，避免跨区域问题
    manager.cleanupPlayer(event.player)
}
```

---

## ⚠️ 常见错误

### 错误 1: 跨线程访问
```
java.lang.IllegalStateException: Entity is not owned by the current region
```
**原因**：在错误的线程访问实体  
**解决**：使用 `entity.scheduler.execute`

### 错误 2: 跨线程删除
```
Cannot remove entity from different region
```
**原因**：在错误的线程调用 `remove()`  
**解决**：在实体自身线程删除

---

## ✅ 测试清单

- [ ] 玩家聊天时气泡正常显示
- [ ] 传送后气泡自动清除
- [ ] 切换世界时无报错
- [ ] 多玩家同时聊天无冲突
- [ ] 无 IllegalStateException 错误
- [ ] TPS 保持稳定

---

## 📝 代码规范

### 修改实体状态
```kotlin
entity.scheduler.execute(plugin, { ->
    if (entity.isValid) {
        // 修改实体属性
    }
}, null, 0L)
```

### 删除实体
```kotlin
entity.scheduler.execute(plugin, { ->
    if (entity.isValid) {
        entity.remove()
    }
}, null, 0L)
```

### 混合操作
```kotlin
player.scheduler.run(plugin, { ->
    // 玩家操作
    val location = player.location
    
    // 实体操作需切换线程
    entity.scheduler.execute(plugin, { ->
        if (entity.isValid) {
            entity.teleport(location)
        }
    }, null, 0L)
})
```

---

## 🎯 关键修改点

| 方法 | 修改内容 | 行号 |
|------|---------|------|
| `createOrUpdateBubble` | 使用 display.scheduler.execute 更新气泡 | ~115 |
| 更新任务 | display.scheduler.execute 包装所有实体操作 | ~155 |
| `cleanupPlayer` | display.scheduler.execute 删除实体 | ~275 |
| `cleanupAll` | display.scheduler.execute 批量删除 | ~285 |
| `onPlayerTeleport` | 新增传送事件处理 | ChatBubbleListener |

---

## 💡 Folia 最佳实践

1. **实体操作 → 实体线程**
2. **玩家操作 → 玩家线程**
3. **跨线程 → scheduler.execute**
4. **传送 → 清理旧数据**
5. **有效性检查 → 先检查再操作**

---

## 📚 相关文档

- 完整总结：`archive/SUMMARY_ChatBubble_Folia_ThreadSafety_Fix.md`
- Folia 文档：https://docs.papermc.io/folia
- 需求文档：`需求.md`

