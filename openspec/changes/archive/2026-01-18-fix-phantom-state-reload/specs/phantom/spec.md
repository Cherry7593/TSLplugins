## MODIFIED Requirements

### Requirement: Phantom Toggle

Players SHALL be able to disable phantom harassment.

#### Scenario: Disable phantoms

- **WHEN** player executes `/tsl phantom off` with permission `tsl.phantom.toggle`
- **THEN** phantoms no longer target or spawn for player

#### Scenario: Enable phantoms

- **WHEN** player executes `/tsl phantom on`
- **THEN** normal phantom behavior resumes

#### Scenario: State persists after hot reload

- **WHEN** player has phantom protection enabled and plugin is hot-reloaded via `/tsl reload`
- **THEN** phantom protection remains active without requiring player to re-enable
