## ADDED Requirements

### Requirement: Realtime Bind Status Update

The plugin SHALL listen for `BIND_STATUS_UPDATE` WebSocket events and update online player's bind cache in realtime.

#### Scenario: Receive bind event for online player

- **WHEN** WebSocket receives `BIND_STATUS_UPDATE` event with `action: "bind"`
- **AND** player with matching `mcUuid` is online
- **THEN** update player's bind cache with new status and QQ number
- **THEN** optionally send notification message to player

#### Scenario: Receive unbind event for online player

- **WHEN** WebSocket receives `BIND_STATUS_UPDATE` event with `action: "unbind"`
- **AND** player with matching `mcUuid` is online
- **THEN** clear player's bind cache (set bound=false, qqNumber="")
- **THEN** optionally send notification message to player

#### Scenario: Receive event for offline player

- **WHEN** WebSocket receives `BIND_STATUS_UPDATE` event
- **AND** player with matching `mcUuid` is not online
- **THEN** ignore the event without error
- **THEN** player will get updated status on next login via `QUERY_BIND_STATUS`

#### Scenario: Event message format

- **WHEN** parsing `BIND_STATUS_UPDATE` event
- **THEN** extract `mcUuid`, `mcName`, `qqNumber`, `action`, `source` from `data` object
- **THEN** `action` SHALL be either `"bind"` or `"unbind"`
- **THEN** `source` indicates origin: `"website"`, `"qq_group"`, `"game"`, or `"admin"`
