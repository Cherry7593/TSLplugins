## MODIFIED Requirements

### Requirement: Landmark Editing

管理员和授权维护者 SHALL 能够编辑地标属性。

- 管理员可编辑所有地标的所有属性
- 维护者仅可编辑被授权地标的 icon/lore/warpPoint
- 普通玩家不可编辑
- **设置传送点（warpPoint）时，玩家当前位置必须在目标地标的区域范围内（X/Z 坐标验证）**

#### Scenario: Admin edits landmark

- **WHEN** 管理员执行 `/lm edit <name>`
- **THEN** 可修改地标的所有属性

#### Scenario: Maintainer edits authorized landmark

- **WHEN** 维护者编辑被授权的地标
- **THEN** 可修改 icon/lore/warpPoint

#### Scenario: Maintainer cannot edit unauthorized landmark

- **WHEN** 维护者尝试编辑未授权的地标
- **THEN** 操作被拒绝

#### Scenario: Set warp point from inside landmark region

- **WHEN** 管理员或维护者在目标地标区域内执行 `/lm setwarp <name>` 或通过 GUI 设置传送点
- **THEN** 传送点设置成功，保存玩家当前位置

#### Scenario: Set warp point from outside landmark region rejected

- **WHEN** 管理员或维护者在目标地标区域外尝试设置传送点
- **THEN** 操作被拒绝并提示"必须在地标区域内才能设置传送点"
