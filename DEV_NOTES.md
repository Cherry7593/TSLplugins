# TSLplugins - 开发笔记

> 内部开发文档 - 简洁的技术要点和架构说明

---

## 基本信息

- **语言**: Kotlin 1.9.21
- **服务器**: Paper/Folia 1.21.8
- **Java**: 21
- **构建工具**: Gradle 8.5 + Kotlin DSL
- **版本**: 1.0

---

## 核心架构

### 设计原则

- **模块化**: 每个软件包是独立的功能模块
- **即插即用**: 各模块互不依赖，可单独禁用
- **事件驱动**: 使用 Bukkit/Folia 事件系统
- **配置驱动**: 所有行为可通过配置文件控制
- **配置缓存**: 开关状态仅在启动/reload时读取，事件处理直接读取缓存（性能优化）

### 项目结构

```
TSLplugins/
├── TSLplugins.kt              # 主类，初始化所有模块
├── TSLCommand.kt              # 统一命令入口（/tsl）
├── ReloadCommand.kt           # 重载所有模块配置
├── ConfigUpdateManager.kt     # 配置文件版本控制
│
├── Alias/                     # 命令别名（动态注册、Tab补全）
├── Maintenance/               # 维护模式（登录拦截、MOTD、白名单）
├── Scale/                     # 体型调整（Attribute.SCALE）
├── Hat/                       # 帽子系统（命令操作、堆叠支持）
├── Ping/                      # 延迟查询（单人查询、全服排行、分页显示）
├── Toss/                      # 生物举起（叠罗汉、投掷、个人开关、速度调节）
├── Ride/                      # 生物骑乘（直接骑乘、个人开关、黑名单）
├── Advancement/               # 成就过滤（公屏消息、PlaceholderAPI）
├── Visitor/                   # 访客保护（LuckPerms权限驱动）
├── Permission/                # 权限检测（PlaceholderAPI变量）
├── Farmprotect/              # 农田保护（事件拦截）
└── Motd/                     # 假玩家数（人数偏移）
```

---

## 技术要点

### 1. Folia 兼容性

**实体操作** - 使用实体调度器：
```kotlin
player.scheduler.run(plugin, { _ ->
    // 实体相关操作
}, null)
```

**定时任务** - 使用区域调度器：
```kotlin
server.globalRegionScheduler.runAtFixedRate(plugin, { _ ->
    // 定时任务
}, 20L, 20L)
```

**延迟执行**：
```kotlin
player.scheduler.runDelayed(plugin, { _ ->
    // 延迟操作
}, null, 20L)
```

### 2. 功能开关系统

**统一开关格式**:
```yaml
feature-name:
  enabled: true  # 默认开启，可单独禁用
```

**配置缓存机制**（性能优化）:
```kotlin
class FeatureClass(private val plugin: JavaPlugin) : Listener {
    private var enabled: Boolean = true  // 缓存开关状态
    
    init {
        loadConfig()
    }
    
    fun loadConfig() {
        enabled = plugin.config.getBoolean("feature.enabled", true)
    }
    
    @EventHandler
    fun onEvent(event: SomeEvent) {
        if (!enabled) return  // 直接读取缓存，零开销
        // 功能逻辑
    }
}
```

**优势**:
- ✅ 减少I/O开销 - 不再频繁访问config
- ✅ 提升响应速度 - 直接内存读取
- ✅ 代码更简洁 - 逻辑清晰易维护
- ✅ 更稳定可靠 - 避免并发问题

**所有模块开关**:
- `fakeplayer.enabled` - MOTD假玩家
- `alias.enabled` - 命令别名
- `advancement.enabled` - 成就消息过滤
- `farmprotect.enabled` - 农田保护
- `visitor.enabled` - 访客模式
- `permission-checker.enabled` - 权限检测
- `maintenance.enabled` - 维护模式
- `scale.enabled` - 体型调整
- `hat.enabled` - 帽子系统
- `ping.enabled` - 延迟查询
- `toss.enabled` - 生物举起
- `ride.enabled` - 生物骑乘

### 3. 配置自动更新

**版本控制** (`ConfigUpdateManager.kt`):
```kotlin
const val CURRENT_CONFIG_VERSION = 6
```

**工作流程**:
1. 启动时检测 `config-version` 字段
2. 版本不同 → 自动备份 + 合并配置
3. 保留用户配置值，添加新配置项
4. 按默认配置顺序重新生成

**更新配置版本**:
- 修改 `config.yml`（资源文件）
- 递增 `CURRENT_CONFIG_VERSION`
- 重新构建

### 3. 命令别名系统

**核心机制**:
- `AliasManager` 加载配置
- `DynamicAliasCommand` 动态注册命令
- 使用反射注入 `CommandMap`
- 代理执行原命令

