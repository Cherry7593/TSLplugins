# TimedAttribute Module

Temporary attribute modifiers with duration-based expiration.

## Requirements

### Requirement: Add Timed Attribute

Operators SHALL be able to add temporary attribute effects.

#### Scenario: Add attribute

- **WHEN** operator executes `/tsl attr add <player> <attribute> <value> <duration>` with permission `tsl.attribute.add`
- **THEN** attribute modifier is applied to player
- **THEN** effect expires after duration seconds

### Requirement: Set Base Attribute

Operators SHALL be able to set attribute base values.

#### Scenario: Set base

- **WHEN** operator executes `/tsl attr set <player> <attribute> <value>` with permission `tsl.attribute.set`
- **THEN** attribute base value is set

### Requirement: Remove Attribute

Operators SHALL be able to remove attribute effects early.

#### Scenario: Remove effect

- **WHEN** operator executes `/tsl attr remove <player> <attribute>` with permission `tsl.attribute.remove`
- **THEN** attribute modifier is removed

### Requirement: List Attributes

Operators SHALL be able to view active attribute effects.

#### Scenario: List effects

- **WHEN** operator executes `/tsl attr list <player>` with permission `tsl.attribute.list`
- **THEN** all active timed attributes with remaining duration are shown

### Requirement: Clear Attributes

Operators SHALL be able to clear all effects from player.

#### Scenario: Clear all

- **WHEN** operator executes `/tsl attr clear <player>` with permission `tsl.attribute.clear`
- **THEN** all timed attribute effects are removed

### Requirement: Periodic Expiration Check

The plugin SHALL periodically check for expired effects.

#### Scenario: Expiration scan

- **WHEN** scan-interval-ticks pass
- **THEN** expired effects are removed from players

## Configuration

```yaml
timed-attribute:
  enabled: false
  scan-interval-ticks: 20
```
