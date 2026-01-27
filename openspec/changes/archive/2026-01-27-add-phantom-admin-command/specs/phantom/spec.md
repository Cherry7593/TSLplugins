## ADDED Requirements

### Requirement: Admin Phantom Control

Administrators SHALL be able to modify phantom settings for other players.

#### Scenario: Admin disables phantom for another player

- **WHEN** admin executes `/tsl phantom off <player>` with permission `tsl.phantom.admin`
- **THEN** phantoms no longer target or spawn for the specified player
- **AND** both admin and target player receive confirmation message

#### Scenario: Admin enables phantom for another player

- **WHEN** admin executes `/tsl phantom on <player>` with permission `tsl.phantom.admin`
- **THEN** normal phantom behavior resumes for the specified player
- **AND** both admin and target player receive confirmation message

#### Scenario: Admin checks phantom status for another player

- **WHEN** admin executes `/tsl phantom status <player>` with permission `tsl.phantom.admin`
- **THEN** the specified player's phantom toggle state is displayed to admin

#### Scenario: Target player not found

- **WHEN** admin executes `/tsl phantom off <player>` but player is not online
- **THEN** admin receives error message indicating player not found

#### Scenario: Permission denied for admin command

- **WHEN** player without `tsl.phantom.admin` permission executes `/tsl phantom off <player>`
- **THEN** player receives permission denied message
