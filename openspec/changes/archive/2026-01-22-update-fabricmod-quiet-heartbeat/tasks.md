## 1. Implementation

- [x] 1.1 在 `ModConfig.java` 添加 `debug` 配置字段，默认值为 `false`
- [x] 1.2 修改 `WebBridgeClient.java` 中的 `processSendQueue()` 方法，根据 debug 配置决定日志级别
- [x] 1.3 更新 `PlayerListReporter.java` 中的日志输出，遵循相同的 debug 配置
- [x] 1.4 构建并测试 FabricMod，验证心跳不再在控制台显示
