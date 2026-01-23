## ADDED Requirements

### Requirement: Region Leave Detection

系统 SHALL 检测玩家离开地标区域并触发提示。

- 玩家从地标区域内移动到区域外时触发离开提示
- 从地标A直接进入地标B时不触发离开提示（视为区域切换）
- 离开提示受冷却时间限制，复用进入提示冷却配置
- 提示形式较进入提示轻量（ActionBar + 聊天消息）

#### Scenario: Player leaves landmark region

- **WHEN** 玩家从地标区域内移动到任何地标区域外
- **THEN** 显示离开提示消息

#### Scenario: Player switches between landmarks

- **WHEN** 玩家从地标A区域直接进入地标B区域
- **THEN** 不显示地标A的离开提示，只显示地标B的进入提示

#### Scenario: Leave message cooldown

- **WHEN** 玩家在冷却时间内多次离开同一地标
- **THEN** 仅首次离开显示提示，后续离开不重复显示
