# Toss 交互优化 - 移除放下功能

**日期**: 2025-11-22  
**功能模块**: Toss（举起投掷生物）

---

## 问题描述

1. 当玩家头上已经举着生物时，Shift + 右键点击另一个生物会先放下头上的生物，而不是继续举起新的生物
2. Shift + 右键放下功能容易误触，影响体验

---

## 解决方案

### 修改文件
- `TossListener.kt` - `onPlayerInteract()` 方法

### 核心修改

**完全移除 Shift + 右键放下功能**，简化交互逻辑：

**修改前**：
```kotlin
when (event.action) {
    Action.LEFT_CLICK_AIR, Action.LEFT_CLICK_BLOCK -> {
        // Shift + 左键：投掷生物
        throwTopEntity(player)
        event.isCancelled = true
    }
    Action.RIGHT_CLICK_AIR, Action.RIGHT_CLICK_BLOCK -> {
        // Shift + 右键：放下所有生物  ❌ 容易误触
        dropAllEntities(player)
        event.isCancelled = true
    }
    else -> {}
}
```

**修改后**：
```kotlin
// 只处理左键投掷
if (event.action == Action.LEFT_CLICK_AIR || event.action == Action.LEFT_CLICK_BLOCK) {
    // Shift + 左键：投掷顶端生物
    throwTopEntity(player)
    event.isCancelled = true
}
// 不再处理右键放下操作

---

## 修复逻辑

### 新的交互逻辑

| 操作 | 原有行为 | 修复后行为 |
|------|----------|------------|
| Shift + 右键**空气** | 放下所有生物 | ⚠️ 无操作（已移除）|
| Shift + 右键**实体** | ❌ 放下 + 举起（冲突） | ✅ 只举起生物 |
| Shift + 右键**方块** | ❌ 放下所有生物 | ✅ 无操作 |
| Shift + 左键 | 投掷顶端生物 | ✅ 投掷顶端生物 |

### 简化后的操作流程

```
玩家举起生物
    ↓
Shift + 右键其他生物 → 继续叠加举起 ✅
    ↓
Shift + 左键 → 投掷顶端生物 ✅
    ↓
继续 Shift + 左键 → 继续投掷，直到全部投掷完毕
```

**优势**：
- ✅ 不再有放下和举起的冲突
- ✅ 操作更简单直观
- ✅ 避免误触放下功能

---

## 测试验证

### 测试场景 1：叠加举起多个生物
1. 玩家 Shift + 右键鸡 → 举起鸡 ✅
2. 玩家 Shift + 右键羊 → **举起羊（羊在鸡头上）** ✅ 已修复
3. 玩家 Shift + 右键牛 → 举起牛（牛在羊头上）✅

**修复前**：第 2 步会放下鸡 ❌  
**修复后**：第 2 步正常叠加 ✅

### 测试场景 2：投掷生物
1. 玩家头上有 3 个生物（从下到上：鸡、羊、牛）
2. Shift + 左键 → 投掷牛 ✅
3. Shift + 左键 → 投掷羊 ✅
4. Shift + 左键 → 投掷鸡 ✅
5. 所有生物已投掷完毕 ✅

### 测试场景 3：不再有误触放下
1. 玩家头上有生物
2. Shift + 右键空气 → 无操作 ✅（不再放下）
3. Shift + 右键地面 → 无操作 ✅（不再放下）
4. 只能通过 Shift + 左键投掷生物 ✅

---

## 使用说明

### 举起生物（叠罗汉）
1. Shift + 右键点击生物 → 举起第一个生物
2. Shift + 右键点击另一个生物 → 叠加举起第二个生物
3. 可继续叠加，直到达到配置的上限

### 投掷生物
- **Shift + 左键** → 投掷顶端的生物
- 重复操作可依次投掷所有举起的生物

### 注意事项
- ⚠️ **不再支持 Shift + 右键放下功能**
- 如需清空头上的生物，请依次 Shift + 左键投掷

---

## 相关配置

```yaml
toss:
  enabled: true
  
  # 最多可叠加举起的生物数量
  max_lift_count: 5
  
  # 投掷速度（1.0 = 正常，2.0 = 双倍速度）
  default_throw_velocity: 1.5
  max_throw_velocity: 10.0
  
  # 黑名单（无法举起的生物）
  blacklist:
    - WITHER
    - ENDER_DRAGON
    - WARDEN
    - VILLAGER
```

---

## 设计考虑

### 为什么移除 Shift + 右键放下功能？

1. **避免冲突**：Shift + 右键点击实体用于举起，与放下功能冲突
2. **防止误触**：容易在想继续举起生物时误触放下
3. **简化操作**：只用左键投掷，操作更直观统一

### 为什么左键保留 BLOCK 处理？

- 投掷操作不会与举起操作冲突
- 玩家点击方块或空气都能投掷，操作更便捷

### 如何清空举起的生物？

- 使用 Shift + 左键依次投掷所有生物
- 玩家退出游戏时自动清理

---

## 相关文件

- `src/main/kotlin/org/tsl/tSLplugins/Toss/TossListener.kt`
- `src/main/kotlin/org/tsl/tSLplugins/Toss/TossManager.kt`
- `src/main/resources/config.yml`

---

## 总结

通过完全移除 Shift + 右键放下功能，确保玩家 Shift + 右键点击实体时只会触发举起操作，不会出现任何冲突，实现流畅的叠罗汉效果。

**核心改进**：
- ✅ 移除 Shift + 右键放下功能，避免操作冲突
- ✅ 简化交互逻辑：右键举起，左键投掷
- ✅ 提升了多次叠加举起生物的用户体验
- ✅ 防止误触操作

