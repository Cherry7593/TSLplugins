# xconomy-trigger Specification

## Purpose
TBD - created by archiving change add-xconomy-balance-trigger. Update Purpose after archive.
## Requirements
### Requirement: Balance Monitoring

The plugin SHALL periodically scan online players' XConomy balance.

#### Scenario: Periodic scan

- **WHEN** scan-interval-seconds passes
- **THEN** all online players' balances are checked asynchronously
- **THEN** no main thread blocking occurs

#### Scenario: XConomy unavailable

- **WHEN** XConomy plugin is not loaded
- **THEN** module logs warning and disables itself
- **THEN** no errors are thrown

### Requirement: Low Balance Trigger

The plugin SHALL execute configured commands when player balance falls below threshold.

#### Scenario: Balance drops below threshold

- **WHEN** player balance < low-balance.threshold
- **AND** player state is NORMAL
- **AND** low-balance.enabled is true
- **THEN** low-balance.commands are executed as console
- **THEN** player state becomes LOW_FIRED

#### Scenario: Balance recovers from low

- **WHEN** player balance >= low-balance.threshold + hysteresis
- **AND** player state is LOW_FIRED
- **THEN** player state becomes NORMAL

### Requirement: High Balance Trigger

The plugin SHALL execute configured commands when player balance exceeds threshold.

#### Scenario: Balance exceeds threshold

- **WHEN** player balance > high-balance.threshold
- **AND** player state is NORMAL
- **AND** high-balance.enabled is true
- **THEN** high-balance.commands are executed as console
- **THEN** player state becomes HIGH_FIRED

#### Scenario: Balance drops from high

- **WHEN** player balance <= high-balance.threshold - hysteresis
- **AND** player state is HIGH_FIRED
- **THEN** player state becomes NORMAL

### Requirement: Trigger Debouncing

The plugin SHALL prevent rapid repeated triggering at threshold boundaries.

#### Scenario: Hysteresis prevents oscillation

- **WHEN** balance fluctuates near threshold
- **THEN** state only changes after crossing threshold by hysteresis amount
- **THEN** commands are not repeatedly executed

#### Scenario: Player cooldown

- **WHEN** command was triggered for player
- **AND** less than player-cooldown-seconds has passed
- **THEN** no additional triggers occur for that player

### Requirement: Command Placeholder Support

The plugin SHALL replace placeholders in command strings before execution.

#### Scenario: Placeholder replacement

- **WHEN** command contains %player%
- **THEN** %player% is replaced with player name
- **WHEN** command contains %uuid%
- **THEN** %uuid% is replaced with player UUID
- **WHEN** command contains %balance%
- **THEN** %balance% is replaced with current balance

### Requirement: Folia Thread Safety

The plugin SHALL use Folia-compatible schedulers for all operations.

#### Scenario: Async balance scanning

- **WHEN** balance scan is triggered
- **THEN** scanning runs on AsyncScheduler
- **THEN** main thread is not blocked

#### Scenario: Command execution

- **WHEN** command needs to be executed
- **THEN** command runs on GlobalRegionScheduler
- **THEN** Folia compatibility is maintained

### Requirement: Configuration Hot Reload

The plugin SHALL support runtime configuration reload.

#### Scenario: Reload command

- **WHEN** /tsl reload is executed
- **THEN** xconomy-trigger config is reloaded
- **THEN** player states are preserved
- **THEN** scan interval is updated

