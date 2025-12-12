# PlayerCountCmd 人数控制命令模块

根据在线人数自动执行控制台命令。

## 适用场景

- **Chunky 预加载控制** - `chunky pause` / `chunky continue`
- **Bluemap 渲染控制** - `bluemap stop` / `bluemap start`  
- **视距调整** - 动态调整服务器视距
- **其他** - 任何需要根据人数控制的功能

## 特性

- ✅ **事件驱动** - 不使用定时轮询，仅在玩家加入/退出时触发
- ✅ **回差防抖** - 状态机设计，避免人数波动时频繁切换
- ✅ **Folia 兼容** - 完全支持 Folia/Luminol 线程模型
- ✅ **配置化** - 阈值、命令、间隔均可配置
- ✅ **零性能压力** - 仅在事件触发时执行轻量逻辑

## 配置说明

```yaml
player-count-cmd:
  enabled: true
  
  # 阈值配置
  upper-threshold: 52      # ≥52人执行高人数命令
  lower-threshold: 48      # ≤48人执行低人数命令
  
  # 防抖间隔
  min-interval-ms: 10000   # 10秒
  
  # 命令配置
  command-when-low: "chunky continue"
  command-when-high: "chunky pause"
```

## 回差逻辑说明

上阈值和下阈值形成一个"回差区间"，防止人数频繁波动导致命令频繁执行：

```
人数: 45 46 47 48 49 50 51 52 53
           ↑              ↑
         下阈值        上阈值
       (低人数命令)  (高人数命令)

状态变化示例：
- 48→49→50→51→52 (到达52时执行高人数命令)
- 52→51→50→49→48 (到达48时执行低人数命令)
- 49→50→51→50→49 (中间区域不切换，保持当前状态)
```

## 使用示例

### Chunky 预加载控制
```yaml
command-when-low: "chunky continue"
command-when-high: "chunky pause"
```

### Bluemap 渲染控制
```yaml
command-when-low: "bluemap start"
command-when-high: "bluemap stop"
```

### 视距调整
```yaml
command-when-low: "minecraft:gamerule viewDistance 12"
command-when-high: "minecraft:gamerule viewDistance 8"
```

## 线程安全

- 使用 `AtomicReference` 和 `AtomicLong` 保证线程安全
- 事件处理使用 `AsyncScheduler` 避免阻塞事件线程
- 命令执行使用 `GlobalRegionScheduler` 符合 Folia 要求

