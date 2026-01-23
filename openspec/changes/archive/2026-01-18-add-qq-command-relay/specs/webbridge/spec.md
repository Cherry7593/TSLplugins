## ADDED Requirements

### Requirement: QQ Command Relay

The plugin SHALL execute commands received from QQ group via WebSocket and return execution results.

#### Scenario: Receive command execution request

- **WHEN** WebSocket message with `action: "QQ_COMMAND_EXECUTE"` is received
- **AND** `serverId` is null or matches this server's ID
- **THEN** command is executed as console
- **THEN** `QQ_COMMAND_RESULT` response is sent with execution status

#### Scenario: Ignore command for other servers

- **WHEN** WebSocket message with `action: "QQ_COMMAND_EXECUTE"` is received
- **AND** `serverId` does not match this server's ID
- **THEN** message is ignored
- **THEN** no response is sent

#### Scenario: Command execution success

- **WHEN** command is executed successfully
- **THEN** response `success` is `true`
- **THEN** response `output` contains command feedback if available
- **THEN** response `error` is `null`

#### Scenario: Command execution failure

- **WHEN** command execution fails or throws exception
- **THEN** response `success` is `false`
- **THEN** response `error` contains error message
- **THEN** response `output` is `null`

#### Scenario: Command logging

- **WHEN** command is received for execution
- **THEN** command details are logged for audit purposes
- **THEN** log includes requestId, command, qqNumber, groupId

### Requirement: QQ Command Response Format

The plugin SHALL send command execution results in the specified JSON format.

#### Scenario: Response message structure

- **WHEN** command execution completes
- **THEN** response includes `type: "response"`
- **THEN** response includes `source: "minecraft"`
- **THEN** response includes original `requestId`
- **THEN** response `data.action` is `"QQ_COMMAND_RESULT"`
- **THEN** response `data.executedAt` is execution timestamp in milliseconds
