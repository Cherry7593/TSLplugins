# FarmProtect Module

Prevent farmland from being trampled.

## Requirements

### Requirement: Trample Prevention

The plugin SHALL prevent farmland from being trampled.

#### Scenario: Player trampling

- **WHEN** player jumps on farmland
- **AND** farmprotect is enabled
- **THEN** farmland is not converted to dirt

#### Scenario: Entity trampling

- **WHEN** entity lands on farmland
- **AND** farmprotect is enabled
- **THEN** farmland is not converted to dirt

## Configuration

```yaml
farmprotect:
  enabled: false
```
