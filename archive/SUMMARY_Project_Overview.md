# TSLplugins 项目全面解析

**生成日期**: 2025-11-25  
**文档类型**: 项目架构和技术分析

---

## 📋 目录

1. [项目简介](#项目简介)
2. [技术栈分析](#技术栈分析)
3. [架构设计](#架构设计)
4. [核心系统](#核心系统)
5. [功能模块详解](#功能模块详解)
6. [设计模式和最佳实践](#设计模式和最佳实践)
7. [性能优化策略](#性能优化策略)
8. [开发流程](#开发流程)
9. [项目特色](#项目特色)

---

## 项目简介

### 基本信息

**TSLplugins** 是一个为 Minecraft Folia 1.21.8 服务端设计的多功能整合插件，使用 Kotlin 语言开发。

- **项目名称**: TSLplugins
- **版本**: 1.0
- **开发语言**: Kotlin 1.9.21
- **目标平台**: Paper/Folia 1.21.8
- **Java 版本**: 21
- **构建工具**: Gradle 8.5 + Kotlin DSL
- **配置版本**: 10

### 项目定位

这是一个**模块化的多功能整合插件**，集成了 **13 个独立功能模块**，涵盖：
- 🔧 **服务器管理工具**（维护模式、玩家冻结、命令别名）
- 🎮 **玩家互动功能**（亲吻、帽子、体型调整、延迟查询）
- 🐾 **生物互动系统**（举起、骑乘、永久幼年）
- 🛡️ **保护功能**（农田保护、访客保护）
- ⚙️ **系统增强**（成就过滤、MOTD假玩家）

### 设计理念

1. **模块化** - 每个功能包独立，互不依赖
2. **即插即用** - 可通过配置单独启用/禁用模块
3. **事件驱动** - 基于 Bukkit/Folia 事件系统，无轮询任务
4. **配置驱动** - 所有行为可通过配置文件定制
5. **配置缓存** - 启动时加载配置到内存，事件处理零开销

---

## 技术栈分析

### 核心技术

#### 1. Kotlin 语言特性
```kotlin
// 数据类
data class PlayerInfo(val uuid: UUID, val name: String)

// 属性访问（而非 Java 的 getter/setter）
val name = player.name
val health = player.health

// 字符串模板
val message = "玩家 $name 的延迟是 ${player.ping}ms"

// Lambda 表达式和集合操作
players.filter { it.health > 10 }
       .map { it.name }
       .forEach { println(it) }

// 空安全
val name = player?.name ?: "Unknown"
```

#### 2. Folia 多线程支持

Folia 是 Paper 的多线程版本，需要使用新的调度器 API：

```kotlin
// ✅ 实体操作使用实体调度器
player.scheduler.run(plugin, { _ ->
    player.inventory.setHelmet(item)
}, null)

// ✅ 全局定时任务
Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, { _ ->
    // 全局任务
}, 20L, 20L)

// ❌ 不再支持传统调度器
Bukkit.getScheduler().runTask(plugin, Runnable { }) // 会报错
```

### 依赖管理

#### 必需依赖
```kotlin
compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
```

#### 可选依赖
```kotlin
compileOnly("net.luckperms:api:5.4")          // 权限组管理（访客保护）
compileOnly("me.clip:placeholderapi:2.11.6")  // 变量系统（Kiss 统计）
```

#### 构建插件
```kotlin
// Shadow 插件：打包 Kotlin 标准库到 JAR
id("com.github.johnrengelman.shadow") version "8.1.1"
```

---

## 架构设计

### 项目结构

```
TSLplugins/
│
├── TSLplugins.kt              # 主插件类：初始化所有模块
├── TSLCommand.kt              # 命令分发器：统一入口 /tsl
├── ReloadCommand.kt           # 重载命令：重载所有模块配置
├── ConfigUpdateManager.kt     # 配置更新：版本控制和自动更新
├── PlayerDataManager.kt       # PDC 管理器：数据持久化
│
├── Alias/                     # 命令别名系统
├── Maintenance/               # 维护模式
├── Scale/                     # 体型调整
├── Hat/                       # 帽子系统
├── Ping/                      # 延迟查询
├── Toss/                      # 生物举起
├── Ride/                      # 生物骑乘
├── BabyLock/                  # 永久幼年
├── Kiss/                      # 玩家亲吻
├── Freeze/                    # 玩家冻结
├── Advancement/               # 成就过滤
├── Visitor/                   # 访客保护
├── Permission/                # 权限检测
├── Farmprotect/               # 农田保护
└── Motd/                      # MOTD 假玩家
```

### Manager-Command-Listener 模式

这是插件的**核心架构模式**，每个功能模块都遵循此结构：

```
Module/
├── ModuleManager.kt    # 管理器：配置管理、状态管理
├── ModuleCommand.kt    # 命令处理器：命令处理、权限检查
└── ModuleListener.kt   # 事件监听器：业务逻辑实现
```

#### Manager（管理器）职责
- ✅ 加载和缓存配置
- ✅ 管理模块启用/禁用状态
- ✅ 提供数据访问方法
- ✅ 管理冷却时间、统计数据等

```kotlin
class FeatureManager(private val plugin: JavaPlugin) {
    // 配置缓存
    private var enabled: Boolean = true
    private var cooldown: Long = 1000
    private val blacklist: MutableSet<EntityType> = mutableSetOf()
    
    init {
        loadConfig()
    }
    
    fun loadConfig() {
        // 从配置文件读取到缓存
        enabled = plugin.config.getBoolean("feature.enabled", true)
        cooldown = plugin.config.getLong("feature.cooldown", 1000)
        // ...
    }
    
    // 提供快速访问方法
    fun isEnabled(): Boolean = enabled
    fun getCooldown(): Long = cooldown
}
```

#### Command（命令处理器）职责
- ✅ 实现 `SubCommandHandler` 接口
- ✅ 处理用户命令
- ✅ 权限检查
- ✅ 提供 Tab 补全

```kotlin
class FeatureCommand(
    private val manager: FeatureManager
) : SubCommandHandler {
    
    override fun handle(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        // 1. 功能检查
        if (!manager.isEnabled()) {
            sender.sendMessage("功能已禁用")
            return true
        }
        
        // 2. 玩家检查
        if (sender !is Player) {
            sender.sendMessage("仅玩家可用")
            return true
        }
        
        // 3. 权限检查
        if (!sender.hasPermission("tsl.feature.use")) {
            sender.sendMessage("无权限")
            return true
        }
        
        // 4. 业务逻辑
        // ...
        
        return true
    }
    
    override fun tabComplete(...): List<String> {
        // Tab 补全逻辑
    }
}
```

#### Listener（监听器）职责
- ✅ 监听 Bukkit 事件
- ✅ 实现核心业务逻辑
- ✅ 调用 Manager 方法

```kotlin
class FeatureListener(
    private val plugin: JavaPlugin,
    private val manager: FeatureManager
) : Listener {
    
    @EventHandler(priority = EventPriority.NORMAL)
    fun onEvent(event: SomeEvent) {
        // 快速失败优化
        if (!manager.isEnabled()) return
        if (!player.hasPermission("tsl.feature.use")) return
        
        // 业务逻辑
        // ...
    }
}
```

---

## 核心系统

### 1. 配置缓存系统

**这是插件最重要的性能优化策略。**

#### 为什么需要配置缓存？

| 方式 | 优点 | 缺点 |
|------|------|------|
| 直接读取 config | 实时最新 | 每次事件都有 I/O 开销，性能极差 |
| **配置缓存** | **零开销，性能最佳** | 修改配置后需手动重载 |

#### 实现方式

```kotlin
class FeatureManager(private val plugin: JavaPlugin) {
    // ✅ 缓存配置值
    private var enabled: Boolean = true
    private var maxValue: Int = 10
    private val blacklist: MutableSet<EntityType> = mutableSetOf()
    
    init {
        loadConfig()  // 启动时加载一次
    }
    
    fun loadConfig() {
        val config = plugin.config
        // 从配置文件读取到内存
        enabled = config.getBoolean("feature.enabled", true)
        maxValue = config.getInt("feature.max_value", 10)
        
        blacklist.clear()
        config.getStringList("feature.blacklist").forEach { name ->
            try {
                blacklist.add(EntityType.valueOf(name.uppercase()))
            } catch (e: IllegalArgumentException) {
                plugin.logger.warning("无效的实体类型: $name")
            }
        }
    }
    
    // ✅ 直接读取缓存（零开销）
    fun isEnabled(): Boolean = enabled
    fun getMaxValue(): Int = maxValue
    fun isBlacklisted(type: EntityType): Boolean = blacklist.contains(type)
}
```

#### Listener 使用缓存

```kotlin
@EventHandler
fun onEvent(event: SomeEvent) {
    // ✅ 读取缓存，没有任何 I/O 开销
    if (!manager.isEnabled()) return
    if (manager.isBlacklisted(entity.type)) return
    
    // 业务逻辑
}
```

#### 重载机制

```kotlin
// ReloadCommand.kt
override fun handle(...): Boolean {
    // 重载所有模块的配置
    plugin.reloadConfig()
    plugin.reloadKissManager()
    plugin.reloadFreezeManager()
    plugin.reloadTossManager()
    // ...
}

// TSLplugins.kt
fun reloadKissManager() {
    kissManager.loadConfig()  // 刷新缓存
}
```

### 2. PDC 数据持久化系统

**PersistentDataContainer** 是 Bukkit API 提供的持久化存储机制，数据保存在玩家的 `.dat` 文件中。

#### 为什么使用 PDC？

| 存储方式 | 优点 | 缺点 |
|----------|------|------|
| HashMap | 快速访问 | 离线丢失、服务器重启丢失 |
| 配置文件 | 持久化 | I/O 开销大、管理复杂 |
| 数据库 | 功能强大 | 配置复杂、依赖外部服务 |
| **PDC** | **持久化、零配置、性能好** | 只能存储简单数据类型 |

#### PlayerDataManager 统一管理

```kotlin
class PlayerDataManager(private val plugin: JavaPlugin) {
    
    // PDC Keys（统一管理避免冲突）
    private val kissToggleKey = NamespacedKey(plugin, "kiss_toggle")
    private val rideToggleKey = NamespacedKey(plugin, "ride_toggle")
    private val tossToggleKey = NamespacedKey(plugin, "toss_toggle")
    private val tossVelocityKey = NamespacedKey(plugin, "toss_velocity")
    
    // ==================== Kiss 功能 ====================
    
    fun getKissToggle(player: Player, defaultValue: Boolean = true): Boolean {
        val pdc = player.persistentDataContainer
        return if (pdc.has(kissToggleKey, PersistentDataType.BOOLEAN)) {
            pdc.get(kissToggleKey, PersistentDataType.BOOLEAN) ?: defaultValue
        } else {
            defaultValue
        }
    }
    
    fun setKissToggle(player: Player, enabled: Boolean) {
        player.persistentDataContainer.set(
            kissToggleKey,
            PersistentDataType.BOOLEAN,
            enabled
        )
    }
    
    // 其他功能的数据存取方法...
}
```

#### Manager 使用 PDC

```kotlin
class KissManager(
    private val plugin: JavaPlugin,
    private val dataManager: PlayerDataManager  // 注入
) {
    // 从 PDC 读取玩家配置
    fun isPlayerEnabled(player: Player): Boolean {
        return dataManager.getKissToggle(player, true)
    }
    
    // 写入 PDC
    fun togglePlayer(player: Player): Boolean {
        val current = isPlayerEnabled(player)
        val newStatus = !current
        dataManager.setKissToggle(player, newStatus)
        return newStatus
    }
}
```

#### 存储的数据

| 模块 | 存储内容 | PDC Key | 数据类型 |
|------|----------|---------|----------|
| Kiss | 功能开关 | `tsl:kiss_toggle` | Boolean |
| Ride | 功能开关 | `tsl:ride_toggle` | Boolean |
| Toss | 功能开关 | `tsl:toss_toggle` | Boolean |
| Toss | 投掷速度 | `tsl:toss_velocity` | Double |

### 3. 配置自动更新系统

`ConfigUpdateManager` 实现了智能的配置版本控制和自动更新。

#### 核心功能

```kotlin
class ConfigUpdateManager(private val plugin: JavaPlugin) {
    
    companion object {
        const val CURRENT_CONFIG_VERSION = 10  // 当前版本
    }
    
    fun checkAndUpdate(): Boolean {
        val configFile = File(plugin.dataFolder, "config.yml")
        val currentConfig = YamlConfiguration.loadConfiguration(configFile)
        val currentVersion = currentConfig.getInt("config-version", 0)
        
        // 版本一致，无需更新
        if (currentVersion == CURRENT_CONFIG_VERSION) {
            return false
        }
        
        // 需要更新
        // 1. 备份旧配置到 config.yml.backup
        // 2. 读取默认配置的原始文本（保留注释）
        // 3. 读取用户配置的值
        // 4. 合并：使用默认配置的格式和注释，填入用户的值
        // 5. 保存更新后的配置
        
        return true
    }
}
```

#### 更新流程

1. **开发者添加新配置项**
   - 修改 `src/main/resources/config.yml`
   - 递增 `CURRENT_CONFIG_VERSION`

2. **插件启动时自动检测**
   - 比对配置版本
   - 发现版本不一致时自动更新

3. **智能合并**
   - ✅ 保留所有注释和格式
   - ✅ 保留用户的配置值
   - ✅ 添加新的配置项（使用默认值）
   - ✅ 备份旧配置文件

4. **用户体验**
   - 无需手动编辑配置
   - 无需删除旧配置
   - 自动平滑升级

### 4. 命令系统

#### 统一命令入口

所有命令都通过 `/tsl` 进入：

```kotlin
class TSLCommand(private val plugin: TSLplugins) : CommandExecutor, TabCompleter {
    
    private val subCommands = mutableMapOf<String, SubCommandHandler>()
    
    fun registerSubCommand(name: String, handler: SubCommandHandler) {
        subCommands[name.lowercase()] = handler
    }
    
    override fun onCommand(...): Boolean {
        if (args.isEmpty()) {
            showHelp(sender)
            return true
        }
        
        val subCommand = args[0].lowercase()
        val handler = subCommands[subCommand]
        
        if (handler != null) {
            return handler.handle(sender, command, label, args.drop(1).toTypedArray())
        } else {
            sender.sendMessage("未知命令")
            return true
        }
    }
    
    override fun onTabComplete(...): List<String> {
        // Tab 补全逻辑
    }
}
```

#### SubCommandHandler 接口

```kotlin
interface SubCommandHandler {
    fun handle(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean
    
    fun tabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): List<String>
    
    fun getDescription(): String = ""
}
```

---

## 功能模块详解

### 管理工具类

#### 1. 维护模式（Maintenance）

**功能**: 服务器维护期间阻止玩家登录，支持白名单管理。

**模块结构**:
```
Maintenance/
├── MaintenanceManager.kt              # 配置和白名单管理
├── MaintenanceCommand.kt              # toggle/add/remove/list 命令
├── MaintenanceLoginListener.kt        # 阻止非白名单玩家登录
├── MaintenanceMotdListener.kt         # 修改 MOTD 显示维护状态
└── MaintenancePermissionListener.kt   # 权限检查
```

**核心功能**:
- ✅ 开关维护模式
- ✅ 白名单管理（UUID 验证）
- ✅ 自定义踢出消息
- ✅ 自定义维护 MOTD
- ✅ Bypass 权限支持

**数据存储**: `maintenance.yml`
```yaml
enabled: false
whitelist:
  550e8400-e29b-41d4-a716-446655440000: "PlayerName"
```

#### 2. 玩家冻结（Freeze）

**功能**: 冻结玩家所有操作，用于管理问题玩家。

**模块结构**:
```
Freeze/
├── FreezeManager.kt    # 冻结状态管理、自动过期检查
├── FreezeCommand.kt    # freeze/unfreeze/list 命令
└── FreezeListener.kt   # 阻止各种操作、ActionBar 提示
```

**核心功能**:
- ✅ 永久冻结或定时冻结
- ✅ 冻结所有操作（移动、交互、指令等）
- ✅ ActionBar 实时显示剩余时间
- ✅ 自动过期解冻
- ✅ Bypass 权限支持

**冻结限制**:
- ❌ 移动（位置移动，视角可转动）
- ❌ 破坏/放置方块
- ❌ 与方块/实体交互
- ❌ 使用指令
- ❌ 丢弃/捡起物品
- ❌ 切换手持物品

**技术亮点**:
```kotlin
// 自动过期检查（全局定时任务）
Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, { _ ->
    val now = System.currentTimeMillis()
    val expired = mutableListOf<UUID>()
    
    frozenPlayers.forEach { (uuid, expireTime) ->
        if (expireTime > 0 && now > expireTime) {
            expired.add(uuid)
        }
    }
    
    expired.forEach { uuid ->
        frozenPlayers.remove(uuid)
        // 通知玩家
    }
}, 20L, 20L)  // 每秒检查一次
```

#### 3. 命令别名（Alias）

**功能**: 为现有命令创建自定义别名，支持中文。

**模块结构**:
```
Alias/
├── AliasManager.kt          # 加载 aliases.yml
├── AliasCommand.kt          # aliasreload 命令
└── DynamicAliasCommand.kt   # 动态注册别名命令
```

**核心功能**:
- ✅ 自定义别名（支持中文）
- ✅ 完整 Tab 补全
- ✅ 热重载
- ✅ 动态注册命令

**配置示例**:
```yaml
aliases:
  - "t:tpa"
  - "传送:tpa"
  - "gms:gamemode survival"
```

### 玩家互动类

#### 4. 玩家亲吻（Kiss）

**功能**: 玩家之间的互动功能，带粒子效果和音效。

**模块结构**:
```
Kiss/
├── KissManager.kt       # 配置、冷却、统计
├── KissCommand.kt       # kiss <玩家> 命令
├── KissExecutor.kt      # 执行亲吻效果
├── KissListener.kt      # Shift+右键触发
└── KissPlaceholder.kt   # PAPI 变量
```

**核心功能**:
- ✅ `/tsl kiss <玩家>` - 命令亲吻
- ✅ Shift + 右键 - 互动亲吻
- ✅ 个人开关防骚扰（PDC 存储）
- ✅ 冷却时间
- ✅ 统计（亲吻次数、被亲吻次数）
- ✅ PlaceholderAPI 支持

**效果**:
- ❤️ 爱心粒子特效
- 🔊 音效（ENTITY_PLAYER_LEVELUP）
- 📢 消息提示

**PAPI 变量**:
- `%tsl_kiss_count%` - 亲吻次数
- `%tsl_kissed_count%` - 被亲吻次数

**技术亮点**:
```kotlin
// PDC 存储个人开关
fun isPlayerEnabled(player: Player): Boolean {
    return dataManager.getKissToggle(player, true)
}

// 冷却管理
private val playerCooldowns: MutableMap<UUID, Long> = ConcurrentHashMap()

fun isInCooldown(uuid: UUID): Boolean {
    val lastUsed = playerCooldowns[uuid] ?: return false
    return System.currentTimeMillis() - lastUsed < cooldown
}
```

#### 5. 帽子系统（Hat）

**功能**: 将手持物品戴在头上。

**模块结构**:
```
Hat/
├── HatManager.kt    # 配置管理
└── HatCommand.kt    # hat 命令
```

**核心功能**:
- ✅ 将任意物品戴头上
- ✅ 自动处理堆叠物品（只戴 1 个）
- ✅ 背包满时自动掉落原帽子
- ✅ 黑名单配置

**技术亮点**:
```kotlin
// 使用 Folia 实体调度器
player.scheduler.run(plugin, { _ ->
    val helmet = player.inventory.helmet
    val handItem = player.inventory.itemInMainHand
    
    // 处理堆叠物品
    if (handItem.amount > 1) {
        val single = handItem.clone()
        single.amount = 1
        player.inventory.setHelmet(single)
        handItem.amount -= 1
    } else {
        player.inventory.setHelmet(handItem)
        player.inventory.setItemInMainHand(null)
    }
    
    // 返还原帽子
    if (helmet != null && helmet.type != Material.AIR) {
        if (player.inventory.firstEmpty() == -1) {
            player.world.dropItem(player.location, helmet)
        } else {
            player.inventory.addItem(helmet)
        }
    }
}, null)
```

#### 6. 体型调整（Scale）

**功能**: 调整玩家体型大小。

**模块结构**:
```
Scale/
├── ScaleManager.kt    # 配置、范围限制
└── ScaleCommand.kt    # scale <数值> 命令
```

**核心功能**:
- ✅ 自定义体型大小
- ✅ 范围限制（0.1 - 5.0）
- ✅ 支持小数精度

**技术实现**:
```kotlin
player.setAttribute(Attribute.GENERIC_SCALE, scale)
```

#### 7. 延迟查询（Ping）

**功能**: 查询玩家延迟，支持排行榜。

**模块结构**:
```
Ping/
├── PingManager.kt     # 配置管理
├── PingCommand.kt     # ping [玩家|all] 命令
└── PingPaginator.kt   # 分页显示
```

**核心功能**:
- ✅ `/tsl ping` - 查询自己
- ✅ `/tsl ping <玩家>` - 查询他人
- ✅ `/tsl ping all` - 延迟排行榜
- ✅ 颜色分级显示
- ✅ 可点击翻页

**颜色分级**:
- 0-50ms: 绿色（优秀）
- 51-100ms: 黄色（良好）
- 101-200ms: 金色（一般）
- 201-500ms: 红色（较差）
- 500+ms: 深红色（很差）

### 生物互动类

#### 8. 生物举起（Toss）

**功能**: 举起和投掷生物。

**模块结构**:
```
Toss/
├── TossManager.kt     # 配置、叠罗汉管理
├── TossCommand.kt     # toss toggle/velocity 命令
└── TossListener.kt    # Shift+右键举起、左键投掷
```

**核心功能**:
- ✅ Shift + 右键 - 举起生物
- ✅ 左键 - 投掷生物
- ✅ 叠罗汉（最多 3 个）
- ✅ 可调投掷速度（PDC 存储）
- ✅ 个人开关（PDC 存储）
- ✅ 黑名单配置

**技术亮点**:
```kotlin
// 投掷逻辑
val direction = player.location.direction
val throwVelocity = direction.multiply(velocity)
throwVelocity.setY(throwVelocity.y + 0.3)  // ⚠️ Kotlin 特性：必须用 setY
passenger.setVelocity(throwVelocity)
```

#### 9. 生物骑乘（Ride）

**功能**: 右键骑乘任意生物。

**模块结构**:
```
Ride/
├── RideManager.kt     # 配置、黑名单
├── RideCommand.kt     # ride toggle 命令
└── RideListener.kt    # 右键触发
```

**核心功能**:
- ✅ 右键生物骑乘
- ✅ 个人开关（PDC 存储）
- ✅ 黑名单配置

#### 10. 永久幼年（BabyLock）

**功能**: 给幼年生物命名特殊前缀，锁定幼年状态。

**模块结构**:
```
BabyLock/
├── BabyLockManager.kt    # 配置、前缀检测
└── BabyLockListener.kt   # 成长事件、命名事件
```

**核心功能**:
- ✅ 命名 `[幼]`、`[小]`、`[Baby]` 前缀锁定
- ✅ 阻止成长
- ✅ 取消命名时解锁

**技术实现**:
```kotlin
@EventHandler
fun onEntityBreed(event: EntityBreedEvent) {
    val child = event.entity
    if (child is Ageable) {
        child.scheduler.runDelayed(plugin, { _ ->
            val name = child.customName()
            if (name != null && manager.hasBabyPrefix(name)) {
                child.setAge(Int.MIN_VALUE)  // 锁定幼年
                child.ageLock = true
            }
        }, null, 1L)
    }
}
```

### 保护功能类

#### 11. 农田保护（FarmProtect）

**功能**: 防止玩家/生物踩踏农田。

**技术实现**:
```kotlin
@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
fun onPlayerInteract(event: PlayerInteractEvent) {
    if (event.action == Action.PHYSICAL) {
        val block = event.clickedBlock ?: return
        if (block.type == Material.FARMLAND) {
            event.isCancelled = true
        }
    }
}
```

#### 12. 访客保护（Visitor）

**功能**: 拥有特定权限的玩家获得保护和发光效果。

**核心功能**:
- ✅ 怪物不会攻击访客
- ✅ 访客发光效果
- ✅ 获得/失去权限时提示
- ✅ 依赖 LuckPerms

### 系统增强类

#### 13. 成就过滤（Advancement）

**功能**: 过滤成就消息、统计成就、PlaceholderAPI 支持。

**模块结构**:
```
Advancement/
├── AdvancementCount.kt     # 统计成就数量
├── AdvancementMessage.kt   # 过滤成就消息
└── AdvancementCommand.kt   # 命令
```

**核心功能**:
- ✅ 关闭/开启成就消息
- ✅ 统计成就数量
- ✅ PAPI 变量

#### 14. MOTD 假玩家（Motd）

**功能**: 修改服务器列表显示的在线人数。

**技术实现**:
```kotlin
@EventHandler
fun onServerListPing(event: ServerListPingEvent) {
    val realPlayers = Bukkit.getOnlinePlayers().size
    val fakeCount = plugin.config.getInt("fakeplayer.count", 0)
    val displayPlayers = (realPlayers + fakeCount).coerceAtLeast(0)
    
    event.numPlayers = displayPlayers
}
```

---

## 设计模式和最佳实践

### 1. 依赖注入

```kotlin
class KissManager(
    private val plugin: JavaPlugin,
    private val dataManager: PlayerDataManager  // 注入
) {
    // ...
}

// 主类中
override fun onEnable() {
    playerDataManager = PlayerDataManager(this)
    kissManager = KissManager(this, playerDataManager)
}
```

### 2. 快速失败优化

```kotlin
@EventHandler
fun onEvent(event: SomeEvent) {
    // ✅ 先检查简单条件（越快越好）
    if (!manager.isEnabled()) return        // 最快：布尔值
    if (player.isSneaking) return          // 次快：对象属性
    if (!player.hasPermission("...")) return  // 较快：权限检查
    
    // 最后检查复杂条件
    if (manager.isBlacklisted(entity.type)) return  // 慢：集合查找
    
    // 业务逻辑
}
```

### 3. 并发安全

```kotlin
// 使用 ConcurrentHashMap
private val playerCooldowns: MutableMap<UUID, Long> = ConcurrentHashMap()

// 使用 CopyOnWriteArrayList（读多写少）
private val listeners: MutableList<Listener> = CopyOnWriteArrayList()
```

### 4. 资源清理

```kotlin
@EventHandler
fun onPlayerQuit(event: PlayerQuitEvent) {
    val uuid = event.player.uniqueId
    
    // 清理内存数据
    playerCooldowns.remove(uuid)
    
    // ⚠️ PDC 数据不需要清理（自动持久化）
}
```

### 5. 空安全

```kotlin
// ✅ 使用安全调用
val name = player?.name ?: "Unknown"

// ✅ 使用 let
player?.let {
    it.sendMessage("Hello")
}

// ❌ 避免 !!（除非确定不为 null）
val name = player!!.name  // 不推荐
```

---

## 性能优化策略

### 已实现的优化

1. ✅ **配置缓存机制** - 启动/reload 时读取，事件处理零开销
2. ✅ **事件驱动架构** - 无轮询任务，按需响应
3. ✅ **快速失败优化** - 先检查简单条件
4. ✅ **Folia 调度器** - 原生多线程支持
5. ✅ **PDC 数据持久化** - 减少内存占用
6. ✅ **并发集合** - ConcurrentHashMap 保证线程安全
7. ✅ **事件优先级** - 合理设置 EventPriority

### 性能监控

```kotlin
// 调试模式
private var debug: Boolean = false

fun loadConfig() {
    debug = plugin.config.getBoolean("debug", false)
}

private fun debugLog(message: String) {
    if (debug) {
        plugin.logger.info("[DEBUG] $message")
    }
}
```

---

## 开发流程

### 添加新功能模块

#### 1. 创建包结构
```
NewFeature/
├── NewFeatureManager.kt
├── NewFeatureCommand.kt
└── NewFeatureListener.kt
```

#### 2. 实现 Manager
```kotlin
class NewFeatureManager(private val plugin: JavaPlugin) {
    private var enabled: Boolean = true
    
    init {
        loadConfig()
    }
    
    fun loadConfig() {
        enabled = plugin.config.getBoolean("new_feature.enabled", true)
    }
    
    fun isEnabled(): Boolean = enabled
}
```

#### 3. 实现 Command
```kotlin
class NewFeatureCommand(
    private val manager: NewFeatureManager
) : SubCommandHandler {
    override fun handle(...): Boolean { ... }
    override fun tabComplete(...): List<String> { ... }
}
```

#### 4. 实现 Listener
```kotlin
class NewFeatureListener(
    private val plugin: JavaPlugin,
    private val manager: NewFeatureManager
) : Listener {
    @EventHandler
    fun onEvent(event: SomeEvent) {
        if (!manager.isEnabled()) return
        // 业务逻辑
    }
}
```

#### 5. 注册到主类
```kotlin
// TSLplugins.kt
private lateinit var newFeatureManager: NewFeatureManager

override fun onEnable() {
    newFeatureManager = NewFeatureManager(this)
    val listener = NewFeatureListener(this, newFeatureManager)
    pm.registerEvents(listener, this)
    
    dispatcher.registerSubCommand("newfeature", NewFeatureCommand(newFeatureManager))
}

fun reloadNewFeatureManager() {
    newFeatureManager.loadConfig()
}
```

#### 6. 添加到 ReloadCommand
```kotlin
plugin.reloadNewFeatureManager()
```

#### 7. 添加配置
```yaml
# config.yml
new_feature:
  enabled: true
  messages:
    prefix: "&6[NewFeature]&r "
```

#### 8. 更新配置版本
```kotlin
// ConfigUpdateManager.kt
const val CURRENT_CONFIG_VERSION = 11  // 递增
```

### 构建和部署

```bash
# Windows
gradlew.bat clean shadowJar

# 输出
build/libs/TSLplugins-1.0.jar
```

---

## 项目特色

### 技术特色

1. **Kotlin 语言优势**
   - 简洁的语法（数据类、Lambda、空安全）
   - 与 Java 完全互操作
   - 现代化的语言特性

2. **Folia 原生支持**
   - 完全使用新的调度器 API
   - 无传统 Bukkit Scheduler
   - 原生多线程性能

3. **配置缓存机制**
   - 零 I/O 开销
   - 最佳性能表现
   - 简单的重载机制

4. **PDC 数据持久化**
   - 零配置需求
   - 数据永不丢失
   - 自动跨服同步

5. **智能配置更新**
   - 自动检测版本
   - 保留注释和格式
   - 平滑升级体验

### 架构特色

1. **模块化设计**
   - 13 个独立模块
   - 互不依赖
   - 可单独禁用

2. **统一架构模式**
   - Manager-Command-Listener
   - SubCommandHandler 接口
   - 一致的代码风格

3. **事件驱动**
   - 无轮询任务
   - 按需响应
   - 高效节能

### 用户体验

1. **配置热重载**
   - `/tsl reload` 重载所有配置
   - 无需重启服务器

2. **完整的 Tab 补全**
   - 所有命令支持 Tab
   - 智能补全玩家名

3. **丰富的消息配置**
   - 所有消息可自定义
   - 支持颜色代码
   - 统一的前缀系统

4. **权限系统**
   - 细粒度权限控制
   - Bypass 权限支持
   - OP 默认拥有所有权限

---

## 总结

TSLplugins 是一个设计精良、架构清晰、性能优越的 Minecraft 服务器插件。通过模块化设计、配置缓存、PDC 持久化等技术，实现了高性能和良好的用户体验。

### 核心优势

- ✅ **性能优越** - 配置缓存、事件驱动、零轮询
- ✅ **架构清晰** - Manager-Command-Listener 模式
- ✅ **易于维护** - 统一风格、模块独立
- ✅ **用户友好** - 热重载、Tab 补全、自定义消息
- ✅ **数据安全** - PDC 持久化、自动备份

### 适合学习的内容

- Kotlin 在 Minecraft 插件开发中的应用
- Folia 多线程调度器的使用
- 事件驱动架构设计
- 配置管理和版本控制
- 数据持久化方案

---

**文档生成日期**: 2025-11-25  
**插件版本**: 1.0  
**配置版本**: 10

