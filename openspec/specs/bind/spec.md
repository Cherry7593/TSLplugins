# Bind Module

## Purpose

QQ 账号绑定功能，支持绑定状态查询和 PAPI 变量。
## Requirements
### Requirement: Bind Status PAPI Variable

The plugin SHALL expose bind status via PAPI variable.

#### Scenario: Query bind status - bound player

- **WHEN** `%tsl_bind%` is requested for a player
- **AND** player has bound QQ account
- **THEN** return `true`

#### Scenario: Query bind status - unbound player

- **WHEN** `%tsl_bind%` is requested for a player
- **AND** player has not bound QQ account
- **THEN** return `false`

#### Scenario: Query bind status - unknown status

- **WHEN** `%tsl_bind%` is requested for a player
- **AND** bind status cache is not available
- **THEN** return `false` (default)

### Requirement: Bind QQ PAPI Variable

The plugin SHALL expose bound QQ number via PAPI variable.

#### Scenario: Query bound QQ - bound player

- **WHEN** `%tsl_bind_qq%` is requested for a player
- **AND** player has bound QQ account
- **THEN** return the bound QQ number

#### Scenario: Query bound QQ - unbound player

- **WHEN** `%tsl_bind_qq%` is requested for a player
- **AND** player has not bound QQ account
- **THEN** return empty string

### Requirement: Bind Cache Persistence

The plugin SHALL cache bind status locally for offline access.

#### Scenario: Cache on join

- **WHEN** player joins server
- **AND** WebBridge is connected
- **THEN** query bind status from Web
- **THEN** update local cache with result

#### Scenario: Cache on bind success

- **WHEN** player successfully binds QQ account
- **THEN** immediately update local cache with bound status and QQ number

#### Scenario: Cache on unbind success

- **WHEN** player successfully unbinds QQ account
- **THEN** immediately update local cache to unbound status

#### Scenario: Offline player query

- **WHEN** PAPI variable is requested for offline player
- **THEN** return cached value from YML storage

### Requirement: Clickable Bind Code Copy

When player requests QQ binding, the verification code SHALL be displayed as a clickable component that copies the bind command to clipboard.

#### Scenario: Player requests bind code

- **WHEN** player executes `/tsl bind` command
- **AND** WebSocket returns verification code successfully
- **THEN** display bind command with clickable copy functionality
- **THEN** clicking the code SHALL copy "绑定 {CODE}" to player's clipboard
- **THEN** display hover text indicating the code is clickable to copy

#### Scenario: Code display styling

- **WHEN** verification code is displayed
- **THEN** code text SHALL be highlighted with bold and distinctive color
- **THEN** hover text SHALL show "点击复制绑定口令"

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

## Configuration

```yaml
bind-cache:
  enabled: true
  query-on-join: true
```
