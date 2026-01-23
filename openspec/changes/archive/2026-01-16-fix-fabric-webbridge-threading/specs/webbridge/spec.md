## ADDED Requirements

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
