# RandomVariable Module

PAPI variables that return weighted random numbers from configured distributions.

## Requirements

### Requirement: Random Variable Definition

Administrators SHALL be able to define random variables with weighted ranges.

#### Scenario: Variable request

- **WHEN** `%tsl_random_<name>%` PAPI variable is requested
- **THEN** random value is generated from configured ranges
- **THEN** value respects configured precision

### Requirement: Weighted Distribution

Random values SHALL respect configured weights for each range.

#### Scenario: Weighted selection

- **WHEN** variable has multiple ranges with different weights
- **THEN** ranges are selected proportionally to their weights
- **THEN** value is uniformly distributed within selected range

## Configuration

```yaml
random-variable:
  enabled: false
  variables:
    scale_random:
      precision: 1
      ranges:
        - min: 0.1
          max: 1.0
          weight: 1.0
        - min: 1.0
          max: 20.0
          weight: 1.0
```
