# Proposal: Fix Module Bugs Batch 1

## Summary

修复三个模块的已知问题：幻翼控制热重载问题、phome 模块多项改进、计时属性命令别名。

## Why

1. **幻翼控制模块** - 热重载后玩家的 `TIME_SINCE_REST` 统计没有被处理，导致幻翼开始生成
2. **phome 模块** - 命令结构不直观，颜色代码解析不完整，缺少成员通知
3. **计时属性模块** - 命令名 `timed-attribute` 太长，需要简短别名 `attr`

## What Changes

### 1. 幻翼控制模块 (PhantomModule)

**问题**：热重载插件后，玩家身边会开始生成幻翼，需要重新开关才能解决。

**原因**：`doEnable()` 没有立即处理在线玩家的 `TIME_SINCE_REST` 统计。

**解决方案**：在 `doEnable()` 中调用 `processAllPlayers()` 立即处理所有在线玩家。

### 2. phome 模块 (TownPHomeModule)

**问题 A**：命令结构不直观
- 当前：`/phome` 显示列表，`/phome gui` 打开 GUI
- 期望：`/phome` 直接打开 GUI，`/phome <名称>` 直接传送

**问题 B**：家名称可以设置为 "set"，与命令冲突
- 解决：禁止将家名称设置为保留字

**问题 C**：十六进制颜色代码解析不完整
- 小镇名称如 `#08c8ff『&#2acfff千&#4cd6ff年&#6fdeff科&#91e5ff技&#b3ecff』` 在 GUI 中显示不正确
- 解决：使用支持 MiniMessage 或十六进制颜色的序列化器

**问题 D**：设置/删除 phome 时不通知小镇成员
- 解决：镇长等管理者设置/移除 phome 时，广播通知给所有在线小镇成员

### 3. 计时属性模块 (TimedAttributeModule)

**问题**：命令 `/tsl timed-attribute` 太长

**解决方案**：注册额外命令别名 `attr`，使 `/tsl attr` 可用

## Scope

- **Files affected**: 
  - `PhantomModule.kt`
  - `TownPHomeCommand.kt`
  - `TownPHomeManager.kt`
  - `TownPHomeGUI.kt`
  - `TimedAttributeModule.kt`
- **Risk**: Low - 都是独立的小修复
- **Breaking changes**: `/phome` 默认行为从显示列表改为打开 GUI
