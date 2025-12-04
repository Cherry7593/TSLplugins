# WebBridge 架构说明

- 客户端：Java-WebSocket 1.5.6
- 管理器：WebBridgeManager 负责初始化与命令入口
- 监听器：WebBridgeChatListener 负责 MC→Web
- 客户端：WebBridgeClient 负责连接与 Web→MC

