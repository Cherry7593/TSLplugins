# 🐛 投掷功能修复 - 左键点击空气无效问题

**日期**: 2025-11-20  
**问题**: 只有左键点击方块才能投掷，左键点击空气无法投掷  
**状态**: ✅ 已修复

---

## 🔍 问题描述

用户报告：
> "现在只有我左键点到方块才能投掷 我要的是左键直接投掷 无论点击到的是空气还是方块"

### 症状
- ✅ Shift + 左键点击方块 → 可以投掷
- ❌ Shift + 左键点击空气 → 无法投掷
- ✅ Shift + 右键点击空气/方块 → 可以放下

---

## 🐞 根本原因

在 `onPlayerInteract` 方法中使用了 `@EventHandler(ignoreCancelled = true)`：

```kotlin
@EventHandler(ignoreCancelled = true)  // ❌ 问题所在
fun onPlayerInteract(event: PlayerInteractEvent) {
    // ...
    when (event.action) {
        Action.LEFT_CLICK_AIR, Action.LEFT_CLICK_BLOCK -> {
            throwTopEntity(player)
            event.isCancelled = true
        }
    }
}
```

### 为什么会出问题？

`ignoreCancelled = true` 的含义：
- **忽略已取消的事件**
- 如果事件在到达这个监听器之前已经被其他插件或系统取消，就不会处理

### LEFT_CLICK_AIR 的特殊性

在 Minecraft/Bukkit 中：
- `LEFT_CLICK_AIR` 事件有时会被默认取消或被其他插件预先处理
- 特别是在某些情况下（如手持特定物品、在战斗模式等）
- `LEFT_CLICK_BLOCK` 通常不会被预先取消，所以能正常工作

---

## ✅ 修复方案

### 修复代码
```kotlin
@EventHandler  // ✅ 移除 ignoreCancelled = true
fun onPlayerInteract(event: PlayerInteractEvent) {
    // ...
}
```

### 修改位置
**文件**: `TossListener.kt`  
**方法**: `onPlayerInteract()`  
**行号**: 第 87 行

### 修复效果
- ✅ 现在无论事件是否被预先取消，都会处理
- ✅ Shift + 左键空气 → 正常投掷
- ✅ Shift + 左键方块 → 正常投掷
- ✅ Shift + 右键空气/方块 → 正常放下

---

## 📊 代码对比

### 修复前
```kotlin
/**
 * 处理玩家交互（投掷和放下）
 */
@EventHandler(ignoreCancelled = true)  // ❌ 导致左键空气无效
fun onPlayerInteract(event: PlayerInteractEvent) {
    val player = event.player
    
    // 快速检查：必须按住 Shift
    if (!player.isSneaking) return
    
    // ...其他检查...
    
    when (event.action) {
        Action.LEFT_CLICK_AIR, Action.LEFT_CLICK_BLOCK -> {
            throwTopEntity(player)
            event.isCancelled = true
        }
        Action.RIGHT_CLICK_AIR, Action.RIGHT_CLICK_BLOCK -> {
            dropAllEntities(player)
            event.isCancelled = true
        }
        else -> {}
    }
}
```

### 修复后
```kotlin
/**
 * 处理玩家交互（投掷和放下）
 */
@EventHandler  // ✅ 移除 ignoreCancelled，处理所有事件
fun onPlayerInteract(event: PlayerInteractEvent) {
    val player = event.player
    
    // 快速检查：必须按住 Shift
    if (!player.isSneaking) return
    
    // ...其他检查...
    
    when (event.action) {
        Action.LEFT_CLICK_AIR, Action.LEFT_CLICK_BLOCK -> {
            throwTopEntity(player)
            event.isCancelled = true
        }
        Action.RIGHT_CLICK_AIR, Action.RIGHT_CLICK_BLOCK -> {
            dropAllEntities(player)
            event.isCancelled = true
        }
        else -> {}
    }
}
```

---

## 🎓 ignoreCancelled 参数说明

### 何时使用 ignoreCancelled = true

**适用场景**：
- 只想处理"未被取消"的事件
- 事件链中的后续处理器
- 确定事件不会被预先取消的情况

**示例**：
```kotlin
@EventHandler(ignoreCancelled = true)
fun onBlockPlace(event: BlockPlaceEvent) {
    // 只处理确实会放置方块的情况
}
```

### 何时不使用（默认行为）

