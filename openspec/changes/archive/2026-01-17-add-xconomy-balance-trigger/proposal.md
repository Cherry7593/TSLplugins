# Change: Add XConomy Balance Monitoring & Dynamic Command Trigger

## Why

服务器需要基于玩家经济余额自动执行控制台命令的能力。常见场景包括：
- 余额过低时自动执行惩罚/提醒命令
- 余额过高时自动执行奖励/限制命令
- 与权限系统联动（如 LuckPerms 组切换）

## What Changes

- 新增 `xconomy-trigger` 模块，监控玩家 XConomy 余额并触发指令
- 支持双向阈值触发（低于 A 执行 X，高于 B 执行 Y）
- 异步轮询机制，避免主线程阻塞
- 状态锁 + 冷却机制，防止阈值边缘重复触发
- 指令支持 `%player%`、`%uuid%`、`%balance%` 占位符
- WebBridge 后台管理集成（可选）

## Impact

- **Affected specs**: 新增 `xconomy-trigger` capability
- **Affected code**:
  - `src/main/kotlin/org/tsl/tSLplugins/XconomyTrigger/` (新目录)
  - `src/main/resources/config.yml` (新增配置节)
  - `TSLplugins.kt` (模块注册)
  - `ReloadCommand.kt` (热重载支持)

## Dependencies

- **XConomy 插件**: 必须安装，提供余额 API
- **Vault API (可选)**: 作为备选经济接口

## Risks

- XConomy 未安装时需优雅降级
- 大量玩家在线时需控制轮询频率
