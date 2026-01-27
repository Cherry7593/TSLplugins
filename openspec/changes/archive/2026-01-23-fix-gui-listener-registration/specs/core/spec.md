## MODIFIED Requirements

### Requirement: GUI Listener Registration

All GUI classes that implement `Listener` and define `@EventHandler` methods SHALL be registered as listeners in the module's `doEnable()` method.

#### Scenario: GUI with event handlers

- **GIVEN** a GUI class implements `Listener`
- **AND** the GUI class has `@EventHandler` methods
- **WHEN** the module is enabled
- **THEN** the GUI is registered via `registerListener(gui)`
- **AND** GUI event handlers are called when events occur
