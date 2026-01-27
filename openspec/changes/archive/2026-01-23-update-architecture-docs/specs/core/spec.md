## ADDED Requirements

### Requirement: Module Directory Structure

All feature modules SHALL be located in the `modules/` directory with standardized structure.

#### Scenario: New module creation

- **GIVEN** a developer wants to create a new module "example"
- **WHEN** they create the module files
- **THEN** files are placed in `src/main/kotlin/org/tsl/tSLplugins/modules/example/`
- **AND** the module class extends `AbstractModule`
- **AND** the module is registered in `TSLplugins.kt`

#### Scenario: Module file naming

- **GIVEN** a module named "example"
- **WHEN** creating module files
- **THEN** the entry class is named `ExampleModule.kt`
- **AND** optional manager is named `ExampleManager.kt`
- **AND** optional command handler is named `ExampleCommand.kt`
- **AND** optional listener is named `ExampleListener.kt`

### Requirement: AbstractModule Lifecycle

All modules SHALL extend `AbstractModule` and implement the standard lifecycle.

#### Scenario: Module enable

- **WHEN** a module is enabled
- **THEN** `loadConfig()` is called first to read `enabled` flag
- **THEN** if enabled=true, `doEnable()` is called
- **THEN** listeners are registered via `registerListener()`
- **THEN** module logs "模块已启用"

#### Scenario: Module disable

- **WHEN** a module is disabled
- **THEN** `doDisable()` is called for cleanup
- **THEN** all registered listeners are automatically unregistered
- **THEN** module logs "模块已禁用"

#### Scenario: Module reload

- **WHEN** `/tsl reload` is executed
- **THEN** `loadConfig()` re-reads configuration
- **THEN** if enabled state changes, appropriate enable/disable logic runs
- **THEN** if still enabled, `doReload()` is called for additional refresh

### Requirement: Service Layer Architecture

Global services SHALL be located in the `service/` directory.

#### Scenario: Service access

- **GIVEN** a module needs database access
- **WHEN** it accesses `DatabaseManager`
- **THEN** it imports from `org.tsl.tSLplugins.service.DatabaseManager`
- **AND** NOT from the root package

#### Scenario: Service list

- **GIVEN** the service layer
- **THEN** it contains `MessageManager` for i18n messages
- **AND** `DatabaseManager` for SQLite database
- **AND** `PlayerDataManager` for player profile persistence
- **AND** `TSLPlayerProfile` and `TSLPlayerProfileStore` for profile data

### Requirement: Folia Scheduler Compliance

All scheduled tasks SHALL use Folia-compatible schedulers.

#### Scenario: Entity-related task

- **GIVEN** code that operates on a player or entity
- **WHEN** scheduling a task
- **THEN** use `entity.scheduler.run(plugin, task, null)`
- **AND** NOT `Bukkit.getScheduler().runTask()`

#### Scenario: Global task

- **GIVEN** code that does not target a specific entity
- **WHEN** scheduling a global task
- **THEN** use `Bukkit.getGlobalRegionScheduler().run(plugin, task)`
- **AND** NOT `Bukkit.getScheduler().runTask()`

#### Scenario: Async task

- **GIVEN** code that performs I/O or heavy computation
- **WHEN** scheduling an async task
- **THEN** use `Bukkit.getAsyncScheduler().runNow(plugin, task)`
- **AND** NOT `Bukkit.getScheduler().runTaskAsynchronously()`
