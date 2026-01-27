# TSLplugins AI 开发指南

> **版本**: 1.0  
> **更新日期**: 2026-01-23  
> **适用于**: AI 辅助开发（Cursor、Claude 等）

---

## 📋 项目概述

TSLplugins 是一个 Minecraft Paper/Folia 服务器插件，采用模块化架构，包含 45+ 个独立功能模块。

### 技术栈
- **语言**: Kotlin 1.9+
- **构建**: Gradle (Kotlin DSL) + Shadow Plugin
- **框架**: Paper 1.21+ / Folia 兼容
- **数据库**: SQLite (异步)
- **配置**: YAML (config.yml, messages.yml)

### 包结构
```
org.tsl.tSLplugins/
├── core/                    # 核心框架（不要修改）
│   ├── TSLModule.kt         # 模块接口
│   ├── AbstractModule.kt    # 模块基类
│   ├── ModuleRegistry.kt    # 模块注册器
│   └── ModuleContext.kt     # 依赖注入上下文
├── modules/                 # 功能模块（新架构）
│   └── xxx/                 # 每个模块一个目录
│       ├── XxxModule.kt     # 模块入口
│       ├── XxxCommand.kt    # 命令处理（可选）
│       └── XxxListener.kt   # 事件监听（可选）
├── Xxx/                     # 旧架构模块（保持不变）
├── DatabaseManager.kt       # 全局数据库管理
├── MessageManager.kt        # 消息国际化
├── PlayerDataManager.kt     # 玩家数据管理
└── TSLplugins.kt            # 主类
```

---

## 🚀 新增模块（新架构）

### 步骤 1: 创建模块目录

```
src/main/kotlin/org/tsl/tSLplugins/modules/mymodule/
```

### 步骤 2: 创建模块入口

```kotlin
package org.tsl.tSLplugins.modules.mymodule

import org.tsl.tSLplugins.core.AbstractModule
import org.tsl.tSLplugins.SubCommandHandler

class MyModule : AbstractModule() {
    // 模块 ID（用于配置路径和命令名）
    override val id = "mymodule"
    
    // 配置路径（默认与 id 相同）
    override val configPath = "mymodule"
    
    // 模块描述
    override fun getDescription() = "我的模块功能描述"
    
    // 模块依赖（可选）
    override val dependencies = listOf<String>()  // 如 listOf("webbridge")
    
    // 启用时执行
    override fun doEnable() {
        // 注册监听器
        registerListener(MyListener(this))
        
        // 启动定时任务等
    }
    
    // 禁用时执行
    override fun doDisable() {
        // 清理资源
    }
    
    // 重载配置时执行
    override fun doReload() {
        // 重新加载配置
    }
    
    // 返回命令处理器（可选）
    override fun getCommandHandler(): SubCommandHandler? {
        return MyCommand(this)
    }
}
```

### 步骤 3: 创建命令处理器（可选）

```kotlin
package org.tsl.tSLplugins.modules.mymodule

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.tsl.tSLplugins.SubCommandHandler

class MyCommand(private val module: MyModule) : SubCommandHandler {
    
    override fun handle(sender: CommandSender, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("仅玩家可用")
            return true
        }
        
        if (!module.isEnabled()) {
            sender.sendMessage(module.getMessage("disabled"))
            return true
        }
        
        // 处理命令逻辑
        sender.sendMessage(module.getMessage("success", "player" to sender.name))
        return true
    }
    
    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        return emptyList()
    }
    
    override fun getDescription(): String = module.getDescription()
}
```

### 步骤 4: 创建事件监听器（可选）

```kotlin
package org.tsl.tSLplugins.modules.mymodule

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

class MyListener(private val module: MyModule) : Listener {
    
    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        if (!module.isEnabled()) return
        
        val player = event.player
        // 处理逻辑
    }
}
```

### 步骤 5: 注册模块

在 `TSLplugins.kt` 中添加：

```kotlin
// 导入
import org.tsl.tSLplugins.modules.mymodule.MyModule

// 在 moduleRegistry 初始化区域添加
moduleRegistry.register(MyModule())
```

### 步骤 6: 添加配置

在 `config.yml` 中：

```yaml
mymodule:
  enabled: true
  # 其他配置项...
```

### 步骤 7: 添加消息

在 `messages.yml` 中：

```yaml
mymodule:
  disabled: "&c该功能未启用"
  success: "&a操作成功，玩家: {player}"
  # 其他消息...
```

---

## ⚠️ 关键规范

### 1. Folia 线程安全

**必须使用 Folia 兼容的调度器：**

```kotlin
// ❌ 错误 - 不兼容 Folia
Bukkit.getScheduler().runTask(plugin) { ... }

// ✅ 正确 - 全局任务
Bukkit.getGlobalRegionScheduler().run(plugin) { ... }

// ✅ 正确 - 玩家相关任务
player.scheduler.run(plugin, { ... }, null)

// ✅ 正确 - 异步任务
Bukkit.getAsyncScheduler().runNow(plugin) { ... }

// ✅ 正确 - 区域任务
Bukkit.getRegionScheduler().run(plugin, location) { ... }
```

