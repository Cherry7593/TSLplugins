# Change: Add Bind PAPI Variables

## Why

玩家绑定状态目前只能通过 WebSocket 交互获知，无法通过 PAPI 变量在其他插件（如计分板、TAB）中显示。需要新增绑定相关变量，支持离线缓存以提升性能。

## What Changes

- **ADDED**: `%tsl_bind%` 变量 - 返回玩家当前绑定状态 (true/false)
- **ADDED**: `%tsl_bind_qq%` 变量 - 返回玩家绑定的 QQ 号码
- **ADDED**: 本地缓存机制 - 支持离线存储绑定状态
- **ADDED**: 智能更新时机 - 玩家上线/下线时更新，绑定状态变更时更新

## Impact

- Affected specs: `bind` (新增), `webbridge` (关联)
- Affected code:
  - `TSLPlaceholderExpansion.kt` - 添加新变量处理
  - `WebBridge/` - 添加绑定状态缓存管理器
  - 数据库或 PDC - 存储绑定缓存

## Design Decisions

### 离线存储方案

使用现有 `TSLPlayerProfile` + `TSLPlayerProfileStore` 存储绑定状态：
- 优点：与现有玩家数据存储保持一致，统一管理
- 存储位置：`playerdata/<uuid>.yml`
- 新增字段：`bindStatus` (Boolean), `bindQQ` (String)

### 更新时机

1. **玩家上线时** - 向 Web 端查询最新绑定状态并缓存
2. **绑定/解绑成功时** - 立即更新本地缓存
3. **玩家下线时** - 确保缓存已持久化（PDC 自动处理）

### 性能考虑

- PAPI 变量查询直接读取本地缓存，不触发 WebSocket 请求
- 避免频繁查询：仅在关键事件时更新
