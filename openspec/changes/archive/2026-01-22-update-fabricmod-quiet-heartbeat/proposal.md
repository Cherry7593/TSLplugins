# Change: 抑制 FabricMod 心跳日志输出

## Why

当前 FabricMod 的 WebBridgeClient 在每次发送消息时都会打印 INFO 级别的日志，包括心跳消息。心跳默认每 30 秒发送一次，导致控制台输出过于频繁和杂乱，影响查看其他重要信息。

## What Changes

- 添加 `debug` 配置选项控制详细日志输出
- 默认关闭详细日志（debug=false）
- 心跳和常规消息发送日志降级为 FINE 级别
- 仅在 debug=true 时以 INFO 级别输出详细消息

## Impact

- Affected specs: `webbridge`
- Affected code: 
  - `FabricMod/TSLWebBridge/src/main/java/org/tsl/webbridge/config/ModConfig.java`
  - `FabricMod/TSLWebBridge/src/main/java/org/tsl/webbridge/WebBridgeClient.java`
  - `FabricMod/TSLWebBridge/src/main/java/org/tsl/webbridge/PlayerListReporter.java`
