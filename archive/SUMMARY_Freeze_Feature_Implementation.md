# Freeze 冻结玩家功能实现总结

**日期**: 2025-11-24  
**功能类型**: 管理员工具

---

## 功能概述

Freeze 是一个管理员工具，用于冻结玩家的所有操作，常用于处理问题玩家或进行调查。

---

## 功能特性

### 1. 冻结操作
- **命令冻结**：`/tsl freeze <玩家> [时间]`
  - 永久冻结：不指定时间
  - 定时冻结：指定秒数，到期自动解冻
- **权限绕过**：拥有 `tsl.freeze.bypass` 权限的玩家无法被冻结

### 2. 解冻操作
- **命令解冻**：`/tsl freeze unfreeze <玩家>`
- **自动解冻**：定时冻结到期后自动解除

### 3. 列表查询
- **查看列表**：`/tsl freeze list`
  - 显示所有被冻结的玩家
  - 显示剩余时间
  - 显示在线/离线状态

### 4. 冻结限制
被冻结的玩家无法执行以下操作：
- ❌ 移动（位置移动，视角转动正常）
- ❌ 破坏方块
- ❌ 放置方块
- ❌ 与方块交互
- ❌ 与实体交互
- ❌ 使用指令
- ❌ 丢弃物品
- ❌ 捡起物品
- ❌ 切换手持物品

### 5. ActionBar 提示
- 被冻结的玩家会在 ActionBar 显示冻结提示
- 每秒更新一次
- 显示剩余时间或"永久冻结"

---

## 文件结构

```
Freeze/
├── FreezeManager.kt        # 管理器：配置、冻结状态、过期检查
├── FreezeCommand.kt        # 命令处理器：freeze/unfreeze/list
└── FreezeListener.kt       # 监听器：阻止各种操作、ActionBar提示
```

---

## 代码实现

### FreezeManager.kt

**职责**：
- 管理配置和消息
- 管理冻结状态（UUID → 过期时间戳）
- 自动过期检查（每秒一次）

**核心方法**：
```kotlin
fun freezePlayer(uuid: UUID, duration: Int = -1)  // 冻结玩家
fun unfreezePlayer(uuid: UUID): Boolean           // 解冻玩家
fun isFrozen(uuid: UUID): Boolean                 // 检查是否冻结
fun getRemainingTime(uuid: UUID): Int             // 获取剩余时间
fun getFrozenPlayers(): Map<UUID, Long>           // 获取冻结列表
```

**自动过期检查**：
```kotlin
Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, { _ ->
    // 每秒检查过期的冻结
    // 自动移除并通知玩家
}, 20L, 20L)
```

---

### FreezeCommand.kt

**职责**：处理 `/tsl freeze` 相关命令

**命令格式**：
- `/tsl freeze <玩家> [时间]` - 冻结玩家
- `/tsl freeze unfreeze <玩家>` - 解冻玩家
- `/tsl freeze list` - 列出被冻结的玩家

**检查流程**：
1. 功能是否启用
2. 权限检查 (`tsl.freeze.use`)
3. 目标玩家检查
4. Bypass 权限检查

**时间格式化**：
```kotlin
private fun formatTime(seconds: Int): String {
    return when {
        seconds < 60 -> "${seconds}秒"
        seconds < 3600 -> "${seconds / 60}分${seconds % 60}秒"
        else -> "${seconds / 3600}小时${(seconds % 3600) / 60}分"
    }
}
```

---

### FreezeListener.kt

**职责**：阻止被冻结玩家的所有操作

**监听的事件**：
```kotlin
@EventHandler PlayerMoveEvent           // 阻止移动
@EventHandler BlockBreakEvent           // 阻止破坏方块
@EventHandler BlockPlaceEvent           // 阻止放置方块
@EventHandler PlayerInteractEvent       // 阻止交互
@EventHandler PlayerInteractEntityEvent // 阻止与实体交互
@EventHandler PlayerCommandPreprocess   // 阻止使用指令
@EventHandler PlayerDropItemEvent       // 阻止丢弃物品
@EventHandler PlayerAttemptPickupItem   // 阻止捡起物品
@EventHandler PlayerItemHeldEvent       // 阻止切换物品
```

**ActionBar 提示**：
```kotlin
player.scheduler.runAtFixedRate(plugin, { task ->
    if (!player.isOnline) {
        task.cancel()
        return@runAtFixedRate
    }
    
    if (manager.isFrozen(player.uniqueId)) {
        val actionBarText = manager.getMessage("actionbar", ...)
        player.sendActionBar(serializer.deserialize(actionBarText))
    }
}, null, 1L, 20L)
```

---

## 配置文件

### config.yml

