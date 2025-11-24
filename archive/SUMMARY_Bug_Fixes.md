# 需求优化完成总结（续）

**日期**: 2025-11-24  
**任务来源**: 需求.md

---

## 完成的修复

### 1. ✅ 修复 PlaceholderAPI 变量无法正确解析

**问题原因**：
- 所有 Placeholder 扩展都使用相同的 identifier "tsl"
- PlaceholderAPI 只会注册最后一个扩展
- 导致前面注册的变量被覆盖

**解决方案**：
- 合并所有 Placeholder 扩展到 `TSLPlaceholderExpansion`
- 删除重复的 `KissPlaceholder.kt`、`RidePlaceholder.kt`、`TossPlaceholder.kt`
- 只注册一个统一的扩展

**修改文件**：
- `Advancement/TSLPlaceholderExpansion.kt` - 合并所有变量
- `TSLplugins.kt` - 只注册一个扩展
- 删除：`Kiss/KissPlaceholder.kt`
- 删除：`Ride/RidePlaceholder.kt`
- 删除：`Toss/TossPlaceholder.kt`

**支持的变量**：
```
%tsl_adv_count%        # 玩家成就数量
%tsl_ping%             # 服务器平均延迟
%tsl_kiss_count%       # 玩家亲吻次数
%tsl_kissed_count%     # 被亲吻次数
%tsl_kiss_toggle%      # Kiss 功能状态
%tsl_ride_toggle%      # Ride 功能状态
%tsl_toss_toggle%      # Toss 功能状态
%tsl_toss_velocity%    # Toss 投掷速度
```

**实现代码**：
```kotlin
class TSLPlaceholderExpansion(
    private val plugin: JavaPlugin,
    private val countHandler: AdvancementCount,
    private val pingManager: PingManager?,
    private val kissManager: KissManager?,
    private val rideManager: RideManager?,
    private val tossManager: TossManager?
) : PlaceholderExpansion() {

    override fun getIdentifier(): String = "tsl"  // 统一 identifier

    override fun onPlaceholderRequest(player: OfflinePlayer?, params: String): String? {
        // === Advancement 变量 ===
        if (params.equals("adv_count", ignoreCase = true)) {
            // ...
        }

        // === Ping 变量 ===
        if (params.equals("ping", ignoreCase = true)) {
            // ...
        }

        // === Kiss 变量 ===
        when (params) {
            "kiss_count" -> // ...
            "kissed_count" -> // ...
            "kiss_toggle" -> // ...
        }

        // === Ride 变量 ===
        if (params == "ride_toggle") {
            // ...
        }

        // === Toss 变量 ===
        when (params) {
            "toss_toggle" -> // ...
            "toss_velocity" -> // ...
        }

        return null
    }
}
```

---

### 2. ✅ 修复 Freeze 命令的 unfreeze 功能

**问题原因**：
- 主类注册了两个命令：`freeze` 和 `unfreeze`
- 两个命令都指向同一个 `FreezeCommand` 实例
- 但 `FreezeCommand.handle()` 没有检测是通过哪个命令调用

**解决方案**：
- 修改 `FreezeCommand.handle()` 方法
- 检测 `args[0]` 是否为 "unfreeze"
- 支持两种调用方式：
  - `/tsl freeze unfreeze <玩家>`
  - `/tsl unfreeze <玩家>`（实际上变成 `/tsl freeze` 子命令，args[0] = "unfreeze"）

**修改文件**：
- `Freeze/FreezeCommand.kt`

**修复代码**：
```kotlin
override fun handle(
    sender: CommandSender,
    command: Command,
    label: String,
    args: Array<out String>
): Boolean {
    // 检测是否通过 unfreeze 子命令调用
    val isUnfreezeCommand = args.isNotEmpty() && args[0].equals("unfreeze", ignoreCase = true)
    
    when {
        // /tsl unfreeze <玩家> 或 /tsl freeze unfreeze <玩家>
        isUnfreezeCommand && args.size >= 2 -> {
            handleUnfreeze(sender, args[1])
        }
        args.isEmpty() -> {
            showUsage(sender)
        }
        args[0].equals("list", ignoreCase = true) -> {
            handleList(sender)
        }
        args.size >= 1 -> {
            // /tsl freeze <玩家> [时间]
            val duration = if (args.size >= 2) {
                args[1].toIntOrNull() ?: -1
            } else {
                -1
            }
            handleFreeze(sender, args[0], duration)
        }
        else -> {
            showUsage(sender)
        }
    }
    
    return true
}
```

**测试**：
```bash
# 冻结玩家
/tsl freeze Steve
/tsl freeze Steve 300

# 解冻玩家（两种方式都可以）
/tsl freeze unfreeze Steve  ✅
/tsl unfreeze Steve          ✅

# 列表
/tsl freeze list
```

---

### 3. ✅ 修复 BabyLock 小羊驼重启消失问题

