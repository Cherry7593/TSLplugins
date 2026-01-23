# FixGhost Module

Fix ghost/phantom blocks around player.

## Requirements

### Requirement: Ghost Block Fix

Players SHALL be able to refresh blocks around them.

#### Scenario: Fix ghost blocks

- **WHEN** player executes `/tsl fixghost` with permission `tsl.fixghost.use`
- **AND** cooldown has expired
- **THEN** blocks within default radius are refreshed to client
- **THEN** cooldown starts

#### Scenario: Custom radius

- **WHEN** player executes `/tsl fixghost <radius>`
- **AND** radius is within max limit
- **THEN** blocks within specified radius are refreshed

#### Scenario: Cooldown active

- **WHEN** player uses fixghost during cooldown
- **THEN** error message with remaining time is shown

## Configuration

```yaml
fixghost:
  enabled: false
  default-radius: 5
  max-radius: 5
  cooldown: 5.0
```
