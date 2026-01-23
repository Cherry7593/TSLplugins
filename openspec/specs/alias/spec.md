# Alias Module

Dynamic command aliases defined in configuration.

## Requirements

### Requirement: Alias Definition

Administrators SHALL be able to define command aliases in aliases.yml.

#### Scenario: Simple alias

- **WHEN** player executes alias command
- **THEN** configured target command is executed

#### Scenario: Alias with arguments

- **WHEN** alias supports argument placeholders
- **THEN** player arguments are substituted into target command

### Requirement: Alias Reload

Operators SHALL be able to reload aliases without restart.

#### Scenario: Reload aliases

- **WHEN** operator executes `/tsl aliasreload` with permission `tsl.alias.reload`
- **THEN** aliases.yml is reloaded
- **THEN** alias commands are re-registered

### Requirement: Dynamic Registration

Aliases SHALL be registered as actual commands at runtime.

#### Scenario: Command registration

- **WHEN** plugin loads aliases
- **THEN** each alias is registered as executable command
- **THEN** tab completion works for aliases

## Configuration

```yaml
# config.yml
alias:
  enabled: false

# aliases.yml (separate file)
aliases:
  spawn:
    command: "warp spawn"
  home:
    command: "essentials:home"
```
