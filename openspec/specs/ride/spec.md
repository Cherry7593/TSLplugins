# Ride Module

Mount and ride any entity without saddle requirement.

## Requirements

### Requirement: Entity Mounting

Players SHALL be able to mount entities by shift-right-clicking.

#### Scenario: Mount entity

- **WHEN** player shift-right-clicks an entity with permission `tsl.ride.use`
- **AND** player has ride enabled
- **AND** entity type is not blacklisted
- **THEN** player becomes passenger of entity

#### Scenario: Blacklisted entity

- **WHEN** player tries to mount blacklisted entity without `tsl.ride.bypass`
- **THEN** mount is denied

### Requirement: Ride Toggle

Players SHALL be able to toggle their ride feature on/off.

#### Scenario: Toggle ride

- **WHEN** player executes `/tsl ride toggle`
- **THEN** ride state toggles and persists in PDC

## Configuration

```yaml
ride:
  enabled: false
  default_enabled: false
  blacklist:
    - WITHER
    - ENDER_DRAGON
    - WARDEN
    - GHAST
    - ELDER_GUARDIAN
    - VILLAGER
    - SHULKER
```
