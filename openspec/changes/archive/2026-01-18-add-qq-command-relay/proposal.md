# Change: Add QQ Command Relay

## Why

允许玩家在 QQ 群内发送触发词，系统自动在 MC 服务器执行对应命令并返回结果。这是 WebBridge 模块的扩展功能，实现 QQ 群与游戏服务器的指令联动。

## What Changes

- **ADDED**: 处理 `QQ_COMMAND_EXECUTE` 请求消息，执行 Web 端下发的命令
- **ADDED**: 返回 `QQ_COMMAND_RESULT` 响应消息，包含执行结果
- **ADDED**: 支持命令输出捕获（可选），用于返回命令执行反馈
- **ADDED**: 支持目标服务器 ID 过滤，仅处理发给本服务器的命令

## Impact

- Affected specs: `webbridge`
- Affected code:
  - `WebBridgeClient.kt` - 添加 `QQ_COMMAND_EXECUTE` 消息处理
  - `WebBridgeManager.kt` - 添加命令执行逻辑和结果返回方法

## Flow

```text
QQ群消息 → Bot → WebSocket Bridge → MC插件 → 执行命令 → 返回结果 → Bot回复群内
```

## Protocol

### Request (Web → MC): `QQ_COMMAND_EXECUTE`

```json
{
  "type": "request",
  "source": "web",
  "requestId": "cmd-xxx",
  "data": {
    "action": "QQ_COMMAND_EXECUTE",
    "command": "say hello",
    "serverId": "server-uuid",
    "qqNumber": "12345****",
    "groupId": "987654321",
    "commandId": "123",
    "timeout": 10000
  }
}
```

### Response (MC → Web): `QQ_COMMAND_RESULT`

```json
{
  "type": "response",
  "source": "minecraft",
  "requestId": "cmd-xxx",
  "data": {
    "action": "QQ_COMMAND_RESULT",
    "success": true,
    "output": "命令输出",
    "error": null,
    "executedAt": 1705567890456
  }
}
```
