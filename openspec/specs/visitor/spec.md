# Visitor Module

Protection mode for visitor/guest players with restricted interactions.

## Requirements

### Requirement: Visitor Detection

The plugin SHALL detect players in visitor LuckPerms groups.

#### Scenario: Group membership

- **WHEN** player is in configured visitor group
- **THEN** visitor restrictions are applied
- **THEN** visual/audio feedback is given

### Requirement: Interaction Restrictions

Visitors SHALL be restricted from certain actions.

#### Scenario: Block break restriction

- **WHEN** visitor tries to break block
- **AND** block-break restriction is enabled
- **THEN** action is cancelled

#### Scenario: Block place restriction

- **WHEN** visitor tries to place block
- **AND** block-place restriction is enabled
- **THEN** action is cancelled

#### Scenario: Container restriction

- **WHEN** visitor tries to open container
- **AND** container-open restriction is enabled
- **THEN** action is cancelled

#### Scenario: Entity damage restriction

- **WHEN** visitor tries to damage entity
- **AND** entity-damage restriction is enabled
- **THEN** damage is cancelled

### Requirement: Status Messages

Players SHALL receive feedback when entering/leaving visitor mode.

#### Scenario: Gain visitor status

- **WHEN** player gains visitor group
- **THEN** chat message, title, and sound are shown

#### Scenario: Lose visitor status

- **WHEN** player loses visitor group
- **THEN** chat message, title, and sound are shown

## Configuration

```yaml
visitor:
  enabled: false
  groups:
    - "visitor"
    - "guest"
  restrictions:
    block-break: true
    block-place: true
    item-use: true
    container-open: true
    pressure-plate: true
    entity-damage: true
  gained:
    chat: "&a[访客模式] &7已进入访客模式"
    title: "&a访客模式"
    subtitle: "&7已启用"
    sound: "entity.player.levelup"
  lost:
    chat: "&c[访客模式] &7已退出访客模式"
    title: "&c访客模式"
    subtitle: "&7已禁用"
    sound: "block.note_block.bass"
```
