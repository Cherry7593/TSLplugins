# death-penalty Specification

## Purpose
TBD - created by archiving change add-death-gold-penalty. Update Purpose after archive.
## Requirements
### Requirement: Death Gold Deduction
当玩家死亡时，系统 SHALL 从玩家的 XConomy 账户中扣除配置的金币数量。

#### Scenario: Sufficient balance deduction
- **WHEN** 玩家死亡且余额充足（>= 配置金额）
- **THEN** 系统扣除配置的金币数量
- **AND** 向玩家发送扣费成功消息（支持颜色代码）

#### Scenario: Insufficient balance exemption
- **WHEN** 玩家死亡但余额不足（< 配置金额）
- **THEN** 系统不执行扣费操作
- **AND** 向玩家发送余额不足提示消息

### Requirement: Asynchronous Processing
死亡惩罚逻辑 SHALL 在异步线程中处理，避免阻塞服务器主线程。

#### Scenario: Async execution
- **WHEN** PlayerDeathEvent 触发
- **THEN** 余额查询和扣费操作在 Folia AsyncScheduler 上执行
- **AND** 消息发送通过 player.scheduler 回到玩家线程

### Requirement: XConomy Dependency Handling
模块 SHALL 在 XConomy 插件未加载或不可用时优雅降级。

#### Scenario: XConomy unavailable
- **WHEN** XConomy 插件未安装或未启用
- **THEN** 死亡惩罚模块自动禁用
- **AND** 输出日志提示 XConomy 不可用

#### Scenario: Player data uninitialized
- **WHEN** 玩家数据尚未被 XConomy 初始化
- **THEN** 系统不抛出 NPE
- **AND** 跳过本次扣费操作

### Requirement: Configurable Settings
模块 SHALL 支持以下可配置项：

#### Scenario: Enable/disable toggle
- **WHEN** 配置 `death-penalty.enabled` 为 false
- **THEN** 模块不监听死亡事件

#### Scenario: Configurable penalty amount
- **WHEN** 配置 `death-penalty.amount` 为 N
- **THEN** 每次死亡扣除 N 金币

#### Scenario: Customizable messages
- **WHEN** 配置自定义消息文案
- **THEN** 玩家收到的提示使用配置的文案
- **AND** 支持 `&` 颜色代码和 `%amount%` 占位符

### Requirement: Hot Reload Support
模块 SHALL 支持通过 `/tsl reload` 命令热重载配置。

#### Scenario: Config hot reload
- **WHEN** 执行 `/tsl reload` 命令
- **THEN** 死亡惩罚模块重新加载配置
- **AND** 新配置立即生效