```yaml
freeze:
  enabled: true  # 是否启用功能
  
  messages:
    prefix: "&c[冻结]&r "
    disabled: "%prefix%&cFreeze 功能已禁用"
    no_permission: "%prefix%&c你没有权限使用此命令"
    player_not_found: "%prefix%&c玩家 &e{player} &c不在线"
    target_bypass: "%prefix%&c{player} 拥有冻结豁免权限"
    freeze_success: "%prefix%&a已冻结 &e{player} &a持续时间: &e{duration}"
    frozen: "%prefix%&c你已被冻结！持续时间: &e{duration}"
    unfreeze_success: "%prefix%&a已解冻 &e{player}"
    unfrozen: "%prefix%&a你已被解冻！"
    expired: "%prefix%&a你的冻结时间已结束！"
    not_frozen: "%prefix%&c{player} 未被冻结"
    cannot_use_commands: "%prefix%&c你被冻结期间无法使用指令！"
    list_header: "%prefix%&e当前被冻结的玩家:"
    no_frozen_players: "%prefix%&7当前没有被冻结的玩家"
    list_entry: "&7- &e{player} &7[&6{status}&7] &8剩余: &f{time}"
    actionbar: "&c&l⚠ 你已被冻结 ⚠ &8| &f{time}"
    usage: |-
      %prefix%&e使用方法:
      &7/tsl freeze <玩家> [时间] &f- 冻结玩家（时间单位:秒，不填为永久）
      &7/tsl freeze unfreeze <玩家> &f- 解冻玩家
      &7/tsl freeze list &f- 列出被冻结的玩家
```

---

## 权限系统

| 权限 | 说明 | 默认 |
|------|------|------|
| `tsl.freeze.use` | 使用 freeze 命令 | OP |
| `tsl.freeze.bypass` | 免疫被冻结 | OP |

---

## 使用示例

### 管理员操作

```bash
# 永久冻结玩家
/tsl freeze PlayerName

# 冻结玩家 5 分钟（300秒）
/tsl freeze PlayerName 300

# 解冻玩家
/tsl freeze unfreeze PlayerName

# 查看被冻结的玩家列表
/tsl freeze list
```

### 冻结效果

```
玩家被冻结后：
1. 无法移动（但可以转动视角）
2. 无法破坏/放置方块
3. 无法使用任何物品
4. 无法使用指令
5. 无法丢弃/捡起物品
6. ActionBar 显示冻结提示
7. 定时冻结到期后自动解除
```

---

## 技术细节

### 冻结状态管理

**数据结构**：
```kotlin
// 冻结的玩家（UUID -> 过期时间戳）
private val frozenPlayers: MutableMap<UUID, Long> = ConcurrentHashMap()

// -1 表示永久冻结
// > 0 表示定时冻结的过期时间戳
```

**过期检查**：
```kotlin
Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, { _ ->
    val currentTime = System.currentTimeMillis()
    val expired = mutableListOf<UUID>()
    
    frozenPlayers.forEach { (uuid, expireTime) ->
        if (expireTime > 0 && currentTime > expireTime) {
            expired.add(uuid)
        }
    }
    
    expired.forEach { uuid ->
        frozenPlayers.remove(uuid)
        // 通知玩家解冻
    }
}, 20L, 20L)
```

---

### 移动限制

**只阻止位置移动，允许视角转动**：
```kotlin
@EventHandler
fun onPlayerMove(event: PlayerMoveEvent) {
    val from = event.from
    val to = event.to ?: return
    
    // 检查位置是否改变（不检查 pitch 和 yaw）
    if (from.x != to.x || from.y != to.y || from.z != to.z) {
        event.isCancelled = true
    }
}
```

---

### ActionBar 提示

**每秒更新**：
```kotlin
player.scheduler.runAtFixedRate(plugin, { task ->
    if (!player.isOnline) {
        task.cancel()
        return@runAtFixedRate
    }
    
    if (manager.isFrozen(player.uniqueId)) {
        val remaining = manager.getRemainingTime(player.uniqueId)
        val timeText = if (remaining < 0) {
            "永久冻结"
        } else {
            "剩余: ${formatTime(remaining)}"
        }
        
        player.sendActionBar(message)
    }
}, null, 1L, 20L)
```

---

### Folia 兼容性

- ✅ 使用 `GlobalRegionScheduler` 进行过期检查
- ✅ 使用 `EntityScheduler` 进行 ActionBar 更新
- ✅ 使用 `ConcurrentHashMap` 确保线程安全

---

## 集成到主插件

### TSLplugins.kt

