# Vote Module

Player voting system to execute server commands.

## Requirements

### Requirement: Vote Initiation

Players SHALL be able to start votes for configured commands.

#### Scenario: Start vote

- **WHEN** player starts vote for configured option
- **AND** cooldown has expired
- **THEN** vote is broadcast to online players
- **THEN** voting period begins for configured duration

### Requirement: Vote Casting

Players SHALL be able to cast votes.

#### Scenario: Cast vote

- **WHEN** player casts vote during voting period
- **THEN** vote is recorded
- **THEN** vote count updates

### Requirement: Vote Resolution

Votes SHALL resolve after duration expires.

#### Scenario: Vote passes

- **WHEN** voting period ends
- **AND** yes percentage >= required percentage
- **THEN** configured command is executed

#### Scenario: Vote fails

- **WHEN** voting period ends
- **AND** yes percentage < required percentage
- **THEN** vote fails with message

### Requirement: Configurable Votes

Multiple vote types SHALL be configurable.

#### Scenario: Vote configuration

- **WHEN** vote type is configured
- **THEN** command, percentage, duration, description are set

## Configuration

```yaml
vote:
  enabled: false
  cooldown-seconds: 60
  default-duration-seconds: 30
  default-percentage: 0.5
  votes:
    day:
      command: "time set day"
      percentage: 0.3
      duration: 30
      description: "切换为白天"
      permission: ""
    clear:
      command: "weather clear"
      percentage: 0.3
      duration: 30
      description: "天气晴朗"
      permission: ""
```
