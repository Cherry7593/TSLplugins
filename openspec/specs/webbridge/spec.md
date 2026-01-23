# WebBridge Module

## Purpose

Bidirectional communication between Minecraft server and external web services via WebSocket.
## Requirements
### Requirement: WebSocket Connection

The plugin SHALL maintain a persistent WebSocket connection to configured server.

#### Scenario: Initial connection

- **WHEN** plugin enables with webbridge enabled
- **THEN** WebSocket connection is established to configured URL
- **THEN** authentication token is sent

#### Scenario: Auto-reconnect on disconnect

- **WHEN** connection is lost
- **AND** auto-reconnect is enabled
- **THEN** reconnection is attempted every 30 seconds
- **THEN** reconnection attempts continue indefinitely until connection succeeds or client is stopped

#### Scenario: Auto-reconnect with max attempts (backward compatible)

- **WHEN** connection is lost
- **AND** auto-reconnect is enabled
- **AND** max-reconnect-attempts is greater than 0
- **THEN** reconnection is attempted at configured interval
- **THEN** attempts stop after max-reconnect-attempts reached

#### Scenario: Plugin reconnect behavior

- **WHEN** WebSocket connection closes unexpectedly
- **THEN** plugin schedules reconnection using Folia-compatible scheduler
- **THEN** reconnection is attempted after reconnect-interval seconds

#### Scenario: Mod reconnect behavior

- **WHEN** WebSocket connection closes unexpectedly
- **THEN** mod schedules reconnection using ScheduledExecutorService
- **THEN** reconnection is attempted after reconnect-interval seconds

### Requirement: Web-to-Game Messages

Messages from web SHALL be relayed to in-game chat.

#### Scenario: Incoming message

- **WHEN** message is received from WebSocket
- **THEN** message is formatted using web-to-game-format
- **THEN** message is broadcast to online players

### Requirement: Game-to-Web Messages

In-game events SHALL be sent to web service.

#### Scenario: Player chat

- **WHEN** player sends chat message
- **THEN** message is forwarded to WebSocket server

#### Scenario: Player list updates

- **WHEN** player-list-interval passes
- **THEN** current player list is sent to WebSocket server

### Requirement: Heartbeat

The plugin SHALL send periodic heartbeats to maintain connection.

#### Scenario: Heartbeat ping

- **WHEN** heartbeat-interval passes
- **THEN** heartbeat message is sent to server

#### Scenario: Heartbeat logging (Fabric Mod)

- **WHEN** heartbeat message is sent
- **AND** debug is false (default)
- **THEN** no log message is printed to console

#### Scenario: Heartbeat logging debug mode (Fabric Mod)

- **WHEN** heartbeat message is sent
- **AND** debug is true
- **THEN** log message is printed to console at INFO level

### Requirement: Fabric Mod Thread Safety

Fabric mod SHALL collect server data on the server main thread before sending to WebSocket.

#### Scenario: Player list collection

- **WHEN** player list update is triggered
- **THEN** player data is collected on server main thread via `server.execute()`
- **THEN** collected data is serialized and enqueued for sending

#### Scenario: Heartbeat collection

- **WHEN** heartbeat interval passes
- **THEN** server ID is collected on server main thread
- **THEN** heartbeat message is enqueued for sending

### Requirement: QQ Command Relay

The plugin SHALL execute commands received from QQ group via WebSocket and return execution results.

#### Scenario: Receive command execution request

- **WHEN** WebSocket message with `action: "QQ_COMMAND_EXECUTE"` is received
- **AND** `serverId` is null or matches this server's ID
- **THEN** command is executed as console
- **THEN** `QQ_COMMAND_RESULT` response is sent with execution status

#### Scenario: Ignore command for other servers

- **WHEN** WebSocket message with `action: "QQ_COMMAND_EXECUTE"` is received
- **AND** `serverId` does not match this server's ID
- **THEN** message is ignored
- **THEN** no response is sent

#### Scenario: Command execution success

- **WHEN** command is executed successfully
- **THEN** response `success` is `true`
- **THEN** response `output` contains command feedback if available
- **THEN** response `error` is `null`

#### Scenario: Command execution failure

- **WHEN** command execution fails or throws exception
- **THEN** response `success` is `false`
- **THEN** response `error` contains error message
- **THEN** response `output` is `null`

#### Scenario: Command logging

- **WHEN** command is received for execution
- **THEN** command details are logged for audit purposes
- **THEN** log includes requestId, command, qqNumber, groupId

### Requirement: QQ Command Response Format

The plugin SHALL send command execution results in the specified JSON format.

#### Scenario: Response message structure

- **WHEN** command execution completes
- **THEN** response includes `type: "response"`
- **THEN** response includes `source: "minecraft"`
- **THEN** response includes original `requestId`
- **THEN** response `data.action` is `"QQ_COMMAND_RESULT"`
- **THEN** response `data.executedAt` is execution timestamp in milliseconds

### Requirement: Fabric Mod Debug Configuration

The Fabric mod SHALL support a debug configuration option to control verbose logging.

#### Scenario: Debug disabled (default)

- **WHEN** debug is false or not configured
- **THEN** routine messages (heartbeat, player list, sent messages) are logged at FINE level only
- **THEN** console remains clean with only important events

#### Scenario: Debug enabled

- **WHEN** debug is true
- **THEN** routine messages are logged at INFO level
- **THEN** detailed information is visible in console for troubleshooting

#### Scenario: Configuration file

- **WHEN** tsl-webbridge.json is loaded
- **THEN** debug field is read with default value false

## Configuration

```yaml
webbridge:
  enabled: false
  server-id: ""
  websocket:
    url: "ws://127.0.0.1:4001/mc-bridge"
    token: ""
  auto-reconnect: true
  reconnect-interval: 30
  max-reconnect-attempts: 5
  player-list-interval: 30
  heartbeat-interval: 30
  web-to-game-format: "&7[&b{source}&7] &f<{playerName}> &7{message}"
```
