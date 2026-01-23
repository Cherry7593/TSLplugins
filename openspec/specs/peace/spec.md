# Peace Module

Pseudo-peaceful mode that prevents hostile mob spawning near players.

## Requirements

### Requirement: Mob Spawn Prevention

The plugin SHALL prevent hostile mobs from spawning near players with peace mode.

#### Scenario: Natural spawn prevention

- **WHEN** hostile mob would naturally spawn
- **AND** spawn location is within nospawn-radius of peace-mode player
- **THEN** spawn is cancelled

### Requirement: Periodic Scan

The plugin SHALL periodically check for peace mode status.

#### Scenario: Interval check

- **WHEN** scan-interval-ticks pass
- **THEN** peace mode status is verified for relevant players

## Configuration

```yaml
peace:
  enabled: false
  scan-interval-ticks: 20
  nospawn-radius: 48
```
