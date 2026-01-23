# Change: Fix Fabric WebBridge Connection and Threading Issues

## Why

两个问题导致 Fabric mod 无法正常工作：

1. **URL 参数缺失**：连接 URL 没有包含 `serverId` 和 `from=mc` 参数，导致 Web 服务器无法识别客户端
2. **线程安全问题**：`PlayerListReporter` 在独立的 `ScheduledExecutorService` 线程上访问 Minecraft 服务器数据

**现象：** 连接成功、能收到服务器消息，但服务器收不到 Fabric mod 发送的任何消息。Web 端日志显示 `MC 客户端未提供 serverId`。

## What Changes

- 修改 `ModConfig.WebSocketConfig.getFullUrl()` 添加 `from=mc` 和 `serverId` 参数
- 修改 `PlayerListReporter.sendPlayerList()` 在服务器主线程收集数据
- 修改 `PlayerListReporter.sendHeartbeat()` 使用相同模式
- 添加调试日志以便追踪消息发送状态

## Impact

- Affected specs: 无（这是 Fabric mod 的 bug 修复，不影响 spec）
- Affected code:
  - `FabricMod/TSLWebBridge/src/main/java/org/tsl/webbridge/config/ModConfig.java`
  - `FabricMod/TSLWebBridge/src/main/java/org/tsl/webbridge/PlayerListReporter.java`
  - `FabricMod/TSLWebBridge/src/main/java/org/tsl/webbridge/WebBridgeClient.java`
