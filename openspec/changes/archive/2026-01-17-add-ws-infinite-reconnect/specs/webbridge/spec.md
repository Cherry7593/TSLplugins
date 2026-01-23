## MODIFIED Requirements

### Requirement: WebSocket Connection

The plugin SHALL maintain a persistent WebSocket connection to configured server.

#### Scenario: Initial connection

- **WHEN** plugin enables with webbridge enabled
- **THEN** WebSocket connection is established to configured URL
- **THEN** authentication token is sent

#### Scenario: Auto-reconnect on disconnect

- **WHEN** connection is lost
- **AND** auto-reconnect is enabled
- **THEN** reconnection is attempted every 30 seconds
- **THEN** reconnection attempts continue indefinitely until connection succeeds or client is stopped

#### Scenario: Auto-reconnect with max attempts (backward compatible)

- **WHEN** connection is lost
- **AND** auto-reconnect is enabled
- **AND** max-reconnect-attempts is greater than 0
- **THEN** reconnection is attempted at configured interval
- **THEN** attempts stop after max-reconnect-attempts reached

#### Scenario: Plugin reconnect behavior

- **WHEN** WebSocket connection closes unexpectedly
- **THEN** plugin schedules reconnection using Folia-compatible scheduler
- **THEN** reconnection is attempted after reconnect-interval seconds

#### Scenario: Mod reconnect behavior

- **WHEN** WebSocket connection closes unexpectedly
- **THEN** mod schedules reconnection using ScheduledExecutorService
- **THEN** reconnection is attempted after reconnect-interval seconds
