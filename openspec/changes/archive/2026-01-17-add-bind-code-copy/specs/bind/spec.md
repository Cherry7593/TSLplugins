## ADDED Requirements

### Requirement: Clickable Bind Code Copy

When player requests QQ binding, the verification code SHALL be displayed as a clickable component that copies the bind command to clipboard.

#### Scenario: Player requests bind code

- **WHEN** player executes `/tsl bind` command
- **AND** WebSocket returns verification code successfully
- **THEN** display bind command with clickable copy functionality
- **THEN** clicking the code SHALL copy "绑定 {CODE}" to player's clipboard
- **THEN** display hover text indicating the code is clickable to copy

#### Scenario: Code display styling

- **WHEN** verification code is displayed
- **THEN** code text SHALL be highlighted with bold and distinctive color
- **THEN** hover text SHALL show "点击复制绑定口令"
