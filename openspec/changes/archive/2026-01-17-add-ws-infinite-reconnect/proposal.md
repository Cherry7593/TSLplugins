# Change: WebSocket 无限自动重连

## Why

当前 WebSocket 连接断开后：
- **Plugin (Kotlin)**：完全没有自动重连逻辑，断开后需要手动执行命令重连
- **Mod (Java)**：有自动重连但限制最大尝试次数（默认 5 次），超过后停止尝试

用户希望在网络不稳定或服务器重启的情况下，客户端能够持续尝试重连，确保服务恢复后自动恢复通信。

## What Changes

- **Plugin**：在 `WebBridgeClient.kt` 添加自动重连逻辑，每 30 秒尝试一次，无限重试
- **Mod**：修改 `WebBridgeClient.java` 移除最大尝试次数限制，改为无限重试
- **配置**：`max-reconnect-attempts` 设为 `-1` 或 `0` 时表示无限重试（保持向后兼容）
- **Spec**：更新 Auto-reconnect 场景，反映无限重连行为

## Impact

- Affected specs: `webbridge`
- Affected code:
  - `src/main/kotlin/org/tsl/tSLplugins/WebBridge/WebBridgeClient.kt`
  - `FabricMod/TSLWebBridge/src/main/java/org/tsl/webbridge/WebBridgeClient.java`
  - `FabricMod/TSLWebBridge/src/main/java/org/tsl/webbridge/config/ModConfig.java`
