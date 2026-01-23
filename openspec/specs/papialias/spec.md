# PapiAlias Module

Map PAPI variable names to other variables.

## Requirements

### Requirement: Variable Mapping

The plugin SHALL map custom PAPI variables to other variables.

#### Scenario: Alias request

- **WHEN** `%tsl_alias_<name>%` PAPI variable is requested
- **AND** mapping exists for name
- **THEN** mapped variable is resolved and returned

#### Scenario: Missing mapping

- **WHEN** mapping does not exist
- **AND** return-original-if-not-found is true
- **THEN** original variable name is returned

## Configuration

```yaml
papi-alias:
  enabled: false
  return-original-if-not-found: true
  mappings: {}
```
