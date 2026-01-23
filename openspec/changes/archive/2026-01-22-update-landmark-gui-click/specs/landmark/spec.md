## MODIFIED Requirements

### Requirement: Landmark GUI

系统 SHALL 提供 GUI 菜单展示所有地标及其状态，并支持左右键分离操作。

- 展示地标名称、描述、解锁状态
- 已解锁地标：左键点击传送，右键点击打开编辑页面（需权限）
- 未解锁地标显示为锁定状态，不可传送
- 可配置是否显示坐标信息
- 右键编辑权限：仅管理员（`tsl.landmark.admin`）或地标维护者可使用，其他玩家右键无反应

#### Scenario: Player opens GUI

- **WHEN** 玩家执行 `/lm` 或 `/lm gui`
- **THEN** 打开地标菜单 GUI

#### Scenario: Unlocked landmark shows teleport option

- **WHEN** 玩家查看已解锁地标
- **THEN** 显示可传送状态，左键点击可发起传送

#### Scenario: Left-click teleports to unlocked landmark

- **WHEN** 玩家左键点击已解锁地标
- **THEN** 发起传送请求（受传送规则约束）

#### Scenario: Right-click opens edit menu for authorized user

- **WHEN** 管理员或地标维护者右键点击地标
- **THEN** 打开该地标的编辑页面

#### Scenario: Right-click ignored for unauthorized user

- **WHEN** 普通玩家（非管理员且非该地标维护者）右键点击地标
- **THEN** 无任何反应（静默忽略）

#### Scenario: Locked landmark shows locked state

- **WHEN** 玩家查看未解锁地标
- **THEN** 显示锁定状态，不可点击传送
