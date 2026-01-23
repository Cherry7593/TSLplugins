# Patrol Module

Teleport randomly between online players for server moderation.

## Requirements

### Requirement: Patrol Mode

Operators SHALL be able to teleport between players randomly.

#### Scenario: Start patrol

- **WHEN** operator executes `/tsl patrol` with permission `tsl.patrol.use`
- **THEN** operator teleports to a random online player
- **THEN** operator can continue patrolling with subsequent commands

#### Scenario: No players online

- **WHEN** operator tries to patrol with no other players online
- **THEN** error message is shown
