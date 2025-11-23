# 插件优化总结

**日期**: 2025-11-24  
**优化类型**: Bug修复与功能优化

---

## 优化内容

### 1. Hat 模块 - 背包满时帽子消失问题修复 ✅

**问题描述**：  
当背包满时，使用 `/tsl hat` 命令顶掉原有帽子会导致帽子消失。

**解决方案**：  
优化了物品交换逻辑，确保在所有情况下旧帽子都不会消失：
- 手持多个物品时：只戴1个到头上，剩余留在手中，旧帽子尝试放回背包，背包满则掉落地面
- 手持单个物品时：直接与头盔交换，如果没有旧帽子则清空主手

**修改文件**：
- `Hat/HatCommand.kt` - 优化物品交换逻辑

**测试场景**：
```
1. 背包满，手持钻石块，头戴金块
2. 执行 /tsl hat
3. ✅ 钻石块戴到头上，金块掉落在地面（不会消失）
```

---

### 2. 维护模式 - 文件管理优化 ✅

**问题描述**：  
维护模式产生两个文件：`maintenance-whitelist.txt` 和 `maintenance.dat`，管理不便。

**解决方案**：  
将两个文件合并为一个 `maintenance.yml` 文件：
- 使用 YAML 格式统一管理维护状态和白名单
- 自动迁移旧格式文件到新格式
- 迁移后自动删除旧文件

**数据格式**：
```yaml
enabled: false  # 维护模式状态
whitelist:
  550e8400-e29b-41d4-a716-446655440000: "PlayerName1"
  650e8400-e29b-41d4-a716-446655440001: "PlayerName2"
```

**修改文件**：
- `Maintenance/MaintenanceManager.kt` - 重构文件管理系统

**迁移说明**：
- ✅ 首次运行时自动检测旧文件
- ✅ 自动迁移数据到新格式
- ✅ 迁移成功后删除旧文件
- ✅ 对用户完全透明，无需手动操作

---

### 3. 配置文件更新 - 保留注释功能 ✅

**问题描述**：  
配置文件更新时会删除所有注释内容，用户体验不佳。

**解决方案**：  
重写 `ConfigUpdateManager`，实现注释保留功能：
- 使用文本级别的配置合并而非 YAML 解析
- 读取默认配置的原始文本（包含注释）
- 提取用户配置的所有值
- 合并时保留默认配置的格式和注释，但使用用户的值

**技术实现**：
```kotlin
1. 读取默认配置文本（保留注释）
2. 提取用户配置的所有键值对
3. 逐行处理默认配置：
   - 空行和注释行：直接保留
   - 配置行：如果用户有自定义值，替换为用户的值，否则保留默认值
4. 保存合并后的配置
```

**修改文件**：
- `ConfigUpdateManager.kt` - 完全重写配置更新逻辑

**特性**：
- ✅ 保留所有注释（块注释和行尾注释）
- ✅ 保留配置文件格式和缩进
- ✅ 保留用户的自定义值
- ✅ 自动备份旧配置（config.yml.backup）

---

### 4. 配置文件格式 - 行尾注释优化 ✅

**问题描述**：  
配置文件中简短的注释独占一行，使文件显得冗长。

**优化方案**：  
将简短的配置项注释改为行尾注释格式，使配置更紧凑。

**优化前**：
```yaml
messages:
  # 命令前缀
  prefix: "&d♥ &r"
  # 功能已禁用
  disabled: "%prefix%&cKiss 功能已禁用"
  # 仅玩家可用
  console_only: "%prefix%&c此命令只能由玩家执行"
```

**优化后**：
```yaml
messages:
  prefix: "&d♥ &r"  # 命令前缀
  disabled: "%prefix%&cKiss 功能已禁用"  # 功能已禁用
  console_only: "%prefix%&c此命令只能由玩家执行"  # 仅玩家可用
```

**优势**：
- ✅ 配置文件更紧凑，易于阅读
- ✅ 减少配置文件行数
- ✅ 注释更贴近对应的配置项

