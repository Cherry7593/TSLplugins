# Scale Module

Adjust player body size/scale.

## Requirements

### Requirement: Scale Command

Players SHALL be able to adjust their body scale.

#### Scenario: Set scale

- **WHEN** player executes `/tsl scale <value>` with permission `tsl.scale.use`
- **AND** value is within min-max range (or has `tsl.scale.bypass`)
- **THEN** player's scale attribute is set to value

#### Scenario: Reset scale

- **WHEN** player executes `/tsl scale reset`
- **THEN** player's scale resets to 1.0

#### Scenario: Out of range

- **WHEN** player sets scale outside allowed range without bypass
- **THEN** scale is clamped to min/max limits

## Configuration

```yaml
scale:
  enabled: false
  min: 0.8
  max: 1.1
```
