# Speed Module

Adjust player walk and fly speed multipliers.

## Requirements

### Requirement: Walk Speed Control

Operators SHALL be able to set player walk speed.

#### Scenario: Set walk speed

- **WHEN** player executes `/tsl speed walk <value>` with permission `tsl.speed.walk`
- **AND** value is within min-max multiplier range
- **THEN** player's walk speed is set to value

### Requirement: Fly Speed Control

Operators SHALL be able to set player fly speed.

#### Scenario: Set fly speed

- **WHEN** player executes `/tsl speed fly <value>` with permission `tsl.speed.fly`
- **AND** value is within min-max multiplier range
- **THEN** player's fly speed is set to value

### Requirement: Speed Limits

Speed values SHALL be clamped to configured range.

#### Scenario: Out of range value

- **WHEN** speed value is outside min-max range
- **THEN** value is clamped to nearest limit

## Configuration

```yaml
speed:
  enabled: false
  min-multiplier: 0.1
  max-multiplier: 10.0
```
