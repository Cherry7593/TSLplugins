# Tasks: WebSocket 无限自动重连

## 1. Plugin (Kotlin) 实现

- [x] 1.1 在 `WebBridgeClient.kt` 添加 `ScheduledTask` 引用用于重连任务
- [x] 1.2 添加 `scheduleReconnect()` 方法，使用 Folia 调度器每 30 秒尝试重连
- [x] 1.3 在 `onClose` 回调中调用 `scheduleReconnect()`
- [x] 1.4 在 `connect()` 失败时调用 `scheduleReconnect()`
- [x] 1.5 在 `stop()` 和手动 `disconnect()` 时取消重连任务
- [x] 1.6 连接成功时清理重连任务

## 2. Mod (Java) 修改

- [x] 2.1 修改 `scheduleReconnect()` 移除最大尝试次数检查
- [x] 2.2 当 `maxReconnectAttempts <= 0` 时表示无限重试
- [x] 2.3 更新 `ModConfig.java` 默认 `maxReconnectAttempts = -1`

## 3. 验证

- [ ] 3.1 测试 Plugin：手动断开服务器，确认每 30 秒尝试重连
- [ ] 3.2 测试 Mod：同上
- [ ] 3.3 测试配置向后兼容：`maxReconnectAttempts > 0` 时仍然限制次数
