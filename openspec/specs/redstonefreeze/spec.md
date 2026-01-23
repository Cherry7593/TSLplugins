# RedstoneFreeze Module

Freeze redstone and physics in a radius around player.

## Requirements

### Requirement: Freeze Toggle

Operators SHALL be able to freeze physics in an area.

#### Scenario: Enable freeze

- **WHEN** operator executes `/tsl redfreeze` with permission `tsl.redfreeze.use`
- **THEN** physics are frozen within max-radius
- **THEN** bossbar indicator is shown to nearby players

#### Scenario: Disable freeze

- **WHEN** operator executes `/tsl redfreeze` while active
- **THEN** physics are restored
- **THEN** bossbar is removed

### Requirement: Affected Components

Multiple physics components SHALL be freezable.

#### Scenario: Redstone signal

- **WHEN** redstone-signal is true in config
- **THEN** redstone signal changes are blocked

#### Scenario: Piston movement

- **WHEN** piston-extend/piston-retract are true
- **THEN** piston movements are blocked

#### Scenario: Block physics

- **WHEN** block-physics is true
- **THEN** block physics updates are blocked

#### Scenario: TNT/Explosions

- **WHEN** tnt-prime/explosion/tnt-spawn are true
- **THEN** TNT and explosion events are blocked

## Configuration

```yaml
redstone-freeze:
  enabled: false
  max-radius: 32
  bossbar-title: "§c❄ 当前区域物理已冻结 ❄"
  affected-components:
    redstone-signal: true
    piston-extend: true
    piston-retract: true
    block-physics: true
    tnt-prime: true
    explosion: true
    tnt-spawn: true
```
