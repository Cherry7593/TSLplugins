# TSL WebBridge - Fabric Mod

Fabric 服务器端 Mod，用于向 Web 后端上报服务器在线人数和玩家列表。

## 功能

- WebSocket 连接到 Web 后端
- 定时上报 `PLAYER_LIST` 事件（在线人数、玩家列表、TPS/MSPT）
- 玩家进出时立即上报
- 心跳保活机制
- 自动重连

## 要求

- Minecraft 1.21.11
- Fabric Loader 0.16.10+
- Fabric API
- Java 21

## 构建

```bash
./gradlew build
```

构建产物位于 `build/libs/tsl-webbridge-1.0.0.jar`

## 配置

首次运行后会在 `config/tsl-webbridge.json` 生成配置文件：

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

### 配置项说明

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `enabled` | 是否启用 | `true` |
| `serverId` | 服务器标识，用于区分不同服务器 | `fabric-server-1` |
| `websocket.url` | WebSocket 服务器地址 | `ws://127.0.0.1:4001/mc-bridge` |
| `websocket.token` | 认证令牌 | `""` |
| `playerListInterval` | 玩家列表上报间隔（秒） | `30` |
| `heartbeatInterval` | 心跳间隔（秒） | `30` |
| `autoReconnect` | 是否自动重连 | `true` |
| `reconnectInterval` | 重连间隔（秒） | `30` |
| `maxReconnectAttempts` | 最大重连次数 | `5` |

## 消息格式

### PLAYER_LIST 事件

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

与 Paper 插件的 WebBridge 模块消息格式完全一致。
