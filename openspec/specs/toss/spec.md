# Toss Module

## Purpose

Lift and throw entities (mobs) with configurable velocity.
## Requirements
### Requirement: Entity Lifting

Players SHALL be able to pick up entities by right-clicking them.

#### Scenario: Lift entity

- **WHEN** player right-clicks an entity with permission `tsl.toss.use`
- **AND** player has toss enabled
- **AND** entity type is not blacklisted
- **AND** lift count is below max_lift_count
- **THEN** entity becomes passenger of player

#### Scenario: Blacklisted entity

- **WHEN** player tries to lift blacklisted entity without `tsl.toss.bypass`
- **THEN** lift is denied with message

#### Scenario: Lift stacked entity without player holder

- **WHEN** player right-clicks an entity that is stacked on another entity (has vehicle)
- **AND** the entity is NOT held by any player
- **AND** other lift conditions are met
- **THEN** entity is removed from its current vehicle
- **AND** entity becomes passenger of player

#### Scenario: Cannot lift entity held by player

- **WHEN** player tries to lift an entity that is in another player's passenger chain
- **THEN** lift is denied with message

### Requirement: Entity Throwing

Players SHALL throw lifted entities by left-clicking.

#### Scenario: Throw entity

- **WHEN** player left-clicks while carrying entity
- **THEN** entity is ejected with configured velocity
- **THEN** entity travels in player's look direction

### Requirement: Toss Toggle

Players SHALL be able to toggle their toss feature on/off.

#### Scenario: Toggle toss

- **WHEN** player executes `/tsl toss toggle`
- **THEN** toss state toggles and persists in PDC

### Requirement: Velocity Configuration

Players SHALL be able to set their throw velocity.

#### Scenario: Set velocity

- **WHEN** player executes `/tsl toss velocity <value>` with `tsl.toss.velocity`
- **AND** value is within min-max range (or has `tsl.toss.velocity.bypass`)
- **THEN** velocity is saved to PDC

## Configuration

```yaml
toss:
  enabled: false
  show_messages: false
  blacklist:
    - WITHER
    - ENDER_DRAGON
    - WARDEN
    - VILLAGER
    - SHULKER
  max_lift_count: 5
  default_enabled: false
  throw_velocity:
    min: 1.0
    max: 3.0
```
