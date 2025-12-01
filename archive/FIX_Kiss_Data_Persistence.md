# Kiss 模块数据持久化修复总结

**修复日期**: 2025-12-01  
**问题**: Kiss 统计数据（kissCount 和 kissedCount）只存储在内存中，服务器重启后会丢失

---

## 🐛 问题分析

### 原始实现问题

**KissManager.kt (旧代码)**:
```kotlin
// 统计数据：亲吻次数（UUID -> 次数）
private val kissCount: MutableMap<UUID, Int> = ConcurrentHashMap()

// 统计数据：被亲吻次数（UUID -> 次数）
private val kissedCount: MutableMap<UUID, Int> = ConcurrentHashMap()

fun incrementKissCount(uuid: UUID) {
    kissCount[uuid] = kissCount.getOrDefault(uuid, 0) + 1
}

fun getKissCount(uuid: UUID): Int {
    return kissCount.getOrDefault(uuid, 0)
}
```

**问题**:
- ❌ 数据只存储在内存 ConcurrentHashMap 中
- ❌ 服务器重启后数据全部丢失
- ❌ 没有持久化到文件或数据库

---

## ✅ 修复方案

### 使用 TSLPlayerProfile 持久化系统

将 Kiss 统计数据集成到现有的 Profile 持久化系统中。

---

## 📝 修改的文件（3个）

### 1. TSLPlayerProfile.kt

**添加统计字段**:
```kotlin
// ==================== 统计数据 ====================

/** Kiss 亲吻次数 */
var kissCount: Int = 0,

/** Kiss 被亲吻次数 */
var kissedCount: Int = 0,
```

---

### 2. TSLPlayerProfileStore.kt

**load() 方法 - 添加读取**:
```kotlin
val profile = TSLPlayerProfile(
    // ...existing fields...
    kissCount = config.getInt("kissCount", 0),
    kissedCount = config.getInt("kissedCount", 0),
    // ...existing fields...
)
```

**save() 方法 - 添加保存**:
```kotlin
config.set("kissCount", profile.kissCount)
config.set("kissedCount", profile.kissedCount)
```

---

### 3. KissManager.kt

**移除内存 Map**:
```kotlin
// 旧代码 ❌
private val kissCount: MutableMap<UUID, Int> = ConcurrentHashMap()
private val kissedCount: MutableMap<UUID, Int> = ConcurrentHashMap()

// 新代码 ✅
// 注意：统计数据现在存储在 TSLPlayerProfile 中，不再使用内存 Map
```

**重写统计方法**:
```kotlin
// 增加亲吻次数（持久化到 Profile）
fun incrementKissCount(uuid: UUID) {
    val profile = dataManager.getProfileStore().getOrCreate(uuid, "Unknown")
    profile.kissCount++
    // 数据会在玩家退出时自动保存
}

// 获取亲吻次数（从 Profile 读取）
fun getKissCount(uuid: UUID): Int {
    val profile = dataManager.getProfileStore().get(uuid)
    return profile?.kissCount ?: 0
}

// 增加被亲吻次数（持久化到 Profile）
fun incrementKissedCount(uuid: UUID) {
    val profile = dataManager.getProfileStore().getOrCreate(uuid, "Unknown")
    profile.kissedCount++
}

// 获取被亲吻次数（从 Profile 读取）
fun getKissedCount(uuid: UUID): Int {
    val profile = dataManager.getProfileStore().get(uuid)
    return profile?.kissedCount ?: 0
}
```

---

## 🔄 数据流程

### 旧流程 ❌
```
玩家亲吻 → 增加内存计数 → 服务器重启 → 数据丢失
```

### 新流程 ✅
```
玩家亲吻
  ↓
增加 Profile.kissCount
  ↓
玩家退出时自动保存到 YAML 文件
  ↓
服务器重启
  ↓
玩家加入时自动从 YAML 文件加载
  ↓
数据恢复 ✅
```

---

## 📊 存储位置

