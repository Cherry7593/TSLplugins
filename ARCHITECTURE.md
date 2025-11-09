# TSLplugins - 架构设计文档

## 📐 命令架构设计

### 设计理念：高内聚、低耦合

本插件采用**模块化命令架构**，每个功能模块独立处理自己的命令逻辑。

---

## 🏗️ 架构概览

```
/tsl 命令
├── TSLCommand (命令分发器)
│   ├── 注册子命令处理器
│   ├── 分发命令到对应模块
│   └── 提供统一的帮助信息
│
└── SubCommandHandler (接口)
    ├── Advancement/AdvancementCommandHandler
    │   └── 处理: /tsl advcount
    │
    ├── Alias/AliasCommand
    │   └── 处理: /tsl aliasreload
    │
    └── Maintenance/MaintenanceCommand
        └── 处理: /tsl maintenance
```

---

## 📦 包结构与职责

### 1. 根包 (`org.tsl.tSLplugins`)

**核心组件：**
- `TSLplugins.kt` - 主插件类，负责初始化和注册
- `TSLCommand.kt` - 命令分发器，负责路由子命令
- `SubCommandHandler.kt` - 子命令处理器接口

**职责：**
✅ 插件生命周期管理
✅ 各模块的组装和协调
✅ 命令的统一分发

### 2. Advancement 包

**文件：**
- `AdvancementMessage.kt` - 成就消息监听器
- `AdvancementCount.kt` - 成就统计管理器
- `AdvancementCommandHandler.kt` - 命令处理器 ⭐
- `TSLPlaceholderExpansion.kt` - PlaceholderAPI 扩展

**命令：** `/tsl advcount refresh <player|all>`

**职责：**
✅ 成就消息的隐藏和处理
✅ 成就数量的统计和缓存
✅ 成就刷新命令的处理
✅ PlaceholderAPI 集成

### 3. Maintenance 包

**文件：**
- `MaintenanceManager.kt` - 维护模式管理器
- `MaintenanceCommand.kt` - 命令处理器 ⭐
- `MaintenanceLoginListener.kt` - 登录监听器
- `MaintenanceMotdListener.kt` - MOTD 监听器

**命令：** `/tsl maintenance`

**职责：**
✅ 维护模式状态管理
✅ 维护模式切换命令处理
✅ 玩家登录拦截
✅ 服务器列表信息自定义

### 4. Alias 包

**文件：**
- `AliasManager.kt` - 别名管理器
- `AliasCommand.kt` - 命令处理器 ⭐
- `DynamicAliasCommand.kt` - 动态别名命令

**命令：** `/tsl aliasreload`

**职责：**
✅ 别名配置的加载和管理
✅ 动态注册别名命令
✅ 别名重载命令处理

### 5. 其他功能包

