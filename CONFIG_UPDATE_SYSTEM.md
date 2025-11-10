# 配置文件更新机制说明

## 功能概述
自动检测并更新配置文件版本，保持配置文件的原始顺序，同时保留用户的现有配置值。

## 核心特性

### 1. ✅ 版本检测
- 插件启动时自动检测配置文件版本
- 版本号定义在 `ConfigUpdateManager.CURRENT_CONFIG_VERSION`
- 配置文件中的 `config-version` 字段用于标识版本

### 2. ✅ 智能更新
更新策略：**只补新键，不动老值**

- **保留用户配置**：所有已存在的配置项保持用户自定义的值
- **添加新配置项**：只添加用户配置中不存在的新配置项（使用默认值）
- **保持配置顺序**：完全按照插件内默认配置文件的顺序重新生成
- **版本号置顶**：`config-version` 始终在配置文件最上方

### 3. ✅ 安全机制
- 更新前自动备份旧配置文件到 `config.yml.backup`
- 如果更新失败，自动从备份恢复
- 详细的日志输出，显示更新过程和结果

## 工作流程

```
插件启动
    ↓
检查配置文件是否存在
    ↓
[不存在] → 创建默认配置文件
    ↓
[存在] → 读取配置文件版本号
    ↓
版本号对比
    ↓
[版本一致] → 无需更新
    ↓
[版本不同] → 开始更新流程
    ↓
1. 读取默认配置（插件JAR内）
2. 读取用户配置（服务器文件）
3. 备份用户配置
4. 创建新配置对象
5. 设置版本号（置顶）
6. 按默认配置顺序合并配置项
   - 存在的键 → 使用用户的值
   - 不存在的键 → 使用默认值
7. 保存新配置文件
    ↓
更新完成
```

## 代码逻辑

### 顺序保持机制
```kotlin
// 创建新配置对象
val newConfig = YamlConfiguration()

// 1. 首先设置版本号（确保在最上方）
newConfig.set("config-version", CURRENT_CONFIG_VERSION)

// 2. 按照默认配置的顺序遍历所有键
for (key in defaultConfig.getKeys(true)) {
    if (key == "config-version") continue // 跳过已设置的版本号
    
    if (currentConfig.contains(key)) {
        // 保留用户的旧配置值
        newConfig.set(key, currentConfig.get(key))
    } else {
        // 添加新的配置项（使用默认值）
        newConfig.set(key, defaultConfig.get(key))
    }
}
```

### 配置合并策略
- ✅ **已存在的配置项**：保留用户自定义的值
- ✅ **新增的配置项**：使用插件默认值
- ✅ **配置顺序**：完全按照默认配置的顺序
- ✅ **版本号位置**：始终在配置文件第一行

## 使用示例

### 场景1：首次安装
```
[INFO] 配置文件不存在，已创建默认配置文件（版本 2）
```

### 场景2：版本一致
```
[INFO] 配置文件版本正确（v2），无需更新
```

### 场景3：版本不同（需要更新）
```
[INFO] 检测到配置文件版本不同（当前: v1, 最新: v2）
[INFO] 开始更新配置文件，保持配置顺序并保留现有配置值...
[INFO] 已备份旧配置文件到: config.yml.backup
[INFO]   + 添加新配置项: fakeplayer.enabled
[INFO]   + 添加新配置项: bossvoice.ender_dragon
[INFO]   + 添加新配置项: bossvoice.wither
[INFO] 配置文件更新完成！
[INFO]   - 保留了 25 个现有配置项
[INFO]   - 添加了 3 个新配置项
[INFO]   - 配置文件已更新到版本 2
```

## 优势对比

### 之前的问题
- ❌ 配置文件顺序混乱，新配置项追加在末尾
- ❌ 版本号位置不固定
- ❌ 没有备份机制

### 现在的优化
- ✅ 配置文件顺序与默认配置完全一致
- ✅ 版本号始终在最上方
- ✅ 自动备份，更新失败可恢复
- ✅ 详细的日志输出

## 配置文件版本管理

### 更新版本号
当需要添加新配置项时：
1. 修改 `config.yml`（插件资源文件）添加新配置项
2. 修改 `ConfigUpdateManager.CURRENT_CONFIG_VERSION` 增加版本号
3. 重新编译插件

### 版本号定义
```kotlin
companion object {
    const val CURRENT_CONFIG_VERSION = 2
}
```

### 配置文件示例
```yaml
config-version: 2  # 始终在最上方

# 成就消息配置
advancement:
  enabled: true
  message: "&a[成就] &f{player} &7获得了成就 &e{advancement}"

# 维护模式配置
maintenance:
  enabled: false
  kick-message:
    - "&c⚠ 服务器维护中 ⚠"

# 新增配置项（版本2新增）
fakeplayer:
  enabled: true

bossvoice:
  ender_dragon: 100
  wither: 50
```

## 注意事项

1. **备份重要性**：更新前自动备份到 `config.yml.backup`
2. **版本号管理**：每次修改配置结构都应更新版本号
3. **顺序一致性**：确保默认配置文件的顺序是你想要的最终顺序
4. **用户配置保护**：所有用户自定义的值都会被保留

## 相关文件
- `ConfigUpdateManager.kt` - 配置文件更新管理器
- `TSLplugins.kt` - 插件主类（启动时调用更新检查）
- `src/main/resources/config.yml` - 默认配置文件模板