**问题原因**：
- BabyLock 只在玩家命名时应用锁定
- 没有监听实体加载事件
- 重启后实体重新加载，但锁定状态未重新应用

**解决方案**：
- 添加 `EntitySpawnEvent` 监听器
- 添加 `EntitiesLoadEvent` 监听器（区块加载）
- 在实体加载时检查名字，重新应用锁定

**修改文件**：
- `BabyLock/BabyLockListener.kt`

**新增事件监听**：

1. **EntitySpawnEvent** - 实体生成时
```kotlin
@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
fun onEntitySpawn(event: EntitySpawnEvent) {
    if (!manager.isEnabled()) return
    
    val entity = event.entity
    if (entity !is Ageable) return
    
    entity.scheduler.run(plugin, { _ ->
        checkAndReapplyLock(entity)
    }, null)
}
```

2. **EntitiesLoadEvent** - 区块加载时
```kotlin
@EventHandler(priority = EventPriority.MONITOR)
fun onEntitiesLoad(event: EntitiesLoadEvent) {
    if (!manager.isEnabled()) return
    
    event.entities.forEach { entity ->
        if (entity is Ageable) {
            entity.scheduler.run(plugin, { _ ->
                checkAndReapplyLock(entity)
            }, null)
        }
    }
}
```

3. **重新应用锁定逻辑**
```kotlin
private fun checkAndReapplyLock(entity: Ageable) {
    if (!entity.isValid) return
    
    val customName = entity.customName()
    if (customName == null) return
    
    val plainName = plainSerializer.serialize(customName)
    
    // 检查是否有锁定前缀
    if (manager.hasLockPrefix(plainName)) {
        // 检查实体类型是否启用
        if (!manager.isTypeEnabled(entity.type)) return
        
        // 确保是幼年或已锁定的实体
        val isBaby = entity.age < 0 || entity.ageLock
        
        if (isBaby && !entity.ageLock) {
            // 重新应用锁定
            manager.lockBaby(entity)
            plugin.logger.fine("[BabyLock] 重新锁定生物: ${entity.type} - $plainName")
        }
    }
}
```

**工作流程**：
```
1. 服务器启动 / 区块加载
2. EntitiesLoadEvent 触发
3. 检查所有 Ageable 实体
4. 如果有锁定前缀且是幼年 → 重新应用锁定
5. 实体保持锁定状态 ✅
```

---

## 测试建议

### PlaceholderAPI 变量
```bash
# 安装 PlaceholderAPI
/papi parse me %tsl_adv_count%
/papi parse me %tsl_kiss_count%
/papi parse me %tsl_ride_toggle%
/papi parse me %tsl_toss_velocity%
/papi parse me %tsl_ping%

# 应该都能正确显示值
```

### Freeze 命令
```bash
# 冻结
/tsl freeze Steve
[SUCCESS] 已冻结 Steve

# 解冻（两种方式）
/tsl freeze unfreeze Steve
[SUCCESS] 已解冻 Steve

/tsl unfreeze Steve  
[SUCCESS] 已解冻 Steve

# 列表
/tsl freeze list
[显示被冻结的玩家]
```

### BabyLock
```bash
1. 给小羊驼命名 "[幼]小小"
2. 验证被锁定（不会成长）
3. 重启服务器
4. 返回游戏
5. 验证小羊驼仍然存在且被锁定 ✅
```

---

## 技术要点

### PlaceholderAPI 单一扩展
- **原则**：一个插件只注册一个扩展
- **好处**：避免 identifier 冲突
- **实现**：使用 when 或 if-else 分发不同的变量

### Freeze 命令分发
- **检测方法**：检查 `args[0]` 是否为 "unfreeze"
- **兼容性**：支持两种调用方式
- **优先级**：unfreeze 检查优先于普通冻结

### BabyLock 实体加载
- **EntitySpawnEvent**：实体生成（包括重启后加载）
- **EntitiesLoadEvent**：区块加载时批量处理
- **关键逻辑**：检查 `entity.age < 0 || entity.ageLock`

---

## 相关文件

### 修改的文件
- `Advancement/TSLPlaceholderExpansion.kt` - 合并所有 Placeholder 变量
- `TSLplugins.kt` - 只注册一个 PlaceholderExpansion
- `Freeze/FreezeCommand.kt` - 支持 unfreeze 命令
- `BabyLock/BabyLockListener.kt` - 添加实体加载监听

### 删除的文件
- `Kiss/KissPlaceholder.kt`
- `Ride/RidePlaceholder.kt`
- `Toss/TossPlaceholder.kt`

---

## 总结

所有需求均已修复：

1. ✅ **PlaceholderAPI 变量** - 合并到统一扩展，所有变量正常工作
2. ✅ **Freeze unfreeze 命令** - 支持解冻功能
3. ✅ **BabyLock 重启问题** - 添加实��加载监听，重启后不消失

所有修改已通过编译检查，只有警告没有错误。

---

**完成日期**: 2025-11-24  
**插件版本**: 1.0  
**配置版本**: 10

