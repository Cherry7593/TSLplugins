# Spec Module

Spectator mode with automatic return after delay.

## Requirements

### Requirement: Enter Spectator Mode

Operators SHALL be able to enter spectator mode temporarily.

#### Scenario: Start spectating

- **WHEN** operator executes `/tsl spec start [delay]` with permission `tsl.spec.use`
- **THEN** player enters spectator gamemode
- **THEN** original location and gamemode are saved
- **THEN** timer starts for automatic return

#### Scenario: Custom delay

- **WHEN** delay is specified within min-max range
- **THEN** return timer uses specified delay

### Requirement: Exit Spectator Mode

Players SHALL be able to exit spectator mode early.

#### Scenario: Stop spectating

- **WHEN** player executes `/tsl spec stop`
- **THEN** player is teleported to saved location
- **THEN** original gamemode is restored

### Requirement: Auto Return

Players SHALL automatically return after delay expires.

#### Scenario: Timer expiry

- **WHEN** spectator timer expires
- **THEN** player is teleported to saved location
- **THEN** original gamemode is restored

### Requirement: Whitelist Management

Operators SHALL be able to manage spec whitelist.

#### Scenario: Add to whitelist

- **WHEN** operator executes `/tsl spec add <player>`
- **THEN** player is added to spec whitelist

#### Scenario: Remove from whitelist

- **WHEN** operator executes `/tsl spec remove <player>`
- **THEN** player is removed from spec whitelist

## Configuration

```yaml
spec:
  enabled: false
  defaultDelay: 30
  minDelay: 10
  maxDelay: 300
  whitelist: []
```
