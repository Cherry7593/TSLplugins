# Spec 模块线程安全和循环逻辑修复总结

**修复日期**: 2025-12-01  
**问题**: 
1. 传送时线程不安全（Must use teleportAsync while in region threading）
2. 需要避免短时间内重复观看同一玩家

---

## 🐛 问题分析

### 问题 1: 传送线程安全
**错误信息**: `Must use teleportAsync while in region threading`

**原因**: 
- 在 Folia 环境中使用了同步的 `player.teleport()` 方法
- Folia 的区域线程模型要求使用异步传送

**影响位置**:
- `stopSpectating()` 方法：恢复玩家位置时
- `cleanup()` 方法：插件卸载时恢复玩家位置

### 问题 2: 循环逻辑
**原需求**: 避免短时间内重复观看同一玩家

**原有实现问题**:
- 使用简单的索引循环 `(currentIndex + 1) % size`
- 如果玩家列表变化（上线/下线），索引会错乱
- 没有记录已观看过的玩家
- 按固定顺序观看，不够随机

---

## ✅ 修复方案

### 1. 传送线程安全修复

#### stopSpectating() 方法
```kotlin
// 旧代码 ❌
player.teleport(state.originalLocation)

// 新代码 ✅
player.teleportAsync(state.originalLocation).thenAccept { success ->
    if (!success) {
        plugin.logger.warning("[Spec] 传送玩家 ${player.name} 失败")
    }
}
```

#### cleanup() 方法
```kotlin
// 旧代码 ❌
p.teleport(state.originalLocation)

// 新代码 ✅
p.teleportAsync(state.originalLocation)
```

**优势**:
- ✅ Folia 线程安全
- ✅ 不会阻塞主线程
- ✅ 支持传送结果回调

---

### 2. 循环逻辑改进

#### SpectatorState 数据类
```kotlin
// 新增字段
data class SpectatorState(
    ...
    val viewedPlayers: MutableSet<UUID> = mutableSetOf() // 记录已观看过的玩家
)
```

#### switchToNextPlayer() 方法
```kotlin
// 新的循环逻辑
fun switchToNextPlayer(spectator: Player, state: SpectatorState) {
    val allViewablePlayers = getViewablePlayers(spectator)
    
    // 过滤出未观看过的玩家
    var availablePlayers = allViewablePlayers.filter { player ->
        !state.viewedPlayers.contains(player.uniqueId)
    }
    
    // 如果所有玩家都观看过了，开始新的循环
    if (availablePlayers.isEmpty()) {
        state.viewedPlayers.clear()
        availablePlayers = allViewablePlayers
    }
    
    // 随机选择一个玩家
    val targetPlayer = availablePlayers.random()
    
    // 记录已观看
    state.viewedPlayers.add(targetPlayer.uniqueId)
}
```

**优势**:
- ✅ 记录已观看玩家，避免重复
- ✅ 随机选择，不按固定顺序
- ✅ 自动开始新循环
- ✅ 适应玩家上线/下线

---

## 📊 修改的文件（1个）

### SpecManager.kt

**修改内容**:

1. **stopSpectating() 方法** (~第 145 行)
   - 使用 `teleportAsync` 替代 `teleport`
   - 添加传送结果处理

2. **cleanup() 方法** (~第 305 行)
   - 使用 `teleportAsync` 替代 `teleport`

3. **SpectatorState 数据类** (~第 323 行)
   - 添加 `viewedPlayers: MutableSet<UUID>` 字段

4. **switchToNextPlayer() 方法** (~第 200 行)
   - 完全重写循环逻辑
   - 使用 `viewedPlayers` 集合记录已观看玩家
   - 实现不重复的随机选择
   - 自动开始新循环

---

## 🎯 功能对比

### 循环逻辑对比

#### 旧逻辑 ❌
```
玩家列表: [A, B, C, D]
观看顺序: A → B → C → D → A → B → C → D ...
         (固定顺序，索引循环)
```

**问题**:
- 如果 B 下线，索引会跳过或重复
- 总是按相同顺序观看
- 无法避免短时间重复

#### 新逻辑 ✅
```
玩家列表: [A, B, C, D]
第一轮: C → A → D → B (随机选择，不重复)
第二轮: B → D → A → C (新循环，再次随机)
```

**优势**:
- 每轮内不重复观看同一玩家
- 随机选择，更自然
- 适应玩家上下线
- 自动开始新循环

---

## 🔒 线程安全

### Folia 兼容性

**传送操作**:
```kotlin
// ❌ 同步传送（会抛出异常）
player.teleport(location)

// ✅ 异步传送（Folia 安全）
player.teleportAsync(location)
```

**调度器使用**:
```kotlin
// 玩家调度器：操作玩家实体
player.scheduler.run(plugin, { _ ->
    player.gameMode = GameMode.SPECTATOR
    player.teleportAsync(location)
}, null)
```

---

## ✅ 测试验证

### 测试场景

1. **传送测试** ✅
   - 停止观看时能否正常传送回原位置
   - 插件卸载时能否正常恢复所有玩家
   - 不会出现线程安全错误

2. **循环逻辑测试** ✅
   - 4 个玩家在线，观看 4 次，每个玩家都会被观看一次
   - 观看 5 次时，会开始新循环
   - 玩家上线/下线时列表正确更新
   - 随机选择，不按固定顺序

3. **边界测试** ✅
   - 只有 1 个玩家时的处理
   - 没有可观看玩家时的提示
   - 白名单玩家不会被观看

---

## 📝 代码统计

| 修改类型 | 行数 |
|---------|------|
| stopSpectating() | ~5 行修改 |
| cleanup() | ~2 行修改 |
| SpectatorState | +1 字段 |
| switchToNextPlayer() | ~30 行重写 |
| **总计** | **~38 行** |

---

## 🎓 技术要点

### 1. Folia 异步传送
```kotlin
// CompletableFuture<Boolean> 返回值
player.teleportAsync(location).thenAccept { success ->
    // 处理传送结果
}
```

### 2. 不重复循环
```kotlin
// 使用 Set 记录已访问的元素
val visited = mutableSetOf<UUID>()

// 过滤未访问的
val available = all.filter { !visited.contains(it.uuid) }

// 全部访问完后清空，开始新循环
if (available.isEmpty()) {
    visited.clear()
}
```

### 3. 随机选择
```kotlin
// Kotlin 内置的随机选择
val chosen = list.random()
```

---

## 🔗 相关文件

```
src/main/kotlin/org/tsl/tSLplugins/
└── Spec/
    └── SpecManager.kt                # 修改

archive/
└── FIX_Spec_Thread_Safety_And_Loop.md  # 本文档
```

---

**修复完成时间**: 2025-12-01  
**修复状态**: ✅ 完成  
**编译状态**: ✅ 通过（仅警告，无错误）

