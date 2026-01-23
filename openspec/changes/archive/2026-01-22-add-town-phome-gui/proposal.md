# Proposal: 为 TownPHome 添加 GUI

## Problem
当前 TownPHome 模块只能通过命令操作，缺乏图形界面。玩家需要记住命令语法才能使用 PHome 功能，用户体验不够友好。

## Solution
为 TownPHome 模块添加一个 GUI 界面，允许玩家通过物品栏界面查看、传送、管理 PHome 点。

### GUI 功能
1. **PHome 列表页面**
   - 显示当前小镇所有 PHome 点
   - 左键点击传送到 PHome
   - 右键点击删除（仅管理角色）
   - 显示当前数量/上限

2. **创建 PHome 功能**
   - 管理角色可通过 GUI 创建新 PHome
   - 点击后在聊天输入名称

### 技术实现
- 采用 `LandmarkGUI` 模式：使用自定义 `InventoryHolder` 存储 GUI 状态
- 支持分页（每页 45 个 PHome）
- 遵循现有代码风格和架构

## Impact
- 新增文件：`TownPHomeGUI.kt`
- 修改文件：`TownPHomeCommand.kt`（添加 `gui` 子命令）
- 修改文件：`messages.yml`（添加 GUI 相关消息）

## Complexity
低 - 参考 LandmarkGUI 实现，模式清晰
