# Change: 修复热重载后幻翼状态失效

## Why

插件热重载后，在线玩家的幻翼保护状态失效，导致幻翼会重新生成。根本原因是 `TSLPlayerProfileStore` 重建后内存缓存为空，而在线玩家不会触发 `onPlayerJoin` 事件来重新加载配置。

## What Changes

- 在 `PlayerDataManager` 中添加 `reloadOnlinePlayers()` 方法，用于重新加载所有在线玩家的配置到缓存
- 在 `/tsl reload` 命令中调用此方法，确保热重载后在线玩家的配置立即可用

## Impact

- Affected specs: `phantom`（添加热重载后状态持久化的场景）
- Affected code: `PlayerDataManager.kt`, `ReloadCommand.kt`