**Visitor/** - 访客模式
**Permission/** - 权限检测
**Farmprotect/** - 农田保护
**Motd/** - MOTD 假玩家

**特点：** 这些模块不需要命令处理，只有事件监听器

---

## 🔌 接口设计

### SubCommandHandler 接口

```kotlin
interface SubCommandHandler {
    // 处理命令
    fun handle(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean
    
    // Tab 补全（可选）
    fun tabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): List<String> = emptyList()
    
    // 命令描述
    fun getDescription(): String
}
```

**优点：**
1. ✅ 统一接口，易于扩展
2. ✅ 每个模块独立实现
3. ✅ 支持 Tab 补全
4. ✅ 自动生成帮助信息

---

## 🔄 命令处理流程

### 执行流程

```
玩家输入: /tsl maintenance
    ↓
1. Bukkit 调用 TSLCommand.onCommand()
    ↓
2. TSLCommand 解析第一个参数 "maintenance"
    ↓
3. 查找注册的 MaintenanceCommand 处理器
    ↓
4. 调用 MaintenanceCommand.handle()
    ↓
5. 处理业务逻辑并返回结果
```

### Tab 补全流程

```
玩家按 Tab: /tsl [Tab]
    ↓
1. Bukkit 调用 TSLCommand.onTabComplete()
    ↓
2. 返回所有已注册的子命令列表
    ["advcount", "aliasreload", "maintenance"]
    ↓
3. 客户端显示补全选项
```

---

## ✨ 架构优势

### 1. 高内聚
每个功能包完整包含：
- 业务逻辑
- 事件监听
- 命令处理
- 配置管理

### 2. 低耦合
- 各模块之间无直接依赖
- 通过接口统一交互
- 便于独立测试和维护

### 3. 易扩展
添加新功能只需：
1. 创建新包（如 `NewFeature/`）
2. 实现 `SubCommandHandler` 接口
3. 在主类中注册：
   ```kotlin
   dispatcher.registerSubCommand("newfeature", NewFeatureCommand())
   ```

### 4. 易维护
- 修改一个功能不影响其他功能
- 代码组织清晰，易于定位
- 符合单一职责原则

### 5. 自动化
- 自动生成帮助信息
- 自动处理 Tab 补全
- 统一的错误处理

---

## 📝 添加新功能示例

### 场景：添加一个 "stats" 统计功能

#### 1. 创建包结构
```
src/main/kotlin/org/tsl/tSLplugins/Stats/
├── StatsManager.kt         # 统计管理器
├── StatsCommand.kt         # 命令处理器
└── StatsListener.kt        # 事件监听器
```

#### 2. 实现命令处理器
```kotlin
package org.tsl.tSLplugins.Stats

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.tsl.tSLplugins.SubCommandHandler

class StatsCommand(private val manager: StatsManager) : SubCommandHandler {
    
    override fun handle(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        // 处理 /tsl stats 命令
        sender.sendMessage("统计信息...")
        return true
    }
    
    override fun getDescription(): String {
        return "查看服务器统计信息"
    }
}
```

#### 3. 注册到主类
```kotlin
// 在 TSLplugins.onEnable() 中
val statsManager = StatsManager(this)
dispatcher.registerSubCommand("stats", StatsCommand(statsManager))
```

**完成！** 新功能已集成，无需修改其他代码。

---

## 🔍 对比旧架构

### 旧架构（问题）
```
AdvancementCommand
├── 处理成就命令
├── 处理别名命令  ❌ 职责不明确
└── 处理维护命令  ❌ 职责不明确
```

**问题：**
- ❌ 违反单一职责原则
- ❌ 模块之间紧耦合
- ❌ 难以维护和扩展
- ❌ 命令逻辑混乱

### 新架构（优势）
```
TSLCommand (分发器)
├── Advancement/AdvancementCommandHandler  ✅ 职责明确
├── Alias/AliasCommand                    ✅ 职责明确
└── Maintenance/MaintenanceCommand        ✅ 职责明确
```

**优势：**
- ✅ 每个包处理自己的命令
- ✅ 模块独立，易于测试
- ✅ 易于添加新功能
- ✅ 代码清晰易懂

---

## 📊 架构对比表

| 特性 | 旧架构 | 新架构 |
|------|--------|--------|
| 单一职责 | ❌ 混乱 | ✅ 清晰 |
| 模块独立性 | ❌ 耦合 | ✅ 独立 |
| 易维护性 | ❌ 困难 | ✅ 简单 |
| 易扩展性 | ❌ 修改现有代码 | ✅ 只需添加新代码 |
| 代码组织 | ❌ 分散 | ✅ 集中在模块内 |
| 测试友好 | ❌ 难以隔离 | ✅ 易于单元测试 |

---

## 🎯 最佳实践

### 1. 功能模块化
将相关功能组织在同一个包内：
```
Feature/
├── FeatureManager.kt      # 业务逻辑
├── FeatureCommand.kt      # 命令处理
├── FeatureListener.kt     # 事件监听
└── FeatureConfig.kt       # 配置管理（可选）
```

### 2. 接口优先
使用 `SubCommandHandler` 接口统一命令处理：
- 降低耦合
- 便于测试
- 易于扩展

### 3. 职责单一
每个类只负责一件事：
- `Manager` - 业务逻辑
- `Command` - 命令处理
- `Listener` - 事件监听

### 4. 依赖注入
通过构造函数传递依赖：
```kotlin
class FeatureCommand(
    private val plugin: JavaPlugin,
    private val manager: FeatureManager
) : SubCommandHandler
```

---

## 📚 总结

**新架构的核心理念：**
> 每个功能包是一个独立的模块，完整包含该功能的所有代码（业务逻辑、命令处理、事件监听）。模块之间通过接口交互，保持低耦合。

**这种架构让插件：**
- 更易于理解和维护
- 更容易添加新功能
- 更方便团队协作开发
- 更符合软件工程最佳实践

---

**推荐阅读：**
- 单一职责原则 (Single Responsibility Principle)
- 开闭原则 (Open-Closed Principle)
- 依赖倒置原则 (Dependency Inversion Principle)

