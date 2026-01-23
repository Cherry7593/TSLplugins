# Title Module

Player title/prefix system with LuckPerms integration.

## Requirements

### Requirement: Title Display

Players SHALL have titles displayed based on LuckPerms metadata.

#### Scenario: Title prefix

- **WHEN** player has title configured
- **THEN** title is displayed as prefix in chat/tab

### Requirement: LuckPerms Priority

Titles SHALL respect LuckPerms priority weight.

#### Scenario: Priority ordering

- **WHEN** player has multiple potential titles
- **THEN** title with highest priority (luckperms-priority) is used

### Requirement: Join Delay

Title application SHALL be delayed after join.

#### Scenario: Join handling

- **WHEN** player joins server
- **THEN** title is applied after join-delay ticks

## Configuration

```yaml
title:
  enabled: false
  luckperms-priority: 100
  join-delay: 20
```
