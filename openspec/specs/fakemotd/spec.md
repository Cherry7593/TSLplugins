# FakePlayerMotd Module

Display fake player count in server MOTD.

## Requirements

### Requirement: Fake Player Count

The plugin SHALL add fake players to MOTD player count.

#### Scenario: Server ping

- **WHEN** server is pinged by client
- **AND** fakeplayer is enabled
- **THEN** online player count is increased by configured amount

## Configuration

```yaml
fakeplayer:
  enabled: false
  count: 3
```
