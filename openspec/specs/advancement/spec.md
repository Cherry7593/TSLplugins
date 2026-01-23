# Advancement Module

Filter advancement messages and track player achievement counts.

## Requirements

### Requirement: Advancement Message Filtering

The plugin SHALL filter/suppress advancement broadcast messages.

#### Scenario: Advancement earned

- **WHEN** player earns advancement
- **AND** advancement filtering is enabled
- **THEN** broadcast message is suppressed or filtered

### Requirement: Advancement Count

The plugin SHALL track player advancement counts.

#### Scenario: Count query

- **WHEN** `%tsl_adv_count%` PAPI variable is requested
- **THEN** player's completed advancement count is returned

### Requirement: Count Refresh

Operators SHALL be able to refresh advancement counts.

#### Scenario: Refresh single player

- **WHEN** operator executes `/tsl advcount refresh <player>`
- **THEN** player's advancement count is recalculated

#### Scenario: Refresh all

- **WHEN** operator executes `/tsl advcount refresh all`
- **THEN** all players' advancement counts are recalculated

## Configuration

```yaml
advancement:
  enabled: false
```
