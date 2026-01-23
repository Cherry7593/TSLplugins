# Vault Module

Periodic reset of vault blocks with epoch tracking.

## Requirements

### Requirement: Vault Reset Tracking

The plugin SHALL track vault reset epochs.

#### Scenario: Epoch check

- **WHEN** reset-interval-days pass since last-reset-timestamp
- **THEN** current-epoch increments
- **THEN** last-reset-timestamp updates

### Requirement: Abnormal Detection

The plugin SHALL detect abnormally high vault values.

#### Scenario: Abnormal threshold

- **WHEN** vault value exceeds abnormal-threshold
- **AND** debug is enabled
- **THEN** warning is logged

## Configuration

```yaml
vault:
  enabled: false
  current-epoch: 1
  reset-interval-days: 15
  last-reset-timestamp: 0
  abnormal-threshold: 100000
  debug: false
```
