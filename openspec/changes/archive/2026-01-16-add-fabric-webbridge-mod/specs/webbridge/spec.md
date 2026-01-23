# WebBridge Module - Fabric Implementation

Fabric Mod 实现，提供与 Paper 插件相同的 WebSocket 通信能力。

## ADDED Requirements

### Requirement: Fabric WebSocket Connection

The Fabric mod SHALL maintain a persistent WebSocket connection to configured server.

#### Scenario: Initial connection on server start

- **WHEN** Fabric server starts with mod enabled
- **THEN** WebSocket connection is established to configured URL
- **THEN** authentication token is included in connection URL

#### Scenario: Auto-reconnect on connection loss

- **WHEN** connection is lost
- **AND** auto-reconnect is enabled
- **THEN** reconnection is attempted at configured interval
- **THEN** attempts stop after max-reconnect-attempts

### Requirement: Player List Reporting

The Fabric mod SHALL report player list to web service.

#### Scenario: Periodic player list update

- **WHEN** player-list-interval passes
- **AND** WebSocket is connected
- **THEN** current player list is sent with serverId
- **THEN** message includes online count, player UUIDs and names

#### Scenario: Player join triggers update

- **WHEN** player joins Fabric server
- **THEN** player list update is sent within 1 second

#### Scenario: Player leave triggers update

- **WHEN** player leaves Fabric server
- **THEN** player list update is sent immediately

### Requirement: Server Performance Metrics

The Fabric mod SHALL include server performance in reports.

#### Scenario: TPS included in player list

- **WHEN** player list is sent
- **THEN** current server TPS is included

#### Scenario: MSPT included in player list

- **WHEN** player list is sent
- **THEN** current server MSPT is included

### Requirement: Heartbeat

The Fabric mod SHALL send periodic heartbeats.

#### Scenario: Heartbeat ping

- **WHEN** heartbeat-interval passes
- **AND** WebSocket is connected
- **THEN** heartbeat message is sent to server

## Configuration

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
