# Ride 骑乘功能村民交易冲突修复

**日期**: 2025-01-22  
**功能模块**: Ride（生物骑乘）

---

## 问题描述

当玩家启用骑乘模式（`/tsl ride toggle`）后，无法打开村民交易界面。

---

## 问题原因

在 `RideListener.kt` 的 `onPlayerInteractEntity` 方法中，代码在**所有检查完成后无条件取消事件**：

```kotlin
// 执行骑乘操作
entity.scheduler.run(plugin, { _ ->
    // ...
}, null)

// 取消默认交互行为
event.isCancelled = true  // ❌ 无条件取消
```

**问题分析**：
- 即使玩家不满足骑乘条件（例如：手持物品、没有权限、实体在黑名单等）
- 代码在返回前就已经取消了事件
- 这导致村民交易、羊驼开箱等正常交互被阻止

---

## 解决方案

### 修改文件
- `RideListener.kt` - `onPlayerInteractEntity()` 方法

### 核心修改

**修改前**：
```kotlin
// 黑名单检查
if (manager.isEntityBlacklisted(entity.type) &&
    !player.hasPermission("tsl.ride.bypass")) {
    event.isCancelled = true  // ❌ 错误：在不满足条件时也取消
    return
}

// ...其他检查...

// 执行骑乘操作
entity.scheduler.run(plugin, { _ ->
    entity.addPassenger(player)
}, null)

// 取消默认交互行为
event.isCancelled = true  // ❌ 放在最后，无论如何都会执行
```

**修改后**：
```kotlin
// 黑名单检查
if (manager.isEntityBlacklisted(entity.type) &&
    !player.hasPermission("tsl.ride.bypass")) {
    return  // ✅ 直接返回，不取消事件
}

// ...其他检查...

// 取消默认交互行为（必须在执行骑乘前取消）
event.isCancelled = true  // ✅ 只有通过所有检查才取消

// 执行骑乘操作
entity.scheduler.run(plugin, { _ ->
    entity.addPassenger(player)
}, null)
```

---

## 修复逻辑

### 新的事件处理流程

```
玩家右键实体
    ↓
功能未启用？ → 是 → 返回（不取消事件）
    ↓ 否
按住 Shift？ → 是 → 返回（不取消事件）
    ↓ 否
不是生物？ → 是 → 返回（不取消事件）
    ↓ 否
手持物品？ → 是 → 返回（不取消事件）← 村民交易正常
    ↓ 否
无权限？ → 是 → 返回（不取消事件）
    ↓ 否
玩家关闭骑乘？ → 是 → 返回（不取消事件）
    ↓ 否
实体在黑名单？ → 是 → 返回（不取消事件）
    ↓ 否
实体有乘客？ → 是 → 返回（不取消事件）
    ↓ 否
✅ 取消事件 + 执行骑乘
```

**核心原则**：
- ✅ **只有确定要执行骑乘时，才取消默认交互**
- ✅ **不满足条件时，直接返回，保留原版交互**

---

## 测试验证

### 测试场景 1：村民交易
1. 玩家启用骑乘模式：`/tsl ride toggle`
2. 空手右键村民 → 骑上村民 ✅
3. **手持绿宝石右键村民 → 打开交易界面** ✅（已修复）

### 测试场景 2：其他正常交互
- ✅ 手持食物喂养动物
- ✅ 手持剪刀剪羊毛
- ✅ 手持桶挤牛奶
- ✅ 手持鞍给马装鞍
- ✅ 羊驼开箱（右键箱子羊驼）

### 测试场景 3：骑乘功能依然正常
- ✅ 空手右键生物 → 骑上生物
- ✅ 按住 Shift + 空手右键 → 不会骑上（避免与 Toss 冲突）
- ✅ 黑名单生物无法骑乘
- ✅ 玩家关闭骑乘模式时无法骑乘

---

## 影响范围

### 修复的交互问题
1. ✅ **村民交易**（主要问题）
2. ✅ 流浪商人交易
3. ✅ 羊驼开箱
4. ✅ 动物喂养
5. ✅ 所有需要手持物品的生物交互

### 不受影响的功能
- ✅ 空手骑乘功能正常
- ✅ 黑名单机制正常
- ✅ 权限检查正常
- ✅ 玩家个人开关正常

---

## 相关配置

```yaml
ride:
  enabled: true
  
  # 默认玩家是否启用骑乘（false = 需要手动开启）
  default_enabled: false
  
  # 黑名单（无法骑乘的生物）
  blacklist:
    - WITHER
    - ENDER_DRAGON
    - WARDEN
    - GHAST
    - ELDER_GUARDIAN
    - VILLAGER  # 村民在黑名单，但空手依然会触发骑乘
```

**注意**：如果希望村民能正常交易且不被骑乘，建议将 `VILLAGER` 添加到黑名单。

---

## 相关文件

- `src/main/kotlin/org/tsl/tSLplugins/Ride/RideListener.kt`
- `src/main/kotlin/org/tsl/tSLplugins/Ride/RideManager.kt`
- `src/main/resources/config.yml`

---

## 总结

通过调整 `event.isCancelled = true` 的位置，确保只有在**确定执行骑乘操作时**才取消默认交互行为，解决了骑乘功能与村民交易等原版交互的冲突问题。

**核心改进**：
- 从"先检查后取消"改为"先取消后执行"
- 确保事件取消只在满足所有骑乘条件时发生
- 保留了其他情况下的原版交互行为

