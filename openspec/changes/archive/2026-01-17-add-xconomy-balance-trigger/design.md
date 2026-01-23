# Design: XConomy Balance Trigger

## Context

需要实现一个经济余额监控系统，当玩家余额达到配置阈值时自动执行控制台命令。
类似于现有的 `PlayerCountCmdController`，但监控目标是玩家个体余额而非全局人数。

**参考实现**: `PlayerCountCmdController.kt` - 状态机 + 回差逻辑

## Goals / Non-Goals

**Goals:**
- 实时/周期性监测在线玩家余额
- 支持低于阈值A执行命令X、高于阈值B执行命令Y
- 防止阈值边缘反复触发（冷却 + 状态锁）
- Folia 线程安全
- 配置热重载

**Non-Goals:**
- 离线玩家余额监控
- 余额修改功能
- 多经济插件同时监控

## Decisions

### Decision 1: 使用异步轮询而非事件监听

**选择**: 异步定时任务扫描在线玩家

**原因**:
- XConomy 不一定提供余额变化事件
- 轮询可控制频率，避免高频事件冲击
- 参考 `PlayerCountCmd` 的 `AsyncScheduler` 模式

**实现**: `Bukkit.getAsyncScheduler().runAtFixedRate()`

### Decision 2: 玩家状态追踪

**选择**: 每个玩家独立状态 `Map<UUID, TriggerState>`

**状态枚举**:
```kotlin
enum class TriggerState {
    NORMAL,      // 余额在正常区间
    LOW_FIRED,   // 已触发低余额命令
    HIGH_FIRED   // 已触发高余额命令
}
```

**状态转换**:
- `NORMAL` → `LOW_FIRED`: 余额 < lowThreshold
- `NORMAL` → `HIGH_FIRED`: 余额 > highThreshold
- `LOW_FIRED` → `NORMAL`: 余额 >= lowThreshold + hysteresis
- `HIGH_FIRED` → `NORMAL`: 余额 <= highThreshold - hysteresis

### Decision 3: 冷却机制

**选择**: 全局冷却 + 玩家冷却双重保护

- 全局最小检测间隔（配置项）
- 单玩家触发后冷却时间（防止同一玩家频繁触发）

### Decision 4: XConomy API 集成

**选择**: 优先使用 XConomy 原生 API，Vault 作为备选

```kotlin
// 优先 XConomy
val balance = XConomy.getInstance()?.getPlayerData(uuid)?.balance
    ?: economy?.getBalance(player) // Vault fallback
    ?: return
```

### Decision 5: 指令安全

**选择**: 白名单 + 占位符替换

- 仅支持预定义占位符: `%player%`, `%uuid%`, `%balance%`
- 不支持任意 PAPI 占位符（防止注入）
- 命令在 `GlobalRegionScheduler` 执行

## Risks / Trade-offs

| Risk | Mitigation |
|------|------------|
| XConomy 未安装 | 启动时检测，未安装则禁用模块并警告 |
| 高频轮询性能 | 默认间隔 60 秒，可配置 |
| 余额在阈值边缘波动 | 回差(hysteresis)逻辑 + 冷却时间 |
| 指令执行失败 | try-catch 包裹，日志记录 |

## Architecture

```
XconomyTrigger/
├── XconomyTriggerManager.kt   # 配置管理、状态追踪、轮询调度
├── XconomyTriggerCommand.kt   # /tsl xctrigger 命令（可选）
└── XconomyApi.kt              # XConomy/Vault API 封装
```

## Configuration Schema

```yaml
xconomy-trigger:
  enabled: false
  scan-interval-seconds: 60
  hysteresis: 100.0          # 回差值，防止边缘抖动
  player-cooldown-seconds: 300
  low-balance:
    enabled: true
    threshold: 1000.0
    commands:
      - "say %player% 余额不足 %balance%"
  high-balance:
    enabled: true
    threshold: 100000.0
    commands:
      - "say %player% 余额过高 %balance%"
```

## Open Questions

- [ ] 是否需要 WebBridge 后台管理界面？（可后续迭代）
- [ ] 是否支持多组触发器规则？（MVP 先单组）
