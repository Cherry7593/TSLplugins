## MODIFIED Requirements

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