### 文件路径
```
plugins/TSLplugins/playerdata/<UUID>.yml
```

### 文件内容示例
```yaml
playerName: "玩家名"
kissEnabled: true
kissCount: 42        # 亲吻次数（新增）
kissedCount: 38      # 被亲吻次数（新增）
rideEnabled: true
tossEnabled: true
tossVelocity: 1.5
# ...其他字段...
```

---

## ✅ 修复效果

### 持久化保证
- ✅ **玩家退出时自动保存** - 通过 PlayerDataManager.onPlayerQuit()
- ✅ **玩家加入时自动加载** - 通过 PlayerDataManager.onPlayerJoin()
- ✅ **服务器关闭时批量保存** - 通过 TSLplugins.onDisable()

### 数据安全
- ✅ 数据存储在 YAML 文件中
- ✅ 服务器重启后数据保留
- ✅ 与现有的 Profile 系统集成

### 性能优化
- ✅ 内存中缓存 Profile 对象
- ✅ 读写操作直接访问内存
- ✅ 只在必要时写入磁盘

---

## 🧪 测试场景

### 场景 1：正常使用
```
1. 玩家 A 亲吻玩家 B
2. kissCount[A]++ (存储在 Profile)
3. kissedCount[B]++ (存储在 Profile)
4. 玩家退出 → 自动保存到 YAML
5. 服务器重启
6. 玩家加入 → 自动从 YAML 加载
7. 数据正确恢复 ✅
```

### 场景 2：PlaceholderAPI
```
1. 玩家查看 %tsl_kiss_count%
2. 从 Profile 读取 kissCount
3. 返回正确的数字 ✅
```

### 场景 3：长期统计
```
1. 玩家累计亲吻 100 次
2. 经过多次服务器重启
3. 数据始终保留 ✅
```

---

## 📊 代码统计

| 文件 | 修改类型 | 行数 |
|------|---------|------|
| TSLPlayerProfile.kt | 添加字段 | +6 |
| TSLPlayerProfileStore.kt | 读写支持 | +4 |
| KissManager.kt | 重写方法 | ~30 (重构) |
| **总计** | | **~40** |

---

## 💡 技术要点

### 1. 使用现有的持久化系统
```kotlin
// 不需要创建新的存储系统
// 直接使用 TSLPlayerProfile + TSLPlayerProfileStore
val profile = dataManager.getProfileStore().getOrCreate(uuid, "Unknown")
profile.kissCount++
```

### 2. 自动保存机制
```kotlin
// 无需手动调用 save()
// PlayerDataManager 会在玩家退出时自动保存
// TSLplugins.onDisable() 会在服务器关闭时批量保存
```

### 3. 内存缓存
```kotlin
// Profile 对象在内存中缓存
// 读写操作快速
// 只在必要时写入磁盘
```

---

## 🔒 数据安全

### 备份建议
```bash
# 定期备份 playerdata 目录
plugins/TSLplugins/playerdata/
```

### 数据迁移
- ✅ 旧数据（内存）会丢失
- ✅ 新数据会正确保存
- ✅ 从修复版本开始，所有数据都会持久化

---

## 📝 注意事项

### 迁移说明
- **旧版本的 Kiss 统计数据无法迁移**（因为从未保存过）
- 从修复版本开始，新的统计数据会正确保存
- 现有玩家的 kissCount 和 kissedCount 会从 0 开始

### 兼容性
- ✅ 与现有的 Profile 系统完全兼容
- ✅ 不影响其他功能
- ✅ YAML 文件格式向后兼容

---

## 🔗 相关文件

```
Modified:
├── TSLPlayerProfile.kt              # 添加统计字段
├── TSLPlayerProfileStore.kt         # 添加读写支持
└── KissManager.kt                   # 重写统计方法

archive/
└── FIX_Kiss_Data_Persistence.md    # 本文档
```

---

**修复完成时间**: 2025-12-01  
**修复状态**: ✅ 完成  
**编译状态**: ✅ 通过（仅警告，无错误）

