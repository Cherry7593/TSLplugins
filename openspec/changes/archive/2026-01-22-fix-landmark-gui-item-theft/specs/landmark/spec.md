# Landmark Spec Delta

## MODIFIED Requirements

### Requirement: GUI Item Protection

The landmark GUI MUST prevent players from taking items out of the interface. The system SHALL use a custom InventoryHolder to identify landmark GUI inventories and properly handle inventory close events.

#### Scenario: Player cannot take items from edit menu

Given a player opens the landmark edit menu via `/tsl landmark edit <name>`
When the player clicks on any item in the GUI
Then the click event is cancelled
And no items are moved to the player's inventory

#### Scenario: Player cannot take items from main menu

Given a player opens the landmark main menu via `/lm` or `/tsl landmark gui`
When the player clicks on any item in the GUI
Then the click event is cancelled
And no items are moved to the player's inventory

#### Scenario: GUI state cleanup on close

Given a player has a landmark GUI open
When the player closes the landmark GUI (not another inventory)
Then the player's menu state is cleared
And subsequent non-landmark inventory interactions work normally
