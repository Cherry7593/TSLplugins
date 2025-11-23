# Kiss 亲吻功能开发总结

**日期**: 2025-11-24  
**功能模块**: Kiss（玩家互动亲吻）

---

## 功能概述

Kiss 功能允许玩家之间进行亲吻互动，增加服务器的趣味性和玩家社交体验。

---

## 功能特性

### 1. 亲吻方式
- **命令方式**：`/tsl kiss <玩家名>`
- **快捷方式**：Shift + 右键点击目标玩家

### 2. 核心功能
- ✅ 冷却时间限制（可配置，默认 1 秒）
- ✅ 玩家个人开关（可关闭功能避免被骚扰）
- ✅ 粒子效果（爱心粒子）
- ✅ 音效效果（悦耳的升级音效）
- ✅ 统计功能（记录亲吻次数和被亲吻次数）
- ✅ PlaceholderAPI 支持

### 3. 权限系统
- `tsl.kiss.use` - 使用 /tsl kiss 命令
- `tsl.kiss.use.click` - 使用 Shift + 右键亲吻
- `tsl.kiss.bypass` - 无视冷却时间限制

---

## 文件结构

```
Kiss/
├── KissManager.kt        # 管理器：配置、状态、统计
├── KissCommand.kt        # 命令处理器
├── KissExecutor.kt       # 执行器：粒子、音效、消息
├── KissListener.kt       # 监听器：Shift + 右键交互
└── KissPlaceholder.kt    # PlaceholderAPI 扩��
```

---

## 代码实现

### KissManager.kt
**职责**：
- 加载和管理配置
- 管理玩家功能开关状态
- 管理冷却时间
- 统计亲吻数据

**核心方法**：
```kotlin
fun isEnabled(): Boolean                          // 功能是否启用
fun isPlayerEnabled(uuid: UUID): Boolean          // 玩家是否启用
fun togglePlayer(uuid: UUID): Boolean             // 切换开关
fun isInCooldown(uuid: UUID): Boolean             // 检查冷却
fun setCooldown(uuid: UUID)                       // 设置冷却
fun incrementKissCount(uuid: UUID)                // 增加亲吻次数
fun incrementKissedCount(uuid: UUID)              // 增加被亲吻次数
fun getKissCount(uuid: UUID): Int                 // 获取亲吻次数
fun getKissedCount(uuid: UUID): Int               // 获取被亲吻次数
```

### KissCommand.kt
**职责**：处理 `/tsl kiss` 命令

**命令格式**：
- `/tsl kiss <玩家>` - 亲吻指定玩家
- `/tsl kiss toggle` - 切换个人开关

**检查流程**：
1. 功能是否启用
2. 是否是玩家执行
3. 权限检查
4. 个人开关检查
5. 冷却检查
6. 目标玩家存在检查
7. 不能亲自己检查
8. 目标玩家开关检查

### KissExecutor.kt
**职责**：执行亲吻动作效果

**实现效果**：
```kotlin
fun executeKiss(sender: Player, target: Player)
    ├── 增加统计数据
    ├── 发送消息给双方
    ├── 生成爱心粒子（20 个粒子）
    └── 播放音效（ENTITY_PLAYER_LEVELUP，音调 1.5）
```

### KissListener.kt
**职责**：监听 Shift + 右键玩家交互

**触发条件**：
- 按住 Shift
- 右键点击玩家
- 通过所有检查（功能启用、权限、开关、冷却等）

### KissPlaceholder.kt
**职责**：提供 PlaceholderAPI 变量

**支持的变量**：
- `%tsl_kiss_count%` - 玩家亲吻别人的次数
- `%tsl_kissed_count%` - 玩家被别人亲吻的次数

---

## 配置文件

### config.yml
```yaml
kiss:
  # 是否启用 Kiss 功能
  enabled: true
  
  # 指令冷却时间（秒）
  cooldown: 1.0
  
  # 消息配置
  messages:
    prefix: "&d♥ &r"
    disabled: "%prefix%&cKiss 功能已禁用"
    no_permission: "%prefix%&c你没有权限使用此命令"
    console_only: "%prefix%&c此命令只能由玩家执行"
    player_disabled: "%prefix%&c你的亲吻功能已禁用"
    target_disabled: "%prefix%&c{player} 拒绝被亲吻"
    cooldown: "%prefix%&c请等待 &e{time}秒 &c后再使用"
    player_not_found: "%prefix%&c玩家 &e{player} &c不在线"
    cannot_kiss_self: "%prefix%&c你不能亲自己哦~"
    kiss_sent: "%prefix%&d你亲了 &e{player} &d一口~ ♥"
    kiss_received: "%prefix%&d{player} &d亲了你一口~ ♥"
    toggle_enabled: "%prefix%&a亲吻功能已启用"
    toggle_disabled: "%prefix%&c亲吻功能已禁用"
    usage: |
      %prefix%&e使用方法:
      &7/tsl kiss <玩家> &f- 亲吻玩家
      &7/tsl kiss toggle &f- 切换亲吻功能
      &7Shift + 右键玩家 &f- 亲吻玩家
```

---

## 使用说明

### 玩家命令
```
/tsl kiss <玩家名>  - 亲吻指定玩家
/tsl kiss toggle    - 开启/关闭被亲吻功能
```

### 快捷操作
```
Shift + 右键玩家 → 亲吻该玩家
```

### 管理员命令
```
/tsl reload - 重新加载配置（包括 Kiss 功能）
```

---

## 权限节点

| 权限 | 说明 | 默认 |
|------|------|------|
| `tsl.kiss.use` | 使用 /tsl kiss 命令 | OP |
| `tsl.kiss.use.click` | 使用 Shift + 右键亲吻 | OP |
| `tsl.kiss.bypass` | 无视冷却时间限制 | OP |

