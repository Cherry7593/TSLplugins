## MODIFIED Requirements

### Requirement: Module Directory Structure

All feature modules SHALL be located exclusively in the `modules/` directory with the following structure:

```
modules/
├── <module-name>/
│   ├── <ModuleName>Module.kt    # Module entry (extends AbstractModule)
│   ├── <ModuleName>Manager.kt   # Business logic (optional)
│   ├── <ModuleName>Command.kt   # Command handler (optional)
│   ├── <ModuleName>Listener.kt  # Event listener (optional)
│   └── ...                      # Additional module files
```

**Constraints:**
- No module code SHALL exist outside `modules/` directory (except core framework in `core/`)
- All module classes SHALL use package `org.tsl.tSLplugins.modules.<module-name>`
- Module entry class SHALL extend `AbstractModule` and implement required lifecycle methods

#### Scenario: All modules in unified directory

- **GIVEN** the TSLplugins codebase
- **WHEN** inspecting the source directory structure
- **THEN** all 49+ feature modules are located in `src/main/kotlin/org/tsl/tSLplugins/modules/`
- **AND** no legacy module directories exist at the same level as `modules/`

#### Scenario: Module registration

- **GIVEN** a feature module in `modules/<name>/`
- **WHEN** the plugin initializes
- **THEN** the module is registered via `ModuleRegistry.register(<ModuleName>Module())`
- **AND** no direct imports from legacy directories are required
