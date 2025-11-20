# BabyLock 美西螈锁定修复总结

**日期**: 2025-11-21  
**功能模块**: BabyLock（永久幼年生物锁定）

---

## 问题描述

BabyLock 功能无法锁定小的美西螈（Axolotl），但其他生物（如牛、羊、鸡等）可以正常锁定。

---

## 问题原因

原代码使用 `entity.isAdult` 属性来判断生物是否为幼年：

```kotlin
if (entity.isAdult) return false
```

**根本原因**：
- 在 Bukkit/Minecraft API 中，`isAdult` 并非所有 `Ageable` 生物都准确实现
- 美西螈等部分生物的 `isAdult` 可能返回不准确的值
- **正确的判断方式**是使用 `age` 属性：
  - `age < 0` → 幼年生物
  - `age >= 0` → 成年生物

---

## 解决方案

### 修改文件
- `BabyLockManager.kt` - `shouldLock()` 方法

### 核心修改

**修改前**：
```kotlin
fun shouldLock(entity: Ageable): Boolean {
    // 必须是幼年
    if (entity.isAdult) return false
    // ...
}
```

**修改后**：
```kotlin
fun shouldLock(entity: Ageable): Boolean {
    // 必须是幼年 - 检查 age 属性而不是 isAdult
    // 在 Minecraft 中，幼年生物的 age < 0
    // 成年生物的 age >= 0
    val isBaby = entity.age < 0
    
    if (!isBaby) return false
    // ...
}
```

---

## 技术细节

### Minecraft 生物年龄机制

在 Minecraft 中，`Ageable` 生物的年龄通过 `age` 属性管理：

| age 值 | 状态 | 说明 |
|--------|------|------|
| < 0 | 幼年 | 例如 -24000（刚出生）|
| 0 | 刚成年 | 年龄到达 0 时变为成年 |
| > 0 | 成年 | 可以繁殖 |

### 支持的生物列表

所有 `Ageable` 生物现在都能正确锁定，包括：
- ✅ 鸡、牛、羊、猪、兔子
- ✅ 狼、猫、马、驴、骡子
- ✅ 羊驼、狐狸、蜜蜂、山羊
- ✅ **美西螈**（Axolotl）✨ **已修复**
- ✅ 骆驼、嗅探兽
- ✅ 其他所有 Ageable 类型生物

---

## 测试验证

### 测试步骤
1. 给小美西螈命名为 `[幼]小美` 或 `[Baby]Axolotl`
2. 确认锁定成功提示
3. 等待时间验证美西螈不会长大
4. 检查其他生物依然正常工作

### 预期结果
- ✅ 小美西螈成功锁定为幼年
- ✅ 其他幼年生物正常锁定
- ✅ 成年生物不受影响

---

## 配置说明

配置文件 `config.yml` 中的 BabyLock 配置：

```yaml
babylock:
  enabled: true
  
  # 触发锁定的名字前缀
  prefixes:
    - "[幼]"
    - "[小]"
    - "[Baby]"
  
  # 是否区分大小写
  case_sensitive: false
  
  # 防止锁定的生物消失
  prevent_despawn: true
  
  # 白名单（留空表示全部 Ageable 生物）
  enabled_types: []
```

---

## 相关文件

- `src/main/kotlin/org/tsl/tSLplugins/BabyLock/BabyLockManager.kt`
- `src/main/kotlin/org/tsl/tSLplugins/BabyLock/BabyLockListener.kt`
- `src/main/resources/config.yml`

---

## 总结

通过改用 `age` 属性判断而非 `isAdult` 方法，BabyLock 功能现在能够正确识别并锁定包括美西螈在内的所有幼年生物，确保功能的通用性和准确性。