**格式**:
```yaml
aliases:
  - "别名:原命令"
  - "子别名 子命令:原命令"
```

### 4. 维护模式

**登录拦截** (`MaintenanceLoginListener`):
- `AsyncPlayerPreLoginEvent` 登录前拦截
- 白名单使用 UUID 验证
- 避免区块加载

**权限检查** (`MaintenancePermissionListener`):
- 主动检查：开启维护、移除白名单、切换世界
- 权限绕过：`tsl.maintenance.bypass`

**MOTD 修改** (`MaintenanceMotdListener`):
- `ServerListPingEvent` 修改 MOTD
- ProtocolLib 修改版本栏

### 5. 体型调整

**API**: `Attribute.SCALE` (Minecraft 1.21.8+)

**范围控制**:
- 普通玩家：`config.yml` 中的 min/max
- Bypass 权限：硬编码 0.1-2.0

**Tab 补全**:
- 根据权限动态生成补全列表
- 普通玩家：配置范围
- OP：完整范围

### 6. 访客模式

**权限驱动** (`VisitorEffect.kt`):
- 监听 LuckPerms `UserDataRecalculateEvent`
- 权限变更 → 实时更新效果
- 无需轮询

**效果**:
- 怪物目标清除（`EntityTargetEvent`）
- 发光效果（`PotionEffect.GLOWING`）

### 7. 成就系统

**消息过滤** (`AdvancementMessage.kt`):
- 拦截 `PlayerAdvancementDoneEvent`
- 普通成就：取消公屏，发送个人消息
- 挑战成就：保留公屏

**PlaceholderAPI** (`TSLPlaceholderExpansion.kt`):
- 提供 `%tsl_adv_count%` 占位符
- 使用 Bukkit API 统计成就数

### 8. Hat 帽子系统

**命令操作** (`HatCommand.kt`):
- `/tsl hat` 将手持物品戴到头上
- 权限检查：`tsl.hat.use`
- 支持堆叠物品，自动只戴1个
- **静默操作**：无成功提示，像原版装备一样
- 冷却时间控制

**特性**:
- 手持多个物品时只戴1个，剩余留在手中
- 原头盔智能处理：放回背包或掉落地上
- 黑名单支持：禁止特定物品戴在头上

**配置管理** (`HatManager.kt`):
- 功能开关（cached）
- 黑名单检查
- 冷却时间管理
- 消息系统

**Folia兼容**:
```kotlin
player.scheduler.run(plugin, { _ ->
    val itemToEquip = itemInHand.clone()
    itemToEquip.amount = 1
    player.inventory.setHelmet(itemToEquip)
    // 智能处理剩余物品和原头盔
}, null)
```

### 9. Ping 延迟查询

**命令系统** (`PingCommand.kt`):
- `/tsl ping` - 查看自己的延迟
- `/tsl ping <玩家名>` - 查看指定玩家延迟
- `/tsl ping all [页码]` - 查看全服延迟排行

**功能特性**:
- 延迟颜色分级（绿色<100ms, 黄色<200ms, 红色>200ms）
- 分页显示，可点击翻页按钮
- 服务器平均延迟统计
- 按延迟从低到高排序

**分页系统** (`PingPaginator.kt`):
- Adventure API 实现可点击按钮
- 自动计算总页数
- 对齐显示（排名、玩家名、延迟）

**配置管理** (`PingManager.kt`):
- 功能开关（cached）
- 每页显示数量配置
- 延迟颜色阈值配置
- 消息系统

### 10. Toss 生物举起

**操作方式**:
- `Shift + 右键生物` - 举起生物（叠罗汉效果）
- `Shift + 左键` - 投掷最顶端的生物
- `Shift + 右键空气/地面` - 放下所有生物

**命令系统** (`TossCommand.kt`):
- `/tsl toss` - 查看当前状态
- `/tsl toss toggle` - 切换举起功能开关（防误触）
- `/tsl toss velocity <数值>` - 设置投掷速度

**功能特性**:
- 叠罗汉效果（多个生物叠成塔）
- 个人开关（防止误触）
- 可配置的投掷速度范围
- OP 速度无限制（0.0-10.0，普通玩家遵守配置）
- 实体黑名单（凋零、末影龙、监守者等）
- 举起数量限制（默认3个）

**投掷系统** (`TossListener.kt`):
- 物理计算（方向 + 速度 + 抛物线）
- 循环引用防护
- 玩家状态管理
- Folia 兼容的实体调度器
- **注意**: `PlayerInteractEvent` 不使用 `ignoreCancelled=true`（确保左键空气也能触发）

