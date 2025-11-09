# TSLplugins 功能文档

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.21-blue.svg)](https://kotlinlang.org)
[![Paper](https://img.shields.io/badge/Paper-1.21.8-green.svg)](https://papermc.io)
[![Folia](https://img.shields.io/badge/Folia-Supported-brightgreen.svg)](https://papermc.io/software/folia)

> TSL服务器的Minecraft插件集合 - 使用 Kotlin 编写，完全支持 Folia 1.21.8

---

## 🆕 配置自动更新系统

插件现在具有**智能配置更新系统**！

- ✅ **自动检测** - 启动时自动检测配置文件版本
- ✅ **智能合并** - 只添加新配置项，完全保留你的自定义配置
- ✅ **无需手动操作** - 插件更新后自动添加新功能的配置
- ✅ **安全可靠** - 不会覆盖或删除任何现有配置

**详细说明：** 查看 [CONFIG-UPDATE.md](CONFIG-UPDATE.md) 了解更多

---

## 🔄 配置重载命令

**命令：** `/tsl reload`

**权限：** OP 或 `tsl.reload`

**功能：** 在服务器运行时重新加载所有配置文件，无需重启服务器

**重载内容：**
- ✅ 主配置文件 (`config.yml`)
- ✅ 命令别名配置 (`aliases.yml`)
- ✅ 维护模式配置
- ✅ 所有功能模块的配置选项

**使用示例：**
```bash
# 修改配置文件后
/tsl reload

# 输出示例：
# [TSLplugins] 正在重新加载配置文件...
# [TSLplugins] 配置文件重载成功！
# [TSLplugins] - 主配置文件已重载
# [TSLplugins] - 命令别名已重载 (15 个别名)
# [TSLplugins] - 维护模式配置已重载
```

**注意事项：**
- 重载会立即应用所有配置更改
- 别名命令会自动同步到所有在线玩家
- 某些功能（如事件监听器）可能需要重启服务器才能完全生效

---

## 📋 快速导航

- [命令别名系统](#1-命令别名系统) - 自定义命令快捷方式
- [维护模式](#2-维护模式) - 服务器维护管理
- [成就系统](#3-成就系统) - 成就消息过滤
- [访客模式](#4-访客模式) - 新玩家保护
- [权限检测器](#5-权限检测器) - 自动权限分配
- [农田保护](#6-农田保护) - 防止踩踏
- [假玩家显示](#7-假玩家显示) - MOTD 人数调整
- [Boss 声音控制](#8-boss-声音控制) - 自定义 Boss 事件声音范围

---

## 1. 命令别名系统

### ✨ 功能特性

- ✅ 为现有命令创建自定义别名
- ✅ 别名显示为**白色**（不是红色）
- ✅ 完整的 **Tab 补全**支持
- ✅ 支持**中文别名**
- ✅ 支持**子命令简化**
- ✅ 实时重载配置

### 📝 配置示例

**aliases.yml**
```yaml
aliases:
  # 基础别名
  - "t:tpa"              # /t → /tpa
  - "ta:tpaccept"        # /ta → /tpaccept
  - "h:home"             # /h → /home
  
  # 中文命令
  - "传送:tpa"
  - "回家:home"
  - "设置家:sethome"
  
  # 子命令简化
  - "stp tp:teleport"    # /stp tp x y z → /teleport x y z
  - "gms:gamemode survival"    # /gms → /gamemode survival
  - "gmc:gamemode creative"    # /gmc → /gamemode creative
```

### 🎮 使用示例

```bash
# 使用别名
/t PlayerName        # → /tpa PlayerName
/传送 Steve          # → /tpa Steve
/stp tp 100 64 100   # → /teleport 100 64 100

# Tab 补全
/t [Tab]             # 显示在线玩家列表
```

### 🔧 管理命令

| 命令 | 说明 | 权限 |
|------|------|------|
| `/tsl aliasreload` | 重载别名配置 | `tsl.alias.reload` |

---

## 2. 维护模式

### ✨ 功能特性

- ✅ 灵活的开关控制（on/off）
- ✅ 自动在登录前拒绝玩家（避免区块加载问题）
- ✅ 白名单系统（通过玩家名管理，UUID 验证）
- ✅ 白名单玩家可直接进入服务器
- ✅ 自定义 **MOTD** 和踢出消息
- ✅ 自定义版本栏显示（如"维护✖"）
- ✅ 假玩家数显示
- ✅ 状态持久化（重启后保持）

### 📝 配置示例

**config.yml - maintenance 部分**
```yaml
maintenance:
  messages:
    enabled: "&a[维护模式] &7服务器维护模式已启用！"
    disabled: "&c[维护模式] &7服务器维护模式已关闭！"
    no-permission: "&c你没有权限使用此命令！"
  
  # 踢出消息（多行）
  kick-message:
    - "&c&l⚠ 服务器维护中 ⚠"
    - ""
    - "&7服务器正在进行维护升级"
    - "&7请稍后再试！"
  
  # MOTD 显示
  motd:
    - "&c&l⚠ 服务器维护中 ⚠"
    - "&7正在进行维护，请稍后再试"
  
  # 版本栏文本
  version-text: "&c维护中 ✖"
  show-incompatible-version: true
  
  # 假玩家数
  show-fake-players: true
  fake-online: 0
  fake-max: 0
  
  # 悬浮提示
  hover-message:
    - "&c&l⚠ 服务器维护中 ⚠"
```

### 🎮 管理命令

| 命令 | 说明 |
|------|------|
| `/tsl maintenance on` | 开启维护模式 |
| `/tsl maintenance off` | 关闭维护模式 |
| `/tsl maintenance add <玩家名>` | 添加玩家到白名单 |
| `/tsl maintenance remove <玩家名>` | 从白名单移除玩家 |
| `/tsl maintenance whitelist` | 查看白名单列表 |

### 💡 使用示例

```bash
# 开启维护模式
/tsl maintenance on

# 添加管理员到白名单（可以在维护期间进入）
/tsl maintenance add MingDeng
/tsl maintenance add Steve

# 查看白名单
/tsl maintenance whitelist

# 关闭维护模式
/tsl maintenance off

# 移除白名单玩家
/tsl maintenance remove Steve
```

### 🔐 权限说明

| 权限 | 说明 | 默认 |
|------|------|------|
| `tsl.maintenance.manage` | 管理维护模式（所有命令） | OP |

### ⚠️ 工作原理

**白名单系统：**
1. 使用 `/tsl maintenance add <玩家名>` 添加玩家
2. 系统会自动获取玩家的 UUID（支持在线和离线玩家）
3. UUID 和玩家名都会保存到 `maintenance-whitelist.txt`
4. 玩家登录时，通过 UUID 检查是否在白名单中
5. 白名单玩家可以在维护模式下正常进入服务器

**登录拦截：**
- ✅ 使用 `AsyncPlayerPreLoginEvent` 在登录前就拒绝玩家
- ✅ 白名单玩家通过 UUID 快速验证
- ✅ 避免区块加载和实体注册
- ✅ 不会触发 "Player is already removed from player chunk loader" 错误

---

## 3. 成就系统

### ✨ 功能特性

- ✅ 自动隐藏普通成就的公屏消息
- ✅ 保留挑战成就的公屏消息（稀有成就）
- ✅ 玩家个人仍能看到自己的成就通知
- ✅ 提供 PlaceholderAPI 占位符
- ✅ 支持手动刷新统计

### 🎮 使用命令

| 命令 | 说明 |
|------|------|
| `/tsl advcount refresh <player>` | 刷新指定玩家的成就统计 |
| `/tsl advcount refresh all` | 刷新所有玩家的成就统计 |

### 📊 PlaceholderAPI 占位符

| 占位符 | 说明 |
|--------|------|
| `%tsl_adv_count%` | 玩家的成就数量 |

---

## 4. 访客模式

### ✨ 功能特性

- ✅ 拥有 `tsl.visitor` 权限的玩家获得保护
- ✅ 怪物不会主动攻击访客
- ✅ 访客自动获得**发光效果**（便于识别）
- ✅ 自定义提示消息、标题和音效
- ✅ **实时权限检测** - 权限变更立即生效，无需重新登录
- ✅ **双重保障机制** - LuckPerms 事件 + 定期检查

### 📝 配置示例

**config.yml - visitor 部分**
```yaml
visitor:
  gained:
    chat: "&a[访客模式] &7你已进入访客模式，怪物将不会攻击你，并且你会发光！"
    title: "&a访客模式"
    subtitle: "&7已启用"
    sound: "entity.player.levelup"
  lost:
    chat: "&c[访客模式] &7你已退出访客模式，怪物现在可以攻击你了！"
    title: "&c访客模式"
    subtitle: "&7已禁用"
    sound: "block.note_block.bass"
```

### 🎮 使用方法

```bash
# 给予访客权限（玩家在线时立即生效）
/lp user <玩家> permission set tsl.visitor true

# 移除访客权限（玩家在线时立即生效）
/lp user <玩家> permission unset tsl.visitor
```

### 🔄 实时权限检测

**双重保障机制：**

1. **LuckPerms 事件监听**
   - 监听权限变更事件 (`UserDataRecalculateEvent`)
   - 权限变更后 0.5 秒内自动应用/移除访客效果
   - 立即显示提示消息

2. **定期检查任务**
   - 每 30 秒自动检查所有在线玩家的权限
   - 作为保险机制，防止事件监听失效
   - 确保权限状态始终同步

**实时效果：**
```bash
# 场景 1：玩家在线时获得访客权限
/lp user Steve permission set tsl.visitor true

# Steve 立即：
# - 开始发光
# - 怪物不再锁定他
# - 收到提示消息和标题
# - 播放获得音效

# 场景 2：玩家在线时失去访客权限
/lp user Steve permission unset tsl.visitor

# Steve 立即：
# - 停止发光
# - 怪物可以攻击他
# - 收到提示消息和标题
# - 播放失去音效
```

### 💡 工作原理

**权限检测流程：**

1. **玩家加入服务器**
   - 检查是否有 `tsl.visitor` 权限
   - 如果有，立即应用访客效果

2. **玩家在线时权限变更**
   - LuckPerms 触发权限更新事件
   - 插件接收事件，延迟 0.5 秒检查
   - 根据新权限状态应用或移除效果

3. **定期检查（保险机制）**
   - 每 30 秒检查所有在线玩家
   - 同步权限状态和实际效果
   - 修复可能的状态不一致

4. **玩家离开服务器**
   - 自动清理访客状态
   - 移除所有效果

### ⚙️ 访客效果

**启用效果：**
- ✅ 发光效果（Glowing）
- ✅ 怪物不会锁定目标
- ✅ 记录到访客列表

**移除效果：**
- ✅ 停止发光
- ✅ 怪物恢复正常行为
- ✅ 从访客列表移除

---

## 5. 权限检测器

### ✨ 功能特性

- ✅ 基于 PlaceholderAPI 变量自动分配权限组
- ✅ 玩家加入时自动检测
- ✅ 可配置目标权限组和触发条件
- ✅ 可选执行自定义命令

### 📝 配置示例

**config.yml - permission-checker 部分**
```yaml
permission-checker:
  # 是否启用权限检测
  enabled: true
  
  # 目标权限组名称
  target-group: "normal"
  
  # PlaceholderAPI 变量名称
  variable-name: "%player_gamemode%"
  
  # 变量期望值（匹配时执行）
  variable-value: "SURVIVAL"
  
  # 是否执行命令
  execute-command: false
  
  # 执行的命令列表（%player% 会被替换）
  commands:
    - "say 欢迎 %player% 加入服务器！"
    - "give %player% diamond 1"
```

### 💡 使用场景

- 生存模式玩家自动获得 "normal" 权限组
- 创造模式玩家自动获得 "builder" 权限组
- 基于经济余额自动升级 VIP 等级

---

## 6. 农田保护

### ✨ 功能特性

- ✅ 防止玩家踩踏农田
- ✅ 防止生物踩踏农田
- ✅ 无需配置，开箱即用

---

## 7. 假玩家显示

### ✨ 功能特性

- ✅ 可开启/关闭假玩家显示功能
- ✅ 在服务器列表中调整显示的在线人数
- ✅ 显示人数 = 实际在线 + 偏移量
- ✅ 最小显示为 0（不会显示负数）

### 📝 配置示例

**config.yml - fakeplayer 部分**
```yaml
fakeplayer:
  # 是否启用假玩家功能
  enabled: true
  
  # 显示的在线人数 = 实际在线 + count（可为负，最小为 0）
  count: 3
```

### 💡 使用场景

- 服务器想让人数看起来更多：设置 `count: 5`
- 服务器想隐藏部分管理员：设置 `count: -2`
- 临时关闭功能：设置 `enabled: false`

---

## 8. Boss 声音控制

### ✨ 功能特性

- ✅ 自定义**末影龙死亡**声音范围
- ✅ 自定义**凋零生成**声音范围
- ✅ 自定义**末地传送门激活**声音范围
- ✅ 支持全服广播（-1）
- ✅ 支持静音（0）
- ✅ 支持自定义范围（任意正数）
- ✅ 无需配置，开箱即用

### 📝 配置示例

**config.yml - bossvoice 部分**
```yaml
bossvoice:
  # 末影龙死亡声音范围（格子/方块）
  # -1: 全服都能听到（默认行为）
  # 0: 静音（没有声音）
  # >0: 指定范围内的玩家才能听到
  ender-dragon-death: -1.0
  
  # 凋零生成声音范围（格子/方块）
  # -1: 全服都能听到（默认行为）
  # 0: 静音（没有声音）
  # >0: 指定范围内的玩家才能听到
  wither-spawn: -1.0
  
  # 末地传送门激活声音范围（格子/方块）
  # -1: 全服都能听到（默认行为）
  # 0: 静音（没有声音）
  # >0: 指定范围内的玩家才能听到
  end-portal-activate: -1.0
```

### 💡 使用场景

**默认设置（-1）：**
- 所有玩家都能听到 Boss 事件声音
- 保持原版 Minecraft 的默认行为

**静音模式（0）：**
- 完全关闭 Boss 事件声音
- 适合不想被打扰的服务器

**限制范围（如 100.0）：**
```yaml
bossvoice:
  ender-dragon-death: 100.0  # 只有 100 格内的玩家能听到
  wither-spawn: 200.0         # 只有 200 格内的玩家能听到
  end-portal-activate: 150.0  # 只有 150 格内的玩家能听到
```

**推荐配置：**
```yaml
# 大型服务器推荐配置
bossvoice:
  ender-dragon-death: 500.0   # 末影龙死亡比较重要，范围大一点
  wither-spawn: 300.0          # 凋零生成中等范围
  end-portal-activate: 400.0   # 末地传送门激活中等范围
```

### ⚙️ 工作原理

1. **末影龙死亡**：当末影龙被击杀时
   - 如果设置为 -1：所有玩家听到声音（默认）
   - 如果设置为 0：没有声音
   - 如果设置为 100：只有距离末影龙 100 格内的玩家听到

2. **凋零生成**：当凋零被召唤时
   - 如果设置为 -1：所有玩家听到声音（默认）
   - 如果设置为 0：没有声音
   - 如果设置为 200：只有距离凋零 200 格内的玩家听到

3. **末地传送门激活**：当末地传送门被激活时
   - 如果设置为 -1：所有玩家听到声音（默认）
   - 如果设置为 0：没有声音
   - 如果设置为 150：只有距离传送门 150 格内的玩家听到

---

## 🎯 完整命令列表

| 命令 | 说明 | 权限 |
|------|------|------|
| `/tsl` | 显示插件帮助 | - |
| `/tsl reload` | 重新加载配置文件 | OP |
| `/tsl advcount refresh <player\|all>` | 刷新成就统计 | - |
| `/tsl aliasreload` | 重载别名配置 | `tsl.alias.reload` |
| `/tsl maintenance on` | 开启维护模式 | `tsl.maintenance.manage` |
| `/tsl maintenance off` | 关闭维护模式 | `tsl.maintenance.manage` |
| `/tsl maintenance add <玩家名>` | 添加玩家到白名单 | `tsl.maintenance.manage` |
| `/tsl maintenance remove <玩家名>` | 从白名单移除玩家 | `tsl.maintenance.manage` |
| `/tsl maintenance whitelist` | 查看白名单列表 | `tsl.maintenance.manage` |

---

## 🔐 完整权限列表

| 权限 | 说明 | 默认 |
|------|------|------|
| `tsl.visitor` | 访客权限（怪物不攻击、发光） | false |
| `tsl.reload` | 重新加载插件配置 | OP |
| `tsl.alias.reload` | 重载命令别名配置 | OP |
| `tsl.maintenance.manage` | 管理维护模式（所有命令） | OP |

---

## 🔧 技术信息

### 核心特性

- **语言**: Kotlin 1.9.21
- **服务器**: Paper/Folia 1.21.8
- **Folia 兼容**: ✅ 完全支持
- **依赖**:
  - LuckPerms API 5.4
  - PlaceholderAPI 2.11.6

### Folia 兼容性说明

本插件完全兼容 Folia 多线程服务器：

- ✅ 使用实体调度器（Entity Scheduler）
- ✅ 使用区域调度器（Region Scheduler）
- ✅ 避免跨区域实体操作
- ✅ 所有功能都经过 Folia 测试

---

## 📦 安装指南

### 快速安装

1. **下载插件** - 将 `TSLplugins-1.0.jar` 放入 `plugins` 文件夹
2. **重启服务器** - 插件会自动生成配置文件
3. **配置功能** - 编辑 `plugins/TSLplugins/config.yml` 和 `aliases.yml`
4. **重载配置** - 使用 `/tsl aliasreload` 重载别名配置

### 依赖插件

- **必需**: 无
- **可选**:
  - LuckPerms（权限管理）
  - PlaceholderAPI（占位符支持）

---

## ❓ 常见问题

### Q: 别名命令显示红色怎么办？
**A**: 新版本已修复此问题，别名会正确显示为白色。使用 `/tsl aliasreload` 重载配置。

### Q: 维护模式下管理员也进不去？
**A**: 使用 `/tsl maintenance add <管理员名>` 将管理员添加到白名单。白名单玩家可以在维护模式下正常进入。

### Q: 维护模式报错 "Player is already removed"？
**A**: 新版本已修复此问题。现在使用 `AsyncPlayerPreLoginEvent`，在登录前就拒绝玩家。

### Q: 如何查看维护模式白名单？
**A**: 使用 `/tsl maintenance whitelist` 查看当前白名单中的所有玩家。

### Q: PlaceholderAPI 占位符不工作？
**A**: 确保安装了 PlaceholderAPI 插件，插件启动时应显示 "PlaceholderAPI 扩展已注册"。

### Q: 如何临时关闭某个功能？
**A**: 编辑 `config.yml` 中对应功能的 `enabled` 选项（部分功能支持）。

---

## 📝 更新日志

### v1.0 - 2024-11
- ✅ 从 Java 完全迁移到 Kotlin
- ✅ 修复别名命令显示红色的问题
- ✅ 修复维护模式的区块加载错误
- ✅ 添加假玩家显示开关
- ✅ 完整的 Folia 支持
- ✅ 改进维护模式使用白名单系统

---

## 📧 支持

如有问题或建议，请联系服务器管理员。

---

**© 2024 TSL Server | Made with ❤️ in Kotlin**

