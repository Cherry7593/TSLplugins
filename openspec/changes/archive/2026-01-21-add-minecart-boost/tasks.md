# Tasks

## 1. Core Implementation

- [x] 1.1 Create `MinecartBoost/MinecartBoostManager.kt` with config loading and block-speed mapping
- [x] 1.2 Create `MinecartBoost/MinecartBoostListener.kt` to handle `VehicleMoveEvent`
- [x] 1.3 Register module in `TSLplugins.kt` main class

## 2. Configuration

- [x] 2.1 Add `minecart-boost` section to `config.yml` with enabled flag and block-speed mappings
- [x] 2.2 Add messages to `messages.yml` (if needed for debug/info) - Not needed, passive effect
- [x] 2.3 Update `ConfigUpdateManager.kt` with new config version (v39 → v40)

## 3. Testing

- [ ] 3.1 In-game test: Place powered rail on each configured block type
- [ ] 3.2 Verify occupied minecart achieves expected speed
- [ ] 3.3 Verify empty minecart is not affected
- [ ] 3.4 Test hot-reload preserves settings
