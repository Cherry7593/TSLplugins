# Kiss Module

Player-to-player kiss interaction with particles and sound effects.

## Requirements

### Requirement: Kiss Command

The plugin SHALL allow players to kiss other players via `/tsl kiss <player>`.

#### Scenario: Successful kiss

- **WHEN** player executes `/tsl kiss <target>` with permission `tsl.kiss.use`
- **AND** target player has kiss enabled
- **AND** cooldown has expired
- **THEN** heart particles spawn between players
- **THEN** kiss sound effect plays
- **THEN** kiss count increments for initiator
- **THEN** kissed count increments for target

#### Scenario: Target has kiss disabled

- **WHEN** player tries to kiss a target who disabled kiss
- **THEN** error message is shown
- **THEN** no kiss occurs

### Requirement: Shift-Click Kiss

The plugin SHALL allow kissing via Shift+Right-click on player.

#### Scenario: Shift-click interaction

- **WHEN** player shift-right-clicks another player
- **AND** kiss feature is enabled
- **THEN** kiss executes same as command

### Requirement: Kiss Toggle

Players SHALL be able to toggle whether others can kiss them.

#### Scenario: Toggle kiss

- **WHEN** player executes `/tsl kiss toggle`
- **THEN** kiss acceptance state toggles
- **THEN** state persists in PDC

### Requirement: Kiss Statistics

The plugin SHALL track kiss counts via PAPI variables.

#### Scenario: PAPI variables

- **WHEN** `%tsl_kiss_count%` is requested
- **THEN** return number of times player kissed others
- **WHEN** `%tsl_kissed_count%` is requested
- **THEN** return number of times player was kissed

## Configuration

```yaml
kiss:
  enabled: false
  cooldown: 1.0  # seconds
```
