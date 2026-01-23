# BlockStats Module

Track and expose block placement statistics via PAPI.

## Requirements

### Requirement: Block Placement Tracking

The plugin SHALL track blocks placed by each player.

#### Scenario: Block placed

- **WHEN** player places a block
- **THEN** placement count is incremented
- **THEN** data is persisted

### Requirement: PAPI Statistics

Block statistics SHALL be available via PAPI.

#### Scenario: Total blocks variable

- **WHEN** `%tsl_blocks_placed_total%` is requested
- **THEN** total blocks placed by player is returned

## Configuration

```yaml
blockstats:
  enabled: false
```
