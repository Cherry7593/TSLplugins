# PlayerCountCmd Module

Execute commands when player count crosses thresholds.

## Requirements

### Requirement: Threshold Commands

The plugin SHALL execute commands when player count changes.

#### Scenario: Below lower threshold

- **WHEN** player count drops below lower-threshold
- **AND** min-interval has passed since last execution
- **THEN** command-when-low is executed

#### Scenario: Above upper threshold

- **WHEN** player count rises above upper-threshold
- **AND** min-interval has passed since last execution
- **THEN** command-when-high is executed

### Requirement: Rate Limiting

Commands SHALL not execute more frequently than min-interval.

#### Scenario: Rapid changes

- **WHEN** player count fluctuates rapidly
- **THEN** commands execute at most once per min-interval-ms

## Configuration

```yaml
player-count-cmd:
  enabled: false
  upper-threshold: 52
  lower-threshold: 48
  min-interval-ms: 10000
  command-when-low: "chunky continue"
  command-when-high: "chunky pause"
```
