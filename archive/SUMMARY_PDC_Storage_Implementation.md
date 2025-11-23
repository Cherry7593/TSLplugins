# PDC 离线存储功能实现总结

**日期**: 2025-11-24  
**功能类型**: 系统优化 - 数据持久化

---

## 功能概述

实现了基于 **PersistentDataContainer (PDC)** 的玩家个人配置离线存储功能，替代原有的内存 HashMap 存储方式。

---

## 核心改进

### 之前的方式
- ❌ 使用 `ConcurrentHashMap<UUID, T>` 存储玩家配置
- ❌ 玩家离线后数据丢失
- ❌ 服务器重启后配置重置为默认值

### 现在的方式
- ✅ 使用 **PDC（PersistentDataContainer）** 存储
- ✅ 数据随玩家存档永久保存
- ✅ 服务器重启后自动恢复玩家配置
- ✅ 支持跨服同步（如果使用共享玩家数据）

---

## 技术实现

### 1. PlayerDataManager

创建了统一的玩家数据管理器，封装所有 PDC 操作：

```kotlin
class PlayerDataManager(private val plugin: JavaPlugin) {
    // PDC Keys
    private val kissToggleKey = NamespacedKey(plugin, "kiss_toggle")
    private val rideToggleKey = NamespacedKey(plugin, "ride_toggle")
    private val tossToggleKey = NamespacedKey(plugin, "toss_toggle")
    private val tossVelocityKey = NamespacedKey(plugin, "toss_velocity")
    
    // 提供统一的读写接口
    fun getKissToggle(player: Player, defaultValue: Boolean): Boolean
    fun setKissToggle(player: Player, enabled: Boolean)
    // ...其他方法
}
```

**优势**：
- ✅ 统一管理所有 PDC 键
- ✅ 避免键名冲突
- ✅ 简化各模块的数据存储代码

---

### 2. 存储的数据

| 模块 | 存储内容 | PDC Key | 数据类型 |
|------|----------|---------|----------|
| Kiss | 功能开关 | `tsl:kiss_toggle` | Boolean |
| Ride | 功能开关 | `tsl:ride_toggle` | Boolean |
| Toss | 功能开关 | `tsl:toss_toggle` | Boolean |
| Toss | 投掷速度 | `tsl:toss_velocity` | Double |

---

### 3. Manager 重构

#### Kiss功能
**修改前**：
```kotlin
class KissManager(private val plugin: JavaPlugin) {
    private val playerStates: MutableMap<UUID, Boolean> = ConcurrentHashMap()
    
    fun isPlayerEnabled(uuid: UUID): Boolean {
        return playerStates.getOrDefault(uuid, true)
    }
}
```

**修改后**：
```kotlin
class KissManager(
    private val plugin: JavaPlugin,
    private val dataManager: PlayerDataManager
) {
    // 移除 playerStates HashMap
    
    fun isPlayerEnabled(player: Player): Boolean {
        return dataManager.getKissToggle(player, true)
    }
}
```

#### Ride功能
- ✅ 同样的重构模式
- ✅ 移除 `playerToggleStatus` HashMap
- ✅ 直接从 PDC 读写

#### Toss功能
- ✅ 移除 `playerToggleStatus` 和 `playerThrowVelocity` HashMap
- ✅ 开关和速度都存储在 PDC
- ✅ 投掷速度支持小数精度

---

## PlaceholderAPI 变量

新增了功能状态查询变量：

| 变量 | 说明 | 返回值 |
|------|------|--------|
| `%tsl_kiss_toggle%` | Kiss 功能状态 | "启用" / "禁用" / "离线" |
| `%tsl_ride_toggle%` | Ride 功能状态 | "启用" / "禁用" / "离线" |
| `%tsl_toss_toggle%` | Toss 功能状态 | "启用" / "禁用" / "离线" |
| `%tsl_toss_velocity%` | Toss 投掷速度 | 数值（如 "1.5"）/ "离线" |

**使用场景**：
- Tab 列表显示玩家功能状态
- 计分板展示
- 提示牌、全息文字等

---

## 文件结构

### 新增文件
```
PlayerDataManager.kt          # 统一的 PDC 管理器
Kiss/KissPlaceholder.kt        # Kiss PlaceholderAPI 扩展（已有，已更新）
Ride/RidePlaceholder.kt        # Ride PlaceholderAPI 扩展（新增）
Toss/TossPlaceholder.kt        # Toss PlaceholderAPI 扩展（新增）
```

### 修改文件
```
TSLplugins.kt                  # 初始化 PlayerDataManager
Kiss/KissManager.kt            # 使用 PDC 存储
Kiss/KissCommand.kt            # 传入 Player 而非 UUID
Kiss/KissListener.kt           # 传入 Player 而非 UUID
Ride/RideManager.kt            # 使用 PDC 存储
Ride/RideCommand.kt            # 传入 Player 而非 UUID
Ride/RideListener.kt           # 传入 Player 而非 UUID
Toss/TossManager.kt            # 使用 PDC 存储
Toss/TossCommand.kt            # 传入 Player 而非 UUID
Toss/TossListener.kt           # 传入 Player 而非 UUID
```

---

