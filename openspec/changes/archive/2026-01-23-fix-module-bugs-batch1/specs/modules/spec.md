## MODIFIED Requirements

### Requirement: Phantom Module Hot Reload

幻翼控制模块 SHALL 在启动/热重载时立即处理所有在线玩家的幻翼状态。

#### Scenario: Hot reload with phantom disabled players

- **GIVEN** 玩家已禁用幻翼骚扰
- **AND** 插件被热重载
- **WHEN** 模块启用
- **THEN** 玩家的 TIME_SINCE_REST 统计立即被重置为 0
- **AND** 幻翼不会生成

### Requirement: TownPHome Command Structure

phome 命令结构 SHALL 简化为直观的使用方式。

#### Scenario: Open GUI without arguments

- **GIVEN** 玩家输入 `/phome`
- **WHEN** 命令执行
- **THEN** 直接打开 phome GUI 界面

#### Scenario: Direct teleport with home name

- **GIVEN** 玩家输入 `/phome <名称>`
- **AND** `<名称>` 不是保留字
- **WHEN** 命令执行
- **THEN** 直接传送到该 phome

#### Scenario: Reserved words protection

- **GIVEN** 管理者尝试设置名为 `set` 的 phome
- **WHEN** 执行 `/phome set set`
- **THEN** 命令被拒绝
- **AND** 显示错误消息

### Requirement: TownPHome Hex Color Support

phome GUI SHALL 正确解析和显示十六进制颜色代码。

#### Scenario: Display town name with hex colors

- **GIVEN** 小镇名称包含 `#xxxxxx` 或 `&#xxxxxx` 格式颜色
- **WHEN** 在 GUI 中显示
- **THEN** 颜色正确渲染

### Requirement: TownPHome Member Notification

设置或删除 phome 时 SHALL 通知小镇在线成员。

#### Scenario: Notify on phome creation

- **GIVEN** 镇长设置了新的 phome
- **WHEN** 设置成功
- **THEN** 所有在线小镇成员收到通知

#### Scenario: Notify on phome deletion

- **GIVEN** 镇长删除了 phome
- **WHEN** 删除成功
- **THEN** 所有在线小镇成员收到通知

### Requirement: TimedAttribute Command Alias

计时属性模块 SHALL 支持简短命令别名。

#### Scenario: Use attr alias

- **GIVEN** 玩家输入 `/tsl attr`
- **WHEN** 命令执行
- **THEN** 与 `/tsl timed-attribute` 行为一致