**配置管理** (`TossManager.kt`):
- 功能开关（cached）
- 玩家开关状态持久化
- 玩家投掷速度持久化
- 速度范围控制（普通玩家受限，OP 无限制）
- 黑名单检查

**关键技术点**:
```kotlin
// Vector 速度设置（y 是只读属性）
throwVelocity.setY(throwVelocity.y + 0.3)  // ✅ 正确
throwVelocity.y = throwVelocity.y + 0.3    // ❌ 错误

// OP 速度无限制
val hasBypass = player.isOp || player.hasPermission("tsl.toss.velocity.bypass")
```

### 11. Ride 生物骑乘

**操作方式**:
- `空手右键生物` - 直接骑乘生物

**命令系统** (`RideCommand.kt`):
- `/tsl ride toggle` - 切换骑乘功能开关（防误触）

**功能特性**:
- 简单直观的骑乘方式
- 个人开关（防止误触）
- 实体黑名单（凋零、末影龙、监守者、幽灵、远古守卫者等）
- **不检查副手**：副手持物品也可以骑乘（用户需求）
- 主手必须为空

**骑乘逻辑** (`RideListener.kt`):
- 快速失败优化（先检查简单条件）
- 并发安全检查（二次验证实体和玩家状态）
- Folia 兼容的实体调度器
- 自动取消默认交互（防止打开 GUI）

**配置管理** (`RideManager.kt`):
- 功能开关（cached）
- 玩家开关状态持久化
- 黑名单检查
- 消息系统

**关键技术点**:
```kotlin
// 事件必须显式取消
if (manager.isEntityBlacklisted(entity.type) && 
    !player.hasPermission("tsl.ride.bypass")) {
    event.isCancelled = true  // ✅ 必须！
    return
}

// 并发安全检查
entity.scheduler.run(plugin, { _ ->
    if (entity.isValid && player.isOnline && 
        entity.passengers.isEmpty() && player.vehicle == null) {
        entity.addPassenger(player)
    }
}, null)
```

---

## 依赖管理

### 必需依赖

```kotlin
compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
```

### 可选依赖

```kotlin
compileOnly("net.luckperms:api:5.4")          // 访客模式、权限检测
compileOnly("me.clip:placeholderapi:2.11.6")  // 成就统计、权限检测
```

### Maven 仓库

```kotlin
repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://repo.dmulloy2.net/repository/public/")
}
```

---

## 构建与部署

### 构建命令

```bash
# 清理构建
./gradlew clean

# 构建 JAR
./gradlew build

# 生成 shadowJar（包含 Kotlin 标准库）
./gradlew shadowJar

# 输出: build/libs/TSLplugins-1.0.jar
```

### Windows 环境

```cmd
gradlew.bat clean shadowJar
```

### 部署流程

1. 复制 JAR 到 `plugins/` 目录
2. 确保安装依赖插件（LuckPerms 必需）
3. 启动服务器（自动生成配置）
4. 根据需要修改 `config.yml` 和 `aliases.yml`
5. 执行 `/tsl reload` 应用配置

---

## 重要文件说明

### 主类 (`TSLplugins.kt`)

**职责**:
- 初始化所有功能模块
- 注册命令和监听器
- 提供重载接口

**初始化顺序**:
1. 配置更新检查
2. 加载各模块配置
3. 注册事件监听器
4. 注册命令处理器
5. 初始化 PlaceholderAPI

### 命令分发器 (`TSLCommand.kt`)

**架构**:
```kotlin
interface SubCommandHandler {
    fun handle(sender: CommandSender, args: Array<out String>): Boolean
    fun tabComplete(sender: CommandSender, args: Array<out String>): List<String>
}
```

**注册子命令**:
```kotlin
private val subCommands = mapOf(
    "reload" to ReloadCommand(this),
    "maintenance" to MaintenanceCommand(manager),
    "scale" to ScaleCommand(scaleManager),
    // ...
)
```

### 配置更新 (`ConfigUpdateManager.kt`)

**关键逻辑**:
```kotlin
// 1. 设置版本号（置顶）
newConfig.set("config-version", CURRENT_CONFIG_VERSION)

// 2. 按默认配置顺序遍历
for (key in defaultConfig.getKeys(true)) {
    if (currentConfig.contains(key)) {
        // 保留用户值
        newConfig.set(key, currentConfig.get(key))
    } else {
        // 添加新配置项
        newConfig.set(key, defaultConfig.get(key))
    }
}
```

---

## 开发规范

### Kotlin 规范

- 优先使用数据类：`data class Config(...)`
- 使用属性访问：`player.name` 而非 `player.getName()`
- 空安全：`player?.health ?: 0.0`
- 字符串模板：`"玩家 $name 已加入"`
- Lambda 表达式：`list.filter { it > 0 }`

