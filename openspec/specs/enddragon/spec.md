# EndDragon Module

Limit Ender Dragon destruction and damage capabilities.

## Requirements

### Requirement: Dragon Damage Prevention

The plugin SHALL prevent Ender Dragon from damaging blocks.

#### Scenario: Block destruction

- **WHEN** Ender Dragon would destroy blocks
- **AND** disable-damage is enabled
- **THEN** block destruction is cancelled

### Requirement: Configurable Protection

Protection SHALL be configurable via config.

#### Scenario: Enable protection

- **WHEN** enddragon.enabled is true
- **AND** disable-damage is true
- **THEN** dragon block damage is prevented

## Configuration

```yaml
enddragon:
  enabled: false
  disable-damage: true
```
