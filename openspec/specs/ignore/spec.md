# Ignore Module

Block chat messages from specific players.

## Requirements

### Requirement: Ignore Player

Players SHALL be able to ignore other players' chat messages.

#### Scenario: Add to ignore list

- **WHEN** player executes `/tsl ignore <player>`
- **AND** ignore list is below max count
- **THEN** target is added to ignore list
- **THEN** target's messages are hidden from player

#### Scenario: Max ignore limit

- **WHEN** player tries to ignore with full list
- **THEN** error message is shown

### Requirement: Unignore Player

Players SHALL be able to remove players from ignore list.

#### Scenario: Remove from ignore list

- **WHEN** player executes `/tsl ignore <player>` for already ignored player
- **THEN** target is removed from ignore list
- **THEN** target's messages are visible again

### Requirement: Ignore List Persistence

Ignore lists SHALL persist across server restarts.

#### Scenario: Data persistence

- **WHEN** server restarts
- **THEN** ignore lists are restored from storage

## Configuration

```yaml
ignore:
  enabled: false
  max-ignore-count: 100
```
