# Design: Town PHome System

## Context
The server uses an external Guild plugin that exposes player town information via PlaceholderAPI:
- `%playerGuild_name%` - Town name
- `%playerGuild_guild_level%` - Town level (integer)
- `%playerGuild_role%` - Player's role in town

These variable names should be **configurable** so administrators can adapt to different Guild plugins.

## Goals
- Enable shared PHome points within a town
- Role-based management (Mayor/Deputy Mayor can create/delete)
- Level-based PHome limits
- Reuse existing patterns (Manager-Command pattern, JSON storage like Landmark)

## Non-Goals
- No GUI implementation (command-only, unless project has reusable GUI system)
- No cross-town PHome sharing
- No custom town/guild system (rely entirely on PAPI)

## Decisions

### 1. PAPI Variable Configuration
Variables are configurable in `config.yml`:
```yaml
town-phome:
  enabled: false
  papi-variables:
    town-name: "playerGuild_name"      # without %...%
    town-level: "playerGuild_guild_level"
    player-role: "playerGuild_role"
```

### 2. Role Permission Configuration
```yaml
town-phome:
  management-roles:
    - "镇长"
    - "副镇长"
    - "Mayor"
    - "Deputy"
```

### 3. Level-to-Limit Mapping
```yaml
town-phome:
  level-limits:
    1: 3
    2: 5
    3: 8
    default: 10  # fallback for higher levels
```

### 4. Storage Pattern
Follow `LandmarkStorage.kt` pattern:
- JSON file: `town-phomes.json`
- In-memory cache with ConcurrentHashMap
- Key: town name (case-insensitive)

### 5. Data Structure
```kotlin
@Serializable
data class TownPHome(
    val townName: String,
    val homes: MutableMap<String, TownPHomeLocation> = mutableMapOf()
)

@Serializable
data class TownPHomeLocation(
    val name: String,
    val world: String,
    val x: Double,
    val y: Double,
    val z: Double,
    val yaw: Float,
    val pitch: Float,
    val createdBy: String,  // UUID
    val createdAt: Long
)
```

### 6. Command Structure
```
/tsl phome              - Show help or list
/tsl phome list         - List town's PHome points
/tsl phome <name>       - Teleport to PHome
/tsl phome set <name>   - Create/overwrite PHome (manager only)
/tsl phome del <name>   - Delete PHome (manager only)
```

## Risks / Trade-offs
- **PAPI dependency**: If PlaceholderAPI is not installed or variables return empty, feature is disabled
- **Case sensitivity**: Town names are stored case-insensitively to avoid duplicates

## Migration Plan
New feature, no migration needed.

## Open Questions
- None (clarified: variables are configurable, command is `tsl phome`)
