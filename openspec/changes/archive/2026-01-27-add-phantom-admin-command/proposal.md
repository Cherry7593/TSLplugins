# Change: 添加幻翼控制管理员命令

## Why

当前幻翼控制模块只允许玩家修改自己的幻翼状态，管理员无法帮助或强制修改其他玩家的设置。这在需要统一管理玩家体验或帮助不熟悉命令的玩家时造成不便。

## What Changes

- 添加 `/tsl phantom off <玩家名>` 命令，允许管理员禁用指定玩家的幻翼骚扰
- 添加 `/tsl phantom on <玩家名>` 命令，允许管理员启用指定玩家的幻翼骚扰
- 添加 `/tsl phantom status <玩家名>` 命令，允许管理员查看指定玩家的幻翼状态
- 新增权限 `tsl.phantom.admin` 用于控制管理员功能

## Impact

- Affected specs: `phantom`
- Affected code: `PhantomModule.kt` (PhantomModuleCommand 类)
