# Maintenance Module

Server maintenance mode with configurable MOTD and kick messages.

## Requirements

### Requirement: Maintenance Toggle

Operators SHALL be able to toggle maintenance mode.

#### Scenario: Enable maintenance

- **WHEN** operator executes `/tsl maintenance` with permission `tsl.maintenance.toggle`
- **AND** maintenance is off
- **THEN** maintenance mode is enabled
- **THEN** non-whitelisted players are kicked

#### Scenario: Disable maintenance

- **WHEN** operator executes `/tsl maintenance`
- **AND** maintenance is on
- **THEN** maintenance mode is disabled

### Requirement: Login Blocking

Non-whitelisted players SHALL be blocked during maintenance.

#### Scenario: Non-whitelisted login

- **WHEN** player without `tsl.maintenance.bypass` tries to join
- **AND** maintenance is enabled
- **THEN** player is kicked with kick-message

#### Scenario: Whitelisted login

- **WHEN** player with `tsl.maintenance.bypass` tries to join
- **AND** maintenance is enabled
- **THEN** player is allowed to join

### Requirement: Maintenance MOTD

Server MOTD SHALL change during maintenance.

#### Scenario: MOTD display

- **WHEN** server is pinged during maintenance
- **THEN** maintenance MOTD is shown
- **THEN** version text shows maintenance status
- **THEN** fake player counts are displayed if configured

### Requirement: Whitelist Management

Operators SHALL be able to manage maintenance whitelist.

#### Scenario: Add to whitelist

- **WHEN** operator adds player to maintenance.yml whitelist
- **THEN** player can join during maintenance

## Configuration

```yaml
maintenance:
  enabled: false
  kick-message:
    - "&c&l⚠ 服务器维护中 ⚠"
  motd:
    - "&c&l⚠ 服务器维护中 ⚠"
    - "&7正在维护，请稍后再试"
  version-text: "&c维护中 ✖"
  show-incompatible-version: true
  show-fake-players: true
  fake-online: 0
  fake-max: 0
  hover-message:
    - "&c&l⚠ 服务器维护中 ⚠"
```