### 命名规范

- **类**: `PascalCase` - `MaintenanceManager`
- **函数**: `camelCase` - `loadConfig()`
- **变量**: `camelCase` - `configFile`
- **常量**: `UPPER_SNAKE_CASE` - `MAX_SCALE`
- **包名**: `lowercase` - `org.tsl.tslplugins`

### 日志规范

```kotlin
logger.info("信息：模块已加载")
logger.warning("警告：配置项缺失")
logger.severe("错误：无法加载配置")
```

---

## 常见开发任务

### 添加新功能模块

1. 创建包：`src/main/kotlin/org/tsl/tSLplugins/NewFeature/`
2. 实现类：`NewFeatureManager.kt`
3. 注册到 `TSLplugins.kt`
4. 添加配置到 `config.yml`
5. 更新配置版本号

### 添加新命令

1. 实现 `SubCommandHandler` 接口
2. 注册到 `TSLCommand.subCommands`
3. 添加权限到 `plugin.yml`

### 修改配置结构

1. 修改 `src/main/resources/config.yml`
2. 递增 `ConfigUpdateManager.CURRENT_CONFIG_VERSION`
3. 重新构建插件

---

## IDE 配置

### IntelliJ IDEA

**Gradle 同步**:
- 右侧 Gradle 面板 → 刷新图标
- 或 `Ctrl+Shift+O`

**JVM 设置**:
- Settings → Build Tools → Gradle
- Gradle JVM: Java 21

**Kotlin 插件**:
- 确保安装 Kotlin 插件
- 版本：1.9.21

---

## 测试要点

### 功能测试

- [ ] 命令别名：Tab 补全、子命令、中文别名
- [ ] 维护模式：白名单、MOTD、登录拦截
- [ ] 体型调整：范围限制、Bypass 权限、Tab 补全
- [ ] Hat 系统：戴帽、堆叠物品、黑名单
- [ ] Ping 查询：单人查询、全服排行、分页翻页
- [ ] Toss 举起：叠罗汉、投掷、速度调整、个人开关、黑名单
- [ ] Ride 骑乘：直接骑乘、个人开关、黑名单、副手检查
- [ ] 访客模式：LuckPerms 权限联动、效果生效
- [ ] 配置重载：所有模块正确重载

### Folia 测试

- [ ] 实体调度器：玩家操作正常
- [ ] 区域调度器：定时任务正常
- [ ] 无跨区域错误

### 性能测试

- [ ] 无内存泄漏
- [ ] 无定期轮询（除必要任务）
- [ ] 事件处理效率

---

## 已知问题与限制

1. **体型设置不持久化** - 重新登录后恢复默认
2. **维护模式白名单基于玩家名** - 改名后需重新添加
3. **别名 Tab 补全仅支持玩家名** - 复杂参数不支持
4. **访客模式需要 LuckPerms** - 不支持其他权限插件

---

## 性能优化记录

### 已优化

- ✅ 访客模式移除轮询，改为事件驱动
- ✅ 维护模式登录前拦截，避免区块加载
- ✅ 配置更新仅启动时执行一次
- ✅ **所有模块开关配置缓存**：启动/reload时读取，事件处理零开销

### 可优化

- ⏳ 成就统计缓存（减少 API 调用）
- ⏳ 别名命令缓存（减少反射）

---

## 快速参考

### 重载模块

```kotlin
// 在各 Manager 中实现
fun reload() {
    config.reload()
    loadConfigValues()
}
```

### 发送彩色消息

```kotlin
player.sendMessage(
    ChatColor.translateAlternateColorCodes('&', message)
)
```

### 获取在线玩家

```kotlin
// Paper/Folia
server.onlinePlayers.forEach { player ->
    // 操作
}
```

### 权限检查

```kotlin
if (player.hasPermission("tsl.feature.use")) {
    // 有权限
}
```

---

## 更新记录

- **2025-11-20**: 文档整理，删除冗余文档，创建综合开发总结
- **2025-11-20**: Ride 生物骑乘功能整合完成
- **2025-11-20**: 修复 Toss 投掷速度和左键空气问题
- **2025-11-19**: Toss 生物举起功能整合完成，OP 速度无限制
- **2025-11-19**: 黑名单功能修复，代码优化完成
- **2025-11-18**: Ping 延迟查询功能整合完成
- **2025-11-18**: 代码清理，移除 ProtocolLib 依赖
- **2025-11-11**: Hat 帽子系统集成完成
- **2025-11-10**: Scale 体型调整功能集成完成
- **2025-11-08**: 配置自动更新系统
- **2025-11-07**: 初始版本发布

---

**最后更新**: 2025-11-20  
**文档版本**: 2.1  
**配置版本**: 7

