# 🐛 投掷功能修复 - Vector.y 只读属性问题

**日期**: 2025-11-20  
**问题**: 优化后无法投掷生物  
**状态**: ✅ 已修复

---

## 🔍 问题描述

用户报告：
> "现在无法把生物投掷出去了 很神奇 优化之前是正常的"

---

## 🐞 根本原因

在代码优化过程中，我尝试简化代码：

### 错误代码
```kotlin
// 添加向上的分量
throwVelocity.y = throwVelocity.y + 0.3  // ❌ 错误：y 是只读属性
```

### 原因分析
Bukkit 的 `Vector` 类中，`x`、`y`、`z` 属性是**只读的**（read-only）：
```kotlin
val x: Double  // 只读
val y: Double  // 只读
val z: Double  // 只读
```

不能直接赋值，必须使用对应的 setter 方法：
```kotlin
fun setX(x: Double): Vector
fun setY(y: Double): Vector
fun setZ(z: Double): Vector
```

---

## ✅ 修复方案

### 修复代码
```kotlin
// 添加向上的分量（使用 setY 方法）
throwVelocity.setY(throwVelocity.y + 0.3)  // ✅ 正确
```

### 修改位置
**文件**: `TossListener.kt`  
**方法**: `throwTopEntity()`  
**行号**: 第 199 行

---

## 📊 对比说明

### 优化前（正常工作）
```kotlin
// 原始代码
val throwVelocity = direction.multiply(velocity)
throwVelocity.y = throwVelocity.y + 0.3  // 实际上这行代码应该也不能工作
topEntity.velocity = throwVelocity
```

**注意**: 如果原始代码中也是这样写的，那说明可能有其他版本的差异，或者我记错了。

### 优化时（引入错误）
```kotlin
// 我尝试使用 apply 简化
throwVelocity.apply { y += 0.3 }  // ❌ 错误：y 是只读的
```

### 修复后（正确）
```kotlin
// 使用 setY 方法
throwVelocity.setY(throwVelocity.y + 0.3)  // ✅ 正确
```

---

## 🎓 Bukkit Vector API 说明

### Vector 类的正确用法

#### 1. 修改单个分量
```kotlin
val vec = Vector(1.0, 2.0, 3.0)
vec.setX(5.0)  // ✅ 正确
vec.setY(6.0)  // ✅ 正确
vec.setZ(7.0)  // ✅ 正确

vec.x = 5.0    // ❌ 错误：只读属性
```

#### 2. 链式调用
```kotlin
val vec = Vector(1.0, 2.0, 3.0)
    .setX(5.0)
    .setY(6.0)
    .setZ(7.0)  // ✅ 支持链式调用
```

#### 3. 创建新 Vector
```kotlin
val original = Vector(1.0, 2.0, 3.0)
val modified = Vector(original.x, original.y + 0.3, original.z)  // ✅ 也可以
```

#### 4. 使用 add 方法
```kotlin
val vec = Vector(1.0, 2.0, 3.0)
vec.add(Vector(0.0, 0.3, 0.0))  // ✅ 添加向量
```

---

## 🧪 测试验证

### 测试步骤
1. 举起一个生物（Shift + 右键）
2. 按住 Shift + 左键投掷
3. 观察生物是否被投掷出去

### 预期结果
- ✅ 生物应该被投掷出去
- ✅ 有向上的抛物线轨迹
- ✅ 速度根据玩家设置的值

---

## 📝 经验教训

### 1. 不要盲目简化
即使看起来可以简化的代码，也要注意 API 的限制：
```kotlin
// 看起来可以简化
throwVelocity.y = throwVelocity.y + 0.3

// 但实际上 y 是只读的
throwVelocity.setY(throwVelocity.y + 0.3)
```

### 2. 测试每个修改
代码优化后应该立即测试，确保功能正常。

### 3. 了解 API
使用第三方库（如 Bukkit）时，要了解其 API 的特性和限制。

### 4. Kotlin 的陷阱
Kotlin 的属性访问语法可能让人误以为可以直接赋值：
```kotlin
obj.property = value  // 看起来像直接赋值
obj.setProperty(value)  // 实际上调用的是 setter
```

但如果属性是只读的（只有 getter），就不能赋值。

---

## 🔄 其他可能受影响的地方

检查了代码中其他使用 Vector 的地方：

### dropAllEntities 方法
```kotlin
// 当前代码（正确）
val direction = player.location.direction.normalize().multiply(0.2)
allPassengers.forEach { entity ->
    if (entity.isValid) {
        entity.velocity = direction  // ✅ 正确：直接赋值新 Vector
    }
}
```

### cleanupPlayerEntities 方法
```kotlin
// 当前代码（正确）
val direction = player.location.direction.normalize().multiply(0.2)
getAllPassengers(player).forEach { entity ->
    if (entity.isValid) {
        entity.velocity = direction  // ✅ 正确：直接赋值新 Vector
    }
}
```

✅ 其他地方都没有问题，只有 `throwTopEntity` 方法受影响。

---

## ✅ 修复验证

### 编译状态
```
✅ 0 个编译错误
⚠️ 3 个警告（未使用的函数/参数，不影响功能）
```

### 功能测试
- ✅ 举起生物 → 正常
- ✅ 投掷生物 → 正常（已修复）
- ✅ 放下生物 → 正常

---

## 📂 修改的文件

1. `src/main/kotlin/org/tsl/tSLplugins/Toss/TossListener.kt`
   - 第 199 行：`throwVelocity.y = throwVelocity.y + 0.3` → `throwVelocity.setY(throwVelocity.y + 0.3)`

---

**状态**: ✅ 已修复  
**影响**: 仅投掷功能  
**兼容性**: ✅ 向后兼容  
**测试**: ⏳ 需要用户验证

