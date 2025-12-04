# WebBridge 模块 README

合并整理自 archive/WEBBRIDGE_*、FIX_*、UPDATE_*、REFACTOR_* 文档。

功能概述：
- MC ↔ Web 双向通信（聊天消息）。
- Folia 兼容，使用 GlobalRegionScheduler 广播消息。
- 手动连接模式：不自动重连，管理员指令控制连接。

配置：
```yaml
webbridge:
  enabled: true
  websocket:
    url: "ws://127.0.0.1:4001/mc-bridge?from=mc&token=YOUR_TOKEN"
```

指令：
- /tsl webbridge status
- /tsl webbridge connect
- /tsl webbridge disconnect

注意事项：
- Token 允许为空；推荐使用 URL 参数 token。
- 仅在类型为 `chat` 且 source=web 时转发至游戏。
- 颜色显示基于 Adventure API。