## PDC 技术细节

### 数据存储位置
```
world/playerdata/<UUID>.dat
  └─ BukkitValues (NBT)
      └─ tsl:kiss_toggle
      └─ tsl:ride_toggle
      └─ tsl:toss_toggle
      └─ tsl:toss_velocity
```

### 读取示例
```kotlin
val pdc = player.persistentDataContainer
val enabled = pdc.get(kissToggleKey, PersistentDataType.BOOLEAN) ?: defaultValue
```

### 写入示例
```kotlin
player.persistentDataContainer.set(
    kissToggleKey, 
    PersistentDataType.BOOLEAN, 
    enabled
)
```

### 数据类型支持
- `BOOLEAN` - 布尔值
- `DOUBLE` - 双精度浮点数
- `INTEGER` - 整数
- `STRING` - 字符串
- 其他：BYTE, SHORT, LONG, FLOAT, BYTE_ARRAY, INTEGER_ARRAY, TAG_CONTAINER 等

---

## 测试场景

### 场景 1：功能开关持久化
```
1. 玩家 A 执行 /tsl kiss toggle（关闭）
2. 玩家 A 退出服务器
3. 玩家 A 重新登录
4. 验证：Kiss 功能仍然是关闭状态 ✅
```

### 场景 2：投掷速度持久化
```
1. 玩家 B 执行 /tsl toss velocity 2.5
2. 玩家 B 退出服务器
3. 服务器重启
4. 玩家 B 重新登录
5. 验证：投掷速度仍然是 2.5 ✅
```

### 场景 3：PlaceholderAPI 变量
```
1. 玩家设置各种功能状态
2. 使用 /papi parse me %tsl_kiss_toggle%
3. 验证：正确显示"启用"或"禁用" ✅
```

### 场景 4：默认值
```
1. 新玩家首次登录
2. 未设置任何配置
3. 验证：使用配置文件中的默认值 ✅
```

---

## 迁移说明

### 从旧版本升级
对于已经使用旧版本的玩家：
- ✅ **无需手动迁移**
- ✅ 第一次执行 toggle 命令时会自动写入 PDC
- ✅ 之前的内存状态会在重启后丢失，但会使用默认值

### 数据清理（可选）
如果需要清理玩家的 TSL 数据：
```kotlin
playerDataManager.clearPlayerData(player)
```

---

## 性能考虑

### PDC vs HashMap 性能对比

| 操作 | HashMap | PDC | 说明 |
|------|---------|-----|------|
| 读取 | 极快 | 快 | PDC 从 NBT 读取，稍慢但可接受 |
| 写入 | 极快 | 较慢 | PDC 需要序列化到 NBT |
| 内存占用 | 高 | 低 | PDC 不占用额外内存 |
| 持久化 | 否 | 是 | PDC 自动保存 |

### 优化策略
1. ✅ **减少频繁写入** - 只在真正改变时写入
2. ✅ **批量读取** - 在需要时才读取，不预加载
3. ✅ **异步处理** - Folia 自动处理实体调度

---

## 兼容性

### Minecraft 版本
- ✅ 1.14+ 全面支持 PDC
- ✅ 本插件目标 1.21.8，完全兼容

### Bukkit/Spigot/Paper/Folia
- ✅ PDC 是 Bukkit API 的一部分
- ✅ 所有分支都支持

### 跨服同步
如果使用共享玩家数据的跨服系统：
- ✅ PDC 数据会自动同步
- ✅ 玩家在任何服务器的配置都一致

---

## 未来扩展

### 可扩展的数据类型
```kotlin
// 可轻松添加新的数据存储
fun getHatEnabled(player: Player): Boolean
fun setHatEnabled(player: Player, enabled: Boolean)

fun getScaleValue(player: Player): Double
fun setScaleValue(player: Player, scale: Double)
```

### 统计数据持久化（未来）
```kotlin
// 当前：统计数据仍在内存
// 未来可以移到 PDC
fun getKissCount(player: Player): Int
fun setKissCount(player: Player, count: Int)
```

---

## 相关文件

### 代码文件
- `src/main/kotlin/org/tsl/tSLplugins/PlayerDataManager.kt`
- `src/main/kotlin/org/tsl/tSLplugins/Kiss/*`
- `src/main/kotlin/org/tsl/tSLplugins/Ride/*`
- `src/main/kotlin/org/tsl/tSLplugins/Toss/*`
- `src/main/kotlin/org/tsl/tSLplugins/TSLplugins.kt`

---

## 总结

通过引入 PDC 存储机制，实现了：

1. ✅ **数据持久化** - 玩家配置永久保存
2. ✅ **服务器重启保留** - 配置不丢失
3. ✅ **统一管理** - PlayerDataManager 封装
4. ✅ **PlaceholderAPI 支持** - 新增状态查询变量
5. ✅ **代码简化** - 移除大量 HashMap 代码
6. ✅ **内存优化** - 减少内存占用
7. ✅ **跨服兼容** - 支持共享玩家数据

**核心原则**：
- 玩家配置应该跟随玩家，而不是服务器重启就重置
- 使用 Minecraft 原生机制（PDC）而非外部数据库
- 保持简单、可靠、高性能

