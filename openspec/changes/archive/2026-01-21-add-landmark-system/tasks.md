# Tasks: Add Landmark System

## 1. Core Infrastructure

- [x] 1.1 创建 `landmark/` 模块目录结构
- [x] 1.2 定义 `LandmarkData.kt` 数据类（Landmark, Region, PlayerUnlocks）
- [x] 1.3 实现 `LandmarkStorage.kt` JSON 持久化（保存/加载地标和解锁数据）
- [x] 1.4 实现 `LandmarkManager.kt` 核心管理类

## 2. Region Detection

- [x] 2.1 实现 AABB 区域检测逻辑
- [x] 2.2 实现玩家当前地标缓存机制
- [x] 2.3 实现 `LandmarkListener.kt` 移动事件监听
- [x] 2.4 实现进入提示与防刷屏逻辑

## 3. Unlock System

- [x] 3.1 实现首次进入自动解锁
- [x] 3.2 实现默认解锁地标逻辑
- [x] 3.3 实现解锁记录查询接口

## 4. Commands

- [x] 4.1 注册 `/lm` 命令
- [x] 4.2 实现 `create` 子命令（创建地标）
- [x] 4.3 实现 `setpos1`/`setpos2`/`finalize` 子命令（设置区域）
- [x] 4.4 实现 `delete` 子命令（删除地标，需确认）
- [x] 4.5 实现 `edit` 子命令（编辑属性）
- [x] 4.6 实现 `trust`/`untrust` 子命令（维护者授权）
- [x] 4.7 实现 `tp` 子命令（传送）
- [x] 4.8 实现 `gui` 子命令（打开菜单）
- [x] 4.9 实现 `list` 子命令（列出地标）
- [x] 4.10 添加 Tab 补全

## 5. GUI System

- [x] 5.1 实现 `LandmarkGUI.kt` 菜单框架
- [x] 5.2 实现地标列表展示（分页）
- [x] 5.3 实现已解锁/未解锁状态显示
- [x] 5.4 实现点击传送功能

## 6. Teleportation

- [x] 6.1 实现传送前置条件检查（在地标内、已解锁）
- [x] 6.2 实现安全落点算法
- [x] 6.3 实现可选吟唱时间
- [x] 6.4 实现可选移动/受伤打断
- [x] 6.5 实现可选冷却时间

## 7. Configuration

- [x] 7.1 添加 `config.yml` 地标配置节
- [x] 7.2 添加消息模板配置
- [x] 7.3 实现配置热重载

## 8. Testing & Documentation

- [x] 8.1 编写验收测试步骤文档
- [x] 8.2 完成基本功能手动测试
- [x] 8.3 更新 README 添加地标系统说明

## Dependencies

- 任务组 1 是基础，必须先完成
- 任务组 2-3 可并行，依赖任务组 1
- 任务组 4-6 依赖任务组 1-3
- 任务组 7 可与任务组 4-6 并行
- 任务组 8 最后完成
