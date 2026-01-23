# Bind Module

QQ 账号绑定功能，支持绑定状态查询和 PAPI 变量。

## ADDED Requirements

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
- **THEN** return cached value from PDC storage

## Configuration

```yaml
bind-cache:
  enabled: true
  query-on-join: true
```
