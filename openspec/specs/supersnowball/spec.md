# SuperSnowball Module

Enhanced snowball with explosion effects, knockback, and snow coverage.

## Requirements

### Requirement: Give Snowball

Operators SHALL be able to give super snowballs to players.

#### Scenario: Give item

- **WHEN** operator executes `/tsl ss give <player>` with permission `tsl.ss.give`
- **THEN** super snowball item is given to player

### Requirement: Enhanced Effects

Super snowballs SHALL have enhanced effects on impact.

#### Scenario: Impact effects

- **WHEN** super snowball hits ground with permission `tsl.ss.use`
- **THEN** snow layers spawn within snow-radius
- **THEN** entities within knockback-radius are knocked back
- **THEN** impact particles spawn
- **THEN** impact sounds play

### Requirement: Custom Model

Super snowballs SHALL optionally use custom item models.

#### Scenario: Custom model

- **WHEN** use-custom-model is true
- **THEN** snowball uses configured custom-model-key

### Requirement: Freeze Effect

Impacted entities SHALL be frozen temporarily.

#### Scenario: Entity freeze

- **WHEN** entity is hit by super snowball
- **THEN** entity is frozen for freeze-ticks duration

## Configuration

```yaml
super-snowball:
  enabled: false
  snow-radius: 5
  knockback-radius: 7.0
  knockback-strength: 1.5
  gravity: 0.06
  velocity-multiplier: 0.6
  trail-particle-count: 3
  trail-particle-interval: 2
  impact-particle-count: 50
  snow-layer-chance: 0.7
  max-snow-layers: 3
  freeze-ticks: 100
  use-custom-model: true
  custom-model-key: "cris_tsl:big_snow_ball"
  impact-sounds: []
```
