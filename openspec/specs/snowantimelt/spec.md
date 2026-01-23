# SnowAntiMelt Module

Prevent snow layers from melting.

## Requirements

### Requirement: Snow Preservation

The plugin SHALL prevent snow layers from melting.

#### Scenario: Light-based melting

- **WHEN** snow would melt due to light level
- **AND** snow-anti-melt is enabled
- **THEN** snow layer is preserved

#### Scenario: Heat-based melting

- **WHEN** snow would melt due to nearby heat sources
- **AND** snow-anti-melt is enabled
- **THEN** snow layer is preserved

## Configuration

```yaml
snow-anti-melt:
  enabled: false
```
