# Permission Module

Rule-based permission checking using PAPI variables.

## Requirements

### Requirement: Rule Definition

Administrators SHALL be able to define permission rules.

#### Scenario: Variable-based rule

- **WHEN** rule checks PAPI variable against value
- **THEN** player is assigned to target-group if match

### Requirement: Rule Modes

Rules SHALL support different action modes.

#### Scenario: Set mode

- **WHEN** mode is "set"
- **AND** variable matches value
- **THEN** player is set to target-group

### Requirement: Command Execution

Rules MAY optionally execute commands.

#### Scenario: Execute commands

- **WHEN** execute-commands is true
- **AND** rule triggers
- **THEN** configured commands are executed

## Configuration

```yaml
permission-checker:
  enabled: false
  rules:
    whitelist-check:
      variable: "%player_is_whitelisted%"
      value: "true"
      target-group: "normal"
      mode: "set"
      execute-commands: false
      commands: []
```
