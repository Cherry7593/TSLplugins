# Freeze Module

Immobilize players temporarily.

## Requirements

### Requirement: Freeze Command

Operators SHALL be able to freeze players.

#### Scenario: Freeze player

- **WHEN** operator executes `/tsl freeze <player>` with permission `tsl.freeze.use`
- **AND** target does not have `tsl.freeze.bypass`
- **THEN** target player cannot move
- **THEN** target player cannot interact

#### Scenario: Freeze with duration

- **WHEN** operator executes `/tsl freeze <player> <seconds>`
- **THEN** player is frozen for specified duration
- **THEN** player automatically unfreezes after duration

### Requirement: Unfreeze Command

Operators SHALL be able to unfreeze players.

#### Scenario: Unfreeze player

- **WHEN** operator executes `/tsl freeze <player>` on frozen player
- **THEN** target player is unfrozen
- **THEN** movement is restored

### Requirement: Freeze Bypass

Players with bypass permission SHALL not be freezable.

#### Scenario: Bypass attempt

- **WHEN** operator tries to freeze player with `tsl.freeze.bypass`
- **THEN** freeze fails with message

## Configuration

```yaml
freeze:
  enabled: false
```