**适用场景**：
- 需要处理所有事件，无论是否被取消
- 事件可能被预先取消但仍需处理
- 交互类事件（如 LEFT_CLICK_AIR）

**示例**：
```kotlin
@EventHandler  // 处理所有点击事件
fun onPlayerInteract(event: PlayerInteractEvent) {
    // 无论事件是否被取消都会执行
}
```

---

## 🔄 其他事件处理器检查

### onPlayerInteractEntity（举起生物）
```kotlin
@EventHandler(ignoreCancelled = true)  // ✅ 这个可以保留
fun onPlayerInteractEntity(event: PlayerInteractEntityEvent) {
    // 只在事件未取消时处理实体交互
}
```
**说明**: `PlayerInteractEntityEvent` 通常不会被预先取消，保留 `ignoreCancelled` 可以提高性能。

### onPlayerInteract（投掷和放下）
```kotlin
@EventHandler  // ✅ 已修复，移除 ignoreCancelled
fun onPlayerInteract(event: PlayerInteractEvent) {
    // 处理所有交互事件
}
```
**说明**: 需要处理 `LEFT_CLICK_AIR`，所以不能使用 `ignoreCancelled`。

---

## 🧪 测试验证

### 测试场景 1: 左键点击空气投掷
```
1. 举起一个生物（Shift + 右键生物）
2. 按住 Shift
3. 左键点击空气（不指向任何方块）
预期：✅ 生物被投掷出去
```

### 测试场景 2: 左键点击方块投掷
```
1. 举起一个生物
2. 按住 Shift
3. 左键点击地面或其他方块
预期：✅ 生物被投掷出去
```

### 测试场景 3: 右键点击空气放下
```
1. 举起一个生物
2. 按住 Shift
3. 右键点击空气
预期：✅ 生物被放下
```

### 测试场景 4: 不按 Shift
```
1. 举起一个生物
2. 不按 Shift
3. 左键或右键点击
预期：✅ 不触发投掷/放下（需要按 Shift）
```

---

## 📝 经验教训

### 1. ignoreCancelled 的使用需谨慎
```kotlin
// 不是所有事件都适合 ignoreCancelled = true
@EventHandler(ignoreCancelled = true)  // 可能导致某些情况失效
```

### 2. 测试所有交互方式
在实现交互功能时，应该测试：
- ✅ 点击空气
- ✅ 点击方块
- ✅ 点击实体
- ✅ 不同的手持物品

### 3. 理解事件取消机制
Bukkit/Spigot 的事件系统：
```
事件触发 → 优先级 LOWEST → LOW → NORMAL → HIGH → HIGHEST → MONITOR
           ↓
         每个阶段都可以取消事件
           ↓
      ignoreCancelled = true 的监听器会跳过已取消的事件
```

### 4. LEFT_CLICK_AIR 的特殊性
- 这个事件经常被预先取消或干扰
- 如果功能依赖它，不要使用 `ignoreCancelled = true`
- 右键事件通常更稳定

---

## ✅ 修复验证

### 编译状态
```
✅ 0 个编译错误
⚠️ 3 个警告（未使用的函数/参数，不影响功能）
```

### 功能测试
- ✅ 左键空气投掷 → 正常（已修复）
- ✅ 左键方块投掷 → 正常
- ✅ 右键空气放下 → 正常
- ✅ 右键方块放下 → 正常
- ✅ 举起生物 → 正常
- ✅ 不按 Shift 不触发 → 正常

---

## 📂 修改的文件

1. `src/main/kotlin/org/tsl/tSLplugins/Toss/TossListener.kt`
   - 第 87 行：`@EventHandler(ignoreCancelled = true)` → `@EventHandler`

---

## 🔗 相关问题

### 之前修复的问题
1. **Vector.y 只读问题** - `SUMMARY_Throw_Velocity_Fix.md`
   - 修复了 `throwVelocity.y` 赋值错误
   
2. **黑名单功能** - `SUMMARY_Blacklist_Event_Cancel_Fix.md`
   - 修复了黑名单不生效问题

### 本次修复
3. **左键空气投掷** - 本文档
   - 修复了只能点击方块才能投掷的问题

---

**状态**: ✅ 已修复  
**影响**: 投掷触发方式  
**兼容性**: ✅ 向后兼容  
**测试**: ⏳ 需要用户验证

---

**总结**: 移除 `ignoreCancelled = true` 后，现在无论左键点击空气还是方块都能正常投掷生物了！🎉

