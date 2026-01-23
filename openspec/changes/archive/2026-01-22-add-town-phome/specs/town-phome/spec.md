## ADDED Requirements

### Requirement: Town PHome Module Enable/Disable
The system SHALL provide a configuration option to enable or disable the Town PHome module.

#### Scenario: Module disabled
- **WHEN** `town-phome.enabled` is `false`
- **THEN** all `/tsl phome` commands respond with a disabled message

### Requirement: Configurable PAPI Variables
The system SHALL allow administrators to configure PlaceholderAPI variable names for town name, town level, and player role.

#### Scenario: Custom variable names
- **WHEN** administrator sets `town-phome.papi-variables.town-name` to `"myGuild_name"`
- **THEN** the system uses `%myGuild_name%` to resolve player's town

#### Scenario: Default variable names
- **WHEN** no custom variable names are configured
- **THEN** the system uses `%playerGuild_name%`, `%playerGuild_guild_level%`, and `%playerGuild_role%`

### Requirement: Town Membership Validation
The system SHALL validate that a player belongs to a town before allowing any PHome operations.

#### Scenario: Player not in any town
- **WHEN** player executes `/tsl phome` commands
- **AND** the town name PAPI variable returns empty or invalid
- **THEN** the system displays an error message indicating the player must join a town

### Requirement: Role-Based PHome Management
The system SHALL restrict PHome creation, deletion, and modification to players with authorized roles.

#### Scenario: Authorized role creates PHome
- **WHEN** player with role in `management-roles` list executes `/tsl phome set <name>`
- **THEN** the system creates or overwrites the PHome at player's current location

#### Scenario: Unauthorized role attempts management
- **WHEN** player without authorized role executes `/tsl phome set <name>` or `/tsl phome del <name>`
- **THEN** the system displays a permission denied message

### Requirement: PHome Limit by Town Level
The system SHALL enforce a maximum PHome count per town based on the town's level.

#### Scenario: Within limit
- **WHEN** town has fewer PHome points than the limit for its level
- **AND** authorized player executes `/tsl phome set <name>`
- **THEN** the system creates the new PHome

#### Scenario: At limit - new PHome blocked
- **WHEN** town has reached the PHome limit for its level
- **AND** authorized player attempts to create a new PHome (not overwrite existing)
- **THEN** the system displays a limit reached message

#### Scenario: Overwrite existing allowed at limit
- **WHEN** town has reached the PHome limit
- **AND** authorized player executes `/tsl phome set <existing-name>`
- **THEN** the system overwrites the existing PHome (no limit violation)

### Requirement: Town Member PHome Access
The system SHALL allow all members of a town to teleport to and view the town's PHome points.

#### Scenario: Member teleports to PHome
- **WHEN** town member executes `/tsl phome <name>` or `/tsl phome tp <name>`
- **AND** the PHome exists
- **THEN** the player is teleported to the PHome location

#### Scenario: Non-member access denied
- **WHEN** player not in the town attempts to teleport to that town's PHome
- **THEN** the system denies access (player can only access their own town's PHome)

### Requirement: PHome List Display
The system SHALL display a list of the player's town PHome points with current count and limit.

#### Scenario: List command
- **WHEN** town member executes `/tsl phome list`
- **THEN** the system displays all PHome names, current count, and maximum limit for the town

### Requirement: PHome Deletion
The system SHALL allow authorized roles to delete existing PHome points.

#### Scenario: Delete existing PHome
- **WHEN** authorized player executes `/tsl phome del <name>`
- **AND** the PHome exists
- **THEN** the system removes the PHome and confirms deletion

#### Scenario: Delete non-existent PHome
- **WHEN** authorized player executes `/tsl phome del <name>`
- **AND** the PHome does not exist
- **THEN** the system displays a not found message

### Requirement: PHome Name Uniqueness
The system SHALL enforce unique PHome names within each town (case-insensitive).

#### Scenario: Duplicate name handling
- **WHEN** authorized player creates PHome with name that already exists (case-insensitive)
- **THEN** the system overwrites the existing PHome at the new location

### Requirement: Error Handling
The system SHALL provide clear error messages for edge cases.

#### Scenario: Invalid world or location
- **WHEN** player attempts to teleport to a PHome in an unloaded world
- **THEN** the system displays an appropriate error message

#### Scenario: PAPI not available
- **WHEN** PlaceholderAPI is not installed
- **THEN** the module displays a warning and disables itself gracefully