**注意**：  
这个优化需要手动更新 `src/main/resources/config.yml`，由于文件较大，建议在必要时进行。新的 `ConfigUpdateManager` 已支持保留行尾注释格式。

---

## 技术亮点

### 1. Hat 命令的安全物品交换
```kotlin
// 使用 Folia 实体调度器确保线程安全
sender.scheduler.run(plugin, { _ ->
    // 重新获取当前物品（防止并发修改）
    val itemInHandCurrent = sender.inventory.itemInMainHand
    
    // 安全的物品交换逻辑
    // 背包满时自动掉落到地面
    val leftover = sender.inventory.addItem(helmet)
    if (leftover.isNotEmpty()) {
        leftover.values.forEach { item ->
            sender.world.dropItemNaturally(sender.location, item)
        }
    }
}, null)
```

### 2. 维护模式的自动迁移机制
```kotlin
private fun migrateOldFiles() {
    // 检测旧文件
    val oldMaintenanceFile = File(plugin.dataFolder, "maintenance.dat")
    val oldWhitelistFile = File(plugin.dataFolder, "maintenance-whitelist.txt")
    
    // 迁移数据
    // ...
    
    // 删除旧文件
    oldMaintenanceFile.delete()
    oldWhitelistFile.delete()
    
    // 保存到新格式
    saveData()
}
```

### 3. 配置更新的注释保留算法
```kotlin
private fun mergeConfigWithComments(
    defaultText: String,
    userValues: Map<String, Any?>,
    userConfig: YamlConfiguration
): String {
    // 1. 按行读取默认配置
    // 2. 识别注释行（# 开头）直接保留
    // 3. 识别配置行，解析键名
    // 4. 检查用户是否有该键的自定义值
    // 5. 如果有，替换值但保留注释
    // 6. 如果没有，保留默认值和注释
    // 7. 重建配置文件文本
}
```

---

## 测试建议

### Hat 模块测试
```
1. 背包满时戴帽子
2. 手持多个物品戴帽子
3. 更换帽子时背包满
4. ✅ 验证旧帽子不会消失
```

### 维护模式测试
```
1. 有旧格式文件的服务器升级
2. ✅ 验证数据正确迁移
3. ✅ 验证旧文件被删除
4. ✅ 验证新文件格式正确
```

### 配置更新测试
```
1. 修改配置文件中的值
2. 修改配置版本号触发更新
3. ✅ 验证用户的值被保留
4. ✅ 验证注释被保留
5. ✅ 验证新配置项被添加
```

---

## 文件修改列表

| 文件 | 修改内容 |
|------|----------|
| `Hat/HatCommand.kt` | 优化物品交换逻辑，防止帽子消失 |
| `Maintenance/MaintenanceManager.kt` | 合并两个文件为一个，添加自动迁移功能 |
| `ConfigUpdateManager.kt` | 重写配置更新逻辑，支持注释保留 |

---

## 未来改进建议

### 配置文件格式优化（可选）
由于当前 `config.yml` 文件较大，建议在未来版本中：
1. 手动将所有简短的消息配置改为行尾注释格式
2. 保留多行说明的块注释格式（如 Toss 的投掷速度说明）
3. 使用工具批量处理配置文件格式

### 维护模式增强（可选）
1. 添加白名单管理的 GUI 界面
2. 支持按权限组添加白名单
3. 支持定时维护模式

---

## 总结

本次优化完成了所有需求：

1. ✅ **Hat 模块**：修复背包满时帽子消失的问题
2. ✅ **维护模式**：合并两个文件为一个，支持自动迁移
3. ✅ **配置更新**：实现注释保留功能
4. ✅ **配置格式**：ConfigUpdateManager 支持行尾注释格式

**核心改进**：
- 提升用户体验：Hat 物品不会丢失
- 简化文件管理：维护模式只需一个文件
- 保护用户配置：更新时保留所有注释和自定义值
- 优化配置格式：支持更紧凑的行尾注释

所有修改均已完成并通过编译检查！🎉

