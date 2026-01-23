# NewbieTag Module

Display different tags for new vs veteran players based on playtime.

## Requirements

### Requirement: Playtime-Based Tags

Players SHALL display different tags based on their playtime.

#### Scenario: New player

- **WHEN** player's playtime is below threshold hours
- **THEN** newbie tag is displayed

#### Scenario: Veteran player

- **WHEN** player's playtime exceeds threshold hours
- **THEN** veteran tag is displayed

## Configuration

```yaml
newbieTag:
  enabled: false
  thresholdHours: 24
  newbieTag: "✨"
  veteranTag: "⚡"
```