---

## 技术细节

### 数据管理
- 使用 `ConcurrentHashMap` 存储玩家状态和统计数据
- 玩家退出时清理冷却数据，保留统计数据
- 统计数据持久化需要额外实现（当前为内存存储）

### 粒子效果
```kotlin
Particle.HEART          // 粒子类型
数量: 20                 // 粒子数量
位置: 玩家头部上方 2 格
偏移: X/Y/Z ± 0.5       // 随机分散
速度: 0.1               // 缓慢飘散
```

### 音效设置
```kotlin
Sound.ENTITY_PLAYER_LEVELUP  // 音效类型
音量: 1.0f                    // 标准音量
音调: 1.5f                    // 提高音调（更甜美）
```

### 冷却机制
- 基于时间戳计算
- OP 和拥有 bypass 权限的玩家无视冷却
- 冷却时间可在配置文件中调整（单位：秒）

---

## 集成到主插件

### TSLplugins.kt 修改
1. ✅ 导入 Kiss 相关类
2. ✅ 添加 `kissManager` 成员变量
3. ✅ 在 `onEnable` 中初始化 Kiss 系统
4. ✅ 注册 Kiss 命令处理器
5. ✅ 注册 KissPlaceholder（如果 PlaceholderAPI 存在）
6. ✅ 添加 `reloadKissManager()` 方法

### ReloadCommand.kt 修改
- ✅ 在重载命令中添加 `plugin.reloadKissManager()`

### ConfigUpdateManager.kt 修改
- ✅ 更新配置版本号到 9

---

## 测试场景

### 场景 1：基本亲吻功能
1. 玩家 A 执行 `/tsl kiss 玩家B`
2. 验证：
   - ✅ 玩家 A 收到消息：你亲了 玩家B 一口~ ♥
   - ✅ 玩家 B 收到消息：玩家A 亲了你一口~ ♥
   - ✅ 玩家 B 头上出现爱心粒子
   - ✅ 播放音效

### 场景 2：Shift + 右键交互
1. 玩家 A 按住 Shift，右键点击玩家 B
2. 验证：
   - ✅ 效果与命令方式相同
   - ✅ 不影响其他 Shift + 右键交互（如 Toss 功能）

### 场景 3：冷却时间
1. 玩家 A 亲吻玩家 B
2. 立即再次执行亲吻命令
3. 验证：
   - ✅ 提示冷却中，显示剩余时间
   - ✅ OP 和 bypass 权限玩家无视冷却

### 场景 4：个人开关
1. 玩家 B 执行 `/tsl kiss toggle` 关闭功能
2. 玩家 A 尝试亲吻玩家 B
3. 验证：
   - ✅ 提示"玩家B 拒绝被亲吻"
   - ✅ 不执行亲吻动作

### 场景 5：边界情况
1. 亲吻自己 → 提示"你不能亲自己哦~"
2. 亲吻离线玩家 → 提示"玩家不在线"
3. 无权限执行 → 提示"你没有权限"

### 场景 6：统计功能
1. 玩家多次亲吻和被亲吻
2. 使用 PlaceholderAPI 变量查看统计
3. 验证：
   - ✅ `%tsl_kiss_count%` 正确显示
   - ✅ `%tsl_kissed_count%` 正确显示

---

## 与其他功能的兼容性

| 功能 | 交互方式 | 冲突检查 | 状态 |
|------|----------|----------|------|
| Toss | Shift + 右键生物 | 目标必须是玩家 | ✅ 无冲突 |
| Ride | 右键生物 | 目标必须是玩家 | ✅ 无冲突 |
| BabyLock | 右键命名幼年生物 | 目标必须是玩家 | ✅ 无冲突 |

---

## 未来扩展

### 可能的功能增强
1. **持久化统计数据**
   - 使用数据库或文件存储
   - 支持排行榜功能

2. **更多互动效果**
   - 可配置粒子类型
   - 可配置音效类型
   - 支持烟花效果

3. **关系系统**
   - 记录玩家之间的互动关系
   - 好感度系统
   - 情侣系统

4. **成就系统**
   - 亲吻次数成就
   - 特殊成就（如亲吻所有在线玩家）

---

## 相关文件

### 代码文件
- `src/main/kotlin/org/tsl/tSLplugins/Kiss/KissManager.kt`
- `src/main/kotlin/org/tsl/tSLplugins/Kiss/KissCommand.kt`
- `src/main/kotlin/org/tsl/tSLplugins/Kiss/KissExecutor.kt`
- `src/main/kotlin/org/tsl/tSLplugins/Kiss/KissListener.kt`
- `src/main/kotlin/org/tsl/tSLplugins/Kiss/KissPlaceholder.kt`
- `src/main/kotlin/org/tsl/tSLplugins/TSLplugins.kt`
- `src/main/kotlin/org/tsl/tSLplugins/ReloadCommand.kt`
- `src/main/kotlin/org/tsl/tSLplugins/ConfigUpdateManager.kt`

### 配置文件
- `src/main/resources/config.yml`

---

## 总结

Kiss 功能已成功实现并集成到 TSLplugins 插件中，包含：

1. ✅ **完整的功能模块**（5 个类文件）
2. ✅ **命令系统**（/tsl kiss）
3. ✅ **交互系统**（Shift + 右键）
4. ✅ **粒子和音效**
5. ✅ **冷却和权限控制**
6. ✅ **个人开关**
7. ✅ **统计系统**
8. ✅ **PlaceholderAPI 支持**
9. ✅ **配置文件**
10. ✅ **主插件集成**

**代码风格**：完全遵循现有模块的架构和风格，保持一致性。

**与新功能.md 的对照**：所有需求均已实现 ✅

