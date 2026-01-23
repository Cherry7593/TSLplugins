# Tasks: Add Town PHome System

## 1. Configuration
- [x] 1.1 Add `town-phome` section to `config.yml` with PAPI variable settings
- [x] 1.2 Add level-to-limit mapping configuration
- [x] 1.3 Add role permission configuration (which roles can manage PHome)

## 2. Data Layer
- [x] 2.1 Create `TownPHomeData.kt` with data classes (TownPHome, TownPHomeLocation)
- [x] 2.2 Create `TownPHomeStorage.kt` using JSON file persistence (following Landmark pattern)

## 3. Manager
- [x] 3.1 Create `TownPHomeManager.kt` with config loading and core logic
- [x] 3.2 Implement PAPI variable resolution for town name/role/level
- [x] 3.3 Implement role-based permission checking
- [x] 3.4 Implement PHome limit checking based on town level

## 4. Commands
- [x] 4.1 Create `TownPHomeCommand.kt` implementing `SubCommandHandler`
- [x] 4.2 Implement `set <name>` - create/overwrite PHome (authorized roles only)
- [x] 4.3 Implement `del <name>` - delete PHome (authorized roles only)
- [x] 4.4 Implement `<name>` / `tp <name>` - teleport to PHome (all town members)
- [x] 4.5 Implement `list` - show town PHome list with count/limit
- [x] 4.6 Implement tab completion

## 5. Integration
- [x] 5.1 Register `TownPHomeCommand` under `/tsl phome` in `TSLCommand.kt`
- [x] 5.2 Add message keys to `messages.yml`

## 6. Testing
- [ ] 6.1 Test with player not in any town (should be blocked)
- [ ] 6.2 Test role-based permissions (mayor vs member)
- [ ] 6.3 Test PHome limit enforcement
- [ ] 6.4 Test teleportation across town members
