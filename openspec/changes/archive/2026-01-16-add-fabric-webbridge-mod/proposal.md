# Change: Add Fabric WebBridge Mod

## Why

当前 WebBridge 仅支持 Paper/Folia 服务器。用户有 Fabric 服务器也在 Velocity 代理后面，需要同样的在线人数、玩家列表上报功能，并且需要通过不同 `serverId` 区分各服务器数据。

## What Changes

- **ADDED**: 新建独立 Fabric Mod 项目 `TSLWebBridge-Fabric`
- **ADDED**: WebSocket 客户端连接到同一 Web 后端
- **ADDED**: 定时上报 `PLAYER_LIST` 事件（在线人数、玩家列表、TPS/MSPT）
- **ADDED**: 心跳保活机制
- **ADDED**: 配置文件支持（serverId、WebSocket URL、上报间隔）

## Impact

- Affected specs: `webbridge` (新增 Fabric 实现说明)
- New project: `FabricMod/TSLWebBridge/` (独立 Fabric Mod)
- Web 端：无需改动，已支持多 serverId

## Design Decisions

### 方案选择：Fabric Mod vs Velocity 插件

选择 **Fabric Mod** 而非 Velocity 插件，原因：

| 考虑因素 | Velocity 插件 | Fabric Mod ✓ |
|---------|--------------|--------------|
| TPS/MSPT | ❌ 无法获取后端数据 | ✅ 直接获取 |
| 玩家列表 | ⚠️ 需要后端配合 | ✅ 原生获取 |
| 独立性 | 需要所有后端配合 | 独立运行 |
| 维护成本 | 一个项目 | 多一个项目 |

### 项目结构

```
FabricMod/
└── TSLWebBridge/
    ├── src/main/
    │   ├── java/org/tsl/webbridge/
    │   │   ├── TSLWebBridgeMod.java      # Mod 入口
    │   │   ├── WebBridgeClient.java      # WebSocket 客户端
    │   │   ├── PlayerListReporter.java   # 玩家列表上报
    │   │   └── config/
    │   │       └── ModConfig.java        # 配置管理
    │   └── resources/
    │       └── fabric.mod.json
    ├── build.gradle
    └── gradle.properties
```

### 技术选型

- **Language**: Java 21 (Fabric 生态主流)
- **Minecraft**: 1.21.x
- **Fabric API**: 最新稳定版
- **WebSocket**: Java-WebSocket 1.5.6 (与 Paper 插件一致)
- **Config**: Fabric 原生配置或 Cloth Config

### 消息格式

与现有 Paper 插件完全一致：

```json
{
  "type": "event",
  "source": "mc",
  "timestamp": 1705401600000,
  "data": {
    "event": "PLAYER_LIST",
    "id": "pl-xxx",
    "serverId": "fabric-server-1",
    "online": 5,
    "players": [
      {"uuid": "xxx", "name": "Player1"}
    ],
    "tps": 20.0,
    "mspt": 5.2
  }
}
```

### 配置文件

```json
{
  "enabled": true,
  "serverId": "fabric-server-1",
  "websocket": {
    "url": "ws://127.0.0.1:4001/mc-bridge",
    "token": ""
  },
  "playerListInterval": 30,
  "heartbeatInterval": 30,
  "autoReconnect": true,
  "reconnectInterval": 30,
  "maxReconnectAttempts": 5
}
```

## Risks & Mitigations

| 风险 | 缓解措施 |
|------|---------|
| Fabric API 版本兼容 | 使用稳定 API，避免 Mixin |
| 维护两套代码 | 消息格式统一，核心逻辑可抽取共享 |
| 依赖冲突 | Shadow/relocate WebSocket 库 |
