# Tasks

## 1. Fix URL Parameters

- [x] 1.1 修改 `ModConfig.WebSocketConfig.getFullUrl()` 添加 `from=mc` 和 `serverId` 参数

## 2. Fix Threading Issue

- [x] 2.1 修改 `sendPlayerList()` 使用 `server.execute()` 在主线程收集数据
- [x] 2.2 修改 `sendHeartbeat()` 使用相同模式
- [x] 2.3 添加发送日志到 `WebBridgeClient.processSendQueue()` 用于调试

## 3. Verification

- [ ] 3.1 构建 Fabric mod
- [ ] 3.2 部署到服务器测试连接和消息发送
- [ ] 3.3 确认 Web 服务器能收到 player list 和 heartbeat
