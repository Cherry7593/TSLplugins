# Near Module

Find nearby players within a specified radius.

## Requirements

### Requirement: Near Query

Players SHALL be able to find nearby players.

#### Scenario: Default radius

- **WHEN** player executes `/tsl near` with permission `tsl.near.use`
- **THEN** list of players within default radius is shown
- **THEN** distance to each player is displayed

#### Scenario: Custom radius

- **WHEN** player executes `/tsl near <radius>`
- **AND** radius is within max limit (or has `tsl.near.bypass`)
- **THEN** list of players within specified radius is shown

#### Scenario: No nearby players

- **WHEN** no players are within radius
- **THEN** appropriate message is shown

## Configuration

```yaml
near:
  enabled: false
  defaultRadius: 1000
  maxRadius: 1000
```
