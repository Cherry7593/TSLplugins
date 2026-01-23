# town-phome Spec Delta

## ADDED Requirements

### Requirement: PHome GUI Access
The system SHALL provide a GUI interface for viewing and managing PHome points.

#### Scenario: Open GUI via command
- **WHEN** player executes `/tsl phome gui`
- **AND** player belongs to a town
- **THEN** the system opens the PHome list GUI

#### Scenario: Player not in town
- **WHEN** player executes `/tsl phome gui`
- **AND** player does not belong to any town
- **THEN** the system displays the "not in town" error message

### Requirement: PHome List GUI
The system SHALL display all PHome points for the player's town in a paginated inventory GUI.

#### Scenario: View PHome list
- **WHEN** player opens the PHome GUI
- **THEN** the system displays PHome points as inventory items
- **AND** shows current count and limit in the title
- **AND** supports pagination (45 items per page)

#### Scenario: Empty list
- **WHEN** player opens the PHome GUI
- **AND** the town has no PHome points
- **THEN** the system displays an informational item indicating the list is empty

### Requirement: GUI Teleportation
The system SHALL allow players to teleport to PHome points by clicking in the GUI.

#### Scenario: Left-click teleport
- **WHEN** player left-clicks a PHome item in the GUI
- **THEN** the system teleports the player to that PHome location
- **AND** closes the GUI

### Requirement: GUI PHome Deletion
The system SHALL allow authorized players to delete PHome points via the GUI.

#### Scenario: Right-click delete by authorized role
- **WHEN** player with management role right-clicks a PHome item
- **THEN** the system deletes the PHome
- **AND** refreshes the GUI

#### Scenario: Right-click by unauthorized role
- **WHEN** player without management role right-clicks a PHome item
- **THEN** the system ignores the click (no action taken)

### Requirement: GUI PHome Creation
The system SHALL allow authorized players to create PHome points via the GUI.

#### Scenario: Create PHome via GUI
- **WHEN** player with management role clicks the "Create" button
- **AND** the town has not reached its PHome limit
- **THEN** the system prompts the player to enter a name in chat
- **AND** creates the PHome at the player's location after input

#### Scenario: Create blocked at limit
- **WHEN** player with management role clicks the "Create" button
- **AND** the town has reached its PHome limit
- **THEN** the system displays a limit reached message