### 2. 线程安全集合

```kotlin
// ❌ 错误 - 非线程安全
private val players = mutableMapOf<UUID, Data>()

// ✅ 正确 - 线程安全
private val players = ConcurrentHashMap<UUID, Data>()
```

### 3. 消息获取

```kotlin
// ❌ 错误 - 直接访问主类
(context.plugin as TSLplugins).messageManager.get("key")

// ✅ 正确 - 使用 AbstractModule 方法
getMessage("key")  // 自动从 messages.yml 的 ${id}.key 获取
getMessage("key", "player" to player.name)  // 带占位符

// ✅ 正确 - 通用消息
getCommonMessage("no-permission")  // 从 messages.yml 根级获取
```

### 4. 配置获取

```kotlin
// ✅ 使用 AbstractModule 便捷方法
val enabled = getConfigBoolean("enabled", false)
val count = getConfigInt("count", 10)
val name = getConfigString("name", "default")
val list = getConfigStringList("items")
```

### 5. 数据库操作

```kotlin
// 异步数据库操作
Bukkit.getAsyncScheduler().runNow(context.plugin) {
    val connection = DatabaseManager.getConnection()
    connection.use { conn ->
        // SQL 操作
    }
}
```

### 6. 权限命名

```
tsl.模块名.操作
```

示例：
- `tsl.freeze.use` - 使用冻结命令
- `tsl.freeze.admin` - 管理员权限
- `tsl.freeze.bypass` - 绕过冻结

---

## 📁 文件命名规范

| 类型 | 命名格式 | 示例 |
|------|----------|------|
| 模块入口 | `XxxModule.kt` | `FreezeModule.kt` |
| 命令处理 | `XxxModuleCommand.kt` 或 `XxxCommand.kt` | `FreezeModuleCommand.kt` |
| 事件监听 | `XxxModuleListener.kt` 或 `XxxListener.kt` | `FreezeModuleListener.kt` |
| 管理器 | `XxxManager.kt` | `FreezeManager.kt` |
| GUI | `XxxGUI.kt` | `LandmarkGUI.kt` |

---

## 🔄 模块生命周期

```
注册 → 检查配置 → onEnable() → doEnable() → [运行中]
                                                  ↓
                                            onReload()
                                                  ↓
                                            doReload()
                                                  ↓
                                            [继续运行]
                                                  ↓
                                            onDisable()
                                                  ↓
                                            doDisable()
                                                  ↓
                                         自动注销监听器
```

### 生命周期方法说明

| 方法 | 调用时机 | 用途 |
|------|----------|------|
| `loadConfig()` | 启用/重载时 | 读取配置项（可重写） |
| `doEnable()` | 模块启用时 | 初始化资源、注册监听器 |
| `doDisable()` | 模块禁用时 | 清理资源 |
| `doReload()` | 配置重载时 | 重新加载配置 |

---

## 🚫 禁止事项

1. **不要直接修改 `core/` 目录下的文件**
2. **不要在新模块中使用 `Bukkit.getScheduler()`**
3. **不要使用非线程安全的集合存储玩家数据**
4. **不要在监听器中直接操作数据库（使用异步）**
5. **不要硬编码消息文本（使用 messages.yml）**
6. **不要在 `doEnable()` 中执行耗时操作（使用异步）**

---

## ✅ 检查清单

新增模块前确认：

- [ ] 模块 ID 唯一且符合命名规范
- [ ] 使用 Folia 兼容的调度器
- [ ] 使用线程安全的集合
- [ ] 消息定义在 messages.yml 中
- [ ] 配置定义在 config.yml 中
- [ ] 权限节点符合命名规范
- [ ] 已在 TSLplugins.kt 中注册模块
- [ ] 编译通过且无警告

---

## 📚 参考示例

推荐参考以下模块作为开发模板：

| 复杂度 | 模块 | 特点 |
|--------|------|------|
| 简单 | `modules/farmprotect/` | 纯监听器，无命令 |
| 中等 | `modules/freeze/` | 命令 + 监听器 + 定时任务 |
| 复杂 | `modules/landmark/` | 命令 + 监听器 + GUI + 数据存储 |

---

## 🔧 常见问题

### Q: 如何访问其他模块？

```kotlin
// 通过 ModuleRegistry 获取（需要在主类暴露）
// 注意：尽量减少模块间依赖
```

### Q: 如何使用 PlaceholderAPI？

```kotlin
if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
    val result = PlaceholderAPI.setPlaceholders(player, "%placeholder%")
}
```

### Q: 如何处理玩家数据持久化？

```kotlin
// 使用 PlayerDataManager（推荐用于简单数据）
context.playerDataManager.setPlayerData(uuid, "key", value)
val value = context.playerDataManager.getPlayerData(uuid, "key")

// 使用 DatabaseManager（推荐用于复杂数据）
// 在异步线程中操作
```

---

> **最后更新**: 2026-01-23 by AI Assistant
