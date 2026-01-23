# Mcedia Module

In-game video player using armor stands with configurable templates.

## Requirements

### Requirement: Player Creation

Operators SHALL be able to create video players.

#### Scenario: Create player

- **WHEN** operator executes `/tsl mcedia create <name>` with permission `tsl.mcedia.create`
- **AND** total players is below max-players
- **THEN** armor stand video player is spawned at operator location
- **THEN** player is registered in database

### Requirement: Player Management

Operators SHALL be able to manage existing players.

#### Scenario: Delete player

- **WHEN** operator executes `/tsl mcedia delete <name>` with permission `tsl.mcedia.delete`
- **THEN** video player entity is removed
- **THEN** player is removed from database

#### Scenario: List players

- **WHEN** operator executes `/tsl mcedia list` with permission `tsl.mcedia.list`
- **THEN** all registered players are displayed

#### Scenario: Teleport to player

- **WHEN** operator executes `/tsl mcedia tp <name>` with permission `tsl.mcedia.teleport`
- **THEN** operator is teleported to player location

### Requirement: Video Control

Operators SHALL be able to control video playback.

#### Scenario: Set video

- **WHEN** operator executes `/tsl mcedia set <name> <url>` with permission `tsl.mcedia.set`
- **THEN** video URL is set for player

### Requirement: Template System

Players SHALL support configuration templates stored in database.

#### Scenario: Apply template

- **WHEN** template is applied to player
- **THEN** scale, volume, and other settings are configured

### Requirement: Trigger Item

Players MAY be activated via configured item interaction.

#### Scenario: Item trigger

- **WHEN** player interacts with trigger-item
- **AND** nearby mcedia player exists
- **THEN** playback interaction occurs

## Configuration

```yaml
mcedia:
  enabled: false
  player-name-prefix: "mcedia"
  default-scale: 1.0
  default-volume: 1.0
  max-players: 50
  trigger-item: ""
```
