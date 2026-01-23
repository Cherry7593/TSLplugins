## MODIFIED Requirements

### Requirement: Heartbeat

The plugin SHALL send periodic heartbeats to maintain connection.

#### Scenario: Heartbeat ping

- **WHEN** heartbeat-interval passes
- **THEN** heartbeat message is sent to server

#### Scenario: Heartbeat logging (Fabric Mod)

- **WHEN** heartbeat message is sent
- **AND** debug is false (default)
- **THEN** no log message is printed to console

#### Scenario: Heartbeat logging debug mode (Fabric Mod)

- **WHEN** heartbeat message is sent
- **AND** debug is true
- **THEN** log message is printed to console at INFO level

## ADDED Requirements

### Requirement: Fabric Mod Debug Configuration

The Fabric mod SHALL support a debug configuration option to control verbose logging.

#### Scenario: Debug disabled (default)

- **WHEN** debug is false or not configured
- **THEN** routine messages (heartbeat, player list, sent messages) are logged at FINE level only
- **THEN** console remains clean with only important events

#### Scenario: Debug enabled

- **WHEN** debug is true
- **THEN** routine messages are logged at INFO level
- **THEN** detailed information is visible in console for troubleshooting

#### Scenario: Configuration file

- **WHEN** tsl-webbridge.json is loaded
- **THEN** debug field is read with default value false
