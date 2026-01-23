## ADDED Requirements

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
