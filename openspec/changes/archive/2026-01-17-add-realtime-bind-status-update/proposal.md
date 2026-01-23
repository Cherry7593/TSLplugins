# Change: 实时绑定状态更新

## Why

当前玩家在 QQ 群或网页完成绑定后，游戏内绑定变量（如 `%tsl_bind%`、`%tsl_bind_qq%`）不会立即更新，需要玩家重新上线才能刷新。Web 端新增了 `BIND_STATUS_UPDATE` WebSocket 事件广播机制，插件需要监听此事件实现实时更新。

## What Changes

- 在 `WebBridgeClient.handleEventMessage()` 中新增对 `BIND_STATUS_UPDATE` 事件的监听
- 在 `QQBindManager` 中新增 `handleBindStatusUpdateEvent()` 方法处理事件
- 收到事件时，查找在线玩家并刷新其绑定变量缓存
- 向玩家发送绑定状态变更提示消息
- 支持配置绑定/解绑时执行的命令（通过 `on-bind-commands` / `on-unbind-commands`）

## Impact

- Affected specs: `bind`
- Affected code: `WebBridgeClient.kt`, `QQBindManager.kt`, `config.yml`
