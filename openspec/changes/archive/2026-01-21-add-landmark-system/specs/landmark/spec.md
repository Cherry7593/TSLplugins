# Landmark System

## ADDED Requirements

### Requirement: Landmark Creation

管理员 SHALL 能够创建地标，包含名称和 AABB 区域范围。

- 地标名称在服务器内必须唯一（大小写敏感性可配置）
- 区域范围由两点 (pos1, pos2) 确定，必须在同一维度
- 区域体积不得超过配置上限
- 创建完成后地标状态为 ACTIVE

#### Scenario: Admin creates landmark successfully

- **WHEN** 管理员执行 `/lm create <name>` 并设置 pos1/pos2
- **THEN** 地标被创建并保存，状态为 ACTIVE

#### Scenario: Duplicate name rejected

- **WHEN** 管理员尝试创建已存在名称的地标
- **THEN** 创建失败并提示名称已存在

#### Scenario: Region volume exceeds limit

- **WHEN** 管理员设置的区域体积超过配置上限
- **THEN** 创建失败并提示体积超限

---

### Requirement: Region Entry Detection

系统 SHALL 检测玩家进入地标区域并触发提示。

- 玩家从区域外进入区域内时触发进入提示
- 停留在区域内不重复触发
- 离开后再进入可再次触发
- 可配置进入提示冷却时间

#### Scenario: Player enters landmark region

- **WHEN** 玩家从地标区域外移动到区域内
- **THEN** 显示进入提示消息

#### Scenario: Player stays in region

- **WHEN** 玩家在地标区域内移动
- **THEN** 不重复显示进入提示

#### Scenario: Player re-enters after leaving

- **WHEN** 玩家离开地标区域后再次进入
- **THEN** 再次显示进入提示

---

### Requirement: Landmark Unlock

系统 SHALL 在玩家首次进入地标区域时自动解锁该地标。

- 首次进入时记录解锁状态
- 默认解锁地标无需进入即视为已解锁
- 解锁记录持久化保存

#### Scenario: First entry unlocks landmark

- **WHEN** 玩家首次进入某地标区域
- **THEN** 该地标被解锁，记录保存

#### Scenario: Default unlocked landmark

- **WHEN** 地标设置为默认解锁
- **THEN** 所有玩家无需进入即视为已解锁

---

### Requirement: Landmark GUI

系统 SHALL 提供 GUI 菜单展示所有地标及其状态。

- 展示地标名称、描述、解锁状态
- 已解锁地标可点击传送
- 未解锁地标显示为锁定状态，不可传送
- 可配置是否显示坐标信息

#### Scenario: Player opens GUI

- **WHEN** 玩家执行 `/lm` 或 `/lm gui`
- **THEN** 打开地标菜单 GUI

#### Scenario: Unlocked landmark shows teleport option

- **WHEN** 玩家查看已解锁地标
- **THEN** 显示可传送状态，点击可发起传送

#### Scenario: Locked landmark shows locked state

- **WHEN** 玩家查看未解锁地标
- **THEN** 显示锁定状态，不可点击传送

---

### Requirement: Teleportation Rules

系统 SHALL 限制传送只能在地标区域内发起，且目标必须是已解锁地标。

- 玩家必须位于任意地标区域内才能发起传送
- 目标地标必须存在且状态为 ACTIVE
- 目标地标必须是已解锁（或默认解锁）
- 传送落点为地标指定点或中心点
- 落点必须安全（非虚空/岩浆/窒息）

#### Scenario: Teleport from inside landmark

- **WHEN** 玩家在地标区域内执行 `/lm tp <name>` 到已解锁地标
- **THEN** 传送成功

#### Scenario: Teleport from outside landmark rejected

- **WHEN** 玩家不在任何地标区域内尝试传送
- **THEN** 传送失败并提示必须在地标区域内

#### Scenario: Teleport to locked landmark rejected

- **WHEN** 玩家尝试传送到未解锁地标
- **THEN** 传送失败并提示目标未解锁

#### Scenario: Unsafe landing rejected

- **WHEN** 目标地标落点不安全且无法找到安全位置
- **THEN** 传送失败并提示原因

---

### Requirement: Landmark Editing

管理员和授权维护者 SHALL 能够编辑地标属性。

- 管理员可编辑所有地标的所有属性
- 维护者仅可编辑被授权地标的 icon/lore/warpPoint
- 普通玩家不可编辑

#### Scenario: Admin edits landmark

- **WHEN** 管理员执行 `/lm edit <name>`
- **THEN** 可修改地标的所有属性

#### Scenario: Maintainer edits authorized landmark

- **WHEN** 维护者编辑被授权的地标
- **THEN** 可修改 icon/lore/warpPoint

#### Scenario: Maintainer cannot edit unauthorized landmark

- **WHEN** 维护者尝试编辑未授权的地标
- **THEN** 操作被拒绝

---

### Requirement: Maintenance Authorization

管理员 SHALL 能够授权玩家成为地标维护者。

- 使用 `/lm trust <name> <player>` 授权
- 使用 `/lm untrust <name> <player>` 撤销授权
- 维护者列表持久化保存

#### Scenario: Grant maintenance permission

- **WHEN** 管理员执行 `/lm trust <name> <player>`
- **THEN** 玩家成为该地标的维护者

#### Scenario: Revoke maintenance permission

- **WHEN** 管理员执行 `/lm untrust <name> <player>`
- **THEN** 玩家不再是该地标的维护者

---

### Requirement: Landmark Deletion

管理员 SHALL 能够删除地标，需要二次确认。

- 删除需要确认（`/lm delete <name> confirm`）
- 删除后地标从活跃列表移除
- 玩家解锁记录可保留或清理（实现一致即可）

#### Scenario: Delete with confirmation

- **WHEN** 管理员执行 `/lm delete <name> confirm`
- **THEN** 地标被删除

#### Scenario: Delete without confirmation rejected

- **WHEN** 管理员执行 `/lm delete <name>` 不带 confirm
- **THEN** 提示需要确认

---

### Requirement: Data Persistence

系统 SHALL 持久化保存地标数据和玩家解锁记录。

- 服务器重启后数据不丢失
- 支持热重载配置

#### Scenario: Data survives restart

- **WHEN** 服务器重启
- **THEN** 地标和解锁记录恢复正常

#### Scenario: Config hot reload

- **WHEN** 执行重载命令
- **THEN** 配置更新生效

---

### Requirement: Teleport Enhancements

系统 SHALL 提供可配置的传送增强功能（默认关闭）。

- 吟唱时间（倒计时）—— 配置 `teleport.castTimeSeconds`
- 移动/受伤打断 —— 配置 `teleport.cancelOnMove`
- 传送冷却时间 —— 配置 `teleport.cooldownSeconds`
- 所有增强功能默认关闭，可通过配置启用

#### Scenario: Cast time enabled

- **WHEN** 配置启用吟唱时间
- **THEN** 玩家传送前需等待倒计时

#### Scenario: Movement cancels teleport

- **WHEN** 配置启用移动打断且玩家在吟唱期间移动
- **THEN** 传送被取消

#### Scenario: Cooldown enforced

- **WHEN** 配置启用冷却时间且玩家刚完成传送
- **THEN** 冷却期间不可再次传送
