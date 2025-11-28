# 维护模式 Folia 兼容性修复

## 修复时间
2025年11月27日

## 问题描述

执行 `/tsl maintenance on` 命令时出现以下错误：
```
java.lang.UnsupportedOperationException
at org.bukkit.craftbukkit.scheduler.CraftScheduler.handle(CraftScheduler.java:517)
at org.bukkit.craftbukkit.scheduler.CraftScheduler.runTaskTimer(CraftScheduler.java:227)
at org.bukkit.craftbukkit.scheduler.CraftScheduler.runTaskLater(CraftScheduler.java:173)
at org.bukkit.craftbukkit.scheduler.CraftScheduler.runTask(CraftScheduler.java:142)
at MaintenancePermissionListener.checkOnlinePlayers(MaintenancePermissionListener.kt:35)
```

## 根本原因

### Folia 的调度器限制

Folia 移除了传统的 `Bukkit.getScheduler()` API，不再支持全局调度器。

**错误代码**：
```kotlin
Bukkit.getScheduler().runTask(plugin, Runnable {
    // 执行代码
})
```

在 Folia 中，必须使用：
- **实体调度器**：`entity.scheduler.run()` - 用于实体相关操作
- **全局区域调度器**：`Bukkit.getGlobalRegionScheduler().run()` - 用于全局操作

### 错误位置

**MaintenancePermissionListener.kt** 中有两处使用了旧 API：

1. **第35行**：`checkOnlinePlayers()` 方法
2. **第73行**：`onWorldChange()` 事件处理

---

## 解决方案

将所有 `Bukkit.getScheduler().runTask()` 替换为 `Bukkit.getGlobalRegionScheduler().run()`。

### 修复前
```kotlin
Bukkit.getScheduler().runTask(plugin, Runnable {
    Bukkit.getOnlinePlayers()
        .filter { !shouldAllowPlayer(it.uniqueId, it.hasPermission("tsl.maintenance.bypass")) }
        .forEach { player ->
            player.kick(kickComponent)
        }
})
```

### 修复后
```kotlin
// 使用全局区域调度器以兼容 Folia
Bukkit.getGlobalRegionScheduler().run(plugin) { _ ->
    Bukkit.getOnlinePlayers()
        .filter { !shouldAllowPlayer(it.uniqueId, it.hasPermission("tsl.maintenance.bypass")) }
        .forEach { player ->
            player.kick(kickComponent)
        }
}
```

---

## 修复详情

### 1. checkOnlinePlayers() 方法

**位置**：MaintenancePermissionListener.kt:35

**修复前**：
```kotlin
Bukkit.getScheduler().runTask(plugin, Runnable {
    // ...
})
```

**修复后**：
```kotlin
Bukkit.getGlobalRegionScheduler().run(plugin) { _ ->
    // ...
}
```

### 2. onWorldChange() 事件

**位置**：MaintenancePermissionListener.kt:73

**修复前**：
```kotlin
Bukkit.getScheduler().runTask(plugin, Runnable {
    // ...
})
```

**修复后**：
```kotlin
Bukkit.getGlobalRegionScheduler().run(plugin) { _ ->
    // ...
}
```

---

## 为什么使用全局区域调度器？

### 场景分析

维护模式需要：
1. 遍历所有在线玩家
2. 检查玩家权限
3. 踢出没有权限的玩家

这些操作涉及**多个玩家/多个区域**，因此必须使用**全局区域调度器**。

### Folia 调度器选择指南

| 操作类型 | 使用的调度器 | API |
|---------|-------------|-----|
| 单个玩家操作 | 实体调度器 | `player.scheduler.run()` |
| 单个实体操作 | 实体调度器 | `entity.scheduler.run()` |
| 区域操作 | 区域调度器 | `region.scheduler.run()` |
| 全局操作 | 全局区域调度器 | `Bukkit.getGlobalRegionScheduler().run()` |
| 命令执行 | 全局区域调度器 | `Bukkit.getGlobalRegionScheduler().run()` |

**维护模式踢人场景**：需要遍历所有玩家（跨区域），使用全局区域调度器 ✅

---

## 语法差异

### Runnable vs Lambda

**旧 API**：
```kotlin
Bukkit.getScheduler().runTask(plugin, Runnable {
    // 代码
})
```

**新 API**：
```kotlin
Bukkit.getGlobalRegionScheduler().run(plugin) { _ ->
    // 代码
}
```

**注意**：
- 新 API 使用 lambda 表达式，不需要 `Runnable`
- Lambda 参数 `_` 表示忽略传入的参数（ScheduledTask）

---

## 测试验证

### 测试步骤
```bash
# 1. 启用维护模式
/tsl maintenance on

# 2. 检查是否有错误日志
# 预期：无 UnsupportedOperationException 错误

# 3. 检查玩家是否被正确踢出
# 没有 bypass 权限且不在白名单的玩家应该被踢出

# 4. 关闭维护模式
/tsl maintenance off
```

### 预期结果
- ✅ 命令执行成功，无错误
- ✅ 维护模式正常启用
- ✅ 无权限玩家被踢出
- ✅ 有权限玩家正常游戏

---

## 其他可能的调度器错误

### 全局搜索结果

已检查项目中所有维护模式相关代码，只有这两处使用了旧 API，现已全部修复。

### 项目其他模块

其他模块已经正确使用了 Folia 兼容的调度器：
- ✅ **PermissionChecker**：使用 `Bukkit.getGlobalRegionScheduler()`
- ✅ **VisitorEffect**：使用 `player.scheduler`
- ✅ **ChatBubbleManager**：使用 `player.scheduler`
- ✅ **FreezeManager**：使用 `player.scheduler`
- ✅ **TossManager**：使用 `entity.scheduler`

---

## 修复的文件

- **MaintenancePermissionListener.kt**
  - 修复 `checkOnlinePlayers()` 方法（第35行）
  - 修复 `onWorldChange()` 事件（第73行）
  - 总计：2处修复

---

## 编译验证

- ✅ **无错误**
- ✅ **无警告**
- ✅ **可直接使用**

---

## 总结

### 问题
维护模式使用了 Folia 不支持的 `Bukkit.getScheduler()` API。

### 解决
将所有旧调度器 API 替换为全局区域调度器。

### 结果
- ✅ 维护模式命令正常工作
- ✅ 完全兼容 Folia
- ✅ 功能保持不变

---

**维护模式 Folia 兼容性修复完成！** 🎉

