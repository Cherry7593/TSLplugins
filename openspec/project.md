# Project Context

## Purpose
TSLplugins is a multi-function integrated plugin for **Paper/Folia 1.21.8** Minecraft servers. It provides modular features for player interaction (Kiss, Hat, Scale, Toss, Ride, ChatBubble), admin tools (Freeze, Maintenance, Spec, Patrol), world features (BabyLock, Phantom control, EndDragon limits), and advanced systems (WebBridge, Mcedia video player, TimedAttribute).

**Goals:**
- Modular, plug-and-play architecture where modules are independent
- Native Folia multi-threading support (no polling tasks)
- Configuration-driven behavior with hot-reload support
- Performance-optimized via config caching and event-driven design

## Tech Stack
- **Language:** Kotlin 1.9.21
- **Build:** Gradle 8.5 with Kotlin DSL + Shadow plugin 8.1.1
- **Runtime:** Java 21
- **Server API:** Paper/Folia API 1.21.8-R0.1-SNAPSHOT
- **Serialization:** kotlinx-serialization-json 1.6.0
- **WebSocket:** Java-WebSocket 1.5.6
- **Optional integrations:** PlaceholderAPI 2.11.6, LuckPerms 5.4

## Project Conventions

### Code Style
- **Classes:** PascalCase (`PlayerManager`)
- **Functions/Variables:** camelCase (`loadConfig`, `playerName`)
- **Constants:** UPPER_SNAKE_CASE (`MAX_PLAYERS`, `CONFIG_VERSION`)
- **Packages:** lowercase (`org.tsl.tslplugins.feature`)
- **Encoding:** UTF-8 for all files
- Use Kotlin idioms: safe calls (`?.`), `let`, string templates, property access
- Avoid `!!` unless null is impossible; avoid Java-style getters
- Use Adventure API with `LegacyComponentSerializer.legacyAmpersand()` for messages

### Architecture Patterns

**Directory Structure:**
```
src/main/kotlin/org/tsl/tSLplugins/
├── core/                    # Framework layer
│   ├── AbstractModule.kt    # Module base class with lifecycle
│   ├── ModuleContext.kt     # Dependency injection context
│   └── ModuleRegistry.kt    # Module registration and management
├── service/                 # Service layer
│   ├── MessageManager.kt    # i18n message management
│   ├── DatabaseManager.kt   # SQLite database access
│   ├── PlayerDataManager.kt # Player profile persistence
│   └── TSLPlayerProfile*.kt # Profile data classes
├── modules/                 # Feature modules (48+)
│   └── <module-name>/       # One directory per module
│       ├── <Name>Module.kt  # Entry point (extends AbstractModule)
│       ├── <Name>Manager.kt # Business logic (optional)
│       ├── <Name>Command.kt # Command handler (optional)
│       └── <Name>Listener.kt# Event listener (optional)
└── TSLplugins.kt            # Main plugin class
```

**AbstractModule Pattern** (recommended for all modules):
```kotlin
class ExampleModule : AbstractModule() {
    override val id = "example"           // Unique module ID
    override val configPath = "example"   // Config section path
    
    override fun doEnable() {
        // Initialize manager, register listeners
        registerListener(ExampleListener(this))
    }
    
    override fun doDisable() {
        // Cleanup resources
    }
    
    override fun getCommandHandler() = ExampleCommand(this)
}
```

**Key patterns:**
- All commands route through `/tsl <subcommand>` via `TSLCommand.kt` dispatcher
- Config cached at startup/reload; zero overhead during event handling
- Services accessed via `ModuleContext` (not direct imports from root package)
- PDC (`PersistentDataContainer`) for player data persistence

**Folia Scheduler Guide** (CRITICAL - never use BukkitScheduler):
```kotlin
// Entity-related task (player, mob, etc.)
player.scheduler.run(plugin, { /* task */ }, null)

// Global task (not tied to entity)
Bukkit.getGlobalRegionScheduler().run(plugin) { /* task */ }

// Async task (I/O, computation)
Bukkit.getAsyncScheduler().runNow(plugin) { /* task */ }

// ❌ NEVER use these (Folia incompatible):
// Bukkit.getScheduler().runTask()
// Bukkit.getScheduler().runTaskAsynchronously()
```

**Module Creation Checklist:**
1. Create directory: `modules/<name>/`
2. Create `<Name>Module.kt` extending `AbstractModule`
3. Implement `doEnable()` with initialization logic
4. Register module in `TSLplugins.kt`: `registry.register(<Name>Module())`
5. Add config section in `config.yml` with `enabled: true`
6. Add messages in `messages.yml` under `<name>:` section

### Testing Strategy
- Manual in-game testing with suggested test steps
- Thread-safety mental checks for Paper/Folia compatibility
- Verify event registration/unregistration and plugin lifecycle
- No automated test framework currently

### Git Workflow
- Single main branch development
- Build with `./gradlew shadowJar` → output: `build/libs/TSLplugins-1.0.jar`
- Config version control via `ConfigUpdateManager` (increment `CURRENT_CONFIG_VERSION` when changing config.yml)

## Domain Context
- **Minecraft Plugin Development:** Bukkit/Paper event system, commands, permissions
- **Folia:** Multi-threaded region-based server; requires entity/region schedulers instead of BukkitScheduler
- **PDC:** PersistentDataContainer for storing player data in player NBT (survives restarts)
- **PAPI:** PlaceholderAPI integration for custom variables (`%tsl_*%`)
- **Adventure API:** Modern text component system for Minecraft messages

## Important Constraints
- **Folia Compatibility:** No blocking calls on main/region thread; use Folia schedulers exclusively
- **Java 21 Required:** Minimum runtime version
- **Paper 1.21.8+:** Minimum server version
- **Config Versioning:** Must increment `CURRENT_CONFIG_VERSION` when modifying `config.yml`
- **Module Independence:** Modules should not depend on each other; can be disabled individually

## External Dependencies
- **PlaceholderAPI (optional):** Provides `%tsl_*%` variables for other plugins
- **LuckPerms (optional):** Used by Visitor module for permission-based protection
- **Paper/Folia API:** Core server API (compileOnly dependency)
- **WebSocket Server:** WebBridge module connects to external web services
