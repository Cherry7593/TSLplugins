# PlayerList Module

Display formatted online player list.

## Requirements

### Requirement: Player List Display

Players SHALL be able to view online player list.

#### Scenario: View list

- **WHEN** player executes `/tsl list` with permission `tsl.list`
- **THEN** formatted list of online players is displayed
- **THEN** player count is shown
