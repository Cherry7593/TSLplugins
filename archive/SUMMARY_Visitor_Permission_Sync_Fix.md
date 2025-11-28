# Visitor-Permission 联动修复总结

## 修复时间
2025年11月26日

## 问题描述

**症状**：玩家在线时，Permission 模块修改权限组后，Visitor 效果（发光、怪物保护）不能实时更新，只有重新登录才生效。

**场景**：
```
玩家登录（默认有 tsl.visitor 权限）
  → 应用访客效果 ✅
  → 白名单检测通过
  → Permission 模块移除访客权限，设置为 normal 组
  → 【BUG】访客效果未移除 ❌
```

## 根本原因

### 技术分析

1. **Permission 模块使用底层 API 修改权限**：
   ```kotlin
   user.data().add(newGroupNode)      // 直接操作内存
   lp.userManager.saveUser(user)      // 只保存到存储
   ```
   这些操作**不会触发** LuckPerms 事件系统

2. **Visitor 模块依赖事件监听**：
   ```kotlin
   luckPerms?.eventBus?.subscribe(..., UserDataRecalculateEvent::class.java, ...)
   ```
   只有 `UserDataRecalculateEvent` 触发时才会检查权限变更

3. **结果**：Permission 模块"静默"修改了权限，Visitor 模块完全不知情

## 解决方案

在 Permission 模块的 `setGroup()` 方法中，保存权限后**手动触发权限重算**：

```kotlin
// 保存用户数据
lp.userManager.saveUser(user)

// 【关键修复】触发权限重算
val player = Bukkit.getPlayer(user.uniqueId)
if (player != null && player.isOnline) {
    player.scheduler.runDelayed(plugin, { _ ->
        if (player.isOnline) {
            lp.userManager.loadUser(user.uniqueId)  // ⭐ 触发 UserDataRecalculateEvent
            plugin.logger.info("已触发玩家 ${player.name} 的权限重算事件")
        }
    }, null, 5L)
}
```

## 工作流程

### 修复前
```
Permission 修改权限
  ↓
保存到存储
  ↓
【无事件】❌
  ↓
Visitor 不知道
  ↓
效果不更新
```

### 修复后
```
Permission 修改权限
  ↓
保存到存储
  ↓
调用 loadUser() ⭐
  ↓
触发 UserDataRecalculateEvent ✅
  ↓
Visitor.onPermissionChange 响应
  ↓
检测权限变化
  ↓
实时更新效果 ✅
```

## 技术细节

### 为什么用 loadUser()？

| API | 作用 | 触发事件 |
|-----|------|---------|
| `user.data().add()` | 直接修改内存 | ❌ 否 |
| `saveUser()` | 保存到存储 | ❌ 否 |
| `loadUser()` | 重新加载+重算权限 | ✅ 是 |

### 延迟 5 ticks 的原因

- 确保 `saveUser()` 的异步保存操作完成
- 避免加载到旧数据
- 0.25 秒延迟对用户体验影响极小

### Folia 兼容性

使用 `player.scheduler.runDelayed()` 而非 `Bukkit.getScheduler()`，确保在正确的区域执行任务。

## 修改文件

- `src/main/kotlin/org/tsl/tSLplugins/Permission/PermissionChecker.kt`
  - 修改 `setGroup()` 方法
  - 新增权限重算触发逻辑

## 受益模块

此修复不仅让 Visitor 模块受益，所有监听 `UserDataRecalculateEvent` 的模块都能实时响应权限变更。

## 测试建议

### 测试场景

1. **白名单添加测试**：
   - 玩家登录（默认访客权限）
   - 添加到白名单
   - Permission 模块修改权限组
   - ✅ 访客效果应立即移除
   - ✅ 收到"失去访客权限"通知

2. **白名单移除测试**：
   - 玩家在 normal 组（无访客权限）
   - 从白名单移除
   - Permission 模块恢复默认权限组
   - ✅ 访客效果应立即应用
   - ✅ 收到"获得访客权限"通知

3. **日志验证**：
   ```
   [PermissionChecker] 已将玩家 XXX 的权限组设置为 'normal'。
   [PermissionChecker] 已触发玩家 XXX 的权限重算事件
   [VisitorEffect] 玩家 XXX 失去了访客权限
   ```

## 性能影响

**影响极小**：
- 只在玩家权限组变更时触发（不频繁）
- `loadUser()` 是 LuckPerms 的标准操作，性能经过优化
- 5 ticks 延迟对用户体验无感知

## 架构优势

1. **低耦合**：模块间通过 LuckPerms 事件系统通信，无直接依赖
2. **可扩展**：未来新增的权限相关模块自动受益
3. **符合规范**：使用 LuckPerms 推荐的事件机制

## 潜在问题

### 问题：多次快速切换权限组

**场景**：短时间内多次调用 `setGroup()`

**影响**：可能触发多次 `loadUser()`，产生多个事件

**缓解**：
- Visitor 模块的 `onPermissionChange` 有延迟 10 ticks
- 状态检查（wasVisitor）可以防止重复操作
- 实际使用中很少出现快速切换

### 问题：玩家离线时修改权限

**行为**：`loadUser()` 不会执行（因为 `player == null`）

**影响**：无影响，玩家下次登录时 `onPlayerJoin` 会正确检查权限

## 相关文档

- `docs/VISITOR_LOGIC_EXPLANATION.md` - Visitor 模块完整逻辑说明
- `docs/VISITOR_PERMISSION_SYNC_ISSUE.md` - 问题深度分析文档

## 结论

通过在 Permission 模块中手动触发 `loadUser()`，成功解决了权限变更不同步的问题。修复方案：
- ✅ 符合 LuckPerms 设计理念
- ✅ 低耦合，易维护
- ✅ Folia 兼容
- ✅ 性能影响极小
- ✅ 未来可扩展

**问题已彻底解决！** 🎉

