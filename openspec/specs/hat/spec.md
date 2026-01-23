# Hat Module

Wear any held item as a hat (helmet slot).

## Requirements

### Requirement: Hat Command

Players SHALL be able to wear held items as hats.

#### Scenario: Wear hat

- **WHEN** player executes `/tsl hat` with permission `tsl.hat.use`
- **AND** player is holding an item
- **AND** item is not blacklisted
- **AND** cooldown has expired
- **THEN** held item moves to helmet slot
- **THEN** existing helmet (if any) moves to hand

#### Scenario: Blacklisted item

- **WHEN** player tries to wear blacklisted item
- **THEN** action is denied with message

#### Scenario: Empty hand

- **WHEN** player executes `/tsl hat` with empty hand
- **THEN** error message is shown

## Configuration

```yaml
hat:
  enabled: false
  cooldown: 0.0
  blacklist:
    - COMMAND_BLOCK
```