```kotlin
// 导入
import org.tsl.tSLplugins.Freeze.FreezeManager
import org.tsl.tSLplugins.Freeze.FreezeCommand
import org.tsl.tSLplugins.Freeze.FreezeListener

// 成员变量
private lateinit var freezeManager: FreezeManager

// 初始化
freezeManager = FreezeManager(this)
val freezeListener = FreezeListener(this, freezeManager)
pm.registerEvents(freezeListener, this)

// 注册命令
dispatcher.registerSubCommand("freeze", FreezeCommand(freezeManager))
dispatcher.registerSubCommand("unfreeze", FreezeCommand(freezeManager))

// 重载方法
fun reloadFreezeManager() {
    freezeManager.loadConfig()
}
```

### ReloadCommand.kt

```kotlin
// 重新加载 Freeze 功能
plugin.reloadFreezeManager()
```

### ConfigUpdateManager.kt

```kotlin
// 配置版本号更新到 10
const val CURRENT_CONFIG_VERSION = 10
```

---

## 测试场景

### 测试 1：永久冻结
```
1. 管理员：/tsl freeze TestPlayer
2. ✅ 验证：TestPlayer 无法移动
3. ✅ 验证：TestPlayer 无法破坏方块
4. ✅ 验证：TestPlayer 无法使用指令
5. ✅ 验证：ActionBar 显示"永久冻结"
6. 管理员：/tsl freeze unfreeze TestPlayer
7. ✅ 验证：TestPlayer 恢复正常
```

### 测试 2：定时冻结
```
1. 管理员：/tsl freeze TestPlayer 60
2. ✅ 验证：TestPlayer 被冻结
3. ✅ 验证：ActionBar 显示剩余时间
4. ✅ 验证：60秒后自动解冻
5. ✅ 验证：TestPlayer 收到解冻提示
```

### 测试 3：Bypass 权限
```
1. 给 TestPlayer 添加 tsl.freeze.bypass 权限
2. 管理员：/tsl freeze TestPlayer
3. ✅ 验证：提示"拥有冻结豁免权限"
4. ✅ 验证：TestPlayer 未被冻结
```

### 测试 4：列表查询
```
1. 冻结多个玩家
2. 管理员：/tsl freeze list
3. ✅ 验证：显示所有被冻结的玩家
4. ✅ 验证：显示在线/离线状态
5. ✅ 验证：显示剩余时间
```

---

## 与其他模块的兼容性

| 模块 | 冻结状态下的行为 | 兼容性 |
|------|------------------|--------|
| Toss | 无法举起生物 | ✅ 完全兼容 |
| Ride | 无法骑乘生物 | ✅ 完全兼容 |
| Kiss | 无法亲吻玩家 | ✅ 完全兼容 |
| Hat | 无法使用帽子 | ✅ 完全兼容 |
| 所有功能 | 被冻结时全部禁用 | ✅ 完全兼容 |

---

## 未来扩展

### 可能的功能增强

1. **冻结原因记录**
   - 记录为什么冻结
   - 在列表中显示原因

2. **冻结日志**
   - 记录冻结/解冻操作
   - 管理员审计

3. **冻结范围控制**
   - 只禁用特定操作
   - 可配置的限制等级

4. **离线玩家冻结**
   - 支持冻结离线玩家
   - 上线后立即生效

---

## 相关文件

### 代码文件
- `src/main/kotlin/org/tsl/tSLplugins/Freeze/FreezeManager.kt`
- `src/main/kotlin/org/tsl/tSLplugins/Freeze/FreezeCommand.kt`
- `src/main/kotlin/org/tsl/tSLplugins/Freeze/FreezeListener.kt`
- `src/main/kotlin/org/tsl/tSLplugins/TSLplugins.kt`
- `src/main/kotlin/org/tsl/tSLplugins/ReloadCommand.kt`
- `src/main/kotlin/org/tsl/tSLplugins/ConfigUpdateManager.kt`

### 配置文件
- `src/main/resources/config.yml`

---

## 总结

Freeze 功能已完整实现并集成到 TSLplugins 插件中：

1. ✅ **冻结系统**：永久冻结 + 定时冻结
2. ✅ **操作限制**：全面阻止被冻结玩家的所有操作
3. ✅ **ActionBar 提示**：实时显示冻结状态和剩余时间
4. ✅ **自动过期**：定时冻结到期自动解除
5. ✅ **列表查询**：查看所有被冻结的玩家
6. ✅ **权限绕过**：Bypass 权限保护管理员
7. ✅ **Folia 兼容**：完全支持 Folia 调度器
8. ✅ **配置化**：所有消息可自定义

**代码风格**：完全遵循现有模块的架构和命名规范，保持一致性。

**与新功能.md 的对照**：所有需求均已实现 ✅

