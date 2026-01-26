# landmark Specification

## Purpose
TBD - created by archiving change add-landmark-system. Update Purpose after archive.
## Requirements
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

### Requirement: Landmark Navigation Compass

系统 SHALL 提供专属导航指南针，帮助玩家发现和前往未解锁地标。

- 指南针为特殊物品，通过命令 `/lm compass` 由管理员给予玩家
- 指南针使用PDC标识，区别于普通指南针
- 指南针lore显示玩家当前解锁进度（格式：已解锁 X/Y）
- 指南针支持追踪任意未解锁地标

#### Scenario: Admin gives compass to player

- **WHEN** 管理员执行 `/lm compass <player>`
- **THEN** 目标玩家获得一个地标导航指南针，lore显示当前解锁进度

#### Scenario: Compass lore shows unlock progress

- **WHEN** 玩家查看指南针lore
- **THEN** 显示"已解锁 X/Y"格式的进度信息，X为已解锁数量，Y为总地标数量

---

### Requirement: Compass Target Switching

玩家 SHALL 能够通过左键/右键在未解锁地标之间切换追踪目标。

- 右键指南针切换到下一个未解锁地标
- 左键指南针切换到上一个未解锁地标
- 切换时发送聊天消息提示当前追踪目标
- 若无未解锁地标，提示玩家已全部解锁

#### Scenario: Right-click switches to next locked landmark

- **WHEN** 玩家手持导航指南针右键
- **THEN** 切换到下一个未解锁地标，聊天提示"正在追踪: <地标名>"

#### Scenario: Left-click switches to previous locked landmark

- **WHEN** 玩家手持导航指南针左键
- **THEN** 切换到上一个未解锁地标，聊天提示"正在追踪: <地标名>"

#### Scenario: Click when all unlocked

- **WHEN** 玩家已解锁所有地标时点击指南针
- **THEN** 提示"你已探索完所有地标！"

---

### Requirement: Particle Guide Trail

系统 SHALL 在玩家手持导航指南针时显示粒子引导线指向目标地标。

- 粒子引导线从玩家前方开始，沿目标方向延伸
- 粒子仅对持有指南针的玩家可见
- 粒子渲染频率和长度可配置
- 引导线随玩家转向实时更新方向

#### Scenario: Particle trail displayed when holding compass

- **WHEN** 玩家手持正在追踪目标的导航指南针
- **THEN** 显示粒子引导线指向目标地标方向

#### Scenario: Particle trail only visible to holder

- **WHEN** 玩家A手持指南针追踪地标
- **THEN** 只有玩家A能看到粒子引导线，其他玩家不可见

#### Scenario: Particle trail updates with player rotation

- **WHEN** 玩家手持指南针转向
- **THEN** 粒子引导线方向随之更新，始终指向目标地标

#### Scenario: No particle when not holding compass

- **WHEN** 玩家将指南针放入背包（非主手持有）
- **THEN** 停止显示粒子引导线

---

### Requirement: Cross-Dimension Navigation

系统 SHALL 正确处理目标地标在其他维度的情况。

- 目标在其他维度时不显示粒子引导线
- 提示玩家目标所在维度名称
- 玩家进入正确维度后恢复粒子引导

#### Scenario: Target in different dimension

- **WHEN** 玩家手持指南针追踪其他维度的地标
- **THEN** 不显示粒子引导线，提示"目标位于 <维度名>，请前往该维度"

#### Scenario: Entering correct dimension resumes guidance

- **WHEN** 玩家进入目标地标所在维度
- **THEN** 恢复显示粒子引导线

---

### Requirement: Arrival Notification

系统 SHALL 在玩家到达追踪目标地标区域时发送提示。

- 进入目标地标区域时发送到达提示
- 到达后自动切换到下一个未解锁地标继续追踪
- 若无其他未解锁地标，停止追踪并提示

#### Scenario: Arrival at tracked landmark

- **WHEN** 玩家进入正在追踪的地标区域
- **THEN** 发送"你已到达追踪目标: <地标名>"提示

#### Scenario: Auto-switch to next after arrival

- **WHEN** 玩家进入追踪目标地标区域且还有其他未解锁地标
- **THEN** 自动切换到下一个未解锁地标继续追踪

#### Scenario: Stop tracking when all explored

- **WHEN** 玩家到达最后一个未解锁地标
- **THEN** 停止追踪，提示"恭喜！你已探索完所有地标"

