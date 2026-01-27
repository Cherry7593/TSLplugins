## MODIFIED Requirements

### Requirement: 地标导航指南针
系统 SHALL 提供导航指南针功能，允许玩家追踪未解锁的地标位置。

#### Scenario: 指南针初始化
- **WHEN** 插件启动时
- **THEN** LandmarkCompass 实例 SHALL 被创建并传递给 LandmarkListener 和 LandmarkCommand

#### Scenario: 指南针交互监听
- **WHEN** 玩家手持导航指南针进行左键或右键操作
- **THEN** 系统 SHALL 切换到上一个或下一个未解锁地标

#### Scenario: 粒子效果启停
- **WHEN** 玩家切换主手物品至导航指南针
- **THEN** 系统 SHALL 启动粒子引导线渲染任务
- **WHEN** 玩家切换主手物品离开导航指南针
- **THEN** 系统 SHALL 停止粒子渲染任务

#### Scenario: 到达目标检测
- **WHEN** 玩家进入正在追踪的地标区域
- **THEN** 系统 SHALL 发送到达提示并自动切换到下一个未解锁地标

#### Scenario: 玩家退出清理
- **WHEN** 玩家退出服务器
- **THEN** 系统 SHALL 清理该玩家的指南针追踪状态和粒子任务
