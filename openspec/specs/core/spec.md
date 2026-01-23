# Core System

The foundational infrastructure that all modules depend on.

## Requirements

### Requirement: Plugin Initialization

The plugin SHALL initialize all modules during server startup via `TSLplugins.kt`.

#### Scenario: Server startup

- **WHEN** the server starts
- **THEN** ConfigUpdateManager checks and updates config.yml
- **THEN** PlayerDataManager initializes PDC key registry
- **THEN** all enabled module Managers are initialized
- **THEN** all module Listeners are registered
- **THEN** TSLCommand dispatcher registers all subcommands
- **THEN** PlaceholderAPI expansion registers if PAPI is present

### Requirement: Unified Command Dispatcher

The plugin SHALL route all commands through `/tsl <subcommand>` via `TSLCommand.kt`.

#### Scenario: Command execution

- **WHEN** player executes `/tsl <subcommand> [args]`
- **THEN** TSLCommand finds the registered SubCommandHandler
- **THEN** the handler processes the command with remaining args

#### Scenario: Unknown subcommand

- **WHEN** player executes `/tsl <unknown>`
- **THEN** the system displays help message with available commands

### Requirement: Configuration Hot-Reload

The plugin SHALL support reloading all module configurations without server restart.

#### Scenario: Reload command

- **WHEN** operator executes `/tsl reload`
- **THEN** main config.yml is reloaded
- **THEN** each module's Manager.loadConfig() is called
- **THEN** aliases.yml is reloaded
- **THEN** success message with reload stats is shown

### Requirement: Configuration Versioning

The plugin SHALL automatically update config.yml when structure changes.

#### Scenario: Config version mismatch

- **WHEN** server starts with outdated config-version
- **THEN** old config is backed up to config.yml.backup
- **THEN** new config structure is merged with user values preserved
- **THEN** config-version is updated to current version

### Requirement: Player Data Persistence

The plugin SHALL persist player-specific settings using PDC (PersistentDataContainer).

#### Scenario: Toggle state persistence

- **WHEN** player toggles a feature (kiss/ride/toss)
- **THEN** the state is saved to player's PDC
- **THEN** the state persists across server restarts

### Requirement: PlaceholderAPI Integration

The plugin SHALL provide PAPI variables with prefix `%tsl_*%`.

#### Scenario: Variable request

- **WHEN** another plugin requests `%tsl_ping%`
- **THEN** TSLPlaceholderExpansion returns the appropriate value
