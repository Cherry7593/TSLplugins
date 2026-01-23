# Tasks: 为 TownPHome 添加 GUI

## 实现任务

### 1. 创建 TownPHomeGUI 类
- [x] 1.1 创建 `TownPHomeGUI.kt`，实现 `Listener` 接口
- [x] 1.2 定义 `TownPHomeInventoryHolder` 存储 GUI 状态（menuType, page, townName）
- [x] 1.3 实现 `openMainMenu(player, page)` 显示 PHome 列表
- [x] 1.4 实现 `onInventoryClick` 处理点击事件（传送、删除、翻页、创建）
- [x] 1.5 实现 `handleChatInput` 处理创建 PHome 时的名称输入

### 2. 修改 TownPHomeCommand
- [x] 2.1 添加 `gui` 子命令打开 GUI
- [x] 2.2 在 `tabComplete` 中添加 `gui` 补全

### 3. 集成到模块
- [x] 3.1 在 `TSLplugins.kt` 主类中注册 GUI Listener

### 4. 添加消息
- [x] 4.1 在 `messages.yml` 添加 GUI 相关消息

## 验证任务
- [x] 5.1 测试普通成员查看列表、传送
- [x] 5.2 测试管理角色创建、删除 PHome
- [x] 5.3 测试分页功能
- [x] 5.4 测试达到上限时的创建行为

## 代码审查
- [x] 修复: OP 创建 PHome 后 GUI 以非 OP 模式重新打开
- [x] 修复: OP 没有小镇时 tabComplete 返回空列表
- [x] 优化: OP 模式下创建 PHome 应绕过等级限制
