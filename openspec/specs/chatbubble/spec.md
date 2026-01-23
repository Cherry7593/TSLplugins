# ChatBubble Module

Display chat messages as floating text above players' heads.

## Requirements

### Requirement: Chat Bubble Display

The plugin SHALL show chat messages as text displays above players.

#### Scenario: Chat message

- **WHEN** player sends a chat message
- **THEN** TextDisplay entity spawns above player's head
- **THEN** message is visible to nearby players within view range
- **THEN** bubble follows player movement
- **THEN** bubble disappears after timeSpan ticks

### Requirement: Sneaking Transparency

Bubbles SHALL become transparent when player is sneaking.

#### Scenario: Sneaking opacity

- **WHEN** player with bubble is sneaking
- **THEN** bubble opacity changes to sneaking opacity value
- **WHEN** player stops sneaking
- **THEN** bubble opacity returns to default

### Requirement: Billboard Mode

Bubbles SHALL face players based on billboard setting.

#### Scenario: Vertical billboard

- **WHEN** billboard is set to VERTICAL
- **THEN** bubble rotates horizontally to face viewer
- **THEN** bubble does not tilt vertically

## Configuration

```yaml
chatbubble:
  enabled: false
  yOffset: 0.75
  timeSpan: 100
  billboard: "VERTICAL"
  shadow: false
  viewRange: 16.0
  updateTicks: 2
  movementTicks: 4
  opacity:
    default: 1.0
    sneaking: 0.25
  background:
    red: -1
    green: 0
    blue: 0
    alpha: 0
```
