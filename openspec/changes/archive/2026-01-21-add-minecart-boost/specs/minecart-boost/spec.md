## ADDED Requirements

### Requirement: Block-Based Speed Boost

The system SHALL boost occupied minecart speed when passing over a powered rail placed on specific blocks.

#### Scenario: Minecart on packed ice

- **WHEN** an occupied minecart passes over a powered rail placed on PACKED_ICE
- **THEN** the minecart velocity is set to 16 m/s

#### Scenario: Minecart on coal block

- **WHEN** an occupied minecart passes over a powered rail placed on COAL_BLOCK
- **THEN** the minecart velocity is set to 16 m/s

#### Scenario: Minecart on chiseled deepslate

- **WHEN** an occupied minecart passes over a powered rail placed on CHISELED_DEEPSLATE
- **THEN** the minecart velocity is set to 24 m/s

#### Scenario: Minecart on polished basalt

- **WHEN** an occupied minecart passes over a powered rail placed on POLISHED_BASALT
- **THEN** the minecart velocity is set to 24 m/s

#### Scenario: Minecart on cut copper

- **WHEN** an occupied minecart passes over a powered rail placed on CUT_COPPER
- **THEN** the minecart velocity is set to 24 m/s

#### Scenario: Minecart on stripped pale oak wood

- **WHEN** an occupied minecart passes over a powered rail placed on STRIPPED_PALE_OAK_WOOD
- **THEN** the minecart velocity is set to 30 m/s

#### Scenario: Minecart on dark prismarine

- **WHEN** an occupied minecart passes over a powered rail placed on DARK_PRISMARINE
- **THEN** the minecart velocity is set to 30 m/s

#### Scenario: Minecart on chiseled tuff bricks

- **WHEN** an occupied minecart passes over a powered rail placed on CHISELED_TUFF_BRICKS
- **THEN** the minecart velocity is set to 30 m/s

### Requirement: Occupied Minecart Only

The system SHALL only boost minecarts that have a player passenger.

#### Scenario: Empty minecart not boosted

- **WHEN** an empty minecart passes over a powered rail placed on a boost block
- **THEN** the minecart velocity is NOT modified

### Requirement: Module Toggle

The system SHALL allow enabling or disabling the minecart boost feature via configuration.

#### Scenario: Module disabled

- **WHEN** `minecart-boost.enabled` is set to `false` in config
- **THEN** no minecart speed modifications occur

#### Scenario: Module enabled

- **WHEN** `minecart-boost.enabled` is set to `true` in config
- **THEN** minecart speed modifications apply as configured

### Requirement: Configurable Block-Speed Mapping

The system SHALL allow customizing block-speed mappings via configuration.

#### Scenario: Custom block mapping

- **WHEN** admin adds a custom block type with speed value in config
- **THEN** that block provides the configured speed boost
