# Neko Module

Add configurable suffix to player chat messages (cat-girl mode).

## Requirements

### Requirement: Neko Status Management

Operators SHALL be able to set players as neko.

#### Scenario: Set neko

- **WHEN** operator executes `/tsl neko set <player>` with permission `tsl.neko.set`
- **THEN** player is marked as neko
- **THEN** suffix will be added to their chat

#### Scenario: Reset neko

- **WHEN** operator executes `/tsl neko reset <player>` with permission `tsl.neko.reset`
- **THEN** player's neko status is removed

#### Scenario: List nekos

- **WHEN** operator executes `/tsl neko list` with permission `tsl.neko.list`
- **THEN** all players with neko status are listed

### Requirement: Chat Suffix

Neko players SHALL have suffix appended to chat messages.

#### Scenario: Chat message

- **WHEN** neko player sends chat message
- **THEN** configured suffix is appended to message

### Requirement: Periodic Scan

The plugin SHALL periodically verify neko status.

#### Scenario: Status scan

- **WHEN** scan-interval-ticks pass
- **THEN** neko status is verified for active players

## Configuration

```yaml
neko:
  enabled: false
  suffix: "喵~"
  scan-interval-ticks: 20
```
