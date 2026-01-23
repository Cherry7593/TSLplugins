# Design: Landmark System

## Context

地标系统是一个服务器级交通网络，需要处理：
- 地标数据的持久化存储
- 玩家位置的高效检测
- 跨维度支持
- Folia 多线程兼容性

## Goals / Non-Goals

**Goals:**

- 提供管理员可管理的地标网络
- 玩家探索式解锁机制
- 安全高效的传送系统
- 性能优化的区域检测

**Non-Goals:**

- 经济系统集成
- 领地保护判定
- 跨服同步（预留接口即可）

## Decisions

### 1. 模块结构

采用标准 Manager-Command-Listener 模式：

```
landmark/
├── LandmarkManager.kt    # 地标数据管理、配置加载、工具方法
├── LandmarkCommand.kt    # 命令处理 (/lm)
├── LandmarkListener.kt   # 事件监听（移动检测、区域进入）
├── LandmarkData.kt       # 数据类定义
├── LandmarkGUI.kt        # GUI 菜单
└── LandmarkStorage.kt    # 数据持久化
```

**理由:** 符合项目现有架构模式，便于维护。

### 2. 数据存储方案

采用 JSON 文件存储：
- `plugins/TSLplugins/landmarks.json` - 地标数据
- `plugins/TSLplugins/landmark-unlocks.json` - 玩家解锁记录

**理由:** 
- 与项目现有模式一致
- 便于调试和手动修改
- kotlinx-serialization 已在依赖中

**备选方案（已否决）:**
- SQLite: 增加额外依赖，对于预期数据量过重
- PDC: 地标数据是全局的，不适合存储在玩家数据中

### 3. 区域检测策略

使用 PlayerMoveEvent 配合缓存优化：
- 每个玩家缓存当前所在地标（或 null）
- 仅在坐标块（Block）变化时检测
- 使用 AABB 包围盒快速判断

**理由:** 平衡检测精度与性能开销。

**优化措施:**
- 使用空间索引（按世界分组，可选按区块分组）
- 缓存玩家当前地标状态，避免重复检测

### 4. 命令结构

使用独立命令 `/lm` 而非 `/tsl lm`：

**理由:** 
- 地标系统使用频率高，独立命令更便捷
- 与文档 PRD 保持一致

### 5. Folia 兼容性

- 使用 `player.scheduler.run()` 处理玩家相关操作
- 使用 `Bukkit.getGlobalRegionScheduler()` 处理全局任务
- 数据操作使用线程安全容器（ConcurrentHashMap）

## Risks / Trade-offs

| 风险 | 缓解措施 |
|------|----------|
| 高频移动事件影响性能 | 块坐标变化检测 + 地标缓存 |
| 大量地标时检测变慢 | 按世界分组 + 可选区块索引 |
| 传送目标不安全 | 安全落点算法 + 配置开关 |

## Migration Plan

无迁移需求，这是新功能。

## Open Questions

1. 是否需要支持非 AABB 形状（圆形/多边形）？—— 当前决定仅支持 AABB，未来可扩展
2. 删除地标时玩家解锁记录如何处理？—— 保留记录但标记为已删除，避免数据不一致
