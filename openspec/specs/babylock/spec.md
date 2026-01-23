# BabyLock Module

Keep entities permanently in baby/young form using name tags.

## Requirements

### Requirement: Baby Lock Trigger

Entities SHALL become permanently young when named with specific prefixes.

#### Scenario: Name with prefix

- **WHEN** player uses name tag with configured prefix on entity
- **AND** entity type supports baby form
- **THEN** entity is set to baby
- **THEN** entity is marked to stay baby permanently

#### Scenario: Prevent aging

- **WHEN** baby-locked entity would normally age
- **THEN** entity remains in baby form

### Requirement: Despawn Prevention

Baby-locked entities SHALL optionally be prevented from despawning.

#### Scenario: Prevent despawn

- **WHEN** prevent_despawn is enabled
- **AND** entity is baby-locked
- **THEN** entity cannot despawn naturally

### Requirement: Entity Type Filter

Only configured entity types SHALL be affected.

#### Scenario: Enabled types filter

- **WHEN** enabled_types is not empty
- **AND** entity type is not in list
- **THEN** entity is not affected by baby lock

## Configuration

```yaml
babylock:
  enabled: false
  prefixes:
    - "[幼]"
    - "[小]"
    - "[Baby]"
  case_sensitive: false
  prevent_despawn: true
  enabled_types: []
```
