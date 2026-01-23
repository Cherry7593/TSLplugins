# PlayTime Module

Track player online time with periodic saves.

## Requirements

### Requirement: Time Tracking

The plugin SHALL track cumulative online time for each player.

#### Scenario: Player online

- **WHEN** player is online
- **THEN** playtime counter increments

### Requirement: Periodic Save

Playtime data SHALL be saved periodically.

#### Scenario: Save interval

- **WHEN** save-interval-ticks pass
- **THEN** all player playtimes are persisted

### Requirement: Timezone Support

Playtime calculations SHALL respect configured timezone.

#### Scenario: Timezone handling

- **WHEN** daily/session stats are calculated
- **THEN** configured timezone is used

## Configuration

```yaml
playtime:
  enabled: false
  save-interval-ticks: 6000
  timezone: "Asia/Shanghai"
```
