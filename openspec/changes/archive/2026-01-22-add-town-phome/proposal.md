# Change: Add Town PHome System

## Why
Players need a shared teleportation point system where PHome belongs to their town (Guild) rather than individuals. This enables town members to share teleport points while restricting management to authorized roles (Mayor/Deputy Mayor).

## What Changes
- Add new `phome` subcommand under `/tsl phome`
- PHome ownership shifts from individual players to towns (identified via PlaceholderAPI variables)
- Role-based permission system for create/delete/modify operations
- Town level determines maximum PHome count
- **Configurable PAPI variables** for town name, role, and level in config.yml

## Impact
- Affected specs: `town-phome` (new capability)
- Affected code:
  - New module: `TownPHome/` (Manager, Command, Storage, Data)
  - Config additions in `config.yml`
  - Message additions in `messages.yml`
- Dependencies: PlaceholderAPI (for `%playerGuild_*%` variables)
